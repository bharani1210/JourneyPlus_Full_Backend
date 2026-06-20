package com.journeyplus.compliance.entity;
 
import com.journeyplus.expense.entity.ExpenseLine;
import com.journeyplus.iam.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "compliance_audits")
@Getter
@Setter
public class ComplianceAudit {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_line_id", nullable = false)
    private ExpenseLine expenseLine;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    private com.journeyplus.expense.entity.ExpenseClaim claim;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditor_id")
    private User auditor;
 
    @Column(name = "audit_date")
    private LocalDateTime auditDate = LocalDateTime.now();
 
    @Column(name = "compliance_status", nullable = false, length = 50)
    private String complianceStatus = "PASSED"; // PASSED, FLAG_BREACH
 
    @Column(name = "violations_found", columnDefinition = "TEXT")
    private String violationsFound;
 
    @Column(name = "audit_notes", columnDefinition = "TEXT")
    private String auditNotes;
 
    public ComplianceAudit() {}
 
    public ComplianceAudit(ExpenseLine expenseLine, User auditor, String complianceStatus, String violationsFound, String auditNotes) {
        this.expenseLine = expenseLine;
        this.auditor = auditor;
        this.complianceStatus = complianceStatus;
        this.violationsFound = violationsFound;
        this.auditNotes = auditNotes;
        this.auditDate = LocalDateTime.now();
    }
}
