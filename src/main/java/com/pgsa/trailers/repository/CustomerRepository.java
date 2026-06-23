// src/main/java/com/pgsa/trailers/repository/CustomerRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.ops.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByName(String name);

    List<Customer> findByIsActiveTrue();

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Customer> searchCustomers(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.customerId = :customerId")
    Long countTripsByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(t.costAmount), 0) FROM Trip t WHERE t.customerId = :customerId")
    Double getTotalSpentByCustomer(@Param("customerId") Long customerId);
}
