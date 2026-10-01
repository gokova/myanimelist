# Feature 06: Anime Details and List Integration

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:details |
| Entry surface | AnimeDetailsRoute from list, taste, recommendation, and detail carousels |
| Related systems | :core:database, :core:domain, :core:network, :feature:mylist, :feature:recommendation |

## Purpose

Provide a full-screen, offline-first detail view for any anime and let the user add an unlisted
title to Plan to Watch without leaving the context in which it was discovered.

## User-facing behavior

- A card body opens the detail destination; a related-anime or recommendation card can push
  another detail destination. Poster-preview behavior remains independent of detail navigation.
- The screen immediately renders cached metadata if present and refreshes rich MAL details in the
  background. A failed refresh keeps cache content and reports a recoverable error.
- It displays title variants, scores, user-list status, synopsis, taxonomy, metadata, related
  anime, and MAL recommendations. Layout adapts between compact and wide screens.
- An anime not in the user list has an Add to List action. Success adds it as Plan to Watch,
  removes it from recommendation/New Seasons discovery rows, and updates the displayed status.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:details domain | Defines detail, related, and recommendation models plus observe, refresh, and add-to-list use cases. |
| :feature:details data | Reads cached rows, fetches MAL detail DTOs, maps data, and retains a small in-memory fresh-detail cache. |
| :feature:details presentation | Owns refresh/add state and renders screen sections, error state, and responsive layouts. |
| :core:database | Provides cached anime and user-list data, plus the transactional add-and-clean operation. |
| :core:network | Retrieves rich anime details and updates the user's MAL list status. |
| :core:domain taxonomy | Partitions displayed tags into genres and themes. |

## Data flow

1. AnimeDetailsRoute supplies its anime ID to AnimeDetailsViewModel.
2. ObserveAnimeDetailsUseCase combines cached anime and user-list rows with any fresh in-memory
   detail value, producing an immediate offline-first state.
3. RefreshAnimeDetailsUseCase calls the MAL detail endpoint, upserts core metadata, stores any
   returned list status, and updates the bounded fresh-detail cache.
4. The ViewModel renders the newest available value and emits a recoverable event if refresh
   fails without usable cache data.
5. AddAnimeToMyListUseCase updates MAL with Plan to Watch and invokes the DAO transaction that
   adds the user-list row while removing recommendation and New Seasons entries.

## Implementation map

| Path | Responsibility |
| --- | --- |
| feature/details/navigation/AnimeDetailsRoute.kt | Type-safe detail route carrying the anime ID. |
| feature/details/presentation/AnimeDetailsScreen.kt and AnimeDetailsViewModel.kt | Stateful screen, presentation state, refresh, retry, add, and navigation events. |
| feature/details/presentation/components/ | Header, metadata, synopsis, taxonomy, related carousel, and recommendation carousel. |
| feature/details/domain/usecase/ObserveAnimeDetailsUseCase.kt | Observes locally available details. |
| feature/details/domain/usecase/RefreshAnimeDetailsUseCase.kt | Fetches and maps up-to-date MAL details. |
| feature/details/domain/usecase/AddAnimeToMyListUseCase.kt | Adds a title to Plan to Watch. |
| feature/details/data/repository/AnimeDetailsRepositoryImpl.kt | Coordinates Room, MAL, and bounded fresh-detail cache access. |
| feature/details/data/mapper/AnimeDetailsMapper.kt | Maps detail DTOs and Room entities to domain models. |
| core/database/dao/UserAnimeListDao.kt | Supplies cached detail rows and performs add-and-clean persistence. |

## Constraints

- Related and recommendation carousel data is intentionally a fresh-detail cache, whereas core
  anime metadata and list status are persisted in Room.
- The in-memory fresh-detail cache is limited to ten entries.
- A details refresh must not remove useful cached content on a network failure.
- Adding a title uses the MAL Plan to Watch status with zero watched episodes and score, then
  updates local data transactionally.

## Verification

feature/details/src/test covers DTO/entity/domain mapping, repository caching and add-to-list
behavior, use cases, and AnimeDetailsViewModel state/event handling.
