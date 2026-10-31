package com.mycompany.logistic.service;

import com.mycompany.logistic.model.Vehicle;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class AipPlanner {
    private AipPlanner() {
    }

    public static BigDecimal estimateCost(BigDecimal distanceKm, BigDecimal weightKg, String vehicleType) {
        BigDecimal baseRate = switch (vehicleType == null ? "" : vehicleType.toUpperCase()) {
            case "VAN" -> new BigDecimal("0.85");
            case "TRUCK" -> new BigDecimal("1.45");
            case "BIKE" -> new BigDecimal("0.55");
            default -> BigDecimal.ONE;
        };
        BigDecimal weightFactor = BigDecimal.ONE.add(weightKg.divide(new BigDecimal("500"), 4, RoundingMode.HALF_UP));
        return distanceKm.multiply(baseRate).multiply(weightFactor).add(new BigDecimal("8.00"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static String priority(BigDecimal weightKg, BigDecimal distanceKm, String requestedPriority) {
        if ("URGENT".equalsIgnoreCase(requestedPriority)) {
            return "URGENT";
        }
        int score = distanceKm.compareTo(new BigDecimal("500")) > 0 ? 2 : 0;
        score += weightKg.compareTo(new BigDecimal("100")) > 0 ? 1 : 0;
        score += "HIGH".equalsIgnoreCase(requestedPriority) ? 2 : 0;
        return score >= 3 ? "HIGH" : "NORMAL";
    }

    public static Vehicle assignVehicle(List<Vehicle> vehicles, BigDecimal weightKg, String requestedType) {
        return vehicles.stream()
                .filter(v -> "AVAILABLE".equalsIgnoreCase(v.status))
                .filter(v -> v.capacityKg.compareTo(weightKg) >= 0)
                .filter(v -> requestedType == null || requestedType.isBlank()
                        || requestedType.equalsIgnoreCase(v.vehicleType))
                .min(Comparator.comparing((Vehicle v) -> v.capacityKg)
                        .thenComparing(v -> v.costPerKm))
                .orElse(null);
    }

    public static Map<String, Object> shortestPath(String start, String destination) {
        Map<String, Map<String, Integer>> graph = new HashMap<>();
        connect(graph, "Central Hub", "North Hub", 12);
        connect(graph, "Central Hub", "East Hub", 18);
        connect(graph, "Central Hub", "West Hub", 16);
        connect(graph, "North Hub", "East Hub", 9);
        connect(graph, "North Hub", "Airport", 22);
        connect(graph, "East Hub", "Airport", 14);
        connect(graph, "East Hub", "South Hub", 20);
        connect(graph, "West Hub", "South Hub", 11);
        connect(graph, "South Hub", "Airport", 25);
        if (!graph.containsKey(start) || !graph.containsKey(destination)) {
            throw new IllegalArgumentException("Use a known node: Central Hub, North Hub, East Hub, West Hub, South Hub, or Airport.");
        }
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));
        graph.keySet().forEach(node -> distances.put(node, Integer.MAX_VALUE));
        distances.put(start, 0);
        queue.add(new NodeDistance(start, 0));
        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (current.distance() != distances.get(current.node())) {
                continue;
            }
            if (current.node().equals(destination)) {
                break;
            }
            graph.get(current.node()).forEach((neighbor, cost) -> {
                int candidate = current.distance() + cost;
                if (candidate < distances.get(neighbor)) {
                    distances.put(neighbor, candidate);
                    previous.put(neighbor, current.node());
                    queue.add(new NodeDistance(neighbor, candidate));
                }
            });
        }
        List<String> path = new ArrayList<>();
        for (String node = destination; node != null; node = previous.get(node)) {
            path.add(0, node);
        }
        return Map.of("path", path, "distanceKm", distances.get(destination), "algorithm", "Dijkstra");
    }

    private static void connect(Map<String, Map<String, Integer>> graph, String a, String b, int distance) {
        graph.computeIfAbsent(a, ignored -> new HashMap<>()).put(b, distance);
        graph.computeIfAbsent(b, ignored -> new HashMap<>()).put(a, distance);
    }

    private record NodeDistance(String node, int distance) {
    }
}
