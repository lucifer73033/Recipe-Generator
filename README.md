# Smart Recipe Generator

A production-ready web application that suggests recipes from ingredients (text or image), applies dietary filters, returns step-by-step instructions with nutrition info, and lets users rate/save favorites.

## 🏗️ Architecture Overview

```mermaid
graph TB
    subgraph "Client Layer"
        UI[React SPA + Tailwind CSS]
        UI --> |HTTP/SSE| GATEWAY
    end
    
    subgraph "Application Layer"
        GATEWAY[Spring Boot Server :8080]
        GATEWAY --> |Static Files| UI
        GATEWAY --> |/api/*| API[REST Controllers]
        GATEWAY --> |OAuth2| AUTH[Google Authentication]
        API --> SERVICES[Business Services]
    end
    
    subgraph "Business Logic"
        SERVICES --> RECIPE[Recipe Service]
        SERVICES --> LLM[OpenRouter/Gemini Service]
        SERVICES --> SUB[Substitution Service]
        RECIPE --> |DB-first Algorithm| SCORING[Match Scoring]
        RECIPE --> |Quality Gate| LLM
    end
    
    subgraph "Data Layer"
        SERVICES --> MONGO[(MongoDB Atlas)]
        MONGO --> |Collections| COLLECTIONS[Users, Recipes, Ratings, Favorites, Logs]
    end
    
    subgraph "External Services"
        LLM --> |API| OPENROUTER[OpenRouter API]
        OPENROUTER --> |Model| GEMINI[Gemini 2.5 Flash]
        AUTH --> |OAuth2| GOOGLE[Google OAuth]
    end
```

## 🚀 Single JAR SPA Serving

This application implements a **single-deployment architecture** where the Spring Boot backend serves the React frontend as static files:

- **Development**: React dev server runs separately via Vite (`npm run dev`)
- **Production**: React builds to `src/main/resources/static/` and Spring Boot serves it
- **SPA Routing**: All non-API routes (`!= /api/*`) fall back to `index.html` for client-side routing
- **Asset Handling**: Static assets (JS, CSS, images) served directly by Spring Boot

### Build Process

```bash
mvn clean package
# 1. Frontend Maven Plugin installs Node.js & npm
# 2. Runs `npm install` in src/main/resources/frontend/
# 3. Runs `npm run build` → outputs to src/main/resources/static/
# 4. Spring Boot packages everything into a single JAR
```

## 🎯 Key Features

### DB-First Recipe Algorithm
- **Step 1**: Score existing recipes using weighted Jaccard similarity + bonuses
- **Quality Gate**: Proceed to LLM only if < 3 recipes with score ≥ 0.60 OR avg top-5 score < 0.55
- **Step 2**: Generate additional recipes via Gemini LLM if quality gate fails
- **Deduplication**: Merge and dedupe results by title + ingredients similarity

### Smart Ingredient Recognition
- Upload images → Gemini 2.5 Flash extracts ingredients with confidence scores
- Filter low-confidence results automatically
- Manual ingredient editing capabilities

### Authentication & Authorization
- Google OAuth 2.0 integration
- **USER**: Generate/browse recipes, rate/save favorites
- **ADMIN**: Seed database, view system logs, access admin endpoints

### Real-time Monitoring
- Server-Sent Events (SSE) for live log streaming
- Structured logging with metadata (no sensitive data exposed)
- User activity tracking and system health monitoring

## 🛠️ Technology Stack

### Backend
- **Java 17** + **Spring Boot 3.5.5**
- **Spring Security** + OAuth2 Client (Google)
- **Spring Data MongoDB** + **MongoDB Atlas**
- **Lombok** for boilerplate reduction
- **SpringDoc OpenAPI** for API documentation
- **WebFlux** for HTTP client (OpenRouter integration)

