package com.webonise.tomcat8.redisession.redisclient;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import java.text.ParseException;
import java.util.*;
import java.util.function.*;

/**
 * Defines how to convert a Date value to and from Strings.
 */
public class DateConverter implements RedisConverter<Date> {

  private static final Log LOG = LogFactory.getLog(DateConverter.class);

  protected static final FastDateFormat FORMAT = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
  private final Supplier<? extends Date> defaultDateFactory;

  /**
   * Constructor.
   *
   * @param defaultDateFactory Provides the date to default to if there is a parse error; may not be {@code null}.
   */
  public DateConverter(Supplier<? extends Date> defaultDateFactory) {
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
  public String convertToString(Date value) {
    return FORMAT.format(value);
  }

  /**
   * Defines how to convert a string into a vlaue.
   *
   * @param value The string to convert; never {@code null}.
   * @return The converted string; never {@code null}.
   */
  @Override
  public Date convertFromString(String value) {
    try {
      return FORMAT.parse(value);
    } catch (ParseException e) {
      Date defaultValue = defaultDateFactory.get();
      String defaultValueString = FORMAT.format(defaultValue);
      LOG.warn("Could not parse date from " + value + " using pattern " + FORMAT.getPattern() + " -- defaulting to " + defaultValueString, e);
      return defaultValue;
    }
  }

}
