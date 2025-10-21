# NCKH Cultural Arts Platform - Backend

## Tech Stack
- Java 17
- Spring Boot 3.2.0
- Spring Security + JWT
- PostgreSQL / H2 Database
- Redis Cache
- MinIO/S3 Storage
- RabbitMQ Message Queue
- WebSocket Support
- Docker & Docker Compose

## Architecture
```
backend/
├── src/main/java/com/nckh/
│   ├── config/           # Configuration classes
│   ├── controller/       # REST API Controllers
│   ├── service/          # Business logic
│   ├── repository/       # Data access layer
│   ├── entity/           # JPA Entities
│   ├── dto/              # Data Transfer Objects
│   ├── security/         # Security components
│   ├── exception/        # Exception handling
│   └── utils/            # Utility classes
```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token
- `POST /api/auth/logout` - User logout

### User Management
- `GET /api/users/profile` - Get current user profile
- `PUT /api/users/profile` - Update user profile
- `POST /api/users/avatar` - Upload user avatar
- `PUT /api/users/password` - Change password

### Project Management
- `GET /api/projects` - List user projects
- `POST /api/projects` - Create new project
- `GET /api/projects/{id}` - Get project details
- `PUT /api/projects/{id}` - Update project
- `DELETE /api/projects/{id}` - Delete project

### AI Services
- `POST /api/ai/face-swap` - Face swap processing
- `POST /api/ai/dance-simulation` - Dance simulation
- `POST /api/ai/karaoke` - Karaoke processing
- `POST /api/ai/lyrics` - Lyrics composition

## Setup Instructions

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL (optional, can use H2)
- Redis (optional for caching)

### Quick Start with Docker

1. Clone the repository
```bash
cd backend
```

2. Start all services with Docker Compose
```bash
docker-compose up -d
```

3. The application will be available at:
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- MinIO Console: http://localhost:9001
- RabbitMQ Management: http://localhost:15672

### Local Development

1. Install dependencies
```bash
mvn clean install
```

2. Run the application
```bash
mvn spring-boot:run
```

Or build and run JAR:
```bash
mvn clean package
java -jar target/cultural-arts-platform-1.0.0.jar
```

### Environment Variables

Create `.env` file:
```env
DB_USERNAME=nckh_user
DB_PASSWORD=nckh_pass
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=your-secret-key
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

## Database Schema

### Users Table
- User authentication and profile data
- Roles: USER, ADMIN, MODERATOR, PREMIUM_USER

### Projects Table
- User projects for different features
- Types: FACE_SWAP, DANCE_SIMULATION, KARAOKE, LYRICS_COMPOSITION

### Media Files Table
- Storage for images, videos, audio files
- Integration with MinIO/S3

## Security Features
- JWT Authentication
- BCrypt password encryption
- Role-based access control
- Account lockout after failed attempts
- CORS configuration
- Request rate limiting

## API Documentation
Swagger/OpenAPI documentation available at:
```
http://localhost:8080/swagger-ui.html
```

## Testing
Run tests:
```bash
mvn test
```

Run with coverage:
```bash
mvn test jacoco:report
```

## Monitoring
- Health check: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Production Deployment

1. Build Docker image:
```bash
docker build -t nckh-backend .
```

2. Run with environment variables:
```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  -e JWT_SECRET=your-secret \
  nckh-backend
```

## License
Copyright © 2025 - Contract 42-2025/HDKT-TECHBYTE