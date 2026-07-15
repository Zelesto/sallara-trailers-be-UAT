// src/main/java/com/pgsa/trailers/repository/SequenceRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.Sequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SequenceRepository extends JpaRepository<Sequence, Long> {

    Optional<Sequence> findByTableNameAndYear(String tableName, Integer year);

    @Modifying
    @Transactional
    @Query("UPDATE Sequence s SET s.nextNumber = s.nextNumber + 1 WHERE s.tableName = :tableName AND s.year = :year")
    int incrementNextNumber(@Param("tableName") String tableName, @Param("year") Integer year);

    @Query("SELECT s.nextNumber FROM Sequence s WHERE s.tableName = :tableName AND s.year = :year")
    Optional<Long> findNextNumberByTableNameAndYear(@Param("tableName") String tableName, @Param("year") Integer year);
}
