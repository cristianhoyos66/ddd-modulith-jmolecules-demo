package com.example.demo.customers.infrastructure;

import com.example.demo.customers.application.WelcomeNotifier;
import com.example.demo.customers.domain.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adapter: pretends to deliver a welcome message by logging it.
 *
 * <p>A real deployment would swap this for an HTTP adapter (e.g. RestClient calling an email
 * provider) without touching the application or domain layers.
 */
@Component
class LoggingWelcomeNotifier implements WelcomeNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingWelcomeNotifier.class);

  @Override
  public void welcome(Customer customer) {
    LOG.info(
        "Welcome email dispatched to {} <{}>", customer.getName(), customer.getEmail().address());
  }
}
