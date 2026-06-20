package com.journeyplus.compliance.entity;
 
import com.journeyplus.common.EncryptedBigDecimalConverter;
import com.journeyplus.expense.entity.ExpenseLine;
import com.journeyplus.policy.entity.TravelPolicy;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "policy_exceptions")
@Getter
@Setter
public class PolicyException {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_audit_id", nullable = false)
    private ComplianceAudit complianceAudit;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private com.journeyplus.expense.entity.ExpenseClaim claim;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private TravelPolicy policy;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_line_id", nullable = false)
    private ExpenseLine expenseLine;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exception_approver_id")
    private com.journeyplus.iam.entity.User exceptionApprover;
 
    @Column(name = "violation_type", nullable = false, length = 100)
    private String violationType; // DAILY_ALLOWANCE_EXCEEDED, TRIP_LIMIT_EXCEEDED
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "amount_exceeded", nullable = false, length = 255)
    private BigDecimal amountExceeded;
 
    @Column(columnDefinition = "TEXT")
    private String justification;
 
    @Column(name = "approval_status", nullable = false, length = 50)
    private String approvalStatus = "PENDING"; // PENDING, APPROVED, REJECTED
 
    public PolicyException() {}
 
    public PolicyException(ComplianceAudit complianceAudit, TravelPolicy policy, ExpenseLine expenseLine, String violationType, BigDecimal amountExceeded) {
        this.complianceAudit = complianceAudit;
        this.policy = policy;
        this.expenseLine = expenseLine;
        this.violationType = violationType;
        this.amountExceeded = amountExceeded;
        this.approvalStatus = "PENDING";
    }
}
