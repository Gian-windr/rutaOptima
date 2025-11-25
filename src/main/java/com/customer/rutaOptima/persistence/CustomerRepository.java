package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Customer
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByActivoTrue();

    @Query("SELECT c FROM Customer c WHERE c.activo = true ORDER BY c.nombre")
    List<Customer> findAllActiveCustomersOrdered();
    
    List<Customer> findByZona(String zona);
}
