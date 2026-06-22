// src/main/java/com/pgsa/trailers/controller/InventoryController.java
package com.pgsa.trailers.controller;

import com.pgsa.trailers.dto.*;
import com.pgsa.trailers.entity.inventory.StockMovement;
import com.pgsa.trailers.service.StockCountService;
import com.pgsa.trailers.service.InventoryItemService;
import com.pgsa.trailers.service.InventoryLocationService;
import com.pgsa.trailers.service.StockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'DISPATCHER')")
public class InventoryController {

    private final InventoryItemService inventoryItemService;
    private final InventoryLocationService inventoryLocationService;
    private final StockMovementService stockMovementService;
    private final StockCountService stockCountService;

    // =============================================
    // Inventory Items
    // =============================================

    @GetMapping("/items")
    public ResponseEntity<Page<InventoryItemResponseDTO>> getAllItems(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(inventoryItemService.getAllItems(pageable));
    }

    @GetMapping("/items/search")
    public ResponseEntity<Page<InventoryItemResponseDTO>> searchItems(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(inventoryItemService.searchItems(search, pageable));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<InventoryItemResponseDTO> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryItemService.getItemById(id));
    }

    @GetMapping("/items/category/{category}")
    public ResponseEntity<List<InventoryItemResponseDTO>> getItemsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(inventoryItemService.getItemsByCategory(category));
    }

    @GetMapping("/items/location/{locationId}")
    public ResponseEntity<List<InventoryItemResponseDTO>> getItemsByLocation(@PathVariable Long locationId) {
        return ResponseEntity.ok(inventoryItemService.getItemsByLocation(locationId));
    }

    @GetMapping("/items/consumable")
    public ResponseEntity<List<InventoryItemResponseDTO>> getConsumableItems() {
        return ResponseEntity.ok(inventoryItemService.getConsumableItems());
    }

    @PostMapping("/items")
    public ResponseEntity<InventoryItemResponseDTO> createItem(@Valid @RequestBody InventoryItemRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryItemService.createItem(request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<InventoryItemResponseDTO> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody InventoryItemRequestDTO request) {
        return ResponseEntity.ok(inventoryItemService.updateItem(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        inventoryItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Inventory Locations
    // =============================================

    @GetMapping("/locations")
    public ResponseEntity<List<InventoryLocationResponseDTO>> getAllLocations() {
        return ResponseEntity.ok(inventoryLocationService.getAllLocations());
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<InventoryLocationResponseDTO> getLocationById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryLocationService.getLocationById(id));
    }

    @PostMapping("/locations")
    public ResponseEntity<InventoryLocationResponseDTO> createLocation(
            @Valid @RequestBody InventoryLocationRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryLocationService.createLocation(request));
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<InventoryLocationResponseDTO> updateLocation(
            @PathVariable Long id,
            @Valid @RequestBody InventoryLocationRequestDTO request) {
        return ResponseEntity.ok(inventoryLocationService.updateLocation(id, request));
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        inventoryLocationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Stock Movements (Existing)
    // =============================================

    @PostMapping("/recordMovement")
    public ResponseEntity<StockMovementResponseDTO> recordMovement(@Valid @RequestBody StockMovementRequestDTO request) {
        log.info("Recording stock movement: {}", request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockMovementService.recordMovement(request));
    }

    @GetMapping("/movements/item/{itemId}")
    public ResponseEntity<Page<StockMovementResponseDTO>> getMovementsByItem(
            @PathVariable Long itemId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockMovementService.getMovementsByItem(itemId, pageable));
    }

    @GetMapping("/movements/trip/{tripId}")
    public ResponseEntity<List<StockMovementResponseDTO>> getMovementsByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(stockMovementService.getMovementsByTrip(tripId));
    }

    @GetMapping("/movements/fuel-slip/{fuelSlipId}")
    public ResponseEntity<List<StockMovementResponseDTO>> getMovementsByFuelSlip(@PathVariable Long fuelSlipId) {
        return ResponseEntity.ok(stockMovementService.getMovementsByFuelSlip(fuelSlipId));
    }

    // =============================================
    // Shrinkage Reports (Existing)
    // =============================================

    @GetMapping("/shrinkage/{id}")
    public ResponseEntity<InventoryVarianceDTO> getShrinkage(@PathVariable Long id) {
        log.info("Getting shrinkage report for item: {}", id);
        return ResponseEntity.ok(stockCountService.getShrinkageReport(id));
    }

    // =============================================
    // Statistics
    // =============================================

    @GetMapping("/stats")
    public ResponseEntity<InventoryStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(inventoryItemService.getStatistics());
    }
}
