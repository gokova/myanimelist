# Feature 01: Authentication (OAuth 2.0)

## Overview
This feature implements the foundational OAuth 2.0 Authentication flow with MyAnimeList (MAL) using PKCE. It establishes secure token storage, automatic token refresh, and global state observation to ensure the user is always presented with the sign-in screen when unauthorized.

## Key Requirements
*   **OAuth 2.0 with PKCE & CSRF State:** Secure login using Chrome Custom Tabs, redirecting back to the app via `com.gokova.myanimelist://oauth2redirect`. (MAL API requires `code_challenge_method = "plain"` and `code_challenge == code_verifier`). Generates and strictly validates a cryptographically secure `state` parameter to prevent CSRF attacks.
*   **Token Storage & Atomic Session Cleanup:** Use Jetpack DataStore encrypted with Google Tink (in `:core:datastore`) with in-memory atomic reference caching to securely persist and swiftly read `access_token`, `refresh_token`, and the temporary PKCE `code_verifier` and `oauth_state`. `saveTokens()` atomically saves tokens while wiping transient flow parameters (`code_verifier`, `oauth_state`) in a single DataStore transaction.
*   **One-Time Redirect Consumption:** Redirect codes and errors are extracted into an `OAuthRedirectResult` sealed hierarchy and consumed immediately upon receipt (`consumeResult()`), ensuring stale codes never linger in Compose state or re-trigger authentication upon user logout.
*   **Global Auth State:** Observe the authentication state globally via `MainActivity` and `MainViewModel` through `ObserveAuthStateUseCase`. If the user is unauthenticated or the token becomes null, navigate them to `AuthRoute`.
*   **Token Interception & Refresh:** 
    *   OkHttp `AuthInterceptor`: Attaches the Bearer token to API requests.
    *   OkHttp `TokenAuthenticator`: Intercepts 401 Unauthorized errors, synchronizes concurrent refresh attempts, limits retries to prevent infinite loops, and refreshes tokens using an `@Unauthenticated` OkHttpClient.
*   **UI Restrictions:** No "Sign up", "Sign in with social media", or "Forgot password" features.

## Architecture & Data Flow
1. **Login Trigger:** User taps login on `AuthScreen`. `AuthViewModel` asynchronously requests an authorization URL via `GetAuthUrlUseCase` without blocking the main thread, and emits an `AuthUiEvent.OpenOAuthUrl` one-time event.
2. **Callback Handling:** The app catches the redirect URI via `OAuthRedirectHandler`. The redirect is parsed into `OAuthRedirectResult` (`Success` or `Error`) and consumed immediately. `AuthViewModel` triggers `LoginWithCodeUseCase` with `code` and `state`.
3. **Storage & Verification:** `AuthRepository` verifies that callback `state` matches the saved state, exchanges the authorization code for tokens, and atomically persists tokens while removing transient `code_verifier` and `oauth_state`.
4. **State Observation:** `MainActivity` collects `MainUiState` from `MainViewModel` (backed by `ObserveAuthStateUseCase`). When authenticated, the NavHost navigates to `MainRoute` (containing the bottom navigation).

---

## Technical Implementation Details

### 1. App Level & Navigation (`:app`)
*   **Type-Safe Navigation**: `navigation/Routes.kt` defining `@Serializable` objects (`AuthRoute`, `MainRoute`, `MyListRoute`, etc.). Active tab selection uses `NavDestination.hasRoute(destination.routeClass)`.
*   **`OAuthRedirectHandler.kt`**: Encapsulates redirect intent extraction, validates scheme and host, and provides `OAuthRedirectHandler` with `consumeResult()` for immediate single-use code consumption.
*   **`MainViewModel.kt`**: Exposes a unified `StateFlow<MainUiState>` (`Loading`, `Authenticated`) and coordinates session termination via `LogoutUseCase`.
*   **`MainActivity.kt`**: Uses `enableEdgeToEdge()` and collects `MainUiState`. Shows `LoadingScreen` until resolved, then renders `AppNavigation`.
*   **`MainScreen.kt`**: The main skeleton UI containing:
    *   `MainTopAppBar`: A zero-elevation TopAppBar featuring centered "MAL" and avatar placeholder.
    *   `LogoutConfirmationDialog`: A Soft & Modern confirmation dialog.
    *   `AppBottomBar`: A Material 3 BottomNavigationBar with type-safe `hasRoute` destination tracking and localized string resources.

### 2. Core: DataStore (`:core:datastore`)
*   **`AuthPreferencesImpl.kt`**: Implementation using `androidx.datastore.preferences`, `com.google.crypto.tink.Aead`, and in-memory `AtomicReference` caching. Exposes `accessToken: Flow<String?>`, `isLoggedIn: Flow<Boolean>`, `oauthState: Flow<String?>`, and handles atomic cleanup during `saveTokens()` and `clearTokens()`.

### 3. Core: Network (`:core:network`)
*   **`AuthInterceptor.kt`**: Synchronously attaches Bearer token from in-memory cache to non-oauth requests.
*   **`TokenAuthenticator.kt`**: Synchronized 401 interceptor that refreshes tokens via injected `@Unauthenticated OkHttpClient`, guards against infinite refresh loops, safely closes responses using `.use { ... }`, and triggers global logout on failure.
*   **`NetworkModule.kt`**: Provides `@Unauthenticated OkHttpClient` and `@Authenticated OkHttpClient` (with interceptor and authenticator attached).

### 4. Core: UI (`:core:ui`)
*   **Theme Updates**: Configured Pastel Blue colors in `Color.kt` and `Theme.kt`.
*   **Multipreviews**: Built `@StandardPreviews` for accessibility and responsive UI testing.

### 5. Feature: Auth (`:feature:auth`)
*   **Domain Layer (`domain/`)**:
    *   `AuthRepository`: Pure Kotlin repository contract with zero Android framework dependencies.
    *   `GetAuthUrlUseCase`: Decoupled from `Uri`, returns URL string.
    *   `LoginWithCodeUseCase`: Exchanging authorization code and validating state.
    *   `LogoutUseCase`: Clearing user session.
    *   `ObserveAuthStateUseCase`: Flow of login state.
*   **Data Layer (`data/`)**:
    *   `PkceGenerator`: Generates PKCE parameters conforming to MAL API's `plain` requirement and cryptographic CSRF `state`.
    *   `MalOAuthClient`: Network client using `@Unauthenticated OkHttpClient` without credential logging.
    *   `AuthRepositoryImpl`: Repository implementation constructing URLs with `HttpUrl`, validating `state`, and atomically saving tokens.
*   **Presentation Layer (`presentation/`)**:
    *   `AuthViewModel`: Non-blocking, reactive ViewModel exposing `StateFlow<AuthUiState>` (`Idle`, `Loading`, `Error`) and one-time event `AuthUiEvent.AuthSuccess`.
    *   `AuthScreen`: Stateless components using `collectAsStateWithLifecycle()` and strict string localization (`strings.xml`).
