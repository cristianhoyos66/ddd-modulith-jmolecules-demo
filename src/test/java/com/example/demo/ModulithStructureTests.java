package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithStructureTests {

  ApplicationModules modules = ApplicationModules.of(DemoApplication.class);

  @Test
  void verifiesModularStructure() {
    modules.verify();
  }

  @Test
  void writesDocumentation() {
    new Documenter(modules).writeDocumentation();
  }
}
