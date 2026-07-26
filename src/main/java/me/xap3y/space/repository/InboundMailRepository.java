package me.xap3y.space.repository;

import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InboundMailRepository extends JpaRepository<InboundEmail, Long> {

    Optional<InboundEmail> findByTempMail(TempMail mail);

    Optional<InboundEmail> findByTempMail_Id(Long tempMailId);

    Optional<InboundEmail> findByTempMail_Email(String tempMailEmail);

    List<InboundEmail> findAllByTempMail(TempMail tempMail);

    List<InboundEmail> findTop20ByTempMailOrderBySentDateDesc(TempMail tempMail);

    /** Delete a specific inbound message only if it belongs to the given temp mail email (safe scoped delete). */
    void deleteByIdAndTempMail_Email(Long id, String tempMailEmail);

    /** Delete all inbound messages belonging to a temp mail address. */
    void deleteAllByTempMail_Email(String tempMailEmail);
}
