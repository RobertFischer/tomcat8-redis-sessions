package com.webonise.tomcat8.redisession.redisclient;

/**
 * Defines how to convert a long value to and from Strings.
 */
public class LongConverter implements RedisConverter<Long> {
  /**
   * Defines how to convert a value to a string.
   *
   * @param value The value to convert; never {@code null}.
   * @return The converted value; never {@code null}.
   */
  @Override
  public String convertToString(Long value) {
    return value.toString();
  }

  /**
   * Defines how to convert a string into a vlaue.
   *
   * @param value The string to convert; never {@code null}.
   * @return The converted string; never {@code null}.
   */
  @Override
  public Long convertFromString(String value) {
    return Long.valueOf(value);
  }
}
