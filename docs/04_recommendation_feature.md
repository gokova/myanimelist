# Feature 04: Smart Recommendations

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:recommendation |
| Entry surface | RecommendationsRoute, Genres and Themes selections |
| Related systems | :core:database, :core:domain, :core:network, WorkManager |

## Purpose

Generate genre- and theme-based recommendations from the user's MAL history, cache the ranked
results locally, and expose them through the Recommendations destination. New Seasons is a
separate capability documented in Feature 05, though it shares this destination.

## User-facing behavior

- The Recommendations screen presents Genres and Themes rankings, cards, match percentages, and
  localized result counts. It retains distinct insufficient-data, calculating, error, and ready
  states.
- At least five user-list entries are required before recommendation work can produce results.
- Opening the destination schedules periodic work and, when appropriate, initial calculation.
  The user can explicitly recalculate without waiting for the background cadence.
- Results are offline-first Room observations; background work updates the screen when it
  completes.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:recommendation data remote | Fetches top-ranked and seasonal MAL candidates. |
| :feature:recommendation data work | Fetches candidates, evaluates them, and schedules initial, periodic, or manual runs. |
| :feature:recommendation domain | Scores candidates and exposes observed results and engine state. |
| :feature:recommendation presentation | Selects recommendation type and maps domain state to one immutable screen state. |
| :core:database | Stores candidate IDs and ranked recommendation rows joined to cached anime metadata. |
| :core:domain taxonomy | Keeps genre/theme partitioning consistent with Taste and Details. |

## Data flow

1. RecommendationViewModel schedules recommendation work and observes the selected Genres or
   Themes data stream.
2. RecommendationScheduler enqueues FetchCandidatesWorker for first-run, periodic (90-day), or
   user-requested calculations.
3. FetchCandidatesWorker retrieves ranked and seasonal candidates, excludes anime already in the
   user's list or New Seasons cache, then persists candidate IDs and metadata.
4. EvaluateRecommendationsWorker obtains the candidate and user-list sets, runs
   RecommendationScorer, writes ranked results, safely prunes unused candidates, and clears the
   temporary candidate table.
5. RecommendationRepositoryImpl exposes Room results and engine state; the ViewModel maps them to
   RecommendationUiState for the screen.

## Implementation map

| Path | Responsibility |
| --- | --- |
| feature/recommendation/presentation/RecommendationScreen.kt and RecommendationViewModel.kt | Destination UI, selected pill, screen states, refresh handling, and permission prompt state. |
| feature/recommendation/presentation/components/RecommendationCard.kt | Ranked card, match percentage, metadata, and detail-navigation action. |
| feature/recommendation/data/remote/RecommendationRemoteDataSource*.kt | MAL ranking and seasonal candidate requests. |
| feature/recommendation/data/work/FetchCandidatesWorker.kt | Candidate collection and local persistence. |
| feature/recommendation/data/work/EvaluateRecommendationsWorker.kt | Candidate evaluation and result cleanup. |
| feature/recommendation/data/work/RecommendationScheduler.kt | Unique initial, periodic, manual, and evaluation WorkManager requests. |
| feature/recommendation/domain/algorithm/RecommendationScorer.kt | TF-IDF-style tag weighting, quality factors, ranks, and match percentages. |
| feature/recommendation/data/repository/RecommendationRepositoryImpl.kt | Observes persisted results and calculation eligibility. |
| core/database/dao/RecommendationDao.kt | Candidate, result, and safe-pruning database operations. |

## Constraints

- Candidate work requires connectivity; the regular evaluation path requires an idle device, while
  an explicit user calculation relaxes that idle constraint.
- Candidate cleanup must never remove anime retained by the user's list or the New Seasons cache.
- Genre and theme rankings are stored independently for the same candidate set.
- Background work uses unique names and appropriate existing-work policies to avoid duplicate
  pipelines.

## Verification

feature/recommendation/src/test covers remote period calculation, mappers, repositories, scoring,
worker scheduling, worker behavior, and RecommendationViewModel state mapping. Core database
migration tests exercise the persistent schema used by the pipeline.
