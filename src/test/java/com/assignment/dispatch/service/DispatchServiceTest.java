package com.assignment.dispatch.service;

import com.assignment.dispatch.dto.DispatchPlanResponseDto;
import com.assignment.dispatch.entity.OrderEntity;
import com.assignment.dispatch.entity.Priority;
import com.assignment.dispatch.entity.VehicleEntity;
import com.assignment.dispatch.repository.OrderRepository;
import com.assignment.dispatch.repository.VehicleRepository;
import com.assignment.dispatch.service.impl.DispatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    private DispatchService dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new DispatchServiceImpl(orderRepository, vehicleRepository);
    }

    @Test
    @DisplayName("High priority orders should be assigned before low priority orders")
    void testPriorityAssignment() {
        VehicleEntity vehicle = VehicleEntity.builder()
                .vehicleId("VEH001")
                .capacity(50.0)
                .currentLatitude(12.9716)
                .currentLongitude(77.5946)
                .currentAddress("Depot")
                .build();

        OrderEntity lowOrder = OrderEntity.builder()
                .orderId("ORD_LOW")
                .latitude(12.9718)
                .longitude(77.5948)
                .packageWeight(30.0)
                .priority(Priority.LOW)
                .address("Nearby Low")
                .build();

        OrderEntity highOrder = OrderEntity.builder()
                .orderId("ORD_HIGH")
                .latitude(12.9720)
                .longitude(77.5950)
                .packageWeight(30.0)
                .priority(Priority.HIGH)
                .address("Nearby High")
                .build();

        when(vehicleRepository.findAll()).thenReturn(List.of(vehicle));
        when(orderRepository.findAll()).thenReturn(List.of(lowOrder, highOrder));

        DispatchPlanResponseDto plan = dispatchService.calculateDispatchPlan();

        assertEquals(1, plan.getDispatchPlan().get(0).getAssignedOrders().size());
        assertEquals("ORD_HIGH", plan.getDispatchPlan().get(0).getAssignedOrders().get(0).getOrderId());
        assertEquals(1, plan.getUnassignedOrders().size());
        assertEquals("ORD_LOW", plan.getUnassignedOrders().get(0).getOrderId());
    }
}