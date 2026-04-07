package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CampusMapService {

    private final CampusBuildingRepository campusBuildingRepository;
    private final CampusRoomRepository campusRoomRepository;
    private final CampusNavigationEdgeRepository navigationEdgeRepository;

    public List<CampusBuilding> getAllBuildings() {
        return campusBuildingRepository.findAll();
    }

    public List<CampusBuilding> getBuildingsByType(CampusBuilding.BuildingType type) {
        return campusBuildingRepository.findByBuildingType(type);
    }

    public CampusBuilding getBuilding(Long id) {
        return campusBuildingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Building not found"));
    }

    public List<CampusRoom> getRoomsByBuilding(Long buildingId) {
        return campusRoomRepository.findByBuildingId(buildingId);
    }

    public CampusRoom getRoom(Long id) {
        return campusRoomRepository.findByIdWithBuilding(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    public List<CampusRoom> searchRooms(String query) {
        return campusRoomRepository.searchByQuery(query);
    }

    public List<CampusBuilding> searchBuildings(String name) {
        return campusBuildingRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Simple Dijkstra-based navigation between two rooms.
     * Returns ordered list of edges forming the shortest path.
     */
    public NavigationResult findRoute(Long fromRoomId, Long toRoomId) {
        if (fromRoomId.equals(toRoomId)) {
            return new NavigationResult(List.of(), 0);
        }

        List<CampusNavigationEdge> allEdges = navigationEdgeRepository.findAllAccessible();

        // Build adjacency list
        Map<Long, List<EdgeInfo>> graph = new HashMap<>();
        for (CampusNavigationEdge edge : allEdges) {
            Long from = edge.getFromRoom() != null ? edge.getFromRoom().getId() : null;
            Long to = edge.getToRoom() != null ? edge.getToRoom().getId() : null;
            if (from != null && to != null) {
                graph.computeIfAbsent(from, k -> new ArrayList<>())
                        .add(new EdgeInfo(to, edge.getDistanceMeters(), edge.getId()));
                graph.computeIfAbsent(to, k -> new ArrayList<>())
                        .add(new EdgeInfo(from, edge.getDistanceMeters(), edge.getId()));
            }
        }

        // Dijkstra
        Map<Long, Double> distances = new HashMap<>();
        Map<Long, Long> previousNode = new HashMap<>();
        Map<Long, Long> previousEdge = new HashMap<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1])));

        distances.put(fromRoomId, 0.0);
        pq.offer(new long[]{fromRoomId, Double.doubleToLongBits(0.0)});

        while (!pq.isEmpty()) {
            long[] current = pq.poll();
            long currentNode = current[0];
            double currentDist = Double.longBitsToDouble(current[1]);

            if (currentNode == toRoomId) break;
            if (currentDist > distances.getOrDefault(currentNode, Double.MAX_VALUE)) continue;

            List<EdgeInfo> neighbors = graph.getOrDefault(currentNode, List.of());
            for (EdgeInfo edge : neighbors) {
                double newDist = currentDist + edge.distance;
                if (newDist < distances.getOrDefault(edge.targetRoomId, Double.MAX_VALUE)) {
                    distances.put(edge.targetRoomId, newDist);
                    previousNode.put(edge.targetRoomId, currentNode);
                    previousEdge.put(edge.targetRoomId, edge.edgeId);
                    pq.offer(new long[]{edge.targetRoomId, Double.doubleToLongBits(newDist)});
                }
            }
        }

        if (!distances.containsKey(toRoomId)) {
            return new NavigationResult(List.of(), -1);
        }

        // Reconstruct path
        List<Long> edgeIds = new ArrayList<>();
        Long current = toRoomId;
        while (previousEdge.containsKey(current)) {
            edgeIds.add(previousEdge.get(current));
            current = previousNode.get(current);
        }
        Collections.reverse(edgeIds);

        List<CampusNavigationEdge> path = edgeIds.stream()
                .map(id -> allEdges.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .toList();

        return new NavigationResult(path, distances.get(toRoomId));
    }

    public record NavigationResult(List<CampusNavigationEdge> edges, double totalDistanceMeters) {}

    private record EdgeInfo(Long targetRoomId, double distance, Long edgeId) {}
}
