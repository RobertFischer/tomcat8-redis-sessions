# Redis Sessions for Tomcat 8

The existing libraries for Redis sessions don't work well in Tomcat 8, and have some interesting design decisions.

## Session Storage Approach

Each fresh session is given a [random UUID](http://docs.oracle.com/javase/8/docs/api/java/util/UUID.html#randomUUID--) as an identifier.
There are five keys in Redis associated with the session:

  * `UUID:attributes` &mdash; The attributes, stored as a hash of attribute name keys onto serialized Java object values.
  * `UUID:valid` &mdash; Whether this session is valid, specified as a boolean string: `true` or `false`. Set to `true` initially, and `false` when explicitly invalidated or discovered as expired.
  * `UUID:creation_time` &mdash; The [creation time](http://bit.ly/1GaaVf9) of the session, set on creation and never updated. The format is [ISO-8601 complete date plus hours and minutes](http://www.w3.org/TR/NOTE-datetime).
  * `UUID:last_access_time` &mdash; The [last access time](http://bit.ly/1f0twkV) of the session, set on created and updated whenever a request is made. The format is [ISO-8601 complete date plus hours and minutes](http://www.w3.org/TR/NOTE-datetime).
  * `UUID:max_inactive_interval` &mdash; The [max_inactive_interval](http://bit.ly/1F6k6cP) of the session, set on creation and updated whenever the user explicitly updates it.

If `max_inactive_interval` is greater than zero, then the `UUID:attributes` entry has an explicit expiration of `max_inactive_interval` seconds after `last_access_time`,
which is set explicitly and updated whenever a request is made. If the session is requested by the user but it is discovered to be after the expiration, the system will
invalidate the session. A session which is invalidated, either explicitly or automatically, will have its `UUID:attributes` key deleted and have its `UUID:valid`
value set to `false`.

All the keys other than `UUID:attributes` are left to be removed by Redis at its discretion: they are left around for auditing support.


