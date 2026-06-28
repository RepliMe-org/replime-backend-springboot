# RepliMe Backend

Backend service for **RepliMe** — a platform that generates personalized AI chatbots for content creators based on their content.

This service is built using Spring Boot and handles:
- Authentication & Authorization (JWT)
- Influencer verification logic
- Chatbot management
- Integration with AI microservice (FastAPI)
- Database management using PostgreSQL

---

## 🛠 Tech Stack

- Java 17+
- Spring Boot
- Spring Data JPA
- Spring Security (JWT)
- PostgreSQL
- Maven

---

## 📁 Project Structure

com.replime
│
├── configs
├── controllers
├── services
├── repos
├── entities
├── dtos
├── exception
├── security

---

## ⚙️ Environment Variables

The application uses environment variables for sensitive configuration.

Create a `.env` file locally (NOT committed):

### ⚠ Important:
Do NOT commit `.env` to GitHub.

---

## 🗄 Database Setup

### Using Docker (Recommended)
You can run the required databases (PostgreSQL and RabbitMQ) easily using Docker.

1. Ensure Docker and Docker Compose are installed.
2. Run the following command in the project root:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL on port `5433`
- RabbitMQ on port `5672` (Management UI available at `http://localhost:15672`)

### Manual Setup
1. Install PostgreSQL
2. Create database:

```sql
CREATE DATABASE replimedb;
```

---

▶️ Running the Application

Set environment variables (Windows example):

```
setx DB_PASSWORD yourpassword
```

Restart IDE after setting variables.

---

Run the application:

```
mvn spring-boot:run
```

Or run directly from IntelliJ.

Server runs on:

http://localhost:8080/api/v1

---

## Running Tests

Run all unit test files:

```bash
mvn "-Dtest=*Test,!DemoApplicationTests" test
```

Run one specific test class:

```bash
mvn "-Dtest=FastApiServiceTest" test
```

Generate the JaCoCo coverage report:

```bash
mvn "-Dtest=*Test,!DemoApplicationTests" test jacoco:report
```

Open the generated coverage report:

```text
target/site/jacoco/index.html
```

`DemoApplicationTests` is excluded from the unit-test command because it starts the full Spring application context and needs external services such as RabbitMQ.

---

## 📘 API Documentation (Swagger)

Interactive API documentation is available via OpenAPI.

After running the application:

http://localhost:8080/api/v1/swagger-ui/index.html

### OpenAPI JSON


http://localhost:8080/api/v1/v3/api-docs


### Swagger allows:

- Testing endpoints directly
- Viewing request/response schemas
- Exploring secured endpoints
