package com.customer.rutaOptima.persistence;

import com.customer.rutaOptima.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad Vehicle
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByActivoTrue();

    List<Vehicle> findByTipo(String tipo);

    @Query("SELECT v FROM Vehicle v WHERE v.activo = true ORDER BY v.capacidadCantidad DESC")
    List<Vehicle> findAllActiveVehiclesOrderedByCapacity();

    List<Vehicle> findByIdInAndActivoTrue(List<Long> ids);
}
