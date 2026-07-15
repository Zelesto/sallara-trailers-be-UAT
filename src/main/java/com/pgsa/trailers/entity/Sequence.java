// src/main/java/com/pgsa/trailers/entity/Sequence.java
package com.pgsa.trailers.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sequence", indexes = {
        @Index(name = "idx_sequence_table_name", columnList = "table_name", unique = true),
        @Index(name = "idx_sequence_year", columnList = "year")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, unique = true, length = 50)
    private String tableName;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "next_number", nullable = false)
    private Long nextNumber;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (year == null) {
            year = LocalDateTime.now().getYear();
        }
        if (nextNumber == null) {
            nextNumber = 1L;
        }
    }
}
