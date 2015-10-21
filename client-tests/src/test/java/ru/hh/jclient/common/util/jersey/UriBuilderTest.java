package ru.hh.jclient.common.util.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import ru.hh.jclient.common.util.jersey.UriBuilder;

// copied from org.apache.cxf.jaxrs.impl.UriBuilderImplTest
public class UriBuilderTest {

  @Test
  public void testUriTemplate() throws Exception {
    UriBuilder builder = UriBuilder.fromUri("http://localhost:8080/{a}/{b}");
    URI uri = builder.build("1", "2");
    assertEquals("http://localhost:8080/1/2", uri.toString());
  }

  @Test
  public void testUriTemplate2() throws Exception {
    UriBuilder builder = UriBuilder.fromUri("http://localhost/{a}/{b}");
    URI uri = builder.build("1", "2");
    assertEquals("http://localhost/1/2", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}").build("{}");
    assertEquals("/%7B%7D", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue2() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}").buildFromEncoded("{}");
    assertEquals("/%7B%7D", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue3() {
    UriBuilder ub = UriBuilder.fromPath("/");
    URI uri = ub.path("{a}").buildFromEncoded("%");
    assertEquals("/%25", uri.toString());
    uri = ub.path("{token}").buildFromEncoded("%", "{}");
    assertEquals("/%25/%7B%7D", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue4() {
    UriBuilder ub = UriBuilder.fromPath("/");
    URI uri = ub.path("{a}").build("%");
    assertEquals("/%25", uri.toString());
    uri = ub.path("{token}").build("%", "{}");
    assertEquals("/%25/%7B%7D", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue5() {
    UriBuilder ub = UriBuilder.fromUri("/%25");
    URI uri = ub.build();
    assertEquals("/%25", uri.toString());
    uri = ub.replacePath("/%/{token}").build("{}");
    assertEquals("/%25/%7B%7D", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue6() {
    UriBuilder ub = UriBuilder.fromPath("/");
    URI uri = ub.path("%").build();
    assertEquals("/%25", uri.toString());
    uri = ub.replacePath("/%/{token}").build("{}");
    assertEquals("/%25/%7B%7D", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue7() {
    UriBuilder ub = UriBuilder.fromPath("/");
    URI uri = ub.replaceQueryParam("a", "%").buildFromEncoded();
    assertEquals("/?a=%25", uri.toString());
    uri = ub.replaceQueryParam("a2", "{token}").buildFromEncoded("{}");
    assertEquals("/?a=%25&a2=%7B%7D", uri.toString());
  }

  @Test
  public void testBuildWithNonEncodedSubstitutionValue8() {
    UriBuilder ub = UriBuilder.fromPath("/");
    URI uri = ub.replaceQueryParam("a", "%").build();
    assertEquals("/?a=%25", uri.toString());
    uri = ub.replaceQueryParam("a2", "{token}").build("{}");
    assertEquals("/?a=%25&a2=%7B%7D", uri.toString());
  }

  @Test
  public void testResolveTemplate() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}").resolveTemplate("a", "1").build();
    assertEquals("/1", uri.toString());
  }

  @Test
  public void testResolveTemplate2() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}/{b}").resolveTemplate("a", "1").build("2");
    assertEquals("/1/2", uri.toString());
  }

  @Test
  public void testResolveTemplate3() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}/{b}").resolveTemplate("b", "1").build("2");
    assertEquals("/2/1", uri.toString());
  }

  @Test
  public void testResolveTemplate4() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}/{b}").queryParam("c", "{c}").resolveTemplate("a", "1").build("2", "3");
    assertEquals("/1/2?c=3", uri.toString());
  }

  @Test
  public void testResolveTemplate5() {
    Map<String, Object> templs = new HashMap<String, Object>();
    templs.put("a", "1");
    templs.put("b", "2");
    URI uri;
    uri = UriBuilder.fromPath("/{a}/{b}").queryParam("c", "{c}").resolveTemplates(templs).build("3");
    assertEquals("/1/2?c=3", uri.toString());
  }

  @Test
  public void testResolveTemplateFromEncoded() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}").resolveTemplate("a", "%20 ").buildFromEncoded();
    assertEquals("/%20%20", uri.toString());
  }

  @Test
  public void testResolveTemplateFromEncodedMap() {
    String expected = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2F%2525test1/fred@example.com/x%25yz";

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("v", new StringBuilder("path-rootless%2Ftest2"));
    map.put("w", new StringBuilder("x%yz"));
    map.put("x", new Object() {
      @Override
      public String toString() {
        return "%2Fpath-absolute%2F%2525test1";
      }
    });
    map.put("y", "fred@example.com");
    UriBuilder builder = UriBuilder.fromPath("").path("{v}/{w}/{x}/{y}/{w}");
    builder = builder.resolveTemplatesFromEncoded(map);
    URI uri = builder.build();
    assertEquals(expected, uri.getRawPath());
  }

  @Test
  public void testResolveTemplateFromMap() {
    URI uri;
    uri = UriBuilder.fromPath("/{a}/{b}").resolveTemplate("a", "1").buildFromMap(Collections.singletonMap("b", "2"));
    assertEquals("/1/2", uri.toString());
  }

  @Test
  public void testResolveTemplateFromMap2() {
    String expected = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2F%2525test1/fred@example.com/x%25yz";

    Map<String, Object> map = new HashMap<String, Object>();
    map.put("x", new StringBuilder("x%yz"));
    map.put("y", new StringBuilder("/path-absolute/%25test1"));
    map.put("z", new Object() {
      @Override
      public String toString() {
        return "fred@example.com";
      }
    });
    map.put("w", "path-rootless/test2");
    UriBuilder builder = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}");
    URI uri = builder.resolveTemplates(map).build();

