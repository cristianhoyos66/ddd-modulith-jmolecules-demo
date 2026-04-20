package com.example.demo.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.catalog.application.AddProductCommand;
import com.example.demo.catalog.application.CatalogService;
import com.example.demo.catalog.domain.ProductAdded;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;

@ApplicationModuleTest
class CatalogModuleTests {

  @Autowired CatalogService service;

  @Test
  void addingAProductPublishesProductAdded(PublishedEvents events) {
    var view = service.add(new AddProductCommand("Widget", "9.99", "USD"));

    assertThat(view.id()).isNotNull();
    assertThat(
            events.ofType(ProductAdded.class).matching(e -> e.productId().id().equals(view.id())))
        .hasSize(1);
  }
}
