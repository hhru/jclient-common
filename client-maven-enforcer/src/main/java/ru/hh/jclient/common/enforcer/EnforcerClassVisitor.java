package ru.hh.jclient.common.enforcer;

import static java.util.stream.Collectors.toList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import ru.hh.jclient.common.JResource;

public class EnforcerClassVisitor extends ClassVisitor {

  private final Collection<MethodDescriptor> methodDescriptors;
  private String className;

  public EnforcerClassVisitor() {
    super(Opcodes.ASM5);
    this.methodDescriptors = new ArrayList<>();
  }

  public Collection<String> getViolations() {
    return methodDescriptors.stream().filter(md -> md.violating).map(md -> className + "." + md.name + "(...):" + md.line).collect(toList());
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.className = name.replace('/', '.');
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor base = super.visitMethod(access, name, desc, signature, exceptions);
    if (access != Opcodes.ACC_PUBLIC) {
      return base;
    }
    String returnType = Type.getReturnType(desc).getClassName();
    if (!returnType.equals(CompletableFuture.class.getName())) {
      return base;
    }
    MethodDescriptor descriptor = new MethodDescriptor(name);
    methodDescriptors.add(descriptor);
    MethodVisitor origVisitor = new MethodVisitor(Opcodes.ASM5, base) {
    };
    AnnotationExtractor annotationExtractor = new AnnotationExtractor(origVisitor, descriptor);
    return annotationExtractor;
  }

  private static class AnnotationExtractor extends InstructionAdapter {

    private MethodDescriptor descriptor;

    public AnnotationExtractor(MethodVisitor visitor, MethodDescriptor descriptor) {
      super(Opcodes.ASM5, visitor);
      this.descriptor = descriptor;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      String annotationName = Type.getType(desc).getClassName();
      if (annotationName.equals(JResource.class.getName())) {
        descriptor.violating = false;
      }
      return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitLineNumber(int line, @SuppressWarnings("unused") Label start) {
      if (descriptor.line == null) {
        descriptor.line = line - 1; // line number of first instruction of current method, so declaration is at (line - 1)
      }
    }
  }
}
