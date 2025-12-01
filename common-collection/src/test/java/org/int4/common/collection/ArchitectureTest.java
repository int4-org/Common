/*
 * MIT License
 *
 * Copyright (c) 2025 John Hendrikx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.int4.common.collection;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.DependencyRules;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

@AnalyzeClasses(packages = ArchitectureTest.BASE_PACKAGE_NAME)
public class ArchitectureTest {
  static final String BASE_PACKAGE_NAME = "org.int4.common.collection";

  @ArchTest
  private final ArchRule packagesShouldBeFreeOfCycles = slices().matching("(**)").should().beFreeOfCycles();

  @ArchTest
  private final ArchRule noClassesShouldDependOnUpperPackages = DependencyRules.NO_CLASSES_SHOULD_DEPEND_UPPER_PACKAGES;

  @ArchTest
  private final ArchRule noClassesShouldPubliclyImplementInterfaceInSamePackage = noClasses().should(publiclyImplementInterfacesInSamePackage());

  private static ArchCondition<JavaClass> publiclyImplementInterfacesInSamePackage() {
    return new ArchCondition<>("publicly implement interfaces that reside in same package") {
      @Override
      public void check(JavaClass cls, ConditionEvents events) {
        for(JavaClass iface : cls.getAllRawInterfaces()) {
          boolean isSamePackageAndPublic = iface.getPackage().equals(cls.getPackage())
            && Modifier.isPublic(cls.reflect().getModifiers())
            && !Modifier.isAbstract(cls.reflect().getModifiers());

          events.add(new SimpleConditionEvent(iface, isSamePackageAndPublic, cls.getDescription() + " is public and not abstract and implements <" + iface.getFullName() + "> in same package"));
        }
      }
    };
  }

  @Test
  void shouldMatchPackageName() {
    assertThat(BASE_PACKAGE_NAME).isEqualTo(MethodHandles.lookup().lookupClass().getPackageName());
  }
}
