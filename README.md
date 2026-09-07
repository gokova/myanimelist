# MyAnimeList (MAL) Android Client

A modern Android application built to interact with the MyAnimeList API. This project serves as a portfolio piece demonstrating advanced Android development practices, Clean Architecture, and SOLID principles.

## Features (MVP)
*   **Authentication**: Secure OAuth 2.0 login via Chrome Custom Tabs and OkHttp Authenticator.
*   **My Anime List**: View personal categorized lists (Plan to Watch, Completed, Watching, etc.).
*   **Anime Taste Analytics**: Visual charts (e.g., bubble chart) summarizing user anime preferences.
*   **Recommendation Engine**: A background-driven (WorkManager) engine that fetches top seasonal/all-time animes, evaluates them against the user's taste, and generates personalized suggestions.
*   **Anime Details**: Detailed view of specific animes.

## Tech Stack
This project leverages the modern Android development ecosystem:
*   **UI Toolkit**: Jetpack Compose & Material 3
*   **Architecture**: Clean Architecture & MVI-style StateFlow (Offline-First)
*   **Language**: Kotlin
*   **Dependency Injection**: Hilt
*   **Local Persistence**: Room Database
*   **Concurrency & Reactive**: Kotlin Coroutines & Flow (StateFlow, SharedFlow)
*   **Networking**: Retrofit & OkHttp
*   **Serialization**: Kotlinx Serialization
*   **Navigation**: Jetpack Navigation Compose (Type-Safe Navigation via Serialization)
*   **Background Processing**: WorkManager

## App Structure
The project utilizes a **Feature-based Multi-Module** architecture to enforce strict boundaries, improve build times, and maintain high scalability.

*   `:app` - Application entry point, Hilt setup, and global navigation.
*   `:core:network` - Network definitions, Retrofit setup, MAL API interfaces, interceptors.
*   `:core:database` - Room database, DAOs, and Entity models.
*   `:core:ui` - Shared Compose components, theme, and design system.
*   `:core:domain` - Core use cases, domain entities, and standard Result/Resource wrappers.
*   `:feature:auth` - Authentication flow.
*   `:feature:mylist` - Personal anime list UI.
*   `:feature:taste` - Taste analytics and charts.
*   `:feature:recommendation` - Recommendation engine logic and Suggestions UI.
*   `:feature:details` - Anime detail screens.

## Testing Strategy
The testing strategy heavily focuses on the **Domain Layer**, verifying the complex business logic (especially the Recommendation Engine use cases) and ViewModels using MockK and Coroutines Test libraries.

## Architecture Guidelines
If you are an AI coding assistant working on this repository, please consult the `docs/AGENTS.md` file for strict architectural rules.
