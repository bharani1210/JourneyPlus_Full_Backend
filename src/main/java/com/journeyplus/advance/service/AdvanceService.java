package com.journeyplus.advance.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.journeyplus.advance.entity.AdvanceRequest;
import com.journeyplus.advance.entity.AdvanceSettlement;
import com.journeyplus.advance.entity.AdvanceStatus;
import com.journeyplus.advance.repository.AdvanceRequestRepository;
import com.journeyplus.advance.repository.AdvanceSettlementRepository;
import com.journeyplus.config.AuditAction;
import com.journeyplus.event.StatusChangeEvent;
import com.journeyplus.iam.entity.User;

@Service
public class AdvanceService {

    @Autowired
    private AdvanceRequestRepository advanceRequestRepository;

    @Autowired
    private AdvanceSettlementRepository advanceSettlementRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    @AuditAction(module = "ADVANCE", action = "CREATE_ADVANCE")
    public AdvanceRequest createAdvanceRequest(AdvanceRequest request) {
        request.setStatus(AdvanceStatus.REQUESTED);
        request.setRequestDate(LocalDate.now());
        AdvanceRequest saved = advanceRequestRepository.save(request);

        // Notify Approving Manager
        if (request.getTripRequest().getApprovingManager() != null) {
            eventPublisher.publishEvent(new StatusChangeEvent(
                request.getTripRequest().getApprovingManager().getId(),
                "New Advance Request",
                "A cash advance of " + request.getAmount() + " " + request.getOriginalCurrency() + 
                " has been requested by " + request.getEmployee().getUsername() + ".",
                request.getEmployee() != null ? request.getEmployee().getId() : null,
                request.getEmployee() != null ? request.getEmployee().getUsername() : null
            ));
        }

        return saved;
    }

    @Transactional
    @AuditAction(module = "ADVANCE", action = "APPROVE_ADVANCE")
    public AdvanceRequest approveAdvanceRequest(Long id, User approver) {
        AdvanceRequest request = advanceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance request not found"));

        if (request.getStatus() != AdvanceStatus.REQUESTED) {
            throw new IllegalStateException("Only REQUESTED advance requests can be approved");
        }

        request.setStatus(AdvanceStatus.APPROVED);
        request.setApprover(approver);
        AdvanceRequest saved = advanceRequestRepository.save(request);

        // Notify Employee (actor = approver)
        eventPublisher.publishEvent(new StatusChangeEvent(
            request.getEmployee().getId(),
            "Advance Request Approved",
            "Your cash advance request for " + request.getAmount() + " " + request.getOriginalCurrency() + 
            " has been approved by " + approver.getUsername() + ".",
            approver != null ? approver.getId() : null,
            approver != null ? approver.getUsername() : null
        ));

        return saved;
    }

    @Transactional
    @AuditAction(module = "ADVANCE", action = "DISBURSE_ADVANCE")
    public AdvanceRequest disburseAdvanceRequest(Long id) {
        AdvanceRequest request = advanceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance request not found"));

        if (request.getStatus() != AdvanceStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED advance requests can be disbursed");
        }

        request.setStatus(AdvanceStatus.DISBURSED);
        request.setDisbursementDate(LocalDate.now());
        AdvanceRequest saved = advanceRequestRepository.save(request);

        // Notify Employee
        eventPublisher.publishEvent(new StatusChangeEvent(
            request.getEmployee().getId(),
            "Advance Disbursed",
            "Your cash advance of " + request.getAmount() + " " + request.getOriginalCurrency() + 
            " has been disbursed to your account.",
            null,
            null
        ));

        return saved;
    }

    @Transactional
    @AuditAction(module = "ADVANCE", action = "SETTLE_ADVANCE")
    public AdvanceRequest settleAdvanceRequest(Long id, AdvanceSettlement settlement) {
        AdvanceRequest request = advanceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance request not found"));

        if (request.getStatus() != AdvanceStatus.DISBURSED) {
            throw new IllegalStateException("Only DISBURSED advance requests can be settled");
        }

        request.setStatus(AdvanceStatus.SETTLED);
        AdvanceRequest savedRequest = advanceRequestRepository.save(request);

        settlement.setAdvanceRequest(savedRequest);
        settlement.setSettlementDate(LocalDate.now());
        settlement.setStatus("SETTLED");
        advanceSettlementRepository.save(settlement);

        // Notify Employee
        eventPublisher.publishEvent(new StatusChangeEvent(
            request.getEmployee().getId(),
            "Advance Request Settled",
            "Your cash advance of " + request.getAmount() + " " + request.getOriginalCurrency() + 
            " has been successfully settled.",
            null,
            null
        ));

        return savedRequest;
    }

    @Transactional
    @AuditAction(module = "ADVANCE", action = "FORFEIT_ADVANCE")
    public AdvanceRequest forfeitAdvanceRequest(Long id) {
        AdvanceRequest request = advanceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance request not found"));

        if (request.getStatus() != AdvanceStatus.APPROVED && request.getStatus() != AdvanceStatus.DISBURSED) {
            throw new IllegalStateException("Only APPROVED or DISBURSED advance requests can be forfeited");
        }

        request.setStatus(AdvanceStatus.FORFEITED);
        AdvanceRequest saved = advanceRequestRepository.save(request);

        // Notify Employee
        eventPublisher.publishEvent(new StatusChangeEvent(
            request.getEmployee().getId(),
            "Advance Request Forfeited",
            "Your cash advance request of " + request.getAmount() + " " + request.getOriginalCurrency() + 
            " has been forfeited.",
            null,
            null
        ));

        return saved;
    }

    public AdvanceRequest getAdvanceRequest(Long id) {
        return advanceRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Advance request not found"));
    }

    public List<AdvanceRequest> getAdvancesByEmployee(Long employeeId) {
        return advanceRequestRepository.findByEmployee_Id(employeeId);
    }
}
