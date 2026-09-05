package com.assignment.dispatch.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineDistanceCalculatorTest {

    @Test
    @DisplayName("Distance between the exact same coordinates must be 0")
    void samePointDistanceIsZero() {
        double dist = HaversineDistanceCalculator.calculateDistance(12.9716, 77.5946, 12.9716, 77.5946);
        assertEquals(0.0, dist, 0.001);
    }

    @Test
    @DisplayName("Distance between Bangalore MG Road and Indiranagar should be approx 5 km")
    void bangaloreDistanceTest() {
        // MG Road (12.9716, 77.5946) to Indiranagar (12.9716, 77.6413)
        double dist = HaversineDistanceCalculator.calculateDistance(12.9716, 77.5946, 12.9716, 77.6413);
        assertTrue(dist >= 4.0 && dist <= 6.0, "Expected approx 5km, got " + dist);
    }
}