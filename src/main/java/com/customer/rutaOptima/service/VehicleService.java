package com.customer.rutaOptima.service;

import com.customer.rutaOptima.config.exception.BusinessException;
import com.customer.rutaOptima.domain.Vehicle;
import com.customer.rutaOptima.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para vehículos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        log.info("Creando vehículo: {}", vehicle.getNombre());
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Vehículo con ID " + id + " no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAllActive() {
        return vehicleRepository.findByActivoTrue();
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle vehicle) {
        Vehicle existing = findById(id);
        existing.setNombre(vehicle.getNombre());
        existing.setTipo(vehicle.getTipo());
        existing.setCapacidadCantidad(vehicle.getCapacidadCantidad());
        existing.setCapacidadVolumen(vehicle.getCapacidadVolumen());
        existing.setCapacidadPeso(vehicle.getCapacidadPeso());
        existing.setVelocidadKmh(vehicle.getVelocidadKmh());
        existing.setCostoKm(vehicle.getCostoKm());
        existing.setDepotLatitud(vehicle.getDepotLatitud());
        existing.setDepotLongitud(vehicle.getDepotLongitud());
        existing.setJornadaInicio(vehicle.getJornadaInicio());
        existing.setJornadaFin(vehicle.getJornadaFin());
        return vehicleRepository.save(existing);
    }

    @Transactional
    public void deactivateVehicle(Long id) {
        Vehicle vehicle = findById(id);
        vehicle.setActivo(false);
        vehicleRepository.save(vehicle);
        log.info("Vehículo con ID {} desactivado", id);
    }
}
