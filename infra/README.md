# Recipe Generator - Docker Setup

This directory contains the Docker configuration for the Recipe Generator application.

## Quick Start

### Prerequisites
- Docker and Docker Compose installed
- At least 4GB of available RAM
- OpenRouter API key (required for recipe generation)

### Development Environment

1. **Clone and navigate to the project:**
   ```bash
   cd infra
   ```

2. **Start the application:**
   ```bash
   docker-compose up -d
   ```

3. **Access the application:**
   - Frontend: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui
   - MongoDB: localhost:27017

4. **View logs:**
   ```bash
   docker-compose logs -f recipe-generator-app
   ```

5. **Stop the application:**
   ```bash
   docker-compose down
   ```

### Production Environment

1. **Set up environment variables:**
   ```bash
   cp env.example .env
   # Edit .env with your production values
   # IMPORTANT: Set your actual API keys and passwords!
   ```

2. **Start production stack:**
   ```bash
   docker-compose -f docker-compose.prod.yml up -d
   ```

3. **Access through Nginx:**
   - Application: http://localhost
   - HTTPS: https://localhost (if SSL configured)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENROUTER_API_KEY` | OpenRouter API key for LLM | Required - set in .env |
| `OPENROUTER_BASE_URL` | OpenRouter API base URL | https://openrouter.ai/api/v1 |
| `OPENROUTER_MODEL` | LLM model to use | google/gemini-2.5-flash |
| `LOG_LEVEL` | Application logging level | INFO |
| `SEED_ON_START` | Seed database on startup | true |
| `SPRING_PROFILES_ACTIVE` | Spring profile | dev |

### MongoDB Configuration

The application uses MongoDB with configurable settings:
- **Username:** Set via MONGO_USERNAME (default: admin)
- **Password:** Set via MONGO_PASSWORD (default: password)
- **Database:** Set via MONGO_DATABASE (default: recipe_generator)
- **Port:** 27017

### Ports

| Service | Port | Description |
|---------|------|-------------|
| Spring Boot App | 8080 | Main application |
| MongoDB | 27017 | Database |
| Nginx (prod) | 80, 443 | Reverse proxy |
| Redis (prod) | 6379 | Session storage |

## Docker Commands

### Build and Run
```bash
# Build the application
docker-compose build

# Run in background
docker-compose up -d

# Run with logs
docker-compose up

# Stop services
docker-compose down

# Rebuild and restart
docker-compose up --build -d
```

### Development Commands
```bash
# View logs
docker-compose logs -f recipe-generator-app

# Execute commands in container
docker-compose exec recipe-generator-app sh

# Access MongoDB shell
docker-compose exec mongodb mongosh

# Restart specific service
docker-compose restart recipe-generator-app
```

### Production Commands
```bash
# Start production stack
docker-compose -f docker-compose.prod.yml up -d

# View production logs
docker-compose -f docker-compose.prod.yml logs -f

# Scale application
docker-compose -f docker-compose.prod.yml up -d --scale recipe-generator-app=3
```

## Architecture

### Development Stack
- **Spring Boot Application:** Main backend service
- **MongoDB:** Database for recipes and user data
- **React Frontend:** Built and served by Spring Boot

### Production Stack
- **Nginx:** Reverse proxy with rate limiting and security headers
- **Spring Boot Application:** Backend service with resource limits
- **MongoDB:** Database with authentication
- **Redis:** Session storage (optional)

## Security Features

### Nginx Configuration
- Rate limiting for API endpoints
- Security headers (XSS protection, CSRF, etc.)
- Gzip compression
- Request forwarding with proper headers

### Application Security
- Non-root user in containers
- Health checks for all services
- Resource limits and reservations
- Environment variable configuration

## Troubleshooting

### Common Issues

1. **Port already in use:**
   ```bash
   # Check what's using the port
   netstat -tulpn | grep :8080
   
   # Stop conflicting services
   docker-compose down
   ```

2. **MongoDB connection issues:**
   ```bash
   # Check MongoDB logs
   docker-compose logs mongodb
   
   # Restart MongoDB
   docker-compose restart mongodb
   ```

3. **Build failures:**
   ```bash
   # Clean build
   docker-compose build --no-cache
   
   # Check Docker daemon
   docker system prune -a
   ```

4. **Memory issues:**
   ```bash
   # Check container resource usage
   docker stats
   
   # Increase Docker memory limit in Docker Desktop
   ```

### Logs and Debugging

```bash
# View all logs
docker-compose logs

# View specific service logs
docker-compose logs recipe-generator-app

# Follow logs in real-time
docker-compose logs -f

# View last 100 lines
docker-compose logs --tail=100 recipe-generator-app
```

## Performance Optimization

### Resource Limits
The production configuration includes resource limits:
- **Application:** 2GB RAM, 1 CPU
- **MongoDB:** 1GB RAM, 0.5 CPU
- **Nginx:** 256MB RAM, 0.25 CPU
- **Redis:** 256MB RAM, 0.25 CPU

### JVM Optimization
The application uses optimized JVM settings:
- G1GC garbage collector
- Container-aware memory settings
- Optimized heap size

## Backup and Restore

### MongoDB Backup
```bash
# Create backup
docker-compose exec mongodb mongodump --out /data/backup

# Copy backup from container
docker cp recipe-generator-mongodb:/data/backup ./backup

# Restore backup
docker-compose exec mongodb mongorestore /data/backup
```

### Volume Backup
```bash
# Backup volumes
docker run --rm -v recipe-generator_mongodb_data:/data -v $(pwd):/backup alpine tar czf /backup/mongodb-backup.tar.gz -C /data .

# Restore volumes
docker run --rm -v recipe-generator_mongodb_data:/data -v $(pwd):/backup alpine tar xzf /backup/mongodb-backup.tar.gz -C /data
```

## Monitoring

### Health Checks
All services include health checks:
- Application: HTTP health endpoint
- MongoDB: Database ping
- Redis: Redis ping

### Metrics
The application exposes metrics at `/actuator/metrics` (if Spring Boot Actuator is enabled).

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review application logs
3. Check Docker and system resources
4. Verify environment variable configuration
