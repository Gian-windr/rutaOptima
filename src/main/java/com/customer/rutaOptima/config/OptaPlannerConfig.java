package com.customer.rutaOptima.config;

import com.customer.rutaOptima.optimization.domain.VehicleRoutingSolution;
import com.customer.rutaOptima.optimization.domain.Visit;
import com.customer.rutaOptima.optimization.solver.VehicleRoutingConstraintProvider;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuración de OptaPlanner.
 * Complementa la configuración en application.yml.
 */
@Configuration
public class OptaPlannerConfig {

    @Value("${optaplanner.solver.termination.spent-limit:20s}")
    private String spentLimit;

    @Value("${optaplanner.solver.environment-mode:REPRODUCIBLE}")
    private String environmentMode;

    /**
     * Bean de SolverManager para resolver problemas de optimización.
     * Spring Boot auto-configura este bean, pero lo definimos explícitamente
     * para tener mayor control sobre la configuración.
     */
    @Bean
    public SolverManager<VehicleRoutingSolution, Long> solverManager() {
        SolverFactory<VehicleRoutingSolution> solverFactory = SolverFactory.create(solverConfig());
        return SolverManager.create(solverFactory);
    }

    /**
     * Configuración del solver de OptaPlanner.
     */
    private SolverConfig solverConfig() {
        SolverConfig solverConfig = new SolverConfig();
        
        // Clase de la solución
        solverConfig.withSolutionClass(VehicleRoutingSolution.class);
        
        // Clases de las entidades de planificación
        solverConfig.withEntityClasses(Visit.class);
        
        // Proveedor de restricciones
        solverConfig.withConstraintProviderClass(VehicleRoutingConstraintProvider.class);
        
        // Configuración de terminación
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.withSpentLimit(Duration.parse("PT" + spentLimit.toUpperCase()));
        solverConfig.withTerminationConfig(terminationConfig);
        
        // Modo de ambiente (REPRODUCIBLE para resultados consistentes)
        solverConfig.withEnvironmentMode(
                org.optaplanner.core.config.solver.EnvironmentMode.valueOf(environmentMode)
        );
        
        return solverConfig;
    }
}
