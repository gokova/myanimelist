# Feature 06: Anime Details Page & List Integration

## Overview
The Anime Details feature provides a dedicated, full-screen, offline-first view displaying comprehensive metadata, synopsis, taxonomy tags (genres and themes), ratings, related works, and community recommendations for any anime. It also enables one-tap addition of unlisted anime directly to the user's personal list (`plan_to_watch`), automatically cleaning up corresponding recommendation or new season entries from local storage.

The page is seamlessly accessible from any anime listing throughout the app:
- My List (`MyListScreen`)
- Taste Drill-down (`TasteBottomSheet`)
- Recommendation Engine (`RecommendationScreen` - Genres & Themes)
- New Seasons (`RecommendationScreen` - New Seasons)
- Related Anime & Recommendations carousels within the Details page itself

---

## Key Requirements & Agreed Design

1. **Navigation**:
   - Registered as `AnimeDetailsRoute(val animeId: Long)` in the root navigation graph (`MainActivity`).
   - Renders as a full-screen destination over the bottom navigation bar/rail.
   - Features a Material 3 `TopAppBar` with back navigation and anime title.
   - Supports forward chaining: tapping any related anime or recommendation pushes a new `AnimeDetailsRoute` onto the backstack.

2. **Offline-First Data Strategy**:
   - Instantly loads existing cached data from Room (`animes` and `user_anime_list` tables).
   - Shows a subtle refreshing indicator while fetching fresh details from `GET /v2/anime/{anime_id}?fields=...`.
   - On success: upserts latest core metadata into `AnimeEntity` in Room and updates the UI state with rich dynamic relations (related anime and recommendations).
   - On failure: retains cached data and displays a non-blocking snackbar notification. If offline and no local record exists, displays an empty/error state with a Retry action.

3. **"Add to List" Integration**:
   - If the anime is not in the user's list: displays a prominent primary CTA button ("Add to List").
   - Tapping invokes `PUT /v2/anime/{anime_id}/my_list_status` with `status=plan_to_watch`, `num_watched_episodes=0`, `score=0`.
   - On success:
     - Persists the new entry in `user_anime_list` Room table.
     - Automatically removes the anime record from `recommendations` and `new_season_animes` Room tables.
     - Displays a confirmation Snackbar.
     - Transitions the button into the theme-styled status chip ("Plan to Watch").

4. **Taxonomy & Metadata Presentation**:
   - Splits tags into **Genres** and **Themes** using `AnimeTaxonomy` (`GENRE_NAMES` and `THEME_NAMES`).
   - Displays English title as main title and Japanese/Romaji title as subtitle when distinct.
   - Displays community score (with star badge) and, if in list, the user's personal score.
   - Shows rank, popularity, scoring users, and total list users.
   - Conditionally displays episode counts and formatted episode duration (omitted or adapted for movies/music).
   - Renders "Related Anime" and "Recommendations" in horizontal scrolling carousels (`LazyRow`) with 2:3 posters, relation tags, and scores.

5. **List Item Interaction**:
   - Tapping an anime card body navigates to `AnimeDetailsRoute`.
   - Tapping the poster thumbnail continues to trigger `AnimePosterPreviewDialog` (zoom preview).

---

## Architectural Breakdown

```
:feature:details/
├── data/
│   ├── mapper/
│   │   └── AnimeDetailsMapper.kt
│   └── repository/
│       └── AnimeDetailsRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── AnimeDetails.kt
│   │   ├── RelatedAnime.kt
│   │   └── RecommendedAnimeItem.kt
│   ├── repository/
│   │   └── AnimeDetailsRepository.kt
│   └── usecase/
│       ├── GetAnimeDetailsUseCase.kt
│       └── AddAnimeToMyListUseCase.kt
├── presentation/
│   ├── AnimeDetailsScreen.kt
│   ├── AnimeDetailsViewModel.kt
│   ├── AnimeDetailsUiState.kt
│   ├── AnimeDetailsUiEvent.kt
│   └── components/
│       ├── AnimeDetailsHeader.kt
│       ├── AnimeDetailsMetadata.kt
│       ├── AnimeDetailsSynopsis.kt
│       ├── AnimeDetailsTaxonomy.kt
│       ├── RelatedAnimeCarousel.kt
│       └── DetailRecommendationsCarousel.kt
└── di/
    └── DetailsModule.kt

:core:network/
├── api/
│   └── MalApiService.kt (updateMyListStatus, updated anime details fields)
└── model/
    └── MalAnimeListDtos.kt (recommendations in details, num_scoring_users)

:core:database/
├── dao/
│   ├── RecommendationDao.kt (deleteRecommendation)
│   └── UserAnimeListDao.kt (getUserAnimeItemById)
```
