package com.assignment.dispatch.dto;

import com.assignment.dispatch.entity.Priority;
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

public class OrderRequestDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonDeserialize(using = BatchOrdersDeserializer.class)
    public static class BatchOrders {
        @NotEmpty(message = "Orders list cannot be empty")
        @Valid
        private List<OrderItem> orders;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItem {
        @NotBlank(message = "orderId is required")
        private String orderId;

        @NotNull(message = "latitude is required")
        @DecimalMin(value = "-90.0", message = "latitude must be >= -90.0")
        @DecimalMax(value = "90.0", message = "latitude must be <= 90.0")
        private Double latitude;

        @NotNull(message = "longitude is required")
        @DecimalMin(value = "-180.0", message = "longitude must be >= -180.0")
        @DecimalMax(value = "180.0", message = "longitude must be <= 180.0")
        private Double longitude;

        @NotBlank(message = "address is required")
        private String address;

        @NotNull(message = "packageWeight is required")
        @Positive(message = "packageWeight must be greater than 0")
        private Double packageWeight;

        @NotNull(message = "priority is required (HIGH, MEDIUM, LOW)")
        private Priority priority;
    }

    public static class BatchOrdersDeserializer extends JsonDeserializer<BatchOrders> {
        @Override
        public BatchOrders deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            List<OrderItem> list = new ArrayList<>();

            JsonNode arrayNode = node.isArray() ? node : node.get("orders");
            if (arrayNode != null && arrayNode.isArray()) {
                for (JsonNode item : arrayNode) {
                    list.add(p.getCodec().treeToValue(item, OrderItem.class));
                }
            }

            return BatchOrders.builder().orders(list).build();
        }
    }
}