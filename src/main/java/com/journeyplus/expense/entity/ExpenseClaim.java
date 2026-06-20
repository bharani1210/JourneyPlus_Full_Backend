package com.journeyplus.expense.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.journeyplus.common.EncryptedBigDecimalConverter;
import com.journeyplus.iam.entity.User;
import com.journeyplus.trip.entity.TripRequest;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_claims")
@Getter
@Setter
public class ExpenseClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_request_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private TripRequest tripRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User employee;

    @Column(name = "claim_title", nullable = false, length = 200)
    private String claimTitle;

    @Column(name = "submitted_date")
    private LocalDate submittedDate;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "total_amount", nullable = false, length = 255)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "original_currency", nullable = false, length = 10)
    private String originalCurrency;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "usd_equivalent", nullable = false, length = 255)
    private BigDecimal usdEquivalent = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "advance_adjusted", length = 255)
    private BigDecimal advanceAdjusted = BigDecimal.ZERO;

    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "net_reimbursable", length = 255)
    private BigDecimal netReimbursable = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @Column(name = "manager_comments", columnDefinition = "TEXT")
    private String managerComments;

    @Column(name = "finance_comments", columnDefinition = "TEXT")
    private String financeComments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private com.journeyplus.iam.entity.User approver;

    public ExpenseClaim() {}

    public ExpenseClaim(TripRequest tripRequest, User employee, String claimTitle, String originalCurrency) {
        this.tripRequest = tripRequest;
        this.employee = employee;
        this.claimTitle = claimTitle;
        this.originalCurrency = originalCurrency;
        this.status = ExpenseStatus.DRAFT;
        this.totalAmount = BigDecimal.ZERO;
        this.usdEquivalent = BigDecimal.ZERO;
    }

    // Getters and Setters
    @JsonProperty("tripRequestId")
    public Long getTripRequestId() {
        return tripRequest != null ? tripRequest.getId() : null;
    }

    @JsonProperty("employeeId")
    public Long getEmployeeId() {
        return employee != null ? employee.getId() : null;
    }
}
