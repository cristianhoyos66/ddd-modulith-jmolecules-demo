package com.example.demo.customers.web;

import com.example.demo.customers.application.CustomerService;
import com.example.demo.customers.application.RegisterCustomerCommand;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
class CustomersController {

  private final CustomerService service;

  CustomersController(CustomerService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<CustomerDto> register(@RequestBody RegisterCustomerRequest request) {
    var view = service.register(new RegisterCustomerCommand(request.name(), request.email()));
    return ResponseEntity.ok(CustomerDto.from(view));
  }

  @GetMapping("/{id}")
  ResponseEntity<CustomerDto> get(@PathVariable UUID id) {
    return ResponseEntity.ok(CustomerDto.from(service.get(id)));
  }
}
