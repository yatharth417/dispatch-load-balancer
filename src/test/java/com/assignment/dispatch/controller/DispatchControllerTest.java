package com.assignment.dispatch.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Full lifecycle integration test using Assignment sample data")
    void testAssignmentFullFlow() throws Exception {
        String ordersJson = """
            {
              "orders": [
                {
                  "orderId": "ORD001",
                  "latitude": 12.9716,
                  "longitude": 77.5946,
                  "address": "MG Road, Bangalore, Karnataka, India",
                  "packageWeight": 10,
                  "priority": "HIGH"
                },
                {
                  "orderId": "ORD002",
                  "latitude": 13.0827,
                  "longitude": 80.2707,
                  "address": "Anna Salai, Chennai, Tamil Nadu, India",
                  "packageWeight": 20,
                  "priority": "MEDIUM"
                }
              ]
            }
        """;

        mockMvc.perform(post("/api/dispatch/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ordersJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Delivery orders accepted."));

        String vehiclesJson = """
            {
              "vehicles": [
                {
                  "vehicleId": "VEH001",
                  "capacity": 100,
                  "currentLatitude": 12.9716,
                  "currentLongitude": 77.6413,
                  "currentAddress": "Indiranagar, Bangalore, Karnataka, India"
                },
                {
                  "vehicleId": "VEH002",
                  "capacity": 150,
                  "currentLatitude": 13.0674,
                  "currentLongitude": 80.2376,
                  "currentAddress": "T Nagar, Chennai, Tamil Nadu, India"
                }
              ]
            }
        """;

        mockMvc.perform(post("/api/dispatch/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclesJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Vehicle details accepted."));

        mockMvc.perform(get("/api/dispatch/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispatchPlan").isArray())
                .andExpect(jsonPath("$.dispatchPlan.length()").value(2));
    }
}