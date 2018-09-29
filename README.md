# TIO Java Wrapper

This provides a simple Java wrapper around the TryItOnline API.

To use, you must first initialize an instance of `run.tio.java.TIO` with `new TIO()`.

Then, you can use either of the following methods:

```java
TIO.Result run(String language, String text) throws IOException
TIO.Result run(String language, String text, String input) throws IOException
```

If the given language does not exist, a `LanguageNotFoundException` will be thrown.
A `TIO.Result` has the getters:
```java
String getOutput() // standard output
String getDebug() // standard error
// these times are from the linix time command
long getRealMillis()
long getUserMillis()
long getSysMillis()
double getCpuShare() // 150% CPU share is 1.5.
int getExitCode()
TIO.Result.State getState()
```

A `TIO.Result.State` can be any of the following: `SUCCESS`, `OUTPUT_TRUNCATED`, `TIMEOUT`