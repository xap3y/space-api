package me.xap3y.space.repository;

import jakarta.transaction.Transactional;
import me.xap3y.space.entity.TempMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TempMailRepository extends JpaRepository<TempMail, Long> {

    Optional<TempMail> findByEmail(String email);

    boolean existsByEmail(String email);

    @Transactional
    void deleteByEmail(String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.SUSPENDED WHERE t.email = :email")
    void suspendEmail(@Param("email") String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.CLOSED WHERE t.email = :email")
    void closeEmail(@Param("email") String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.DELETED WHERE t.email = :email")
    void deleteEmail(@Param("email") String email);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TempMail t SET t.status = me.xap3y.space.api.enums.TempMailStatus.OPEN WHERE t.email = :email")
    void openMail(@Param("email") String email);
}
