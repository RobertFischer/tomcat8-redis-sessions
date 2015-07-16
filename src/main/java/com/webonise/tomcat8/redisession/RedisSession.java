package com.webonise.tomcat8.redisession;

import com.webonise.tomcat8.redisession.redisclient.*;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardSession;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * The Redis-backed {@link Session} implementation.
 */
public class RedisSession extends StandardSession implements Session {

  private static final Log log = LogFactory.getLog(RedisSession.class);

  private volatile RedisHashBackedPropertySupport<String> authProperty;
  private volatile RedisHashBackedPropertySupport<Long> creationTimeProperty;
  private volatile RedisHashBackedPropertySupport<Long> thisAccessedTimeProperty;
  private volatile RedisHashBackedPropertySupport<Long> lastAccessedTimeProperty;
  private volatile RedisHashBackedPropertySupport<Integer> maxInactiveIntervalProperty;
  private volatile RedisHashBackedPropertySupport<Serializable> principalProperty;
  private volatile Map<String, RedisHashBackedPropertySupport<Serializable>> attributesProperties;
  private volatile RedisHashBackedPropertySupport<Boolean> isValidProperty;

  public RedisSession(RedisSessionManager manager, String id) {
    super(manager);
    Objects.requireNonNull(manager, "manager responsible for this session");
    Objects.requireNonNull(manager.getRedis(), "Redis client for this session");
    Objects.requireNonNull(id, "the initial id for this session");
    this.id = id;
    initProperties();
    manager.autovivifySession(id);
  }

  protected static void triggerProperty(String name, RedisBackedPropertySupport<?> property) {
    if (property == null) {
      throw new IllegalStateException("Property is not initialized for " + name);
    }
    property.trigger();
  }

  protected static <T> void storeProperty(String name, RedisBackedPropertySupport<T> property, T value) {
    if (property == null) {
      throw new IllegalStateException("Property is not initialized for " + name);
    }
    property.store(value);
  }

  protected void initProperties() {
    Redis redis = getRedis();
    String metadataKey = getMetadataKey();

    attributesProperties = new ConcurrentSkipListMap<>();

    authProperty = new RedisHashBackedPropertySupport<>(
                                                           redis, metadataKey,
                                                           Convention.AUTH_TYPE_HKEY,
                                                           new StringConverter(),
                                                           authType -> this.authType = authType
    );

    creationTimeProperty = new RedisHashBackedPropertySupport<>(
                                                                   redis, metadataKey,
                                                                   Convention.CREATION_TIME_HKEY,
                                                                   new TimestampConverter(Date::new),
                                                                   time -> this.creationTime = time
    );

    thisAccessedTimeProperty = new RedisHashBackedPropertySupport<>(
                                                                       redis, metadataKey,
                                                                       Convention.THIS_ACCESSED_TIME_HKEY,
                                                                       new TimestampConverter(Date::new),
                                                                       time -> this.thisAccessedTime = time
    );

    lastAccessedTimeProperty = new RedisHashBackedPropertySupport<>(
                                                                       redis, metadataKey,
                                                                       Convention.LAST_ACCESS_TIME_HKEY,
                                                                       new TimestampConverter(Date::new),
                                                                       time -> this.lastAccessedTime = time
    );

    maxInactiveIntervalProperty = new RedisHashBackedPropertySupport<>(
                                                                          redis, metadataKey,
                                                                          Convention.MAX_INACTIVE_INTERVAL_HKEY,
                                                                          new IntegerConverter(),
                                                                          maxInterval -> this.maxInactiveInterval = maxInterval
    );

    principalProperty = new RedisHashBackedPropertySupport<>(
                                                                redis, metadataKey,
                                                                Convention.PRINCIPAL_HKEY,
                                                                new SerializableConverter<>(),
                                                                principal -> this.principal = (Principal) principal
    );

    isValidProperty = new RedisHashBackedPropertySupport<>(
                                                              redis, metadataKey,
                                                              Convention.IS_VALID_HKEY,
                                                              new BooleanConverter(),
                                                              newValue -> this.isValid = newValue
    );
  }

  private String getKey(String type, UnaryOperator<String> impl) {
    Objects.requireNonNull(type, "type of the key to generate");
    Objects.requireNonNull(impl, "implementation to generate this key");
    String id = this.id;
    if (id == null || id.isEmpty()) {
      throw new IllegalStateException("Cannot get the " + type + " key when the session id is null");
    }
    return impl.apply(id);
  }

