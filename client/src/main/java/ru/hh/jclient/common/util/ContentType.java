package ru.hh.jclient.common.util;

import java.util.Arrays;
import java.util.Objects;

public class ContentType {

  private static final String WILDCARD = "*";

  public static final String WITH_CHARSET = "; charset=";

  public static final String TEXT_ANY = "text/*";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String TEXT_PLAIN_UTF_8 = TEXT_PLAIN + WITH_CHARSET + "utf-8";
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_JSON_UTF_8 = APPLICATION_JSON + WITH_CHARSET + "utf-8";
  public static final String TEXT_XML = "text/xml";
  public static final String APPLICATION_XML = "application/xml";
  public static final String TEXT_XML_UTF_8 = TEXT_XML + WITH_CHARSET + "utf-8";
  public static final String VIDEO_ANY = "video/*";
  public static final String APPLICATION_PROTOBUF = "application/protobuf";
  public static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";
  public static final String ANY = "*/*";


  private String type1;
  private String type2;
  private String charset;

  public ContentType(String contentType) {
    if (contentType == null) {
      throw new IllegalArgumentException("Content type is null");
    }
    String[] typeAndParams = contentType.trim().toLowerCase().split(";");
    String[] type = typeAndParams[0].split("/");
    type1 = type[0].trim();
    type2 = type[1].trim();

    if (typeAndParams.length > 1) {
      String[] params = typeAndParams[1].trim().split(",");
      charset = Arrays.stream(params).filter(p -> p.contains("charset")).findAny().map(p -> p.split("=")[1].trim()).orElse(null);
    }
  }

  public boolean allows(ContentType contentType) {
    boolean type1Allows = type1.equals(WILDCARD) || type1.equals(contentType.type1);
    boolean type2Allows = type2.equals(WILDCARD) || type2.equals(contentType.type2);
    boolean charsetAllows = charset == null || Objects.equals(charset, contentType.charset);

    return type1Allows && type2Allows && charsetAllows;
  }

}
