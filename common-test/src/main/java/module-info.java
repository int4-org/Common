/**
 * Module containing common testing classes.
 */
module org.int4.common.test {
  exports org.int4.common.test.explorer;

  // Open to JUnit to allow reflective access to @Action and @Assertion methods during tests:
  opens org.int4.common.test.explorer to org.junit.platform.commons;

  requires transitive org.int4.common.function;
  requires org.junit.jupiter.api;
  requires org.assertj.core;
  requires org.junit.jupiter.params;
  requires org.junit.platform.commons;
  requires org.opentest4j;
}
