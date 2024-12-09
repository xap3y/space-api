package me.xap3y.space.repository;

import me.xap3y.space.entity.InviteCode;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InviteCodeRepository extends JpaRepository<InviteCode, Long> {

    Optional<InviteCode> findByCode(String code);

    boolean existsByCode(String code);

    void deleteByCode(String code);

    @Modifying
    @Transactional
    @Query("UPDATE InviteCode e " +
            "SET e.used = true, e.usedAt = :usedAt, e.usedBy = :usedBy " +
            "WHERE e.code = :code")
    int markAsUsed(@Param("code") String code,
                   @Param("usedAt") LocalDateTime usedAt,
                   @Param("usedBy") User usedBy);
}
