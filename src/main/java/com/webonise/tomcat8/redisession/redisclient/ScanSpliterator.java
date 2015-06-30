package com.webonise.tomcat8.redisession.redisclient;

import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * An iterator over a {@code SCAN} starting at the given cursor.
 */
public class ScanSpliterator implements Spliterator<String> {

  /**
   * The initial cursor to perofrm a scan.
   */
  public static final String STARTING_CURSOR = "0";

  private volatile ForkJoinTask<ScanSpliterator> next; // NB: synchronize(this) if you touch this.next
  private final String[] results;
  private final AtomicInteger idx = new AtomicInteger(0);

  /**
   * Construct the iterator to start at the cursor with no match results.
   *
   * @param redisClient The client to use; may not be {@code null}.
   * @param cursor      The cursor to start at; may not be {@code null}.
   */
  public ScanSpliterator(Redis redisClient, String cursor) throws Exception {
    this(redisClient, cursor, null);
  }

  /**
   * Construct the iterator to start at the cursor with no match results.
   *
   * @param redisClient The client to use; may not be {@code null}.
   * @param cursor      The cursor to start at; may not be {@code null}.
   * @param params      The parameters; {@code null} means no parameters.
   */
  public ScanSpliterator(Redis redisClient, String cursor, ScanParams params) throws Exception {
    Objects.requireNonNull(redisClient, "Client for Redis");
    Objects.requireNonNull(cursor, "The cursor to start from (you probably meanto use STARTING_CURSOR)");

    ScanResult<String> scanResult = redisClient.withRedis(jedis -> {
      if (params == null) {
        return jedis.scan(cursor);
      } else {
        return jedis.scan(cursor, params);
      }
    });

    results = scanResult.getResult().toArray(new String[scanResult.getResult().size()]);
    if (scanResult.getStringCursor().equals("0")) {
      this.next = null;
    } else {
      this.next = ForkJoinPool.commonPool().submit(() -> {
        return new ScanSpliterator(redisClient, scanResult.getStringCursor(), params);
      });
    }
  }

  /**
   * If a remaining element exists, performs the given action on it,
   * returning {@code true}; else returns {@code false}.  If this
   * Spliterator is {@link #ORDERED} the action is performed on the
   * next element in encounter order.  Exceptions thrown by the
   * action are relayed to the caller.
   *
   * @param action The action
   * @return {@code false} if no remaining elements existed
   * upon entry to this method, else {@code true}.
   * @throws NullPointerException if the specified action is null
   */
  @Override
  public boolean tryAdvance(Consumer<? super String> action) {
    if (action == null) throw new NullPointerException("action to perform is null");

    int myIdx = idx.getAndIncrement();
    if (myIdx < results.length) {
      action.accept(results[myIdx]);
      return true;
    }

    synchronized (this) {
      if (next != null) {
        return next.join().tryAdvance(action);
      }
    }

    return false;
  }

