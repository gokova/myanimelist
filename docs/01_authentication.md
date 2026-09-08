# Feature 01: Authentication (OAuth 2.0)

## Overview
This feature implements the foundational OAuth 2.0 Authentication flow with MyAnimeList (MAL) using PKCE. It establishes secure token storage, automatic token refresh, and global state observation to ensure the user is always presented with the sign-in screen when unauthorized.

## Key Requirements
*   **OAuth 2.0 with PKCE:** Secure login using Chrome Custom Tabs, redirecting back to the app via `com.gokova.myanimelist://oauth2redirect`.
*   **Token Storage:** Use Jetpack DataStore encrypted with Google Tink (in `:core:datastore`) to securely persist the `access_token`, `refresh_token`, and the temporary PKCE `code_verifier`. (Replaced `EncryptedSharedPreferences` for modern future-proofing).
*   **Global Auth State:** Observe the authentication state globally via `MainActivity`. If the user is unauthenticated or the token becomes null, navigate them to the `AuthRoute`.
*   **Token Interception & Refresh:** 
    *   OkHttp `Interceptor`: Attaches the Bearer token to API requests.
    *   OkHttp `TokenAuthenticator`: Intercepts 401 Unauthorized errors, attempts to fetch a new token using the `refresh_token`, and retries the request. If refresh fails, it clears the tokens, triggering a global logout.
*   **UI Restrictions:** No "Sign up", "Sign in with social media", or "Forgot password" features.

## Architecture & Data Flow
1. **Login Trigger:** `AuthScreen` triggers the `GetAuthUrlUseCase` to generate a PKCE verifier, saves it, and opens the MAL OAuth URL.
2. **Callback Handling:** The app catches the redirect URI. `AuthViewModel` triggers `LoginWithCodeUseCase` to exchange the authorization code for tokens via the MAL Token API.
3. **Storage:** Tokens are saved in `AuthPreferencesImpl` (Jetpack DataStore + Tink `Aead`) located in `:core:datastore`.
4. **State Observation:** `MainActivity` collects an `isLoggedIn` flow from `MainViewModel`. When `true`, the NavHost navigates to `MainRoute` (containing the bottom navigation).

---

## Technical Implementation Details

### 1. App Level & Navigation (`:app`)
*   **Type-Safe Navigation**: Added `navigation/Routes.kt` defining `@Serializable` objects (`AuthRoute`, `MainRoute`, `MyListRoute`, etc.).
*   **`MainActivity.kt`**: Uses `enableEdgeToEdge()` and collects the `isLoggedIn` flow from `MainViewModel`. Shows a Splash/Loading screen until the flow resolves, then routes to `AuthRoute` or `MainRoute`. Also observes logout events to seamlessly navigate back to `AuthRoute`.
*   **`MainScreen.kt`**: The main skeleton UI containing:
    *   `MainTopAppBar`: A zero-elevation TopAppBar featuring the centered "MAL" title and a circular user avatar placeholder.
    *   `LogoutConfirmationDialog`: A Soft & Modern dialog confirming user logout intent.
    *   `AppBottomBar`: A Material 3 BottomNavigationBar ("My List", "My Taste", "Recommendations") routing to destination composables.

### 2. Core: DataStore (`:core:datastore`)
*   **`AuthPreferencesImpl.kt`**: Implementation using `androidx.datastore.preferences` and `com.google.crypto.tink.Aead`. Exposes `accessToken: Flow<String?>`, `isLoggedIn: Flow<Boolean>`, handles the temporary `code_verifier`, and provides `clearTokens()` for session termination.

### 3. Core: Network (`:core:network`)
*   **`AuthInterceptor.kt`**: OkHttp Interceptor that synchronously reads the `accessToken` from `AuthPreferences`.
*   **`TokenAuthenticator.kt`**: OkHttp Authenticator that catches `401 Unauthorized` and makes a synchronous network call to refresh the token using `kotlinx.serialization` and an `@OptIn(InternalSerializationApi::class)`.
*   **`NetworkModule.kt`**: Provides the singleton `OkHttpClient` with the interceptor and authenticator attached.

### 4. Core: UI (`:core:ui`)
*   **Theme Updates**: Configured the Pastel Blue colors (for Light and Dark themes as defined in `DESIGN.md`) in `Color.kt` and `Theme.kt`. 
*   **Multipreviews**: Built `@StandardPreviews` for comprehensive accessibility and responsive UI testing.

### 5. Feature: Auth (`:feature:auth`)
*   **`AndroidManifest.xml`**: Registers the `<intent-filter>` for OAuth deep linking.
*   **Clean Architecture (Domain & Data)**:
    *   `MalAuthenticator`: Abstract domain port for OAuth URL generation, code exchange, and logout.
    *   `MalOAuthClient`: Infrastructure adapter handling OkHttp FormBody requests and JSON decoding.
    *   `GetAuthUrlUseCase`: Coordinates PKCE generation and URL acquisition.
    *   `LoginWithCodeUseCase`: Coordinates authorization code token exchange.
    *   `LogoutUseCase`: Orchestrates session invalidation and DataStore token removal.
*   **`AuthScreen.kt`**: Highly modular screen (`AuthHeader`, `AuthActionArea`) using strict localization (`strings.xml`) and deep link intent interception.