  protected String getAttributesKey() {
    return getKey("attributes", Convention::sessionIdToAttributesKey);
  }

  protected String getMetadataKey() {
    return getKey("metadata", Convention::sessionIdToMetadataKey);
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
    triggerAuthTypeLoad();
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
    triggerAuthTypeStore(authType);
    super.setAuthType(authType);
  }

  protected void triggerAuthTypeLoad() {
    triggerProperty("authType", authProperty);
  }

  protected void triggerAuthTypeStore(String authType) {
    storeProperty("authType", authProperty, authType);
  }

  protected void triggerCreationTimeStore(Long time) {
    storeProperty("creationTime", creationTimeProperty, time);
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
    triggerThisAccessedTimeLoad();
    return super.getThisAccessedTime();
  }

  protected void triggerThisAccessedTimeLoad() {
    triggerProperty("thisAccessedTime", thisAccessedTimeProperty);
  }

  /**
   * Return the last client access time without invalidation check
   *
   * @see #getThisAccessedTime()
   */
  @Override
  public long getThisAccessedTimeInternal() {
    triggerThisAccessedTimeLoad();
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
    triggerLastAccessedTimeLoad();
    return super.getLastAccessedTime();
  }

  protected void triggerLastAccessedTimeLoad() {
    triggerProperty("lastAccessedTime", lastAccessedTimeProperty);
  }

  /**
   * Return the last client access time without invalidation check
   *
   * @see #getLastAccessedTime()
   */
  @Override
  public long getLastAccessedTimeInternal() {
    triggerLastAccessedTimeLoad();
    return super.getLastAccessedTimeInternal();
  }

  /**
   * Return the maximum time interval, in seconds, between client requests
   * before the servlet container will invalidate the session.  A negative
   * time indicates that the session should never time out.
   */
  @Override
  public int getMaxInactiveInterval() {
    triggerMaxInactiveIntervalLoad();
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
    triggerMaxInactiveIntervalStore(interval);
    super.setMaxInactiveInterval(interval);
  }

  protected void triggerMaxInactiveIntervalLoad() {
    triggerProperty("maxInactiveInterval", maxInactiveIntervalProperty);
  }

  protected void triggerMaxInactiveIntervalStore(Integer interval) {
    storeProperty("maxInactiveInterval", maxInactiveIntervalProperty, interval);
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
    triggerPrincipalLoad();
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
    if (principal != null) {
      if (!(principal instanceof Serializable)) {
        throw new IllegalArgumentException("Need a serializable Principal for Redis storage, but found " + principal.getClass());
      }
    }
    triggerPrincipalStore((Serializable) principal);
    super.setPrincipal(principal);
  }

  protected void triggerPrincipalLoad() {
    triggerProperty("principal", principalProperty);
  }

  protected void triggerPrincipalStore(Serializable principal) {
    storeProperty("principal", principalProperty, principal);
  }

  /**
   * Release all object references, and initialize instance variables, in
   * preparation for reuse of this object.
   */
  @Override
  public void recycle() {
    String oldId = getId();
    super.recycle();
    String newId = getId();
    if (newId == null || Objects.equals(oldId, newId)) {
      getManager().changeSessionId(this);
    }
    initProperties();
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
    triggerCreationTimeLoad();
    return super.getCreationTime();
  }

  /**
   * Set the creation time for this session.  This method is called by the
   * Manager when an existing Session instance is reused.
   *
   * @param time The new creation time
   */
  @Override
  public void setCreationTime(long time) {
    triggerCreationTimeStore(time);
    super.setCreationTime(time);
  }

  protected void triggerCreationTimeLoad() {
    creationTimeProperty.trigger();
  }

  /**
   * Return the time when this session was created, in milliseconds since
   * midnight, January 1, 1970 GMT, bypassing the session validation checks.
   */
  @Override
  public long getCreationTimeInternal() {
    triggerCreationTimeLoad();
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
    triggerAttributeLoad(name);
    return super.getAttribute(name);
  }

  protected void triggerAttributeLoad(String name) {
    attributesProperties.computeIfAbsent(name, this::makeAttributeProperty).trigger();
  }

