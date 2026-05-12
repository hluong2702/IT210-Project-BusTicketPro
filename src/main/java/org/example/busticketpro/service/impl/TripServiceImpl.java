package org.example.busticketpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.request.TripRequest;
import org.example.busticketpro.entity.*;
import org.example.busticketpro.enums.SeatStatus;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.exception.ResourceNotFoundException;
import org.example.busticketpro.repository.*;
import org.example.busticketpro.service.TripService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Trip> search(Long departureId, Long arrivalId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        return tripRepository.findAvailableTrips(departureId, arrivalId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public Trip findById(Long id) {
        return tripRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", id));
    }

    @Override
    public Trip create(TripRequest request) {
        Route route = routeRepository.findById(request.getRouteId())
            .orElseThrow(() -> new ResourceNotFoundException("Tuyến đường", request.getRouteId()));
        Bus bus = busRepository.findById(request.getBusId())
            .orElseThrow(() -> new ResourceNotFoundException("Xe", request.getBusId()));

        if (!request.getArrivalTime().isAfter(request.getDepartureTime())) {
            throw new BusinessException("INVALID_TIME", "Giờ đến phải sau giờ khởi hành");
        }

        Trip trip = tripRepository.save(Trip.builder()
            .route(route).bus(bus)
            .departureTime(request.getDepartureTime())
            .arrivalTime(request.getArrivalTime())
            .price(request.getPrice())
            .active(true)
            .build());

        // Generate seats for this trip
        generateSeats(trip, bus);
        return trip;
    }

    private void generateSeats(Trip trip, Bus bus) {
        int total = bus.getTotalSeats();
        List<Seat> seats = new ArrayList<>();
        int cols = 2;
        int rows = (total + cols - 1) / cols;
        boolean isDoubleDecker = bus.getBusType().name().contains("DOUBLE");
        int floors = isDoubleDecker ? 2 : 1;
        int rowsPerFloor = rows / floors;

        for (int f = 1; f <= floors; f++) {
            for (int r = 1; r <= rowsPerFloor; r++) {
                for (int c = 1; c <= cols; c++) {
                    int seatNum = (f - 1) * rowsPerFloor * cols + (r - 1) * cols + c;
                    if (seatNum > total) break;
                    String label = String.format("%s%02d", (char) ('A' + c - 1), r + (f - 1) * rowsPerFloor);
                    seats.add(Seat.builder()
                        .trip(trip).seatNumber(label).floor(f)
                        .status(SeatStatus.AVAILABLE).build());
                }
            }
        }
        seatRepository.saveAll(seats);
    }

    @Override
    public Trip update(Long id, TripRequest request) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", id));
        Route route = routeRepository.findById(request.getRouteId())
            .orElseThrow(() -> new ResourceNotFoundException("Tuyến đường", request.getRouteId()));
        Bus bus = busRepository.findById(request.getBusId())
            .orElseThrow(() -> new ResourceNotFoundException("Xe", request.getBusId()));

        trip.setRoute(route);
        trip.setBus(bus);
        trip.setDepartureTime(request.getDepartureTime());
        trip.setArrivalTime(request.getArrivalTime());
        trip.setPrice(request.getPrice());
        return tripRepository.save(trip);
    }

    @Override
    public void delete(Long id) {
        Trip trip = tripRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Chuyến xe", id));
        trip.setActive(false);
        tripRepository.save(trip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Trip> findAll() {
        return tripRepository.findAllActiveWithDetails();
    }
}
