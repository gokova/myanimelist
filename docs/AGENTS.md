# AI Agent Instructions (AGENTS.md)

Welcome! If you are an AI coding assistant (like Antigravity) working on this repository, you **MUST** read and adhere to these guidelines.

---

# Project Overview

This project is a personal Android application for analyzing and visualizing data from the MyAnimeList (MAL) API.

The primary goal is maintainability, readability, scalability, and strict separation of concerns. Code should prioritize simplicity over cleverness.

This application is intended for a single user (the developer). Do not introduce unnecessary abstractions for multi-user or enterprise scenarios.

---

# Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose & Material 3 (adhering to `docs/DESIGN.md`)
- **Architecture:** Clean Architecture & MVI-style StateFlow (Feature-based Multi-Module)
- **Dependency Injection:** Hilt
- **Networking:** Retrofit & OkHttp
- **Serialization:** Kotlinx Serialization
- **Local Persistence:** Room Database & Jetpack DataStore (Tink Encrypted)
- **Concurrency:** Kotlin Coroutines & Flow (StateFlow, SharedFlow, Channels)
- **Navigation:** Jetpack Navigation Compose with Type-Safe Routes (Kotlin Serialization)
- **Background Processing:** WorkManager

---

# General Principles

- Follow SOLID principles strictly:
  - **Single Responsibility:** Each class must have one reason to change (e.g., separate PKCE generation, network client, and repository).
  - **Open/Closed:** Prefer composition and extension over modifying working abstractions.
  - **Liskov Substitution:** Test fakes and subclasses must adhere to contract behaviors without throwing unexpected exceptions.
  - **Interface Segregation:** Keep interfaces cohesive; do not force clients or fakes to implement unneeded methods.
  - **Dependency Inversion:** Depend on abstractions, not concrete implementations. ViewModels depend on Use Cases, Use Cases depend on Repository interfaces.
- Prefer composition over inheritance.
- Keep classes focused and avoid large, bloated classes.
- Avoid unnecessary abstractions.
- Prefer immutable data structures.
- Favor readability and simplicity over clever optimizations.
- Do not introduce new libraries unless explicitly requested.
- **YAGNI & No Dead Code:** Do not write speculative functions, unused parameters, or placeholder abstractions. Never use `@Suppress("UnusedParameter")` or suppressions to preserve dead code—delete it instead.

---

# Architecture & Modularization

The project is organized by feature.

Each feature owns its own:
- `data/`
- `domain/`
- `presentation/`
- `di/`

No feature module should directly access another feature's internal implementation. Feature modules communicate through shared domain models/use cases or navigation routes. Shared functionality belongs inside `:core:*` modules.

### Feature Structure

```
feature_x/
    data/
        local/
        remote/
        mapper/
        repository/

    domain/
        model/
        repository/
        usecase/

    presentation/
        screen/
        viewmodel/
        components/

    di/
```

- Repository interfaces belong inside: `domain/repository/`
- Repository implementations belong inside: `data/repository/`

---

# Core Modules

Core modules contain reusable functionality shared by multiple features:
- `:core:network` - OkHttp, Retrofit, authentication interceptors and authenticators.
- `:core:database` - Room database, DAOs, and database entities.
- `:core:datastore` - DataStore preferences and token storage.
- `:core:domain` - Core domain entities, common models, and shared Result wrappers.
- `:core:ui` - Shared Compose components, theme, and design system.

Never place feature-specific business logic inside core.

---

# Data Layer Rules

The data layer is responsible for:
- Remote API communication (Retrofit services, OkHttp clients)
- Local database access (Room DAOs)
- DTOs and DataStore preferences
- Entity and DTO mapping
- Repository implementations

