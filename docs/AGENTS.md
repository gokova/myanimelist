# AI Agent Instructions (AGENTS.md)

Welcome! If you are an AI coding assistant (like Antigravity) working on this repository, you **MUST** read and adhere to these guidelines.

## ⚠️ STRICT RULE: Architectural Adherence
This project has a carefully designed **Feature-based Multi-Module** and **Offline-First** architecture. 
**You are NOT permitted to arbitrarily change the architecture, data flow, or tech stack.**

If the user requests a feature or change that conflicts with the established architecture, you must:
1. **Challenge the user.** Explain why the request breaks the architectural rules.
2. **Use `/grill-me`.** Propose starting a `/grill-me` session to formally re-evaluate the architecture and update this documentation if necessary.

## Architecture Blueprint

### 1. Data Flow (Offline-First)
*   **Single Source of Truth:** Room Database.
*   **Flow:** API -> Room -> Domain -> UI.
*   Network responses must ALWAYS be saved to Room first. The UI and Domain logic should only observe flows emitted from Room.

### 2. State Management (MVI-style)
*   ViewModels must expose a single `StateFlow<UiState>` per screen.
*   Use a unified data class wrapping the data (loading, success, error states).

### 3. Modularization
*   Code must be placed in the appropriate module (`:core:*` or `:feature:*`).
*   Feature modules should NOT depend on each other unless absolutely necessary (use navigation interfaces or the app module for orchestration).

### 4. Tech Stack Constraints
*   **UI:** Jetpack Compose (Material 3). No XML layouts.
*   **Navigation:** Jetpack Navigation Compose with **Type-Safe Routes** (Kotlin Serialization). Do not use string-based routes.
*   **Dependency Injection:** Hilt.
*   **Concurrency:** Kotlin Coroutines and Flows.
*   **Async Work:** WorkManager (used extensively for the background Recommendation Engine).

### 5. Feature Documentation
Whenever you implement a new major feature, generate a detailed markdown documentation file in this `docs/` folder explaining the feature's specific data flow, use cases, and module interactions.
