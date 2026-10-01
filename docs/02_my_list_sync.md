# Feature 02: My List and Synchronization

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:mylist |
| Entry surface | MyListRoute |
| Related systems | :core:database, :core:datastore, :core:network |

## Purpose

Show the user's MAL anime list from a local cache immediately, then keep that cache current
through a rate-limited, paginated synchronization flow.

## User-facing behavior

- The My List destination shows cached Room data without waiting for the network.
- The user can filter by All, Watching, Completed, On Hold, Dropped, or Plan to Watch and can
  sort by score, title, or last update.
- Entering the screen starts a stale-while-revalidate sync when the 15-minute cooldown has
  elapsed. Pull-to-refresh and the empty-state action force a sync.
- Synchronization is non-blocking: started and failed states are surfaced as events while Room
  emissions update the cards. A failed refresh retains cached content and offers retry.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:mylist domain | Defines list models, filter and sort options, sync status, and observe/sync use cases. |
| :feature:mylist data | Fetches every MAL list page, maps wire models, coordinates the cooldown, and writes the list. |
| :core:database | Stores normalized anime and user-list rows and exposes filtered Room Flows. |
| :core:datastore | Stores the latest successful list-sync timestamp. |
| :core:network | Provides the authenticated MAL list endpoint and retry support. |

## Data flow

1. MyListViewModel combines the selected category and sort option with
   ObserveUserAnimeListUseCase.
2. The repository queries UserAnimeListDao and maps Room relations to UserAnime; the resulting
   Flow continuously updates the screen.
3. The ViewModel invokes SyncUserAnimeListUseCase on entry or after a user refresh.
4. Unless forced, MyListRepositoryImpl skips a run within the 15-minute cooldown. Otherwise the
   remote data source follows MAL paging until it has every entry.
5. The repository maps fetched entries and calls the DAO's transactional sync method, then stores
   the successful timestamp. Room publishes the updated list.

## Implementation map

| Path | Responsibility |
| --- | --- |
| feature/mylist/presentation/MyListScreen.kt and MyListViewModel.kt | Screen state, filters, sorting, refresh, and transient sync feedback. |
| feature/mylist/presentation/components/ | List cards, status chips, category tabs, header, empty state, and previews. |
| feature/mylist/domain/usecase/ObserveUserAnimeListUseCase.kt | Applies user-selected sorting to the observed list. |
| feature/mylist/domain/usecase/SyncUserAnimeListUseCase.kt | Exposes synchronization as Flow of SyncStatus. |
| feature/mylist/data/remote/AnimeListRemoteDataSource*.kt | Retrieves all MAL pages. |
| feature/mylist/data/repository/MyListRepositoryImpl.kt | Enforces cooldown, maps data, and coordinates persistence. |
| feature/mylist/data/mapper/AnimeListMapper.kt | Converts MAL entries to Room and domain models. |
| core/database/dao/UserAnimeListDao.kt | Queries and transactionally replaces the cached list. |

## Constraints

- A complete remote result is persisted as one Room transaction so categories do not show a
  partially refreshed list.
- Remote paging uses MAL-provided next URLs and request pacing; cancellation is rethrown.
- DTOs and database entities do not leave the data layer.
- Cards preserve the score slot for missing ratings and use localized resources for visible text
  and quantities.

## Verification

feature/mylist/src/test covers mapping, repository behavior, list observation and sorting, sync
status, and MyListViewModel behavior. Core database tests cover the underlying Room contracts.
