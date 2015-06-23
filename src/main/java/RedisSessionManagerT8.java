import org.apache.catalina.*;
import org.apache.catalina.session.ManagerBase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.io.IOException;
import java.util.*;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import redis.clients.jedis.Transaction;

/**
 * Created by Cesar Valverde on 6/10/2015.
 */
public class RedisSessionManagerT8 extends ManagerBase{

    private static final Log log = LogFactory.getLog(RedisSessionManagerT8.class);

    //Redis Keys
    private static final String REDIS_METADATA_KEY = ":metadata";
    private static final String REDIS_ATTRIBUTES_KEY = ":attributes";

    //Redis Connection pool
    protected JedisPool redisConnectionPool = null;
    protected JedisPoolConfig redisConnectionPoolConfig = null;

    //Database info
    private String dbHost = "localhost";
    private int dbPort = 2222;
    private int dbTimeout = 0;
    private String dbPassword = "";

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
    protected RedisSessionT8 getNewSession() {
        return new RedisSessionT8(this);
    }

    @Override
    public RedisSessionT8 createEmptySession() {
        return getNewSession();
    }

    @Override
    public Session createSession(String requestedSessionId) {

        RedisSessionT8 session = createEmptySession();

        // Initialize the properties of the new session and return it
        session.setId(UUID.randomUUID().toString());
        session.setNew(true);
        session.setValid(true);
        session.setCreationTime(System.currentTimeMillis());
        session.setMaxInactiveInterval(this.maxInactiveInterval);

        try {
            saveSession(session);
        } catch (Exception e) {
            log.error("Error - Context: createSession.", e);
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
            saveSession((RedisSessionT8) session);
        } catch (Exception e) {
            log.error("Error - Context: add.", e);
        }
    }

    /**
     * Find a session by id
     * @param id
     * @return
     * @throws IOException
     */
    @Override
    public Session findSession(String id) throws IOException {
        if(Objects.isNull(id)){
            return null;
        }
        Hashtable<String, String> result = null;
        try {
            result = withRedis(
                (Jedis jedis)-> {
                    Hashtable<String, String> r =  new Hashtable<>();
                    r.put(REDIS_METADATA_KEY, jedis.get(id + REDIS_METADATA_KEY));
                    r.put(REDIS_ATTRIBUTES_KEY, jedis.get(id + REDIS_ATTRIBUTES_KEY));
                    return r;
                }
            );
            try {
                return RedisSessionT8.getSession(this, RedisSessionT8.getMapFromEncodedString(result.get(REDIS_METADATA_KEY)),
                        RedisSessionT8.getMapFromEncodedString(result.get(REDIS_ATTRIBUTES_KEY)));
            } catch (ClassNotFoundException e) {
                log.error("Error - Context: findSession.", e);
                return null;
            }
        } catch (Exception e) {
            log.error("Error - Context: findSession.", e);
            return null;
        }
    }

    @Override
    public void remove(Session session) {
        remove(session, false);
    }

    @Override
    public void remove(Session session, boolean update) {
        try {
            withRedis(
                (Jedis jedis)-> {

                    //Remove attributes from redis
                    jedis.del((session.getId() + REDIS_ATTRIBUTES_KEY));

                    //Get encoded string metadata from Redis
                    String encodedStringMetadata = jedis.get((session.getId() + REDIS_METADATA_KEY));

                    //Update the valid value in metadata to false
                    Map<String, Object> metadata = RedisSessionT8.getMapFromEncodedString(encodedStringMetadata);
                    metadata.put(RedisSessionT8.METADATA_VALID, String.valueOf(false));

                    //Update the metadata en Redis
                    jedis.set(session.getId() + REDIS_METADATA_KEY, RedisSessionT8.getEncodedStringMetadata(metadata));

                    return 0;
                }
            );
        } catch (Exception e) {
            log.error("Error - Context: remove." + e);
        }
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
    // START REDIS
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Method to saved session in Redis
     * @param session
     */
    protected void saveSession(RedisSessionT8 session) throws Exception{
        withRedis(
            (Jedis jedis)-> {
                jedis.setex(session.getId() + REDIS_ATTRIBUTES_KEY, session.getMaxInactiveInterval(),
                        RedisSessionT8.getEncodedStringAttributes(session.getAttributes()));
                jedis.set(session.getId() + REDIS_METADATA_KEY, RedisSessionT8.getEncodedStringMetadata(RedisSessionT8.getMetadata(session)));
                return 0;
            }
        );
    }

    private interface RedisCallback<T> {
        T apply(Jedis jedis) throws Exception;
    }

    private <T> T withRedis(RedisCallback<T> callback) throws Exception {
        Objects.requireNonNull(redisConnectionPool, "redis connection pool must be initialized");
        Objects.requireNonNull(callback, "callback needs to be provided");
        Jedis jedis = null;
        Transaction transaction = null;
        try {
            jedis = redisConnectionPool.getResource();
            transaction = jedis.multi();
            T result = callback.apply(jedis);
            transaction.exec();
            redisConnectionPool.returnResourceObject(jedis);
            return result;
        } catch (Exception e) {
            if(transaction != null){
                transaction.discard();
            }
            redisConnectionPool.returnBrokenResource(jedis);
            throw e;
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
            log.error("Error - Context: stopInternal.", e);
            throw new LifecycleException(e.getMessage());
        }
        super.stopInternal();
    }

    //------------------------------------------------------------------------------------------------------------------
    // END Lifecycle
    //------------------------------------------------------------------------------------------------------------------
}
