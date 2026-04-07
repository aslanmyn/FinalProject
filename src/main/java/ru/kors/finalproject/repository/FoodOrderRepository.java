package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.FoodOrder;

import java.util.List;
import java.util.Optional;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    @Query("""
            SELECT DISTINCT o
            FROM FoodOrder o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.foodItem
            WHERE o.student.id = :studentId
            ORDER BY o.createdAt DESC
            """)
    List<FoodOrder> findByStudentIdWithItems(Long studentId);

    @Query("""
            SELECT DISTINCT o
            FROM FoodOrder o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.foodItem
            WHERE o.id = :id AND o.student.id = :studentId
            """)
    Optional<FoodOrder> findByIdAndStudentId(Long id, Long studentId);

    @Query("""
            SELECT DISTINCT o
            FROM FoodOrder o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.foodItem
            WHERE o.id = :id
            """)
    Optional<FoodOrder> findByIdWithItems(Long id);
}
