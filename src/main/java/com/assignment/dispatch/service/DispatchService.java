package com.assignment.dispatch.service;

import com.assignment.dispatch.dto.ApiResponseDto;
import com.assignment.dispatch.dto.DispatchPlanResponseDto;
import com.assignment.dispatch.dto.OrderRequestDto;
import com.assignment.dispatch.dto.VehicleRequestDto;

public interface DispatchService {
    ApiResponseDto saveOrders(OrderRequestDto.BatchOrders orders);
    ApiResponseDto saveVehicles(VehicleRequestDto.BatchVehicles vehicles);
    DispatchPlanResponseDto calculateDispatchPlan();
}