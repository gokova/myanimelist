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

### 5. Design System
*   **UI Guidelines:** All UI development must strictly adhere to the visual language described in `docs/DESIGN.md` (Soft & Modern aesthetic, pastel colors, large rounded corners, flat surfaces). You must consult this file when building new components or screens.

### 6. Feature Documentation (Living Tech Specs)
Whenever you start planning a new major feature, you **MUST** create a feature documentation file in this `docs/` folder **before** beginning any implementation.
*   **Chronological Naming:** Prefix the filename with a number (e.g., `01_authentication.md`).
*   **Living Document:** Use this file as a living "Tech Spec" to store your granular file-by-file implementation plan. This ensures your planned architecture and steps survive across multiple AI chat sessions.
*   **Finalization:** Once the feature is fully implemented, update the file to reflect the final data flow, use cases, and module interactions.

### 7. Domain Logic (Use Cases) & Clean Architecture Boundaries
*   **Orchestration, Not Mechanism:** Use Cases must ONLY coordinate application behavior (orchestration). They must **never** contain infrastructure mechanics like HTTP POST construction, OkHttp clients, or JSON parsing.
*   **Ports & Adapters:** Use Cases must depend on abstract interfaces (Ports) such as `UserRepository` or `MalAuthenticator`. The actual implementations (Adapters like `MalOAuthClient` or `RoomUserRepository`) belong in the Data/Infrastructure layer.
*   **No Business/Network Logic in ViewModels:** ViewModels should strictly manage UI state. All business logic MUST be extracted into these isolated Use Cases.
*   **Location:** Place use cases and interfaces in the `:core:domain` module for shared logic, or inside the feature module's `domain` package. Place implementations in `:core:network`, `:core:database`, or feature-specific `data` layers.

### 8. UI Implementation Rules
*   **Localization:** You MUST extract all user-facing strings to strings.xml. Hardcoding strings in Compose functions is strictly forbidden.
*   **Compose Modularity:** Break down large screens into small, self-managing, stateless Compose components. Avoid bloated LongMethod Compose functions. Extract components into a components package or within the same file if they are screen-specific.
*   **Visual Testing (Previews):** Always generate @Preview annotations for new UI components. See \docs/DESIGN.md\ for the standard Multipreview configuration you must apply.

