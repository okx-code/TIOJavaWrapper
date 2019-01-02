package run.tio.java;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TIOTest {
  private TIO tio = new TIO();

  @Test
  public void run() throws IOException {
    TIO.Result result = tio.run("java-openjdk", "public class Main {\n" +
        "\tpublic static void main(String[] args) throws Exception {\n" +
        "\t\tSystem.out.print(\"test out\");Thread.sleep(1000);\n" +
        " System.err.print(\"test err\");\n" +
        "\t}\n" +
        "}");
    assertEquals("test out", result.getOutput());
    assertEquals("test err", result.getDebug());
    assertTrue(result.getRealMillis() > 1000);
    assertEquals(0, result.getExitCode());
    assertEquals(result.getState(), TIO.Result.State.SUCCESS);
  }

  @Test
  public void runInput() throws IOException {
    TIO.Result result = tio.run("java-openjdk", "public class Main {\n" +
        "\tpublic static void main(String[] args) {\n" +
        "\t\tSystem.out.print(new java.util.Scanner(System.in).nextLine());\n" +
        "\t}\n" +
        "}", "input");
    assertEquals("input", result.getOutput());
    assertEquals(result.getState(), TIO.Result.State.SUCCESS);
  }

  @Test(expected = LanguageNotFoundException.class)
  public void runLanguageNotFound() throws IOException {
    tio.run("somelanguagethatdoesntexist", "wejiog");
  }

  @Test
  public void testTruncated() throws IOException {
    TIO.Result result = tio.run("java-openjdk", "public class Main {" +
        "public static void main(String[] args){" +
        "for(int i=0;i<128_000;i++)System.out.println(\"loop\");}}", "input");
    assertEquals(TIO.Result.State.OUTPUT_TRUNCATED, result.getState());
  }

  @Test
  public void testTimeout() throws IOException {
    TIO.Result result = tio.run("java-openjdk", "public class Main {" +
        "public static void main(String[] args)throws Exception{Thread.sleep(61*1000);}}", "input");
    assertEquals(TIO.Result.State.TIMEOUT, result.getState());
  }

  @Test
  public void testCurry() throws IOException {
    TIO.Result result = tio.run("bash", "cat > code.lcurry\n" +
        "/opt/curry-pakcs/bin/pakcs :load code.lcurry :save :quit > /dev/null\n" +
        "./code", "> main = putStr \"hi\"");
    assertEquals(TIO.Result.State.SUCCESS, result.getState());
    assertEquals("hi", result.getOutput());
  }
}