package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.TelegramConnectCodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramConnectCodesRepository extends JpaRepository<TelegramConnectCodes, Long> {

    Optional<TelegramConnectCodes> findByCode(String code);

    List<TelegramConnectCodes> findByUser_Id(Long userId);

    @Transactional
    void deleteByCode(String code);
}
