package me.xap3y.space.repository;

import me.xap3y.space.entity.InboundEmail;
import me.xap3y.space.entity.TempMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InboundMailRepository extends JpaRepository<InboundEmail, Long> {

    Optional<InboundEmail> findByTempMail(TempMail mail);

    Optional<InboundEmail> findByTempMail_Id(Long tempMailId);

    Optional<InboundEmail> findByTempMail_Email(String tempMailEmail);
}