  protected RedisHashBackedPropertySupport<Serializable> makeAttributeProperty(String name) {
    Redis redis = getRedis();
    String redisKey = getAttributesKey();
    String hashKey = name;
    RedisConverter<Serializable> converter = new SerializableConverter<>();
    Consumer<Serializable> setter = value -> this.attributes.put(name, value);
    return
        new RedisHashBackedPropertySupport<>(
                                                redis,
                                                redisKey, hashKey,
                                                converter,
                                                setter
        );
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
    Collection<String> attributeNames = fetchAttributeNames();
    return Collections.enumeration(attributeNames);
  }

  /**
   * Retrieve the attribute names from Redis.
   *
   * @return The attribute names; never {@code null}.
   */
  protected SortedSet<String> fetchAttributeNames() {
    try {
      Set<String> names = getRedis().withRedis(jedis -> {
        return jedis.hkeys(getAttributesKey());
      });
      return new TreeSet<>(names);
    } catch (Exception e) {
      log.error("Could not retrieve attribute names; using empty set", e);
      return Collections.emptySortedSet();
    }
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
    doRedisInvalidation();
  }

  protected void doRedisInvalidation() {
    getManager().invalidateSession(getIdInternal());
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
    doRemoveAttribute(name);
    super.removeAttribute(name);
  }

  protected void doRemoveAttribute(String name) {
    try {
      getRedis().withRedis(jedis -> {
        jedis.hdel(getAttributesKey(), name);
      });
      attributesProperties.remove(name);
    } catch (Exception e) {
      log.error("Could not remove attribute from " + getIdInternal() + " for name " + name, e);
    }
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
    doRemoveAttribute(name);
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
    if (value != null) {
      if (!(value instanceof Serializable)) {
        throw new IllegalArgumentException("Need a Serializable value, but found an instance of " + value.getClass());
      }
    }
    triggerAttributeStore(name, (Serializable) value);
    super.setAttribute(name, value);
  }

  protected void triggerAttributeStore(String name, Serializable value) {
    attributesProperties.computeIfAbsent(name, this::makeAttributeProperty).store(value);
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
    if (value != null) {
      if (!(value instanceof Serializable)) {
        throw new IllegalArgumentException("Need a Serializable value, but found an instance of " + value.getClass());
      }
    }
    triggerAttributeStore(name, (Serializable) value);
    super.setAttribute(name, value, notify);
  }

  /**
   * Return the names of all currently defined session attributes
   * as an array of Strings.  If there are no defined attributes, a
   * zero-length array is returned.
   */
  @Override
  protected String[] keys() {
    return fetchAttributeNames().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
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
    doRemoveAttribute(name);
    super.removeAttributeInternal(name, notify);
  }

  /**
   * Return the <code>isValid</code> flag for this session without any expiration
   * check.
   */
  @Override
  protected boolean isValidInternal() {
    triggerIsValidLoad();
    return super.isValidInternal();
  }

  protected void triggerIsValidLoad() {
    isValidProperty.trigger();
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
    return true;
  }

  protected void triggerIsValidStore(boolean isValid) {
    isValidProperty.store(isValid);
  }

  /**
   * Return the <code>isValid</code> flag for this session.
   */
  @Override
  public boolean isValid() {
    triggerIsValidLoad();
    return super.isValid();
  }

  /**
   * Set the <code>isValid</code> flag for this session.
   *
   * @param isValid The new value for the <code>isValid</code> flag
   */
  @Override
  public void setValid(boolean isValid) {
    triggerIsValidStore(isValid);
    super.setValid(isValid);
  }

  /**
   * Perform the internal processing required to invalidate this session,
   * without triggering an exception if the session has already expired.
   */
  @Override
  public void expire() {
    doRedisInvalidation();
    super.expire();
  }

  /**
   * Perform the internal processing required to invalidate this session,
   * without triggering an exception if the session has already expired.
   *
   * @param notify Should we notify listeners about the demise of
   *               this session?
   */
  @Override
  public void expire(boolean notify) {
    doRedisInvalidation();
    super.expire(notify);
  }

  @Override
  public void setId(String id) {
    super.setId(id);
    initProperties();
  }

  @Override
  public void setId(String id, boolean notify) {
    super.setId(id, notify);
    initProperties();
  }

}
