# Feature 07: User Profile and Avatar

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:profile |
| Entry surface | ProfileRoute from the main app-bar avatar |
| Related systems | :app, :core:datastore, :core:domain, :core:network |

## Purpose

Fetch and cache the authenticated MAL user's profile and anime statistics, show the avatar in the
app bar, and provide a dedicated profile screen with session logout.

## User-facing behavior

- The main app bar displays the cached profile picture when available and a person placeholder
  otherwise. Tapping it navigates to the Profile destination.
- The profile screen renders cached data while a refresh is in progress, presents an error with a
  retry action when no profile is available, and includes a back action.
- Profile content includes the avatar, account details, primary watch metrics, and status
  distribution. User-visible dates and quantities are localized.
- Logout is initiated from the profile screen and requires confirmation before the app clears the
  session and returns to authentication.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :core:domain | Owns UserProfile and UserAnimeStatistics, ProfileRepository, and observe/refresh use cases. |
| :feature:profile data | Requests the authenticated MAL profile, maps the DTO, and persists it for the active session. |
| :core:datastore | Persists profile fields alongside session data and rejects stale-session writes. |
| :feature:profile presentation | Combines cached profile, refresh state, errors, and logout-dialog state into ProfileUiState. |
| :app | Refreshes profile for an authenticated session, supplies the app-bar avatar, and hosts ProfileRoute. |
| :core:network | Provides the authenticated users/@me profile endpoint. |

## Data flow

1. MainViewModel observes the auth state and cached profile. When a user becomes authenticated, it
   starts RefreshUserProfileUseCase and uses the cached picture URL for MainTopAppBar.
2. ProfileViewModel observes the same profile Flow and refreshes on creation or retry.
3. ProfileRepositoryImpl captures the current session ID, requests users/@me with
   anime_statistics, maps the DTO to UserProfile, and saves it only if that session remains active.
4. UserPreferences emits the saved profile to MainViewModel and ProfileViewModel, updating the
   avatar and profile screen reactively.
5. Logout clears the session DataStore, including cached profile values, so both surfaces revert
   immediately.

## Implementation map

| Path | Responsibility |
| --- | --- |
| app/MainViewModel.kt | Coordinates auth-scoped refreshes and exposes the avatar URL. |
| app/MainActivity.kt and MainScreen.kt | Registers ProfileRoute and routes main app-bar avatar clicks. |
| feature/profile/navigation/ProfileRoute.kt | Defines the type-safe profile route. |
| feature/profile/presentation/ProfileScreen.kt and ProfileViewModel.kt | Screen state, retry, back, logout confirmation, and events. |
| feature/profile/presentation/components/ProfileHeader.kt | Avatar and account header. |
| feature/profile/presentation/components/ProfileStatsSection.kt | Metrics and accessible status distribution. |
| feature/profile/data/repository/ProfileRepositoryImpl.kt | Fetches, session-checks, maps, and saves profile data. |
| feature/profile/data/mapper/ProfileMapper.kt | Converts network DTOs to core domain models. |
| core/domain/repository/ProfileRepository.kt and usecase/ | Shared profile contract plus observe and refresh use cases. |
| core/datastore/UserPreferences*.kt | Session-bound profile persistence and clearing. |

## Constraints

- The profile endpoint is authenticated and requests anime_statistics.
- A profile write must use the session ID observed before the request; a logout or account switch
  cannot allow that stale response to repopulate cached data.
- Clearing auth tokens clears the shared session DataStore, including profile fields.
- Runtime logout confirmation has an inspection-mode simulated overlay so it remains visible in
  Compose previews.

## Verification

feature/profile/src/test covers mapping, repository session safety, ProfileViewModel behavior, and
date formatting. core/datastore/src/test covers session/profile persistence boundaries.
