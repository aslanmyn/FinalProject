package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodOrderServiceTest {

    @Mock private FoodCategoryRepository foodCategoryRepository;
    @Mock private FoodItemRepository foodItemRepository;
    @Mock private FoodOrderRepository foodOrderRepository;

    @InjectMocks
    private FoodOrderService foodOrderService;

    private Student student;
    private FoodCategory category;
    private FoodItem item1;
    private FoodItem item2;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(1L).email("test@kbtu.kz").name("Test Student").build();
        category = FoodCategory.builder().id(1L).name("Lunch").build();
        item1 = FoodItem.builder().id(10L).name("Mediterranean Bowl").category(category)
                .price(new BigDecimal("5500")).available(true).build();
        item2 = FoodItem.builder().id(11L).name("Classic Margherita").category(category)
                .price(new BigDecimal("4500")).available(true).build();
    }

    // =========================================================================
    // createOrder
    // =========================================================================

    @Test
    @DisplayName("createOrder - creates order with correct total")
    void createOrder_success() {
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(item1));
        when(foodItemRepository.findById(11L)).thenReturn(Optional.of(item2));
        when(foodOrderRepository.save(any(FoodOrder.class))).thenAnswer(inv -> {
            FoodOrder o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        Map<Long, Integer> items = Map.of(10L, 2, 11L, 1);
        FoodOrder result = foodOrderService.createOrder(student, items, "No onions", null);

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(FoodOrder.OrderStatus.PENDING);
        // 5500*2 + 4500*1 = 15500
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("15500"));
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getNote()).isEqualTo("No onions");
    }

    @Test
    @DisplayName("createOrder - throws when items is empty")
    void createOrder_emptyItems() {
        assertThatThrownBy(() -> foodOrderService.createOrder(student, Map.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    @DisplayName("createOrder - throws when item not found")
    void createOrder_itemNotFound() {
        when(foodItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> foodOrderService.createOrder(student, Map.of(99L, 1), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("createOrder - throws when item is unavailable")
    void createOrder_itemUnavailable() {
        item1.setAvailable(false);
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(item1));

        assertThatThrownBy(() -> foodOrderService.createOrder(student, Map.of(10L, 1), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    // =========================================================================
    // cancelOrder
    // =========================================================================

    @Test
    @DisplayName("cancelOrder - cancels pending order")
    void cancelOrder_success() {
        FoodOrder order = FoodOrder.builder().id(100L).student(student)
                .status(FoodOrder.OrderStatus.PENDING).totalAmount(BigDecimal.ZERO).build();

        when(foodOrderRepository.findByIdAndStudentId(100L, 1L)).thenReturn(Optional.of(order));
        when(foodOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FoodOrder result = foodOrderService.cancelOrder(100L, 1L);
        assertThat(result.getStatus()).isEqualTo(FoodOrder.OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelOrder - throws when order is not pending")
    void cancelOrder_notPending() {
        FoodOrder order = FoodOrder.builder().id(100L).student(student)
                .status(FoodOrder.OrderStatus.CONFIRMED).totalAmount(BigDecimal.ZERO).build();

        when(foodOrderRepository.findByIdAndStudentId(100L, 1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> foodOrderService.cancelOrder(100L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending");
    }

    @Test
    @DisplayName("cancelOrder - throws when order not found")
    void cancelOrder_notFound() {
        when(foodOrderRepository.findByIdAndStudentId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> foodOrderService.cancelOrder(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
