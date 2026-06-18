package com.journeyplus.trip.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.journeyplus.iam.entity.User;
import com.journeyplus.iam.repository.UserRepository;
import com.journeyplus.trip.dto.ItineraryLegInput;
import com.journeyplus.trip.dto.SimpleUserDTO;
import com.journeyplus.trip.dto.TripCreationRequest;
import com.journeyplus.trip.dto.TripRequestInput;
import com.journeyplus.trip.dto.TripResponse;
import com.journeyplus.trip.dto.VisaRequirementInput;
import com.journeyplus.trip.entity.TripRequest;
import com.journeyplus.trip.entity.TripStatus;
import com.journeyplus.trip.service.TripService;


@RestController
@RequestMapping("/api/trips")
public class TripRequestController {

    @Autowired
    private TripService tripService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TripRequest> createTripRequest(
            @RequestBody TripCreationRequest payload,
            @AuthenticationPrincipal User employee) {

        TripRequestInput r = payload.getTripRequest();
        TripRequest tripEntity = new TripRequest();
        if (r != null) {
            tripEntity.setPurpose(r.getPurpose());
            tripEntity.setDestination(r.getDestination());
            tripEntity.setStartDate(r.getStartDate());
            tripEntity.setEndDate(r.getEndDate());
            tripEntity.setComments(r.getComments());
            if (r.getApprovingManagerUsername() != null && !r.getApprovingManagerUsername().isBlank()) {
                com.journeyplus.iam.entity.User mgr = userRepository.findByUsername(r.getApprovingManagerUsername())
                        .orElseThrow(() -> new IllegalArgumentException("Approving manager not found: " + r.getApprovingManagerUsername()));
                tripEntity.setApprovingManager(mgr);
            }
        }

        // Map legs
        java.util.List<com.journeyplus.trip.entity.ItineraryLeg> legs = null;
        if (payload.getLegs() != null) {
            legs = new java.util.ArrayList<>();
            for (ItineraryLegInput li : payload.getLegs()) {
                com.journeyplus.trip.entity.ItineraryLeg leg = new com.journeyplus.trip.entity.ItineraryLeg();
                leg.setDepartureCity(li.getDepartureCity());
                leg.setArrivalCity(li.getArrivalCity());
                leg.setTravelMode(li.getTravelMode());
                leg.setTravelDate(li.getTravelDate());
                if (li.getEstimatedCost() != null) leg.setEstimatedCost(li.getEstimatedCost());
                leg.setOriginalCurrency(li.getOriginalCurrency());
                if (li.getUsdEquivalent() != null) leg.setUsdEquivalent(li.getUsdEquivalent());
                leg.setCarrierDetails(li.getCarrierDetails());
                leg.setBookingReference(li.getBookingReference());
                legs.add(leg);
            }
        }

        // Map visas
        java.util.List<com.journeyplus.trip.entity.VisaRequirement> visas = null;
        if (payload.getVisas() != null) {
            visas = new java.util.ArrayList<>();
            for (VisaRequirementInput vi : payload.getVisas()) {
                com.journeyplus.trip.entity.VisaRequirement v = new com.journeyplus.trip.entity.VisaRequirement();
                v.setDestinationCountry(vi.getDestinationCountry());
                v.setRequiresVisa(vi.isRequiresVisa());
                v.setNotes(vi.getNotes());
                visas.add(v);
            }
        }

        // The controller/service will set employee from the authenticated principal
        tripEntity.setEmployee(employee);

        TripRequest trip = tripService.createTripRequest(
                tripEntity,
                legs,
                visas
        );

        return ResponseEntity.ok(trip);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<TripResponse> submitTripRequest(@PathVariable Long id) {
        try {
            // Read current trip status first to avoid throwing and to provide idempotent responses
            TripRequest current = tripService.getTripRequest(id);
            if (current == null) {
                return ResponseEntity.status(404).body(null);
            }

            // If not in DRAFT state, return appropriate response without invoking submit
            if (current.getStatus() != TripStatus.DRAFT) {
                if (current.getStatus() == TripStatus.SUBMITTED) {
                    // Already submitted — return 200 with current trip (idempotent)
                    return ResponseEntity.ok(toTripResponse(current));
                }
                // Other non-allowed states -> 409 Conflict with current trip in body
                return ResponseEntity.status(409).body(toTripResponse(current));
            }

            TripRequest saved = tripService.submitTripRequest(id);
            return ResponseEntity.ok(toTripResponse(saved));
        } catch (IllegalArgumentException e) {
            // Trip not found or other invalid argument
            return ResponseEntity.status(404).body(null);
        } catch (Exception e) {
            // Unexpected error: return 500 with no stacktrace
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('APPROVING_MANAGER')")
    public ResponseEntity<TripRequest> approveTripRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal User manager) {
        return ResponseEntity.ok(tripService.approveOrRejectTripRequest(id, TripStatus.APPROVED, comments, manager));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('APPROVING_MANAGER')")
    public ResponseEntity<TripRequest> rejectTripRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String comments,
            @AuthenticationPrincipal User manager) {
        return ResponseEntity.ok(tripService.approveOrRejectTripRequest(id, TripStatus.REJECTED, comments, manager));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TRAVEL_DESK_COORDINATOR')")
    public ResponseEntity<TripRequest> completeTripRequest(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.completeOrCancelTripRequest(id, TripStatus.COMPLETED));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'TRAVEL_DESK_COORDINATOR')")
    public ResponseEntity<TripRequest> cancelTripRequest(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.completeOrCancelTripRequest(id, TripStatus.CANCELLED));
    }

    @GetMapping("/my-trips")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<TripResponse>> getMyTrips(@AuthenticationPrincipal User employee) {
        List<TripRequest> trips = tripService.getTripsByEmployee(employee.getId());
        List<TripResponse> dto = trips.stream().map(this::toTripResponse).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/pending-approvals")
    @PreAuthorize("hasRole('APPROVING_MANAGER')")
    public ResponseEntity<List<TripResponse>> getPendingApprovals(@AuthenticationPrincipal User manager) {
        List<TripRequest> trips = tripService.getTripsForManager(manager.getId());
        List<TripResponse> dto = trips.stream().map(this::toTripResponse).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripRequest(@PathVariable Long id, @AuthenticationPrincipal User user) {
        TripRequest trip = tripService.getTripRequest(id);
        if (trip == null) throw new IllegalArgumentException("Trip not found");

        // Allow if user is Travel Admin, Compliance, Finance, Travel Desk Coordinator
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> auths = auth.getAuthorities();
            for (org.springframework.security.core.GrantedAuthority a : auths) {
                String r = a.getAuthority();
                if (r != null && (r.endsWith("TRAVEL_ADMIN") || r.endsWith("COMPLIANCE_OFFICER") || r.endsWith("FINANCE_EXECUTIVE") || r.endsWith("TRAVEL_DESK_COORDINATOR"))) {
                    return ResponseEntity.ok(toTripResponse(trip));
                }
            }
        }

        // Allow employee owner or approving manager
        if (trip.getEmployee() != null && user != null && trip.getEmployee().getId().equals(user.getId())) {
            return ResponseEntity.ok(toTripResponse(trip));
        }
        if (trip.getApprovingManager() != null && user != null && trip.getApprovingManager().getId().equals(user.getId())) {
            return ResponseEntity.ok(toTripResponse(trip));
        }

        throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this trip");
    }

    private SimpleUserDTO toSimpleUser(com.journeyplus.iam.entity.User u) {
        if (u == null) return null;
        SimpleUserDTO s = new SimpleUserDTO();
        try {
            if (u instanceof org.hibernate.proxy.HibernateProxy) {
                org.hibernate.proxy.HibernateProxy proxy = (org.hibernate.proxy.HibernateProxy) u;
                Object idObj = proxy.getHibernateLazyInitializer().getIdentifier();
                if (idObj != null) {
                    s.setId(Long.valueOf(idObj.toString()));
                }
                if (proxy.getHibernateLazyInitializer().isUninitialized()) {
                    return s;
                }
                u = (com.journeyplus.iam.entity.User) proxy.getHibernateLazyInitializer().getImplementation();
            }

            s.setId(u.getId());
            s.setUsername(u.getUsername());
            s.setEmail(u.getEmail());
            s.setRole(u.getRole() != null ? u.getRole().name() : null);
        } catch (Exception e) {
            try {
                if (u.getId() != null) s.setId(u.getId());
            } catch (Exception ex) {
            }
        }
        return s;
    }

    private TripResponse toTripResponse(TripRequest t) {
        if (t == null) return null;
        TripResponse r = new TripResponse();
        r.setId(t.getId());
        r.setEmployee(toSimpleUser(t.getEmployee()));
        r.setPurpose(t.getPurpose());
        r.setDestination(t.getDestination());
        r.setStartDate(t.getStartDate());
        r.setEndDate(t.getEndDate());
        r.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        r.setComments(t.getComments());
        r.setApprovingManager(toSimpleUser(t.getApprovingManager()));
        r.setCreatedAt(t.getCreatedAt());
        r.setUpdatedAt(t.getUpdatedAt());
        return r;
    }
}
