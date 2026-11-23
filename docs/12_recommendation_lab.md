# Feature 12: Recommendation Lab

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:recommendation |
| Entry surface | RecommendationsRoute, RecommendationHeader "Tune" action, RecommendationLabBottomSheet |
| Related systems | :core:database, :core:domain, :core:ui, WorkManager |

## Purpose

Provide an interactive, explainable tuning desk for the smart recommendation engine. Users can
dynamically adjust familiar versus surprising recommendations, mainstream versus niche popularity,
time commitments, and community score thresholds, immediately observing the re-ranked candidates and
transparent "Why this match?" score breakdowns.

## User-facing behavior

- A "Tune" icon appears in the Recommendations header when viewing the Genres or Themes tabs.
- When an active tuning configuration differs from default "Balanced", a subtle indicator pill
  appears in the header (e.g., "Tune • Hidden gems").
- Tapping "Tune" opens the Recommendation Lab bottom sheet with:
  - 6 Presets: Comfort Zone, Balanced (default), Serendipity, Hidden Gems, Weekend Binge (≤13 eps),
    and Long Commitment (26+ eps).
  - Familiarity slider: Familiar ⟵ Balanced ⟶ Surprising.
  - Popularity slider: Mainstream ⟵ Balanced ⟶ Niche.
  - Time commitment filter chips: Any, ≤ 13 episodes, ≤ 26 episodes, 27+ episodes.
  - Minimum community score slider: Any, 6.5+, 7.0+, 7.5+, 8.0+.
  - Exclude Genres & Themes filter chips: Tab-scoped selectable chips (standard genres on Genres tab,
    top themes on Themes tab) allowing users to exclude unwanted tags in-memory during the session.
    Excluded chips display a trailing Close icon and subtle error container tint.
  - Live candidate match counter (e.g. "Showing 142 matches").
  - "Apply" and "Reset" action buttons.
- Recommendation cards feature an expandable "Why this match?" accordion revealing grounded evidence
  bullet points:
  - Matched affinity bullet indicating how many genres/themes matched user favorites with plural grammar.
  - Exploration anchor bullet indicating new tags and the familiar anchor tag from user taste.
  - Format and popularity fit bullet combining duration tier and popularity tier.
- User taste calculation automatically filters out unrated "Plan to Watch" entries (only status
  "completed", "watching", or titles with user score > 0 are used to derive taste weights).
- The existing "Not Interested" ignore feature is retained as-is.
- Lab settings remain active during the app session across tab switches and revert cleanly to
  "Balanced" on app restart or user logout.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:recommendation data remote | Fetches top 1,500 global rankings, top 100 movies, and 5 seasonal windows. |
| :feature:recommendation data work | Background candidate collection and persistence in Room candidate pool. |
| :feature:recommendation domain | Lab configuration models, presets, exploration/niche scoring, and breakdown. |
| :feature:recommendation presentation | Bottom sheet UI, live preview count, local re-ranking, and card explainability. |
| :core:database | Retains candidate pool and baseline evaluated recommendations. |

## Data flow

1. FetchCandidatesWorker retrieves up to 1,500 global ranking anime, 100 top movies, and 5 seasonal
   windows, persisting them into `recommendation_candidates` and `animes`.
2. EvaluateRecommendationsWorker calculates baseline "Balanced" recommendations for initial display.
3. RecommendationViewModel observes baseline recommendations and candidate pool.
4. When the user opens the Recommendation Lab bottom sheet, changes to sliders/chips calculate a
   live match count preview.
5. Tapping "Apply" applies `RecommendationLabConfig` to the candidate pool in-memory via
   `RecommendationScorer.evaluateLab(...)`, updating the UI state with re-ranked recommendations
   and their score breakdowns.
6. Tapping "Reset" restores default Balanced parameters.

## Implementation map

| Path | Responsibility |
| --- | --- |
| feature/recommendation/presentation/components/RecommendationLabBottomSheet.kt | Modal bottom sheet with sliders, chips, live counter, and inspection preview. |
| feature/recommendation/presentation/components/LabExclusionSection.kt | Tab-scoped genre and theme exclusion filter chip group. |
| feature/recommendation/presentation/components/RecommendationHeader.kt | Header with "Tune" action button and active preset indicator. |
| feature/recommendation/presentation/components/RecommendationCard.kt | Expandable "Why this match?" accordion and evidence bullets. |
| feature/recommendation/domain/model/RecommendationLabConfig.kt | Lab configuration, presets, exclusions, duration filter, and breakdown models. |
| feature/recommendation/domain/algorithm/RecommendationScorer.kt | Exploration scoring, niche weighting, hard filtering, and breakdown generation. |
| feature/recommendation/presentation/RecommendationViewModel.kt | Local re-ranking, live count calculation, and lab UI state management. |
| feature/recommendation/data/remote/RecommendationRemoteDataSource*.kt | Extended ranking (1,500), movie ranking (100), and seasonal fetch. |
| feature/recommendation/data/remote/SeasonalPeriodCalculator.kt | Calculates 5 target seasons (past 3, current, next). |
| feature/recommendation/data/work/FetchCandidatesWorker.kt | Expanded candidate collection and Room retention. |
| core/database/dao/RecommendationDao.kt | Candidate pool queries and safe pruning. |

## Constraints

- Sliders and chips must re-rank in-memory from cached Room candidates with zero network requests.
- Excluded tags silently filter out any candidate containing one or more of the excluded tags scoped to the active tab's tag type (genres or themes), with cross-tab exclusions visible and dismissible in the UI.
- Evidence breakdown displays the total count of matched tags (`totalMatchedCount`) while capping the displayed tag sample at 3.
- User taste profile calculation strictly ignores unrated "Plan to Watch" items in both live tuning and baseline background evaluation.
- "Surprising" familiarity requires an anchor: candidate must have at least one high-affinity tag.
- "Niche" popularity enforces a minimum mean score and user floor to prevent unvetted noise.
- Lab settings are session-scoped and do not mutate persistent DataStore preferences.
- Comply strictly with Detekt structural rules (≤60 lines per composable, ≤10 functions per file,
  ≤6 parameters per function) and zero hardcoded strings.

## Verification

Unit tests in `feature/recommendation/src/test` covering `SeasonalPeriodCalculatorTest`,
`RecommendationRemoteDataSourceTest`, `FetchCandidatesWorkerTest`, `RecommendationScorerTest` (lab
presets, familiarity/popularity formulas, breakdown generation), and `RecommendationViewModelTest`
(re-ranking, live preview, apply/reset events). Compose `@StandardPreviews` for bottom sheet and
expandable card states.
