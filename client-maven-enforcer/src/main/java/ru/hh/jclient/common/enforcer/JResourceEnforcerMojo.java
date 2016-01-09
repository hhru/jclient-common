package ru.hh.jclient.common.enforcer;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.ClassReader;

@Mojo(name = "enforceJResource", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true)
public class JResourceEnforcerMojo extends AbstractMojo {

  /** The maven project (effective pom). */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /** The output directory into which to find the resources. */
  @Parameter(property = "project.build.outputDirectory")
  private File outputDirectory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      long count = recurseFiles(outputDirectory);
      if (count != 0) {
        throw new MojoExecutionException("Found " + count + " unannotated methods");
      }
    }
    catch (IOException e) {
      throw new MojoExecutionException("Failed to process classess", e);
    }
  }

  private long recurseFiles(File file) throws IOException {
    long count = 0;
    if (!file.exists()) {
      return count;
    }
    if (file.isDirectory()) {
      String[] children = file.list();
      if (children != null) {
        for (String child : children) {
          count += recurseFiles(new File(file, child));
        }
      }
    }
    else if (file.getPath().endsWith(".class")) {
      InputStream is = new FileInputStream(file);
      try {
        Collection<String> violations = check(is);
        for (String violation : violations) {
          getLog().error("Method without @JResource: " + violation);
          count++;
        }
      }
      finally {
        closeQuietly(is);
      }
    }
    return count;
  }

  private Collection<String> check(InputStream is) throws IOException {
    return check(new ClassReader(is));
  }

  private Collection<String> check(ClassReader classReader) {
    EnforcerClassVisitor visitor = new EnforcerClassVisitor();
    classReader.accept(visitor, 0);
    return visitor.getViolations();
  }

  static void closeQuietly(Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    }
    catch (IOException ioe) {
      // swallow exception
    }
  }
}
