package com.webonise.tomcat8.redisession;

import com.webonise.tomcat8.redisession.redisclient.*;
import org.apache.catalina.Session;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.io.Serializable;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.*;

/**
 * The Redis-backed {@link Session} implementation.
 */
public class EstablishedRedisSession extends RedisSession {

  private static final Log LOG = LogFactory.getLog(EstablishedRedisSession.class);



  public EstablishedRedisSession(RedisSessionManager manager, String id) {
    super(manager);
    Objects.requireNonNull(manager, "manager responsible for this session");
    Objects.requireNonNull(id, "id of the established Redis session");
    this.id = id;

  }

  @Override
  protected void triggerAuthTypeLoad() {
    authProperty.trigger();
  }

  @Override
  protected void triggerAuthTypeStore(String authType) {
    authProperty.store(authType);
  }

  @Override
  protected void triggerCreationTimeStore(Long time) {
    creationTimeProperty.store(time);
  }

  @Override
  protected void triggerThisAccessedTimeLoad() {
    thisAccessedTimeProperty.trigger();
  }

  @Override
  protected void triggerLastAccessedTimeLoad() {
    lastAccessedTimeProperty.trigger();
  }

  @Override
  protected void triggerMaxInactiveIntervalLoad() {
    maxInactiveIntervalProperty.trigger();
  }

  @Override
  protected void triggerMaxInactiveIntervalStore(Integer interval) {
    maxInactiveIntervalProperty.store(interval);
  }

  @Override
  protected void triggerPrincipalLoad() {
    principalProperty.trigger();
  }

  @Override
  protected void triggerPrincipalStore(Principal principal) {
    if (!(principal instanceof Serializable)) {
      throw new IllegalArgumentException("Need a principal that is Serializable; received " + principal);
    }
    principalProperty.store((Serializable) principal);
  }

  @Override
  protected void triggerCreationTimeLoad() {
    creationTimeProperty.trigger();
  }

  private final ConcurrentMap<String, RedisHashBackedPropertySupport<Serializable>> values = new ConcurrentHashMap<>();

  @Override
  protected void triggerAttributeLoad(String name) {
    values.computeIfAbsent(name,
                              hkey ->
                                  new RedisHashBackedPropertySupport<>(
                                                                          this.getRedis(),
                                                                          this.attributesKey, hkey,
                                                                          new SerializableConverter<>(),
                                                                          it -> this.attributes.put(name, it))
    ).trigger();
  }

  /**
   * Retrieve the attribute names from Redis.
   *
   * @return The attribute names; never {@code null}.
   */
  @Override
  protected SortedSet<String> fetchAttributeNames() {
    try {
      Set<String> keys = getRedis().withRedis(jedis -> {
        return jedis.hkeys(this.attributesKey);
      });
      if (keys == null || keys.isEmpty()) return Collections.emptySortedSet();
      return new TreeSet<>(keys);
    } catch (Exception e) {
      LOG.warn("Could not fetch attibute names", e);
      return Collections.emptySortedSet();
    }
  }

  @Override
  protected void doRedisInvalidation() {
    getManager().invalidateSession(this.getId());
  }

  @Override
  protected void doRemoveAttribute(String name) {

  }

  @Override
  protected void triggerAttributeStore(String name, Serializable value) {

  }

  @Override
  protected void triggerIsValidLoad() {

  }

  @Override
  protected void doChangeSessionId(String oldId, String newId) {

  }

  @Override
  protected void triggerIsValidStore(boolean isValid) {

  }
}
