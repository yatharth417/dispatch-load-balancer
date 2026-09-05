package com.assignment.dispatch.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchPlanResponseDto {

    private List<VehiclePlan> dispatchPlan;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<OrderRequestDto.OrderItem> unassignedOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehiclePlan {
        private String vehicleId;
        private Double totalLoad;
        private String totalDistance;
        private List<OrderRequestDto.OrderItem> assignedOrders;
    }
}