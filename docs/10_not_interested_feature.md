# Feature 10: Not Interested / Ignore Feature

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:recommendation, :feature:details |
| Entry surface | Recommendations & New Seasons card long-press, Anime Details TopAppBar |
| Related systems | :core:database, :core:domain, :core:ui, WorkManager |

## Purpose

Allow users to mark anime they are not interested in, removing them from discovery surfaces
(Recommendations and New Seasons) while preserving the ability to discover them via search,
revisit them via direct navigation, or reverse the decision via an immediate Undo or the Details page.

## User-facing behavior

- **Recommendations & New Seasons Lists:**
  - Long-pressing any card in Genres, Themes, or New Seasons displays a focused modal bottom sheet
    displaying the anime poster, title, and a "Not Interested" action.
  - Tapping "Not Interested" closes the sheet, immediately hides the anime from the list, and triggers
    a bottom Snackbar: "Marked as Not Interested" with an "Undo" action.
  - Tapping "Undo" in the Snackbar un-ignores the anime, restoring it immediately to the list.
- **Anime Details Screen:**
  - For any anime not currently in the user's MAL list, the Top App Bar includes an eye-off icon
    button that toggles the "Not Interested" status.
  - When marked as Not Interested, a distinct "Not Interested" badge chip is displayed in the header
    section below the score badges.
  - Tapping the Top App Bar icon toggles the state between ignored and unignored, showing immediate
    visual feedback and a confirmation Snackbar.
  - Tapping "+ Add to List" on an ignored anime automatically removes it from the ignored list,
    updates its status to "Plan to Watch" on MAL and Room, and updates the UI accordingly.
- **Search Screen:**
  - Search results from MAL continue to display all matching titles without filtering.
  - Tapping an ignored anime from search navigates to Anime Details, where its "Not Interested" badge
    and unignore toggle are displayed.
- **Subsequent Seasons Traversal:**
  - If a specific continuation (e.g., Season 2 or an OVA) is ignored, only that specific title is
    hidden. Traversal continues to discover subsequent seasons (e.g., Season 3 or 4) so future
    installments remain discoverable unless also individually ignored.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :core:database | `IgnoredAnimeEntity`, `IgnoredAnimeDao`, Room migration 5 to 6, and orphan cleanup protection. |
| :core:domain | `IgnoredAnime` model, repository interface, and use cases for setting, removing, and observing ignored status. |
| :feature:recommendation | Long-press bottom sheet, card gestures, ViewModel event handling, and Worker exclusion filters. |
| :feature:details | Top App Bar toggle icon, header "Not Interested" badge chip, and auto-unignore integration on "+ Add to List". |
| :core:ui | Shared icons, theme tokens, and accessibility semantics adhering to `docs/DESIGN.md`. |

## Data flow

1. **Marking as Not Interested (List):**
   - User long-presses a recommendation or new season card, opening `NotInterestedBottomSheet`.
   - User confirms "Not Interested".
   - `RecommendationViewModel` invokes `SetAnimeNotInterestedUseCase(animeId, title, titleEnglish, pictureUrl)`.
   - `IgnoredAnimeDao` inserts the record into `ignored_animes`.
   - Reactive Room queries in `RecommendationDao` and `NewSeasonDao` (`WHERE anime_id NOT IN (SELECT anime_id FROM ignored_animes)`)
     immediately emit updated lists without the ignored item.
   - A Snackbar with an "Undo" button is displayed.
2. **Undoing Action:**
   - User taps "Undo" in the Snackbar.
   - `RecommendationViewModel` invokes `RemoveAnimeFromNotInterestedUseCase(animeId)`.
   - `IgnoredAnimeDao` removes the record; Room queries automatically re-emit the item in its original position.
3. **Details Screen Observation & Toggle:**
   - `AnimeDetailsViewModel` observes `ObserveIsAnimeNotInterestedUseCase(animeId)`.
   - UI reflects the status via the Top App Bar icon and the header badge.
   - Tapping the icon calls `ToggleNotInterestedUseCase` or the respective set/remove use cases.
