# Logging

Seaty logs through SLF4J/Logback (bundled with Spring Boot). Configuration lives in
`src/main/resources/logback-spring.xml`.

## Log levels

Use these meanings consistently — do not mix ERROR/WARN or INFO/DEBUG:

- **ERROR** — the app could not fulfil a request due to an unexpected, unrecoverable failure that
  needs operator attention. In this app: database/infrastructure failures
  (`DataAccessException`), truly unexpected exceptions. Not for user input errors.
- **WARN** — a handled, anticipated abnormal condition, often security-relevant. In this app:
  failed authentication attempts, requests rejected for a missing/invalid session cookie,
  validation rejections. The app keeps working, but someone may want to notice (e.g. probing).
- **INFO** — high-level, low-volume normal business events forming a readable behavior trail. In
  this app: app started, a guest search performed (prefix + result count), a successful
  authentication, security wiring registered at startup. One line per meaningful user action.
- **DEBUG** — developer detail for troubleshooting, off in prod. In this app: exact query
  parameters, SQL about to run, row counts, interceptor allow-decisions, cookie present/absent
  (never its value).
- **TRACE** — very fine-grained (per-row); essentially unused here.

## Logger idiom

```kotlin
private companion object {
    val log: Logger = LoggerFactory.getLogger(GuestController::class.java)
}
```

Use an explicit class reference (not `javaClass`), since `javaClass` on a proxied Spring bean
(e.g. `@Configuration` classes) resolves to the CGLIB proxy name instead of the real class.

## Security rules (must-follow)

- Never log `secret`, `providedSecret`, the `session` cookie value, or the raw `Set-Cookie`/
  `Cookie` header.
- `Utils.constantTimeEquals` stays log-free — adding I/O would break its constant-time guarantee
  and risk leaking secrets.
- Guest-name search *prefixes* may be logged at INFO — they're the core troubleshooting signal and
  low sensitivity.
- `remoteAddr` may be a proxy/load-balancer IP rather than the real client IP if one sits in
  front of the app.

## Local vs server behavior

- **Local (default profile):** console only, colored, app package (`com.janusa.seaty`) at DEBUG.
  No log file is written.
- **Server (`prod` profile):** console (colored) + rolling file (uncolored, for clean `grep`),
  app package at INFO.

Enable file logging on the server with:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar seaty.jar
```

Set `LOG_PATH` to an absolute path so the log location is deterministic regardless of working
directory (defaults to `./logs` otherwise):

```bash
SPRING_PROFILES_ACTIVE=prod LOG_PATH=/var/log/seaty java -jar seaty.jar
```

## Rotation policy

Rotation is handled in-process by Logback's `SizeAndTimeBasedRollingPolicy` — no OS-level
logrotate needed:

- Roll over at **10MB** or daily, whichever comes first.
- Keep **14** days of history.
- Cap total rotated log size at **200MB** (oldest files deleted first).
- Rotated files are gzip-compressed (`seaty.yyyy-MM-dd.i.log.gz`).

## Accessing logs on the VPS

```bash
tail -f $LOG_PATH/seaty.log          # follow the live log
grep "some text" $LOG_PATH/seaty.log # search the current file
zgrep "some text" $LOG_PATH/*.log.gz # search rotated (compressed) files
zcat $LOG_PATH/seaty.2026-07-01.0.log.gz | less
scp host:$LOG_PATH/seaty.log .       # copy a file off-box
```
