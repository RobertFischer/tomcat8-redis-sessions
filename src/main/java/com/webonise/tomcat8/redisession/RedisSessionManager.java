package com.webonise.tomcat8.redisession;

import com.webonise.tomcat8.redisession.redisclient.Redis;
import org.apache.catalina.*;
import org.apache.catalina.util.StandardSessionIdGenerator;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import redis.clients.jedis.ScanParams;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Responsible for creating sessions that persist into Redis.
 */
public class RedisSessionManager implements Manager {

  private static final Log LOG = LogFactory.getLog(RedisSessionManager.class);
  private final AtomicLong sessionCounter = new AtomicLong(0L);
  private final PropertyChangeSupport changeListeners = new PropertyChangeSupport(this);
  private final Redis redis = new Redis(); // TODO Allow this to be populated from XML config
  private volatile Context context;
  private volatile boolean distributable = true;
  private volatile int getMaxInactiveInterval = (int) TimeUnit.HOURS.toSeconds(1L);
  private volatile SessionIdGenerator sessionIdGenerator = new StandardSessionIdGenerator();

  public Redis getRedis() {
    return redis;
  }

  /**
   * Return the Container with which this Manager is associated.
   *
   * @deprecated Use {@link #getContext()}. This method will be removed in
   * Tomcat 9 onwards.
   */
  @Override
  @Deprecated
  public Container getContainer() {
    return getContext();
  }

  /**
   * Set the Container with which this Manager is associated.
   *
   * @param container The newly associated Container
   * @deprecated Use {@link #setContext(Context)}. This method will be removed in
   * Tomcat 9 onwards.
   */
  @Override
  @Deprecated
  public void setContainer(Container container) {
    setContext((Context) container);
  }

  /**
   * Return the Context with which this Manager is associated.
   */
  @Override
  public Context getContext() {
    return this.context;
  }

  /**
   * Set the Container with which this Manager is associated.
   *
   * @param context The newly associated Context
   */
  @Override
  public void setContext(Context context) {
    Objects.requireNonNull(context, "context associated with the manager");
    this.context = context;
  }

  /**
   * Return the distributable flag for the sessions supported by
   * this Manager, which is {@code true} by default.
   */
  @Override
  public boolean getDistributable() {
    return this.distributable;
  }

  /**
   * Set the distributable flag for the sessions supported by this
   * Manager.  If this flag is set, all user data objects added to
   * sessions associated with this manager must implement Serializable.
   *
   * @param distributable The new distributable flag
   */
  @Override
  public void setDistributable(boolean distributable) {
    this.distributable = distributable;
  }

  /**
   * Return the default maximum inactive interval (in seconds)
   * for Sessions created by this Manager, which is 12 hours (in seconds) by default.
   */
  @Override
  public int getMaxInactiveInterval() {
    return this.getMaxInactiveInterval;
  }

  /**
   * Set the default maximum inactive interval (in seconds)
   * for Sessions created by this Manager.
   *
   * @param interval The new default value, which must be greater than 0.
   */
  @Override
  public void setMaxInactiveInterval(int interval) {
    if (interval <= 0) {
      throw new IllegalArgumentException("Interval must be greater than 0; was " + interval);
    }
    this.getMaxInactiveInterval = interval;
  }

  /**
   * return the session id generator
   */
  @Override
  public SessionIdGenerator getSessionIdGenerator() {
    return this.sessionIdGenerator;
  }

  /**
   * Sets the session id generator
   *
   * @param sessionIdGenerator The session id generator, which may not be {@code ull}
   */
  @Override
  public void setSessionIdGenerator(SessionIdGenerator sessionIdGenerator) {
    Objects.requireNonNull(sessionIdGenerator, "session ID generator");
    this.sessionIdGenerator = sessionIdGenerator;
  }

  /**
   * Gets the session id length (in bytes) of Sessions created by
   * this Manager.
   *
   * @return The session id length
   * @deprecated Use {@link SessionIdGenerator#getSessionIdLength()}.
   * This method will be removed in Tomcat 9 onwards.
   */
  @Override
  @Deprecated
  public int getSessionIdLength() {
    return sessionIdGenerator.getSessionIdLength();
  }

