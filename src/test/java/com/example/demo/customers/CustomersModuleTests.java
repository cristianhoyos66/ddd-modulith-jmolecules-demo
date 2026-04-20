package com.example.demo.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.demo.customers.application.CustomerService;
import com.example.demo.customers.application.RegisterCustomerCommand;
import com.example.demo.customers.domain.CustomerRegistered;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.PublishedEvents;

@ApplicationModuleTest
class CustomersModuleTests {

  @Autowired CustomerService service;

  @Test
  void registeringACustomerPublishesCustomerRegistered(PublishedEvents events) {
    var view = service.register(new RegisterCustomerCommand("Alice", "alice@example.com"));

    assertThat(view.id()).isNotNull();
    assertThat(
            events
                .ofType(CustomerRegistered.class)
                .matching(e -> e.customerId().id().equals(view.id())))
        .hasSize(1);
  }
}