### Frontend
- **React 18** + **TypeScript** + **Vite**
- **Tailwind CSS** (utility-first styling)
- **Radix UI Tabs** (accessible tab navigation)
- **TanStack Query** (server state management)
- **React Router** (client-side routing)

### External Services
- **OpenRouter API** → **Gemini 2.5 Flash**
- **MongoDB Atlas** (free tier)
- **Google OAuth 2.0**

## 🚀 Local Development

### Prerequisites
- **Java 17+**
- **Maven 3.6+**
- **MongoDB** (local or Atlas connection)
- **Node.js 18+** (for frontend development)

### Environment Variables
Create `application-local.yml` or set environment variables:

```yaml
openrouter:
  api-key: ${OPENROUTER_API_KEY}
  
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${OAUTH_GOOGLE_CLIENT_ID}
            client-secret: ${OAUTH_GOOGLE_CLIENT_SECRET}
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/recipe_generator}

seed:
  on-start: true  # Auto-seed database on startup
```

### Quick Start

1. **Clone & Build**
   ```bash
   git clone <repository-url>
   cd Recipe_Generator
   ./mvnw clean package
   ```

2. **Run Application**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access Application**
   - Main App: http://localhost:8080
   - API Docs: http://localhost:8080/swagger-ui
   - API JSON: http://localhost:8080/api-docs

### Frontend Development
For hot reloading during development:

```bash
cd src/main/resources/frontend
npm install
npm run dev  # Runs on http://localhost:5173 with API proxy
```

### Docker Development
```bash
# MongoDB local setup
docker run -d -p 27017:27017 --name mongodb mongo:7
```

## 🗄️ Data Models

### Recipe
```typescript
{
  id: string
  title: string
  ingredients: [{ name, quantity, unit }]
  steps: string[]
  timeMinutes: number
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  cuisine?: string
  dietTags: string[]  // vegetarian, vegan, gluten-free, etc.
  nutrition?: { kcal, protein, carbs, fat }
  source: 'DB' | 'LLM' | 'FALLBACK'
  createdBy?: string  // userId for user-generated recipes
}
```

### User
```typescript
{
  id: string
  googleId: string
  email: string
  name: string
  picture?: string
  roles: ['USER' | 'ADMIN']
  createdAt: DateTime
}
```

## 🔐 API Authentication

### Public Endpoints
- `GET /api/recipes*` - Browse/search recipes
- `POST /api/recipes/generate` - Generate recipes
- `POST /api/ingredients/recognize` - Image recognition
- `GET /api/logs/stream` - Live logs (SSE)

### Protected Endpoints (USER)
- `POST /api/recipes/*/rate` - Rate recipes
- `POST /api/recipes/*/save` - Save favorites
- `GET /api/me/*` - User profile/data

### Admin Endpoints (ADMIN)
- `POST /admin/seed` - Seed database
- `GET /admin/logs` - Historical logs
- `GET /admin/stats` - System statistics

## 📊 Recipe Scoring Algorithm

```java
double score = 0.7 * jaccardSimilarity + 0.15 * timeBonus + 0.15 * difficultyBonus - dietPenalty

// Where:
// - jaccardSimilarity = |intersection| / |union| of available vs recipe ingredients
// - timeBonus = 0-0.15 based on cooking time vs user preference
// - difficultyBonus = 0-0.15 based on difficulty match
// - dietPenalty = 0-0.3 based on unmet dietary restrictions
```

**Quality Gate Thresholds:**
- `SCORE_MIN=0.60` - Minimum score for individual recipes
- `SCORE_AVG_MIN=0.55` - Minimum average score for top 5 recipes

## 🧪 Testing

Run all tests:
```bash
./mvnw test
```

### Test Coverage
- **Recipe Service**: Match scoring, quality gates, LLM fallback
- **Substitution Service**: Diet-based ingredient substitutions
- **Controllers**: REST API endpoints with security
- **Integration**: Full request/response cycles

## 🚀 Deployment

