import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.util.*;

/**
 * Created by Cesar Valverde on 6/19/2015.
 */
public class RedisSessionT8 extends StandardSession {

    private static final Log log = LogFactory.getLog(RedisSessionT8.class);

    //Metadata keys
    public static final String METADATA_VALID = "valid";
    public static final String METADATA_CREATION_TIME = "creation_time";
    public static final String METADATA_LAST_ACCESS_TIME = "last_access_time";
    public static final String METADATA_MAX_INACTIVE_INTERVAL = "max_inactive_interval";

    /**
     * Construct a new Session associated with the specified Manager.
     *
     * @param manager The manager with which this Session is associated
     */
    public RedisSessionT8(Manager manager) {
        super(manager);
    }

    @Override
    public RedisSessionManagerT8 getManager() {
        return (RedisSessionManagerT8) super.getManager();
    }

    protected void setLastAccessedTime(long lastAccessedTime){
        super.lastAccessedTime = lastAccessedTime;
    }

    /**
     * Get attributes
     */
    protected Map<String, Object> getAttributes(){
        return this.attributes;
    }


    /**
     * Set attributes
     */
    protected void setAttributes(Map<String, Object> attributes){
        this.attributes = attributes;
    }


    /**
     * Get metadata from a Session
     */
    public static Map<String, Object> getMetadata(Session session){
        Map<String, Object> metadata = new Hashtable<>();
        metadata.put(METADATA_VALID, String.valueOf(session.isValid()));
        metadata.put(METADATA_CREATION_TIME, DateFormatUtils.ISO_DATE_FORMAT.format(new Date(session.getCreationTime())));
        metadata.put(METADATA_LAST_ACCESS_TIME, DateFormatUtils.ISO_DATE_FORMAT.format(new Date(session.getLastAccessedTime())));
        metadata.put(METADATA_MAX_INACTIVE_INTERVAL, String.valueOf(session.getMaxInactiveInterval()));
        return metadata;
    }

    /**
     * Get session from metadata and attributes
     * @param metadata
     * @param attributes
     * @return
     */
    public static RedisSessionT8 getSession(Manager manager, Map<String, Object> metadata, Map<String, Object> attributes) throws IllegalArgumentException {

        Objects.requireNonNull(manager, "manager needs to be provided");
        Objects.requireNonNull(metadata, "metadata needs to be provided");
        Objects.requireNonNull(attributes, "attributes needs to be provided");

        RedisSessionT8 session = (RedisSessionT8) manager.createEmptySession();

        RedisSessionT8.validateKey(metadata, RedisSessionT8.METADATA_VALID, () -> {
            session.setValid(Boolean.valueOf((String) metadata.get(RedisSessionT8.METADATA_VALID)));
        });

        RedisSessionT8.validateKey(metadata, RedisSessionT8.METADATA_CREATION_TIME, () -> {
            session.setCreationTime(DateFormatUtils.ISO_DATE_FORMAT.parse(((String) metadata.get(RedisSessionT8.METADATA_CREATION_TIME))).getTime());
        });

        RedisSessionT8.validateKey(metadata, RedisSessionT8.METADATA_LAST_ACCESS_TIME, () -> {
            session.setLastAccessedTime(DateFormatUtils.ISO_DATE_FORMAT.parse(((String) metadata.get(RedisSessionT8.METADATA_LAST_ACCESS_TIME))).getTime());
        });

        RedisSessionT8.validateKey(metadata, RedisSessionT8.METADATA_MAX_INACTIVE_INTERVAL, () -> {
            session.setMaxInactiveInterval(Integer.valueOf((String) metadata.get(RedisSessionT8.METADATA_MAX_INACTIVE_INTERVAL)));
        });

        session.setAttributes(attributes);

        return session;
    }

    public interface CallBack {
        void apply() throws Exception;
    }

    public static void validateKey(Map map, String key, CallBack callback) throws IllegalArgumentException {
        Objects.requireNonNull(map, "map needs to be provided");
        Objects.requireNonNull(key, "key needs to be provided");
        Objects.requireNonNull(callback, "callback needs to be provided");
        if(map.containsKey(key) && Objects.nonNull(map.get(key))){
            try {
                callback.apply();
            } catch (Exception e) {
                throw new IllegalArgumentException(key + " is required", e);
            }
        }else{
            throw new IllegalArgumentException(key + " is required");
        }
    }


}
