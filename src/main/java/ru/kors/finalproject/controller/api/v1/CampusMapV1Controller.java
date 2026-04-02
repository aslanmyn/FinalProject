package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.service.CampusMapService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student/campus-map")
@RequiredArgsConstructor
public class CampusMapV1Controller {

    private final CampusMapService campusMapService;

    // ===== Buildings =====

    @GetMapping("/buildings")
    public ResponseEntity<?> getBuildings(@RequestParam(required = false) String type) {
        List<CampusBuilding> buildings;
        if (type != null) {
            buildings = campusMapService.getBuildingsByType(CampusBuilding.BuildingType.valueOf(type));
        } else {
            buildings = campusMapService.getAllBuildings();
        }
        return ResponseEntity.ok(buildings.stream().map(this::toBuildingDto).toList());
    }

    @GetMapping("/buildings/{id}")
    public ResponseEntity<?> getBuilding(@PathVariable Long id) {
        return ResponseEntity.ok(toBuildingDto(campusMapService.getBuilding(id)));
    }

    @GetMapping("/buildings/search")
    public ResponseEntity<?> searchBuildings(@RequestParam String q) {
        return ResponseEntity.ok(campusMapService.searchBuildings(q).stream()
                .map(this::toBuildingDto).toList());
    }

    // ===== Rooms =====

    @GetMapping("/buildings/{buildingId}/rooms")
    public ResponseEntity<?> getRoomsByBuilding(@PathVariable Long buildingId) {
        return ResponseEntity.ok(campusMapService.getRoomsByBuilding(buildingId).stream()
                .map(this::toRoomDto).toList());
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<?> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(toRoomDto(campusMapService.getRoom(id)));
    }

    @GetMapping("/rooms/search")
    public ResponseEntity<?> searchRooms(@RequestParam String q) {
        return ResponseEntity.ok(campusMapService.searchRooms(q).stream()
                .map(this::toRoomDto).toList());
    }

    // ===== Navigation =====

    @GetMapping("/navigate")
    public ResponseEntity<?> navigate(@RequestParam Long fromRoomId, @RequestParam Long toRoomId) {
        CampusMapService.NavigationResult result = campusMapService.findRoute(fromRoomId, toRoomId);
        List<NavigationEdgeDto> edges = result.edges().stream()
                .map(e -> new NavigationEdgeDto(
                        e.getId(),
                        e.getFromRoom() != null ? e.getFromRoom().getId() : null,
                        e.getToRoom() != null ? e.getToRoom().getId() : null,
                        e.getFromBuilding() != null ? e.getFromBuilding().getId() : null,
                        e.getToBuilding() != null ? e.getToBuilding().getId() : null,
                        e.getDistanceMeters(), e.isAccessible()))
                .toList();
        return ResponseEntity.ok(new NavigationResultDto(edges, result.totalDistanceMeters()));
    }

    // ===== DTOs =====

    private BuildingDto toBuildingDto(CampusBuilding b) {
        return new BuildingDto(b.getId(), b.getName(), b.getCode(), b.getDescription(),
                b.getBuildingType() != null ? b.getBuildingType().name() : null,
                b.getLatitude(), b.getLongitude(), b.getFloorCount(), b.getImageUrl());
    }

    private RoomDto toRoomDto(CampusRoom r) {
        return new RoomDto(r.getId(),
                r.getBuilding() != null ? r.getBuilding().getId() : null,
                r.getBuilding() != null ? r.getBuilding().getName() : null,
                r.getRoomNumber(), r.getFloor(),
                r.getRoomType() != null ? r.getRoomType().name() : null,
                r.getName(), r.getDescription(), r.getCapacity(),
                r.getLatitude(), r.getLongitude());
    }

    public record BuildingDto(Long id, String name, String code, String description,
                               String buildingType, Double latitude, Double longitude,
                               int floorCount, String imageUrl) {}
    public record RoomDto(Long id, Long buildingId, String buildingName, String roomNumber,
                          int floor, String roomType, String name, String description,
                          Integer capacity, Double latitude, Double longitude) {}
    public record NavigationEdgeDto(Long id, Long fromRoomId, Long toRoomId,
                                     Long fromBuildingId, Long toBuildingId,
                                     double distanceMeters, boolean accessible) {}
    public record NavigationResultDto(List<NavigationEdgeDto> edges, double totalDistanceMeters) {}
}
