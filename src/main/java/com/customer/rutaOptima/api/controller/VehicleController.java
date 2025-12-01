package com.customer.rutaOptima.api.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.customer.rutaOptima.api.dto.VehicleDTO;
import com.customer.rutaOptima.domain.Vehicle;
import com.customer.rutaOptima.service.VehicleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para vehículos
 */
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<List<VehicleDTO>> getAllVehicles(@RequestParam(required = false) Boolean activo) {
        List<Vehicle> vehicles = (activo != null && activo) 
                ? vehicleService.findAllActive() 
                : vehicleService.findAll();
        
        List<VehicleDTO> dtos = vehicles.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleDTO> getVehicleById(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.findById(id);
        return ResponseEntity.ok(toDTO(vehicle));
    }

    @PostMapping
    public ResponseEntity<VehicleDTO> createVehicle(@Valid @RequestBody VehicleDTO dto) {
        Vehicle vehicle = toEntity(dto);
        Vehicle created = vehicleService.createVehicle(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleDTO> updateVehicle(@PathVariable Long id, 
                                                     @Valid @RequestBody VehicleDTO dto) {
        Vehicle vehicle = toEntity(dto);
        Vehicle updated = vehicleService.updateVehicle(id, vehicle);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateVehicle(@PathVariable Long id) {
        vehicleService.deactivateVehicle(id);
        return ResponseEntity.noContent().build();
    }

    private VehicleDTO toDTO(Vehicle vehicle) {
        return VehicleDTO.builder()
                .id(vehicle.getId())
                .nombre(vehicle.getNombre())
                .tipo(vehicle.getTipo())
                .patente(vehicle.getPatente())
                .conductor(vehicle.getConductor())
                .zona(vehicle.getZona())
                .color(vehicle.getColor())
                .capacidadCantidad(vehicle.getCapacidadCantidad())
                .capacidadVolumen(vehicle.getCapacidadVolumen())
                .capacidadPeso(vehicle.getCapacidadPeso())
                .velocidadKmh(vehicle.getVelocidadKmh())
                .costoKm(vehicle.getCostoKm())
                .activo(vehicle.getActivo())
                .depotLatitud(vehicle.getDepotLatitud())
                .depotLongitud(vehicle.getDepotLongitud())
                .jornadaInicio(vehicle.getJornadaInicio())
                .jornadaFin(vehicle.getJornadaFin())
                .build();
    }

    private Vehicle toEntity(VehicleDTO dto) {
        return Vehicle.builder()
                .nombre(dto.getNombre())
                .tipo(dto.getTipo())
                .patente(dto.getPatente())
                .conductor(dto.getConductor())
                .zona(dto.getZona())
                .color(dto.getColor() != null ? dto.getColor() : "#3B82F6")
                .capacidadCantidad(dto.getCapacidadCantidad())
                .capacidadVolumen(dto.getCapacidadVolumen())
                .capacidadPeso(dto.getCapacidadPeso())
                .velocidadKmh(dto.getVelocidadKmh())
                .costoKm(dto.getCostoKm())
                .activo(dto.getActivo() == null || dto.getActivo())
                .depotLatitud(dto.getDepotLatitud())
                .depotLongitud(dto.getDepotLongitud())
                .jornadaInicio(dto.getJornadaInicio())
                .jornadaFin(dto.getJornadaFin())
                .build();
    }
}
