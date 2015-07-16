package com.webonise.tomcat8.redisession;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import javax.servlet.http.HttpSession;
import java.text.ParseException;
import java.util.*;
import java.util.function.*;

/**
 * Various conventions used to determine keys and other derived values.
 */
public class Convention {

  /*
      You almost certainly don't want to be accessing these from outside of this class. If you think you want
      to access these, you are almost certainly wrong. These conventional rules should be accessed via methods,
      not by retrieving these fields. These private fields are implementation details.
   */
  private static final char KEY_DELIMITER = ':';
  private static final String SESSION_PREFIX = "TOMCAT_SESSION" + KEY_DELIMITER;
  private static final String METADATA_SUFFIX = "" + KEY_DELIMITER + "metadata";
  private static final String ATTRIBUTES_SUFFIX = "" + KEY_DELIMITER + "attributes";
  private static final FastDateFormat DATE_FORMAT = DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;

  /**
   * The pattern for {@code SCAN} that will select session attribute keys.
   */
  public static final String ATTRIBUTES_KEY_PATTERN = SESSION_PREFIX + "*" + ATTRIBUTES_SUFFIX;
  public static final String METADATA_KEY_PATTERN = SESSION_PREFIX + "*" + METADATA_SUFFIX;

  /**
   * The hash key denoting whether the session was valid last we checked.
   */
  public static final String IS_VALID_HKEY = "IS_VALID";

  /**
   * The hash key storing the string representation of the moment when the session was created.
   *
   * @see #stringFromDate(Date)
   * @see #dateFromString(String)
   */
  public static final String CREATION_TIME_HKEY = "CREATION_TIME";

  /**
   * The hash key storing the string representation of most recent moment when a request came in using this session.
   *
   * @see #stringFromDate(Date)
   * @see #dateFromString(String)
   * @see HttpSession#getLastAccessedTime()
   */
  public static final String LAST_ACCESS_TIME_HKEY = "LAST_REQUEST_END_TIME";

  /**
   * The maximum inactive interval in integral seconds.
   *
   * @see HttpSession#getMaxInactiveInterval()
   */
  public static final String MAX_INACTIVE_INTERVAL_HKEY = "TTL";

  /**
   * The hash key storing the string representation of when the session was expired.
   *
   * @see #stringFromDate(Date)
   * @see #dateFromString(String)
   */
  public static final String EXPIRED_TIME_HKEY = "EXPIRED_AT";

  /**
   * The key holding the count of expired sessions.
   */
  public static final String EXPIRED_SESSIONS_COUNT_KEY = "EXPIRED_SESSIONS";

  /**
   * The key holding the maximum alive time of a given session, in {@code int} seconds
   */
  public static final String SESSION_MAX_ALIVE_TIME_KEY = "MAX_ALIVE_TIME";

  /**
   * The key holding the maximum active sessions.
   */
  public static final String SESSION_MAX_ACTIVE_KEY = "MAX_ACTIVE";

  /**
   * The key holding the current active sessions.
   */
  public static final String ACTIVE_SESSIONS_COUNT_KEY = "CURRENT_ACTIVE";

  /**
   * The key holding the average alive time (in seconds).
   */
  public static final String SESSION_AVERAGE_ALIVE_TIME_KEY = "AVERAGE_ALIVE_TIME";


  /**
   * The key holding the create rate for sessions, in integer sessions/minute.
   */
  public static final String SESSION_CREATE_RATE_KEY = "SESSION_CREATE_RATE";

  /**
   * The key holdint the expiration rate for sessions, in integer sessions/minute.
   */
  public static final String SESSION_EXPIRE_RATE_KEY = "SESSION_EXPIRE_RATE";

  /**
   * The container for all the functions that convert from a session id to a key where session data is stored.
   * If you add a new key for a session, then be sure to add it into this list.
   */
  private static final List<UnaryOperator<String>> SESSION_CONVERSION_FUNCTIONS =
      Arrays.asList(
                       Convention::sessionIdToAttributesKey,
                       Convention::sessionIdToMetadataKey
      );// TODO We should derive this via reflection using annotations

  public static final String AUTH_TYPE_HKEY = "AUTH_TYPE";
  public static final String THIS_ACCESSED_TIME_HKEY = "LAST_REQUEST_START_TIME";
  public static final String PRINCIPAL_HKEY = "PRINCIPAL";