4. **Add to List Auto-Unignore:**
   - When `AddAnimeToMyListUseCase` is invoked, it updates MAL and calls the database transaction
     that inserts the user anime list entry and removes any matching row from `ignored_animes`.
5. **Background Sync Exclusion:**
   - `FetchCandidatesWorker` excludes IDs from `recommendationDao.getExcludedCandidateAnimeIds()`,
     which includes `UNION SELECT anime_id FROM ignored_animes`.
   - `FetchNewSeasonsWorker` skips persisting ignored IDs into `new_season_animes`, but permits
     continuing graph traversal across continuation edges.

## Implementation map

| Path | Responsibility |
| --- | --- |
| core/database/entity/IgnoredAnimeEntity.kt | Room entity storing anime ID, titles, image URL, and creation timestamp. |
| core/database/dao/IgnoredAnimeDao.kt | DAO for inserting, deleting, querying, and observing ignored anime. |
| core/database/AppDatabase.kt | Room database definition incremented to version 6 with `MIGRATION_5_6`. |
| core/database/dao/RecommendationDao.kt | Updated queries filtering out ignored anime and excluding them from candidate sets. |
| core/database/dao/NewSeasonDao.kt | Updated queries filtering out ignored anime and protecting them during orphan pruning. |
| core/domain/model/IgnoredAnime.kt | Platform-neutral domain representation of an ignored anime. |
| core/domain/repository/IgnoredAnimeRepository.kt | Domain repository contract for ignored anime operations. |
| core/domain/usecase/SetAnimeNotInterestedUseCase.kt | Use case to persist an anime as ignored. |
| core/domain/usecase/RemoveAnimeFromNotInterestedUseCase.kt | Use case to remove an anime from the ignored list. |
| core/domain/usecase/ObserveIsAnimeNotInterestedUseCase.kt | Use case to observe whether an anime ID is currently ignored. |
| feature/recommendation/presentation/components/NotInterestedBottomSheet.kt | Modal bottom sheet with anime preview and "Not Interested" action. |
| feature/recommendation/presentation/RecommendationScreen.kt | Gesture wiring (combinedClickable / onLongClick), sheet hosting, and Snackbar with Undo. |
| feature/recommendation/presentation/RecommendationViewModel.kt | Action handling for ignoring and undoing recommendations or new seasons. |
| feature/recommendation/data/work/FetchCandidatesWorker.kt | Candidate exclusion ensuring ignored anime is never added to candidate pool. |
| feature/recommendation/data/work/FetchNewSeasonsWorker.kt | Continuation traversal that hides ignored items while maintaining recursive search. |
| feature/details/presentation/AnimeDetailsScreen.kt | Top App Bar eye-off toggle icon button and event forwarding. |
| feature/details/presentation/components/AnimeDetailsHeader.kt | Header "Not Interested" badge chip rendering below score chips. |
| feature/details/domain/usecase/AddAnimeToMyListUseCase.kt | Unifies list insertion with clearing ignored status. |

## Constraints

- Ignored items must never be cleared during MAL account sync or candidate cache pruning.
- The `ignored_animes` table is self-contained with snapshot titles and poster URL to support
  the future profile screen list without requiring active cache rows in `animes`.
- Room queries must dynamically exclude ignored anime so list removal and undo operations are
  instantaneous without requiring network calls or full recalculations.
- Adding an anime to the user list automatically clears its ignored status.|
- Minimum touch targets (\(\ge 48.dp\)) and WCAG AA contrast must be preserved across both
  light and dark themes for the bottom sheet, badges, and toggle icons.

## Verification

- Migration unit test for `MIGRATION_5_6` validating SQLite schema creation and data retention.
- DAO tests for `IgnoredAnimeDao`, `RecommendationDao`, and `NewSeasonDao` verifying query exclusions.
- Use case and repository unit tests for set, remove, and observe operations.
- `RecommendationViewModelTest` verifying long-press ignore, immediate list update, and Snackbar undo.
- `AnimeDetailsViewModelTest` verifying Top App Bar toggle, header badge state, and Add to List auto-unignore.
- Static analysis checks: `./gradlew detekt`, `./gradlew ktlintCheck`, and `./gradlew test`.
