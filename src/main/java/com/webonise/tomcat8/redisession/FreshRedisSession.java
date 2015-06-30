package com.webonise.tomcat8.redisession;

import com.webonise.tomcat8.redisession.redisclient.Redis;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;

import java.security.Principal;
import java.util.*;

/**
 * The Redis-backed {@link Session} implementation.
 */
public class FreshRedisSession extends StandardSession implements Session {

  public FreshRedisSession(RedisSessionManager manager) {
    super(manager);
    Objects.requireNonNull(manager, "manager responsible for this session");
  }

  /**
   * Provides direct access to the Redis client.
   *
   * @return The client to access Redis; never {@code null}.
   */
  protected Redis getRedis() {
    return getManager().getRedis();
  }

  @Override
  public RedisSessionManager getManager() {
    return RedisSessionManager.class.cast(super.getManager());
  }

  /**
   * Return the authentication type used to authenticate our cached
   * Principal, if any.
   */
  @Override
  public String getAuthType() {
    return super.getAuthType();
  }

  /**
   * Set the authentication type used to authenticate our cached
   * Principal, if any.
   *
   * @param authType The new cached authentication type
   */
  @Override
  public void setAuthType(String authType) {
    super.setAuthType(authType);
  }

  /**
   * Set the creation time for this session.  This method is called by the
   * Manager when an existing Session instance is reused.
   *
   * @param time The new creation time
   */
  @Override
  public void setCreationTime(long time) {
    super.setCreationTime(time);
  }

  /**
   * Return the last time the client sent a request associated with this
   * session, as the number of milliseconds since midnight, January 1, 1970
   * GMT.  Actions that your application takes, such as getting or setting
   * a value associated with the session, do not affect the access time.
   * This one gets updated whenever a request starts.
   */
  @Override
  public long getThisAccessedTime() {
    return super.getThisAccessedTime();
  }

  /**
   * Return the last client access time without invalidation check
   *
   * @see #getThisAccessedTime()
   */
  @Override
  public long getThisAccessedTimeInternal() {
    return super.getThisAccessedTimeInternal();
  }

  /**
   * Return the last time the client sent a request associated with this
   * session, as the number of milliseconds since midnight, January 1, 1970
   * GMT.  Actions that your application takes, such as getting or setting
   * a value associated with the session, do not affect the access time.
   * This one gets updated whenever a request finishes.
   */
  @Override
  public long getLastAccessedTime() {
    return super.getLastAccessedTime();
  }

  /**
   * Return the last client access time without invalidation check
   *
   * @see #getLastAccessedTime()
   */
  @Override
  public long getLastAccessedTimeInternal() {
    return super.getLastAccessedTimeInternal();
  }

  /**
   * Return the idle time (in milliseconds) from last client access time.
   */
  @Override
  public long getIdleTime() {
    return super.getIdleTime();
  }

  /**
   * Return the idle time from last client access time without invalidation check
   *
   * @see #getIdleTime()
   */
  @Override
  public long getIdleTimeInternal() {
    return super.getIdleTimeInternal();
  }

  /**
   * Return the maximum time interval, in seconds, between client requests
   * before the servlet container will invalidate the session.  A negative
   * time indicates that the session should never time out.
   */
  @Override
  public int getMaxInactiveInterval() {
    return super.getMaxInactiveInterval();
  }

  /**
   * Set the maximum time interval, in seconds, between client requests
   * before the servlet container will invalidate the session.  A zero or
   * negative time indicates that the session should never time out.
   *
   * @param interval The new maximum interval
   */
  @Override
  public void setMaxInactiveInterval(int interval) {
    super.setMaxInactiveInterval(interval);
  }

  /**
   * Return the authenticated Principal that is associated with this Session.
   * This provides an <code>Authenticator</code> with a means to cache a
   * previously authenticated Principal, and avoid potentially expensive
   * <code>Realm.authenticate()</code> calls on every request.  If there
   * is no current associated Principal, return <code>null</code>.
   */
  @Override
  public Principal getPrincipal() {
    return super.getPrincipal();
  }

  /**
   * Set the authenticated Principal that is associated with this Session.
   * This provides an <code>Authenticator</code> with a means to cache a
   * previously authenticated Principal, and avoid potentially expensive
   * <code>Realm.authenticate()</code> calls on every request.
   *
   * @param principal The new Principal, or <code>null</code> if none
   */
  @Override
  public void setPrincipal(Principal principal) {
    super.setPrincipal(principal);
  }

  /**
   * Release all object references, and initialize instance variables, in
   * preparation for reuse of this object.
   */
  @Override
  public void recycle() {
    super.recycle();
  }

  /**
   * Perform the internal processing required to passivate
   * this session.
   */
  @Override
  public void passivate() {
    super.passivate();
  }

  /**
   * Return the time when this session was created, in milliseconds since
   * midnight, January 1, 1970 GMT.
   *
   * @throws IllegalStateException if this method is called on an
   *                               invalidated session
   */
  @Override
  public long getCreationTime() {
    return super.getCreationTime();
  }

