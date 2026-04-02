package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.FoodItem;

import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findByCategoryIdAndAvailableTrue(Long categoryId);

    List<FoodItem> findByAvailableTrue();

    List<FoodItem> findByPopularTrueAndAvailableTrue();
}