  /**
   * Sets the session id length (in bytes) for Sessions created by this
   * Manager.
   *
   * @param idLength The session id length
   * @deprecated Use {@link SessionIdGenerator#setSessionIdLength(int)}.
   * This method will be removed in Tomcat 9 onwards.
   */
  @Override
  @Deprecated
  public void setSessionIdLength(int idLength) {
    sessionIdGenerator.setSessionIdLength(idLength);
  }

  /**
   * Returns the total number of sessions created by this manager.
   *
   * @return Total number of sessions created by this manager.
   */
  @Override
  public long getSessionCounter() {
    return sessionCounter.get();
  }

  /**
   * Sets the total number of sessions created by this manager.
   *
   * @param sessionCounter Total number of sessions created by this manager, which must be nonnegative.
   */
  @Override
  public void setSessionCounter(long sessionCounter) {
    this.sessionCounter.set(sessionCounter);
  }

  /**
   * Gets the maximum number of sessions that have been active at the same
   * time.
   *
   * @return Maximum number of sessions that have been active at the same
   * time
   */
  @Override
  public int getMaxActive() {
    return fetchIntegerKey(Convention.SESSION_MAX_ACTIVE_KEY);
  }

  /**
   * (Re)sets the maximum number of sessions that have been active at the
   * same time.
   *
   * @param maxActive Maximum number of sessions that have been active at
   *                  the same time, which must be nonnegative.
   */
  @Override
  public void setMaxActive(int maxActive) {
    if (maxActive < 0) {
      throw new IllegalArgumentException("Maximum active sesions must be nonnegative");
    }
    storeIntegerKey(Convention.SESSION_MAX_ACTIVE_KEY, maxActive);
  }

  /**
   * Gets the number of currently active sessions.
   *
   * @return Number of currently active sessions
   */
  @Override
  public int getActiveSessions() {
    return fetchIntegerKey(Convention.ACTIVE_SESSIONS_COUNT_KEY);
  }

  public void setActiveSessions(int activeSessions) {
    redis.withRedisAsync(jedis -> {
      jedis.set(Convention.ACTIVE_SESSIONS_COUNT_KEY, Integer.toString(activeSessions));
    });
    int maxActive = getMaxActive();
    if (maxActive < activeSessions) setMaxActive(activeSessions);
  }

  /**
   * Cleans all the sessions as per {@link #cleanSession(String)}.
   */
  protected void cleanSessions() throws Exception {
    createAttributesKeyStream()
        .map(Convention::attributesKeyToSessionId)
        .forEach(sessionId -> this.cleanSession(sessionId));
  }

  /**
   * Cleans up the state of the session, invalidating it if its expiration has passed or it is otherwise nonsensical.
   *
   * @param sessionid The session id to check; never {@code null}
   * @return {@code true} if the session is still valid after cleaning; {@code false} if the session is now invalid
   * @throws Exception
   */
  protected boolean cleanSession(String sessionid) {
    try {
      Date expDate = getSessionExpirationDate(sessionid);

      // If the expiration date is in the past, expire it.
      if (expDate.before(new Date())) {
        invalidateSession(sessionid);
        return false;
      }

      return true;
    } catch (Exception e) {
      LOG.warn("Exception when checking for expiration; the session may not be invalidated", e);
      return true;
    }
  }

  protected void invalidateSession(String sessionid) {
    String metadataKey = Convention.sessionIdToMetadataKey(sessionid);
    try {
      redis.withRedis(jedis -> {
        // Ordering is significant: invalid requires expired time to be set, and deleting attributes requires invalid
        jedis.hset(metadataKey, Convention.EXPIRED_TIME_HKEY, Convention.stringFromDate(new Date()));
        jedis.hset(metadataKey, Convention.IS_VALID_HKEY, Boolean.FALSE.toString());
        jedis.del(Convention.sessionIdToAttributesKey(sessionid));
      });
    } catch (Exception e) {
      LOG.error("Could not invalidate session with id: " + sessionid, e);
    }
  }

  /**
   * Gets the number of sessions that have expired.
   *
   * @return Number of sessions that have expired
   */
  @Override
  public long getExpiredSessions() {
    return fetchLongKey(Convention.EXPIRED_SESSIONS_COUNT_KEY);
  }

