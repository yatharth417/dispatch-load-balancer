Dispatch Load Balancer

A production-ready Spring Boot microservice that solves a constrained Capacitated Vehicle Routing Problem (CVRP) variant. The service optimizes the assignment of prioritized delivery orders to a fleet of vehicles based on geographical coordinates, minimizes total travel distance using the Haversine formula, and enforces strict vehicle capacity thresholds.

📌 Architecture & Design Highlights

Mathematical Routing Engine: Calculates Great-Circle distances using the spherical Haversine formula with mean Earth radius $R = 6371.0\text{ km}$.

Constrained Priority Allocation: Orders are prioritized in strict hierarchical order (HIGH $\rightarrow$ MEDIUM $\rightarrow$ LOW), with tie-breakers sorted by package weight descending.

Greedy Spatial Clustering: Employs a nearest-neighbor heuristic that matches each order to the vehicle whose current route location minimizes incremental transit distance without exceeding weight limits.

Idempotent Upsert Persistence: Database operations update existing entities by orderId or vehicleId to prevent duplicates and constraint violations.

Defensive Error Handling: Standardized HTTP 400/404/500 JSON error responses via @RestControllerAdvice covering malformed JSON, out-of-range coordinates, invalid priority enums, and overcapacity.

Flexible Ingestion: Custom Jackson deserializers accept both object-wrapped lists ({"orders": [...]}) and raw array payloads ([...]).

🚀 Getting Started

Prerequisites

Java Development Kit (JDK): Version 17 or higher

Apache Maven: Version 3.8+ (or use the included ./mvnw wrapper)

Run Unit & Integration Tests

Bash

mvn clean test

Or using the wrapper:

./mvnw clean test

Start the Application

mvn spring-boot:run

Or using the wrapper:

./mvnw spring-boot:run

Server Port: 8080

Base URL: http://localhost:8080

H2 Database Console: http://localhost:8080/h2-console

JDBC URL: jdbc:h2:mem:dispatchdb

Username: sa

Password: (empty)

📡 API Reference & Contracts

1. Ingest Delivery Orders

Accepts a batch of delivery orders and persists or updates them in the database.

Endpoint: POST /api/dispatch/orders

Content-Type: application/json

Request Body:

JSON

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

Success Response (HTTP 200 OK):

JSON

{
  "message": "Delivery orders accepted.",
  "status": "success"
}

2. Ingest Fleet Vehicles

Registers or updates available fleet vehicles and their starting locations.

Endpoint: POST /api/dispatch/vehicles

Content-Type: application/json

Request Body:

JSON

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

Success Response (HTTP 200 OK):

JSON

{
  "message": "Vehicle details accepted.",
  "status": "success"
}

3. Generate Dispatch Plan

Executes the optimization algorithm and outputs vehicle route assignments, total load, cumulative distance, and unassigned orders if capacity is exhausted.

Endpoint: GET /api/dispatch/plan

Success Response (HTTP 200 OK):

JSON

{
  "dispatchPlan": [
    {
      "vehicleId": "VEH001",
      "totalLoad": 10.0,
      "totalDistance": "5 km",
      "assignedOrders": [
        {
          "orderId": "ORD001",
          "latitude": 12.9716,
          "longitude": 77.5946,
          "address": "MG Road, Bangalore, Karnataka, India",
          "packageWeight": 10.0,
          "priority": "HIGH"
        }
      ]
    },
    {
      "vehicleId": "VEH002",
      "totalLoad": 20.0,
      "totalDistance": "6 km",
      "assignedOrders": [
        {
          "orderId": "ORD002",
          "latitude": 13.0827,
          "longitude": 80.2707,
          "address": "Anna Salai, Chennai, Tamil Nadu, India",
          "packageWeight": 20.0,
          "priority": "MEDIUM"
        }
      ]
    }
  ],
  "unassignedOrders": []
}

🧮 Mathematical Formulation

Haversine Distance Formula

Given two points with latitude and longitude coordinates $(\phi_1, \lambda_1)$ and $(\phi_2, \lambda_2)$:
$$\Delta \phi = \phi_2 - \phi_1, \quad \Delta \lambda = \lambda_2 - \lambda_1$$
$$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)$$
$$c = 2 \cdot \operatorname{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$
$$d = R \cdot c$$
where $R = 6371.0\text{ km}$.

🛡️ Edge Cases & Error Handling

Overcapacity / Unassignable Orders: If an order cannot fit within the residual capacity of any vehicle in the fleet, it is routed to the unassignedOrders collection in the response rather than failing the execution.

Zero Fleet Registered: Invoking /api/dispatch/plan with no registered vehicles returns HTTP 400 Bad Request with message: "No vehicles registered in the fleet.".

Coordinate Bounds Validation: Latitudes outside $[-90, 90]$ and longitudes outside $[-180, 180]$ trigger validation errors.

Payload Robustness: Ingest endpoints handle mixed-case priorities (high, HIGH) and accept both bare arrays and root object wrappers.

📂 Project Structure

Plaintext

dispatch-load-balancer/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/assignment/dispatch/
│   │   │   ├── DispatchApplication.java
│   │   │   ├── controller/
│   │   │   │   └── DispatchController.java
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponseDto.java
│   │   │   │   ├── DispatchPlanResponseDto.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── OrderRequestDto.java
│   │   │   │   └── VehicleRequestDto.java
│   │   │   ├── entity/
│   │   │   │   ├── OrderEntity.java
│   │   │   │   ├── Priority.java
│   │   │   │   └── VehicleEntity.java
│   │   │   ├── exception/
│   │   │   │   ├── DispatchException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── InvalidInputException.java
│   │   │   ├── repository/
│   │   │   │   ├── OrderRepository.java
│   │   │   │   └── VehicleRepository.java
│   │   │   ├── service/
│   │   │   │   ├── DispatchService.java
│   │   │   │   └── impl/DispatchServiceImpl.java
│   │   │   └── util/
│   │   │       └── HaversineDistanceCalculator.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/assignment/dispatch/
│           ├── controller/DispatchControllerTest.java
│           ├── service/DispatchServiceTest.java
│           └── util/HaversineDistanceCalculatorTest.java