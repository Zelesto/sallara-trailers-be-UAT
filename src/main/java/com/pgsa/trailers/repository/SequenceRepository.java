// src/main/java/com/pgsa/trailers/repository/SequenceRepository.java
package com.pgsa.trailers.repository;

import com.pgsa.trailers.entity.Sequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SequenceRepository extends JpaRepository<Sequence, Long> {

     Optional<Sequence> findByTableNameAndYear(String tableName, String year);  
    
    List<Sequence> findByTableName(String tableName);
    
    // Optional: Find by year only
    List<Sequence> findByYear(String year);
    
    // Optional: Delete by year
    void deleteByYear(String year);
}
