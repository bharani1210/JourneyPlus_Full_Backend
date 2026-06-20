package com.journeyplus.expense.controller;

import com.journeyplus.expense.entity.ExpenseClaim;
import com.journeyplus.expense.entity.ExpenseLine;
import com.journeyplus.expense.entity.ExpenseStatus;
import com.journeyplus.expense.entity.Reimbursement;
import com.journeyplus.expense.service.ExpenseService;
import com.journeyplus.iam.entity.User;
import com.journeyplus.trip.entity.TripRequest;
import com.journeyplus.trip.service.TripService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private TripService tripService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Create an expense claim", description = "Create an expense claim for a trip. Provide 'tripRequestId' as request parameter and claim fields in the JSON body.")
    public ResponseEntity<ExpenseClaim> createExpenseClaim(
            @RequestParam Long tripRequestId,
            @RequestBody com.journeyplus.expense.dto.ExpenseClaimRequest claimRequest,
            @AuthenticationPrincipal User employee) {

        TripRequest trip = tripService.getTripRequest(tripRequestId);
        if (!trip.getEmployee().getId().equals(employee.getId())) {
            throw new IllegalArgumentException("Trip request does not belong to the authenticated employee");
        }

        // Map minimal incoming request to ExpenseClaim entity
        ExpenseClaim claim = new ExpenseClaim();
        claim.setTripRequest(trip);
        claim.setEmployee(employee);
        claim.setClaimTitle(claimRequest.getClaimTitle());
        claim.setSubmittedDate(claimRequest.getSubmittedDate());
        if (claimRequest.getTotalAmount() != null) claim.setTotalAmount(new java.math.BigDecimal(claimRequest.getTotalAmount().toString()));
        claim.setOriginalCurrency(claimRequest.getOriginalCurrency());

        return ResponseEntity.ok(expenseService.createExpenseClaim(claim, claimRequest.getExpenseLines()));
    }

    @PostMapping("/{claimId}/lines")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Add an expense line", description = "Add an expense line to an existing claim. Provide expense line JSON in the request body.")
    public ResponseEntity<ExpenseLine> addExpenseLine(
            @PathVariable Long claimId,
            @RequestBody com.journeyplus.expense.dto.ExpenseLineRequest lineRequest) {
        // Map minimal incoming fields to the ExpenseLine entity; do not require caller to send internal fields
        ExpenseLine line = new ExpenseLine();
        line.setExpenseDate(lineRequest.getExpenseDate());
        line.setCategory(lineRequest.getCategory());
        line.setAmount(lineRequest.getAmount());
        line.setOriginalCurrency(lineRequest.getOriginalCurrency());
        line.setReceiptPath(lineRequest.getReceiptPath());

        return ResponseEntity.ok(expenseService.addExpenseLine(claimId, line));
    }

    @PostMapping("/{claimId}/lines/{lineId}/submit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Submit an expense line for compliance re-check", description = "Trigger compliance re-check and persist the expense line. This does not submit the entire claim.")
    public ResponseEntity<ExpenseLine> submitExpenseLine(
            @PathVariable Long claimId,
            @PathVariable Long lineId) {
        return ResponseEntity.ok(expenseService.submitExpenseLine(claimId, lineId));
    }

    @PostMapping("/{claimId}/submit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ExpenseClaim> submitExpenseClaim(@PathVariable Long claimId) {
        return ResponseEntity.ok(expenseService.submitExpenseClaim(claimId));
    }

    @PostMapping("/{claimId}/approve")
    @PreAuthorize("hasRole('APPROVING_MANAGER')")
    public ResponseEntity<ExpenseClaim> approveExpenseClaim(
            @PathVariable Long claimId,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal User manager) {
        return ResponseEntity.ok(expenseService.approveOrRejectExpenseClaim(claimId, ExpenseStatus.APPROVED, comments, manager));
    }

    @PostMapping("/{claimId}/reject")
    @PreAuthorize("hasRole('APPROVING_MANAGER')")
    public ResponseEntity<ExpenseClaim> rejectExpenseClaim(
            @PathVariable Long claimId,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal User manager) {
        return ResponseEntity.ok(expenseService.approveOrRejectExpenseClaim(claimId, ExpenseStatus.REJECTED, comments, manager));
    }

    @PostMapping("/{claimId}/reimburse")
    @PreAuthorize("hasRole('FINANCE_EXECUTIVE')")
    @Operation(summary = "Disburse reimbursement", description = "Create a reimbursement record for a claim. Provide reimbursement JSON in the request body.")
    public ResponseEntity<ExpenseClaim> disburseReimbursement(
            @PathVariable Long claimId,
            @RequestBody Reimbursement reimbursement) {
        return ResponseEntity.ok(expenseService.payReimbursement(claimId, reimbursement));
    }

    @GetMapping("/my-claims")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ExpenseClaim>> getMyClaims(@AuthenticationPrincipal User employee) {
        return ResponseEntity.ok(expenseService.getClaimsByEmployee(employee.getId()));
    }

    @GetMapping("/{claimId}")
    public ResponseEntity<ExpenseClaim> getExpenseClaim(@PathVariable Long claimId, @AuthenticationPrincipal User user) {
        ExpenseClaim claim = expenseService.getExpenseClaim(claimId);
        if (claim == null) throw new IllegalArgumentException("Expense claim not found");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            for (org.springframework.security.core.GrantedAuthority a : auth.getAuthorities()) {
                String r = a.getAuthority();
                if (r != null && (r.endsWith("TRAVEL_ADMIN") || r.endsWith("COMPLIANCE_OFFICER") || r.endsWith("FINANCE_EXECUTIVE"))) {
                    return ResponseEntity.ok(claim);
                }
            }
        }
        if (claim.getEmployee() != null && user != null && claim.getEmployee().getId().equals(user.getId())) {
            return ResponseEntity.ok(claim);
        }
        if (claim.getTripRequest() != null && claim.getTripRequest().getApprovingManager() != null && user != null && claim.getTripRequest().getApprovingManager().getId().equals(user.getId())) {
            return ResponseEntity.ok(claim);
        }
        throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this expense claim");
    }

    @GetMapping("/{claimId}/lines")
    public ResponseEntity<List<ExpenseLine>> getExpenseLines(@PathVariable Long claimId, @AuthenticationPrincipal User user) {
        ExpenseClaim claim = expenseService.getExpenseClaim(claimId);
        if (claim == null) throw new IllegalArgumentException("Expense claim not found");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            for (org.springframework.security.core.GrantedAuthority a : auth.getAuthorities()) {
                String r = a.getAuthority();
                if (r != null && (r.endsWith("TRAVEL_ADMIN") || r.endsWith("COMPLIANCE_OFFICER") || r.endsWith("FINANCE_EXECUTIVE"))) {
                    return ResponseEntity.ok(expenseService.getLinesByClaim(claimId));
                }
            }
        }
        if (claim.getEmployee() != null && user != null && claim.getEmployee().getId().equals(user.getId())) {
            return ResponseEntity.ok(expenseService.getLinesByClaim(claimId));
        }
        if (claim.getTripRequest() != null && claim.getTripRequest().getApprovingManager() != null && user != null && claim.getTripRequest().getApprovingManager().getId().equals(user.getId())) {
            return ResponseEntity.ok(expenseService.getLinesByClaim(claimId));
        }
        throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view expense lines");
    }
}
