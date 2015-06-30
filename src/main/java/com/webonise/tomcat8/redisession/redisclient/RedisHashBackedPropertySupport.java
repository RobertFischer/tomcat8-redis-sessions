package com.webonise.tomcat8.redisession.redisclient;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.function.*;

/**
 * A support class for when a property is backed by Redis within a hash. This can be thought of as a lazy thunk implementation for
 * a property. In your getter, call {@link #trigger()} to force the evaluation of property. If it has not been evaluated
 * before, it will be loaded. In your setter, call {@link #store(U)} to store the new value.
 */
public class RedisHashBackedPropertySupport<U> extends RedisBackedPropertySupport<U> {

  private static final Log LOG = LogFactory.getLog(RedisHashBackedPropertySupport.class);
  private final String hashKey;

  public RedisHashBackedPropertySupport(Redis client, String redisKey, String hashKey, RedisConverter<U> converter, Consumer<U> setter) {
    super(client, redisKey, converter, setter);

    Objects.requireNonNull(hashKey, "key within the hash in Redis");
    this.hashKey = hashKey;
  }

  /**
   * Implements the raw Redis call.
   *
   * @param jedis The jedis client; never {@code null}.
   * @return The return value from Redis, or {@code null} if it is not set in Redis.
   */
  @Override
  protected String doFetch(Jedis jedis) throws Exception {
    return jedis.hget(redisKey, hashKey);
  }

  /**
   * Clears the property's storage from Redis.
   *
   * @param jedis The client; never {@code null}
   */
  @Override
  protected void doClear(Jedis jedis) throws Exception {
    jedis.hdel(redisKey, hashKey);
  }

  /**
   * Stores the property into Redis.
   *
   * @param jedis The client; never {@code null}
   * @param value The value to store; never {@code null}
   */
  @Override
  protected void doStore(Jedis jedis, String value) throws Exception {
    jedis.hset(redisKey, hashKey, value);
  }
}
