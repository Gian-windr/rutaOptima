package com.customer.rutaOptima.config;

import java.time.Duration;

import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.customer.rutaOptima.optimization.domain.VehicleRoutingSolution;
import com.customer.rutaOptima.optimization.solver.VehicleRoutingConstraintProvider;

/**
 * Configuración de OptaPlanner para optimización de rutas.
 * Integra OSRM para distancias reales a través de shadow variables.
 */
@Configuration
public class OptaPlannerConfig {

    @Bean
    public SolverConfig solverConfig() {
        // Fase 1: Construction Heuristic
        // ALLOCATE_ENTITY_FROM_QUEUE inicializa todas las variables de planificación
        ConstructionHeuristicPhaseConfig constructionHeuristicConfig = new ConstructionHeuristicPhaseConfig()
            .withConstructionHeuristicType(ConstructionHeuristicType.ALLOCATE_ENTITY_FROM_QUEUE);
        
        // Fase 2: Local Search (mejorar la solución inicial)
        LocalSearchPhaseConfig localSearchConfig = new LocalSearchPhaseConfig()
            .withTerminationConfig(new TerminationConfig()
                .withSecondsSpentLimit(25L)); // 25 segundos para Local Search
        
        return new SolverConfig()
            .withSolutionClass(VehicleRoutingSolution.class)
            .withEntityClasses(com.customer.rutaOptima.optimization.domain.Visit.class)
            .withConstraintProviderClass(VehicleRoutingConstraintProvider.class)
            .withPhases(constructionHeuristicConfig, localSearchConfig)
            .withTerminationConfig(new TerminationConfig()
                .withSpentLimit(Duration.ofSeconds(30)) // 30 segundos máximo total
                .withBestScoreLimit("0hard/*soft")) // para si encuentra solución perfecta
            .withEnvironmentMode(org.optaplanner.core.config.solver.EnvironmentMode.REPRODUCIBLE);
    }

    @Bean
    public SolverFactory<VehicleRoutingSolution> solverFactory(SolverConfig solverConfig) {
        return SolverFactory.create(solverConfig);
    }

    @Bean
    public SolverManager<VehicleRoutingSolution, Long> solverManager(
            SolverFactory<VehicleRoutingSolution> solverFactory) {
        return SolverManager.create(solverFactory);
    }
}
