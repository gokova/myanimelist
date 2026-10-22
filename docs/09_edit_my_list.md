# Feature 09: Edit My List Status & Progress

| Attribute | Details |
| :--- | :--- |
| **Status** | Implemented |
| **Primary module** | `:feature:mylist` |
| **Entry surface** | My List screen (inline card action & edit button), Anime Details screen (header status chip & edit action) |
| **Related systems** | `:core:domain`, `:core:database`, `:core:network`, `:core:ui`, `:feature:details` |

---

## 1. Purpose

The Edit My List feature gives users direct, authoritative control over their anime tracking status, watched episode count, numerical score rating (1–10), and rewatching status from within the application. It turns the app into a daily companion for logging watched episodes immediately after viewing and maintaining anime list entries without leaving the app.

---

## 2. User-Facing Behavior

### 2.1 My List Screen Entry Points
- **Inline +1 Episode Increment:** On anime cards with `Watching` status, an inline `+1` button allows quick episode increment with a single tap. A compact progress spinner indicates network mutation; once confirmed by MAL, the card updates. If the increment reaches the final known episode count (`totalEpisodes > 0 && episodesWatched >= totalEpisodes`), a prompt offers to mark the series as `Completed`. For ongoing/airing anime with unknown/zero total episodes (e.g. One Piece), completion is never assumed and no prompt is shown.
- **Edit Sheet Trigger:** Every anime card in My List features a dedicated Edit action (pencil icon / button) that opens the Edit List Status bottom sheet. When saving progress that reaches the final known episode count, a completion prompt is offered. Conversely, if watched episodes are reduced on an anime marked as `Completed`, a prompt offers to mark it back to `Watching`.

### 2.2 Anime Details Entry Points
- When an anime is not in the user list, the primary action remains "Add to List" (adding to `Plan to Watch` with 0 progress).
- Once an anime is in the user list, the header displays its interactive status badge and an Edit button. Tapping either opens the Edit List Status bottom sheet.
- Saving changes in the bottom sheet from the Details screen triggers the same contextual prompts as My List: a snackbar prompt offering "Mark Complete" when reaching the final episode (`episodesWatched >= totalEpisodes`, `totalEpisodes > 0`), and "Mark Watching" when reducing watched episodes on a previously `Completed` anime.

### 2.3 Edit List Status Bottom Sheet
The bottom sheet provides:
- **Header:** Anime title, dismiss (`X`) button, and `Save` button (showing loading spinner while saving).
- **Status Selector:** 5 status choices (`Watching`, `Completed`, `Plan to Watch`, `On Hold`, `Dropped`) styled with semantic container and text colors from `docs/DESIGN.md`. Selecting `Completed` automatically updates watched episodes to total episodes when the total is known (`totalEpisodes > 0`). For ongoing/airing anime with unknown/zero total episodes (e.g. One Piece), watched episodes are preserved unchanged.
- **Progress Stepper & Inline Entry:** `[ - ]` and `[ + ]` stepper controls alongside a compact outlined text box for the watched episode count and total episode indicator (e.g., `[-] [ 74 ] / 74 ep [+]` or `[-] [ 1050 ] / ? ep [+]`). Tapping the number focuses a numeric keypad with all digits selected for instant overwrite, with automatic clamping to total episodes when known, and fallback to `0` if unfocused while empty.
- **Score Selector:** Horizontal selector featuring `Not rated` (`-`) and `10` down to `1`, accompanied by official MAL rating labels (e.g. 10: Masterpiece, 9: Great, 8: Very Good).
- **Rewatching Toggle:** A checkbox indicating "I am rewatching this series".
- **Remove from List:** A destructive action in red with a confirmation `AlertDialog` ("Remove from List?") that calls the MAL DELETE API and removes the entry locally.

---

## 3. Architecture

The feature follows Clean Architecture and feature modularization rules:
- **`:core:network`:** Provides `MalApiService` PUT and DELETE endpoints for `/v2/anime/{anime_id}/my_list_status`.
- **`:core:database`:** Provides Room operations in `UserAnimeListDao` for upserting and deleting `UserAnimeListEntity`, and houses `UserAnimeListSyncCoordinator` to serialize full list sync, mutations, and details refresh writes across features.
- **`:core:domain`:** Declares shared domain models (`UpdateUserAnimeStatusParams`, `UserAnimeListUpdate`), repository interface `UserAnimeListStatusRepository`, and use cases `UpdateAnimeStatusUseCase` and `DeleteAnimeFromListUseCase`.
- **`:feature:mylist`:** Owns user list data operations. Houses `UserAnimeListStatusRepositoryImpl` bound via Hilt.
- **`:core:ui`:** Hosts the reusable `EditAnimeStatusBottomSheet` component, ensuring uniform design and zero code duplication between `:feature:mylist` and `:feature:details`.
- **`:feature:details`:** Injects domain use cases from `:core:domain` to update or remove list entries directly from the details screen, and coordinates fresh details refreshes via `UserAnimeListSyncCoordinator`.

