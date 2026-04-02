package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodOrderService {

    private final FoodCategoryRepository foodCategoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final FoodOrderRepository foodOrderRepository;

    public List<FoodCategory> getAllCategories() {
        return foodCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    public List<FoodItem> getItems(Long categoryId) {
        if (categoryId != null) {
            return foodItemRepository.findByCategoryIdAndAvailableTrue(categoryId);
        }
        return foodItemRepository.findByAvailableTrue();
    }

    public List<FoodItem> getPopularItems() {
        return foodItemRepository.findByPopularTrueAndAvailableTrue();
    }

    public List<FoodOrder> getStudentOrders(Long studentId) {
        return foodOrderRepository.findByStudentIdWithItems(studentId);
    }

    public FoodOrder getOrder(Long orderId, Long studentId) {
        return foodOrderRepository.findByIdAndStudentId(orderId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    /**
     * Create a new food order.
     * @param items map of foodItemId -> quantity
     */
    @Transactional
    public FoodOrder createOrder(Student student, Map<Long, Integer> items, String note, Instant pickupTime) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }

        FoodOrder order = FoodOrder.builder()
                .student(student)
                .status(FoodOrder.OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .note(note)
                .pickupTime(pickupTime)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            FoodItem foodItem = foodItemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Food item not found: " + entry.getKey()));

            if (!foodItem.isAvailable()) {
                throw new IllegalStateException("Item is not available: " + foodItem.getName());
            }

            int quantity = entry.getValue();
            if (quantity < 1) {
                throw new IllegalArgumentException("Quantity must be at least 1");
            }

            FoodOrderItem orderItem = FoodOrderItem.builder()
                    .order(order)
                    .foodItem(foodItem)
                    .quantity(quantity)
                    .unitPrice(foodItem.getPrice())
                    .build();
            order.getItems().add(orderItem);
            total = total.add(foodItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }

        order.setTotalAmount(total);
        return foodOrderRepository.save(order);
    }

    @Transactional
    public FoodOrder cancelOrder(Long orderId, Long studentId) {
        FoodOrder order = getOrder(orderId, studentId);
        if (order.getStatus() != FoodOrder.OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        order.setStatus(FoodOrder.OrderStatus.CANCELLED);
        order.setUpdatedAt(Instant.now());
        return foodOrderRepository.save(order);
    }
}
