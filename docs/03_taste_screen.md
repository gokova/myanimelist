# Feature 03: Anime Taste Analytics

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:taste |
| Entry surface | TasteRoute |
| Related systems | :core:database, :core:domain, :core:ui |

## Purpose

Turn the user's locally cached anime list into an accessible visual summary of genre and theme
preferences, with a drill-down to the matching titles.

## User-facing behavior

- The My Taste destination observes the local list only; it does not start a network sync.
- Users switch between Genres and Themes. Categories with fewer than three matching anime are
  omitted, and the screen explains both an empty list and insufficient-category states.
- The chart supports pan, pinch-to-zoom, selection, and re-centering. Selecting a bubble opens a
  bottom sheet with its matching anime, average user score, ratings, and watch-status chips.
- The chart has virtual accessibility nodes and actions so individual bubbles remain discoverable
  and operable with TalkBack.

## Architecture

| Layer | Responsibility |
| --- | --- |
| :feature:taste domain | Defines taste records and bubbles, aggregates them, and packs bubble positions in pure Kotlin. |
| :feature:taste data | Observes the user-list Room relation and maps it to the analytics input model. |
| :feature:taste presentation | Manages selected type and bubble state, renders the chart, and hosts the drill-down sheet. |
| :core:domain taxonomy | Classifies MAL tags as genres or themes for this and other features. |
| :core:ui | Supplies deterministic, semantic taste color pairs. |

## Data flow

1. TasteViewModel collects ObserveTasteAnalyticsUseCase.
2. TasteRepositoryImpl observes UserAnimeListDao and maps Room records to UserAnimeRecord.
3. The use case partitions every tag through AnimeTaxonomy, groups records by tag, filters groups
   smaller than three, calculates summaries, and asks CirclePacking for deterministic positions.
4. TasteViewModel exposes the currently selected type and bubble as TasteUiState.
5. PackedBubbleChart renders that state and sends the selected bubble back to the ViewModel for
   TasteBottomSheet to display.

## Implementation map

| Path | Responsibility |
| --- | --- |
| feature/taste/presentation/TasteScreen.kt and TasteViewModel.kt | Stateful screen, selected type, selected bubble, and re-center actions. |
| feature/taste/presentation/components/PackedBubbleChart.kt | Canvas gesture handling and bubble rendering. |
| feature/taste/presentation/components/BubbleAccessibilityOverlay.kt | Semantic touch targets and custom accessibility actions for chart data. |
| feature/taste/presentation/components/TasteBottomSheet.kt | Accessible selected-category drill-down, including inspection-mode preview support. |
| feature/taste/domain/usecase/ObserveTasteAnalyticsUseCase.kt | Aggregates and thresholds the observed list. |
| feature/taste/domain/algorithm/CirclePacking.kt | Pure deterministic circle-packing implementation. |
| feature/taste/data/repository/TasteRepositoryImpl.kt | Converts Room list relations to taste records. |
| core/domain/taxonomy/AnimeTaxonomy.kt | Owns shared tag classification and reports unknown tags once. |

## Constraints

- Bubble radii use square-root scaling and are bounded so a frequent tag does not obscure the
  rest of the chart.
- Pan and zoom stay within the chart's supported scale range; changing type resets the view.
- An unknown MAL tag falls back to Theme and is logged once through the domain logger so the
  taxonomy can be expanded deliberately.
- The bottom sheet preserves nested-scroll fling handling and has a simulated inspection overlay.

## Verification

feature/taste/src/test covers the packing algorithm, analytics aggregation, and TasteViewModel.
core/domain/src/test covers taxonomy classification behavior.
