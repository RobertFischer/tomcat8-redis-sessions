package com.webonise.tomcat8.redisession.redisclient;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import redis.clients.jedis.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * The class responsible for communicating with Redis.
 */
public class Redis {

  /**
   * The interface to be implemented (probably as a lambda) to define what to do with Redis.
   *
   * @param <T> The type to return from the callback. Use {@link Void} when returning nothing.
   */
  public interface RedisFunction<T> {
    T apply(Jedis jedis) throws Exception;
  }

  /**
   * The interface to be implemented (probably by a lambda) to define what to do with Redis when you don't
   * get a result.
   */
  public interface RedisConsumer {
    void apply(Jedis jedis) throws Exception;
  }

  /**
   * The interface to be implemented (probably by a lambda) to define what to do with transaction results.
   *
   * @param <T> The type to return from the transactionr esults.
   */
  public interface TransactionResultHandler<T> {
    T apply(Jedis jedis, List<Object> transactionResults) throws Exception;
  }

  private static final Log log = LogFactory.getLog(Redis.class);

  public static final String DEFAULT_HOST = Protocol.DEFAULT_HOST;
  public static final int DEFAULT_PORT = Protocol.DEFAULT_PORT;
  public static final int DEFAULT_TIMEOUT = Math.max(Protocol.DEFAULT_TIMEOUT, (int) TimeUnit.SECONDS.toMillis(1L));

  private final JedisPool pool;

  public Redis() {
    this(DEFAULT_PORT);
  }

  public Redis(int redisPort) {
    this(DEFAULT_HOST, redisPort);
  }

  public Redis(String redisHost) {
    this(redisHost, DEFAULT_PORT);
  }

  public Redis(String redisHost, int redisPort) {
    this(redisHost, redisPort, DEFAULT_TIMEOUT);
  }

  public Redis(String redisHost, int redisPort, int redisTimeout) {
    Objects.requireNonNull(redisHost, "Host for Redis");
    if (redisHost.equals("")) throw new IllegalArgumentException("Host for Redis was empty");

    if (redisPort <= 0) throw new IllegalArgumentException("Invalid port: " + redisPort);

    if (redisTimeout < 0) {
      throw new IllegalArgumentException("Timeout must be nonnegative: " + redisTimeout);
    } else if (redisTimeout == 0) {
      log.warn("Redis timeout is 0, which is just asking for your application to hang.");
    }

    JedisPoolConfig config = new JedisPoolConfig();
    config.setBlockWhenExhausted(false);
    config.setFairness(false);
    config.setLifo(true);

    pool = new JedisPool(config, redisHost, redisPort, redisTimeout);
  }

  /**
   * Executes the callback and returns the result.
   *
   * @param callback The callback to execute; may not be {@code null}.
   * @param <T>      The type to return from the callback.
   * @return The return value of the callback.
   * @throws Exception If an exception occurs, either with Redis or within the callback.
   */
  public <T> T withRedis(RedisFunction<T> callback) throws Exception {
    Objects.requireNonNull(callback, "callback to execute with Redis");
    try (Jedis jedis = pool.getResource()) {
      return callback.apply(jedis);
    }
  }

  /**
   * Executes the callback and returns no result.
   *
   * @param callback The callback to execute; may not be {@code null}.
   * @throws Exception If an exception occurs, either with Redis or within the callback.
   */
  public void withRedis(RedisConsumer callback) throws Exception {
    Objects.requireNonNull(callback, "callback to execute with Redis");
    withRedis(jedis -> {
      callback.apply(jedis);
      return null;
    });
  }

  /**
   * Queues the callback to be executed as a {@link ForkJoinTask} using {@link ForkJoinPool#commonPool()}.
   *
   * @param callback The callback to execute; may not be {@code null}.
   * @param <T>      The type to return from the fork-join task.
   * @return The task; never {@code null}.
   */
  public <T> ForkJoinTask<T> withRedisAsync(RedisFunction<T> callback) {
    Objects.requireNonNull(callback, "callback to execute with Redis");
    return ForkJoinPool.commonPool().submit(() -> {
      return withRedis(callback);
    });
  }

