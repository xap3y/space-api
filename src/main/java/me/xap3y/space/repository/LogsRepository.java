package me.xap3y.space.repository;

import me.xap3y.space.entity.LogRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogsRepository extends JpaRepository<LogRecord, Long> {
}
