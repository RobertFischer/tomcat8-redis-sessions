package com.webonise.tomcat8.redisession.redisclient;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.function.*;

/**
 * A support class for when a property is backed by Redis. This can be thought of as a lazy thunk implementation for
 * a property. In your getter, call {@link #trigger()} to force the evaluation of property. If it has not been evaluated
 * before, it will be loaded. In your setter, call {@link #store(U)} to store the new value.
 */
public class RedisBackedPropertySupport<U> {

  private static final Log LOG = LogFactory.getLog(RedisBackedPropertySupport.class);

  protected final Redis client;
  protected final String redisKey;
  protected final Function<String, U> fromString;
  private final Consumer<U> setter;
  private final Function<U, String> toString;
  protected volatile Optional<U> fetchedValue = Optional.empty();

  public RedisBackedPropertySupport(Redis client, String redisKey, RedisConverter<? extends U> converter, Consumer<U> setter) {
    Objects.requireNonNull(client, "Redis client for working with the propery");
    this.client = client;

    Objects.requireNonNull(redisKey, "Key that this propery is based on");
    this.redisKey = redisKey;

    Objects.requireNonNull(converter, "Conversion object");
    this.fromString = converter::convertFromString;
    this.toString = converter::convertToString;

    Objects.requireNonNull(setter, "Setter function to assign value");
    this.setter = setter;
  }

  /**
   * Retrieves the value from Redis, performing the conversation as necessary.
   *
   * @return The value of the property, or {@code null} if it was not found in Redis.
   */
  public U fetch() throws Exception {
    final String result = client.withRedis(this::doFetch);
    return Optional.ofNullable(result).map(fromString).orElse(null);
  }

  /**
   * Implements the raw Redis call.
   *
   * @param jedis The jedis client; never {@code null}.
   * @return The return value from Redis, or {@code null} if it is not set in Redis.
   */
  protected String doFetch(Jedis jedis) throws Exception {
    return jedis.get(redisKey);
  }

  /**
   * Evaluates the property, if need be. In a multithreaded environment, this may be
   * called multiple times.
   */
  public void trigger() {
    synchronized (this) {
      if (fetchedValue.isPresent()) return;
      try {
        (fetchedValue = Optional.ofNullable(fetch())).ifPresent(setter);
      } catch (Exception e) {
        LOG.warn("Could not retrieve " + redisKey, e);
      }
    }
  }

  /**
   * Stores the value into Redis. If the {@code value} is {@code null}, then the key is deleted from Redis.
   * Otherwise, the
   *
   * @param value The value to persist into Redis; may be {@code null}
   */
  public void store(U value) {
    try {
      synchronized (this) {
        U oldValue = this.fetchedValue.orElse(null);
        String valueToStore = Optional.ofNullable(value).map(toString).orElse(null);
        if (valueToStore == null) {
          client.withRedis(this::doClear);
        } else if (!Objects.equals(oldValue, value)) {
          client.withRedis(jedis -> {
            doStore(jedis, valueToStore);
          });
        }
        fetchedValue = Optional.ofNullable(value);
        setter.accept(value);
      }
    } catch (Exception e) {
      LOG.warn("Could not store value to " + redisKey + " => " + value, e);
    }
  }

  /**
   * Clears the property's storage from Redis.
   *
   * @param jedis The client; never {@code null}
   */
  protected void doClear(Jedis jedis) throws Exception {
    jedis.del(redisKey);
  }

  /**
   * Stores the property into Redis.
   *
   * @param jedis The client; never {@code null}
   * @param value The value to store; never {@code null}
   */
  protected void doStore(Jedis jedis, String value) throws Exception {
    jedis.set(redisKey, value);
  }

}
