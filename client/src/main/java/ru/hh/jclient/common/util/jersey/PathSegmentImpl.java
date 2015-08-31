package ru.hh.jclient.common.util.jersey;

class PathSegmentImpl implements PathSegment {

  private String path;

  public PathSegmentImpl(String path) {
    this(path, true);
  }

  public PathSegmentImpl(String path, boolean decode) {
    this.path = decode ? HttpUtils.pathDecode(path) : path;
  }

  @Override
  public MultivaluedMap<String, String> getMatrixParameters() {
    return JAXRSUtils.getMatrixParams(path, false);
  }

  @Override
  public String getPath() {
    int index = path.indexOf(';');
    String value = index != -1 ? path.substring(0, index) : path;
    if (value.startsWith("/")) {
      value = value.length() == 1 ? "" : value.substring(1);
    }
    return value;
  }

  public String getOriginalPath() {
    return path;
  }

  public String getMatrixString() {
    int index = path.indexOf(';');
    if (index == -1) {
      return null;
    }
    else {
      return path.substring(index);
    }
  }

  @Override
  public String toString() {
    return path;
  }
}
