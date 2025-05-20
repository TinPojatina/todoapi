### Features

- RESTful CRUD API for task management
- GraphQL API for flexible data querying
- Real-time notifications via WebSocket/STOMP
- Authentication using JWT
- Data validation with detailed error responses
- Caching with Caffeine
- Rate limiting for API protection
- Pagination, filtering, and sorting for task retrieval
- Optimistic locking for concurrent modifications
- OpenAPI 3 documentation with Swagger UI
- Database migrations with Flyway
- Comprehensive test coverage (>80%)
- Dockerized environment

### Tech Stack

- Java 21
- Spring Boot 3.4.5
- Spring Data JPA
- Spring Security (JWT)
- Spring WebSocket + STOMP
- Spring GraphQL
- PostgreSQL
- Flyway Migrations
- Caffeine Cache
- JUnit 5 + Mockito + Testcontainers
- Docker & Docker Compose

### Prerequisites

- Java 21 or higher
- Maven (or use the Maven wrapper)
- Docker and Docker Compose (for containerized deployment)

### Getting Started
Running with Docker Compose
The easiest way to run the application is using Docker Compose:

```Build and start the application with PostgreSQL
docker-compose up -d
```

This will:

- Create a PostgreSQL database
- Start the Spring Boot application on port 8080
- Initialize the database with Flyway migrations
http://localhost:8080/swagger-ui/index.html

### REST API Endpoints
The API provides the following main endpoints:
Authentication
#### Authorization
- POST /api/auth/register - Register a new user
- POST /api/auth/login - Login and get JWT token

#### Task Management

- GET /api/tasks - Get all tasks (with pagination, sorting, filtering)
- GET /api/tasks/{id} - Get a specific task
- POST /api/tasks - Create a new task
- PUT /api/tasks/{id} - Update a task
- PATCH /api/tasks/{id} - Partially update a task
- DELETE /api/tasks/{id} - Delete a task
- GET /api/tasks/search - Search tasks with multiple criteria
- GET /api/tasks/assigned/{userId} - Get tasks assigned to a user
- GET /api/tasks/created/{userId} - Get tasks created by a user


### GraphQL API
The application also provides a GraphQL API at:
http://localhost:8080/graphql
GraphiQL interface is available at:
http://localhost:8080/graphiql

- Example GraphQL operations:
```
# Query all tasks
query {
  tasks(page: 0, size: 10) {
    content {
      id
      title
      description
      status
      priority
      createdBy {
        id
        username
      }
      assignedTo {
        id
        username
      }
    }
    totalElements
    totalPages
  }
}

# Get a specific task
query {
  task(id: 1) {
    id
    title
    description
    status
  }
}

# Create a new task
mutation {
  createTask(input: {
    title: "New task"
    description: "Task description"
    status: TO_DO
    priority: MED
  }) {
    id
    title
    status
  }
}
```