  /**
   * Queues the callback to be executed as a {@link ForkJoinTask} using {@link ForkJoinPool#commonPool()}.
   *
   * @param callback The callback to execute; may not be {@code null}.
   * @return The task, which will always return {@code null}; never {@code null} itself.
   */
  public ForkJoinTask<Void> withRedisAsync(RedisConsumer callback) {
    Objects.requireNonNull(callback, "callback to execute with Redis");
    return withRedisAsync(jedis -> {
      callback.apply(jedis);
      return null;
    });
  }

  /**
   * Performs a Redis transaction and returns the result.
   *
   * @param callback      The callback to execute within a transaction; never {@code null}
   * @param resultHandler The handler for the various results from the various calls; never {@code null}
   * @return The return value from {@code resultHandler}
   */
  public <T> T withRedisTransaction(RedisConsumer callback, TransactionResultHandler<T> resultHandler) throws Exception {
    Objects.requireNonNull(callback, "callback to execute with Redis");
    Objects.requireNonNull(resultHandler, "the handler for the results of the transaction");
    return withRedis(toTransactionFunction(callback, resultHandler));
  }

  /**
   * Performs a redis transaction and ignores the result.
   *
   * @param callback The callback to execute within a transaction; never {@code null}
   * @throws Exception
   */
  public void withRedisTransaction(RedisConsumer callback) throws Exception {
    withRedisTransaction(callback, getDoNothingTransactionResultHandler());
  }

  /**
   * Queues a callback to be executed within a transaction as a {@link ForkJoinTask} using {@link ForkJoinPool#commonPool()}.
   *
   * @param callback The callback to execute; may not be {@code null}.
   * @return The task, which will always return {@code null}; never {@code null} itself.
   */
  public <T> ForkJoinTask<T> withRedisTransactionAsync(RedisConsumer callback, TransactionResultHandler<T> resultHandler) {
    return withRedisAsync(toTransactionFunction(callback, resultHandler));
  }


  /**
   * Queues a callback to be executed within a transaction as a {@link ForkJoinTask} using {@link ForkJoinPool#commonPool()},
   * and ignores the result.
   *
   * @param callback The callback to execute; may not be {@code null}.
   * @return The task, which will always return {@code null}; never {@code null} itself.
   */
  public ForkJoinTask<Void> withRedisTransactionAsync(RedisConsumer callback) {
    return withRedisTransactionAsync(callback, getDoNothingTransactionResultHandler());
  }

  /**
   * Performs a full scan.
   *
   * @param paramsFactory The producer for params; never {@code null}
   * @return The stream of results from scanning; never {@code null}.
   */
  public Stream<String> fullScan(Supplier<ScanParams> paramsFactory) throws Exception {
    Objects.requireNonNull(paramsFactory, "The factory for the ScanParams");
    ScanParams params = paramsFactory.get();
    ScanSpliterator spliterator = new ScanSpliterator(this, ScanSpliterator.STARTING_CURSOR, params);
    return StreamSupport.stream(spliterator, true);
  }

  /**
   * A {@link Redis.TransactionResultHandler} that does nothing except return {@code null}.
   */
  protected static TransactionResultHandler<Void> getDoNothingTransactionResultHandler() {
    return (jedis, list) -> {
      return null;
    };
  }

  /**
   * Generates the {@code RedisFunction} based on opening a transaction using {@code MULTI}, calling {@code callback},
   * executing {@code EXEC}, and then feeding the results into {@code TransactionResultHandler}. If an exception occurs,
   * the transaction is discarded and the exception is re-thrown.
   */
  protected static <T> RedisFunction<T> toTransactionFunction(RedisConsumer callback, TransactionResultHandler<T> resultHandler) {
    Objects.requireNonNull(callback, "callback to execute with Redis");
    Objects.requireNonNull(resultHandler, "the handler for the results of the transaction");
    return jedis -> {
      Transaction transaction = null;
      try {
        transaction = jedis.multi();
        List<Object> results = transaction.exec();
        transaction = null;
        return resultHandler.apply(jedis, results);
      } catch (Exception e) {
        if (transaction != null) transaction.discard();
        throw e;
      }
    };
  }


}