Rules:
- Remote models (DTOs) must never reach Presentation.
- Database entities must never reach Presentation.
- Convert DTOs and Entities into Domain models using mappers.
- Keep Retrofit and Room isolated from business logic.
- Always cleanly close network responses and I/O resources (e.g., using `.use { ... }`).
- **Atomic Multi-Field Persistence:** When persisting related data (e.g., storing auth tokens while clearing transient PKCE verifiers and CSRF states), execute them in a single atomic transaction (e.g., inside a single `dataStore.edit { ... }` block or Room `@Transaction`) to prevent partial or corrupted state.
- **Type-Safe URL Construction:** Never construct URLs with query parameters via manual string concatenation. Always use `HttpUrl.toHttpUrl().newBuilder()` or Retrofit to guarantee correct character escaping and encoding.
- **API Contract Verification:** Always verify the exact wire format and JSON field names against the official API documentation before implementing network DTOs and mappers. Never guess field names (e.g., MAL API `users/{user_id}/animelist` returns `list_status`, not `my_list_status`). Write unit tests locking in serialization contracts.
- **Resilient Network & Rate Limiting:** Remote data sources must pace requests politely (e.g., 500ms delay between paginated requests), follow URL-based pagination (`paging.next`) when provided by the API contract, and retry on transient network errors including both `IOException` and `HttpException` (for HTTP 429 and 5xx) using exponential backoff. Never catch or swallow `CancellationException`.
- **Safe Database Migrations:** Never use `.fallbackToDestructiveMigration(true)` in Room configuration—always provide safe migrations or let missing migrations fail fast during development rather than silently destroying user offline data.

---

# Domain Layer Rules

The domain layer contains pure business logic.

Rules:
- **Zero Android Framework Dependencies:** The domain layer must NEVER import `android.*` packages (e.g., `android.net.Uri`, `android.content.Context`). Domain contracts must use platform-neutral types (e.g., `String` for URLs).
- **Zero Framework Leakage:** Domain must not depend on Retrofit, Room, or Jetpack Compose.
- **No Presentation Callbacks in Domain:** Never pass UI callbacks (e.g., `onSyncStarted: () -> Unit`) into repository or use case methods. Operations with intermediate statuses or progress must return reactive streams like `Flow<DomainStatus>` (e.g., `Flow<SyncStatus>`), preserving clean unidirectional data flow.
- Domain contains: Models, Repository interfaces, and Use Cases.
- Use cases should represent a single business action (e.g., `GetAuthUrlUseCase`, `LoginWithCodeUseCase`, `LogoutUseCase`, `ObserveAuthStateUseCase`).

---

# Presentation & ViewModel Rules

Presentation uses:
- Jetpack Compose (Material 3)
- ViewModel
- `StateFlow` for state
- `Channel` or `SharedFlow` for one-time events

Rules:
- **MVI Contract:** ViewModels must expose a single immutable `StateFlow<UiState>` per screen.
- **State vs. Event Distinction:** `UiState` must strictly represent continuous visual states (e.g., `Idle`, `Loading`, `Content`, `Error`). Transient actions (navigation, toast/snackbars, opening browser URLs) must be emitted as one-time events via buffered Channels / Flows (e.g., `Channel<AuthUiEvent>(Channel.BUFFERED)`), never as UI state properties that could re-trigger on recomposition.
- **Lifecycle-Aware State Collection:** Always use `collectAsStateWithLifecycle()` (from `androidx.lifecycle.compose`) in composable screens instead of plain `collectAsState()` to prevent background collection and resource leaks.
- **One-Time External Input Consumption:** Deep links, OAuth redirects, and external intent extras must follow a strict one-time consumption pattern. Immediately clear or consume them upon receipt (e.g., via `consumeResult()`) so navigating back to the screen or recomposing (such as after user logout) never re-executes stale operations.
- **Reactive UI:** Composable functions should only render UI from state. No business logic in composables.
- **Never Block Main Thread:** Never call `runBlocking` inside ViewModels or Composable handlers. All asynchronous work must be executed via `viewModelScope.launch` using suspend functions.
- **No Direct Data/Infra in ViewModels:** ViewModels must strictly interact with Domain Use Cases. Never inject `DataStore`, `AuthPreferences`, Room DAOs, or Retrofit services directly into ViewModels.
- Avoid passing ViewModels deep into the UI hierarchy; hoist state and pass lambdas.
- **Stateful vs. Stateless Screen Split:** Always split screens into an internal/private stateful composable (which injects the ViewModel via Hilt) and a public stateless composable (which only receives state and event callbacks). Previews must render the stateless composable to avoid `HiltViewModelFactory` preview crashes.
- **Configuration-Aware Window Sizing:** Never use `LocalConfiguration.current.screenWidthDp` to derive responsive layout branching. Always use `LocalWindowInfo.current.containerSize` converted with `LocalDensity.current` to ensure window size calculations are configuration-aware and preview-safe.

---

# Navigation

