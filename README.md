# Doitsh

A task and project management app for Android, with a Kotlin/Ktor backend.

## Architecture

```
Doitsh/
├── gradle/
│   └── libs.versions.toml  # Version catalog — single source of truth for versions
├── app/                    # Android client (Jetpack Compose)
│   └── src/main/java/com/doitsh/app/
│       ├── data/           # Room entities, DAOs, API client, repositories
│       ├── domain/         # Domain models (Task, Project) and repository interfaces
│       ├── di/             # Hilt dependency injection modules
│       └── ui/             # Compose screens, theme, and components
└── server/                 # Ktor REST API server
    └── src/main/kotlin/com/doitsh/server/
        ├── Application.kt   # Server entry point
        ├── Routing.kt       # API endpoints
        ├── Tables.kt        # Exposed ORM table definitions
        ├── Security.kt      # JWT authentication
        ├── Database.kt      # DB connection and pooling
        ├── Cors.kt          # CORS configuration
        └── Serialization.kt # JSON content negotiation and error handling
```

## Tech Stack

All dependency versions live in one place: [`gradle/libs.versions.toml`](gradle/libs.versions.toml)
(Gradle version catalog). Both modules read versions from it rather than inlining strings.

### Build toolchain
- **Gradle** 9.6.0 (wrapper) · **AGP** 9.4.1 · **Kotlin** 2.4.21 · **KSP** 2.3.12 · **JDK** 17+
- AGP 9 has *built-in Kotlin*, so `org.jetbrains.kotlin.android` is deliberately **not**
  applied — declaring it fails the build. Kotlin options live in a top-level
  `kotlin { compilerOptions { } }` block, not `android.kotlinOptions {}`.

### Android Client
- **UI**: Jetpack Compose (BOM 2026.09.00) with **Material 3 Expressive** throughout:
  `DoitshTheme` passes `MotionScheme.expressive()` and the 8-role Expressive `Shapes`
  scale to the 5-argument `MaterialTheme`, and the feature code uses the Expressive
  component set (`FloatingActionButtonMenu`, `ShortNavigationBar`, `ToggleButton`,
  `LoadingIndicator`, `ListItem`, `SwipeToDismissBox`). material3 is pinned explicitly
  to 1.5.0-beta01 because the BOM's 1.4.0 lacks those APIs
- **Navigation**: Navigation 3 (`androidx.navigation3` 1.2.0) with a real back stack
  and `NavDisplay`
- **Local DB**: Room 2.8.5 with type converters
- **DI**: Hilt 2.60.1
- **Networking**: Ktor client 3.6.0 with kotlinx.serialization
- **Async**: Kotlin Coroutines 1.11.0 + Flow
- **Adaptive**: Material 3 Adaptive 1.3.0 — `currentWindowAdaptiveInfoV2()` drives the
  choice, using the adaptive navigation suite's own rule
  (`NavigationSuiteScaffoldDefaults.navigationSuiteType`) rather than an ad-hoc
  breakpoint. `ShortNavigationBar` holds the top-level destinations unless the window
  is at least medium width **and** medium height and not in tabletop posture, in which
  case `WideNavigationRail` takes over. A phone in landscape stays on the bar: it is
  wide but its height is compact, which is the `ShortNavigationBarMedium` case —
  icons move beside their labels, but the bar stays at the bottom
- **Also on the classpath** (added, not yet adopted by feature code): DataStore 1.2.1,
  WorkManager 2.12.0, Paging 3.5.3, Coil 3.6.3,
  ProfileInstaller 1.4.1, OkHttp 5.5.0

`compileSdk` is **37** (not 36): every library above ships AAR metadata declaring
`minCompileSdk=37`, and AGP 9 fails the build outright on a mismatch. `targetSdk` is 36.

### Server
- **Framework**: Ktor (Netty engine) 2.3.12
- **ORM**: Exposed (JetBrains) 0.54.0
- **Database**: SQLite (default) / PostgreSQL (optional)
- **Auth**: JWT + BCrypt
- **Connection pool**: HikariCP

The server's own runtime dependency versions are intentionally unchanged; it shares only
the Kotlin 2.4.21 compiler with the app.

## Getting Started

### Prerequisites
- Android Studio (recent; must support AGP 9)
- JDK 17 or later (JDK 21 tested)
- Gradle is supplied by the wrapper — do not install it separately

### Android Client

Open the project root in Android Studio, sync Gradle, and run the `app` module on an
emulator or device (min API 34 / Android 14).

### Server

The server builds against the same toolchain and is included in the Gradle build.

```bash
./gradlew :server:run
```

The server starts on `http://0.0.0.0:8080`.

> **Note:** the `server/` Kotlin sources currently do not compile — they were written
> against an older Exposed API and reference a package that does not exist in 0.54.0.
> See "Known issues" below.

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
- Task filtering: **Inbox** (tasks belonging to no project) and **Today** (open tasks
  due today or earlier, most overdue first). Both are derived from the full task list in
  `TaskFilters.kt` rather than queried per data mode, so local and self-hosted cannot
  disagree about what they mean. The Inbox keeps completed tasks — with no completed-tasks
  view, hiding them would make a mis-ticked task unreachable
- New tasks are added from a modal bottom sheet that keeps the list visible behind it and
  carries the fields the model already had — due date, priority and project
- Due dates: Today / Tomorrow / Next week presets, plus a date picker for anything else. A
  task's due date is compared by calendar day (`Task.isDueTodayOrEarlier`)
- Priority and project: a connected button group (tapping the selected priority again
  clears it) and a project picker offering Inbox plus the user's projects. The picker opens
  as a dropdown on a tap and as a press-and-hold drag list on a long press, the latter
  modelled on Google Translate's language button — the list appears with the current choice
  already under your finger, sliding moves the highlight, and releasing commits it
- Material 3 Expressive design with a floating action button menu
- Swipe-to-dismiss task deletion, plus a per-row delete button
- Adaptive navigation: `ShortNavigationBar` by default, `WideNavigationRail` only on
  windows that are at least medium width and height (and not half-folded)
- Navigation 3 back stack: `Today`, `Inbox` and `Projects` in the bar, `Settings` pushed from a top bar action
  (the bar and rail belong to the top level, so they are hidden while Settings — a page one level up —
  is open, and Settings shows a back arrow in the top bar instead)
- All user-facing strings in `res/values/strings.xml` (English)

## Known issues

**`server/` does not compile.** These are pre-existing bugs, not toolchain fallout —
`server/src` is untouched since the initial commit. Three distinct problems:

1. `Tables.kt:4` imports `org.jetbrains.exposed.java.time.datetime`. The package does not
   exist; in Exposed 0.54.0 the classes live in `org.jetbrains.exposed.sql.javatime`.
2. `Routing.kt` uses `FieldSet.select { }`, which Exposed 0.54.0 marks
   `@DeprecationLevel.ERROR` (the SELECT DSL was redesigned). It is a hard compile error,
   not a warning. The replacement is `selectAll().where { }`.
3. `Routing.kt` references `JWTPrincipal` and `payload` without importing them —
   `io.ktor.server.auth.*` does not cover the `io.ktor.server.auth.jwt` subpackage.

`:app` is unaffected and builds, lints, and assembles cleanly.

testtest
testtest66666