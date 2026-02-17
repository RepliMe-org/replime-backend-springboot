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
├── config
├── controller
├── service
├── repository
├── entity
├── dto
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