### Build Production JAR
```bash
./mvnw clean package -Pprod
# Creates target/Recipe_Generator-0.0.1-SNAPSHOT.jar
```

### Environment Setup
Set these environment variables in production:

```bash
export OPENROUTER_API_KEY="your-key"
export OAUTH_GOOGLE_CLIENT_ID="your-client-id"
export OAUTH_GOOGLE_CLIENT_SECRET="your-client-secret"
export MONGODB_URI="mongodb+srv://user:pass@cluster.mongodb.net/recipe_generator"
export SPRING_PROFILES_ACTIVE="prod"
```

### Deployment Platforms

#### Railway
```bash
# railway.json
{
  "deploy": {
    "startCommand": "java -jar target/Recipe_Generator-0.0.1-SNAPSHOT.jar",
    "healthcheckPath": "/actuator/health"
  }
}
```

#### Render
```yaml
# render.yaml
services:
  - type: web
    name: recipe-generator
    env: java
    buildCommand: "./mvnw clean package"
    startCommand: "java -jar target/Recipe_Generator-0.0.1-SNAPSHOT.jar"
    envVars:
      - key: PORT
        value: 8080
```

#### Fly.io
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/Recipe_Generator-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### Database Setup
1. Create MongoDB Atlas cluster (free tier)
2. Create database user and get connection string
3. Set `MONGODB_URI` environment variable
4. Use admin endpoint to seed initial data: `POST /admin/seed`

## 🎨 Design System

The UI follows a **minimal, utility-first approach** inspired by https://hackrx-bajaj-simulator.vercel.app/:

### Design Principles
- **Neutral color palette**: Slate grays for backgrounds, clean whites for cards
- **Utility-first CSS**: Tailwind classes only, no custom CSS
- **Subtle depth**: Minimal shadows and borders
- **Compact typography**: Small text, tight line heights
- **Focus on function**: Forms and data display over decoration

### Component Patterns
- **Cards**: `bg-white border border-neutral-200 shadow-sm rounded-lg`
- **Buttons**: Solid neutrals, no gradients
- **Inputs**: Simple borders with focus rings
- **Chips**: Neutral backgrounds for tags/filters
- **Loading states**: Skeleton patterns

### Accessibility Features
- Skip-to-content link
- Keyboard navigation support
- Focus indicators
- ARIA labels and roles
- Screen reader friendly

## 🔧 Configuration

### Customizable Thresholds
```yaml
recipe:
  score:
    min: 0.60        # Minimum individual recipe score
    avg-min: 0.55    # Minimum average score for top 5
    
openrouter:
  model: "google/gemini-2.5-flash"  # Configurable LLM model
```

### Logging Configuration
- **INFO**: User activities, recipe generation
- **WARN**: Quality gate failures, fallback usage
- **ERROR**: LLM failures, database errors
- **DEBUG**: Detailed scoring information

## 📖 API Documentation

Interactive API documentation available at:
- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI JSON**: http://localhost:8080/api-docs

### Key Endpoints

#### Recipe Generation
```http
POST /api/recipes/generate
Content-Type: application/json

{
  "ingredients": ["chicken", "tomato", "onion"],
  "dietTags": ["gluten-free"],
  "maxTimeMinutes": 45,
  "difficulty": "EASY",
  "cuisine": "Italian",
  "servings": 4
}
```

#### Image Recognition
```http
POST /api/ingredients/recognize
Content-Type: multipart/form-data

image: <file.jpg>
```

#### Live Logs Stream
```http
GET /api/logs/stream
Accept: text/event-stream
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit a pull request

### Code Style
- Java: Follow Spring Boot conventions
- TypeScript: Use ESLint + Prettier configuration
- Commits: Conventional commit format

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **OpenRouter** for LLM API access
- **Google** for OAuth and Gemini model
- **MongoDB Atlas** for database hosting
- **Tailwind CSS** for utility-first styling
- **Radix UI** for accessible components



