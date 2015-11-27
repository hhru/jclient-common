package ru.hh.jclient.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates jclient method that calls one or more resources. {@link #value()} is used to specify constant(s) that will be also used in
 * {@link JClient} on the resource side.
 *
 * Constant can be searched for usages to reveal which resource and jclients are linked.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JResource {

  String[] value();

}
