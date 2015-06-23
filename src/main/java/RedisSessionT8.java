import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.text.ParseException;
import java.util.*;

/**
 * Created by jorge.rojas on 6/19/2015.
 */
public class RedisSessionT8 extends StandardSession {

    private static final Log log = LogFactory.getLog(RedisSessionT8.class);

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

    //Metadata keys
    public static final String METADATA_VALID = "valid";
    public static final String METADATA_CREATION_TIME = "creation_time";
    public static final String METADATA_LAST_ACCESS_TIME = "last_access_time";
    public static final String METADATA_MAX_INACTIVE_INTERVAL = "max_inactive_interval";

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
    public static Hashtable<String, Object> getMetadata(Session session){
        Hashtable<String, Object> metadata = new Hashtable<>();
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
    public static RedisSessionT8 getSession(Manager manager, Map<String, Object> metadata, Map<String, Object> attributes){
        RedisSessionT8 session = (RedisSessionT8) manager.createEmptySession();

        if(metadata.containsKey(RedisSessionT8.METADATA_VALID)){
            session.setValid(Boolean.valueOf((String) metadata.get(RedisSessionT8.METADATA_VALID)));
        }

        try {
            session.setCreationTime(DateFormatUtils.ISO_DATE_FORMAT.parse(((String) metadata.get(RedisSessionT8.METADATA_CREATION_TIME))).getTime());
        } catch (ParseException e) {
            log.error("Error - Context: getSession." + e);
        }

        try {
            session.setLastAccessedTime(DateFormatUtils.ISO_DATE_FORMAT.parse(((String) metadata.get(RedisSessionT8.METADATA_LAST_ACCESS_TIME))).getTime());
        } catch (ParseException e) {
            log.error("Error - Context: getSession.", e);
        }

        if(metadata.containsKey(RedisSessionT8.METADATA_MAX_INACTIVE_INTERVAL)){
            session.setMaxInactiveInterval(Integer.valueOf((String) metadata.get(RedisSessionT8.METADATA_MAX_INACTIVE_INTERVAL)));
        }

        session.setAttributes(attributes);

        return session;
    }


}
