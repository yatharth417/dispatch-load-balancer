# Dispatch Load Balancer

A production-ready Spring Boot microservice that solves a constrained **Capacitated Vehicle Routing Problem (CVRP)** variant. The service optimizes the assignment of prioritized delivery orders to a fleet of vehicles based on geographical coordinates, minimizes total travel distance using the **Haversine formula**, and enforces strict vehicle capacity thresholds.

> **Current implementation:** Priority-based dispatch optimization with Haversine distance calculation, greedy spatial clustering, vehicle capacity enforcement, idempotent persistence, and defensive API error handling.

---

## 📌 Architecture & Design Highlights

- **Mathematical Routing Engine:** Calculates Great-Circle distances using the spherical Haversine formula with mean Earth radius `R = 6371.0 km`.

- **Constrained Priority Allocation:** Orders are prioritized in strict hierarchical order (`HIGH` → `MEDIUM` → `LOW`), with tie-breakers sorted by package weight descending.

- **Greedy Spatial Clustering:** Employs a nearest-neighbor heuristic that matches each order to the vehicle whose current route location minimizes incremental transit distance without exceeding weight limits.

- **Idempotent Upsert Persistence:** Database operations update existing entities by `orderId` or `vehicleId` to prevent duplicates and constraint violations.

- **Defensive Error Handling:** Standardized HTTP `400/404/500` JSON error responses via `@RestControllerAdvice` covering malformed JSON, out-of-range coordinates, invalid priority enums, and overcapacity.

- **Flexible Ingestion:** Custom Jackson deserializers accept both object-wrapped lists (`{"orders": [...]}`) and raw array payloads (`[...]`).

> *The service is designed to remain deterministic, capacity-aware, and resilient to invalid or repeated input.*

---

## 🚀 Getting Started

### Prerequisites

| Requirement | Version |
|---|---|
| **Java Development Kit (JDK)** | 17 or higher |
| **Apache Maven** | 3.8+ |
| **Database** | H2 |
| **Server Port** | 8080 |

### Run Unit & Integration Tests

```bash
mvn clean test
```

*Or using the Maven wrapper:*

```bash
./mvnw clean test
```

### Start the Application

```bash
mvn spring-boot:run
```

*Or using the Maven wrapper:*

```bash
./mvnw spring-boot:run
```

### Application Details

| Property | Value |
|---|---|
| **Server Port** | `8080` |
| **Base URL** | `http://localhost:8080` |
| **H2 Database Console** | `http://localhost:8080/h2-console` |
| **JDBC URL** | `jdbc:h2:mem:dispatchdb` |
| **Username** | `sa` |
| **Password** | *(empty)* |

