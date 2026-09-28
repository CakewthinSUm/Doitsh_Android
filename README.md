# Doitsh

A task and project management app for Android, with a Kotlin/Ktor backend.

## Architecture

```
Doitsh/
├── app/                    # Android client (Jetpack Compose)
│   └── src/main/java/com/doitsh/app/
│       ├── data/           # Room entities, DAOs, API client, repositories
│       ├── domain/         # Domain models (Task, Project) and repository interfaces
│       ├── di/             # Hilt dependency injection modules
│       └── ui/             # Compose screens, theme, and components
└── server/                 # Ktor REST API server
    └── src/main/kotlin/com/doitsh/server/
        ├── Application.kt  # Server entry point
        ├── Routing.kt      # API endpoints
        ├── Tables.kt       # Exposed ORM table definitions
        ├── Security.kt     # JWT authentication
        └── Database.kt     # DB connection and pooling
```

## Tech Stack

### Android Client
- **UI**: Jetpack Compose with Material 3
- **Local DB**: Room with type converters
- **DI**: Hilt
- **Networking**: Ktor client with kotlinx.serialization
- **Async**: Kotlin Coroutines + Flow

### Server
- **Framework**: Ktor (Netty engine)
- **ORM**: Exposed (JetBrains)
- **Database**: SQLite (default) / PostgreSQL (optional)
- **Auth**: JWT + BCrypt
- **Connection pool**: HikariCP

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Gradle 9

### Android Client

Open the project root in Android Studio, sync Gradle, and run the `app` module on an emulator or device (min API 26).

### Server

The server module is temporarily disabled due to Ktor plugin incompatibility with Gradle 9. To run it manually:

```bash
cd server
./gradlew run
```

The server starts on `http://0.0.0.0:8080`.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/health` | No | Health check |
| `POST` | `/api/auth/register` | No | Register a new user |
| `POST` | `/api/auth/login` | No | Login, returns JWT |
| `GET` | `/api/tasks` | JWT | List user's tasks |
| `POST` | `/api/tasks` | JWT | Create a task |
| `DELETE` | `/api/tasks/{id}` | JWT | Delete a task |
| `GET` | `/api/projects` | JWT | List user's projects |
| `POST` | `/api/projects` | JWT | Create a project |

## Data Models

### Task
- UUID-based ID
- Title, description, completion status
- Priority (None / Low / Medium / High)
- Optional due date and project association
- Created/updated timestamps

### Project
- UUID-based ID
- Name, description, color
- Created/updated timestamps

## Features
- Offline-first: Room-backed local storage with repository abstraction
- Remote sync ready: Ktor API client with configurable base URL and auth token
- Task filtering: All / Today / This Week
- Material 3 design with floating toolbar navigation