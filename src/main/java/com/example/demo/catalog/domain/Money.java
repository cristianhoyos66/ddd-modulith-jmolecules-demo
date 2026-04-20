package com.example.demo.catalog.domain;

import java.math.BigDecimal;
import java.util.Currency;
import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record Money(BigDecimal amount, Currency currency) {

  public Money {
    if (amount.signum() < 0) {
      throw new IllegalArgumentException("Amount must be non-negative");
    }
  }

  public static Money of(String amount, String currencyCode) {
    return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
  }
}