> **H2 Console:** Open [`http://localhost:8080/h2-console`](http://localhost:8080/h2-console) while the application is running.

---

## 📡 API Reference & Contracts

The service exposes three primary REST endpoints for ingesting delivery orders, registering fleet vehicles, and generating the optimized dispatch plan.

### 1. Ingest Delivery Orders

Accepts a batch of delivery orders and persists or updates them in the database.

**Endpoint:**

```http
POST /api/dispatch/orders
```

**Content-Type:**

```http
application/json
```

#### Request Body

```json
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
```

#### Success Response

> **HTTP 200 OK**

```json
{
  "message": "Delivery orders accepted.",
  "status": "success"
}
```

---

### 2. Ingest Fleet Vehicles

Registers or updates available fleet vehicles and their starting locations.

**Endpoint:**

```http
POST /api/dispatch/vehicles
```

**Content-Type:**

```http
application/json
```

#### Request Body

```json
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
```

#### Success Response

> **HTTP 200 OK**

```json
{
  "message": "Vehicle details accepted.",
  "status": "success"
}
```

---

### 3. Generate Dispatch Plan

Executes the optimization algorithm and outputs vehicle route assignments, total load, cumulative distance, and unassigned orders if capacity is exhausted.

**Endpoint:**

```http
GET /api/dispatch/plan
```

#### Success Response

> **HTTP 200 OK**

```json
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
```

---

## 🧮 Mathematical Formulation

### Haversine Distance Formula

Given two points with latitude and longitude coordinates `(ϕ₁, λ₁)` and `(ϕ₂, λ₂)`:

$$
\Delta \phi = \phi_2 - \phi_1,\qquad
\Delta \lambda = \lambda_2 - \lambda_1
$$

$$
a =
\sin^2\left(\frac{\Delta \phi}{2}\right)
+
\cos(\phi_1)\cos(\phi_2)
\sin^2\left(\frac{\Delta \lambda}{2}\right)
$$

$$
c =
2 \cdot \mathrm{atan2}\left(\sqrt{a},\sqrt{1-a}\right)
$$

$$
d = R \cdot c
$$

where:

$$
R = 6371.0\text{ km}
$$

The resulting value represents the **Great-Circle distance in kilometers** between the two geographic coordinates.

---

## 🧠 Dispatch Allocation Strategy

Orders are processed according to a strict priority hierarchy:

| Priority | Processing Order |
|---|---:|
| `HIGH` | 1 |
| `MEDIUM` | 2 |
| `LOW` | 3 |

Within the same priority level, orders are sorted by **package weight in descending order**.

For each order, the dispatch engine:

1. Evaluates all registered vehicles.
2. Filters out vehicles that do not have sufficient remaining capacity.
3. Calculates the distance from each eligible vehicle's current route location to the order.
4. Selects the vehicle with the minimum incremental distance.
5. Assigns the order to that vehicle.
6. Updates the vehicle's current route location and accumulated load.
7. Places the order in `unassignedOrders` when no vehicle can accommodate it.

> **Note:** The implementation uses a greedy nearest-neighbor heuristic. It is designed to provide an efficient constrained dispatch plan rather than guarantee the mathematically optimal global CVRP solution.

---

## 🛡️ Edge Cases & Error Handling

- **Overcapacity / Unassignable Orders:** If an order cannot fit within the residual capacity of any vehicle in the fleet, it is routed to the `unassignedOrders` collection in the response rather than failing the execution.

- **Zero Fleet Registered:** Invoking `/api/dispatch/plan` with no registered vehicles returns `HTTP 400 Bad Request` with message: `"No vehicles registered in the fleet."`.

- **Coordinate Bounds Validation:** Latitudes outside `[-90, 90]` and longitudes outside `[-180, 180]` trigger validation errors.

- **Payload Robustness:** Ingest endpoints handle mixed-case priorities (`high`, `HIGH`) and accept both bare arrays and root object wrappers.

- **Malformed JSON:** Invalid JSON payloads are handled through the global exception handling layer and returned as standardized API error responses.

- **Invalid Priority Values:** Unsupported priority values are rejected and converted into a standardized validation error response.

- **Duplicate Orders:** Existing orders are updated using their `orderId` rather than creating duplicate records.

- **Duplicate Vehicles:** Existing vehicles are updated using their `vehicleId` rather than creating duplicate records.

---

## 🧪 Testing

The project includes unit and integration tests covering the main routing, service, and controller behaviors.

Run the complete test suite using:

```bash
mvn clean test
```

### Test Coverage Areas

- Haversine distance calculations
- Dispatch service logic
- Priority-based order allocation
- Vehicle capacity enforcement
- Unassigned order handling
- Controller request/response behavior
- Validation and error handling
- Order and vehicle upsert behavior

---

## 📂 Project Structure

```text
dispatch-load-balancer/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/assignment/dispatch/
│   │   │   ├── DispatchApplication.java
│   │   │   │
│   │   │   ├── controller/
│   │   │   │   └── DispatchController.java
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponseDto.java
│   │   │   │   ├── DispatchPlanResponseDto.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── OrderRequestDto.java
│   │   │   │   └── VehicleRequestDto.java
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   ├── OrderEntity.java
│   │   │   │   ├── Priority.java
│   │   │   │   └── VehicleEntity.java
│   │   │   │
│   │   │   ├── exception/
│   │   │   │   ├── DispatchException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── InvalidInputException.java
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── OrderRepository.java
│   │   │   │   └── VehicleRepository.java
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── DispatchService.java
│   │   │   │   └── impl/
│   │   │   │       └── DispatchServiceImpl.java
│   │   │   │
│   │   │   └── util/
│   │   │       └── HaversineDistanceCalculator.java
│   │   │
│   │   └── resources/
│   │       └── application.yml
│   │
│   └── test/
│       └── java/com/assignment/dispatch/
│           ├── controller/
│           │   └── DispatchControllerTest.java
│           ├── service/
│           │   └── DispatchServiceTest.java
│           └── util/
│               └── HaversineDistanceCalculatorTest.java
```

<details>
<summary><strong>📁 Package Responsibilities</strong></summary>

| Package | Responsibility |
|---|---|
| `controller` | REST API endpoints and HTTP request/response handling |
| `dto` | Request and response data transfer objects |
| `entity` | JPA entities and priority model |
| `exception` | Custom exceptions and centralized error handling |
| `repository` | Database persistence through Spring Data JPA |
| `service` | Core dispatch and optimization logic |
| `util` | Haversine distance calculation utilities |

</details>

---

## 🔗 API Summary

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/dispatch/orders` | Ingest or update delivery orders |
| `POST` | `/api/dispatch/vehicles` | Register or update fleet vehicles |
| `GET` | `/api/dispatch/plan` | Generate the optimized dispatch plan |

---

## 📌 Notes

- The application runs locally on **port `8080`** by default.
- Persistence is handled using an in-memory **H2 database**.
- The dispatch algorithm prioritizes **delivery priority first**, followed by **package weight**, and then **spatial proximity**.
- Vehicle capacity is treated as a strict constraint.
- Orders that cannot be assigned are returned instead of causing the entire dispatch operation to fail.

> **Repository:** `dispatch-load-balancer`