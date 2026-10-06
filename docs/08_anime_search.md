# Feature 08: Anime Search

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:search |
| Entry surface | Search icon action in MainTopAppBar |
| Related systems | :core:network, :core:database, :core:domain, :core:ui, :feature:details |

## Purpose

Enable online anime title searching against the MyAnimeList catalog without saving search results or untracked anime metadata to the local Room database, while calculating real-time taste match ratios for discovered titles.

## User-facing behavior

- Tapping the search icon in the top app bar opens a full-screen search view without bottom navigation or navigation rail.
- A top-anchored Material 3 search bar auto-focuses with the software keyboard visible on entry.
- Queries require at least 3 characters before search can be triggered. When fewer than 3 characters are entered, an inline validation hint is shown and the search action is disabled.
- Search execution is triggered explicitly via the keyboard search action (IME Action Search) or the trailing search icon.
- Results stream into a scrollable list of rich anime cards displaying poster thumbnails, localized titles, taste match percentage badges (for users with at least 5 anime in their list), MAL mean scores, media formats, episode counts, genres, and personal list status chips (if the anime is already in the user's list).
- Infinite scrolling pagination loads 20 results per page and fetches subsequent pages using MAL's next page links with inline loading and retry indicators.
- Tapping a search result card navigates to the Anime Details screen. Search results and untracked anime metadata are not persisted to Room unless the user explicitly taps "Add to Plan to Watch".

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:search presentation | Owns search query state, pagination states, MVI UI state/events, and Material 3 search bar UI. |
| :feature:search domain | Validates search queries, exposes search/pagination use cases, and calculates candidate taste match ratios. |
| :feature:search data remote | Executes paginated MAL search requests and next page fetches with request pacing and exponential retry. |
| :feature:search data repository | Coordinates remote data source and local taste profile items to produce scored search results. |
| :feature:details | Employs ephemeral in-memory caching for untracked search titles, writing metadata to Room only upon list addition. |
| :core:network | Provides Retrofit endpoint for GET /v2/anime with query, limit, offset, and fields. |
| :core:database | Supplies user list items for taste profile evaluation and persists untracked anime only when added to user list. |
| :core:domain | Provides taxonomy classification and taste matching algorithms. |

## Data flow

1. The user taps the search icon in MainTopAppBar and navigates to SearchRoute.
2. The user enters a query (>= 3 characters) and submits via the keyboard search action or icon.
3. SearchViewModel emits searching state and invokes SearchAnimeUseCase.
4. SearchRepositoryImpl calls SearchRemoteDataSource to fetch the first page of results from MAL API (GET /v2/anime).
5. SearchRepositoryImpl reads the user's anime list from UserAnimeListDao to build their taste profile, calculates taste match percentages for each candidate, and marks existing list statuses.
6. The ViewModel displays the content state with rich search cards.
7. As the user scrolls near the bottom of the list, LoadMoreSearchResultsUseCase fetches the next page using paging.next and appends results.
8. Tapping a result navigates to AnimeDetailsRoute. If the title is not already saved in Room, AnimeDetailsRepositoryImpl caches it purely in memory.
9. Tapping "Add to Plan to Watch" on details persists the anime metadata and list entry to Room transactionally.

## Implementation map

| Path | Responsibility |
| --- | --- |
| docs/08_anime_search.md | Living technical specification for the search feature. |
| core/network/src/main/java/com/gokova/myanimelist/core/network/api/MalApiService.kt | GET /v2/anime endpoint definition. |
| feature/details/src/main/java/com/gokova/myanimelist/feature/details/data/repository/AnimeDetailsRepositoryImpl.kt | Ephemeral details caching for untracked anime and foreign-key safe persistence on add. |
| feature/search/src/main/java/com/gokova/myanimelist/feature/search/ | Complete :feature:search module implementation (data, domain, presentation, di, navigation). |
| app/src/main/java/com/gokova/myanimelist/components/MainTopAppBar.kt | Search icon action leading the avatar button. |
| app/src/main/java/com/gokova/myanimelist/MainActivity.kt | AppNavigation wiring for SearchRoute. |

## Constraints

- Search results are strictly in-memory and never written to Room database tables.
- Untracked anime opened from search must not insert rows into the animes table unless the user adds the title to their list.
- Adding an anime to Plan to Watch must persist the anime entity before or alongside the user anime list entity to uphold Room foreign key constraints.
- Queries with fewer than 3 characters must not be sent to the MAL API to prevent HTTP 400 Bad Request responses.
- Paginated requests must use request pacing (500ms delay) and exponential backoff retry.
- All strings must be extracted to strings.xml.
- Touch targets must be at least 48.dp, and responsive layouts must adapt to compact and wide windows.

## Verification

- Unit tests in :core:network for search endpoint serialization and error handling.
- Unit tests in :feature:details for ephemeral metadata caching and adding untracked anime to list.
- Unit tests in :feature:search for data source, repository, mapper, use cases, and SearchViewModel state transitions.
- Detekt, KtLint, and Android lint compliance.
