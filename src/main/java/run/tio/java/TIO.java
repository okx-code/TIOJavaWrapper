package run.tio.java;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class TIO {
  private OkHttpClient client = new OkHttpClient.Builder().readTimeout(61, TimeUnit.SECONDS).build();
  private Pattern pattern;

  public TIO() {
    pattern = Pattern.compile("(.*)\\nReal time: ([0-9]+)\\.([0-9]+) s\\nUser time: ([0-9])+\\.([0-9]+) s\\nSys\\. time: ([0-9]+)\\.([0-9]+) s\\nCPU share: ([0-9]+\\.[0-9]+) %\\nExit code: ([0-9]+)", Pattern.DOTALL);

  }

  public Result run(String language, String text) throws IOException {
    return run(language, text, "");
  }

  public Result run(String language, String text, String input) throws IOException {
    String encoded = encode(language, text, input);
    byte[] compressed = compress(encoded);

    Request request = new Request.Builder()
        .url("https://tio.run/cgi-bin/run/api/")
        .post(RequestBody.create(null, compressed))
        .build();

    String response = client.newCall(request).execute().body().string();
    String separator = response.substring(0, 16);
    String[] parts = response.substring(16).split(Pattern.quote(separator));
    String output = parts[0];
    if (output.equals("The language '" + language + "' could not be found on the server.\n")) {
      throw new LanguageNotFoundException(language);
    }

    String debug = parts[1];
    Matcher matcher = pattern.matcher(debug);
    matcher.matches();

    Result.State state;
    if(parts.length < 3) {
      state = Result.State.SUCCESS;
    } else {
      state = Result.State.fromMessage(parts[2]);
    }

    return new Result(output, matcher.group(1), parseMillis(matcher, 1), parseMillis(matcher, 2), parseMillis(matcher, 3), parsePercentage(matcher, 8), Integer.parseInt(matcher.group(9)), state);
  }

  private String encode(String language, String text, String input) {
    if (language.contains("\0") || text.contains("\0") || input.contains("\0")) {
      throw new IllegalArgumentException("Argument contains NUL character.");
    }

    return "Vlang\0001\000" + language + "\000F.input.tio\000" + input.length() + "\000" + input + "\000" + "F.code.tio\000" + text.length() + "\000" + text + "R";
  }

  private byte[] compress(String str) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    // create deflater without header
    DeflaterOutputStream def = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION, true));
    def.write(str.getBytes());
    def.close();
    return out.toByteArray();
  }

  private int parseMillis(Matcher matcher, int index) {
    return (Integer.parseInt(matcher.group(index * 2)) * 1000) + Integer.parseInt(matcher.group((index * 2) + 1));
  }

  private float parsePercentage(Matcher matcher, int index) {
    return Float.parseFloat(matcher.group(index)) / 100;
  }

  @Data
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static final class Result {
    private final String output;
    private final String debug;
    private final long realMillis;
    private final long userMillis;
    private final long sysTime;
    private final double cpuShare;
    private final int exitCode;
    private final State state;

    @RequiredArgsConstructor
    public enum State {
      SUCCESS(""),
      OUTPUT_TRUNCATED("The output exceeded 128 KiB and was truncated.\n"),
      TIMEOUT("The request exceeded the 60 second time limit and was terminated.\n");

      @Getter
      private final String message;

      public static State fromMessage(String message) {
        for(State state : values()) {
          if(message.equalsIgnoreCase(state.message)) {
            return state;
          }
        }
        return null;
      }
    }
  }
}
