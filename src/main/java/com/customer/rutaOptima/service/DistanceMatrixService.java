package com.customer.rutaOptima.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Servicio para calcular distancias y tiempos reales usando OSRM
 * (Open Source Routing Machine) - Gratuito
 */
@Service
@Slf4j
public class DistanceMatrixService {

    private static final String OSRM_API_URL = "http://router.project-osrm.org/table/v1/driving/";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Calcula matriz de distancias entre múltiples ubicaciones
     * @param locations Lista de ubicaciones [lat, lng]
     * @return Matriz de distancias en metros y duraciones en segundos
     */
    @Cacheable(value = "distanceMatrix", key = "#locations.hashCode()")
    public DistanceMatrix calculateDistanceMatrix(List<Location> locations) {
        if (locations.isEmpty()) {
            return new DistanceMatrix(new double[0][0], new int[0][0]);
        }

        try {
            // Construir coordenadas para OSRM: lng,lat;lng,lat;...
            StringBuilder coordinates = new StringBuilder();
            for (int i = 0; i < locations.size(); i++) {
                Location loc = locations.get(i);
                if (i > 0) coordinates.append(";");
                coordinates.append(loc.getLongitude()).append(",").append(loc.getLatitude());
            }

            // Llamar a OSRM
            String url = OSRM_API_URL + coordinates + "?annotations=distance,duration";
            
            log.debug("Calling OSRM API: {}", url);
            OSRMResponse response = restTemplate.getForObject(url, OSRMResponse.class);

            if (response == null || !"Ok".equals(response.getCode())) {
                log.error("OSRM API error: {}", response != null ? response.getMessage() : "null response");
                return calculateHaversineMatrix(locations); // Fallback
            }

            // Convertir respuesta a matriz
            int n = locations.size();
            double[][] distances = new double[n][n];
            int[][] durations = new int[n][n];

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    distances[i][j] = response.getDistances()[i][j]; // metros
                    durations[i][j] = (int) response.getDurations()[i][j]; // segundos
                }
            }

            log.info("Distance matrix calculated for {} locations using OSRM", n);
            return new DistanceMatrix(distances, durations);

        } catch (Exception e) {
            log.error("Error calling OSRM API, falling back to Haversine", e);
            return calculateHaversineMatrix(locations);
        }
    }

    /**
     * Calcula distancia y tiempo entre dos puntos específicos
     */
    public RouteInfo getRouteInfo(Location from, Location to) {
        try {
            String url = String.format(
                "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                from.getLongitude(), from.getLatitude(),
                to.getLongitude(), to.getLatitude()
            );

            OSRMRouteResponse response = restTemplate.getForObject(url, OSRMRouteResponse.class);

            if (response != null && response.getRoutes() != null && !response.getRoutes().isEmpty()) {
                OSRMRoute route = response.getRoutes().get(0);
                
                RouteInfo info = new RouteInfo();
                info.setDistanceMeters(route.getDistance());
                info.setDurationSeconds((int) route.getDuration());
                info.setGeometry(route.getGeometry().getCoordinates()); // Polyline para mapa
                
                return info;
            }
        } catch (Exception e) {
            log.warn("Error getting route from OSRM: {}", e.getMessage());
        }

        // Fallback: cálculo directo
        return calculateHaversine(from, to);
    }

    /**
     * Fallback: Cálculo Haversine (línea recta) cuando OSRM falla
     */
    private DistanceMatrix calculateHaversineMatrix(List<Location> locations) {
        int n = locations.size();
        double[][] distances = new double[n][n];
        int[][] durations = new int[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    distances[i][j] = 0;
                    durations[i][j] = 0;
                } else {
                    double distKm = haversineDistance(locations.get(i), locations.get(j));
                    distances[i][j] = distKm * 1000; // a metros
                    durations[i][j] = (int) (distKm / 40 * 3600); // Asume 40 km/h
                }
            }
        }

        return new DistanceMatrix(distances, durations);
    }

    private RouteInfo calculateHaversine(Location from, Location to) {
        double distKm = haversineDistance(from, to);
        RouteInfo info = new RouteInfo();
        info.setDistanceMeters(distKm * 1000);
        info.setDurationSeconds((int) (distKm / 40 * 3600));
        info.setGeometry(Arrays.asList(
            new double[]{from.getLongitude(), from.getLatitude()},
            new double[]{to.getLongitude(), to.getLatitude()}
        ));
        return info;
    }

    /**
     * Fórmula Haversine para distancia en línea recta
     */
    private double haversineDistance(Location loc1, Location loc2) {
        final int R = 6371; // Radio de la Tierra en km

        double latDistance = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double lonDistance = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(loc1.getLatitude())) * Math.cos(Math.toRadians(loc2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Retorna km
    }

    // ============== DTOs ==============

    @Data
    public static class Location {
        private double latitude;
        private double longitude;

        public Location(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Location(BigDecimal latitude, BigDecimal longitude) {
            this.latitude = latitude.doubleValue();
            this.longitude = longitude.doubleValue();
        }
    }

    @Data
    public static class DistanceMatrix {
        private double[][] distances; // metros
        private int[][] durations;    // segundos

        public DistanceMatrix(double[][] distances, int[][] durations) {
            this.distances = distances;
            this.durations = durations;
        }
    }

    @Data
    public static class RouteInfo {
        private double distanceMeters;
        private int durationSeconds;
        private List<double[]> geometry; // [[lng, lat], [lng, lat], ...]
    }

    // ============== OSRM Response DTOs ==============

    @Data
    private static class OSRMResponse {
        private String code;
        private String message;
        private double[][] distances;
        private double[][] durations;
    }

    @Data
    private static class OSRMRouteResponse {
        private String code;
        private List<OSRMRoute> routes;
    }

    @Data
    private static class OSRMRoute {
        private double distance;
        private double duration;
        private OSRMGeometry geometry;
    }

    @Data
    private static class OSRMGeometry {
        private String type;
        private List<double[]> coordinates;
    }
}
