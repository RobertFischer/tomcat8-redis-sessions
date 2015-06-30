package com.webonise.tomcat8.redisession.redisclient;

/**
 * Encapsulates the logic for converting to and from a value.
 */
public interface RedisConverter<U> {

  /**
   * Defines how to convert a value to a string.
   *
   * @param value The value to convert; never {@code null}.
   * @return The converted value; never {@code null}.
   */
  String convertToString(U value);

  /**
   * Defines how to convert a string into a vlaue.
   *
   * @param value The string to convert; never {@code null}.
   * @return The converted string; never {@code null}.
   */
  U convertFromString(String value);
}
