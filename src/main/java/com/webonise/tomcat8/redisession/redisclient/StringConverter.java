package com.webonise.tomcat8.redisession.redisclient;

/**
 * Defines how to convert a long value to and from Strings.
 */
public class StringConverter implements RedisConverter<String> {
  /**
   * Defines how to convert a value to a string.
   *
   * @param value The value to convert; never {@code null}.
   * @return The converted value; never {@code null}.
   */
  @Override
  public String convertToString(String value) {
    return value;
  }

  /**
   * Defines how to convert a string into a vlaue.
   *
   * @param value The string to convert; never {@code null}.
   * @return The converted string; never {@code null}.
   */
  @Override
  public String convertFromString(String value) { return value; }
}
