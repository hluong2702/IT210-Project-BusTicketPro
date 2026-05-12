package org.example.busticketpro.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.busticketpro.entity.Location;
import org.example.busticketpro.exception.BusinessException;
import org.example.busticketpro.repository.LocationRepository;
import org.example.busticketpro.service.LocationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {
    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Location> findAll() {
        return locationRepository.findByActiveTrue();
    }

    @Override
    public Location create(String name, String province, String address) {
        if (locationRepository.existsByName(name))
            throw new BusinessException("Địa điểm đã tồn tại: " + name);
        return locationRepository.save(Location.builder()
            .name(name).province(province).address(address).active(true).build());
    }
}