  /**
   * Sets the number of sessions that have expired.
   *
   * @param expiredSessions Number of sessions that have expired
   */
  @Override
  public void setExpiredSessions(long expiredSessions) {
    storeLongKey(Convention.EXPIRED_SESSIONS_COUNT_KEY, expiredSessions);
  }

  protected long fetchLongKey(String key) {
    try {
      // See if we have a cached value
      String valueString = redis.withRedis(jedis -> {
        return jedis.get(key);
      });
      if (valueString == null || valueString.isEmpty()) return 0L;
      return Long.parseLong(valueString);
    } catch (Exception e) {
      LOG.error("Error retrieving long key " + key + "; returning 0", e);
      return 0L;
    }
  }

  protected void storeLongKey(String key, long value) {
    try {
      redis.withRedis(jedis -> {
        jedis.set(key, Long.toString(value));
      });
    } catch (Exception e) {
      LOG.error("Error storing long value into key " + key + "; nothing set", e);
    }
  }

  /**
   * Gets the number of sessions that were not created because the maximum
   * number of active sessions was reached.
   *
   * @return Number of rejected sessions
   */
  @Override
  public int getRejectedSessions() {
    // We don't do this
    return 0;
  }

  /**
   * Gets the longest time (in seconds) that an expired session had been
   * alive.
   *
   * @return Longest time (in seconds) that an expired session had been
   * alive.
   */
  @Override
  public int getSessionMaxAliveTime() {
    return fetchIntegerKey(Convention.SESSION_MAX_ALIVE_TIME_KEY);
  }

  /**
   * Sets the longest time (in seconds) that an expired session had been
   * alive.
   *
   * @param sessionMaxAliveTime Longest time (in seconds) that an expired
   *                            session had been alive.
   */
  @Override
  public void setSessionMaxAliveTime(int sessionMaxAliveTime) {
    storeIntegerKey(Convention.SESSION_MAX_ALIVE_TIME_KEY, sessionMaxAliveTime);
  }

  /**
   * Provides how long the session was alive for, assuming it was expired.
   *
   * @param sessionId The session to check; never {@code null}
   * @return How long the session was alive for (in milliseconds); may be {@code null} if  it could not be determined.
   */
  protected Long getSessionAliveTime(String sessionId) {
    if (sessionId == null || sessionId.isEmpty()) return null;
    try {
      String metadataKey = Convention.metadataKeyToSessionId(sessionId);

      List<String> results = redis.withRedis(jedis -> {
        return jedis.hmget(metadataKey, Convention.CREATION_TIME_HKEY, Convention.EXPIRED_TIME_HKEY);
      });
      String creationTimeString = results.get(0);
      String expiredTimeString = results.get(1);

      if (creationTimeString == null || expiredTimeString == null) return null;
      if (creationTimeString.isEmpty() || expiredTimeString.isEmpty()) return null;

      Date creationTime = Convention.dateFromString(creationTimeString);
      Date expiredTime = Convention.dateFromString(expiredTimeString);
      return expiredTime.getTime() - creationTime.getTime();
    } catch (Exception e) {
      LOG.info("Could not determine the session alive time for " + sessionId, e);
      return null;
    }
  }

  /**
   * Gets the average time (in seconds) that expired sessions had been
   * alive. This may be based on sample data.
   *
   * @return Average time (in seconds) that expired sessions had been
   * alive.
   */
  @Override
  public int getSessionAverageAliveTime() {
    return fetchIntegerKey(Convention.SESSION_AVERAGE_ALIVE_TIME_KEY);
  }

  protected int fetchIntegerKey(String key) {
    try {
      String valueString = redis.withRedis(jedis -> {
        return jedis.get(key);
      });
      if (valueString == null || valueString.isEmpty()) return 0;
      return Integer.parseInt(valueString);
    } catch (Exception e) {
      LOG.error("Error when trying to retrieve key " + key + " from Redis; returning 0", e);
      return 0;
    }
  }

  protected void storeIntegerKey(String key, int value) {
    try {
      redis.withRedis(jedis -> {
        jedis.set(key, Integer.toString(value));
      });
    } catch (Exception e) {
      LOG.error("Error when trying to store the key " + key + " with value " + value + " in Redis; not updated", e);
    }
  }

