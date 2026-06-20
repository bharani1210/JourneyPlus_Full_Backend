package com.journeyplus.trip.entity;
 
import com.journeyplus.iam.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.journeyplus.common.EncryptedBigDecimalConverter;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "trip_requests")
@Getter
@Setter
public class TripRequest {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;
 
    @Column(nullable = false)
    private String purpose;
 
    @Column(nullable = false, length = 150)
    private String destination;
 
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
 
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
 
    @Column(name = "travel_type", length = 50)
    private String travelType; // DOMESTIC / INTERNATIONAL
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "estimated_cost", length = 255)
    private BigDecimal estimatedCost;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "VARCHAR(50)")
    private TripStatus status = TripStatus.DRAFT;
 
    @Column(columnDefinition = "TEXT")
    private String comments;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approving_manager_id")
    private User approvingManager;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
 
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
 
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
 
    public TripRequest() {}
 
    public TripRequest(User employee, String purpose, String destination, LocalDate startDate, LocalDate endDate) {
        this.employee = employee;
        this.purpose = purpose;
        this.destination = destination;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = TripStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }
}
