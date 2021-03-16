package ru.hh.jclient.common.util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

public class ContentType {

  private static final String WILDCARD = "*";

  private static final String WITH_CHARSET = "; charset=";

  public static final String TEXT_ANY = "text/*";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String TEXT_PLAIN_UTF_8 = withCharset(TEXT_PLAIN, "utf-8");
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_JSON_UTF_8 = withCharset(APPLICATION_JSON, "utf-8");
  public static final String TEXT_XML = "text/xml";
  public static final String APPLICATION_XML = "application/xml";
  public static final String TEXT_XML_UTF_8 = withCharset(TEXT_XML, "utf-8");
  public static final String VIDEO_ANY = "video/*";
  public static final String APPLICATION_PROTOBUF = "application/protobuf";
  public static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";
  public static final String ANY = "*/*";
  public static final String FORM_DATA = "x-www-form-urlencoded";

  private final String type1;
  private final String type2;
  private final String charset;

  public ContentType(String contentType) {
    if (contentType == null) {
      throw new IllegalArgumentException("Content type is null");
    }
    if (!contentType.contains("/")) {
      throw new IllegalArgumentException("Wrong content type format");
    }

    String[] typeAndParams = contentType.trim().toLowerCase().split(";");
    String[] type = typeAndParams[0].split("/");
    type1 = type[0].trim();
    type2 = type[1].trim();

    if (typeAndParams.length > 1) {
      String[] params = typeAndParams[1].trim().split(",");
      charset = Arrays.stream(params).filter(p -> p.contains("charset")).findAny().map(p -> p.split("=")[1].trim()).orElse(null);
    }
    else {
      charset = null;
    }
  }

  public boolean allows(ContentType contentType) {
    if (!type1.equals(WILDCARD) && !type1.equals(contentType.type1)) {
      return false;
    }
    if (!type2.equals(WILDCARD) && !type2.equals(contentType.type2)) {
      return false;
    }
    return charset == null || Objects.equals(charset, contentType.charset);
  }

  public static String withCharset(String contentType, Charset charset) {
    return withCharset(contentType, charset.name());
  }

  public static String withCharset(String contentType, String charset) {
    return contentType + WITH_CHARSET + charset.toLowerCase();
  }

}
