package com.assignment.dispatch.controller;

import com.assignment.dispatch.dto.ApiResponseDto;
import com.assignment.dispatch.dto.DispatchPlanResponseDto;
import com.assignment.dispatch.dto.OrderRequestDto;
import com.assignment.dispatch.dto.VehicleRequestDto;
import com.assignment.dispatch.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponseDto> acceptOrders(@Valid @RequestBody OrderRequestDto.BatchOrders ordersBatch) {
        ApiResponseDto response = dispatchService.saveOrders(ordersBatch);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vehicles")
    public ResponseEntity<ApiResponseDto> acceptVehicles(@Valid @RequestBody VehicleRequestDto.BatchVehicles vehiclesBatch) {
        ApiResponseDto response = dispatchService.saveVehicles(vehiclesBatch);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/plan")
    public ResponseEntity<DispatchPlanResponseDto> getDispatchPlan() {
        DispatchPlanResponseDto plan = dispatchService.calculateDispatchPlan();
        return ResponseEntity.ok(plan);
    }
}