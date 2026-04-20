package com.example.demo;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.junit.jupiter.api.Test;

/**
 * Enforces the jMolecules DDD rules across the codebase via ArchUnit. Fails the build if any
 * aggregate, entity, value object, event, or identifier violates the tactical DDD constraints.
 */
class JMoleculesRulesTests {

  @Test
  void enforcesDddRules() {
    var classes = new ClassFileImporter().importPackagesOf(DemoApplication.class);
    JMoleculesDddRules.all().check(classes);
  }
}