  /**
   * Gets the current rate of session creation (in session per minute). This
   * may be based on sample data.
   *
   * @return The current rate (in sessions per minute) of session creation
   */
  @Override
  public int getSessionCreateRate() {
    return fetchIntegerKey(Convention.SESSION_CREATE_RATE_KEY);
  }

  public void setSessionCreateRate(int sessionsPerMinute) {
    storeIntegerKey(Convention.SESSION_CREATE_RATE_KEY, sessionsPerMinute);
  }

  protected int calculateSessionCreateRate() throws Exception {
    return
        createSessionIdStream()
            .map(this::getSessionCreateDate).filter(it -> it != null)
            .map(SessionRateData::new)
            .reduce(SessionRateData::merge)
            .map(SessionRateData::getSessionsPerMinute).orElse(0);
  }

  private Date getSessionCreateDate(String sessionId) {
    try {
      String metadataKey = Convention.sessionIdToMetadataKey(sessionId);
      String createDateString = redis.withRedis(jedis -> {
        return jedis.hget(metadataKey, Convention.CREATION_TIME_HKEY);
      });
      if (createDateString == null || createDateString.isEmpty()) return new Date();
      return Convention.dateFromString(createDateString);
    } catch (Exception e) {
      LOG.warn("Could not get the create date for sessionId " + sessionId + "; defaulting to now", e);
      return new Date();
    }
  }

  private Stream<String> createSessionIdStream() throws Exception {
    return createMetadataKeyStream().map(Convention::metadataKeyToSessionId);
  }

  /**
   * Gets the current rate of session expiration (in session per minute). This
   * may be based on sample data
   *
   * @return The current rate (in sessions per minute) of session expiration
   */
  @Override
  public int getSessionExpireRate() {
    return fetchIntegerKey(Convention.SESSION_EXPIRE_RATE_KEY);
  }

  public void setSessionExpireRate(int rate) {
    storeIntegerKey(Convention.SESSION_EXPIRE_RATE_KEY, rate);
  }

  public int calculateSessionExpireRate() throws Exception {
    return createExpiredSessionIdStream()
               .map(this::getSessionExpirationDate).filter(it -> it != null)
               .map(SessionRateData::new)
               .reduce(SessionRateData::merge)
               .map(SessionRateData::getSessionsPerMinute).orElse(0);
  }

  /**
   * Add this Session to the set of active Sessions for this Manager.
   *
   * @param session Session to be added
   */
  @Override
  public void add(Session session) {
    if (session == null || session instanceof RedisSession) return;
    throw new IllegalArgumentException("This manager only supports RedisSession");
    // TODO Maybe clone the session into Redis?
  }

  /**
   * Add a property change listener to this component.
   *
   * @param listener The listener to add
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.changeListeners.addPropertyChangeListener(listener);
  }

  /**
   * Change the session ID of the current session to a new randomly generated
   * session ID.
   *
   * @param session The session to change the session ID for
   */
  @Override
  public void changeSessionId(Session session) {
    Objects.requireNonNull(sessionIdGenerator, "the means of generating the session");
    String newId = sessionIdGenerator.generateSessionId();
    this.changeSessionId(session, newId);
  }

  /**
   * Change the session ID of the current session to a specified session ID.
   *
   * @param session The session to change the session ID for
   * @param newId   new session ID
   */
  @Override
  public void changeSessionId(Session session, String newId) {
    Objects.requireNonNull(session, "session to change the id");
    Objects.requireNonNull(redis, "the Redis client");

    String oldId = session.getId();
    Map<String, String> keyMapping = Convention.getChangeSessionIdMapping(oldId, newId);

    try {
      redis.withRedis(jedis -> {
        keyMapping.entrySet().forEach(entry -> {
          jedis.rename(entry.getKey(), entry.getValue());
        });
      });
    } catch (Exception e) {
      throw new RuntimeException("Could not change session id for " + oldId + " to " + newId, e);
    }

  }

  /**
   * Get a session from the recycled ones or create a new empty one.
   * The PersistentManager manager does not need to create session data
   * because it reads it from the Store.
   */
  @Override
  public RedisSession createEmptySession() {
    return createSession(null);
  }