    assertEquals(expected, uri.getRawPath());
  }

  @Test
  public void testResolveTemplatesMapBooleanSlashEncoded() throws Exception {
    String expected = "path-rootless%2Ftest2/x%25yz/%2Fpath-absolute%2F%2525test1/fred@example.com/x%25yz";
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("x", new StringBuilder("x%yz"));
    map.put("y", new StringBuilder("/path-absolute/%25test1"));
    map.put("z", new Object() {
      @Override
      public String toString() {
        return "fred@example.com";
      }
    });
    map.put("w", "path-rootless/test2");
    UriBuilder builder = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}");
    URI uri = builder.resolveTemplates(map, true).build();
    assertEquals(expected, uri.getRawPath());
  }

  @Test
  public void testResolveTemplatesMapBooleanSlashNotEncoded() throws Exception {
    String expected = "path-rootless/test2/x%25yz//path-absolute/test1/fred@example.com/x%25yz";
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("x", new StringBuilder("x%yz"));
    map.put("y", new StringBuilder("/path-absolute/test1"));
    map.put("z", new Object() {
      @Override
      public String toString() {
        return "fred@example.com";
      }
    });
    map.put("w", "path-rootless/test2");
    UriBuilder builder = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}");
    URI uri = builder.resolveTemplates(map, false).build();
    assertEquals(expected, uri.getRawPath());
  }

  @Test
  public void testQueryParamWithTemplateValues() {
    URI uri;
    uri = UriBuilder.fromPath("/index.jsp").queryParam("a", "{a}").queryParam("b", "{b}").build("valueA", "valueB");
    assertEquals("/index.jsp?a=valueA&b=valueB", uri.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testQueryParamWithMissingTemplateValues() {
    UriBuilder.fromPath("/index.jsp").queryParam("a", "{a}").queryParam("b", "{b}").build("valueA");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testQueryParamWithMissingTemplateValues2() {
    UriBuilder.fromPath("/index.jsp").queryParam("a", "{a}").build();
  }

  @Test
  public void testPathAndQueryParamWithTemplateValues() {
    URI uri;
    uri = UriBuilder.fromPath("/index{ind}.jsp").queryParam("a", "{a}").queryParam("b", "{b}").build("1", "valueA", "valueB");
    assertEquals("/index1.jsp?a=valueA&b=valueB", uri.toString());
  }

  @Test
  public void testReplaceQueryStringWithTemplateValues() {
    URI uri;
    uri = UriBuilder.fromUri("/index.jsp").replaceQuery("a={a}&b={b}").build("valueA", "valueB");
    assertEquals("/index.jsp?a=valueA&b=valueB", uri.toString());
  }

  @Test
  public void testQueryParamUsingMapWithTemplateValues() {
    Map<String, String> values = new HashMap<String, String>();
    values.put("a", "valueA");
    values.put("b", "valueB");
    URI uri;
    uri = UriBuilder.fromPath("/index.jsp").queryParam("a", "{a}").queryParam("b", "{b}").buildFromMap(values);
    assertEquals("/index.jsp?a=valueA&b=valueB", uri.toString());
  }

  @Test
  public void testPathAndQueryParamUsingMapWithTemplateValues() {
    Map<String, String> values = new HashMap<String, String>();
    values.put("a", "valueA");
    values.put("b", "valueB");
    values.put("ind", "1");
    URI uri;
    uri = UriBuilder.fromPath("/index{ind}.jsp").queryParam("a", "{a}").queryParam("b", "{b}").buildFromMap(values);
    assertEquals("/index1.jsp?a=valueA&b=valueB", uri.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCtorNull() throws Exception {
    new UriBuilder(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPathStringNull() throws Exception {
    new UriBuilder().path((String) null);
  }

  @Test
  public void testCtorAndBuild() throws Exception {
    URI uri = new URI("http://foo/bar/baz?query=1#fragment");
    URI newUri = new UriBuilder(uri).build();
    assertEquals("URI is not built correctly", uri, newUri);
  }

  @Test
  public void testTrailingSlash() throws Exception {
    URI uri = new URI("http://bar/");
    URI newUri = new UriBuilder(uri).build();
    assertEquals("URI is not built correctly", "http://bar/", newUri.toString());
  }

  @Test
  public void testPathTrailingSlash() throws Exception {
    URI uri = new URI("http://bar");
    URI newUri = new UriBuilder(uri).path("/").build();
    assertEquals("URI is not built correctly", "http://bar/", newUri.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullPathWithBuildEncoded() throws Exception {
    URI uri = new URI("http://bar");
    new UriBuilder(uri).path("{bar}").buildFromEncoded((Object[]) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullPathWithBuildEncoded2() throws Exception {
    URI uri = new URI("http://bar");
    new UriBuilder(uri).path("{bar}").buildFromEncoded(new Object[] { null });
  }

  @Test
  public void testPathTrailingSlash2() throws Exception {
    URI uri = new URI("http://bar");
    URI newUri = new UriBuilder(uri).path("/").path("/").build();
    assertEquals("URI is not built correctly", "http://bar/", newUri.toString());
  }

  @Test
  public void testClone() throws Exception {
    URI uri = new URI("http://bar");
    URI newUri = new UriBuilder(uri).clone().build();
    assertEquals("URI is not built correctly", "http://bar", newUri.toString());
  }

  @Test
  public void testCloneWithoutLeadingSlash() throws Exception {
    URI uri = new URI("bar/foo");
    URI newUri = new UriBuilder(uri).clone().build();
    assertEquals("URI is not built correctly", "bar/foo", newUri.toString());
  }

  @Test
  public void testCloneWithLeadingSlash() throws Exception {
    URI uri = new URI("/bar/foo");
    URI newUri = new UriBuilder(uri).clone().build();
    assertEquals("URI is not built correctly", "/bar/foo", newUri.toString());
  }

  @Test
  public void testBuildWithLeadingSlash() throws Exception {
    URI uri = new URI("/bar/foo");
    URI newUri = UriBuilder.fromUri(uri).build();
    assertEquals("URI is not built correctly", "/bar/foo", newUri.toString());
  }

  @Test
  public void testClonePctEncodedFromUri() throws Exception {
    URI uri = new URI("http://bar/foo%20");
    URI newUri = new UriBuilder(uri).clone().buildFromEncoded();
    assertEquals("URI is not built correctly", "http://bar/foo%20", newUri.toString());
  }

  @Test
  public void testClonePctEncoded() throws Exception {
    URI uri = new URI("http://bar");
    URI newUri = new UriBuilder(uri)
        .path("{a}")
        .path("{b}")
        .matrixParam("m", "m1 ", "m2+%20")
        .queryParam("q", "q1 ", "q2+q3%20")
        .clone()
        .buildFromEncoded("a+ ", "b%2B%20 ");
    assertEquals("URI is not built correctly", "http://bar/a+%20/b%2B%20%20;m=m1%20;m=m2+%20?q=q1+&q=q2%2Bq3%20", newUri.toString());
  }

  @Test
  public void testEncodedPathQueryFromExistingURI() throws Exception {
    URI uri = new URI("http://bar/foo+%20%2B?q=a+b%20%2B");
    URI newUri = new UriBuilder(uri).buildFromEncoded();
    assertEquals("URI is not built correctly", "http://bar/foo+%20%2B?q=a+b%20%2B", newUri.toString());
  }

  @Test
  public void testEncodedPathWithAsteriscs() throws Exception {
    URI uri = new URI("http://bar/foo/");
    URI newUri = new UriBuilder(uri).path("*").buildFromEncoded();
    assertEquals("URI is not built correctly", "http://bar/foo/*", newUri.toString());
  }

  @Test
  public void testPathWithAsteriscs() throws Exception {
    URI uri = new URI("http://bar/foo/");
    URI newUri = new UriBuilder(uri).path("*").build();
    assertEquals("URI is not built correctly", "http://bar/foo/*", newUri.toString());
  }

  @Test
  public void testEncodedPathWithTwoAsteriscs() throws Exception {
    URI uri = new URI("http://bar/foo/");
    URI newUri = new UriBuilder(uri).path("**").buildFromEncoded();
    assertEquals("URI is not built correctly", "http://bar/foo/**", newUri.toString());
  }

  @Test
  public void testPathWithTwoAsteriscs() throws Exception {
    URI uri = new URI("http://bar/foo/");
    URI newUri = new UriBuilder(uri).path("**").build();
    assertEquals("URI is not built correctly", "http://bar/foo/**", newUri.toString());
  }

  @Test
  public void testEncodedAddedQuery() throws Exception {
    URI uri = new URI("http://bar");
    URI newUri = new UriBuilder(uri).queryParam("q", "a+b%20%2B").buildFromEncoded();
    assertEquals("URI is not built correctly", "http://bar?q=a%2Bb%20%2B", newUri.toString());
  }

  @Test
  public void testQueryWithNoValue() throws Exception {
    URI uri = new URI("http://bar");
    URI newUri = new UriBuilder(uri).queryParam("q").build();
    assertEquals("URI is not built correctly", "http://bar?q", newUri.toString());
  }

  @Test
  public void testMatrixWithNoValue() throws Exception {
    URI uri = new URI("http://bar/foo");
    URI newUri = new UriBuilder(uri).matrixParam("q").build();
    assertEquals("URI is not built correctly", "http://bar/foo;q", newUri.toString());
  }

  @Test
  public void testMatrixWithSlash() throws Exception {
    URI uri = new URI("http://bar/foo");
    URI newUri = new UriBuilder(uri).matrixParam("q", "1/2").build();
    assertEquals("URI is not built correctly", "http://bar/foo;q=1%2F2", newUri.toString());
  }

  @Test
  public void replaceMatrixParamWithEmptyPathTest() throws Exception {
    String name = "name";
    String expected = "http://localhost:8080;name=x;name=y;name=y%20x;name=x%25y;name=%20";

    URI uri = UriBuilder
        .fromPath("http://localhost:8080;name=x=;name=y?;name=x y;name=&")
        .replaceMatrixParam(name, "x", "y", "y x", "x%y", "%20")
        .build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void replaceMatrixWithEmptyPathTest() throws Exception {
    String expected = "http://localhost:8080;name=x;name=y;name=y%20x;name=x%25y;name=%20";
    String value = "name=x;name=y;name=y x;name=x%y;name= ";

    URI uri = UriBuilder.fromPath("http://localhost:8080;name=x=;name=y?;name=x y;name=&").replaceMatrix(value).build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testAddMatrixToEmptyPath() throws Exception {
    String name = "name";
    String expected = "http://localhost:8080;name=x;name=y";

    URI uri = UriBuilder.fromPath("http://localhost:8080").matrixParam(name, "x", "y").build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testSchemeSpecificPart() throws Exception {
    URI uri = new URI("http://bar");
    URI newUri = new UriBuilder(uri).scheme("https").schemeSpecificPart("//localhost:8080/foo/bar").build();
    assertEquals("URI is not built correctly", "https://localhost:8080/foo/bar", newUri.toString());
  }

  @Test
  public void testOpaqueSchemeSpecificPart() throws Exception {
    URI expectedUri = new URI("mailto:javanet@java.net.com");
    URI newUri = new UriBuilder().scheme("mailto").schemeSpecificPart("javanet@java.net.com").build();
    assertEquals("URI is not built correctly", expectedUri, newUri);
  }

  @Test
  public void testReplacePath() throws Exception {
    URI uri = new URI("http://foo/bar/baz;m1=m1value");
    URI newUri = new UriBuilder(uri).replacePath("/newpath").build();
    assertEquals("URI is not built correctly", "http://foo/newpath", newUri.toString());
  }

  @Test
  public void testReplacePathHttpString() throws Exception {
    URI uri = new URI("http://foo/bar/baz;m1=m1value");
    URI newUri = new UriBuilder(uri).replacePath("httppnewpath").build();
    assertEquals("URI is not built correctly", "http://foo/httppnewpath", newUri.toString());
  }

  @Test
  public void testReplaceNullPath() throws Exception {
    URI uri = new URI("http://foo/bar/baz;m1=m1value");
    URI newUri = new UriBuilder(uri).replacePath(null).build();
    assertEquals("URI is not built correctly", "http://foo", newUri.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUriNull() throws Exception {
    new UriBuilder().uri((URI) null);
  }

  @Test
  public void testUri() throws Exception {
    URI uri = new URI("http://foo/bar/baz?query=1#fragment");
    URI newUri = new UriBuilder().uri(uri).build();
    assertEquals("URI is not built correctly", uri, newUri);
  }

  @Test
  public void testBuildValues() throws Exception {
    URI uri = new URI("http://zzz");
    URI newUri = new UriBuilder(uri).path("/{b}/{a}/{b}").build("foo", "bar", "baz");
    assertEquals("URI is not built correctly", new URI("http://zzz/foo/bar/foo"), newUri);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildMissingValues() throws Exception {
    URI uri = new URI("http://zzz");
    new UriBuilder(uri).path("/{b}/{a}/{b}").build("foo");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildMissingValues2() throws Exception {
    URI uri = new URI("http://zzz");
    new UriBuilder(uri).path("/{b}").build();
  }

  @Test
  public void testBuildValueWithBrackets() throws Exception {
    URI uri = new URI("http://zzz");
    URI newUri = new UriBuilder(uri).path("/{a}").build("{foo}");
    assertEquals("URI is not built correctly", new URI("http://zzz/%7Bfoo%7D"), newUri);
  }

  @Test
  public void testBuildValuesPct() throws Exception {
    URI uri = new URI("http://zzz");
    URI newUri = new UriBuilder(uri).path("/{a}").build("foo%25/bar%");
    assertEquals("URI is not built correctly", new URI("http://zzz/foo%2525%2Fbar%25"), newUri);
  }

  @Test
  public void testBuildValuesPctEncoded() throws Exception {
    URI uri = new URI("http://zzz");
    URI newUri = new UriBuilder(uri).path("/{a}/{b}/{c}").buildFromEncoded("foo%25", "bar%", "baz%20");
    assertEquals("URI is not built correctly", new URI("http://zzz/foo%25/bar%25/baz%20"), newUri);
  }

  @Test
  public void testBuildFromMapValues() throws Exception {
    URI uri = new URI("http://zzz");
    Map<String, String> map = new HashMap<String, String>();
    map.put("b", "foo");
    map.put("a", "bar");
    Map<String, String> immutable = Collections.unmodifiableMap(map);
    URI newUri = new UriBuilder(uri).path("/{b}/{a}/{b}").buildFromMap(immutable);
    assertEquals("URI is not built correctly", new URI("http://zzz/foo/bar/foo"), newUri);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFromMapMissingValues() throws Exception {
    URI uri = new URI("http://zzz");
    Map<String, String> map = new HashMap<String, String>();
    map.put("b", "foo");
    Map<String, String> immutable = Collections.unmodifiableMap(map);
    new UriBuilder(uri).path("/{b}/{a}/{b}").buildFromMap(immutable);
  }

  @Test
  public void testBuildFromMapValueWithBrackets() throws Exception {
    URI uri = new URI("http://zzz");
    Map<String, String> map = new HashMap<String, String>();
    map.put("a", "{foo}");
    Map<String, String> immutable = Collections.unmodifiableMap(map);
    URI newUri = new UriBuilder(uri).path("/{a}").buildFromMap(immutable);
    assertEquals("URI is not built correctly", new URI("http://zzz/%7Bfoo%7D"), newUri);
  }

  @Test
  public void testBuildFromMapValuesPct() throws Exception {
    URI uri = new URI("http://zzz");
    Map<String, String> map = new HashMap<String, String>();
    map.put("a", "foo%25/bar%");
    Map<String, String> immutable = Collections.unmodifiableMap(map);
    URI newUri = new UriBuilder(uri).path("/{a}").buildFromMap(immutable);
    assertEquals("URI is not built correctly", new URI("http://zzz/foo%2525%2Fbar%25"), newUri);
  }

  @Test
  public void testBuildFromMapValuesPctEncoded() throws Exception {
    URI uri = new URI("http://zzz");
    Map<String, String> map = new HashMap<String, String>();
    map.put("a", "foo%25");
    map.put("b", "bar%");
    Map<String, String> immutable = Collections.unmodifiableMap(map);
    URI newUri = new UriBuilder(uri).path("/{a}/{b}").buildFromEncodedMap(immutable);
    assertEquals("URI is not built correctly", new URI("http://zzz/foo%25/bar%25"), newUri);
  }

  @Test
  public void testBuildFromEncodedMapComplex() throws Exception {
    Map<String, Object> maps = new HashMap<String, Object>();
    maps.put("x", "x%20yz");
    maps.put("y", "/path-absolute/%test1");
    maps.put("z", "fred@example.com");
    maps.put("w", "path-rootless/test2");

    String expectedPath = "path-rootless/test2/x%20yz//path-absolute/%25test1/fred@example.com/x%20yz";

    URI uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromEncodedMap(maps);
    String rawPath = uri.getRawPath();
    assertEquals(expectedPath, rawPath);
  }

  @Test
  public void testBuildFromEncodedMapComplex2() throws Exception {
    Map<String, Object> maps = new HashMap<String, Object>();
    maps.put("x", "x%yz");
    maps.put("y", "/path-absolute/test1");
    maps.put("z", "fred@example.com");
    maps.put("w", "path-rootless/test2");
    maps.put("u", "extra");

    String expectedPath = "path-rootless/test2/x%25yz//path-absolute/test1/fred@example.com/x%25yz";

    URI uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromEncodedMap(maps);
    String rawPath = uri.getRawPath();
    assertEquals(expectedPath, rawPath);
  }

  @Test
  public void testBuildFromEncodedMapMultipleTimes() throws Exception {
    Map<String, Object> maps = new HashMap<String, Object>();
    maps.put("x", "x%yz");
    maps.put("y", "/path-absolute/test1");
    maps.put("z", "fred@example.com");
    maps.put("w", "path-rootless/test2");

    Map<String, Object> maps1 = new HashMap<String, Object>();
    maps1.put("x", "x%20yz");
    maps1.put("y", "/path-absolute/test1");
    maps1.put("z", "fred@example.com");
    maps1.put("w", "path-rootless/test2");

    Map<String, Object> maps2 = new HashMap<String, Object>();
    maps2.put("x", "x%yz");
    maps2.put("y", "/path-absolute/test1");
    maps2.put("z", "fred@example.com");
    maps2.put("w", "path-rootless/test2");
    maps2.put("v", "xyz");

    String expectedPath = "path-rootless/test2/x%25yz//path-absolute/test1/fred@example.com/x%25yz";

    String expectedPath1 = "path-rootless/test2/x%20yz//path-absolute/test1/fred@example.com/x%20yz";

    String expectedPath2 = "path-rootless/test2/x%25yz//path-absolute/test1/fred@example.com/x%25yz";

    UriBuilder ub = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}");

    URI uri = ub.buildFromEncodedMap(maps);
    assertEquals(expectedPath, uri.getRawPath());

    uri = ub.buildFromEncodedMap(maps1);
    assertEquals(expectedPath1, uri.getRawPath());

    uri = ub.buildFromEncodedMap(maps2);
    assertEquals(expectedPath2, uri.getRawPath());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildFromEncodedMapWithNullValue() throws Exception {

    Map<String, Object> maps = new HashMap<String, Object>();
    maps.put("x", null);
    maps.put("y", "bar");
    UriBuilder.fromPath("").path("{x}/{y}").buildFromEncodedMap(maps);
  }

  @Test
  public void testAddPath() throws Exception {
    URI uri = new URI("http://foo/bar");
    URI newUri = new UriBuilder().uri(uri).path("baz").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar/baz"), newUri);
    newUri = new UriBuilder().uri(uri).path("baz").path("1").path("2").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar/baz/1/2"), newUri);
  }

  @Test
  public void testAddPathSlashes() throws Exception {
    URI uri = new URI("http://foo/");
    URI newUri = new UriBuilder().uri(uri).path("/bar").path("baz/").path("/blah/").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar/baz/blah/"), newUri);
  }

  @Test
  public void testAddPathSlashes2() throws Exception {
    URI uri = new URI("http://foo/");
    URI newUri = new UriBuilder().uri(uri).path("/bar///baz").path("blah//").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar/baz/blah/"), newUri);
  }

  @Test
  public void testAddPathSlashes3() throws Exception {
    URI uri = new URI("http://foo/");
    URI newUri = new UriBuilder().uri(uri).path("/bar/").path("").path("baz").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar/baz"), newUri);
  }

  @Test
  public void testSchemeHostPortQueryFragment() throws Exception {
    URI uri = new URI("http://foo:1234/bar?n1=v1&n2=v2#fragment");
    URI newUri = new UriBuilder()
        .scheme("http")
        .host("foo")
        .port(1234)
        .path("bar")
        .queryParam("n1", "v1")
        .queryParam("n2", "v2")
        .fragment("fragment")
        .build();
    compareURIs(uri, newUri);
  }

  @Test
  public void testReplaceQueryNull() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1&p2=v2");
    URI newUri = new UriBuilder(uri).replaceQuery(null).build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
  }

  @Test
  public void testReplaceQueryWithNull2() {
    String expected = "http://localhost:8080";

    URI uri = UriBuilder.fromPath("http://localhost:8080").queryParam("name", "x=", "y?", "x y", "&").replaceQuery(null).build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testReplaceQueryEmpty() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1&p2=v2");
    URI newUri = new UriBuilder(uri).replaceQuery("").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
  }

  @Test
  public void testReplaceQuery() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1");
    URI newUri = new UriBuilder(uri).replaceQuery("p1=nv1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1"), newUri);
  }

  @Test
  public void testReplaceQuery2() throws Exception {
    URI uri = new URI("http://foo/bar");
    URI newUri = new UriBuilder(uri).replaceQuery("p1=nv1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1"), newUri);
  }

  @Test
  public void testReplaceQuery3() {
    String expected = "http://localhost:8080?name1=xyz";

    URI uri = UriBuilder.fromPath("http://localhost:8080").queryParam("name", "x=", "y?", "x y", "&").replaceQuery("name1=xyz").build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testFromPathUriOnly() {
    String expected = "http://localhost:8080";

    URI uri = UriBuilder.fromPath("http://localhost:8080").build();
    assertEquals(expected, uri.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testQueryParamNameNull() throws Exception {
    new UriBuilder().queryParam(null, "baz");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testQueryParamNullVal() throws Exception {
    new UriBuilder().queryParam("foo", "bar", null, "baz");
  }

  @Test
  public void testNullQueryParamValues() {
    try {
      UriBuilder.fromPath("http://localhost:8080").queryParam("hello", (Object[]) null);
      fail("Should be IllegalArgumentException");
    }
    catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testQueryParamSameNameAndVal() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1");
    URI newUri = new UriBuilder(uri).queryParam("p1", "v1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p1=v1"), newUri);
  }

  @Test
  public void testQueryParamVal() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1");
    URI newUri = new UriBuilder(uri).queryParam("p2", "v2").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p2=v2"), newUri);
  }

  @Test
  public void testQueryParamSameNameDiffVal() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1");
    URI newUri = new UriBuilder(uri).queryParam("p1", "v2").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p1=v2"), newUri);
  }

  @Test
  public void testQueryParamMultiVal() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1");
    URI newUri = new UriBuilder(uri).queryParam("p1", "v2", "v3").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=v1&p1=v2&p1=v3"), newUri);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReplaceQueryParamNameNull() throws Exception {
    new UriBuilder().replaceQueryParam(null, "baz");
  }

  @Test
  public void testReplaceQueryParamValNull() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1&p2=v2&p1=v3");
    URI newUri = new UriBuilder(uri).replaceQueryParam("p1", (Object) null).build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p2=v2"), newUri);
  }

  @Test
  public void testReplaceQueryParamValEmpty() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1&p2=v2&p1=v3");
    URI newUri = new UriBuilder(uri).replaceQueryParam("p1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p2=v2"), newUri);
  }

  @Test
  public void testReplaceQueryParamExisting() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1");
    URI newUri = new UriBuilder(uri).replaceQueryParam("p1", "nv1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1"), newUri);
  }

  @Test
  public void testReplaceQueryParamExistingMulti() throws Exception {
    URI uri = new URI("http://foo/bar?p1=v1&p2=v2");
    URI newUri = new UriBuilder(uri).replaceQueryParam("p1", "nv1", "nv2").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar?p1=nv1&p1=nv2&p2=v2"), newUri);
  }

  @Test
  public void testReplaceMatrixNull() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
    URI newUri = new UriBuilder(uri).replaceMatrix(null).build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
  }

  @Test
  public void testReplaceMatrixEmpty() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
    URI newUri = new UriBuilder(uri).replaceMatrix("").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar"), newUri);
  }

  @Test
  public void testReplaceMatrix() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
    URI newUri = new UriBuilder(uri).replaceMatrix("p1=nv1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=nv1"), newUri);
  }

  @Test
  public void testReplaceMatrix2() throws Exception {
    URI uri = new URI("http://foo/bar/");
    URI newUri = new UriBuilder(uri).replaceMatrix("p1=nv1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar/;p1=nv1"), newUri);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMatrixParamNameNull() throws Exception {
    new UriBuilder().matrixParam(null, "baz");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMatrixParamNullVal() throws Exception {
    new UriBuilder().matrixParam("foo", "bar", null, "baz");
  }

  @Test
  public void testMatrixParamSameNameAndVal() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1");
    URI newUri = new UriBuilder(uri).matrixParam("p1", "v1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p1=v1"), newUri);
  }

  @Test
  public void testMatrixParamNewNameAndVal() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1");
    URI newUri = new UriBuilder(uri).matrixParam("p2", "v2").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p2=v2"), newUri);
  }

  @Test
  public void testMatrixParamSameNameDiffVal() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1");
    URI newUri = new UriBuilder(uri).matrixParam("p1", "v2").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p1=v2"), newUri);
  }

  @Test
  public void testMatrixParamMultiSameNameNewVals() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1");
    URI newUri = new UriBuilder(uri).matrixParam("p1", "v2", "v3").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1;p1=v2;p1=v3"), newUri);
  }

  @Test
  public void testPctEncodedMatrixParam() throws Exception {
    URI uri = new URI("http://foo/bar");
    URI newUri = new UriBuilder(uri).matrixParam("p1", "v1%20").buildFromEncoded();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=v1%20"), newUri);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testReplaceMatrixParamNameNull() throws Exception {
    new UriBuilder().replaceMatrixParam(null, "baz");
  }

  @Test
  public void testReplaceMatrixParamValNull() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1;p2=v2;p1=v3?noise=bazzz");
    URI newUri = new UriBuilder(uri).replaceMatrixParam("p1", (Object) null).build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p2=v2?noise=bazzz"), newUri);
  }

  @Test
  public void testReplaceMatrixParamValEmpty() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1;p2=v2;p1=v3?noise=bazzz");
    URI newUri = new UriBuilder(uri).replaceMatrixParam("p1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p2=v2?noise=bazzz"), newUri);
  }

  @Test
  public void testReplaceMatrixParamExisting() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1");
    URI newUri = new UriBuilder(uri).replaceMatrixParam("p1", "nv1").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=nv1"), newUri);
  }

  @Test
  public void testReplaceMatrixParamExistingMulti() throws Exception {
    URI uri = new URI("http://foo/bar;p1=v1;p2=v2");
    URI newUri = new UriBuilder(uri).replaceMatrixParam("p1", "nv1", "nv2").build();
    assertEquals("URI is not built correctly", new URI("http://foo/bar;p1=nv1;p1=nv2;p2=v2"), newUri);
  }

  @Test
  public void testMatrixNonFinalPathSegment() throws Exception {
    URI uri = new URI("http://blah/foo;p1=v1/bar");
    URI newUri = new UriBuilder(uri).build();
    assertEquals("URI is not built correctly", new URI("http://blah/foo;p1=v1/bar"), newUri);
  }

  @Test
  public void testMatrixFinalPathSegment() throws Exception {
    URI uri = new URI("http://blah/foo;p1=v1/bar;p2=v2");
    URI newUri = new UriBuilder(uri).build();
    assertEquals("URI is not built correctly", new URI("http://blah/foo;p1=v1/bar;p2=v2"), newUri);
  }

  @Test
  public void testAddPathWithMatrix() throws Exception {
    URI uri = new URI("http://blah/foo/bar;p1=v1");
    URI newUri = new UriBuilder(uri).path("baz;p2=v2").build();
    assertEquals("URI is not built correctly", new URI("http://blah/foo/bar;p1=v1/baz;p2=v2"), newUri);
  }

  @Test
  public void testNonHttpSchemes() {
    String[] uris = {
        "ftp://ftp.is.co.za/rfc/rfc1808.txt",
        "mailto:java-net@java.sun.com",
        "news:comp.lang.java",
        "urn:isbn:096139212y",
        "ldap://[2001:db8::7]/c=GB?objectClass?one",
        "telnet://194.1.2.17:81/",
        "tel:+1-816-555-1212",
        "foo://bar.com:8042/there/here?name=baz#brr" };

    int expectedCount = 0;

    for (int i = 0; i < uris.length; i++) {
      URI uri = UriBuilder.fromUri(uris[i]).build();
      assertEquals("Strange", uri.toString(), uris[i]);
      expectedCount++;
    }
    assertEquals(8, expectedCount);
  }

  private void compareURIs(URI uri1, URI uri2) {

    assertEquals("Unexpected scheme", uri1.getScheme(), uri2.getScheme());
    assertEquals("Unexpected host", uri1.getHost(), uri2.getHost());
    assertEquals("Unexpected port", uri1.getPort(), uri2.getPort());
    assertEquals("Unexpected path", uri1.getPath(), uri2.getPath());
    assertEquals("Unexpected fragment", uri1.getFragment(), uri2.getFragment());

    MultivaluedMap<String, String> queries1 = JAXRSUtils.getStructuredParams(uri1.getRawQuery(), "&", false, false);
    MultivaluedMap<String, String> queries2 = JAXRSUtils.getStructuredParams(uri2.getRawQuery(), "&", false, false);
    assertEquals("Unexpected queries", queries1, queries2);
  }

  @Test
  public void testTck1() {
    String value = "test1#test2";
    String expected = "test1%23test2";
    String path = "{arg1}";
    URI uri = UriBuilder.fromPath(path).build(value);
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testNullPathValue() {
    String value = null;
    String path = "{arg1}";
    try {
      UriBuilder.fromPath(path).build(value);
      fail("Should be IllegalArgumentException");
    }
    catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testFragment() {
    String expected = "test#abc";
    String path = "test";
    URI uri = UriBuilder.fromPath(path).fragment("abc").build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testFragmentTemplate() {
    String expected = "abc#xyz";
    URI uri = UriBuilder.fromPath("{arg1}").fragment("{arg2}").build("abc", "xyz");
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testSegments() {
    String path1 = "ab";
    String[] path2 = { "a1", "x/y", "3b " };
    String expected = "ab/a1/x%2Fy/3b%20";

    URI uri = UriBuilder.fromPath(path1).segment(path2).build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testSegments2() {
    String path1 = "";
    String[] path2 = { "a1", "/", "3b " };
    String expected = "a1/%2F/3b%20";

    URI uri = UriBuilder.fromPath(path1).segment(path2).build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testSegments3() {
    String path1 = "ab";
    String[] path2 = { "a1", "{xy}", "3b " };
    String expected = "ab/a1/x%2Fy/3b%20";

    URI uri = UriBuilder.fromPath(path1).segment(path2).build("x/y");
    assertEquals(uri.toString(), expected);
  }

  public void testToTemplate() {
    String path1 = "ab";
    String[] path2 = { "a1", "{xy}", "3b " };
    String expected = "ab/a1/{xy}/3b ";

    String template = UriBuilder.fromPath(path1).segment(path2).toTemplate();
    assertEquals(template, expected);
  }

  @Test
  public void testToTemplateAndResolved() {
    Map<String, Object> templs = new HashMap<String, Object>();
    templs.put("a", "1");
    templs.put("b", "2");
    String template = UriBuilder.fromPath("/{a}/{b}").queryParam("c", "{c}").resolveTemplates(templs).toTemplate();
    assertEquals("/1/2?c={c}", template);
  }

  @Test
  public void testSegments4() {
    String path1 = "ab";
    String[] path2 = { "a1", "{xy}", "3b " };
    String expected = "ab/a1/x/y/3b%20";

    URI uri = UriBuilder.fromPath(path1).segment(path2).build(new Object[] { "x/y" }, false);
    assertEquals(uri.toString(), expected);
  }

  @Test
  public void testPathEncodedSlash() {
    String path1 = "ab";
    String path2 = "{xy}";
    String expected = "ab/x%2Fy";

    URI uri = UriBuilder.fromPath(path1).path(path2).build(new Object[] { "x/y" }, true);
    assertEquals(uri.toString(), expected);
  }

  @Test
  public void testPathEncodedSlashNot() {
    String path1 = "ab";
    String path2 = "{xy}";
    String expected = "ab/x/y";

    URI uri = UriBuilder.fromPath(path1).path(path2).build(new Object[] { "x/y" }, false);
    assertEquals(uri.toString(), expected);
  }

  @Test
  public void testInvalidUriReplacement() throws Exception {
    UriBuilder builder = UriBuilder.fromUri(new URI("news:comp.lang.java"));
    try {
      builder.uri("").build();
      fail("IAE exception is expected");
    }
    catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void testNullSegment() {
    try {
      UriBuilder.fromPath("/").segment((String) null).build();
      fail("Should be IllegalArgumentException");
    }
    catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testInvalidPort() {
    try {
      UriBuilder.fromUri("http://localhost:8080/some/path?name=foo").port(-10).build();
      fail("Should be IllegalArgumentException");
    }
    catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testResetPort() {
    URI uri = UriBuilder.fromUri("http://localhost:8080/some/path").port(-1).build();
    assertEquals("http://localhost/some/path", uri.toString());
  }

  @Test
  public void testInvalidHost() {
    try {
      UriBuilder.fromUri("http://localhost:8080/some/path?name=foo").host("").build();
      fail("Should be IllegalArgumentException");
    }
    catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testFromEncodedDuplicateVar2() {
    String expected = "http://localhost:8080/xy/%20/%25/xy";
    URI uri = UriBuilder.fromPath("http://localhost:8080").path("/{x}/{y}/{z}/{x}").buildFromEncoded("xy", " ", "%");
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testFromEncodedDuplicateVar3() {
    String expected = "http://localhost:8080/1/2/3/1";
    URI uri = UriBuilder.fromPath("http://localhost:8080").path("/{a}/{b}/{c}/{a}").buildFromEncoded("1", "2", "3");

    assertEquals(expected, uri.toString());
  }

  @Test
  public void testFromEncodedDuplicateVarReplacePath() {
    String expected = "http://localhost:8080/1/2/3/1";
    URI uri = UriBuilder.fromPath("").replacePath("http://localhost:8080").path("/{a}/{b}/{c}/{a}").buildFromEncoded("1", "2", "3");

    assertEquals(expected, uri.toString());
  }

  @Test
  public void testNullScheme() {
    String expected = "localhost:8080";
    URI uri = UriBuilder.fromUri("http://localhost:8080").scheme(null).build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testNullMapValue() {
    try {
      Map<String, String> maps = new HashMap<String, String>();
      maps.put("x", null);
      maps.put("y", "/path-absolute/test1");
      maps.put("z", "fred@example.com");
      maps.put("w", "path-rootless/test2");
      maps.put("u", "extra");

      URI uri = UriBuilder.fromPath("").path("{w}/{x}/{y}/{z}/{x}").buildFromMap(maps);

      fail("Should be IllegalArgumentException.  Not return " + uri.toString());
    }
    catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testMissingMapValue() {
    try {
      Map<String, String> maps = new HashMap<String, String>();
      maps.put("x", null);
      maps.put("y", "/path-absolute/test1");
      maps.put("z", "fred@example.com");
      maps.put("w", "path-rootless/test2");
      maps.put("u", "extra");

      URI uri = UriBuilder.fromPath("").path("{w}/{v}/{x}/{y}/{z}/{x}").buildFromMap(maps);

      fail("Should be IllegalArgumentException.  Not return " + uri.toString());
    }
    catch (IllegalArgumentException ex) {
      // expected
    }
  }

  @Test
  public void testFromEncodedDuplicateVar() {
    String expected = "http://localhost:8080/a/%25/=/%25G0/%25/=";

    URI uri = UriBuilder.fromPath("http://localhost:8080").path("/{v}/{w}/{x}/{y}/{z}/{x}").buildFromEncoded("a", "%25", "=", "%G0", "%", "23");
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testMultipleUriSchemes() throws Exception {
    URI uri;

    String[] urisOriginal = {
        "ftp://ftp.is.co.za/rfc/rfc1808.txt",
        "ftp://ftp.is.co.za/rfc/rfc1808.txt",
        "mailto:java-net@java.sun.com",
        "mailto:java-net@java.sun.com",
        "news:comp.lang.java",
        "news:comp.lang.java",
        "urn:isbn:096139210x",
        "http://www.ietf.org/rfc/rfc2396.txt",
        "http://www.ietf.org/rfc/rfc2396.txt",
        "ldap://[2001:db8::7]/c=GB?objectClass?one",
        "ldap://[2001:db8::7]/c=GB?objectClass?one",
        "tel:+1-816-555-1212",
        "tel:+1-816-555-1212",
        "telnet://192.0.2.16:80/",
        "telnet://192.0.2.16:80/",
        "foo://example.com:8042/over/there?name=ferret#nose",
        "foo://example.com:8042/over/there?name=ferret#nose" };

    URI urisReplace[] = new URI[urisOriginal.length];

    urisReplace[0] = new URI("http", "//ftp.is.co.za/rfc/rfc1808.txt", null);
    urisReplace[1] = new URI(null, "ftp.is.co.za", "/test/rfc1808.txt", null, null);
    urisReplace[2] = new URI("mailto", "java-net@java.sun.com", null);
    urisReplace[3] = new URI(null, "testuser@sun.com", null);
    urisReplace[4] = new URI("http", "//comp.lang.java", null);
    urisReplace[5] = new URI(null, "news.lang.java", null);
    urisReplace[6] = new URI("urn:isbn:096139210x");
    urisReplace[7] = new URI(null, "//www.ietf.org/rfc/rfc2396.txt", null);
    urisReplace[8] = new URI(null, "www.ietf.org", "/rfc/rfc2396.txt", null, null);
    urisReplace[9] = new URI("ldap", "//[2001:db8::7]/c=GB?objectClass?one", null);
    urisReplace[10] = new URI(null, "//[2001:db8::7]/c=GB?objectClass?one", null);
    urisReplace[11] = new URI("tel", "+1-816-555-1212", null);
    urisReplace[12] = new URI(null, "+1-866-555-1212", null);
    urisReplace[13] = new URI("telnet", "//192.0.2.16:80/", null);
    urisReplace[14] = new URI(null, "//192.0.2.16:81/", null);
    urisReplace[15] = new URI("http", "//example.com:8042/over/there?name=ferret", null);
    urisReplace[16] = new URI(null, "//example.com:8042/over/there?name=ferret", "mouth");

    String[] urisExpected = {
        "http://ftp.is.co.za/rfc/rfc1808.txt",
        "ftp://ftp.is.co.za/test/rfc1808.txt",
        "mailto:java-net@java.sun.com",
        "mailto:testuser@sun.com",
        "http://comp.lang.java",
        "news:news.lang.java",
        "urn:isbn:096139210x",
        "http://www.ietf.org/rfc/rfc2396.txt",
        "http://www.ietf.org/rfc/rfc2396.txt",
        "ldap://[2001:db8::7]/c=GB?objectClass?one",
        "ldap://[2001:db8::7]/c=GB?objectClass?one",
        "tel:+1-816-555-1212",
        "tel:+1-866-555-1212",
        "telnet://192.0.2.16:80/",
        "telnet://192.0.2.16:81/",
        "http://example.com:8042/over/there?name=ferret#nose",
        "foo://example.com:8042/over/there?name=ferret#mouth" };

    for (int i = 0; i < urisOriginal.length; i++) {
      uri = UriBuilder.fromUri(new URI(urisOriginal[i])).uri(urisReplace[i]).build();
      if (uri.toString().trim().compareToIgnoreCase(urisExpected[i]) != 0) {
        fail("Problem replacing " + urisOriginal[i] + " with " + urisReplace[i] + ", index " + i);
      }
    }
  }

  @Test
  public void testEncodingQueryParamFromBuild() throws Exception {
    String expectedValue = "http://localhost:8080?name=x%3D&name=y?&name=x+y&name=%26";
    URI uri = UriBuilder.fromPath("http://localhost:8080").queryParam("name", "x=", "y?", "x y", "&").build();
    assertEquals(expectedValue, uri.toString());
  }

  @Test
  public void testReplaceParamAndEncodeQueryParamFromBuild() throws Exception {
    String expectedValue = "http://localhost:8080?name=x&name=y&name=y+x&name=x%25y&name=%20";
    URI uri = UriBuilder
        .fromPath("http://localhost:8080")
        .queryParam("name", "x=", "y?", "x y", "&")
        .replaceQueryParam("name", "x", "y", "y x", "x%y", "%20")
        .build();
    assertEquals(expectedValue, uri.toString());
  }

  @Test
  public void testReplaceStringAndEncodeQueryParamFromBuild() {
    String expected = "http://localhost:8080?name1=x&name2=%20&name3=x+y&name4=23&name5=x%20y";

    URI uri = UriBuilder
        .fromPath("http://localhost:8080")
        .queryParam("name", "x=", "y?", "x y", "&")
        .replaceQuery("name1=x&name2=%20&name3=x+y&name4=23&name5=x y")
        .build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testPathParamSpaceBuild() {
    String expected = "http://localhost:8080/name/%20";
    URI uri = UriBuilder.fromUri("http://localhost:8080").path("name/%20").build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testPathParamSpaceBuild2() {
    String expected = "http://localhost:8080/name/%2520";
    URI uri = UriBuilder.fromUri("http://localhost:8080").path("name/{value}").build("%20");
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testPathParamSpaceBuild3() {
    String expected = "http://localhost:8080/name%20space";
    URI uri = UriBuilder.fromUri("http://localhost:8080").path("name space").build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testPathParamSpaceBuild4() {
    String expected = "http://localhost:8080/name%20space";
    URI uri = UriBuilder.fromUri("http://localhost:8080").path("name space").buildFromEncoded();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testPathParamSpaceBuildEncoded() {
    String expected = "http://localhost:8080/name/%20";
    URI uri = UriBuilder.fromUri("http://localhost:8080").path("name/%20").buildFromEncoded();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testPathParamSpaceBuildEncoded2() {
    String expected = "http://localhost:8080/name/%20";
    URI uri = UriBuilder.fromUri("http://localhost:8080").path("name/{value}").buildFromEncoded("%20");
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testQueryParamSpaceBuild() {
    String expected = "http://localhost:8080?name=%20";
    URI uri = UriBuilder.fromUri("http://localhost:8080").queryParam("name", "%20").build();
    assertEquals(expected, uri.toString());
  }

  @Test
  public void testQueryParamSpaceBuild2() {
    String expected = "http://localhost:8080?name=%2520";
    URI uri = UriBuilder.fromUri("http://localhost:8080").queryParam("name", "{value}").build("%20");
    assertEquals(expected, uri.toString());
  }
}