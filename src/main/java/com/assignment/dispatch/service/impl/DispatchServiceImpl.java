package com.assignment.dispatch.service.impl;

import com.assignment.dispatch.dto.ApiResponseDto;
import com.assignment.dispatch.dto.DispatchPlanResponseDto;
import com.assignment.dispatch.dto.OrderRequestDto;
import com.assignment.dispatch.dto.VehicleRequestDto;
import com.assignment.dispatch.entity.OrderEntity;
import com.assignment.dispatch.entity.VehicleEntity;
import com.assignment.dispatch.exception.InvalidInputException;
import com.assignment.dispatch.repository.OrderRepository;
import com.assignment.dispatch.repository.VehicleRepository;
import com.assignment.dispatch.service.DispatchService;
import com.assignment.dispatch.util.HaversineDistanceCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {

    private final OrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public ApiResponseDto saveOrders(OrderRequestDto.BatchOrders batch) {
        if (batch == null || batch.getOrders() == null || batch.getOrders().isEmpty()) {
            throw new InvalidInputException("Orders payload cannot be empty");
        }

        List<OrderEntity> entities = batch.getOrders().stream().map(dto ->
                OrderEntity.builder()
                        .orderId(dto.getOrderId())
                        .latitude(dto.getLatitude())
                        .longitude(dto.getLongitude())
                        .address(dto.getAddress())
                        .packageWeight(dto.getPackageWeight())
                        .priority(dto.getPriority())
                        .build()
        ).collect(Collectors.toList());

        orderRepository.saveAll(entities);

        return ApiResponseDto.builder()
                .message("Delivery orders accepted.")
                .status("success")
                .build();
    }

    @Override
    @Transactional
    public ApiResponseDto saveVehicles(VehicleRequestDto.BatchVehicles batch) {
        if (batch == null || batch.getVehicles() == null || batch.getVehicles().isEmpty()) {
            throw new InvalidInputException("Vehicles payload cannot be empty");
        }

        List<VehicleEntity> entities = batch.getVehicles().stream().map(dto ->
                VehicleEntity.builder()
                        .vehicleId(dto.getVehicleId())
                        .capacity(dto.getCapacity())
                        .currentLatitude(dto.getCurrentLatitude())
                        .currentLongitude(dto.getCurrentLongitude())
                        .currentAddress(dto.getCurrentAddress())
                        .build()
        ).collect(Collectors.toList());

        vehicleRepository.saveAll(entities);

        return ApiResponseDto.builder()
                .message("Vehicle details accepted.")
                .status("success")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchPlanResponseDto calculateDispatchPlan() {
        List<OrderEntity> orders = orderRepository.findAll();
        List<VehicleEntity> vehicles = vehicleRepository.findAll();

        if (vehicles.isEmpty()) {
            throw new InvalidInputException("No vehicles registered in the fleet.");
        }

        // Sort orders strictly by Priority rank (HIGH=1, MEDIUM=2, LOW=3), then by package weight descending
        List<OrderEntity> sortedOrders = orders.stream()
                .sorted(Comparator.comparingInt((OrderEntity o) -> o.getPriority().getRank())
                        .thenComparing(Comparator.comparingDouble(OrderEntity::getPackageWeight).reversed()))
                .toList();

        // Working state wrapper for vehicles during assignment
        class VehicleRouteState {
            final VehicleEntity entity;
            double currentLoad = 0.0;
            double lastLat;
            double lastLon;
            final List<OrderEntity> assigned = new ArrayList<>();

            VehicleRouteState(VehicleEntity vehicle) {
                this.entity = vehicle;
                this.lastLat = vehicle.getCurrentLatitude();
                this.lastLon = vehicle.getCurrentLongitude();
            }

            boolean canFit(double weight) {
                return (currentLoad + weight) <= entity.getCapacity();
            }

            void assign(OrderEntity order) {
                assigned.add(order);
                currentLoad += order.getPackageWeight();
                lastLat = order.getLatitude();
                lastLon = order.getLongitude();
            }
        }

        List<VehicleRouteState> routeStates = vehicles.stream()
                .map(VehicleRouteState::new)
                .collect(Collectors.toList());

        List<OrderEntity> unassignable = new ArrayList<>();

        for (OrderEntity order : sortedOrders) {
            VehicleRouteState bestVehicle = null;
            double minDistance = Double.MAX_VALUE;

            for (VehicleRouteState vState : routeStates) {
                if (vState.canFit(order.getPackageWeight())) {
                    double dist = HaversineDistanceCalculator.calculateDistance(
                            vState.lastLat, vState.lastLon,
                            order.getLatitude(), order.getLongitude()
                    );
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestVehicle = vState;
                    }
                }
            }

            if (bestVehicle != null) {
                bestVehicle.assign(order);
            } else {
                unassignable.add(order);
            }
        }

        // Build Response
        List<DispatchPlanResponseDto.VehiclePlan> vehiclePlans = new ArrayList<>();

        for (VehicleRouteState vState : routeStates) {
            double totalDistKm = 0.0;
            double currentLat = vState.entity.getCurrentLatitude();
            double currentLon = vState.entity.getCurrentLongitude();

            List<OrderRequestDto.OrderItem> orderDtos = new ArrayList<>();
            for (OrderEntity order : vState.assigned) {
                totalDistKm += HaversineDistanceCalculator.calculateDistance(
                        currentLat, currentLon, order.getLatitude(), order.getLongitude()
                );
                currentLat = order.getLatitude();
                currentLon = order.getLongitude();

                orderDtos.add(OrderRequestDto.OrderItem.builder()
                        .orderId(order.getOrderId())
                        .latitude(order.getLatitude())
                        .longitude(order.getLongitude())
                        .address(order.getAddress())
                        .packageWeight(order.getPackageWeight())
                        .priority(order.getPriority())
                        .build());
            }

            // Round distance to nearest integer km or formatted string
            String formattedDistance = Math.round(totalDistKm) + " km";

            vehiclePlans.add(DispatchPlanResponseDto.VehiclePlan.builder()
                    .vehicleId(vState.entity.getVehicleId())
                    .totalLoad(vState.currentLoad)
                    .totalDistance(formattedDistance)
                    .assignedOrders(orderDtos)
                    .build());
        }

        List<OrderRequestDto.OrderItem> unassignedDtos = unassignable.stream().map(o ->
                OrderRequestDto.OrderItem.builder()
                        .orderId(o.getOrderId())
                        .latitude(o.getLatitude())
                        .longitude(o.getLongitude())
                        .address(o.getAddress())
                        .packageWeight(o.getPackageWeight())
                        .priority(o.getPriority())
                        .build()
        ).collect(Collectors.toList());

        return DispatchPlanResponseDto.builder()
                .dispatchPlan(vehiclePlans)
                .unassignedOrders(unassignedDtos)
                .build();
    }
}