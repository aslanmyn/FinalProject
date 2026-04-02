package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampusMapServiceTest {

    @Mock private CampusBuildingRepository campusBuildingRepository;
    @Mock private CampusRoomRepository campusRoomRepository;
    @Mock private CampusNavigationEdgeRepository navigationEdgeRepository;

    @InjectMocks
    private CampusMapService campusMapService;

    // =========================================================================
    // Buildings
    // =========================================================================

    @Test
    @DisplayName("getAllBuildings - returns all buildings")
    void getAllBuildings() {
        CampusBuilding b1 = CampusBuilding.builder().id(1L).name("Main Hall").build();
        CampusBuilding b2 = CampusBuilding.builder().id(2L).name("Library").build();
        when(campusBuildingRepository.findAll()).thenReturn(List.of(b1, b2));

        List<CampusBuilding> result = campusMapService.getAllBuildings();
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getBuilding - throws when not found")
    void getBuilding_notFound() {
        when(campusBuildingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> campusMapService.getBuilding(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // Rooms
    // =========================================================================

    @Test
    @DisplayName("searchRooms - delegates to repository")
    void searchRooms() {
        CampusBuilding building = CampusBuilding.builder().id(1L).name("IT Faculty").build();
        CampusRoom room = CampusRoom.builder().id(1L).roomNumber("402").building(building).floor(4).build();
        when(campusRoomRepository.searchByQuery("402")).thenReturn(List.of(room));

        List<CampusRoom> result = campusMapService.searchRooms("402");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomNumber()).isEqualTo("402");
    }

    // =========================================================================
    // Navigation - Dijkstra
    // =========================================================================

    @Test
    @DisplayName("findRoute - same room returns empty path")
    void findRoute_sameRoom() {
        CampusMapService.NavigationResult result = campusMapService.findRoute(1L, 1L);
        assertThat(result.edges()).isEmpty();
        assertThat(result.totalDistanceMeters()).isEqualTo(0);
    }

    @Test
    @DisplayName("findRoute - finds shortest path through graph")
    void findRoute_shortest() {
        CampusRoom r1 = CampusRoom.builder().id(1L).build();
        CampusRoom r2 = CampusRoom.builder().id(2L).build();
        CampusRoom r3 = CampusRoom.builder().id(3L).build();

        // r1 -> r2 (50m), r2 -> r3 (30m), r1 -> r3 (100m)
        CampusNavigationEdge e1 = CampusNavigationEdge.builder().id(1L)
                .fromRoom(r1).toRoom(r2).distanceMeters(50).accessible(true).build();
        CampusNavigationEdge e2 = CampusNavigationEdge.builder().id(2L)
                .fromRoom(r2).toRoom(r3).distanceMeters(30).accessible(true).build();
        CampusNavigationEdge e3 = CampusNavigationEdge.builder().id(3L)
                .fromRoom(r1).toRoom(r3).distanceMeters(100).accessible(true).build();

        when(navigationEdgeRepository.findAllAccessible()).thenReturn(List.of(e1, e2, e3));

        CampusMapService.NavigationResult result = campusMapService.findRoute(1L, 3L);

        // Shortest: r1 -> r2 (50) -> r3 (30) = 80m, not the direct r1 -> r3 (100m)
        assertThat(result.totalDistanceMeters()).isEqualTo(80.0);
        assertThat(result.edges()).hasSize(2);
    }

    @Test
    @DisplayName("findRoute - returns -1 distance when no path exists")
    void findRoute_noPath() {
        CampusRoom r1 = CampusRoom.builder().id(1L).build();
        CampusRoom r2 = CampusRoom.builder().id(2L).build();
        // r1 and r2 connected, but r3 is isolated
        CampusNavigationEdge e1 = CampusNavigationEdge.builder().id(1L)
                .fromRoom(r1).toRoom(r2).distanceMeters(50).accessible(true).build();
        when(navigationEdgeRepository.findAllAccessible()).thenReturn(List.of(e1));

        CampusMapService.NavigationResult result = campusMapService.findRoute(1L, 99L);
        assertThat(result.totalDistanceMeters()).isEqualTo(-1);
        assertThat(result.edges()).isEmpty();
    }
}
