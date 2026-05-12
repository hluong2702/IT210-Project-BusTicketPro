package org.example.busticketpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.busticketpro.dto.request.BusRequest;
import org.example.busticketpro.entity.Bus;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.exception.ResourceNotFoundException;
import org.example.busticketpro.repository.BusRepository;
import org.example.busticketpro.service.BusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BusServiceImpl implements BusService {

    private final BusRepository busRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Bus> findAll() {
        return busRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Bus findById(Long id) {
        return busRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Xe", id));
    }

    @Override
    public Bus create(BusRequest request) {
        if (busRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BusinessException("DUPLICATE_LICENSE", "Biển số xe đã tồn tại");
        }
        Bus bus = Bus.builder()
            .licensePlate(request.getLicensePlate().toUpperCase())
            .busType(request.getBusType())
            .totalSeats(request.getBusType().getSeatCount())
            .company(request.getCompany())
            .driverName(request.getDriverName())
            .driverPhone(request.getDriverPhone())
            .color(request.getColor())
            .amenities(request.getAmenities())
            .build();
        return busRepository.save(bus);
    }

    @Override
    public Bus update(Long id, BusRequest request) {
        Bus bus = findById(id);
        if (!bus.getLicensePlate().equals(request.getLicensePlate()) &&
            busRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new BusinessException("DUPLICATE_LICENSE", "Biển số xe đã tồn tại");
        }
        bus.setLicensePlate(request.getLicensePlate().toUpperCase());
        bus.setBusType(request.getBusType());
        bus.setTotalSeats(request.getBusType().getSeatCount());
        bus.setCompany(request.getCompany());
        bus.setDriverName(request.getDriverName());
        bus.setDriverPhone(request.getDriverPhone());
        bus.setColor(request.getColor());
        bus.setAmenities(request.getAmenities());
        return busRepository.save(bus);
    }

    @Override
    public void delete(Long id) {
        Bus bus = findById(id);
        bus.setActive(false);
        busRepository.save(bus);
    }
}
