package com.example.demo.customers.application;

import com.example.demo.customers.domain.Customer;
import java.util.UUID;

/** Application-layer read model returned by CustomerService. */
public record CustomerView(UUID id, String name, String email) {

  static CustomerView from(Customer customer) {
    return new CustomerView(
        customer.getId().id(), customer.getName(), customer.getEmail().address());
  }
}
