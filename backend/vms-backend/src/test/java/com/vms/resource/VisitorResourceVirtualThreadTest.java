package com.vms.resource;

import com.vms.entity.Personnel;
import com.vms.entity.Visitor;
import com.vms.repository.PersonnelRepository;
import com.vms.repository.VisitorRepository;
import com.vms.service.VisitorService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class VisitorResourceVirtualThreadTest {

    @Test
    void testCheckOutEndpointHasRunOnVirtualThreadAnnotation() throws NoSuchMethodException {
        Method checkOutMethod = VisitorResource.class.getMethod("checkOut", jakarta.ws.rs.core.SecurityContext.class, Long.class);
        Assertions.assertTrue(
            checkOutMethod.isAnnotationPresent(RunOnVirtualThread.class),
            "@RunOnVirtualThread annotation must be present on VisitorResource.checkOut(Long id)"
        );
    }

    @Test
    void testCheckOutUpdatesVisitorAndExecutesVirtualThreadSimulation() throws Throwable {
        Personnel host = new Personnel("Ayşe Demir", "Yazılım", "Uzman", "ayse@firma.com");
        Visitor activeVisitor = new Visitor("Ahmet Yılmaz", host, LocalDateTime.now().minusHours(1), null, true);
        activeVisitor.setId(100L);

        VisitorRepository stubVisitorRepo = new VisitorRepository() {
            @Override
            public Optional<Visitor> findByIdWithHost(Long id) {
                if (id.equals(100L)) {
                    return Optional.of(activeVisitor);
                }
                return Optional.empty();
            }
        };

        PersonnelRepository stubPersonnelRepo = new PersonnelRepository();
        com.vms.service.NotificationService stubNotificationService = new com.vms.service.NotificationService(null, null);
        VisitorService service = new VisitorService(stubVisitorRepo, stubPersonnelRepo, stubNotificationService);

        AtomicBoolean isVirtualThreadExecuted = new AtomicBoolean(false);

        // Execute checkout on a Virtual Thread explicitly to test Virtual Thread behavior
        Thread vThread = Thread.ofVirtual().start(() -> {
            Visitor result = service.checkOut(100L);

            Assertions.assertFalse(result.getIsInside(), "Visitor isInside must be false after checkout");
            Assertions.assertNotNull(result.getExitTime(), "Visitor exitTime must not be null after checkout");
            Assertions.assertTrue(Thread.currentThread().isVirtual(), "Thread running checkout simulation must be virtual");

            isVirtualThreadExecuted.set(true);
        });

        vThread.join();

        Assertions.assertTrue(isVirtualThreadExecuted.get(), "Checkout logic must complete on virtual thread");
    }
}
