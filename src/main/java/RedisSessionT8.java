import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;

/**
 * Created by jorge.rojas on 6/19/2015.
 */
public class RedisSessionT8 extends StandardSession {

    /**
     * Construct a new Session associated with the specified Manager.
     *
     * @param manager The manager with which this Session is associated
     */
    public RedisSessionT8(Manager manager) {
        super(manager);
    }


    protected void setLastAccessedTime(long lastAccessedTime){
        super.lastAccessedTime = lastAccessedTime;
    }

}
