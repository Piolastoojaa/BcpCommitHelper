package com.duberlyguarnizo.bcpcommithelper.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageProvider {
  private static final ResourceBundle messages;

  static {
    var locale = Locale.forLanguageTag("es");
    messages = ResourceBundle.getBundle("inspection_messages", locale);
  }

  private MessageProvider(){}

  public static String getMessage(String key, Object... args) {
    String message = messages.getString(key);
    return MessageFormat.format(message, args);
  }

  public static String getMessage(String key) {
    return messages.getString(key);
  }
}