  /**
   * If this spliterator can be partitioned, returns a Spliterator
   * covering elements, that will, upon return from this method, not
   * be covered by this Spliterator.
   * <p>
   * <p>If this Spliterator is {@link #ORDERED}, the returned Spliterator
   * must cover a strict prefix of the elements.
   * <p>
   * <p>Unless this Spliterator covers an infinite number of elements,
   * repeated calls to {@code trySplit()} must eventually return {@code null}.
   * Upon non-null return:
   * <ul>
   * <li>the value reported for {@code estimateSize()} before splitting,
   * must, after splitting, be greater than or equal to {@code estimateSize()}
   * for this and the returned Spliterator; and</li>
   * <li>if this Spliterator is {@code SUBSIZED}, then {@code estimateSize()}
   * for this spliterator before splitting must be equal to the sum of
   * {@code estimateSize()} for this and the returned Spliterator after
   * splitting.</li>
   * </ul>
   * <p>
   * <p>This method may return {@code null} for any reason,
   * including emptiness, inability to split after traversal has
   * commenced, data structure constraints, and efficiency
   * considerations.
   *
   * @return a {@code Spliterator} covering some portion of the
   * elements, or {@code null} if this spliterator cannot be split
   * @apiNote An ideal {@code trySplit} method efficiently (without
   * traversal) divides its elements exactly in half, allowing
   * balanced parallel computation.  Many departures from this ideal
   * remain highly effective; for example, only approximately
   * splitting an approximately balanced tree, or for a tree in
   * which leaf nodes may contain either one or two elements,
   * failing to further split these nodes.  However, large
   * deviations in balance and/or overly inefficient {@code
   * trySplit} mechanics typically result in poor parallel
   * performance.
   */
  @Override
  public Spliterator<String> trySplit() {
    synchronized (this) {
      // If we have no next element readily on hand, then just don't do the split.
      if (this.next == null) return null;

      // Get a handle on the next spliterator: this will block until it resolves
      ScanSpliterator nextSpliterator = this.next.join();

      // For something like balance, try to advance 50% of the times when we can
      // (This may involve additional blocking...)
      if (nextSpliterator.next != null && ThreadLocalRandom.current().nextBoolean()) {
        Spliterator<String> toReturn = nextSpliterator.trySplit();
        if (toReturn != null) return toReturn;
      }

      // We either can't or don't want to advance; use our next value as a spliterator
      this.next = null;
      return nextSpliterator;
    }
  }

  /**
   * Returns an estimate of the number of elements that would be
   * encountered by a {@link #forEachRemaining} traversal, or returns {@link
   * Long#MAX_VALUE} if infinite, unknown, or too expensive to compute.
   * <p>
   * <p>If this Spliterator is {@link #SIZED} and has not yet been partially
   * traversed or split, or this Spliterator is {@link #SUBSIZED} and has
   * not yet been partially traversed, this estimate must be an accurate
   * count of elements that would be encountered by a complete traversal.
   * Otherwise, this estimate may be arbitrarily inaccurate, but must decrease
   * as specified across invocations of {@link #trySplit}.
   *
   * @return the estimated size, or {@code Long.MAX_VALUE} if infinite,
   * unknown, or too expensive to compute.
   * @apiNote Even an inexact estimate is often useful and inexpensive to compute.
   * For example, a sub-spliterator of an approximately balanced binary tree
   * may return a value that estimates the number of elements to be half of
   * that of its parent; if the root Spliterator does not maintain an
   * accurate count, it could estimate size to be the power of two
   * corresponding to its maximum depth.
   */
  @Override
  public long estimateSize() {
    if (this.next == null) return getCurrentSize();
    if (!this.next.isCompletedNormally()) return (this.getCurrentSize() + 1) * 2;
    return ((long) this.getCurrentSize()) + this.next.join().getCurrentSize();
  }

  /**
   * Gets the current size for this specific spliterator, not including any subsequent spliterators.
   *
   * @return The current size for this specific spliterator.
   */
  protected int getCurrentSize() {
    return Math.max(0, this.idx.get() - this.results.length + 1);
  }

  /**
   * Returns a set of characteristics of this Spliterator and its
   * elements. The result is represented as ORed values from {@link
   * #ORDERED}, {@link #DISTINCT}, {@link #SORTED}, {@link #SIZED},
   * {@link #NONNULL}, {@link #IMMUTABLE}, {@link #CONCURRENT},
   * {@link #SUBSIZED}.  Repeated calls to {@code characteristics()} on
   * a given spliterator, prior to or in-between calls to {@code trySplit},
   * should always return the same result.
   * <p>
   * <p>If a Spliterator reports an inconsistent set of
   * characteristics (either those returned from a single invocation
   * or across multiple invocations), no guarantees can be made
   * about any computation using this Spliterator.
   *
   * @return a representation of characteristics
   * @apiNote The characteristics of a given spliterator before splitting
   * may differ from the characteristics after splitting.  For specific
   * examples see the characteristic values {@link #SIZED}, {@link #SUBSIZED}
   * and {@link #CONCURRENT}.
   */
  @Override
  public int characteristics() {
    return Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.CONCURRENT;
  }
}
