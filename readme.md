# Kanban API

A Spring Boot backend for a Kanban board application with REST API, WebSocket notifications, and all modern service features.

## Features

- RESTful CRUD API for task management
- Real-time notifications via WebSocket/STOMP
- Authentication using JWT
- Pagination, filtering, and validation
- OpenAPI 3 documentation with Swagger UI
- Comprehensive test coverage (>80%)
- Dockerized environment

## Tech Stack

- Java 17
- Spring Boot 3.1.3
- Spring Data JPA
- Spring Security (JWT)
- Spring WebSocket + STOMP
- PostgreSQL
- Flyway Migrations
- JUnit 5 + Mockito + Testcontainers
- Docker & Docker Compose

## Getting Started

### Prerequisites

- Java 17+
- Maven or Docker

### Running with Docker Compose

The easiest way to run the application is using Docker Compose:

```bash
# Build and start the application with PostgreSQL
docker-compose up -d