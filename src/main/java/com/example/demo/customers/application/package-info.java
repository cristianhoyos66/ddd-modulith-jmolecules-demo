/**
 * Use cases for the customers module. Exposed as a named interface so other modules may invoke
 * query use cases (e.g. {@code exists}) on {@code CustomerService}. The write-side use cases are
 * equally accessible; callers should simply ignore the ones that don't belong to them.
 */
@org.springframework.modulith.NamedInterface
@org.jspecify.annotations.NullMarked
package com.example.demo.customers.application;
