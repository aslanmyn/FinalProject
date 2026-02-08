package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.News;

import java.util.List;

public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByOrderByCreatedAtDesc();
}
