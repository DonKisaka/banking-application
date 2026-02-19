package com.banking_application.controller;

import com.banking_application.dto.FraudAlertResponseDto;
import com.banking_application.dto.ResolveAlertRequestDto;
import com.banking_application.exception.ResourceNotFoundException;
import com.banking_application.model.User;
import com.banking_application.repository.UserRepository;
import com.banking_application.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/fraud-alerts")
public class FraudAlertController {

    private final FraudDetectionService fraudDetectionService;
    private final UserRepository userRepository;

    public FraudAlertController(FraudDetectionService fraudDetectionService,
                                UserRepository userRepository) {
        this.fraudDetectionService = fraudDetectionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/pending")
    public List<FraudAlertResponseDto> getPendingAlerts() {
        return fraudDetectionService.getPendingAlerts();
    }

    @GetMapping
    public List<FraudAlertResponseDto> getAlertsByRiskScore(
            @RequestParam(defaultValue = "0") Integer minScore) {
        return fraudDetectionService.getAlertsByRiskScore(minScore);
    }

    @PutMapping("/{alertId}/resolve")
    public FraudAlertResponseDto resolveAlert(@PathVariable Long alertId,
                                              @Valid @RequestBody ResolveAlertRequestDto dto) {
        return fraudDetectionService.resolveAlert(
                alertId, dto.adminRemarks(), dto.actionTaken(), dto.newStatus()
        );
    }

    @GetMapping("/user/{userUuid}")
    public Page<FraudAlertResponseDto> getUserFraudHistory(@PathVariable UUID userUuid,
                                                           Pageable pageable) {
        User user = findUserOrThrow(userUuid);
        return fraudDetectionService.getUserFraudHistory(user, pageable);
    }

    @GetMapping("/user/{userUuid}/unresolved")
    public Map<String, Boolean> hasUnresolvedAlerts(@PathVariable UUID userUuid) {
        User user = findUserOrThrow(userUuid);
        return Map.of("hasUnresolvedAlerts", fraudDetectionService.hasUnresolvedAlerts(user));
    }

    private User findUserOrThrow(UUID userUuid) {
        return userRepository.findByUserUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userUuid", userUuid));
    }
}
