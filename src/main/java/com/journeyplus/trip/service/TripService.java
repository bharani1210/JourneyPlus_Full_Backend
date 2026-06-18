package com.journeyplus.trip.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.journeyplus.config.AuditAction;
import com.journeyplus.event.StatusChangeEvent;
import com.journeyplus.iam.entity.User;
import com.journeyplus.trip.dto.VisaRequest;
import com.journeyplus.trip.entity.ItineraryLeg;
import com.journeyplus.trip.entity.TripRequest;
import com.journeyplus.trip.entity.TripStatus;
import com.journeyplus.trip.entity.VisaRequirement;
import com.journeyplus.trip.repository.ItineraryLegRepository;
import com.journeyplus.trip.repository.TripRequestRepository;
import com.journeyplus.trip.repository.VisaRequirementRepository;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    @Autowired
    private TripRequestRepository tripRequestRepository;

    @Autowired
    private ItineraryLegRepository itineraryLegRepository;

    @Autowired
    private VisaRequirementRepository visaRequirementRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    @AuditAction(module = "TRIP", action = "CREATE_TRIP")
    public TripRequest createTripRequest(TripRequest tripRequest, List<ItineraryLeg> legs, List<VisaRequirement> visas) {
        tripRequest.setStatus(TripStatus.DRAFT);
        TripRequest savedTrip = tripRequestRepository.save(tripRequest);

        // Debug log to verify which employee id was persisted for the created trip
        try {
            Long empId = savedTrip.getEmployee() != null ? savedTrip.getEmployee().getId() : null;
            log.info("Created Trip id={} employeeId={}", savedTrip.getId(), empId);
        } catch (Exception e) {
            log.warn("Could not log saved trip employee id: {}", e.getMessage());
        }

        if (legs != null) {
            for (ItineraryLeg leg : legs) {
                leg.setTripRequest(savedTrip);
                itineraryLegRepository.save(leg);
            }
        }

        if (visas != null) {
            for (VisaRequirement visa : visas) {
                visa.setTripRequest(savedTrip);
                visaRequirementRepository.save(visa);
            }
        }

        return savedTrip;
    }

    @Transactional
    @AuditAction(module = "TRIP", action = "SUBMIT_TRIP")
    public TripRequest submitTripRequest(Long tripId) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip request not found"));

        if (trip.getStatus() != TripStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT trip requests can be submitted");
        }

        trip.setStatus(TripStatus.SUBMITTED);
        TripRequest saved = tripRequestRepository.save(trip);

        // Publish event for status change notification using persisted entity (saved)
        try {
            if (saved.getApprovingManager() != null) {
                Long mgrId = saved.getApprovingManager().getId();
                log.info("Publishing submission event for manager id={} tripId={}", mgrId, saved.getId());
                eventPublisher.publishEvent(new StatusChangeEvent(
                    mgrId,
                    "New Trip Request Submitted",
                    "A trip request has been submitted by " + saved.getEmployee().getUsername() + " and requires your review.",
                    saved.getEmployee() != null ? saved.getEmployee().getId() : null,
                    saved.getEmployee() != null ? saved.getEmployee().getUsername() : null
                ));
            } else {
                log.info("No approving manager set for tripId={}", saved.getId());
            }
        } catch (Exception e) {
            log.error("Error while publishing manager notification for tripId={}: {}", saved.getId(), e.getMessage());
        }

        try {
                eventPublisher.publishEvent(new StatusChangeEvent(
                    saved.getEmployee().getId(),
                    "Trip Request Submitted",
                    "Your trip request to " + saved.getDestination() + " has been successfully submitted.",
                    saved.getEmployee() != null ? saved.getEmployee().getId() : null,
                    saved.getEmployee() != null ? saved.getEmployee().getUsername() : null
                ));
        } catch (Exception e) {
            log.error("Error while publishing employee notification for tripId={}: {}", saved.getId(), e.getMessage());
        }

        return saved;
    }

    @Transactional
    @AuditAction(module = "TRIP", action = "APPROVE_REJECT_TRIP")
    public TripRequest approveOrRejectTripRequest(Long tripId, TripStatus newStatus, String comments, User manager) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip request not found"));

        if (trip.getStatus() != TripStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED trip requests can be approved or rejected");
        }

        if (newStatus != TripStatus.APPROVED && newStatus != TripStatus.REJECTED) {
            throw new IllegalArgumentException("Invalid status: Status must be APPROVED or REJECTED");
        }

        trip.setStatus(newStatus);
        trip.setComments(comments);
        trip.setApprovingManager(manager);
        TripRequest saved = tripRequestRepository.save(trip);

        // Notify employee
        eventPublisher.publishEvent(new StatusChangeEvent(
            trip.getEmployee().getId(),
            "Trip Request " + newStatus.name(),
            "Your trip request to " + trip.getDestination() + " has been " + newStatus.name().toLowerCase() + " by " + manager.getUsername() + ".",
            manager != null ? manager.getId() : null,
            manager != null ? manager.getUsername() : null
        ));

        return saved;
    }

    @Transactional
    @AuditAction(module = "TRIP", action = "COMPLETE_CANCEL_TRIP")
    public TripRequest completeOrCancelTripRequest(Long tripId, TripStatus newStatus) {
        TripRequest trip = tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip request not found"));

        if (newStatus == TripStatus.COMPLETED) {
            if (trip.getStatus() != TripStatus.APPROVED) {
                throw new IllegalStateException("Only APPROVED trips can be marked as COMPLETED");
            }
        } else if (newStatus == TripStatus.CANCELLED) {
            if (trip.getStatus() == TripStatus.COMPLETED || trip.getStatus() == TripStatus.CANCELLED) {
                throw new IllegalStateException("Cannot cancel a completed or already cancelled trip");
            }
        } else {
            throw new IllegalArgumentException("Invalid target status. Must be COMPLETED or CANCELLED");
        }

        trip.setStatus(newStatus);
        TripRequest saved = tripRequestRepository.save(trip);

        // Notify employee
        eventPublisher.publishEvent(new StatusChangeEvent(
                trip.getEmployee().getId(),
                "Trip Request " + newStatus.name(),
                "Your trip request to " + trip.getDestination() + " is now " + newStatus.name().toLowerCase() + "."
        ));

        return saved;
    }

    public TripRequest getTripRequest(Long tripId) {
        return tripRequestRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip request not found"));
    }

    @Transactional
    @AuditAction(module = "TRIP", action = "UPDATE_VISA")
    public VisaRequirement updateVisaRequirement(Long tripId, Long visaId, VisaRequest visaRequest) {
        VisaRequirement v = visaRequirementRepository.findById(visaId)
                .orElseThrow(() -> new IllegalArgumentException("Visa requirement not found"));
        if (v.getTripRequest() == null || !v.getTripRequest().getId().equals(tripId)) {
            throw new IllegalArgumentException("Visa requirement does not belong to the specified trip");
        }
        if (visaRequest.getDestinationCountry() != null) v.setDestinationCountry(visaRequest.getDestinationCountry());
        if (visaRequest.getStatus() != null) v.setStatus(visaRequest.getStatus().getValue());
        if (visaRequest.getNotes() != null) v.setNotes(visaRequest.getNotes());
        if (visaRequest.getRequiresVisa() != null) v.setRequiresVisa(visaRequest.getRequiresVisa());
        return visaRequirementRepository.save(v);
    }

    public List<TripRequest> getTripsByEmployee(Long employeeId) {
        return tripRequestRepository.findByEmployee_Id(employeeId);
    }

    public List<TripRequest> getTripsForManager(Long managerId) {
        return tripRequestRepository.findByApprovingManager_Id(managerId);
    }
}
