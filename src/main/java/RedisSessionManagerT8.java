import org.apache.catalina.*;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.session.StandardSession;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.commons.lang3.time.DateFormatUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.apache.commons.lang3.SerializationUtils;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Created by Cesar Valverde on 6/10/2015.
 */
public class RedisSessionManagerT8 extends ManagerBase implements Lifecycle {

    private static final Log log = LogFactory.getLog(RedisSessionManagerT8.class);

    private static final String REDIS_METADATA_KEY = ":metadata";
    private static final String REDIS_ATTRIBUTES_KEY = ":attributes";

    //Metadata keys
    public static final String METADATA_VALID = "valid";
    public static final String METADATA_CREATION_TIME = "creation_time";
    public static final String METADATA_LAST_ACCESS_TIME = "last_access_time";
    public static final String METADATA_MAX_INACTIVE_INTERVAL = "max_inactive_interval";

    protected JedisPool redisConnectionPool = null;
    protected JedisPoolConfig redisConnectionPoolConfig = null;

    //Database info
    private String dbHost = "localhost";
    private int dbPort = 2222;
    private int dbTimeout = 0;
    private String dbPassword = "";

    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);

    @Override
    public void load() throws ClassNotFoundException, IOException {
    }

    @Override
    public void unload() throws IOException {
    }

    //------------------------------------------------------------------------------------------------------------------
    // START Sessions
    //------------------------------------------------------------------------------------------------------------------

    @Override
    public int getRejectedSessions() {
        // Essentially do nothing.
        return 0;
    }

    @Override
    public Session createSession(String requestedSessionId) {

        Session session = createEmptySession();

        // Initialize the properties of the new session and return it
        session.setId(UUID.randomUUID().toString());
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(this.maxInactiveInterval);

        try {
            saveSession(session);
        } catch (IOException e) {
            log.error("Error - Context: createSession. Description: " + e.getMessage());
        }

        return session;
    }

    /**
     * Add session to this manager
     * @param session
     */
    @Override
    public void add(Session session) {
        try {
            saveSession(session);
        } catch (IOException e) {
            log.error("Error - Context: add. Description: " + e.getMessage());
        }
    }

    @Override
    public Session findSession(String id) throws IOException {

        //Get metadata from Redis
        byte[] encodedMetadata = withRedis((Jedis jedis)-> jedis.get((id + REDIS_METADATA_KEY).getBytes()));
        Hashtable<String, Object> metadata =
                (Hashtable)SerializationUtils.deserialize(Base64.getDecoder().decode(encodedMetadata));

        //Get attributes from Redis
        byte[] encodedAttributes = withRedis((Jedis jedis)-> jedis.get((id + REDIS_ATTRIBUTES_KEY).getBytes()));
        Hashtable<String, Object> attributes =
                (Hashtable)SerializationUtils.deserialize(Base64.getDecoder().decode(encodedAttributes));

        Session session = getSession(metadata, attributes);

        return session;
    }

    @Override
    public void remove(Session session) {

        remove(session, false);
    }

    @Override
    public void remove(Session session, boolean update) {
        //Redis will remove the attributes
    }

    @Override
    public void processExpires() {
        // We are going to use Redis's ability to expire keys for session expiration.
        // Do nothing.
    }

    //------------------------------------------------------------------------------------------------------------------
    // END Sessions
    //------------------------------------------------------------------------------------------------------------------


    //------------------------------------------------------------------------------------------------------------------
    // START Utils
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Get session from metadata and attributes
     * @param metadata
     * @param attributes
     * @return
     */
    private StandardSession getSession(Hashtable<String, Object> metadata, Hashtable<String, Object> attributes){
        StandardSession standardSession = (StandardSession) createEmptySession();
        standardSession.setValid(Boolean.valueOf((String)metadata.get(METADATA_VALID)));
        try {
            standardSession.setCreationTime(DateFormatUtils.ISO_DATE_FORMAT.parse(((String) metadata.get(METADATA_CREATION_TIME))).getTime());
        } catch (ParseException e) {
            log.error("Error - Context: getSession. Description: " + e.getMessage());
        }
        standardSession.setMaxInactiveInterval(Integer.valueOf((String) metadata.get(METADATA_MAX_INACTIVE_INTERVAL)));

        for (Enumeration<String> enumerator = attributes.keys(); enumerator.hasMoreElements();) {
            String key = enumerator.nextElement();
            standardSession.setAttribute(key, attributes.get(key));
        }

        return standardSession;
    }


    /**
     * Get metadata from a Session
     * @param session
     * @return
     */
    private Hashtable<String, Object> getMetadata(Session session){
        Hashtable<String, Object> metadata = new Hashtable<>();
        metadata.put(METADATA_VALID, String.valueOf(session.isValid()));
        metadata.put(METADATA_CREATION_TIME, DateFormatUtils.ISO_DATE_FORMAT.format(new Date(session.getCreationTime())));
        metadata.put(METADATA_LAST_ACCESS_TIME, DateFormatUtils.ISO_DATE_FORMAT.format(new Date(session.getLastAccessedTime())));
        metadata.put(METADATA_MAX_INACTIVE_INTERVAL, String.valueOf(session.getMaxInactiveInterval()));
        return metadata;
    }

    /**
     * Get attributes from a Session
     * @param session
     * @return
     */
    private Hashtable<String, Object> getAttributes(Session session){
        StandardSession standardSession = (StandardSession)session;
        Hashtable<String, Object> attributes = new Hashtable();
        for (Enumeration<String> enumerator = standardSession.getAttributeNames(); enumerator.hasMoreElements();) {
            String key = enumerator.nextElement();
            attributes.put(key, standardSession.getAttribute(key));
        }
        return attributes;
    }

    //------------------------------------------------------------------------------------------------------------------
    // END Utils
    //------------------------------------------------------------------------------------------------------------------


    //------------------------------------------------------------------------------------------------------------------
    // START REDIS
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Method to saved session in redis
     * @param session
     * @return
     * @throws IOException
     */
    protected boolean saveSession(Session session) throws IOException {
        boolean errorSaving = true;
        //Get the session Id
        String id = session.getId();
        Hashtable<String, Object> metadata = getMetadata(session);
        Hashtable<String, Object> attributes = getAttributes(session);
        try{

            byte[] encodedMetadata = Base64.getEncoder().encode(SerializationUtils.serialize(metadata));
            byte[] encodedAttributes = Base64.getEncoder().encode(SerializationUtils.serialize(attributes));

            withRedis(
                    (Jedis jedis)-> {
                        jedis.setex((id + REDIS_ATTRIBUTES_KEY).getBytes(), session.getMaxInactiveInterval(), encodedAttributes);
                        return jedis.set((id + REDIS_METADATA_KEY).getBytes(), encodedMetadata);
                    }
            );

            errorSaving = false;
        }catch (Exception e){
            log.error("Error - Context: saveToRedis. Description: " + e.getMessage());
        }
        return errorSaving;
    }

    private interface RedisCallback<T> {
        T apply(Jedis jedis) throws Exception;
    }

    private <T> T withRedis(RedisCallback<T> callback) throws Exception {
        Objects.requireNonNull(redisConnectionPool, "redis connection pool must be initialized");
        Objects.requireNonNull(callback, "callback needs to be provided");
        try(Jedis jedis = redisConnectionPool.getResource()){
            T result = callback.apply(jedis);
            redisConnectionPool.returnResourceObject(jedis);
            return result;
        }catch(Exception e) {
            redisConnectionPool.returnBrokenResource(jedis);
            throw new RuntimeException("Error working with Redis", e);
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    // END REDIS
    //------------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------------
    // START Lifecycle
    //------------------------------------------------------------------------------------------------------------------

    @Override
    protected synchronized void startInternal() throws LifecycleException {
        super.startInternal();
        setState(LifecycleState.STARTING);
        redisConnectionPoolConfig = new JedisPoolConfig();
        redisConnectionPool = new JedisPool(this.redisConnectionPoolConfig, dbHost, dbPort, dbTimeout, dbPassword);
    }

    @Override
    protected synchronized void stopInternal() throws LifecycleException {
        setState(LifecycleState.STOPPING);
        try {
            redisConnectionPool.destroy();
        } catch(Exception e) {
            log.error("Error - Context: stopInternal. Description: " + e.getMessage());
        }
        super.stopInternal();
    }

    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        lifecycle.addLifecycleListener(listener);
    }

    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }
    //------------------------------------------------------------------------------------------------------------------
    // END Lifecycle
    //------------------------------------------------------------------------------------------------------------------
}
