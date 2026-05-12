package org.example.busticketpro.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.busticketpro.entity.*;
import org.example.busticketpro.enums.BusType;
import org.example.busticketpro.enums.Role;
import org.example.busticketpro.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final RouteRepository routeRepository;
    private final BusRepository busRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (locationRepository.count() > 0) {
            log.info("Data already initialized, skipping...");
            return;
        }
        log.info("Initializing seed data...");
        seedUsers();
        List<Location> locations = seedLocations();
        List<Route> routes = seedRoutes(locations);
        List<Bus> buses = seedBuses();
        seedTripsAndSeats(routes, buses);
        log.info("Seed data initialized successfully!");
    }

    private void seedUsers() {
        // Admin
        userRepository.save(User.builder()
            .username("admin").passwordHash(passwordEncoder.encode("Admin@123"))
            .role(Role.ADMIN).fullName("Quản trị viên").phone("0901234567")
            .email("admin@busticketpro.vn").enabled(true).build());

        // Staff
        userRepository.save(User.builder()
            .username("staff1").passwordHash(passwordEncoder.encode("Staff@123"))
            .role(Role.STAFF).fullName("Nguyễn Văn Nhân Viên").phone("0912345678")
            .email("staff1@busticketpro.vn").enabled(true).build());

        // Demo passenger
        userRepository.save(User.builder()
            .username("passenger1").passwordHash(passwordEncoder.encode("Pass@123"))
            .role(Role.PASSENGER).fullName("Trần Thị Hành Khách").phone("0923456789")
            .email("passenger1@example.com").enabled(true).build());

        log.info("Users seeded: admin/Admin@123, staff1/Staff@123, passenger1/Pass@123");
    }

    private List<Location> seedLocations() {
        var locations = List.of(
            Location.builder().name("Hà Nội").province("Hà Nội").address("Bến xe Giáp Bát, Hoàng Mai, Hà Nội").build(),
            Location.builder().name("Hải Phòng").province("Hải Phòng").address("Bến xe Niệm Nghĩa, Lê Chân, Hải Phòng").build(),
            Location.builder().name("Nam Định").province("Nam Định").address("Bến xe Nam Định, TP. Nam Định").build(),
            Location.builder().name("Thanh Hóa").province("Thanh Hóa").address("Bến xe Thanh Hóa, TP. Thanh Hóa").build(),
            Location.builder().name("Vinh").province("Nghệ An").address("Bến xe Vinh, TP. Vinh, Nghệ An").build(),
            Location.builder().name("Huế").province("Thừa Thiên Huế").address("Bến xe Phía Nam, TP. Huế").build(),
            Location.builder().name("Đà Nẵng").province("Đà Nẵng").address("Bến xe Đà Nẵng, Liên Chiểu, Đà Nẵng").build(),
            Location.builder().name("Hồ Chí Minh").province("Hồ Chí Minh").address("Bến xe Miền Đông, Bình Thạnh, TP.HCM").build(),
            Location.builder().name("Cần Thơ").province("Cần Thơ").address("Bến xe Cần Thơ, Ninh Kiều, Cần Thơ").build(),
            Location.builder().name("Nha Trang").province("Khánh Hòa").address("Bến xe Nha Trang, TP. Nha Trang").build()
        );
        return locationRepository.saveAll(locations);
    }

    private List<Route> seedRoutes(List<Location> locs) {
        Map<String, Location> locMap = new java.util.HashMap<>();
        locs.forEach(l -> locMap.put(l.getName(), l));

        var routes = List.of(
            Route.builder().departureLocation(locMap.get("Hà Nội")).arrivalLocation(locMap.get("Hồ Chí Minh"))
                .distanceKm(1726).basePrice(new BigDecimal("450000")).durationMinutes(1800).build(),
            Route.builder().departureLocation(locMap.get("Hồ Chí Minh")).arrivalLocation(locMap.get("Hà Nội"))
                .distanceKm(1726).basePrice(new BigDecimal("450000")).durationMinutes(1800).build(),
            Route.builder().departureLocation(locMap.get("Hà Nội")).arrivalLocation(locMap.get("Đà Nẵng"))
                .distanceKm(764).basePrice(new BigDecimal("280000")).durationMinutes(840).build(),
            Route.builder().departureLocation(locMap.get("Đà Nẵng")).arrivalLocation(locMap.get("Hà Nội"))
                .distanceKm(764).basePrice(new BigDecimal("280000")).durationMinutes(840).build(),
            Route.builder().departureLocation(locMap.get("Hồ Chí Minh")).arrivalLocation(locMap.get("Đà Nẵng"))
                .distanceKm(963).basePrice(new BigDecimal("320000")).durationMinutes(960).build(),
            Route.builder().departureLocation(locMap.get("Đà Nẵng")).arrivalLocation(locMap.get("Hồ Chí Minh"))
                .distanceKm(963).basePrice(new BigDecimal("320000")).durationMinutes(960).build(),
            Route.builder().departureLocation(locMap.get("Hà Nội")).arrivalLocation(locMap.get("Hải Phòng"))
                .distanceKm(120).basePrice(new BigDecimal("80000")).durationMinutes(120).build(),
            Route.builder().departureLocation(locMap.get("Hải Phòng")).arrivalLocation(locMap.get("Hà Nội"))
                .distanceKm(120).basePrice(new BigDecimal("80000")).durationMinutes(120).build(),
            Route.builder().departureLocation(locMap.get("Hồ Chí Minh")).arrivalLocation(locMap.get("Cần Thơ"))
                .distanceKm(169).basePrice(new BigDecimal("100000")).durationMinutes(180).build(),
            Route.builder().departureLocation(locMap.get("Hồ Chí Minh")).arrivalLocation(locMap.get("Nha Trang"))
                .distanceKm(447).basePrice(new BigDecimal("200000")).durationMinutes(480).build()
        );
        return routeRepository.saveAll(routes);
    }

    private List<Bus> seedBuses() {
        var buses = List.of(
            Bus.builder().licensePlate("51B-12345").busType(BusType.SLEEPER_34).totalSeats(34)
                .company("Phương Trang").driverName("Nguyễn Văn An").driverPhone("0901111111")
                .color("Xanh dương").amenities("WiFi, Điều hoà, TV").build(),
            Bus.builder().licensePlate("29A-56789").busType(BusType.STANDARD_45).totalSeats(45)
                .company("Hoàng Long").driverName("Trần Văn Bình").driverPhone("0902222222")
                .color("Trắng").amenities("Điều hoà, Nước uống").build(),
            Bus.builder().licensePlate("43C-11111").busType(BusType.LIMOUSINE_20).totalSeats(20)
                .company("VeXeRe Premium").driverName("Lê Văn Cường").driverPhone("0903333333")
                .color("Đen").amenities("WiFi, Điều hoà, USB sạc, Màn hình riêng").build(),
            Bus.builder().licensePlate("51D-99999").busType(BusType.DOUBLE_DECKER_40).totalSeats(40)
                .company("Thành Bưởi").driverName("Phạm Văn Dũng").driverPhone("0904444444")
                .color("Đỏ").amenities("WiFi, Điều hoà, Nhà vệ sinh").build(),
            Bus.builder().licensePlate("30E-44444").busType(BusType.STANDARD_29).totalSeats(29)
                .company("Sao Việt").driverName("Hoàng Văn Em").driverPhone("0905555555")
                .color("Vàng").amenities("Điều hoà").build()
        );
        return busRepository.saveAll(buses);
    }

    private void seedTripsAndSeats(List<Route> routes, List<Bus> buses) {
        LocalDateTime now = LocalDateTime.now();
        // Create trips for next 7 days
        for (int day = 0; day < 7; day++) {
            for (int i = 0; i < Math.min(routes.size(), 5); i++) {
                Route route = routes.get(i);
                Bus bus = buses.get(i % buses.size());
                LocalDateTime departure = now.plusDays(day).withHour(6 + (i * 3) % 15).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime arrival = departure.plusMinutes(route.getDurationMinutes());

                Trip trip = tripRepository.save(Trip.builder()
                    .route(route).bus(bus)
                    .departureTime(departure).arrivalTime(arrival)
                    .price(route.getBasePrice()).active(true).build());

                // Create seats for the trip
                int totalSeats = bus.getTotalSeats();
                int cols = 2;
                int rows = (totalSeats + cols - 1) / cols;
                int floor = (bus.getBusType() == BusType.DOUBLE_DECKER_40) ? 2 : 1;

                for (int f = 1; f <= floor; f++) {
                    for (int r = 1; r <= rows / floor; r++) {
                        for (int c = 1; c <= cols; c++) {
                            int seatNum = (f - 1) * (rows / floor * cols) + (r - 1) * cols + c;
                            if (seatNum > totalSeats) break;
                            String seatLabel = String.format("%s%02d", (char)('A' + c - 1), r + (f - 1) * (rows / floor));
                            seatRepository.save(Seat.builder()
                                .trip(trip).seatNumber(seatLabel).floor(f).build());
                        }
                    }
                }
            }
        }
    }
}
