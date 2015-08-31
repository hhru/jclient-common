package ru.hh.jclient.common.util.jersey;

interface PathSegment {

  /**
   * Get the path segment.
   * <p>
   *
   * @return the path segment
   */
  String getPath();

  /**
   * Get a map of the matrix parameters associated with the path segment. The map keys are the names of the matrix parameters with any percent-escaped
   * octets decoded.
   *
   * @return the map of matrix parameters
   * @see <a href="http://www.w3.org/DesignIssues/MatrixURIs.html">Matrix URIs</a>
   */
  MultivaluedMap<String, String> getMatrixParameters();

}
