package run.tio.java;

public class LanguageNotFoundException extends RuntimeException {
  public LanguageNotFoundException(String language) {
    super(language);
  }
}
