package com.duberlyguarnizo.bcpcommithelper.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MessageProvider {
  private static ResourceBundle messages;

  static {
    //TODO: get default or configure
    messages = ResourceBundle.getBundle("inspection_messages", new Locale("es"));
  }

  public static String getMessage(String key, Object... args) {
    String message = messages.getString(key);
    return MessageFormat.format(message, args);
  }

  public static String getMessage(String key) {
    return messages.getString(key);
  }
}
