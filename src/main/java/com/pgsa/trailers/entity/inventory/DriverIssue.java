// src/main/java/com/pgsa/trailers/entity/inventory/DriverIssue.java
package com.pgsa.trailers.entity.inventory;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "driver_issues")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_number", unique = true, nullable = false, length = 50)
    private String issueNumber;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "issue_date")
    private LocalDateTime issueDate;

    @Column(name = "status", length = 20)
    private String status = "ISSUED";

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DriverIssueItem> issueItems = new ArrayList<>();

    public void addIssueItem(DriverIssueItem item) {
        issueItems.add(item);
        item.setIssue(this);
    }

    public void removeIssueItem(DriverIssueItem item) {
        issueItems.remove(item);
        item.setIssue(null);
    }
}
