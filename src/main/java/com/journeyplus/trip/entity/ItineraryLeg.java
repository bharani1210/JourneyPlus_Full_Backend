package com.journeyplus.trip.entity;
 
import com.journeyplus.common.EncryptedBigDecimalConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
 
@Entity
@Table(name = "itinerary_legs")
@Getter
@Setter
public class ItineraryLeg {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_request_id", nullable = false)
    private TripRequest tripRequest;
 
    @Column(name = "departure_city", nullable = false, length = 100)
    private String departureCity;
 
    @Column(name = "arrival_city", nullable = false, length = 100)
    private String arrivalCity;
 
    @Column(name = "travel_mode", nullable = false, length = 50)
    private String travelMode; // FLIGHT, TRAIN, CAB, BUS
 
    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;
 
    @Column(name = "departure_date_time")
    private LocalDateTime departureDateTime;
 
    @Column(name = "arrival_date_time")
    private LocalDateTime arrivalDateTime;
 
    @Column(name = "carrier_details", length = 150)
    private String carrierDetails;
 
    @Column(name = "booking_reference", length = 100)
    private String bookingReference;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "estimated_cost", nullable = false, length = 255)
    private BigDecimal estimatedCost;
 
    @Column(name = "original_currency", nullable = false, length = 10)
    private String originalCurrency;
 
    @Convert(converter = EncryptedBigDecimalConverter.class)
    @Column(name = "usd_equivalent", nullable = false, length = 255)
    private BigDecimal usdEquivalent;
 
    @Column(name = "booking_status", nullable = false, length = 50)
    private String bookingStatus = "PENDING"; // PENDING, BOOKED, CANCELLED
 
    public ItineraryLeg() {}
 
    public ItineraryLeg(TripRequest tripRequest, String departureCity, String arrivalCity, String travelMode, LocalDate travelDate, BigDecimal estimatedCost, String originalCurrency, BigDecimal usdEquivalent) {
        this.tripRequest = tripRequest;
        this.departureCity = departureCity;
        this.arrivalCity = arrivalCity;
        this.travelMode = travelMode;
        this.travelDate = travelDate;
        this.estimatedCost = estimatedCost;
        this.originalCurrency = originalCurrency;
        this.usdEquivalent = usdEquivalent;
        this.bookingStatus = "PENDING";
    }
}
