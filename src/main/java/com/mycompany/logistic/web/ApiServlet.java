package com.mycompany.logistic.web;

import com.mycompany.logistic.model.CustomerOrder;
import com.mycompany.logistic.model.Driver;
import com.mycompany.logistic.model.InventoryItem;
import com.mycompany.logistic.model.Shipment;
import com.mycompany.logistic.model.ShipmentEvent;
import com.mycompany.logistic.model.Vehicle;
import com.mycompany.logistic.service.AipPlanner;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet("/api/*")
public class ApiServlet extends JsonServlet {
    private static final List<String> SHIPMENT_STATUSES =
            List.of("CREATED", "PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED", "DELAYED", "CANCELLED");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireRole(request, response, "ADMIN", "WAREHOUSE_STAFF", "DELIVERY_PERSONNEL")) {
            return;
        }
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        try (EntityManager em = entityManager()) {
            switch (path) {
                case "/", "/dashboard" -> dashboard(em, response, request);
                case "/shipments" -> json(response, 200, Map.of("shipments", em.createQuery(
                        "select s from Shipment s order by s.createdAt desc", Shipment.class)
                        .setMaxResults(80).getResultList().stream().map(ApiServlet::shipmentMap).toList()));
                case "/orders" -> json(response, 200, Map.of("orders", em.createQuery(
                        "select o from CustomerOrder o order by o.createdAt desc", CustomerOrder.class)
                        .setMaxResults(100).getResultList().stream().map(ApiServlet::orderMap).toList()));
                case "/inventory" -> json(response, 200, Map.of("inventory", em.createQuery(
                        "select i from InventoryItem i order by i.quantity asc", InventoryItem.class)
                        .setMaxResults(100).getResultList().stream().map(ApiServlet::inventoryMap).toList()));
                case "/vehicles" -> json(response, 200, Map.of("vehicles", em.createQuery(
                        "select v from Vehicle v order by v.status, v.capacityKg", Vehicle.class)
                        .getResultList().stream().map(ApiServlet::vehicleMap).toList()));
                case "/drivers" -> json(response, 200, Map.of("drivers", em.createQuery(
                        "select d from Driver d order by d.fullName", Driver.class)
                        .getResultList().stream().map(ApiServlet::driverMap).toList()));
                case "/route" -> {
                    try {
                        json(response, 200, AipPlanner.shortestPath(
                                request.getParameter("from"), request.getParameter("to")));
                    } catch (IllegalArgumentException ex) {
                        error(response, 400, ex.getMessage());
                    }
                }
                default -> {
                    if (path.startsWith("/shipments/") && path.endsWith("/history")) {
                        String[] parts = path.split("/");
                        if (parts.length != 4) {
                            error(response, 404, "Shipment history endpoint not found.");
                        } else {
                            List<Map<String, Object>> events = em.createQuery(
                                    "select e from ShipmentEvent e, Shipment s where e.shipmentId = s.id "
                                            + "and s.trackingNumber = :tracking order by e.occurredAt desc",
                                    ShipmentEvent.class).setParameter("tracking", parts[2]).getResultList().stream()
                                    .map(e -> Map.<String, Object>of("status", e.status, "note",
                                            e.note == null ? "" : e.note, "occurredAt", e.occurredAt.format(DATE)))
                                    .toList();
                            json(response, 200, Map.of("events", events));
                        }
                    } else {
                        error(response, 404, "API endpoint not found.");
                    }
                }
            }
        } catch (RuntimeException ex) {
            getServletContext().log("API request failed: " + path, ex);
            error(response, 500, "The request could not be completed.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        if ("/shipments".equals(path)) {
            if (!requireRole(request, response, "ADMIN", "WAREHOUSE_STAFF")) {
                return;
            }
            createShipment(request, response);
        } else if (path.startsWith("/shipments/") && path.endsWith("/status")) {
            if (!requireRole(request, response, "ADMIN", "WAREHOUSE_STAFF", "DELIVERY_PERSONNEL")) {
                return;
            }
            updateStatus(request, response, path);
        } else if ("/orders".equals(path)) {
            if (!requireRole(request, response, "ADMIN", "WAREHOUSE_STAFF")) {
                return;
            }
            createOrder(request, response);
        } else if ("/optimize".equals(path)) {
            if (!requireRole(request, response, "ADMIN", "WAREHOUSE_STAFF")) {
                return;
            }
            optimize(request, response);
        } else if ("/inventory/adjust".equals(path)) {
            if (!requireRole(request, response, "ADMIN", "WAREHOUSE_STAFF")) {
                return;
            }
            adjustInventory(request, response);
        } else if (path.startsWith("/drivers/") && path.endsWith("/availability")) {
            if (!requireRole(request, response, "ADMIN", "WAREHOUSE_STAFF")) {
                return;
            }
            updateDriverAvailability(request, response, path);
        } else {
            error(response, 404, "API endpoint not found.");
        }
    }

    private void dashboard(EntityManager em, HttpServletResponse response, HttpServletRequest request) throws IOException {
        long orders = em.createQuery("select count(o) from CustomerOrder o", Long.class).getSingleResult();
        long pendingOrders = em.createQuery("select count(o) from CustomerOrder o where o.status in ('PENDING','PROCESSING')",
                Long.class).getSingleResult();
        long activeShipments = em.createQuery("select count(s) from Shipment s where s.status not in ('DELIVERED','CANCELLED')",
                Long.class).getSingleResult();
        long delivered = em.createQuery("select count(s) from Shipment s where s.status = 'DELIVERED'",
                Long.class).getSingleResult();
        long delayed = em.createQuery("select count(s) from Shipment s where s.status = 'DELAYED'",
                Long.class).getSingleResult();
        long lowStock = em.createQuery("select count(i) from InventoryItem i where i.quantity <= i.reorderLevel",
                Long.class).getSingleResult();
        List<Map<String, Object>> shipments = em.createQuery(
                "select s from Shipment s order by s.createdAt desc", Shipment.class).setMaxResults(7)
                .getResultList().stream().map(ApiServlet::shipmentMap).toList();
        Object fullName = request.getSession(false).getAttribute("fullName");
        Object role = request.getSession(false).getAttribute("role");
        json(response, 200, Map.of(
                "user", Map.of("name", fullName, "role", role),
                "metrics", Map.of("orders", orders, "pendingOrders", pendingOrders, "activeShipments", activeShipments,
                        "delivered", delivered, "delayed", delayed, "lowStock", lowStock),
                "recentShipments", shipments));
    }

    private void createShipment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> input = body(request);
        String recipient = stringValue(input.get("recipientName"), "");
        String destination = stringValue(input.get("destination"), "");
        BigDecimal weight = decimal(input.get("weightKg"));
        BigDecimal distance = decimal(input.get("distanceKm"));
        if (recipient.isBlank() || destination.isBlank() || weight == null || distance == null
                || weight.signum() <= 0 || distance.signum() <= 0 || weight.compareTo(new BigDecimal("100000")) > 0) {
            error(response, 400, "Recipient, destination, positive weight (up to 100,000 kg), and positive distance are required.");
            return;
        }
        String orderId = stringValue(input.get("orderId"), "");
        String requestedPriority = stringValue(input.get("priority"), "NORMAL");
        if (!List.of("NORMAL", "HIGH", "URGENT").contains(requestedPriority.toUpperCase())) {
            error(response, 400, "Priority must be NORMAL, HIGH, or URGENT.");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Shipment shipment = new Shipment();
        shipment.id = UUID.randomUUID().toString();
        shipment.trackingNumber = "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        shipment.recipientName = recipient;
        shipment.destination = destination;
        shipment.status = "CREATED";
        shipment.weightKg = weight;
        shipment.distanceKm = distance;
        shipment.priority = AipPlanner.priority(weight, distance, requestedPriority);
        shipment.createdAt = now;
        shipment.updatedAt = now;
        shipment.estimatedDelivery = now.plusHours(Math.max(2, distance.longValue() / 45));
        if (!orderId.isBlank()) {
            shipment.orderId = orderId;
        }
        EntityManager em = entityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (shipment.orderId != null) {
                CustomerOrder order = em.find(CustomerOrder.class, shipment.orderId);
                if (order == null) {
                    tx.rollback();
                    error(response, 400, "The linked order does not exist.");
                    return;
                }
                order.status = "PROCESSING";
            }
            List<Vehicle> available = em.createQuery("select v from Vehicle v where v.status = 'AVAILABLE' order by v.capacityKg",
                    Vehicle.class).getResultList();
            Vehicle assigned = AipPlanner.assignVehicle(available, weight, stringValue(input.get("vehicleType"), ""));
            if (assigned != null) {
                shipment.vehicleId = assigned.id;
                assigned.status = "IN_USE";
            }
            shipment.estimatedCost = AipPlanner.estimateCost(distance, weight,
                    assigned == null ? stringValue(input.get("vehicleType"), "") : assigned.vehicleType);
            em.persist(shipment);
            persistEvent(em, shipment, "CREATED", "Shipment created", currentUserId(request), now);
            tx.commit();
            json(response, 201, shipmentMap(shipment));
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            getServletContext().log("Shipment creation failed.", ex);
            error(response, 500, "Shipment could not be created.");
        } finally {
            em.close();
        }
    }

    private void updateStatus(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length != 4) {
            error(response, 404, "Shipment status endpoint not found.");
            return;
        }
        Map<String, Object> input = body(request);
        String status = stringValue(input.get("status"), "").toUpperCase();
        if (!SHIPMENT_STATUSES.contains(status)) {
            error(response, 400, "Unknown shipment status.");
            return;
        }
        String trackingNumber = parts[2];
        EntityManager em = entityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Shipment shipment = em.createQuery("select s from Shipment s where s.trackingNumber = :tracking",
                    Shipment.class).setParameter("tracking", trackingNumber).getResultStream().findFirst().orElse(null);
            if (shipment == null) {
                tx.rollback();
                error(response, 404, "Shipment not found.");
                return;
            }
            if ("DELIVERED".equals(shipment.status) || "CANCELLED".equals(shipment.status)) {
                tx.rollback();
                error(response, 409, "A completed or cancelled shipment cannot be changed.");
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            shipment.status = status;
            shipment.updatedAt = now;
            if ("DELAYED".equals(status) && !"URGENT".equals(shipment.priority)) {
                shipment.priority = "HIGH";
            }
            if (("DELIVERED".equals(status) || "CANCELLED".equals(status)) && shipment.vehicleId != null) {
                Vehicle vehicle = em.find(Vehicle.class, shipment.vehicleId);
                if (vehicle != null && "IN_USE".equals(vehicle.status)) {
                    vehicle.status = "AVAILABLE";
                }
            }
            persistEvent(em, shipment, status, stringValue(input.get("note"), ""), currentUserId(request), now);
            tx.commit();
            json(response, 200, shipmentMap(shipment));
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            getServletContext().log("Shipment status update failed.", ex);
            error(response, 500, "Shipment status could not be updated.");
        } finally {
            em.close();
        }
    }

    private void createOrder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> input = body(request);
        String customer = stringValue(input.get("customerName"), "");
        String destination = stringValue(input.get("destination"), "");
        BigDecimal weight = decimal(input.get("weightKg"));
        String priority = stringValue(input.get("priority"), "NORMAL").toUpperCase();
        if (customer.isBlank() || destination.isBlank() || weight == null || weight.signum() <= 0
                || !List.of("NORMAL", "HIGH", "URGENT").contains(priority)) {
            error(response, 400, "Customer, destination, positive weight, and valid priority are required.");
            return;
        }
        CustomerOrder order = new CustomerOrder();
        order.id = UUID.randomUUID().toString();
        order.orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.customerName = customer;
        order.destination = destination;
        order.totalWeightKg = weight;
        order.priority = priority;
        order.status = "PENDING";
        order.createdAt = LocalDateTime.now();
        try (EntityManager em = entityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            em.persist(order);
            tx.commit();
            json(response, 201, Map.of("id", order.id, "orderNumber", order.orderNumber,
                    "status", order.status, "createdAt", order.createdAt.format(DATE)));
        } catch (RuntimeException ex) {
            getServletContext().log("Order creation failed.", ex);
            error(response, 500, "Order could not be created.");
        }
    }

    private void optimize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> input = body(request);
        BigDecimal weight = decimal(input.get("weightKg"));
        BigDecimal distance = decimal(input.get("distanceKm"));
        if (weight == null || distance == null || weight.signum() <= 0 || distance.signum() <= 0) {
            error(response, 400, "Positive weightKg and distanceKm values are required.");
            return;
        }
        String vehicleType = stringValue(input.get("vehicleType"), "");
        try (EntityManager em = entityManager()) {
            List<Vehicle> vehicles = em.createQuery("select v from Vehicle v where v.status = 'AVAILABLE'",
                    Vehicle.class).getResultList();
            Vehicle selected = AipPlanner.assignVehicle(vehicles, weight, vehicleType);
            json(response, 200, Map.of("vehicle", selected == null ? Map.of() : vehicleMap(selected),
                    "estimatedCost", AipPlanner.estimateCost(distance, weight,
                            selected == null ? vehicleType : selected.vehicleType),
                    "priority", AipPlanner.priority(weight, distance, stringValue(input.get("priority"), "NORMAL"))));
        }
    }

    private void adjustInventory(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> input = body(request);
        String id = stringValue(input.get("id"), "");
        Object quantityInput = input.get("quantityChange");
        if (id.isBlank() || !(quantityInput instanceof Number)) {
            error(response, 400, "Inventory id and numeric quantityChange are required.");
            return;
        }

        int change = ((Number) quantityInput).intValue();
        if (change == 0) {
            error(response, 400, "quantityChange must not be zero.");
            return;
        }
        try (EntityManager em = entityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            InventoryItem item = em.find(InventoryItem.class, id);
            if (item == null || item.quantity + change < 0) {
                tx.rollback();
                error(response, item == null ? 404 : 409, item == null ? "Inventory item not found." : "Stock cannot become negative.");
                return;
            }
            item.quantity += change;
            item.updatedAt = LocalDateTime.now();
            tx.commit();
            json(response, 200, inventoryMap(item));
        } catch (RuntimeException ex) {
            getServletContext().log("Inventory adjustment failed.", ex);
            error(response, 500, "Inventory could not be updated.");
        }
    }

    private void updateDriverAvailability(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        String[] parts = path.split("/");
        if (parts.length != 4) {
            error(response, 404, "Driver availability endpoint not found.");
            return;
        }
        String status = stringValue(body(request).get("status"), "").toUpperCase();
        if (!List.of("AVAILABLE", "OFF_DUTY").contains(status)) {
            error(response, 400, "Driver status must be AVAILABLE or OFF_DUTY.");
            return;
        }
        try (EntityManager em = entityManager()) {
            EntityTransaction tx = em.getTransaction();
            tx.begin();
            Driver driver = em.find(Driver.class, parts[2]);
            if (driver == null) {
                tx.rollback();
                error(response, 404, "Driver not found.");
                return;
            }
            driver.status = status;
            tx.commit();
            json(response, 200, driverMap(driver));
        } catch (RuntimeException ex) {
            getServletContext().log("Driver availability update failed.", ex);
            error(response, 500, "Driver availability could not be updated.");
        }
    }

    private static void persistEvent(EntityManager em, Shipment shipment, String status, String note,
            String changedBy, LocalDateTime at) {
        ShipmentEvent event = new ShipmentEvent();
        event.shipmentId = shipment.id;
        event.status = status;
        event.note = note.isBlank() ? null : note;
        event.changedBy = changedBy;
        event.occurredAt = at;
        em.persist(event);
    }

    private static BigDecimal decimal(Object value) {
        try {
            return value == null ? null : new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Map<String, Object> shipmentMap(Shipment s) {
        return Map.ofEntries(Map.entry("id", s.id), Map.entry("trackingNumber", s.trackingNumber),
                Map.entry("recipientName", s.recipientName), Map.entry("destination", s.destination),
                Map.entry("status", s.status), Map.entry("weightKg", s.weightKg),
                Map.entry("distanceKm", s.distanceKm), Map.entry("priority", s.priority),
                Map.entry("estimatedCost", s.estimatedCost), Map.entry("vehicleId", s.vehicleId == null ? "" : s.vehicleId),
                Map.entry("createdAt", s.createdAt == null ? "" : s.createdAt.format(DATE)));
    }

    private static Map<String, Object> inventoryMap(InventoryItem item) {
        return Map.of("id", item.id, "warehouseId", item.warehouseId, "sku", item.sku,
                "productName", item.productName, "quantity", item.quantity, "reorderLevel", item.reorderLevel,
                "updatedAt", item.updatedAt == null ? "" : item.updatedAt.format(DATE),
                "lowStock", item.quantity <= item.reorderLevel);
    }

    private static Map<String, Object> vehicleMap(Vehicle v) {
        return Map.of("id", v.id, "registrationNumber", v.registrationNumber, "vehicleType", v.vehicleType,
                "capacityKg", v.capacityKg, "status", v.status, "costPerKm", v.costPerKm);
    }

    private static Map<String, Object> orderMap(CustomerOrder o) {
        return Map.of("id", o.id, "orderNumber", o.orderNumber, "customerName", o.customerName,
                "destination", o.destination, "status", o.status, "weightKg", o.totalWeightKg,
                "priority", o.priority, "createdAt", o.createdAt.format(DATE));
    }

    private static Map<String, Object> driverMap(Driver d) {
        return Map.of("id", d.id, "fullName", d.fullName, "phone", d.phone,
                "status", d.status, "warehouseId", d.warehouseId == null ? "" : d.warehouseId);
    }
}
