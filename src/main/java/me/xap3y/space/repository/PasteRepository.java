package me.xap3y.space.repository;

import me.xap3y.space.entity.Paste;
import me.xap3y.space.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasteRepository extends JpaRepository<Paste, Long> {

    Optional<Paste> findByUniqueId(String uniqueId);

    Optional<List<Paste>> findByCreatedBy(User createdBy);
}
