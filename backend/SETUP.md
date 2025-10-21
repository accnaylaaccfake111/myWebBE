# Backend Setup Guide

## 1. Cài đặt Prerequisites

### Java 17
1. Download Java 17 từ: https://adoptium.net/
2. Cài đặt và set JAVA_HOME environment variable
3. Kiểm tra: `java -version`

### Apache Maven 3.9+
1. Download Maven từ: https://maven.apache.org/download.cgi
2. Extract và thêm vào PATH environment variable
3. Kiểm tra: `mvn -version`

### Docker Desktop (Optional)
1. Download từ: https://www.docker.com/products/docker-desktop/
2. Cài đặt và chạy Docker Desktop

## 2. Run Backend

### Option 1: Maven (Development)
```bash
# Navigate to backend directory
cd backend

# Install dependencies & run
mvn clean install -DskipTests
mvn spring-boot:run

# Or build JAR and run
mvn clean package -DskipTests
java -jar target/cultural-arts-platform-1.0.0.jar
```

### Option 2: Docker Compose (Full Stack)
```bash
# Navigate to backend directory
cd backend

# Start all services (PostgreSQL, Redis, RabbitMQ, MinIO, Backend)
docker-compose up -d

# Check logs
docker-compose logs -f backend

# Stop all services
docker-compose down
```

### Option 3: IDE (IntelliJ IDEA/Eclipse)
1. Import project as Maven project
2. Set JDK 17
3. Enable annotation processing (for Lombok)
4. Run `CulturalArtsPlatformApplication.java`

## 3. Database Setup

### PostgreSQL (Recommended)
```sql
CREATE DATABASE nckh_db;
CREATE USER nckh_user WITH PASSWORD 'nckh_pass';
GRANT ALL PRIVILEGES ON DATABASE nckh_db TO nckh_user;
```

### H2 Database (Development)
- Automatically created when app starts
- Access H2 console: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:testdb

## 4. Configuration

### Environment Variables
Create `.env` file in backend directory:
```env
# Database
DB_USERNAME=nckh_user
DB_PASSWORD=nckh_pass
DB_HOST=localhost
DB_PORT=5432

# JWT
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

### Application Profiles
- `dev`: Development (H2 database)
- `docker`: Docker environment
- `prod`: Production

Set active profile:
```bash
# In application.yml
spring.profiles.active: dev

# Or via command line
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## 5. Access URLs

After startup, access:
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console (dev profile)
- **Health Check**: http://localhost:8080/actuator/health

With Docker Compose:
- **MinIO Console**: http://localhost:9001 (minioadmin/minioadmin)
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)

## 6. Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Integration tests
mvn test -Dtest=*IT

# API testing with curl
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123456",
    "fullName": "Test User"
  }'
```

## 7. Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   ```bash
   # Change port in application.yml
   server.port: 8081
   ```

2. **Database connection failed**
   - Check PostgreSQL is running
   - Verify credentials in application.yml
   - Use H2 for development: set profile to 'dev'

3. **Maven dependencies not downloading**
   ```bash
   # Clear Maven cache
   mvn dependency:purge-local-repository
   mvn clean install
   ```

4. **Lombok not working**
   - Enable annotation processing in IDE
   - Install Lombok plugin for IntelliJ/Eclipse

5. **JWT errors**
   - Ensure JWT_SECRET is set and at least 256 bits
   - Check system time is synchronized

## 8. Development Tips

### Hot Reload
Add to application.yml:
```yaml
spring:
  devtools:
    restart:
      enabled: true
```

### Debug Mode
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Database Migration
- Use Flyway or Liquibase for production
- Current setup uses JPA auto-DDL (development only)

### Monitoring
- Metrics: http://localhost:8080/actuator/metrics
- Health: http://localhost:8080/actuator/health
- Info: http://localhost:8080/actuator/info