- Use Jetpack Navigation Compose with **Type-Safe Routes** (Kotlin Serialization).
- Always use `NavDestination.hasRoute(KClass)` to check active route hierarchy (e.g., `currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) }`).
- Do not use string matching or class qualified name matching for route determination.

---

# UI & Design System

- Adhere to the **"Soft & Modern"** design guidelines in `docs/DESIGN.md` (Pastel Blue palette, rounded shapes, tonal depth without harsh shadows).
- **Strict Localization:** You MUST extract all user-facing strings to `strings.xml`. Hardcoding strings in Compose functions, ViewModels, or preview providers is strictly forbidden.
- **Visual Previews:** Always apply `@StandardPreviews` to composable UI components for responsive and accessible preview validation.
- **Descriptive Preview Parameters:** Override `getDisplayName(index: Int)` on all `PreviewParameterProvider` classes with human-readable descriptions (e.g., `"Watching"`, `"Empty"`, `"Syncing"`) instead of relying on default indexed labels.
- Keep composables modular, small, and stateless where possible.

---

# Logging & Security

- **Avoid Ad-Hoc Logging:** Do not leave `android.util.Log` statements in production code.
- **Never Log Sensitive Data:** Never log access tokens, refresh tokens, credentials, or user-identifying data.
- **Strict Token & Header Redaction:** OkHttp logging interceptors must redact sensitive headers (`redactHeader("Authorization")`, `redactHeader("Cookie")`) and use `HttpLoggingInterceptor.Level.BASIC` for authentication/token exchange clients so tokens are never exposed in logcat.
- **OAuth & CSRF Security:** Always implement PKCE (with cryptographically secure verifiers) and validate high-entropy CSRF `state` parameters on OAuth callback redirects.

---

# Concurrency & Coroutines

- Follow structured concurrency.
- Never block the Main thread.
- Prefer suspend functions for one-shot operations and `Flow` for streams of data.
- Use Dispatchers appropriately (`Dispatchers.IO` for disk/network).

---

# Dependency Injection (Hilt)

- Use Hilt for all dependency injection.
- When multiple instances of a type exist (e.g., OkHttp clients), use qualifiers (e.g., `@Authenticated` vs `@Unauthenticated`).
- Ensure interceptors and authenticators are actively wired into network configurations.

---

# Testing & Code Quality

- Write testable code with constructor injection and interface-based boundaries.
- Domain Use Cases and ViewModels must have accompanying unit tests using `kotlinx-coroutines-test` and test fakes.
- Test fakes must implement interface contracts cleanly without throwing arbitrary exceptions.
- **Lifecycle & Edge Case Testing:** Unit test transient flows including cancellations (e.g., user denying permissions/OAuth), one-time event consumption, and navigation resets (e.g., logging out then logging back in).
- **Minimum SDK Compatibility (`minSdk = 24`):** Always maintain compatibility with Android API level 24. Do not use API 26+ Java 8 APIs (`java.time.*`, `java.util.function.*`, `java.util.stream.*`) without core library desugaring. For date and timestamp parsing, use Java 7-compatible utilities (`java.text.SimpleDateFormat` with UTC/Locale, or `kotlin.time.Duration`).
- **Static Analysis & Formatting Compliance:** All code MUST pass:
  - `./gradlew test` (100% tests pass)
  - `./gradlew detekt` (0 violations, strictly respecting the 100-character line length limit)
  - `./gradlew ktlintCheck` (100% compliant formatting)
  - `./gradlew lintDebug` (0 NewApi or critical lint errors)

---

# Feature Documentation (Living Tech Specs)

Whenever you start planning a new major feature, you **MUST** create a feature documentation file in this `docs/` folder **before** beginning any implementation.
- **Chronological Naming:** Prefix the filename with a number (e.g., `01_authentication.md`).
- **Living Document:** Use this file as a living "Tech Spec" to store your granular file-by-file implementation plan. This ensures your planned architecture and steps survive across multiple AI chat sessions.
- **Finalization:** Once the feature is fully implemented, update the file to reflect the final data flow, use cases, and module interactions.

---

# Agent Behavior

When implementing features or refactoring:
- Modify existing architecture instead of inventing new patterns.
- Reuse existing components whenever possible.
- Do not duplicate code.
- Keep changes minimal, focused, and well-verified.
- Preserve architectural consistency.
- Ask before introducing new dependencies.
