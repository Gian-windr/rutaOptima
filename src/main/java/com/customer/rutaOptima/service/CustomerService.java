package com.customer.rutaOptima.service;

import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.Customer;
import com.customer.rutaOptima.persistence.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para clientes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createCustomer(Customer customer) {
        log.info("Creando cliente: {}", customer.getNombre());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Cliente con ID " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Customer> findAllActive() {
        return customerRepository.findByActivoTrue();
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        Customer existing = findById(id);
        existing.setNombre(customer.getNombre());
        existing.setDireccion(customer.getDireccion());
        existing.setLatitud(customer.getLatitud());
        existing.setLongitud(customer.getLongitud());
        existing.setVentanaHorariaInicio(customer.getVentanaHorariaInicio());
        existing.setVentanaHorariaFin(customer.getVentanaHorariaFin());
        existing.setTelefono(customer.getTelefono());
        existing.setEmail(customer.getEmail());
        existing.setZona(customer.getZona());
        return customerRepository.save(existing);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new BusinessException("Cliente con ID " + id + " no encontrado");
        }
        customerRepository.deleteById(id);
        log.info("Cliente con ID {} eliminado", id);
    }
}
