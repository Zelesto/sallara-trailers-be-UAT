// src/main/java/com/pgsa/trailers/service/LeaveService.java
package com.pgsa.trailers.service;

import com.pgsa.trailers.dto.LeaveRequestDTO;
import com.pgsa.trailers.entity.assets.Driver;
import com.pgsa.trailers.entity.attendance.LeaveBalance;
import com.pgsa.trailers.entity.attendance.LeaveRequest;
import com.pgsa.trailers.entity.attendance.LeaveType;
import com.pgsa.trailers.repository.DriverRepository;
import com.pgsa.trailers.repository.LeaveBalanceRepository;
import com.pgsa.trailers.repository.LeaveRequestRepository;
import com.pgsa.trailers.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public LeaveRequest requestLeave(LeaveRequestDTO request) {
        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new RuntimeException("Leave type not found"));

        // Calculate duration
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        // Check for overlapping leave
        List<LeaveRequest> overlapping = leaveRequestRepository.findOverlappingApprovedLeave(
                driver.getId(), request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new RuntimeException("Leave request overlaps with existing approved leave");
        }

        // Check balance
        LeaveBalance balance = leaveBalanceRepository
                .findByDriverIdAndLeaveTypeIdAndYear(driver.getId(), leaveType.getId(), request.getStartDate().getYear())
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        if (balance.getRemainingDays().compareTo(BigDecimal.valueOf(days)) < 0) {
            throw new RuntimeException("Insufficient leave balance");
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .driver(driver)
                .leaveType(leaveType)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .durationDays((int) days)
                .status("PENDING")
                .reason(request.getReason())
                .notes(request.getNotes())
                .requestedAt(LocalDateTime.now())
                .build();

        // Update pending days in balance
        balance.setPendingDays(balance.getPendingDays().add(BigDecimal.valueOf(days)));
        balance.setRemainingDays(balance.getRemainingDays().subtract(BigDecimal.valueOf(days)));
        leaveBalanceRepository.save(balance);

        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    public LeaveRequest approveLeave(Long leaveRequestId, Long approverId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new RuntimeException("Leave request is not pending");
        }

        leaveRequest.setStatus("APPROVED");
        leaveRequest.setApprovedBy(approverId);
        leaveRequest.setApprovedAt(LocalDateTime.now());

        // Update balance - move from pending to used
        LeaveBalance balance = leaveBalanceRepository
                .findByDriverIdAndLeaveTypeIdAndYear(
                        leaveRequest.getDriver().getId(),
                        leaveRequest.getLeaveType().getId(),
                        leaveRequest.getStartDate().getYear())
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setPendingDays(balance.getPendingDays().subtract(BigDecimal.valueOf(leaveRequest.getDurationDays())));
        balance.setUsedDays(balance.getUsedDays().add(BigDecimal.valueOf(leaveRequest.getDurationDays())));
        leaveBalanceRepository.save(balance);

        return leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    public LeaveRequest rejectLeave(Long leaveRequestId, String reason) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!"PENDING".equals(leaveRequest.getStatus())) {
            throw new RuntimeException("Leave request is not pending");
        }

        leaveRequest.setStatus("REJECTED");
        leaveRequest.setRejectionReason(reason);

        // Return days to balance
        LeaveBalance balance = leaveBalanceRepository
                .findByDriverIdAndLeaveTypeIdAndYear(
                        leaveRequest.getDriver().getId(),
                        leaveRequest.getLeaveType().getId(),
                        leaveRequest.getStartDate().getYear())
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setPendingDays(balance.getPendingDays().subtract(BigDecimal.valueOf(leaveRequest.getDurationDays())));
        balance.setRemainingDays(balance.getRemainingDays().add(BigDecimal.valueOf(leaveRequest.getDurationDays())));
        leaveBalanceRepository.save(balance);

        return leaveRequestRepository.save(leaveRequest);
    }

    public List<LeaveRequest> getLeaveRequestsByDriver(Long driverId) {
        return leaveRequestRepository.findByDriverId(driverId);
    }

    public List<LeaveBalance> getLeaveBalances(Long driverId) {
        return leaveBalanceRepository.findByDriverId(driverId);
    }
}