  /**
   * Construct and return a new session object, based on the default
   * settings specified by this Manager's properties.  The session
   * id specified will be used as the session id.
   * If a new session cannot be created for any reason, return
   * <code>null</code>.
   *
   * @param sessionId The session id which should be used to create the
   *                  new session; if <code>null</code>, the session
   *                  id will be assigned by this method, and available via the getId()
   *                  method of the returned session.
   * @throws IllegalStateException if a new session cannot be
   *                               instantiated for any reason
   */
  @Override
  public RedisSession createSession(String sessionId) {
    if (sessionId == null) {
      Objects.requireNonNull(this.sessionIdGenerator, "session id generator");
      sessionId = this.sessionIdGenerator.generateSessionId();
    }
    return new RedisSession(this, sessionId);
  }

  /**
   * Return the active Session, associated with this Manager, with the
   * specified session id (if any); otherwise return <code>null</code>.
   *
   * @param id The session id for the session to be returned
   * @throws IllegalStateException if a new session cannot be
   *                               instantiated for any reason
   * @throws IOException           if an input/output error occurs while
   *                               processing this request
   */
  @Override
  public RedisSession findSession(String id) throws IOException {
    try {
      boolean found = redis.withRedis(jedis -> {
        return jedis.exists(Convention.sessionIdToMetadataKey(id));
      });
      if (!found) return null;
      RedisSession session = new RedisSession(this, id);
      boolean isValid = session.isValidInternal();
      if (!isValid) return null;
      return session;
    } catch (Exception e) {
      LOG.error("Could not retrieve session for id " + id, e);
      return null;
    }
  }

  /**
   * Return the set of active Sessions associated with this Manager.
   * If this Manager has no active Sessions, a zero-length array is returned.
   */
  @Override
  public Session[] findSessions() {
    try {
      Function<String, Session> sessionFunction = sessionId -> {
        try {
          return this.findSession(sessionId);
        } catch (Exception e) {
          LOG.error("Could not retrieve session for id " + sessionId, e);
          return null;
        }
      };
      return createValidSessionIdStream().map(sessionFunction).filter(it -> it != null).toArray(i -> new Session[i]);
    } catch (Exception e) {
      LOG.error("Could not retrieve sessions; returning an empty array as per the API", e);
      return new Session[0];
    }
  }


  /**
   * Load any currently active sessions that were previously unloaded
   * to the appropriate persistence mechanism, if any.  If persistence is not
   * supported, this method returns without doing anything.
   *
   * @throws ClassNotFoundException if a serialized class cannot be
   *                                found during the reload
   * @throws IOException            if an input/output error occurs
   */
  @Override
  public void load() throws ClassNotFoundException, IOException {
    // DO NOTHING
  }

  /**
   * Remove this Session from the active Sessions for this Manager.
   *
   * @param session Session to be removed
   */
  @Override
  public void remove(Session session) {
    remove(session, false);
  }

  /**
   * Remove this Session from the active Sessions for this Manager.
   *
   * @param session Session to be removed
   * @param update  Should the expiration statistics be updated
   */
  @Override
  public void remove(Session session, boolean update) {
    Objects.requireNonNull(session, "session to remove");
    String sessionId = session.getId();
    Objects.requireNonNull(sessionId, "id of session to remove");
    invalidateSession(sessionId);

    if (update) {
      try {
        setSessionExpireRate(calculateSessionExpireRate());
        setExpiredSessions(countCurrentExpiredSessions());
      } catch (Exception e) {
        throw new RuntimeException("Could not update the expiration statistics", e);
      }
    }
  }

  /**
   * Remove a property change listener from this component.
   *
   * @param listener The listener to remove
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.changeListeners.removePropertyChangeListener(listener);
  }

  /**
   * Save any currently active sessions in the appropriate persistence
   * mechanism, if any.  If persistence is not supported, this method
   * returns without doing anything.
   *
   * @throws IOException if an input/output error occurs
   */
  @Override
  public void unload() throws IOException {
    // DO NOTHING
  }

