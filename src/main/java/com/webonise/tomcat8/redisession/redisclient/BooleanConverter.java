package com.webonise.tomcat8.redisession.redisclient;

/**
 * Defines how to convert a boolean value to and from Strings.
 * <p>
 * This implementation uses 1 for true and 0 for false because Redis is optimized for storing integer values.
 */
public class BooleanConverter implements RedisConverter<Boolean> {

  /**
   * Defines how to convert a value to a string.
   *
   * @param value The value to convert; never {@code null}.
   * @return The converted value; never {@code null}.
   */
  @Override
  public String convertToString(Boolean value) {
    final int i;
    if (value == null || !value) {
      i = 0;
    } else {
      i = 1;
    } 
    return Integer.toString(i);
  }

  /**
   * Defines how to convert a string into a value.
   *
   * @param value The string to convert; never {@code null}.
   * @return The converted string; never {@code null}.
   */
  @Override
  public Boolean convertFromString(String value) {
    if (value == null || value.isEmpty()) return false;
    int i = Integer.parseInt(value);
    return i != 0;
  }
  
}
