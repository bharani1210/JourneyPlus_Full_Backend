package com.journeyplus.expense.entity;
 
import com.journeyplus.common.EncryptedBigDecimalConverter;
import com.journeyplus.iam.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "reimbursements")
@Getter
@Setter
public class Reimbursement {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_claim_id", nullable = false, unique = true)
    private ExpenseClaim expenseClaim;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(nullable = false, length = 255)
    private BigDecimal amount;
 
    @Column(name = "original_currency", nullable = false, length = 10)
    private String originalCurrency;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "usd_equivalent", nullable = false, length = 255)
    private BigDecimal usdEquivalent;
 
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod; // BANK_TRANSFER, CORPORATE_CARD
 
    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;
 
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate = LocalDate.now();
 
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, PROCESSED, FAILED
 
    public Reimbursement() {}
 
    public Reimbursement(ExpenseClaim expenseClaim, User recipient, BigDecimal amount, String originalCurrency, BigDecimal usdEquivalent, String paymentMethod, String transactionReference) {
        this.expenseClaim = expenseClaim;
        this.recipient = recipient;
        this.amount = amount;
        this.originalCurrency = originalCurrency;
        this.usdEquivalent = usdEquivalent;
        this.paymentMethod = paymentMethod;
        this.transactionReference = transactionReference;
        this.paymentDate = LocalDate.now();
    }
}