---

## 4. Data Flow

```
User initiates edit (inline +1 or bottom sheet save)
        ↓
ViewModel invokes UpdateAnimeStatusUseCase
        ↓
UserAnimeListStatusRepositoryImpl acquires UserAnimeListSyncCoordinator lock & sends PUT
        ↓
MAL confirms update with MyListStatusDto
        ↓
Room user_anime_list table is updated atomically via UserAnimeListDao
        ↓
ObserveUserAnimeListUseCase emits updated list to My List screen
        ↓
Fresh details cache & observed local state update Anime Details screen
```

For removal:
```
User confirms 'Remove from list' dialog
        ↓
ViewModel invokes DeleteAnimeFromListUseCase
        ↓
UserAnimeListStatusRepositoryImpl acquires coordinator lock & sends DELETE to MAL API (200 OK / 404 ignored)
        ↓
Room user_anime_list row deleted for anime_id
        ↓
Screens reactively update (item leaves My List; Details returns to 'Add to List')
```

---

## 5. Implementation Map

| File Path | Responsibility |
| :--- | :--- |
| `core/network/api/MalApiService.kt` | PUT and DELETE endpoints for `/v2/anime/{anime_id}/my_list_status` |
| `core/database/dao/UserAnimeListDao.kt` | Deletion query `deleteUserAnime(animeId)` |
| `core/database/util/UserAnimeListSyncCoordinator.kt` | Mutex coordinator serializing list sync, mutations, and details writes |
| `core/domain/model/UpdateUserAnimeStatusParams.kt` | Mutation parameter domain model |
| `core/domain/model/UserAnimeListUpdate.kt` | Confirmed update domain model |
| `core/domain/repository/UserAnimeListStatusRepository.kt` | Shared repository interface |
| `core/domain/usecase/UpdateAnimeStatusUseCase.kt` | Use case for partial/full list updates |
| `core/domain/usecase/DeleteAnimeFromListUseCase.kt` | Use case for deleting anime from list |
| `core/ui/component/editlist/EditAnimeStatusBottomSheet.kt` | Reusable Compose bottom sheet UI |
| `feature/mylist/data/repository/UserAnimeListStatusRepositoryImpl.kt` | Authoritative MAL mutation and Room sync |
| `feature/mylist/presentation/MyListViewModel.kt` | My List state management for inline +1 and edit sheet |
| `feature/mylist/presentation/components/AnimeCard.kt` | Card UI with inline +1 and Edit button |
| `feature/details/data/repository/AnimeDetailsRepositoryImpl.kt` | Coordinated fresh details refresh with stale response rejection |
| `feature/details/presentation/AnimeDetailsViewModel.kt` | Details state management for edit sheet and deletion |
| `feature/details/presentation/components/AnimeDetailsHeader.kt` | Header UI with interactive status chip and edit button |

---

## 6. Constraints

- **Authoritative Online Mutations:** Mutations are confirmed against the MAL API before committing to Room; no offline queuing with unresolved conflicts.
- **Cross-Feature Mutation & Refresh Serialization:** Full list sync (`MyListRepositoryImpl`), mutations (`UserAnimeListStatusRepositoryImpl`), and details refreshes (`AnimeDetailsRepositoryImpl`) are serialized via `UserAnimeListSyncCoordinator` in `:core:database`.
- **Stale Response Rejection:** Details refresh rejects remote `myListStatus` payloads whose `updated_at` timestamp is older than local Room records, preventing stale network snapshots from overwriting confirmed local edits or resurrecting deleted entries.
- **Selective Parameter Transmission:** Retrofit form encoding omits `null` parameters to prevent accidental clearing of existing user progress or scores.
- **Clamped Episode Range:** Episode progress cannot be negative and cannot exceed `num_episodes` when the total count is known.
- **Detekt & Formatting:** Composables must not exceed 60 lines, files must not exceed 10 functions, parameter lists must not exceed 6 arguments, and lines must not exceed 100 characters.

---

## 7. Verification

- `UserAnimeListStatusRepositoryImplTest`: Covers partial field updates, score zeroing, deletion handling, and error propagation.
- `AnimeDetailsRepositoryImplTest`: Covers coordinated fresh details fetch, stale remote status rejection, newer status acceptance, and atomic addition to Plan to Watch.
- `UpdateAnimeStatusUseCaseTest` & `DeleteAnimeFromListUseCaseTest`: Verifies business delegation and result passing.
- `MyListViewModelTest`: Verifies inline +1, loading states, edit sheet workflows, final episode prompt, and deletion.
- `AnimeDetailsViewModelTest`: Verifies edit sheet opening, saving, and deletion cache updates.
- `./gradlew test` (100% pass)
- `./gradlew detekt` (0 violations)
- `./gradlew ktlintCheck` (100% compliant)
- `./gradlew lintDebug` (0 NewApi or critical lint errors)
- `git diff --check` (0 trailing whitespace or format issues)
