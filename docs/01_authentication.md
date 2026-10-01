# Feature 01: Authentication

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:auth |
| Entry surface | AuthRoute and the OAuth redirect activity intent |
| Related systems | :app, :core:datastore, :core:network |

## Purpose

Authenticate the single app user with the MyAnimeList OAuth 2.0 flow, retain a secure
session, and return the app to the signed-out state when that session is no longer valid.

## User-facing behavior

- The sign-in screen opens the MAL authorization page in a Chrome Custom Tab.
- The app consumes the redirect to com.gokova.myanimelist://oauth2redirect once, then
  either completes sign-in or presents the returned error.
- Application navigation observes the session and shows the authenticated application only
  after a valid session is available.
- A logout action clears the session and returns the user to authentication. Account
  recovery, social sign-in, and sign-up are not part of this feature.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:auth domain | Defines the authentication contract and the get-URL, login, logout, and observe-session use cases. |
| :feature:auth data | Generates PKCE and CSRF values, builds the authorization URL, validates the callback state, and exchanges the code for tokens. |
| :core:datastore | Stores tokens and temporary PKCE/CSRF values; token saves and session cleanup are atomic. |
| :core:network | Adds bearer credentials to authenticated requests and refreshes a session after a 401 response. |
| :app | Parses the redirect intent, launches external authorization, and gates root navigation on auth state. |

## Data flow

1. AuthScreen asks AuthViewModel to begin sign-in.
2. GetAuthUrlUseCase creates a URL after AuthRepositoryImpl stores a PKCE verifier and CSRF
   state; the ViewModel emits a one-time URL event.
3. OAuthRedirectHandler parses and consumes the callback. AuthViewModel passes its code and
   state to LoginWithCodeUseCase.
4. AuthRepositoryImpl validates the state, exchanges the code through MalOAuthClient, and
   atomically stores the tokens while clearing temporary values.
5. ObserveAuthStateUseCase drives MainViewModel and root navigation. AuthInterceptor attaches
   the access token; TokenAuthenticator refreshes it after an eligible 401 or clears the
   session on refresh failure.

## Implementation map

| Path | Responsibility |
| --- | --- |
| app/navigation/OAuthRedirectHandler.kt | Parses and one-time consumes the OAuth callback. |
| app/MainActivity.kt and app/MainViewModel.kt | Launches authorization and selects the signed-in or signed-out navigation graph. |
| feature/auth/AuthScreen.kt and AuthViewModel.kt | Renders sign-in state and emits one-time browser and completion events. |
| feature/auth/data/pkce/PkceGenerator.kt | Produces the MAL-compatible plain PKCE verifier/challenge and CSRF state. |
| feature/auth/data/repository/AuthRepositoryImpl.kt | Coordinates authorization URL creation, callback validation, token exchange, and logout. |
| core/datastore/AuthPreferences*.kt | Persists and clears credentials and transient OAuth values. |
| core/network/auth/AuthInterceptor.kt and TokenAuthenticator.kt | Applies credentials and performs synchronized token refresh. |

## Constraints

- MAL requires plain PKCE: the verifier is the challenge and the request declares the plain
  challenge method.
- Callback state must match the stored state before exchanging a code.
- Redirect values are transient and must never be processed more than once.
- Authenticated and unauthenticated clients remain separate; tokens and authorization headers
  must not be logged.

## Verification

Unit coverage is in feature/auth/src/test for PKCE generation, OAuth exchange, repository
behavior, use cases, and AuthViewModel. Core network and datastore suites cover the token
authenticator and persisted session behavior.
