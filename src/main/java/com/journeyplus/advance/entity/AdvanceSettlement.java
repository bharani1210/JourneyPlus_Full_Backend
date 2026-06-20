package com.journeyplus.advance.entity;
 
import com.journeyplus.common.EncryptedBigDecimalConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "advance_settlements")
@Getter
@Setter
public class AdvanceSettlement {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advance_request_id", nullable = false, unique = true)
    private AdvanceRequest advanceRequest;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "actual_spent", nullable = false, length = 255)
    private BigDecimal actualSpent;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "returned_amount", nullable = false, length = 255)
    private BigDecimal returnedAmount;
 
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate = LocalDate.now();
 
    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, SETTLED, AUDITED
 
    @Column(columnDefinition = "TEXT")
    private String remarks;
 
    public AdvanceSettlement() {}
 
    public AdvanceSettlement(AdvanceRequest advanceRequest, BigDecimal actualSpent, BigDecimal returnedAmount, String status, String remarks) {
        this.advanceRequest = advanceRequest;
        this.actualSpent = actualSpent;
        this.returnedAmount = returnedAmount;
        this.settlementDate = LocalDate.now();
        this.status = status;
        this.remarks = remarks;
    }
}
