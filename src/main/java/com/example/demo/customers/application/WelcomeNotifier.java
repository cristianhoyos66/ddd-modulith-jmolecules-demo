package com.example.demo.customers.application;

import com.example.demo.customers.domain.Customer;

/**
 * Port: what the customers use case needs from the outside world to greet a new customer.
 *
 * <p>The application declares the contract in its own terms ("welcome this customer"); the
 * infrastructure layer provides the concrete adapter (email, SMS, external HTTP API, …). Keeping
 * the interface here means the domain and application layers don't depend on any particular
 * delivery mechanism.
 */
public interface WelcomeNotifier {

  void welcome(Customer customer);
}
