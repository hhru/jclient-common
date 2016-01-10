package ru.hh.jclient.common.enforcer;

import static java.util.stream.Collectors.toSet;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.Searcher;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.expression.AnnotationValue;
import com.thoughtworks.qdox.model.expression.AnnotationValueList;
import com.thoughtworks.qdox.model.impl.DefaultJavaAnnotation;
import com.thoughtworks.qdox.model.impl.DefaultJavaMethod;
import ru.hh.jclient.common.JClient;
import ru.hh.jclient.common.JResource;

@Mojo(name = "enforceJClient", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true)
public class JClientEnforcerMojo extends AbstractMojo {

  @Parameter(defaultValue = "${reactorProjects}", readonly = true)
  List<MavenProject> projects;

  /** The maven project (effective pom). */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /** The output directory into which to find the resources. */
  @Parameter(property = "project.build.outputDirectory")
  private File outputDirectory;

  /** The output directory into which to find the source code. */
  @Parameter(property = "project.build.sourceDirectory")
  private File sourceDirectory;

  @Parameter(required = true)
  private File clientSourceDirectory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (clientSourceDirectory == null || !clientSourceDirectory.exists()) {
      throw new MojoExecutionException("clientSourceDirectory not specified or points to non-existing directory");
    }

    JavaProjectBuilder builder = prepareBuilder(sourceDirectory);
    Map<JavaAnnotation, Collection<String>> resourceAnnotations = findAndResolve(builder, JClient.class);
    Set<String> resourceAnnotationValues = resourceAnnotations.values().stream().flatMap(v -> v.stream()).collect(toSet());
    
    builder = prepareBuilder(clientSourceDirectory);
    Map<JavaAnnotation, Collection<String>> clientAnnotations = findAndResolve(builder, JResource.class);
    Set<String> clientAnnotationValues = clientAnnotations.values().stream().flatMap(v -> v.stream()).collect(toSet());

    boolean resourcesInvalid = resourceAnnotations
        .entrySet()
        .stream()
        .map(e -> checkValid(e.getKey(), e.getValue(), clientAnnotationValues, "is not referenced by any clients"))
        .collect(toSet())
        .contains(false);
    boolean clientsInvalid = clientAnnotations
        .entrySet()
        .stream()
        .map(e -> checkValid(e.getKey(), e.getValue(), resourceAnnotationValues, "is not referenced by any resources"))
        .collect(toSet())
        .contains(false);

    if (resourcesInvalid || clientsInvalid) {
      throw new MojoExecutionException("Annotations are missing");
    }
  }

  private boolean checkValid(JavaAnnotation annotation, Collection<String> fields, Set<String> otherSide, String message) {
    return !fields.stream().map(f -> checkFieldValid(annotation, f, otherSide, message)).collect(toSet()).contains(false);
  }

  private Boolean checkFieldValid(JavaAnnotation annotation, String field, Set<String> otherSide, String message) {
    if (otherSide.contains(field)) {
      return true;
    }

    DefaultJavaAnnotation detailedAnnotation = (DefaultJavaAnnotation) annotation;
    DefaultJavaMethod detailedMethod = (DefaultJavaMethod) detailedAnnotation.getContext();
    StringBuilder errorString = new StringBuilder();
    if (detailedMethod.getParentClass() != null) {
      errorString.append(detailedMethod.getParentClass().getFullyQualifiedName()).append(".");
    }
    errorString
        .append(detailedMethod.getName())
        .append("(...):")
        .append(detailedMethod.getLineNumber())
        .append(" pointing to ")
        .append(field)
        .append(" ")
        .append(message);

    getLog().error(errorString);
    return false;
  }

  private JavaProjectBuilder prepareBuilder(File sourcesDirectory) {
    getLog().info("Scanning " + sourcesDirectory);
    JavaProjectBuilder builder = new JavaProjectBuilder();
    builder.setErrorHandler(e -> getLog().warn(e.getMessage()));
    builder.addSourceTree(sourcesDirectory, f -> getLog().warn("Failed to parse file " + f));
    getLog().info("Found " + builder.getClasses().size() + " classess");
    getLog().info("Found " + builder.getSources().size() + " sources");
    return builder;
  }

  private Map<JavaAnnotation, Collection<String>> findAndResolve(JavaProjectBuilder builder, Class<?> annotationClass) {
    return builder
        .search(new AnnotationSearcher(annotationClass))
        .stream()
        .flatMap(c -> c.getMethods().stream())
        .flatMap(m -> m.getAnnotations().stream())
        .filter(a -> a.getType().getFullyQualifiedName().equals(annotationClass.getName()))
        .collect(Collectors.toMap(Function.identity(), this::resolveField));
  }

  private Collection<String> resolveField(JavaAnnotation annotation) {
    Collection<String> names;
    AnnotationValue value = annotation.getProperty("value");
    if (value instanceof AnnotationValueList) {
      names = ((AnnotationValueList) value).getValueList().stream().map(a -> a.getParameterValue().toString()).collect(Collectors.toList());
    }
    else {
      names = Collections.singleton(value.getParameterValue().toString());
    }
    
    // search fieldName in static imports
    return names.stream().map(n -> resolveSingleField(annotation, n)).collect(Collectors.toList());
  }

  private String resolveSingleField(JavaAnnotation annotation, String fieldName) {
    DefaultJavaAnnotation detailedAnnotation = (DefaultJavaAnnotation) annotation;
    return ((DefaultJavaMethod) detailedAnnotation.getContext())
        .getDeclaringClass()
        .getSource()
        .getImports()
        .stream()
        .filter(i -> i.startsWith("static "))
        .filter(i -> i.endsWith("." + fieldName))
        .map(i -> i.substring("static ".length()))
        .findFirst()
        .orElse(fieldName);
  }

  private static class AnnotationSearcher implements Searcher {

    private final String annotationClassName;

    public AnnotationSearcher(Class<?> annotationClass) {
      this.annotationClassName = annotationClass.getName();
    }
    @Override
    public boolean eval(JavaClass cls) {
      return cls.getMethods().stream().flatMap(m -> m.getAnnotations().stream()).anyMatch(
          a -> a.getType().getFullyQualifiedName().equals(annotationClassName));
    }
  }
}
