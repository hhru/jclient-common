package ru.hh.jclient.common.util.jersey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

class JAXRSUtils {
  private JAXRSUtils() {
  }

  public static List<PathSegment> getPathSegments(String thePath, boolean decode, boolean ignoreLastSlash) {
    String[] segments = StringUtils.split(thePath, "/");
    List<PathSegment> theList = new ArrayList<PathSegment>();
    for (String path : segments) {
      if (!StringUtils.isEmpty(path)) {
        theList.add(new PathSegmentImpl(path, decode));
      }
    }
    int len = thePath.length();
    if (len > 0 && thePath.charAt(len - 1) == '/') {
      String value = ignoreLastSlash ? "" : "/";
      theList.add(new PathSegmentImpl(value, false));
    }
    return theList;
  }

  public static MultivaluedMap<String, String> getMatrixParams(String path, boolean decode) {
    int index = path.indexOf(';');
    return index == -1 ? new MetadataMap<String, String>() : JAXRSUtils.getStructuredParams(path.substring(index + 1), ";", decode, false);
  }

  /**
   * Retrieve map of query parameters from the passed in message
   *
   * @param message
   * @return a Map of query parameters.
   */
  public static MultivaluedMap<String, String> getStructuredParams(String query, String sep, boolean decode, boolean decodePlus) {
    MultivaluedMap<String, String> map = new MetadataMap<String, String>(new LinkedHashMap<String, List<String>>());

    getStructuredParams(map, query, sep, decode, decodePlus);

    return map;
  }

  public static void getStructuredParams(MultivaluedMap<String, String> queries, String query, String sep, boolean decode, boolean decodePlus) {
    if (!StringUtils.isEmpty(query)) {
      List<String> parts = Arrays.asList(StringUtils.split(query, sep));
      for (String part : parts) {
        int index = part.indexOf('=');
        String name = null;
        String value = null;
        if (index == -1) {
          name = part;
          value = "";
        }
        else {
          name = part.substring(0, index);
          value = index < part.length() ? part.substring(index + 1) : "";
          if (decodePlus && value.contains("+")) {
            value = value.replace('+', ' ');
          }
          if (decode) {
            value = (";".equals(sep)) ? HttpUtils.pathDecode(value) : HttpUtils.urlDecode(value);
          }
        }
        queries.add(HttpUtils.urlDecode(name), value);
      }
    }
  }

}
