package ru.hh.jclient.common.util;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class ContentType {

  private static final String WILDCARD = "*";

  private static final String WITH_CHARSET = "; charset=";

  public static final String TEXT_ANY = "text/*";
  public static final String TEXT_PLAIN = "text/plain";
  public static final String TEXT_PLAIN_UTF_8 = withCharset(TEXT_PLAIN, "utf-8");
  public static final String TEXT_HTML = "text/html";
  public static final String TEXT_HTML_UTF_8 = withCharset(TEXT_HTML, "utf-8");
  public static final String APPLICATION_JSON = "application/json";
  public static final String APPLICATION_JSON_UTF_8 = withCharset(APPLICATION_JSON, "utf-8");
  public static final String TEXT_XML = "text/xml";
  public static final String APPLICATION_XML = "application/xml";
  public static final String TEXT_XML_UTF_8 = withCharset(TEXT_XML, "utf-8");
  public static final String VIDEO_ANY = "video/*";
  public static final String APPLICATION_PROTOBUF = "application/protobuf";
  public static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";
  public static final String ANY = "*/*";
  public static final String APPLICATION_FORM_DATA = "application/x-www-form-urlencoded";
  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

  private final String type;
  private final String subtype;
  @Nullable
  private final String charset;

  public ContentType(String contentType) {
    if (contentType == null) {
      throw new IllegalArgumentException("Content type is null");
    }
    if (!contentType.contains("/")) {
      throw new IllegalArgumentException("Wrong content type format");
    }

    String[] splittedContentType = contentType.trim().toLowerCase().split(";");
    String[] typeAndSubtype = splittedContentType[0].split("/");
    type = typeAndSubtype[0].trim();
    subtype = typeAndSubtype[1].trim();

    if (splittedContentType.length > 1) {
      String[] params = splittedContentType[1].trim().split(",");
      charset = Arrays.stream(params).filter(p -> p.contains("charset")).findAny().map(p -> p.split("=")[1].trim()).orElse(null);
    }
    else {
      charset = null;
    }
  }

  public String getType() {
    return type;
  }

  public String getSubtype() {
    return subtype;
  }

  public Optional<String> getCharset() {
    return Optional.ofNullable(charset);
  }

  public boolean allows(ContentType contentType) {
    if (!type.equals(WILDCARD) && !type.equals(contentType.type)) {
      return false;
    }
    if (!subtype.equals(WILDCARD) && !subtype.equals(contentType.subtype)) {
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
