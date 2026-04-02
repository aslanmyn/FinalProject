package ru.kors.finalproject.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.CampusNavigationEdge;
import ru.kors.finalproject.entity.CampusRoom;
import ru.kors.finalproject.repository.CampusBuildingRepository;
import ru.kors.finalproject.repository.CampusNavigationEdgeRepository;
import ru.kors.finalproject.repository.CampusRoomRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusMapServiceTest {

    @Mock
    private CampusBuildingRepository campusBuildingRepository;
    @Mock
    private CampusRoomRepository campusRoomRepository;
    @Mock
    private CampusNavigationEdgeRepository navigationEdgeRepository;

    @InjectMocks
    private CampusMapService campusMapService;

    @Test
    @DisplayName("findRoute returns zero distance for the same room")
    void findRoute_sameRoom() {
        CampusMapService.NavigationResult result = campusMapService.findRoute(10L, 10L);

        assertThat(result.totalDistanceMeters()).isZero();
        assertThat(result.edges()).isEmpty();
    }

    @Test
    @DisplayName("findRoute returns -1 when rooms are disconnected")
    void findRoute_noPath() {
        CampusRoom a = CampusRoom.builder().id(1L).roomNumber("L-101").build();
        CampusRoom b = CampusRoom.builder().id(2L).roomNumber("LIB-1").build();
        CampusNavigationEdge edge = CampusNavigationEdge.builder()
                .id(100L)
                .fromRoom(a)
                .toRoom(b)
                .distanceMeters(50)
                .accessible(true)
                .build();

        when(navigationEdgeRepository.findAllAccessible()).thenReturn(List.of(edge));

        CampusMapService.NavigationResult result = campusMapService.findRoute(1L, 99L);

        assertThat(result.totalDistanceMeters()).isEqualTo(-1);
        assertThat(result.edges()).isEmpty();
    }

    @Test
    @DisplayName("findRoute chooses the shortest accessible path")
    void findRoute_shortestPath() {
        CampusRoom a = CampusRoom.builder().id(1L).roomNumber("L-101").build();
        CampusRoom b = CampusRoom.builder().id(2L).roomNumber("L-202").build();
        CampusRoom c = CampusRoom.builder().id(3L).roomNumber("LIB-1").build();

        CampusNavigationEdge direct = CampusNavigationEdge.builder()
                .id(200L)
                .fromRoom(a)
                .toRoom(c)
                .distanceMeters(120)
                .accessible(true)
                .build();
        CampusNavigationEdge firstHop = CampusNavigationEdge.builder()
                .id(201L)
                .fromRoom(a)
                .toRoom(b)
                .distanceMeters(40)
                .accessible(true)
                .build();
        CampusNavigationEdge secondHop = CampusNavigationEdge.builder()
                .id(202L)
                .fromRoom(b)
                .toRoom(c)
                .distanceMeters(35)
                .accessible(true)
                .build();

        when(navigationEdgeRepository.findAllAccessible()).thenReturn(List.of(direct, firstHop, secondHop));

        CampusMapService.NavigationResult result = campusMapService.findRoute(1L, 3L);

        assertThat(result.totalDistanceMeters()).isEqualTo(75);
        assertThat(result.edges()).extracting(CampusNavigationEdge::getId).containsExactly(201L, 202L);
    }
}
