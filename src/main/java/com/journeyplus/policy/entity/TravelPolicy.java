package com.journeyplus.policy.entity;
 
import com.journeyplus.common.EncryptedBigDecimalConverter;
import com.journeyplus.iam.entity.Role;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "travel_policies")
@Getter
@Setter
public class TravelPolicy {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(name = "policy_name", nullable = false, length = 150)
    private String policyName;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "employee_role", nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private Role employeeRole;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "max_amount_per_trip", nullable = false, length = 255)
    private BigDecimal maxAmountPerTrip;
 
    // Optional fields added to align with design (kept nullable)
    @Column(name = "travel_type", length = 50)
    private String travelType; // DOMESTIC / INTERNATIONAL
 
    @Column(name = "flight_class", length = 50)
    private String flightClass;
 
    @Column(name = "hotel_category", length = 50)
    private String hotelCategory;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "per_diem_rate", length = 255)
    private BigDecimal perDiemRate;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "local_conveyance_limit", length = 255)
    private BigDecimal localConveyanceLimit;
 
    @Column(name = "effective_date")
    private java.time.LocalDateTime effectiveDate;
 
    @Column(name = "policy_status", length = 50)
    private String policyStatus; // ACTIVE / SUPERSEDED
 
    @Column(name = "requires_visa_verification")
    private boolean requiresVisaVerification = false;
 
    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
 
    public TravelPolicy() {}
 
    public TravelPolicy(String policyName, String description, Role employeeRole, BigDecimal maxAmountPerTrip, boolean requiresVisaVerification, String createdBy) {
        this.policyName = policyName;
        this.description = description;
        this.employeeRole = employeeRole;
        this.maxAmountPerTrip = maxAmountPerTrip;
        this.requiresVisaVerification = requiresVisaVerification;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }
}
