# Feature 13: Franchise Map / Roadmap

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:franchise |
| Entry surface | TopAppBar action icon (AccountTree) on AnimeDetailsScreen |
| Related systems | :core:database, :core:network, :feature:details, :feature:recommendation |

## Purpose

Provide anime viewers with an intuitive, interactive visual graph and roadmap of an entire
anime franchise (prequels, sequels, side stories, OVAs, movies, specials, and spin-offs).
The feature operates with 0 extra network calls on open via passive relationship ingestion,
featuring a chronological spine layout, multi-touch canvas navigation, quick episode progress
tracking, and a fully accessible structured List View alternative.

## User-facing behavior

- **Entry Point**:
  - The `AnimeDetailsScreen` TopAppBar displays a dedicated branch graph icon (`AccountTree`)
    whenever franchise relationships exist for the current anime.
  - Tapping the icon navigates smoothly to the full-screen Franchise Roadmap (`/anime/{id}/franchise`).
- **Interactive Visual Canvas (Map View)**:
  - **Mainline Vertical Spine**: Sequels and prequels are arranged vertically in strict
    chronological order, connected by smooth Bézier curves with clean directional arrowheads.
  - **Lateral Side-Work Clusters**: OVAs, movies, side stories, and spin-offs are grouped
    laterally beside their associated mainline entry (alternating left and right) with
    contextual format tags.
  - **Origin Highlighting**: The anime from which the roadmap was launched is highlighted with
    a prominent primary accent border and an "ORIGIN" chip.
  - **Watch Status Badging**: Each node card displays personal status badges with WCAG AA
    compliant container and content colors (*Watching*, *Completed*, *On Hold*, *Dropped*,
    *Plan to Watch*).
  - **Ignored Anime Continuity**: Titles marked as "Not Interested" appear as muted, semi-transparent
    nodes with an eye-off icon badge to preserve complete roadmap continuity without clutter.
- **Multi-Touch Gestures & Floating Controls**:
  - Supports smooth 2-finger pinch-to-zoom (0.4x to 2.5x) and fluid 1-finger panning.
  - Floating controls overlay provides quick action buttons with smooth, interruptible
    animation transitions: Zoom In (+), Zoom Out (-), Fit to Screen, and Recenter on Origin.
- **Quick Node Drill-Down (Bottom Sheet)**:
  - Tapping any node opens a modal bottom sheet displaying full poster art, media format, title,
    and watch progress stepper (e.g. "Progress: 5/12").
  - Includes a direct "+1 Episode" increment button for watching anime and an "Unignore" action
    for hidden titles.
  - "Open Details" button transitions seamlessly to the full `AnimeDetailsScreen` for that title.
- **Accessible List View Alternative**:
  - TopAppBar action toggle switches instantaneously between the 2D Canvas Map and a structured,
    linear List View grouped into "Mainline Story" and "Side Stories & Spin-offs".
  - Perfect for screen readers, keyboard navigation, and fast text browsing.

## Architecture

| Layer | Responsibility |
| --- | --- |
| `:feature:franchise` presentation | Interactive Compose Canvas (`FranchiseCanvas`), gesture handling, node cards (`FranchiseNodeCard`), bottom sheet (`FranchiseNodeBottomSheet`), and linear list (`FranchiseListView`). |
| `:feature:franchise` domain | Graph data structures (`FranchiseGraph`, `FranchiseNode`, `FranchiseEdge`), layout engine (`FranchiseLayoutCalculator`), and graph builder use case (`GetFranchiseGraphUseCase`). |
| `:feature:franchise` data repository | `FranchiseRepositoryImpl` performing breadth-first search (BFS) across SQLite relationships to discover connected franchise components with 0 network calls. |
| `:core:database` | `AnimeRelationshipEntity` and `AnimeRelationshipDao` storing directed edges, title stubs, and reactive Flow queries. |
| `:feature:details` & `:feature:recommendation` | Passive relationship ingestion hooks saving related anime edges during background sync or manual details views. |

## Data flow

1. When anime details are fetched (either opportunistically when opening `AnimeDetailsScreen`
   or during background `FetchNewSeasonsWorker` scans), the returned `related_anime` edges and
   stub entities are saved into Room via `AnimeRelationshipDao.saveRelationships()`.
2. `AnimeDetailsViewModel` observes `AnimeRelationshipDao.observeHasRelationships(animeId)` to
   conditionally display the `AccountTree` action icon in the TopAppBar.
3. Upon tapping the icon, `MainActivity` routes to `FranchiseRoute(animeId)`.
4. `FranchiseViewModel` invokes `FranchiseRepository.observeFranchiseGraph(rootAnimeId)`:
   - Performs BFS in SQLite to gather all connected ancestor and descendant IDs.
   - Combines connected `AnimeEntity` rows, `UserAnimeListItem` entries, and `ignored_animes` IDs.
   - Normalizes reciprocal sequel/prequel edges into forward-directed Earlier ➔ Later edges.
   - Partitions nodes into the longest chronological mainline sequence and lateral side works.
5. `FranchiseLayoutCalculator` computes coordinate bounds, spine positions, lateral side clusters,
   and Bézier connection curves.
6. The Compose UI renders hardware-accelerated node cards and smooth curve paths with reactive
   pan/zoom transforms.

## Constraints & Engineering Standards

- **Zero Network Calls on Open**: Operates strictly from local SQLite cached relationships.
- **Performance**: Panning and zooming utilize `Modifier.graphicsLayer` transforms, avoiding
  recomposition churn during gesture execution.
- **Accessibility**: Semantics traversal and alternative structured `FranchiseListView` provide
  full TalkBack compatibility.
