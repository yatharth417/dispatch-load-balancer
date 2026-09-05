package com.assignment.dispatch.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VehicleRequestDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonDeserialize(using = BatchVehiclesDeserializer.class)
    public static class BatchVehicles {
        @NotEmpty(message = "Vehicles list cannot be empty")
        @Valid
        private List<VehicleItem> vehicles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleItem {
        @NotBlank(message = "vehicleId is required")
        private String vehicleId;

        @NotNull(message = "capacity is required")
        @Positive(message = "capacity must be greater than 0")
        private Double capacity;

        @NotNull(message = "currentLatitude is required")
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        private Double currentLatitude;

        @NotNull(message = "currentLongitude is required")
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        private Double currentLongitude;

        @NotBlank(message = "currentAddress is required")
        private String currentAddress;
    }

    public static class BatchVehiclesDeserializer extends JsonDeserializer<BatchVehicles> {
        @Override
        public BatchVehicles deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<VehicleItem> list = new ArrayList<>();

            JsonNode arrayNode = node.isArray() ? node : node.get("vehicles");
            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode item : arrayNode) {
                    list.add(p.getCodec().treeToValue(item, VehicleItem.class));
                }
            }

            return BatchVehicles.builder().vehicles(list).build();
        }
    }
}