  /**
   * Return the time when this session was created, in milliseconds since
   * midnight, January 1, 1970 GMT, bypassing the session validation checks.
   */
  @Override
  public long getCreationTimeInternal() {
    return super.getCreationTimeInternal();
  }

  /**
   * Return the object bound with the specified name in this session, or
   * <code>null</code> if no object is bound with that name.
   *
   * @param name Name of the attribute to be returned
   * @throws IllegalStateException if this method is called on an
   *                               invalidated session
   */
  @Override
  public Object getAttribute(String name) {
    return super.getAttribute(name);
  }

  /**
   * Return an <code>Enumeration</code> of <code>String</code> objects
   * containing the names of the objects bound to this session.
   *
   * @throws IllegalStateException if this method is called on an
   *                               invalidated session
   */
  @Override
  public Enumeration<String> getAttributeNames() {
    return super.getAttributeNames();
  }

  /**
   * Invalidates this session and unbinds any objects bound to it.
   *
   * @throws IllegalStateException if this method is called on
   *                               an invalidated session
   */
  @Override
  public void invalidate() {
    super.invalidate();
  }

  /**
   * Remove the object bound with the specified name from this session.  If
   * the session does not have an object bound with this name, this method
   * does nothing.
   * <p>
   * After this method executes, and if the object implements
   * <code>HttpSessionBindingListener</code>, the container calls
   * <code>valueUnbound()</code> on the object.
   *
   * @param name Name of the object to remove from this session.
   * @throws IllegalStateException if this method is called on an
   *                               invalidated session
   */
  @Override
  public void removeAttribute(String name) {
    super.removeAttribute(name);
  }

  /**
   * Remove the object bound with the specified name from this session.  If
   * the session does not have an object bound with this name, this method
   * does nothing.
   * <p>
   * After this method executes, and if the object implements
   * <code>HttpSessionBindingListener</code>, the container calls
   * <code>valueUnbound()</code> on the object.
   *
   * @param name   Name of the object to remove from this session.
   * @param notify Should we notify interested listeners that this
   *               attribute is being removed?
   * @throws IllegalStateException if this method is called on an
   *                               invalidated session
   */
  @Override
  public void removeAttribute(String name, boolean notify) {
    super.removeAttribute(name, notify);
  }

  /**
   * Bind an object to this session, using the specified name.  If an object
   * of the same name is already bound to this session, the object is
   * replaced.
   * <p>
   * After this method executes, and if the object implements
   * <code>HttpSessionBindingListener</code>, the container calls
   * <code>valueBound()</code> on the object.
   *
   * @param name  Name to which the object is bound, cannot be null
   * @param value Object to be bound, cannot be null
   * @throws IllegalArgumentException if an attempt is made to add a
   *                                  non-serializable object in an environment marked distributable.
   * @throws IllegalStateException    if this method is called on an
   *                                  invalidated session
   */
  @Override
  public void setAttribute(String name, Object value) {
    super.setAttribute(name, value);
  }

  /**
   * Bind an object to this session, using the specified name.  If an object
   * of the same name is already bound to this session, the object is
   * replaced.
   * <p>
   * After this method executes, and if the object implements
   * <code>HttpSessionBindingListener</code>, the container calls
   * <code>valueBound()</code> on the object.
   *
   * @param name   Name to which the object is bound, cannot be null
   * @param value  Object to be bound, cannot be null
   * @param notify whether to notify session listeners
   * @throws IllegalArgumentException if an attempt is made to add a
   *                                  non-serializable object in an environment marked distributable.
   * @throws IllegalStateException    if this method is called on an
   *                                  invalidated session
   */
  @Override
  public void setAttribute(String name, Object value, boolean notify) {
    super.setAttribute(name, value, notify);
  }

  /**
   * Return the names of all currently defined session attributes
   * as an array of Strings.  If there are no defined attributes, a
   * zero-length array is returned.
   */
  @Override
  protected String[] keys() {
    return super.keys();
  }

  /**
   * Remove the object bound with the specified name from this session.  If
   * the session does not have an object bound with this name, this method
   * does nothing.
   * <p>
   * After this method executes, and if the object implements
   * <code>HttpSessionBindingListener</code>, the container calls
   * <code>valueUnbound()</code> on the object.
   *
   * @param name   Name of the object to remove from this session.
   * @param notify Should we notify interested listeners that this
   */
  @Override
  protected void removeAttributeInternal(String name, boolean notify) {
    super.removeAttributeInternal(name, notify);
  }

  /**
   * Return the <code>isValid</code> flag for this session without any expiration
   * check.
   */
  @Override
  protected boolean isValidInternal() {
    return super.isValidInternal();
  }

  /**
   * Check whether the Object can be distributed. This implementation
   * simply checks for serializability. Derived classes might use other
   * distribution technology not based on serialization and can extend
   * this check.
   *
   * @param name  The name of the attribute to check
   * @param value The value of the attribute to check
   * @return true if the attribute is distributable, false otherwise
   */
  @Override
  protected boolean isAttributeDistributable(String name, Object value) {
    return super.isAttributeDistributable(name, value);
  }
}
