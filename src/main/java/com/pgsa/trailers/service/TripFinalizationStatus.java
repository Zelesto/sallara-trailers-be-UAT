// src/main/java/com/pgsa/trailers/service/TripFinalizationStatus.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.enums.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripFinalizationStatus {
    private Long tripId;
    private String tripNumber;
    private TripStatus currentStatus;
    private boolean canBeFinalized;
    private boolean hasPods;
    private long podCount;
    private long invalidPods;
    private String message;
}
