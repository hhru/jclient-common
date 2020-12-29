package ru.hh.jclient.common.util;

import java.util.Arrays;
import java.util.Objects;

public class ContentType {

  public static final String WITH_CHARSET = "; charset=";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String TEXT_PLAIN_UTF_8 = TEXT_PLAIN + WITH_CHARSET + "utf-8";
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_JSON_UTF_8 = APPLICATION_JSON + WITH_CHARSET + "utf-8";
  public static final String TEXT_XML = "text/xml";
  public static final String APPLICATION_XML = "application/xml";
  public static final String TEXT_XML_UTF_8 = TEXT_XML + WITH_CHARSET + "utf-8";
  public static final String ANY_VIDEO = "video/*";
  public static final String APPLICATION_PROTOBUF = "application/protobuf";
  public static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";
  public static final String ANY = "*/*";

  private String type;
  private String charset;

  public ContentType(String contentType) {
    if (contentType == null) {
      throw new IllegalArgumentException("Content type is null");
    }
    String[] typeAndParams = contentType.trim().toLowerCase().split(";");
    type = typeAndParams[0];
    if (typeAndParams.length > 1) {
      String[] params = typeAndParams[1].trim().split(",");
      charset = Arrays.stream(params).filter(p -> p.contains("charset")).findAny().map(p -> p.split("=")[1]).orElse(null);
    }
  }

  public boolean matches(ContentType contentType) {
    return Objects.equals(type, contentType.type) && (charset == null || Objects.equals(charset, contentType.charset));
  }

}
