package com.example.demo.customers.web;

import com.example.demo.customers.application.CustomerView;
import java.util.UUID;

public record CustomerDto(UUID id, String name, String email) {

  static CustomerDto from(CustomerView view) {
    return new CustomerDto(view.id(), view.name(), view.email());
  }
}
