# Feature 14: Offline Updates and Synchronization

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:mylist |
| Entry surface | My List screen (cards, banner, sync sheet), Anime Details screen (header, edit sheet) |
| Related systems | :core:database, :core:domain, :core:network, :core:ui, :feature:details, :feature:profile |

## Purpose

Enable users to update their anime progress, status, score, and rewatching state immediately while
offline. Edits are durably saved on-device with zero perceived UI latency, coalesced per anime in an
outbox, and automatically synchronized with the MyAnimeList (MAL) API when connectivity allows,
safeguarding local edits against remote wipes and resolving remote conflicts transparently.

## User-facing behavior

- **Snappy Local Edits**: Tapping quick +1 on watching cards or saving changes from the Edit List
  bottom sheet updates the UI instantly, regardless of network connectivity.
- **Sync Status Badges**: Anime cards and details headers reflect sync status:
  - *Pending*: A subtle clock/pending badge indicates changes are queued locally.
  - *Syncing*: A subtle progress indicator indicates active transmission.
  - *Conflict*: A distinct conflict indicator flags that progress changed remotely on MAL.
  - *Synced*: Standard card appearance with no extra badges.
- **Pending Updates & Conflict Banner**: When updates are waiting to sync or require attention, a
  tonal summary banner appears on My List (e.g., *"3 updates pending sync"* or *"1 update needs
  review"*). Tapping the banner opens the Pending Updates & Conflicts bottom sheet.
- **Interactive Conflict Resolution**: If an anime's field was modified on MAL while the device was
  offline, a 3-way conflict sheet displays the last synced value, current device value, and remote
  MAL value, with explicit actions: **[Keep Device]**, **[Use MAL]**, or **[Edit]**. Non-conflicting
  fields auto-merge without interrupting the user.
- **Resilient Full-List Refresh**: Pull-to-refresh and background list sync do not wipe or overwrite
  pending local edits. If MAL already reflects the pending desired state, the mutation is marked
  synced automatically.
- **Session Boundary Warnings**: The logout confirmation dialog warns if unsynced edits exist,
  preventing accidental data loss. Confirmed logout clears local pending mutations. When auth expires
  (HTTP 401), pending mutations pause with an "Auth Required" notice until re-login.

## Architecture

| Layer | Responsibility |
| --- | --- |
| `:core:database` | Stores `pending_user_anime_mutations` outbox table, coordinates atomic optimistic writes to `user_anime_list`, and provides selective merge during list sync. |
| `:core:domain` | Defines `PendingAnimeMutation`, sync mutation states, conflict models, and use cases for observing, syncing, resolving conflicts, and discarding mutations. |
| `:core:network` | Executes authenticated MAL PUT requests and pre-write status verification queries. |
| `:feature:mylist` | Implements `SyncPendingMutationsWorker` via WorkManager, manages outbox mutations, handles UI banners, cards, and conflict resolution sheets. |
| `:core:ui` | Provides reusable conflict resolution and pending status UI components conforming to the Soft & Modern design system. |
| `:feature:details` | Updates details header badges and coordinates details refreshes without clobbering pending mutations. |
| `:feature:profile` | Queries pending mutation counts to display warning copy in the logout dialog. |

## Data flow

1. **User Edit**:
   - User triggers +1 increment or saves the Edit List bottom sheet.
   - `UpdateAnimeStatusUseCase` executes an atomic Room transaction:
     - Optimistically updates `user_anime_list` so UI updates instantly.
     - Inserts or coalesces the mutation in `pending_user_anime_mutations` (preserving the original baseline).
2. **Dual-Path Sync Dispatch**:
   - **Fast Path**: Repository attempts an immediate network dispatch in the background coroutine.
     - On HTTP 200: outbox entry is deleted, local row updated with remote timestamp.
   - **Background Fallback**: If offline or transient network error occurs:
     - Mutation remains in `PENDING` state.
     - Unique one-time `SyncPendingMutationsWorker` is enqueued with `NetworkType.CONNECTED` constraints and exponential backoff.
3. **Remote Conflict Verification**:
   - Worker queries MAL for current anime status.
   - Compares: baseline values vs. local desired values vs. MAL remote values.
   - If no conflict: sends PUT to MAL, commits confirmed state, removes outbox entry.
   - If conflict on same field: marks outbox entry as `CONFLICT`.
4. **Interactive Conflict Resolution**:
   - User opens conflict sheet from card or banner.
   - User selects [Keep Device] (forces overwrite to MAL), [Use MAL] (discards pending mutation and takes remote values), or [Edit] (opens stepper/sheet).
5. **Full List Refresh Coexistence**:
   - `syncUserAnimeList` selectively merges incoming MAL list: non-pending rows update normally, pending rows retain local optimistic values.

## Implementation map

| Path | Responsibility |
| --- | --- |
| `core/database/entity/PendingUserAnimeMutationEntity.kt` | Room entity representing the outbox record per anime. |
| `core/database/dao/PendingUserAnimeMutationDao.kt` | DAO for querying, coalescing, and updating outbox entries. |
| `core/database/AppDatabase.kt` | Database version 8 and `MIGRATION_7_8` for outbox table creation. |
| `core/database/dao/UserAnimeListDao.kt` | Selective merge list sync preserving pending rows. |
| `core/domain/model/PendingAnimeMutation.kt` | Domain model for pending mutation and sync state enum. |
| `core/domain/model/AnimeConflictDetails.kt` | Domain model holding 3-way conflict diff values. |
| `core/domain/repository/PendingMutationRepository.kt` | Repository interface for observing and managing outbox mutations. |
| `core/domain/usecase/ObservePendingMutationsUseCase.kt` | Flow of active pending mutations and conflict count. |
| `core/domain/usecase/SyncPendingMutationsUseCase.kt` | Dispatches pending mutations and detects conflicts. |
| `core/domain/usecase/ResolveAnimeConflictUseCase.kt` | Applies user conflict resolution decision. |
| `feature/mylist/data/work/SyncPendingMutationsWorker.kt` | Background WorkManager worker with network constraints. |
| `feature/mylist/data/repository/PendingMutationRepositoryImpl.kt` | Outbox coordinator and network sync logic. |
| `feature/mylist/presentation/components/PendingSyncBanner.kt` | Summary banner on My List screen for pending/conflict items. |
| `core/ui/component/conflict/ConflictResolutionBottomSheet.kt` | 3-way conflict comparison and resolution bottom sheet. |
| `feature/mylist/presentation/components/AnimeCard.kt` | Card badge displaying pending/syncing/conflict state. |
| `feature/profile/presentation/ProfileViewModel.kt` | Queries pending count to populate logout confirmation warning. |

## Constraints

- **Single Record Per Anime Outbox Coalescing**: Multiple rapid edits on the same anime coalesce into one pending record while preserving the original baseline from the first edit.
- **Non-Destructive Room Migrations**: Database version 7 -> 8 migration creates the outbox table cleanly without dropping or altering existing user list data.
- **Detekt & Design Rules**: All new composables must not exceed 60 lines, files must not exceed 10 functions, and parameters must not exceed 6 arguments. All text must be localized via `strings.xml`.
- **Zero Leakage**: Outbox entities and Room models must not escape into the Presentation layer.

## Verification

- `PendingUserAnimeMutationDaoTest`: Verifies outbox insertion, coalescing, status updates, and deletions.
- `UserAnimeListDaoSyncTest`: Verifies selective list merge during full sync preserving pending mutations.
- `PendingMutationRepositoryImplTest`: Tests fast path, conflict detection, auto-merging non-conflicting fields, and error handling.
- `SyncPendingMutationsWorkerTest`: Tests WorkManager execution and retry constraints.
- `MyListViewModelTest`: Verifies pending banner emissions, conflict resolution triggers, and quick +1 offline flow.
- `ProfileViewModelTest`: Verifies logout warning display when pending mutations exist.
- Full verification: `./gradlew test`, `./gradlew detekt`, `./gradlew ktlintCheck`, `./gradlew lintDebug`.
