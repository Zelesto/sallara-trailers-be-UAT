// src/main/java/com/pgsa/trailers/controller/PodStatistics.java
package com.pgsa.trailers.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodStatistics {
    private long total;
    private long pending;
    private long delivered;
    private long verified;
    private long rejected;
}
