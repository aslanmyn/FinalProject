package ru.kors.finalproject.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.kors.finalproject.entity.FoodCategory;
import ru.kors.finalproject.entity.FoodItem;
import ru.kors.finalproject.entity.FoodOrder;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.repository.FoodCategoryRepository;
import ru.kors.finalproject.repository.FoodItemRepository;
import ru.kors.finalproject.repository.FoodOrderRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodOrderServiceTest {

    @Mock
    private FoodCategoryRepository foodCategoryRepository;
    @Mock
    private FoodItemRepository foodItemRepository;
    @Mock
    private FoodOrderRepository foodOrderRepository;

    @InjectMocks
    private FoodOrderService foodOrderService;

    private Student student;
    private FoodItem bowl;
    private FoodItem latte;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(15L)
                .email("a_student@kbtu.kz")
                .name("Student Example")
                .build();

        FoodCategory category = FoodCategory.builder().id(1L).name("Hot Meals").build();
        bowl = FoodItem.builder()
                .id(10L)
                .category(category)
                .name("Chicken Teriyaki Bowl")
                .price(BigDecimal.valueOf(2450))
                .available(true)
                .popular(true)
                .build();
        latte = FoodItem.builder()
                .id(11L)
                .category(FoodCategory.builder().id(2L).name("Drinks").build())
                .name("Iced Latte")
                .price(BigDecimal.valueOf(1050))
                .available(true)
                .popular(true)
                .build();
    }

    @Test
    @DisplayName("createOrder rejects empty carts")
    void createOrder_rejectsEmptyCart() {
        assertThatThrownBy(() -> foodOrderService.createOrder(student, Map.of(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    @DisplayName("createOrder rejects unavailable menu items")
    void createOrder_rejectsUnavailableItem() {
        FoodItem unavailable = FoodItem.builder()
                .id(99L)
                .name("Sold out soup")
                .price(BigDecimal.valueOf(900))
                .available(false)
                .build();
        when(foodItemRepository.findById(99L)).thenReturn(Optional.of(unavailable));

        assertThatThrownBy(() -> foodOrderService.createOrder(student, Map.of(99L, 1), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not available");
    }

    @Test
    @DisplayName("createOrder calculates total and stores all order lines")
    void createOrder_calculatesTotal() {
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(bowl));
        when(foodItemRepository.findById(11L)).thenReturn(Optional.of(latte));
        when(foodOrderRepository.save(any(FoodOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<Long, Integer> items = new LinkedHashMap<>();
        items.put(10L, 2);
        items.put(11L, 1);

        FoodOrder order = foodOrderService.createOrder(student, items, "No onions", Instant.parse("2026-04-03T12:30:00Z"));

        assertThat(order.getStatus()).isEqualTo(FoodOrder.OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("5950");
        assertThat(order.getNote()).isEqualTo("No onions");
    }

    @Test
    @DisplayName("cancelOrder only allows pending orders")
    void cancelOrder_onlyPending() {
        FoodOrder readyOrder = FoodOrder.builder()
                .id(44L)
                .student(student)
                .status(FoodOrder.OrderStatus.READY)
                .totalAmount(BigDecimal.valueOf(1050))
                .build();
        when(foodOrderRepository.findByIdAndStudentId(44L, student.getId())).thenReturn(Optional.of(readyOrder));

        assertThatThrownBy(() -> foodOrderService.cancelOrder(44L, student.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending");
    }
}
