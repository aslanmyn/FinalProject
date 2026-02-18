package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.RefreshToken;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);
}