  /**
   * Provides a session id given an attributes key.
   *
   * @param attributesKey The attributes from which to derive the session id; never {@code null}.
   * @return The session id, if the string is a properly-formed attributes key.
   * @throws IllegalArgumentException If the argument is not a properly-formed attributes key.
   */
  public static String attributesKeyToSessionId(String attributesKey) {
    Objects.requireNonNull(attributesKey, "attributes key to convert to session may not be null");
    String toReturn = StringUtils.substringBetween(attributesKey, SESSION_PREFIX, ATTRIBUTES_SUFFIX);
    if (toReturn == null || toReturn.isEmpty()) {
      throw new IllegalArgumentException("Argument is not a valid attributes key: " + attributesKey);
    }
    return toReturn;
  }

  /**
   * Provides the metadata key given a session id.
   *
   * @param sessionId The session id to convert; may not be {@code null} or empty.
   * @return The metadata key that should be used for that session id.
   */
  public static String sessionIdToMetadataKey(String sessionId) {
    Objects.requireNonNull(sessionId, "session id to convert to metadata key may not be null");
    if (sessionId.isEmpty()) {
      throw new IllegalArgumentException("session id to convert to metadata key may not be empty");
    }
    return SESSION_PREFIX + sessionId + METADATA_SUFFIX;
  }

  /**
   * Provides the conventional {@code String} representation of the given {@code Date}.
   *
   * @param date The date to convert; may not be {@code null}.
   * @return The {@code String} representation of the {@code Date}; never {@code null}.
   */
  public static String stringFromDate(Date date) {
    Objects.requireNonNull(date, "date to write to string");
    return DATE_FORMAT.format(date);
  }

  /**
   * Provides the {@code Date} derived from a conventional {@code String} representation of dates.
   *
   * @param dateString The date to convert; may not be {@code null}.
   * @return The date represented by the string.
   * @throws IllegalArgumentException If the argument is not a properly conventional date string.
   */
  public static Date dateFromString(String dateString) {
    Objects.requireNonNull(dateString, "date string to parse");
    try {
      return DATE_FORMAT.parse(dateString);
    } catch (ParseException pe) {
      throw new IllegalArgumentException(
                                            "Could not convert string to date: " + dateString +
                                                " (format is: " + DATE_FORMAT.getPattern() + ")",
                                            pe
      );
    }
  }

  /**
   * Provides the attributes key given a session id
   *
   * @param sessionid The session id to convert; may not be {@code null} or empty.
   * @return The key that should be used for working with attributes; never {@code null}
   */
  public static String sessionIdToAttributesKey(String sessionid) {
    Objects.requireNonNull(sessionid, "session id to convert to attributes key");
    if (sessionid.isEmpty()) {
      throw new IllegalArgumentException("Cannot convert an empty session id to an attributes key");
    }
    return SESSION_PREFIX + sessionid + ATTRIBUTES_SUFFIX;
  }

  public static String metadataKeyToSessionId(String metadataKey) {
    Objects.requireNonNull(metadataKey, "metadata key to convert to session id");
    if (metadataKey.isEmpty()) {
      throw new IllegalArgumentException("Cannot convert an empty metadata key to a session id");
    }
    String toReturn = StringUtils.substringBetween(metadataKey, SESSION_PREFIX, METADATA_SUFFIX);
    if (toReturn == null || toReturn.isEmpty()) {
      throw new IllegalArgumentException("Argument is not a valid metadata key: " + metadataKey);
    }
    return toReturn;
  }

  /**
   * Provides all the mappings of keys necessary to change a session id from the first argument to the second.
   *
   * @param oldId The old session id to move from.
   * @param newId The new session id to move to.
   * @return A map of the keys that have to move.
   */
  public static Map<String, String> getChangeSessionIdMapping(String oldId, String newId) {
    Objects.requireNonNull(oldId, "the old session id to convert");
    Objects.requireNonNull(newId, "the new session id to convert");

    Map<String, String> toReturn = new HashMap<>(SESSION_CONVERSION_FUNCTIONS.size());
    SESSION_CONVERSION_FUNCTIONS.forEach(keyMaker -> {
          toReturn.put(keyMaker.apply(oldId), keyMaker.apply(newId));
        }
    );
    return toReturn;
  }
}