  /**
   * This method will be invoked by the context/container on a periodic
   * basis and allows the manager to implement
   * a method that executes periodic tasks, such as expiring sessions etc.
   */
  @Override
  public void backgroundProcess() {
    doInBackground(this::cleanSessions);
    doInBackground(() -> {
      setActiveSessions(countCurrentActiveSessions());
    });
    doInBackground(() -> {
      setExpiredSessions(countCurrentExpiredSessions());
    });
    doInBackground(() -> {
      setSessionMaxAliveTime(calculateSessionMaxAliveTime());
    });
    doInBackground(() -> {
      setSessionAverageAliveTime(calculateSessionAverageAliveTime());
    });
    doInBackground(() -> {
      setSessionCreateRate(calculateSessionCreateRate());
    });
    doInBackground(() -> {
      setSessionExpireRate(calculateSessionExpireRate());
    });
  }

  /**
   * Returns the session average alive time in seconds.
   *
   * @return The average time that a session has been alive, in seconds; or 0 if that cannot be calculated.
   */
  protected int calculateSessionAverageAliveTime() {
    try {
      Collector<String, ?, Double> averager = Collectors.averagingLong(this::getSessionAliveTime);
      Double average = createExpiredSessionIdStream().collect(averager);
      if (average == null) return 0;
      return Double.valueOf(average / 1000).intValue();
    } catch (Exception e) {
      LOG.error("Could not calculate the average session alive time; defaulting to 0", e);
      return 0;
    }
  }

  protected int setSessionAverageAliveTime(int averageAliveTimeSeconds) {
    if (averageAliveTimeSeconds < 0) {
      throw new IllegalArgumentException("Average alive time must be nonnegative; was " + averageAliveTimeSeconds);
    }
    try {
      redis.withRedis(jedis -> {
        jedis.set(Convention.SESSION_AVERAGE_ALIVE_TIME_KEY, Integer.toString(averageAliveTimeSeconds));
      });
    } catch (Exception e) {
      LOG.error("Could not set the average session alive time in Redis", e);
    }
    return getSessionAverageAliveTime();
  }


  protected int calculateSessionMaxAliveTime() {
    try {
      Optional<Long> result =
          createMetadataKeyStream()
              .map(this::getSessionAliveTime)
              .max(Comparator.naturalOrder());
      Long maxAliveTime = result.orElse(null);
      if (maxAliveTime == null) return 0;
      maxAliveTime = TimeUnit.MILLISECONDS.toSeconds(maxAliveTime);
      return (int) Math.min(Integer.MAX_VALUE, maxAliveTime);
    } catch (Exception e) {
      LOG.error("Error determining the session max alive time; defaulting to 0", e);
      return 0;
    }
  }

  protected void doInBackground(Procedure action) {
    ForkJoinPool.commonPool().submit(() -> {
      action.apply();
      return null;
    });
  }

  protected void doInBackground(Supplier<?> action) {
    ForkJoinPool.commonPool().submit(action::get);
  }

  protected long countCurrentExpiredSessions() {
    try {
      return
          createMetadataKeyStream()
              .map(Convention::metadataKeyToSessionId)
              .filter(sessionId -> !getSessionValidity(sessionId))
              .distinct().count();
    } catch (Exception e) {
      LOG.error("Error counting the current expired sessions; returning 0", e);
      return 0L;
    }
  }

  /**
   * Given a session id, provides the expiration date for that session.
   *
   * @param sessionId The session whose expiry is desired; never {@code null}.
   * @return The expiration date for that session, if known; otherwise, {@code new Date(0)}.
   */
  protected Date getSessionExpirationDate(String sessionId) {
    Objects.requireNonNull(sessionId, "session id whose expiration date is desired");
    Date defaultDate = new Date(0L);
    String metadataKey = Convention.sessionIdToMetadataKey(sessionId);
    try {
      List<String> results = redis.withRedis(jedis -> {
        return jedis.hmget(metadataKey, Convention.LAST_ACCESS_TIME_HKEY, Convention.MAX_INACTIVE_INTERVAL_HKEY);
      });
      String lastAccessTimeString = results.get(0);
      if (lastAccessTimeString == null || lastAccessTimeString.isEmpty()) {
        LOG.info("No last access time in Redis for " + sessionId + "; returning " + defaultDate);
        return defaultDate;
      }
      Date lastAccessTime = Convention.dateFromString(lastAccessTimeString);

      String maxInactiveIntervalString = results.get(1);
      if (maxInactiveIntervalString == null || maxInactiveIntervalString.isEmpty()) {
        LOG.info("No max inactive inveral in Redis for " + sessionId + "; returning " + defaultDate);
        return defaultDate;
      }
      long maxInactiveInterval = Long.parseLong(maxInactiveIntervalString);

      return new Date(lastAccessTime.getTime() + maxInactiveInterval);
    } catch (Exception e) {
      LOG.error("Error while determining the session expiration date for " + sessionId + "; returning " + defaultDate, e);
      return defaultDate;
    }
  }

