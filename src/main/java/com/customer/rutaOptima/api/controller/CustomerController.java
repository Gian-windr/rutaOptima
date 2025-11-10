package com.customer.rutaOptima.api.controller;

import com.customer.rutaOptima.api.dto.CustomerDTO;
import com.customer.rutaOptima.domain.Customer;
import com.customer.rutaOptima.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para clientes
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(@RequestParam(required = false) Boolean activo) {
        List<Customer> customers = (activo != null && activo) 
                ? customerService.findAllActive() 
                : customerService.findAll();
        
        List<CustomerDTO> dtos = customers.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.findById(id);
        return ResponseEntity.ok(toDTO(customer));
    }

    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO dto) {
        Customer customer = toEntity(dto);
        Customer created = customerService.createCustomer(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable Long id, 
                                                       @Valid @RequestBody CustomerDTO dto) {
        Customer customer = toEntity(dto);
        Customer updated = customerService.updateCustomer(id, customer);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // Mapeo simple (en producción usar MapStruct)
    private CustomerDTO toDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .nombre(customer.getNombre())
                .direccion(customer.getDireccion())
                .latitud(customer.getLatitud())
                .longitud(customer.getLongitud())
                .esNuevo(customer.getEsNuevo())
                .ventanaHorariaInicio(customer.getVentanaHorariaInicio())
                .ventanaHorariaFin(customer.getVentanaHorariaFin())
                .demandaPromedioSemanal(customer.getDemandaPromedioSemanal())
                .factorEstacionalidad(customer.getFactorEstacionalidad())
                .telefono(customer.getTelefono())
                .email(customer.getEmail())
                .activo(customer.getActivo())
                .build();
    }

    private Customer toEntity(CustomerDTO dto) {
        return Customer.builder()
                .nombre(dto.getNombre())
                .direccion(dto.getDireccion())
                .latitud(dto.getLatitud())
                .longitud(dto.getLongitud())
                .esNuevo(dto.getEsNuevo() != null ? dto.getEsNuevo() : true)
                .ventanaHorariaInicio(dto.getVentanaHorariaInicio())
                .ventanaHorariaFin(dto.getVentanaHorariaFin())
                .demandaPromedioSemanal(dto.getDemandaPromedioSemanal())
                .factorEstacionalidad(dto.getFactorEstacionalidad())
                .telefono(dto.getTelefono())
                .email(dto.getEmail())
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .build();
    }
}
