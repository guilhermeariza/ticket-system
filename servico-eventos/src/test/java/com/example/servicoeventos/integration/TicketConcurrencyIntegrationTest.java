package com.example.servicoeventos.integration;

import com.example.servicoeventos.exception.InsufficientTicketsException;
import com.example.servicoeventos.model.Event;
import com.example.servicoeventos.model.TicketType;
import com.example.servicoeventos.repository.EventRepository;
import com.example.servicoeventos.repository.TicketTypeRepository;
import com.example.servicoeventos.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the fix for the ticket oversell race condition: concurrent buyers racing for the
 * last available ticket must yield exactly one winner, backed by an atomic conditional
 * UPDATE rather than a read-then-write. Deliberately not @Transactional at the class/method
 * level - each thread must run decrementTicketQuantity in its own real transaction to
 * exercise actual DB-level concurrency, not a single Spring-managed test transaction.
 */
@SpringBootTest
@ActiveProfiles("test")
class TicketConcurrencyIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    private Long ticketTypeId;

    @BeforeEach
    void setUp() {
        Event event = new Event();
        event.setName("Concurrency Test Event");
        event.setDescription("Event used to test atomic ticket decrement");
        event.setDate(LocalDateTime.now().plusDays(7));
        event.setLocation("Test Location");
        event = eventRepository.save(event);

        TicketType ticketType = new TicketType();
        ticketType.setName("Last Ticket");
        ticketType.setPrice(new BigDecimal("50.00"));
        ticketType.setTotalQuantity(1);
        ticketType.setAvailableQuantity(1);
        ticketType.setEvent(event);
        ticketType = ticketTypeRepository.save(ticketType);

        ticketTypeId = ticketType.getId();
    }

    @Test
    void decrementTicketQuantity_UnderConcurrency_OnlyOneBuyerSucceeds() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger insufficientCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventService.decrementTicketQuantity(ticketTypeId, 1);
                    successCount.incrementAndGet();
                } catch (InsufficientTicketsException e) {
                    insufficientCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "all threads should finish within the timeout");
        assertEquals(1, successCount.get(), "exactly one buyer should win the last ticket");
        assertEquals(threadCount - 1, insufficientCount.get(), "every other buyer should see InsufficientTicketsException");
        assertEquals(0, ticketTypeRepository.findById(ticketTypeId).orElseThrow().getAvailableQuantity());
    }
}