  protected boolean getSessionValidity(String sessionId) {
    Objects.requireNonNull(sessionId, "session id whose expiration date is desired");
    String metadataKey = Convention.sessionIdToMetadataKey(sessionId);
    try {
      String validityString = redis.withRedis(jedis -> {
        return jedis.hget(metadataKey, Convention.IS_VALID_HKEY);
      });
      if (validityString == null || validityString.isEmpty()) return false;
      return Boolean.valueOf(validityString);
    } catch (Exception e) {
      LOG.error("Error while determining session validity for " + sessionId +
                    "; returning invalid", e
      );
      return false;
    }

  }

  /**
   * Provides a stream of the metadata keys (some of which may be duplicated).
   *
   * @return The stream of metadata keys.
   */
  protected Stream<String> createMetadataKeyStream() throws Exception {
    return createKeyScanStream(Convention.METADATA_KEY_PATTERN);
  }

  /**
   * Provides a stream of attribute keys (some of which may be duplicated).
   *
   * @return The stream of attribute keys.
   */
  protected Stream<String> createAttributesKeyStream() throws Exception {
    return createKeyScanStream(Convention.ATTRIBUTES_KEY_PATTERN);
  }

  /**
   * Creates a key scan stream that matches keys with the given pattern.
   *
   * @param pattern The Redis pattern to match.
   */
  protected Stream<String> createKeyScanStream(String pattern) throws Exception {
    return redis.fullScan(() -> {
      ScanParams param = new ScanParams();
      param.match(pattern);
      return param;
    }).parallel();
  }

  protected Stream<String> createValidSessionIdStream() throws Exception {
    cleanSessions();
    return createSessionIdStreamWithValidityFilter(true);
  }

  protected Stream<String> createExpiredSessionIdStream() throws Exception {
    return createSessionIdStreamWithValidityFilter(false);
  }

  protected Stream<String> createSessionIdStreamWithValidityFilter(boolean valid) throws Exception {
    Predicate<String> validityTest = this::getSessionValidity;
    if (!valid) validityTest = validityTest.negate();
    return createSessionIdStream().filter(validityTest);
  }

  protected int countCurrentActiveSessions() {
    try {
      long count = createValidSessionIdStream().distinct().count();
      return (int) Math.min(Integer.MAX_VALUE, count);
    } catch (Exception e) {
      LOG.error("Could not calculate max active; returning 0", e);
      return 0;
    }
  }

  private interface Procedure {
    void apply() throws Exception;
  }

  private static class SessionRateData {
    private final long earliestCreateMoment;
    private final long sessionCount;

    public SessionRateData(Date earliestCreateTime) {
      this(earliestCreateTime.getTime(), 1L);
    }

    public SessionRateData(long earliestCreateMoment, long sessionCount) {
      this.earliestCreateMoment = earliestCreateMoment;
      this.sessionCount = sessionCount;
    }

    public static SessionRateData merge(SessionRateData left, SessionRateData right) {
      final long newEarliestMoment = Math.min(left.earliestCreateMoment, right.earliestCreateMoment);
      final long newSessionCount = left.sessionCount + right.sessionCount;
      return new SessionRateData(newEarliestMoment, newSessionCount);
    }

    public int getSessionsPerMinute() {
      long count = sessionCount;
      if (count == 0) return 0;
      long now = System.currentTimeMillis();
      long earliest = earliestCreateMoment;
      if (earliest >= now) return 0;

      double rate = (count * 1.0) / (now - earliest);
      rate = rate / (1000 * 60); // Convert from millis to minutes
      return Double.valueOf(rate).intValue();
    }

  }
}
