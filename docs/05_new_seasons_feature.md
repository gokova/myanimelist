# Feature 05: New Seasons

| Field | Value |
| --- | --- |
| Status | Implemented; see current operational limitations |
| Primary module | :feature:recommendation |
| Entry surface | RecommendationsRoute, New Seasons selection |
| Related systems | :core:database, :core:network, WorkManager |

## Purpose

Discover narrative continuations and related works for anime in the user's list, enrich them with
MAL metadata, and present them as a third selection in the Recommendations destination.

## User-facing behavior

- The New Seasons selection shows cached suggestions ordered by release date, score, or title.
- It distinguishes insufficient list data, background calculation, a genuine all-caught-up result,
  an error, and populated results.
- Each card identifies the relationship to a watched title and shows the same essential metadata
  as other anime cards.
- Opening Recommendations schedules periodic work and eligible initial work; the header can
  trigger an immediate recalculation. At least five list entries are required.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:recommendation data work | Traverses related MAL anime, resumes progress after retry, and maintains the suggestions cache. |
| :feature:recommendation data | Maps enriched remote details to Room rows and observes New Seasons state. |
| :feature:recommendation domain | Defines NewSeasonAnime, sorting, state, repository contracts, and the NewSeasonsInteractor facade. |
| :feature:recommendation presentation | Integrates New Seasons with the shared recommendation screen, selector, header, and empty states. |
| :core:database | Stores each suggested anime, its parent anime, relation metadata, and creation time. |
| :core:network | Supplies the MAL details request including related-anime fields. |

## Data flow

1. RecommendationViewModel selects New Seasons and uses NewSeasonsInteractor to observe cached
   data, selected sort, and worker state.
2. NewSeasonScheduler enqueues 30-day periodic work or a one-time initial/manual session with an
   explicit session ID and exponential backoff for transient failures.
3. FetchNewSeasonsWorker snapshots the ordered user list in NewSeasonSyncTracker, then requests
   related-anime edges for each root in a bounded batch.
4. The worker uses bounded-depth breadth-first traversal for continuation edges, de-duplicates
   visited IDs, excludes anime already in the user list, and fetches full metadata for candidates.
5. It incrementally upserts suggestions and persists the root cursor. A completed batch returns
   success and appends a continuation work item; only transient failures return retry. The final
   session prunes stale rows. Room updates the screen through NewSeasonRepositoryImpl.
6. FetchCandidatesWorker also excludes New Seasons rows so general recommendations stay focused on
   unrelated discoveries.

## Implementation map

| Path | Responsibility |
| --- | --- |
| feature/recommendation/presentation/RecommendationViewModel.kt | Switches between general and New Seasons streams and routes manual refresh. |
| feature/recommendation/presentation/components/NewSeasonCard.kt | Renders suggestion metadata and its parent-relation badge. |
| feature/recommendation/data/work/FetchNewSeasonsWorker.kt | Related-anime traversal, enrichment, persistence, retry, and stale-result cleanup. |
| feature/recommendation/data/work/NewSeasonScheduler.kt | Creates unique initial, manual, and periodic WorkManager requests. |
| feature/recommendation/data/work/NewSeasonSyncTracker.kt | Persists session identity, root snapshot, cursor, and sync-start time for resumable processing. |
| feature/recommendation/data/repository/NewSeasonRepositoryImpl.kt | Observes suggestions and maps calculation eligibility/state. |
| feature/recommendation/data/mapper/NewSeasonMapper.kt | Maps database relations to NewSeasonAnime. |
| feature/recommendation/domain/usecase/NewSeasonsInteractor.kt | Groups New Seasons observation, scheduling, and manual calculation use cases. |
| core/database/dao/NewSeasonDao.kt | Persists, queries, and prunes New Seasons rows. |

## Constraints

- Narrative relation types are included; recap and non-narrative relation types are excluded.
- Only sequential continuation edges are recursively traversed. Other eligible relationships can
  appear as direct suggestions but do not expand the graph.
- A visited-ID set prevents cycles and the worker paces requests; transient network and server
  failures are retried with WorkManager backoff while a missing anime is permanent for that item.
- Normal batch continuation returns success and is appended as a separate work item with
  `APPEND_OR_REPLACE`; retry is not used as pagination. Each execution is bounded to ten roots and
  fifty candidates per root, but the complete session can still span multiple work items and has no
  global candidate-fetch cap.
- Periodic work requires connectivity and idle time. Current initial and manual one-time requests
  do not add a connectivity constraint, so transient network failures still rely on retry handling.

## Verification

feature/recommendation/src/test covers New Seasons mapping, repository and ViewModel behavior,
FetchNewSeasonsWorker handling, resumable session state, successful batch continuation, transient
retry behavior, stale continuation handling, and the scheduler's work-request configuration. Core
database migration tests cover the persisted New Seasons schema.
