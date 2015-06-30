package com.webonise.tomcat8.redisession.redisclient;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.text.ParseException;
import java.util.*;
import java.util.function.*;

/**
 * Defines how to convert a timestamp (milliseconds since the beginning of the epoch) value to and from Strings.
 */
public class TimestampConverter implements RedisConverter<Long> {

  private static final Log LOG = LogFactory.getLog(TimestampConverter.class);

  protected static final FastDateFormat FORMAT = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
  private final Supplier<? extends Date> defaultDateFactory;

  /**
   * Constructor.
   *
   * @param defaultDateFactory Provides the date to default to if there is a parse error; may not be {@code null}.
   */
  public TimestampConverter(Supplier<? extends Date> defaultDateFactory) {
    Objects.requireNonNull(defaultDateFactory, "supplier for the date to default to");
    this.defaultDateFactory = defaultDateFactory;
  }

  /**
   * Defines how to convert a value to a string.
   *
   * @param value The value to convert; never {@code null}.
   * @return The converted value; never {@code null}.
   */
  @Override
  public String convertToString(Long value) {
    return FORMAT.format(new Date(value));
  }

  /**
   * Defines how to convert a string into a vlaue.
   *
   * @param value The string to convert; never {@code null}.
   * @return The converted string; never {@code null}.
   */
  @Override
  public Long convertFromString(String value) {
    try {
      return FORMAT.parse(value).getTime();
    } catch (ParseException e) {
      Date defaultValue = defaultDateFactory.get();
      String defaultValueString = FORMAT.format(defaultValue);
      LOG.warn("Could not parse date from " + value + " using pattern " + FORMAT.getPattern() + " -- defaulting to " + defaultValueString, e);
      return defaultValue.getTime();
    }
  }

}
