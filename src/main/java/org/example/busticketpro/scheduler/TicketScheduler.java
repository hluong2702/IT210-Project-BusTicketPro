package org.example.busticketpro.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.busticketpro.repository.SeatRepository;
import org.example.busticketpro.service.TicketService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketScheduler {

    private final TicketService ticketService;
    private final SeatRepository seatRepository;

    /**
     * Every 10 minutes: auto-cancel PENDING tickets older than 30 minutes
     * CORE-08 extension + Extension 3
     */
    @Scheduled(fixedDelay = 600_000) // 10 minutes
    @Transactional
    public void autoCancelExpiredTickets() {
        log.debug("Running auto-cancel job at {}", LocalDateTime.now());
        ticketService.autoCancelExpired();
    }

    /**
     * Every 5 minutes: release expired seat holds
     */
    @Scheduled(fixedDelay = 300_000) // 5 minutes
    @Transactional
    public void releaseExpiredSeatHolds() {
        int released = seatRepository.releaseExpiredHolds(LocalDateTime.now());
        if (released > 0) {
            log.info("Released {} expired seat holds", released);
        }
    }
}
