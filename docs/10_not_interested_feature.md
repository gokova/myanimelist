# Feature 10: Not Interested / Ignore Feature

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:recommendation, :feature:details, :feature:profile |
| Entry surface | Recommendations & New Seasons card long-press, Anime Details TopAppBar, Profile Screen |
| Related systems | :core:database, :core:domain, :core:ui, WorkManager, Storage Access Framework |

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
- **Profile Screen Export & Import (Backup & Restore):**
  - A dedicated "Ignored Anime" section card is displayed between the Status Distribution section and the Log Out button.
  - Displays the total count of currently ignored anime (e.g., "12 anime").
  - Provides side-by-side "Export" and "Import" buttons styled according to `docs/DESIGN.md`.
  - The "Export" button is disabled when the ignored anime count is zero.
  - Tapping "Export" initiates the Android Storage Access Framework (SAF) `CreateDocument` picker with suggested filename `mal_ignored_anime_<date>.csv` (`text/csv`).
  - Upon selecting a destination, exports all ignored anime into a standard RFC 4180 CSV file with proper escaping of quotes, commas, and newlines.
  - Tapping "Import" initiates the SAF `OpenDocument` picker filtering for CSV/text files.
  - Upon selecting a file, parses RFC 4180 CSV tolerantly (header-aware). Performs a Merge & Upsert into Room: existing items not present in the CSV remain intact, new items are inserted, and matching IDs are updated.
  - Shows feedback via Snackbar:
    - Export success: "Exported X anime to CSV".
    - Import success: "Imported X anime (Y new, Z updated)".
    - Error states: Clear descriptive Snackbar messages (e.g., "Failed to export CSV", "Failed to import CSV: Invalid format").

## Architecture

| Layer | Responsibility |
| --- | --- |
| :core:database | `IgnoredAnimeEntity`, `IgnoredAnimeDao`, Room migration 5 to 6, batch upsert, and orphan cleanup protection. |
| :core:domain | `IgnoredAnime` model, repository interface, and use cases for setting, removing, observing, exporting, and importing ignored anime via RFC 4180 CSV format. |
| :feature:recommendation | Long-press bottom sheet, card gestures, ViewModel event handling, and Worker exclusion filters. |
| :feature:details | Top App Bar toggle icon, header "Not Interested" badge chip, and auto-unignore integration on "+ Add to List". |
| :feature:profile | Dedicated "Ignored Anime" section card, SAF launcher integration (`CreateDocument` / `OpenDocument`), reactive count observation, and ViewModel event orchestration. |
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
6. **Exporting Ignored Anime to CSV (Profile Screen):**
   - User taps "Export" on the Profile Screen.
   - Profile Screen triggers `CreateDocument("text/csv")` with default filename `mal_ignored_anime_<date>.csv`.
   - On Uri selected, `ProfileViewModel` executes `ExportIgnoredAnimeUseCase`.
   - `ExportIgnoredAnimeUseCase` queries all ignored anime from `IgnoredAnimeRepository`, serializes them to RFC 4180 CSV (header: `anime_id,title,title_english,picture_url,created_at`, quoting commas/newlines/quotes and doubling `""`), and writes content to the selected `Uri` via stream.
   - Success Snackbar displays count of exported anime.
7. **Importing Ignored Anime from CSV (Profile Screen):**
   - User taps "Import" on the Profile Screen.
   - Profile Screen triggers `OpenDocument()` for CSV MIME types.
   - On Uri selected, `ProfileViewModel` executes `ImportIgnoredAnimeUseCase`.
   - `ImportIgnoredAnimeUseCase` parses RFC 4180 CSV rows tolerantly (stripping UTF-8 BOM, validating quote placement, skipping individual malformed lines without discarding the entire dataset, matching headers, defaulting missing `created_at` to current timestamp, null for optional titles/posters). Only fails with `InvalidFormat` if the header or all rows are malformed.
   - `IgnoredAnimeRepository` executes batch merge & upsert in Room (`upsertIgnoredAnimes`). Existing rows not in CSV are preserved.
   - Calculates count of new and updated records, returning `ImportIgnoredAnimeResult.Success(newCount, updatedCount)`.
   - Success Snackbar displays: "Imported X anime (Y new, Z updated)". If file is corrupt or empty, error Snackbar is displayed.

## Implementation map

