package ru.hh.jclient.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates resource method that is called by one or more jclients. {@link #value()} is used to specify constant that will be also used in
 * {@link JResource} on the jclient side.
 *
 * Constant can be searched for usages to reveal which resource and jclients are linked.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface JClient {

  String value();

}
