package com.example.demo.customers.application;

import com.example.demo.customers.domain.Customer;
import com.example.demo.customers.domain.CustomerRepository;
import com.example.demo.customers.domain.EmailAddress;
import java.util.UUID;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private final CustomerRepository repository;
  private final WelcomeNotifier welcomeNotifier;

  public CustomerService(CustomerRepository repository, WelcomeNotifier welcomeNotifier) {
    this.repository = repository;
    this.welcomeNotifier = welcomeNotifier;
  }

  @Transactional
  public CustomerView register(RegisterCustomerCommand command) {
    var customer = repository.save(new Customer(command.name(), new EmailAddress(command.email())));
    welcomeNotifier.welcome(customer);
    return CustomerView.from(customer);
  }

  public CustomerView get(UUID id) {
    var customer =
        repository
            .findById(new Customer.CustomerIdentifier(id))
            .orElseThrow(() -> new IllegalArgumentException("No customer " + id));
    return CustomerView.from(customer);
  }

  /** Query use case for other modules: does a customer with this id exist? */
  public boolean exists(Customer.CustomerIdentifier id) {
    return repository.existsById(id);
  }
}
