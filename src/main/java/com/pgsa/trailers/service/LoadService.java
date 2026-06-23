// src/main/java/com/pgsa/trailers/service/LoadService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.LoadRequestDTO;
import com.pgsa.trailers.dto.LoadResponseDTO;
import com.pgsa.trailers.dto.TripSummaryDTO;
import com.pgsa.trailers.entity.ops.Load;
import com.pgsa.trailers.entity.ops.Trip;
import com.pgsa.trailers.repository.CustomerRepository;
import com.pgsa.trailers.repository.LoadRepository;
import com.pgsa.trailers.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoadService {

    private final LoadRepository loadRepository;
    private final TripRepository tripRepository;
    private final CustomerRepository customerRepository;

    public LoadResponseDTO createLoad(LoadRequestDTO request) {
        // Check if load number already exists
        if (request.getLoadNumber() != null && 
            loadRepository.findByLoadNumber(request.getLoadNumber()).isPresent()) {
            throw new RuntimeException("Load number already exists: " + request.getLoadNumber());
        }

        // Validate customer exists if provided
        if (request.getCustomerId() != null && 
            !customerRepository.existsById(request.getCustomerId())) {
            throw new RuntimeException("Customer not found with ID: " + request.getCustomerId());
        }

        Load load = Load.builder()
                .loadNumber(request.getLoadNumber())
                .loadType(request.getLoadType())
                .description(request.getDescription())
                .customerId(request.getCustomerId())
                .status(request.getStatus())
                .priority(request.getPriority())
                .estimatedValue(request.getEstimatedValue())
                .actualValue(request.getActualValue())
                .notes(request.getNotes())
                .build();

        Load saved = loadRepository.save(load);
        log.info("Created load with ID: {}", saved.getId());
        return mapToResponseDTO(saved);
    }

    public LoadResponseDTO updateLoad(Long id, LoadRequestDTO request) {
        Load load = loadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Load not found with ID: " + id));

        if (request.getLoadNumber() != null && 
            !load.getLoadNumber().equals(request.getLoadNumber()) &&
            loadRepository.findByLoadNumber(request.getLoadNumber()).isPresent()) {
            throw new RuntimeException("Load number already exists: " + request.getLoadNumber());
        }

        load.setLoadNumber(request.getLoadNumber());
        load.setLoadType(request.getLoadType());
        load.setDescription(request.getDescription());
        load.setCustomerId(request.getCustomerId());
        load.setStatus(request.getStatus());
        load.setPriority(request.getPriority());
        load.setEstimatedValue(request.getEstimatedValue());
        load.setActualValue(request.getActualValue());
        load.setNotes(request.getNotes());

        Load updated = loadRepository.save(load);
        log.info("Updated load with ID: {}", updated.getId());
        return mapToResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public LoadResponseDTO getLoadById(Long id) {
        Load load = loadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Load not found with ID: " + id));
        return mapToResponseDTO(load);
    }

    @Transactional(readOnly = true)
    public LoadResponseDTO getLoadByNumber(String loadNumber) {
        Load load = loadRepository.findByLoadNumber(loadNumber)
                .orElseThrow(() -> new RuntimeException("Load not found with number: " + loadNumber));
        return mapToResponseDTO(load);
    }

    @Transactional(readOnly = true)
    public Page<LoadResponseDTO> getAllLoads(Pageable pageable) {
        return loadRepository.findAll(pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public Page<LoadResponseDTO> searchLoads(String search, Pageable pageable) {
        return loadRepository.searchLoads(search, pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<LoadResponseDTO> getLoadsByCustomer(Long customerId) {
        return loadRepository.findByCustomerId(customerId)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoadResponseDTO> getLoadsByStatus(String status) {
        return loadRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public void deleteLoad(Long id) {
        if (!loadRepository.existsById(id)) {
            throw new RuntimeException("Load not found with ID: " + id);
        }
        loadRepository.deleteById(id);
        log.info("Deleted load with ID: {}", id);
    }

    private LoadResponseDTO mapToResponseDTO(Load load) {
        Long tripCount = loadRepository.countTripsByLoad(load.getLoadNumber());
        
        // Get trips for this load
        List<Trip> trips = tripRepository.findByLoadId(load.getLoadNumber());
        List<TripSummaryDTO> tripSummaries = trips.stream()
                .map(trip -> TripSummaryDTO.builder()
                        .id(trip.getId())
                        .tripNumber(trip.getTripNumber())
                        .status(trip.getStatus())
                        .origin(trip.getOriginCity())
                        .destination(trip.getDestinationCity())
                        .build())
                .collect(Collectors.toList());

        String customerName = null;
        if (load.getCustomerId() != null) {
            customerRepository.findById(load.getCustomerId())
                    .ifPresent(customer -> customerName = customer.getName());
        }

        return LoadResponseDTO.builder()
                .id(load.getId())
                .loadNumber(load.getLoadNumber())
                .loadType(load.getLoadType())
                .description(load.getDescription())
                .customerId(load.getCustomerId())
                .customerName(customerName)
                .status(load.getStatus())
                .priority(load.getPriority())
                .estimatedValue(load.getEstimatedValue())
                .actualValue(load.getActualValue())
                .notes(load.getNotes())
                .createdAt(load.getCreatedAt())
                .createdBy(load.getCreatedBy())
                .updatedAt(load.getUpdatedAt())
                .updatedBy(load.getUpdatedBy())
                .tripCount(tripCount != null ? tripCount.intValue() : 0)
                .trips(tripSummaries)
                .build();
    }
}