| Path | Responsibility |
| --- | --- |
| core/database/entity/IgnoredAnimeEntity.kt | Room entity storing anime ID, titles, image URL, and creation timestamp. |
| core/database/dao/IgnoredAnimeDao.kt | DAO for inserting, deleting, batch upserting, querying, and observing ignored anime. |
| core/database/AppDatabase.kt | Room database definition incremented to version 6 with `MIGRATION_5_6`. |
| core/database/dao/RecommendationDao.kt | Updated queries filtering out ignored anime and excluding them from candidate sets. |
| core/database/dao/NewSeasonDao.kt | Updated queries filtering out ignored anime and protecting them during orphan pruning. |
| core/domain/model/IgnoredAnime.kt | Platform-neutral domain representation of an ignored anime. |
| core/domain/model/ImportIgnoredAnimeResult.kt | Domain result model representing import statistics (total, new, updated) or failure. |
| core/domain/repository/IgnoredAnimeRepository.kt | Domain repository contract for ignored anime operations including batch upsert and full list query. |
| core/domain/usecase/SetAnimeNotInterestedUseCase.kt | Use case to persist an anime as ignored. |
| core/domain/usecase/RemoveAnimeFromNotInterestedUseCase.kt | Use case to remove an anime from the ignored list. |
| core/domain/usecase/ObserveIsAnimeNotInterestedUseCase.kt | Use case to observe whether an anime ID is currently ignored. |
| core/domain/usecase/ObserveIgnoredAnimeCountUseCase.kt | Use case to observe the total count of ignored anime for the Profile screen. |
| core/domain/usecase/ExportIgnoredAnimeUseCase.kt | Use case to format and export ignored anime list into RFC 4180 compliant CSV stream. |
| core/domain/usecase/ImportIgnoredAnimeUseCase.kt | Use case to parse RFC 4180 CSV stream and merge/upsert records into database. |
| feature/recommendation/presentation/components/NotInterestedBottomSheet.kt | Modal bottom sheet with anime preview and "Not Interested" action. |
| feature/recommendation/presentation/RecommendationScreen.kt | Gesture wiring (combinedClickable / onLongClick), sheet hosting, and Snackbar with Undo. |
| feature/recommendation/presentation/RecommendationViewModel.kt | Action handling for ignoring and undoing recommendations or new seasons. |
| feature/recommendation/data/work/FetchCandidatesWorker.kt | Candidate exclusion ensuring ignored anime is never added to candidate pool. |
| feature/recommendation/data/work/FetchNewSeasonsWorker.kt | Continuation traversal that hides ignored items while maintaining recursive search. |
| feature/details/presentation/AnimeDetailsScreen.kt | Top App Bar eye-off toggle icon button and event forwarding. |
| feature/details/presentation/components/AnimeDetailsHeader.kt | Header "Not Interested" badge chip rendering below score chips. |
| feature/details/domain/usecase/AddAnimeToMyListUseCase.kt | Unifies list insertion with clearing ignored status. |
| feature/profile/presentation/components/IgnoredAnimeSection.kt | Dedicated Profile card showing ignored count and Export/Import buttons. |
| feature/profile/presentation/ProfileScreen.kt | Hosts `IgnoredAnimeSection` and wires Activity Result SAF launchers for CSV file picking. |
| feature/profile/presentation/ProfileViewModel.kt | Manages ignored count state flow, export/import actions, and feedback event propagation. |
| feature/profile/presentation/ProfileUiState.kt | Updated to include ignored anime count. |
| feature/profile/presentation/ProfileActions.kt | Updated with `onExportClick` and `onImportClick` handler callbacks. |
| feature/profile/presentation/ProfileUiEvent.kt | Updated with export and import result Snackbar events. |

## Constraints

- Ignored items must never be cleared during MAL account sync or candidate cache pruning.
- The `ignored_animes` table is self-contained with snapshot titles and poster URL to support
  the profile screen list without requiring active cache rows in `animes`.
- Room queries must dynamically exclude ignored anime so list removal and undo operations are
  instantaneous without requiring network calls or full recalculations.
- Adding an anime to the user list automatically clears its ignored status.
- **RFC 4180 CSV Compliance:** Fields containing commas, quotation marks, or line breaks must be enclosed in double quotes. Internal quotes must be escaped by doubling (`""`). Pure Kotlin domain parsing/formatting without Android SDK dependencies to guarantee unit testability.
- **Tolerant Schema Parsing:** Header row is expected (`anime_id,title,title_english,picture_url,created_at`), but missing optional columns or extra whitespace must be tolerated gracefully, defaulting `created_at` to the current epoch timestamp and optional titles/posters to null.
- **Merge & Upsert Integrity:** Importing must never delete or overwrite unreferenced existing ignored anime. Only matching IDs are updated; non-matching IDs are inserted.
- **Storage Access Framework (SAF):** File access must use `ActivityResultContracts.CreateDocument` and `ActivityResultContracts.OpenDocument` to remain fully compliant with Scoped Storage across modern Android versions (API 24 to 37) without requiring legacy storage permissions.
- Minimum touch targets (\(\ge 48.dp\)) and WCAG AA contrast must be preserved across both
  light and dark themes for all buttons, bottom sheets, badges, and toggle icons adhering to `docs/DESIGN.md`.

## Verification

- Migration unit test for `MIGRATION_5_6` validating SQLite schema creation and data retention.
- DAO tests for `IgnoredAnimeDao`, `RecommendationDao`, and `NewSeasonDao` verifying query exclusions and batch upserts.
- Use case and repository unit tests for set, remove, observe, export, and import operations.
- CSV parser and generator unit tests covering edge cases: commas in titles, embedded quotes, newlines, missing optional columns, and malformed rows.
- `RecommendationViewModelTest` verifying long-press ignore, immediate list update, and Snackbar undo.
- `AnimeDetailsViewModelTest` verifying Top App Bar toggle, header badge state, and Add to List auto-unignore.
- `ProfileViewModelTest` verifying ignored anime count observation, export generation, import merge counts, and error event emission.
- Static analysis checks: `./gradlew detekt`, `./gradlew ktlintCheck`, and `./gradlew test`.
