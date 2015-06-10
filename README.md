# Redis Sessions for Tomcat 8

The existing libraries for Redis sessions don't work well in Tomcat 8, and have some interesting design decisions.

## Dependencies

This library leverages [Jedis](https://github.com/xetorthio/jedis) to work with Redis. Those Redis connections are pooled using [Apache Commons Pool2](https://commons.apache.org/proper/commons-pool/). The library is compiled for Java 8.

## Session Storage Approach

Each fresh session is given a [random UUID](http://docs.oracle.com/javase/8/docs/api/java/util/UUID.html#randomUUID--) as an identifier.
There are two keys in Redis associated with the session:

  * `UUID:attributes` &mdash; The attributes, stored as a hash of attribute name keys onto Base64-encoded serialized Java object values.
  * `UUID:metadata` &mdash; A hash of the various bits of metadata associated with this UUID.

### Metadata

  * `valid` &mdash; Whether this session is valid, specified as a boolean string: `true` or `false`. Set to `true` initially, and `false` when explicitly invalidated or discovered as expired.
  * `creation_time` &mdash; The [creation time](http://bit.ly/1GaaVf9) of the session, set on creation and never updated. The format is [ISO-8601 complete date plus hours and minutes](http://www.w3.org/TR/NOTE-datetime).
  * `last_access_time` &mdash; The [last access time](http://bit.ly/1f0twkV) of the session, set on created and updated whenever a request is made. The format is [ISO-8601 complete date plus hours and minutes](http://www.w3.org/TR/NOTE-datetime).
  * `max_inactive_interval` &mdash; The [max_inactive_interval](http://bit.ly/1F6k6cP) of the session, set on creation and updated whenever the user explicitly updates it.

### Expiration

If `max_inactive_interval` is greater than zero, then the `UUID:attributes` entry has an explicit expiration of `max_inactive_interval` seconds after `last_access_time`,
which is set explicitly and updated whenever a request is made. If the session is requested by the user but it is discovered to be after the expiration, the system will
invalidate the session. A session which is invalidated, either explicitly or automatically, will have its `UUID:attributes` key deleted and have its `valid`
value set to `false`.

The `UUID:metadata` entry has no expiration, and will be removed by Redis at its discretion: they are left around for auditing support.

## Session Retrieval Approach

Session attributes and metadata are retrieved the first time they are needed in a request. Metadata is updated when the request is retrieved, or when the metadata is
explicitly manipulated through the `Session` or `HTTPSession` API. Any retrieved or assigned session attribute is persisted back to Redis when the
request completes: we have to persist any retrieved session attribute because Java allows mutable data to be stored in the session attribute, and it is difficult to
recognize when the mutable data has changed.
