package com.journeyplus.expense.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.journeyplus.compliance.service.PolicyComplianceEngine;
import com.journeyplus.config.AuditAction;
import com.journeyplus.event.StatusChangeEvent;
import com.journeyplus.expense.entity.ExpenseClaim;
import com.journeyplus.expense.entity.ExpenseLine;
import com.journeyplus.expense.entity.ExpenseStatus;
import com.journeyplus.expense.entity.Reimbursement;
import com.journeyplus.expense.repository.ExpenseClaimRepository;
import com.journeyplus.expense.repository.ExpenseLineRepository;
import com.journeyplus.expense.repository.ReimbursementRepository;
import com.journeyplus.iam.entity.User;
import com.journeyplus.expense.dto.ExpenseLineRequest;

@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    @Autowired
    private ExpenseClaimRepository expenseClaimRepository;

    @Autowired
    private ExpenseLineRepository expenseLineRepository;

    @Autowired
    private ReimbursementRepository reimbursementRepository;

    @Autowired
    private PolicyComplianceEngine complianceEngine;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // Standard static conversion rates for multi-currency conversion to USD
    private BigDecimal getExchangeRateToUsd(String currency) {
        if (currency == null) return BigDecimal.ONE;
        return switch (currency.toUpperCase()) {
            case "INR" -> new BigDecimal("0.012");
            case "EUR" -> new BigDecimal("1.08");
            case "GBP" -> new BigDecimal("1.25");
            case "JPY" -> new BigDecimal("0.0064");
            case "CAD" -> new BigDecimal("0.73");
            default -> BigDecimal.ONE;
        };
    }

    @Transactional
    @AuditAction(module = "EXPENSE", action = "CREATE_EXPENSE_CLAIM")
    public ExpenseClaim createExpenseClaim(ExpenseClaim claim) {
        return createExpenseClaim(claim, null);
    }

    @Transactional
    @AuditAction(module = "EXPENSE", action = "CREATE_EXPENSE_CLAIM")
    public ExpenseClaim createExpenseClaim(ExpenseClaim claim, List<ExpenseLineRequest> lineRequests) {
        log.info("Attempting to create expense claim with title: '{}'", claim.getClaimTitle());
        claim.setStatus(ExpenseStatus.DRAFT);
        claim.setTotalAmount(BigDecimal.ZERO);
        claim.setUsdEquivalent(BigDecimal.ZERO);
        ExpenseClaim saved = expenseClaimRepository.save(claim);
        log.info("Expense claim successfully created with ID: {}", saved.getId());

        if (lineRequests != null && !lineRequests.isEmpty()) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal totalUsdEquivalent = BigDecimal.ZERO;

            for (ExpenseLineRequest lineReq : lineRequests) {
                ExpenseLine line = new ExpenseLine();
                line.setExpenseDate(lineReq.getExpenseDate());
                line.setCategory(lineReq.getCategory());
                line.setAmount(lineReq.getAmount());
                line.setOriginalCurrency(lineReq.getOriginalCurrency());
                line.setReceiptPath(lineReq.getReceiptPath());

                // Defensive: do not trust client-supplied identifiers or compliance fields
                line.setId(null);
                // Always associate the persisted claim using path variable
                line.setExpenseClaim(saved);
                // Ensure a non-null policy compliance status before persisting to avoid DB NOT NULL constraint
                if (line.getPolicyComplianceStatus() == null) {
                    line.setPolicyComplianceStatus("COMPLIANT");
                }
                // Clear any client-supplied compliance remarks; compliance engine will set them as needed
                line.setComplianceRemarks(null);

                // Validate amount and currency
                if (line.getAmount() == null) {
                    log.warn("Failed to add line: Expense line amount is required");
                    throw new IllegalArgumentException("Expense line amount is required");
                }
                if (line.getOriginalCurrency() == null) {
                    line.setOriginalCurrency(saved.getOriginalCurrency());
                }

                // Multi-currency calculation
                BigDecimal rate = getExchangeRateToUsd(line.getOriginalCurrency());
                BigDecimal usdEquivalent = line.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
                line.setUsdEquivalent(usdEquivalent);
                log.info("Calculated USD equivalent for line: {} USD (rate: {})", usdEquivalent, rate);

                // 1) Persist ExpenseLine first so subsequent ComplianceAudit/PolicyException can reference its DB id
                ExpenseLine savedLine = expenseLineRepository.save(line);

                // 2) Run policy compliance checks
                log.info("Running compliance engine check for line ID: {}", savedLine.getId());
                complianceEngine.runComplianceCheck(savedLine);

                // 3) Persist any changes made by compliance engine
                savedLine = expenseLineRepository.save(savedLine);
                log.info("Compliance check completed for line ID: {}. Status: {}, Remarks: '{}'",
                        savedLine.getId(), savedLine.getPolicyComplianceStatus(), savedLine.getComplianceRemarks());

                totalAmount = totalAmount.add(savedLine.getAmount());
                totalUsdEquivalent = totalUsdEquivalent.add(savedLine.getUsdEquivalent());
            }

            saved.setTotalAmount(totalAmount);
            saved.setUsdEquivalent(totalUsdEquivalent);
            saved = expenseClaimRepository.save(saved);
            log.info("Updated claim ID: {} totals after bulk save -> Original total: {} {}, USD equivalent total: {} USD",
                    saved.getId(), totalAmount, saved.getOriginalCurrency(), totalUsdEquivalent);
        }

        return saved;
    }

    @Transactional
    @AuditAction(module = "EXPENSE", action = "ADD_EXPENSE_LINE")
    public ExpenseLine addExpenseLine(Long claimId, ExpenseLine line) {
        log.info("Attempting to add expense line of category '{}', amount '{}' {} to claim ID: {}", 
                line.getCategory(), line.getAmount(), line.getOriginalCurrency(), claimId);

        ExpenseClaim claim = expenseClaimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.warn("Failed to add line: Expense claim ID {} not found", claimId);
                    return new IllegalArgumentException("Expense claim not found");
                });

        if (claim.getStatus() != ExpenseStatus.DRAFT) {
            log.warn("Failed to add line: Claim ID {} is in state {}, only DRAFT allowed", claimId, claim.getStatus());
            throw new IllegalStateException("Can only add expense lines to DRAFT claims");
        }

        // Defensive: do not trust client-supplied identifiers or compliance fields
        line.setId(null);
        // Always associate the persisted claim using path variable
        line.setExpenseClaim(claim);
        // Ensure a non-null policy compliance status before persisting to avoid DB NOT NULL constraint
        if (line.getPolicyComplianceStatus() == null) {
            line.setPolicyComplianceStatus("COMPLIANT");
        }
        // Clear any client-supplied compliance remarks; compliance engine will set them as needed
        line.setComplianceRemarks(null);

        // Validate amount and currency
        if (line.getAmount() == null) {
            log.warn("Failed to add line: Expense line amount is required");
            throw new IllegalArgumentException("Expense line amount is required");
        }
        if (line.getOriginalCurrency() == null) line.setOriginalCurrency(claim.getOriginalCurrency());

        // Multi-currency calculation
        BigDecimal rate = getExchangeRateToUsd(line.getOriginalCurrency());
        BigDecimal usdEquivalent = line.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        line.setUsdEquivalent(usdEquivalent);
        log.info("Calculated USD equivalent for line: {} USD (rate: {})", usdEquivalent, rate);

        // 1) Persist ExpenseLine first so subsequent ComplianceAudit/PolicyException can reference its DB id
        ExpenseLine savedLine = expenseLineRepository.save(line);

        // 2) Run policy compliance checks (this will create ComplianceAudit and PolicyException records as needed)
        // The compliance engine expects an ExpenseLine with an expense claim and usdEquivalent set
        log.info("Running compliance engine check for line ID: {}", savedLine.getId());
        complianceEngine.runComplianceCheck(savedLine);

        // 3) Persist any changes made by the compliance engine (policyComplianceStatus, complianceRemarks)
        savedLine = expenseLineRepository.save(savedLine);
        log.info("Compliance check completed for line ID: {}. Status: {}, Remarks: '{}'", 
                savedLine.getId(), savedLine.getPolicyComplianceStatus(), savedLine.getComplianceRemarks());

        // 4) Update total claim sums after the line has been recorded
        BigDecimal originalTotal = claim.getTotalAmount().add(line.getAmount());
        BigDecimal usdTotal = claim.getUsdEquivalent().add(usdEquivalent);
        claim.setTotalAmount(originalTotal);
        claim.setUsdEquivalent(usdTotal);
        expenseClaimRepository.save(claim);
        log.info("Updated claim ID: {} totals -> Original total: {} {}, USD equivalent total: {} USD", 
                claim.getId(), originalTotal, claim.getOriginalCurrency(), usdTotal);

        return savedLine;
    }

    @Transactional
    @AuditAction(module = "EXPENSE", action = "SUBMIT_EXPENSE_CLAIM")
    public ExpenseClaim submitExpenseClaim(Long claimId) {
        log.info("Attempting to submit expense claim ID: {}", claimId);
        ExpenseClaim claim = expenseClaimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.warn("Failed to submit: Expense claim ID {} not found", claimId);
                    return new IllegalArgumentException("Expense claim not found");
                });

        if (claim.getStatus() != ExpenseStatus.DRAFT) {
            log.warn("Failed to submit: Claim ID {} is in state {}, only DRAFT claims can be submitted", claimId, claim.getStatus());
            throw new IllegalStateException("Only DRAFT claims can be submitted");
        }

        claim.setStatus(ExpenseStatus.SUBMITTED);
        claim.setSubmittedDate(LocalDate.now());
        ExpenseClaim saved = expenseClaimRepository.save(claim);
        log.info("Expense claim ID: {} successfully submitted", claimId);

        // Notify Approving Manager
        if (claim.getTripRequest().getApprovingManager() != null) {
            log.info("Publishing status event for approving manager ID: {} for submitted claim ID: {}", 
                    claim.getTripRequest().getApprovingManager().getId(), claimId);
            eventPublisher.publishEvent(new StatusChangeEvent(
                claim.getTripRequest().getApprovingManager().getId(),
                "Expense Claim Submitted",
                "An expense claim titled '" + claim.getClaimTitle() + "' has been submitted by " + 
                claim.getEmployee().getUsername() + " and is awaiting your review.",
                claim.getEmployee() != null ? claim.getEmployee().getId() : null,
                claim.getEmployee() != null ? claim.getEmployee().getUsername() : null
            ));
        } else {
            log.warn("No approving manager found for trip request associated with claim ID: {}", claimId);
        }

        return saved;
    }

    @Transactional
    @AuditAction(module = "EXPENSE", action = "APPROVE_REJECT_EXPENSE_CLAIM")
    public ExpenseClaim approveOrRejectExpenseClaim(Long claimId, ExpenseStatus newStatus, String comments, User manager) {
        log.info("Manager '{}' attempting to set status of claim ID: {} to {}", manager.getUsername(), claimId, newStatus);
        ExpenseClaim claim = expenseClaimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.warn("Failed to review: Expense claim ID {} not found", claimId);
                    return new IllegalArgumentException("Expense claim not found");
                });

        if (claim.getStatus() != ExpenseStatus.SUBMITTED) {
            log.warn("Failed to review: Claim ID {} is in state {}, only SUBMITTED claims can be approved or rejected", claimId, claim.getStatus());
            throw new IllegalStateException("Only SUBMITTED claims can be approved or rejected");
        }

        if (newStatus != ExpenseStatus.APPROVED && newStatus != ExpenseStatus.REJECTED) {
            log.warn("Failed to review: Invalid target status {}", newStatus);
            throw new IllegalArgumentException("Target status must be APPROVED or REJECTED");
        }

        claim.setStatus(newStatus);
        claim.setManagerComments(comments);
        ExpenseClaim saved = expenseClaimRepository.save(claim);
        log.info("Claim ID: {} status successfully updated to {} by manager", claimId, newStatus);

        // Notify Employee (actor = manager)
        log.info("Publishing status event to employee ID: {} for reviewed claim ID: {}", claim.getEmployee().getId(), claimId);
        eventPublisher.publishEvent(new StatusChangeEvent(
            claim.getEmployee().getId(),
            "Expense Claim " + newStatus.name(),
            "Your expense claim '" + claim.getClaimTitle() + "' has been " + newStatus.name().toLowerCase() + ".",
            manager != null ? manager.getId() : null,
            manager != null ? manager.getUsername() : null
        ));

        return saved;
    }

    @Transactional
    @AuditAction(module = "EXPENSE", action = "PAY_REIMBURSEMENT")
    public ExpenseClaim payReimbursement(Long claimId, Reimbursement reimbursement) {
        log.info("Attempting to pay reimbursement for claim ID: {}, method: {}", claimId, reimbursement.getPaymentMethod());
        ExpenseClaim claim = expenseClaimRepository.findById(claimId)
                .orElseThrow(() -> {
                    log.warn("Failed to disburse payment: Expense claim ID {} not found", claimId);
                    return new IllegalArgumentException("Expense claim not found");
                });

        if (claim.getStatus() != ExpenseStatus.APPROVED) {
            log.warn("Failed to disburse payment: Claim ID {} is in state {}, only APPROVED claims can be PAID", claimId, claim.getStatus());
            throw new IllegalStateException("Reimbursements can only be disbursed for APPROVED claims");
        }

        claim.setStatus(ExpenseStatus.PAID);
        ExpenseClaim savedClaim = expenseClaimRepository.save(claim);

        reimbursement.setExpenseClaim(savedClaim);
        reimbursement.setRecipient(savedClaim.getEmployee());
        reimbursement.setAmount(savedClaim.getTotalAmount());
        reimbursement.setOriginalCurrency(savedClaim.getOriginalCurrency());
        reimbursement.setUsdEquivalent(savedClaim.getUsdEquivalent());
        reimbursement.setPaymentDate(LocalDate.now());
        reimbursementRepository.save(reimbursement);
        log.info("Reimbursement record created successfully for claim ID: {}, payment method: {}", claimId, reimbursement.getPaymentMethod());

        // Notify Employee (no explicit actor available for payment operation)
        log.info("Publishing status event to employee ID: {} for PAID claim ID: {}", claim.getEmployee().getId(), claimId);
        eventPublisher.publishEvent(new StatusChangeEvent(
            claim.getEmployee().getId(),
            "Expense Claim Paid",
            "Your expense claim '" + claim.getClaimTitle() + "' has been fully paid via " + 
            reimbursement.getPaymentMethod() + ".",
            null,
            null
        ));

        return savedClaim;
    }

    public ExpenseClaim getExpenseClaim(Long id) {
        log.info("Retrieving expense claim ID: {}", id);
        return expenseClaimRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Lookup failed: Expense claim ID {} not found", id);
                    return new IllegalArgumentException("Expense claim not found");
                });
    }

    public List<ExpenseClaim> getClaimsByEmployee(Long employeeId) {
        log.info("Retrieving expense claims for employee ID: {}", employeeId);
        return expenseClaimRepository.findByEmployee_Id(employeeId);
    }

    public List<ExpenseLine> getLinesByClaim(Long claimId) {
        log.info("Retrieving expense lines for claim ID: {}", claimId);
        return expenseLineRepository.findByExpenseClaim_Id(claimId);
    }

    @Transactional
    @AuditAction(module = "EXPENSE", action = "SUBMIT_EXPENSE_LINE")
    public ExpenseLine submitExpenseLine(Long claimId, Long lineId) {
        log.info("Attempting to submit expense line ID: {} for claim ID: {}", lineId, claimId);
        ExpenseClaim claim = expenseClaimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Expense claim not found"));

        ExpenseLine line = expenseLineRepository.findById(lineId)
                .orElseThrow(() -> new IllegalArgumentException("Expense line not found"));

        if (line.getExpenseClaim() == null || !line.getExpenseClaim().getId().equals(claim.getId())) {
            log.warn("Line {} does not belong to claim {}", lineId, claimId);
            throw new IllegalArgumentException("Expense line does not belong to the provided claim");
        }

        if (claim.getStatus() != ExpenseStatus.DRAFT) {
            log.warn("Cannot submit line: Claim {} is in state {}", claimId, claim.getStatus());
            throw new IllegalStateException("Can only submit lines for claims in DRAFT state");
        }

        // Re-run policy compliance to ensure line is evaluated before final claim submission
        complianceEngine.runComplianceCheck(line);
        ExpenseLine saved = expenseLineRepository.save(line);
        log.info("Expense line ID: {} re-checked and saved", saved.getId());
        return saved;
    }
}
