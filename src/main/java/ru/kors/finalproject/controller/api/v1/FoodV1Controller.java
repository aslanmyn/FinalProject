package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.service.FoodOrderService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/student/food")
@RequiredArgsConstructor
@Tag(name = "Student Food", description = "Canteen categories, menu items, and student food ordering.")
@SecurityRequirement(name = "Bearer")
public class FoodV1Controller {

    private final FoodOrderService foodOrderService;
    private final CurrentUserHelper currentUserHelper;

    // ===== Menu =====

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(foodOrderService.getAllCategories().stream()
                .map(c -> new CategoryDto(c.getId(), c.getName(), c.getIcon(), c.getSortOrder()))
                .toList());
    }

    @GetMapping("/items")
    public ResponseEntity<?> getItems(@RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(foodOrderService.getItems(categoryId).stream()
                .map(this::toItemDto)
                .toList());
    }

    @GetMapping("/items/popular")
    public ResponseEntity<?> getPopularItems() {
        return ResponseEntity.ok(foodOrderService.getPopularItems().stream()
                .map(this::toItemDto)
                .toList());
    }

    // ===== Orders =====

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(foodOrderService.getStudentOrders(student.getId()).stream()
                .map(this::toOrderDto)
                .toList());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toOrderDto(foodOrderService.getOrder(id, student.getId())));
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal User user,
            @RequestBody CreateOrderBody body) {
        Student student = currentUserHelper.requireStudent(user);
        FoodOrder order = foodOrderService.createOrder(student, body.items(), body.note(), body.pickupTime());
        return ResponseEntity.ok(toOrderDto(order));
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@AuthenticationPrincipal User user, @PathVariable Long id) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toOrderDto(foodOrderService.cancelOrder(id, student.getId())));
    }

    // ===== DTOs =====

    private ItemDto toItemDto(FoodItem item) {
        return new ItemDto(item.getId(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getName(), item.getDescription(), item.getPrice(),
                item.getImageUrl(), item.isPopular());
    }

    private OrderDto toOrderDto(FoodOrder order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getId(),
                        i.getFoodItem() != null ? i.getFoodItem().getId() : null,
                        i.getFoodItem() != null ? i.getFoodItem().getName() : null,
                        i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderDto(order.getId(), order.getStatus().name(), order.getTotalAmount(),
                items, order.getNote(), order.getPickupTime(),
                order.getCreatedAt(), order.getUpdatedAt());
    }

    public record CategoryDto(Long id, String name, String icon, int sortOrder) {}
    public record ItemDto(Long id, Long categoryId, String name, String description,
                          BigDecimal price, String imageUrl, boolean popular) {}
    public record OrderDto(Long id, String status, BigDecimal totalAmount,
                           List<OrderItemDto> items, String note, Instant pickupTime,
                           Instant createdAt, Instant updatedAt) {}
    public record OrderItemDto(Long id, Long foodItemId, String foodItemName,
                               int quantity, BigDecimal unitPrice) {}
    public record CreateOrderBody(Map<Long, Integer> items, String note, Instant pickupTime) {}
}
