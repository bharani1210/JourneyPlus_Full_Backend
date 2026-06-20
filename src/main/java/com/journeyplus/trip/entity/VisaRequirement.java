package com.journeyplus.trip.entity;
 
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "visa_requirements")
@Getter
@Setter
public class VisaRequirement {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_request_id", nullable = false)
    private TripRequest tripRequest;
 
    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;
 
    @Column(name = "requires_visa")
    private boolean requiresVisa = false;
 
    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, APPLIED, APPROVED, EXEMPTED
 
    @Column(columnDefinition = "TEXT")
    private String notes;
 
    public VisaRequirement() {}
 
    public VisaRequirement(TripRequest tripRequest, String destinationCountry, boolean requiresVisa, String status, String notes) {
        this.tripRequest = tripRequest;
        this.destinationCountry = destinationCountry;
        this.requiresVisa = requiresVisa;
        this.status = status;
        this.notes = notes;
    }
}
