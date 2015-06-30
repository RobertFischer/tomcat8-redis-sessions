package com.webonise.tomcat8.redisession.redisclient;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Defines how to convert a serializable value to and from Strings.
 */
public class SerializableConverter<U extends Serializable> implements RedisConverter<U> {

  /**
   * Defines how to convert a value to a string.
   *
   * @param value The value to convert; never {@code null}.
   * @return The converted value; never {@code null}.
   */
  @Override
  public String convertToString(U value) {
    byte[] bytes = SerializationUtils.serialize(value);
    return Base64.getEncoder().encodeToString(bytes);
  }

  /**
   * Defines how to convert a string into a vlaue.
   *
   * @param value The string to convert; never {@code null}.
   * @return The converted string; never {@code null}.
   */
  @Override
  public U convertFromString(String value) {
    byte[] bytes = Base64.getDecoder().decode(value);
    return SerializationUtils.deserialize(bytes);
  }
}
