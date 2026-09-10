# Design System (Soft & Modern)

This document outlines the official design language, visual guidelines, tokens, component specifications, and developer rules for the MyAnimeList Android application. All UI components, screens, and future additions must strictly adhere to these principles.

---

## 1. Core Philosophy

The application follows a **"Soft & Modern"** aesthetic. The goal is to create a calming, friendly, and approachable user experience tailored for anime and manga enthusiasts:
*   **Pastel & Vibrant Harmony:** A vibrant, accessible primary blue paired with soft pastel tonal containers.
*   **Organic Shapes:** Friendly, rounded geometry (`8.dp` to `32.dp`) avoiding harsh rectangular edges.
*   **Tonal Depth Over Drop Shadows:** Flat, clean surfaces separated by subtle background-to-surface tonal shifts rather than harsh drop shadows.
*   **Accessibility First:** High contrast (WCAG AA compliant, \(\ge 4.5:1\) for body text) paired with minimum `48.dp` interactive touch targets.

---

## 2. Guidelines for AI Agents & Developers ("Theme-First Rule")

When designing or implementing screens and widgets, future AI agents and engineers **must** follow these non-negotiable rules:

### Rule 1: Single Source of Truth (`:core:ui`)
All design system tokens, color palettes, typography hierarchies, shape definitions, spacing tokens, and reusable foundational components reside exclusively in the `:core:ui` module (`com.gokova.myanimelist.core.ui.theme`).
*   **Do not** create app-local or feature-local theme overrides or duplicate `Theme.kt` / `Color.kt` files.
*   **Do not** define XML color resources in `:app` for Compose screens.

### Rule 2: No Ad-Hoc / In-Place Theming
*   **Never hardcode raw colors:** Do not use `Color(0xFF...)` or `Color.Blue` directly inside feature composables. Always reference `MaterialTheme.colorScheme.*` or the semantic tokens in `com.gokova.myanimelist.core.ui.theme`.
*   **Never use arbitrary dimensions:** Do not use random paddings like `Modifier.padding(13.dp)`. Use the spacing tokens from `MaterialTheme.spacing` (e.g., `MaterialTheme.spacing.medium`).

### Rule 3: Workflow for Implementing Unstyled / Future Widgets
When building a new widget or component (such as an Anime Card, Chip, Bottom Sheet, or Rating bar) for which no explicit component style currently exists:
1.  **Do NOT do in-place styling** by guessing hex colors or arbitrary paddings inside the screen file.
2.  **Inspect the Design System:** Check if existing semantic roles (`primaryContainer`, `secondaryContainer`, `tertiary`, `surfaceContainer`, `MaterialTheme.shapes`, `MaterialTheme.spacing`) fit the widget.
3.  **Update the Design System if Needed:** If generic tokens or component styles are missing, define them in `:core:ui` under `theme/` or create a reusable component in `:core:ui` (e.g. `core/ui/src/main/java/com/gokova/myanimelist/core/ui/component/`).
4.  **Validate Accessibility:** Ensure the new component meets WCAG AA contrast (\(\ge 4.5:1\)) and minimum `48.dp` interactive touch targets.
5.  **Preview Coverage:** Annotate every new reusable component or screen with `@StandardPreviews`.

---

## 3. Design Tokens

### 3.1 Material 3 Color Schemes

#### Light Theme Palette
The light theme uses an off-white background (`#F8F9FA`) with pure white (`#FFFFFF`) or cool tinted containers (`#E0E3E8` / `#EDF1F5`) to create soft, natural contrast without shadows.

| Role | Hex | On-Role Color | Contrast Ratio | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `primary` | `#2065C0` | `#FFFFFF` (`onPrimary`) | 5.7:1 (Passes AA) | Main action buttons, active navigation indicators, key highlights |
| `primaryContainer` | `#D8E6FF` | `#001D40` (`onPrimaryContainer`) | 12.8:1 (Passes AA) | Highlighted badges, active tab containers, tonal buttons |
| `secondary` | `#535F70` | `#FFFFFF` (`onSecondary`) | 6.2:1 (Passes AA) | Secondary controls, section icons, supplementary actions |
| `secondaryContainer` | `#D7E3F7` | `#101C2B` (`onSecondaryContainer`) | 12.2:1 (Passes AA) | Filter chips, metadata tag backgrounds, secondary pills |
| `tertiary` | `#6B5778` | `#FFFFFF` (`onTertiary`) | 6.1:1 (Passes AA) | Accent tags, bottom sheet highlights, anime genre badges |
| `tertiaryContainer` | `#F2DAFF` | `#251431` (`onTertiaryContainer`) | 12.5:1 (Passes AA) | Special genre chips (Romance/Fantasy), celebratory accents |
| `error` | `#BA1A1A` | `#FFFFFF` (`onError`) | 5.8:1 (Passes AA) | Destructive actions, validation errors, dropped anime status |
| `errorContainer` | `#FFDAD6` | `#410002` (`onErrorContainer`) | 13.1:1 (Passes AA) | Error notification banners, alert containers |
| `background` | `#F8F9FA` | `#2C3E50` (`onBackground`) | 10.4:1 (Passes AA) | Main app canvas background |
| `surface` | `#FFFFFF` | `#2C3E50` (`onSurface`) | 11.2:1 (Passes AA) | Cards, bottom sheets, dialogs, elevated containers |
| `surfaceVariant` | `#E0E3E8` | `#43474E` (`onSurfaceVariant`) | 5.2:1 (Passes AA) | Avatar circles, divider lines, unselected navigation icons |
| `surfaceContainer` | `#EDF1F5` | `#2C3E50` (`onSurface`) | 9.8:1 (Passes AA) | Grouped card containers, list headers |
| `outline` | `#74777F` | — | — | Outlined buttons, input borders, structural dividers |
| `outlineVariant` | `#C4C7D0` | — | — | Subtle card borders, decorative dividers |

#### Dark Theme Palette
The dark theme avoids pure blacks (`#000000`), using rich dark gray-blue (`#1A1C1E`) and elevated charcoal (`#202429`) to reduce eye fatigue and maintain atmospheric softness.

| Role | Hex | On-Role Color | Contrast Ratio | Usage |
| :--- | :--- | :--- | :--- | :--- |
| `primary` | `#82B1FF` | `#001C3A` (`onPrimary`) | 10.2:1 (Passes AA) | Main action buttons, active navigation, key highlights |
| `primaryContainer` | `#00458E` | `#D8E6FF` (`onPrimaryContainer`) | 7.3:1 (Passes AA) | Active tab containers, prominent badges |
| `secondary` | `#BBC7DB` | `#253140` (`onSecondary`) | 8.2:1 (Passes AA) | Secondary controls, section icons |
| `secondaryContainer` | `#3B4858` | `#D7E3F7` (`onSecondaryContainer`) | 6.8:1 (Passes AA) | Filter chips, metadata tag backgrounds |
| `tertiary` | `#D6BEE4` | `#3B2A47` (`onTertiary`) | 8.5:1 (Passes AA) | Accent tags, bottom sheet highlights, genre tags |
| `tertiaryContainer` | `#52405F` | `#F2DAFF` (`onTertiaryContainer`) | 6.5:1 (Passes AA) | Special genre chips, celebratory accents |
| `error` | `#FFB4AB` | `#690005` (`onError`) | 8.1:1 (Passes AA) | Destructive actions, validation errors |
| `errorContainer` | `#93000A` | `#FFDAD6` (`onErrorContainer`) | 7.9:1 (Passes AA) | Error banners, alert containers |
| `background` | `#1A1C1E` | `#E3E3E3` (`onBackground`) | 11.5:1 (Passes AA) | Main dark canvas background |
| `surface` | `#202429` | `#E3E3E3` (`onSurface`) | 10.8:1 (Passes AA) | Cards, bottom sheets, dialogs |
| `surfaceVariant` | `#2D333B` | `#C4C7D0` (`onSurfaceVariant`) | 6.9:1 (Passes AA) | Avatar circles, unselected navigation icons |
| `surfaceContainer` | `#202429` | `#E3E3E3` (`onSurface`) | 10.8:1 (Passes AA) | Grouped card containers |
| `outline` | `#8E9099` | — | — | Outlined buttons, input borders |
| `outlineVariant` | `#43474E` | — | — | Subtle card borders, dividers |

### 3.2 Semantic Status Colors (Anime Tracking)
To ensure accessibility, status chips and badges must **never** place white text over soft/pastel colors. Always pair containers with high-contrast foreground text/icons:

| Status | Container Hex | Foreground Hex | Contrast Ratio | Visual Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Watching** | `#E8F5E9` (Soft Green) | `#1B5E20` (Dark Forest) | 7.2:1 (Passes AA) | Active watching status chip / badge |
| **Completed** | `#E3F2FD` (Soft Blue) | `#0D47A1` (Deep Navy) | 8.1:1 (Passes AA) | Finished series status chip / badge |
| **On-Hold** | `#FFF8E1` (Soft Amber) | `#E65100` (Deep Amber) | 5.6:1 (Passes AA) | Paused series status chip / badge |
| **Dropped** | `#FFEBEE` (Soft Red) | `#B71C1C` (Deep Crimson) | 6.8:1 (Passes AA) | Discontinued series status chip / badge |
| **Plan to Watch** | `#F3E5F5` (Soft Purple) | `#4A148C` (Deep Purple) | 7.9:1 (Passes AA) | Wishlist / backlog status chip / badge |

> [!NOTE]
> In addition to color, every status component **must** include an accompanying text label or icon (e.g., checkmark for Completed, pause icon for On-Hold) so status is never conveyed by color alone.

---

### 3.3 Typography Hierarchy

Typography roles are centralized in `com.gokova.myanimelist.core.ui.theme.Typography`:

| Role | Font Size | Line Height | Weight | Letter Spacing | Usage in MyAnimeList |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | `57.sp` | `64.sp` | Normal | `-0.25.sp` | Hero numbers, prominent brand titles |
| `displayMedium` | `45.sp` | `52.sp` | Normal | `0.sp` | Large section banners |
| `displaySmall` | `36.sp` | `44.sp` | Normal | `0.sp` | Onboarding headings |
| `headlineLarge` | `32.sp` | `40.sp` | SemiBold | `0.sp` | Screen primary titles, Anime detail titles |
| `headlineMedium`| `28.sp` | `36.sp` | SemiBold | `0.sp` | Modal dialog titles, major section headers |
| `headlineSmall` | `24.sp` | `32.sp` | SemiBold | `0.sp` | Confirmation dialog titles, card group titles |
| `titleLarge` | `22.sp` | `28.sp` | Medium | `0.sp` | Top app bar title ("MAL"), featured anime titles |
| `titleMedium` | `16.sp` | `24.sp` | Medium | `0.15.sp` | Anime card titles, button labels, list headers |
| `titleSmall` | `14.sp` | `20.sp` | Medium | `0.1.sp` | Sub-headers, episode titles, dialog subheadings |
| `bodyLarge` | `16.sp` | `24.sp` | Normal | `0.5.sp` | Synopsis text, user reviews, long descriptions |
| `bodyMedium` | `14.sp` | `20.sp` | Normal | `0.25.sp` | Dialog body descriptions, secondary synopsis |
| `bodySmall` | `12.sp` | `16.sp` | Normal | `0.4.sp` | Timestamp, studio name, secondary metadata |
| `labelLarge` | `14.sp` | `20.sp` | Medium | `0.1.sp` | Button text, bottom navigation labels |
| `labelMedium` | `12.sp` | `16.sp` | Medium | `0.5.sp` | Status chip text, genre tag labels |
| `labelSmall` | `11.sp` | `16.sp` | Medium | `0.5.sp` | Score badges, season year pill labels |

---

### 3.4 Shapes Scale

Defined in `com.gokova.myanimelist.core.ui.theme.Shapes`:

| Shape Token | Corner Radius | Intended Component Usages |
| :--- | :--- | :--- |
| `extraSmall` | `4.dp` | Status chips, small badges, tooltips, score pills |
| `small` | `8.dp` | Anime thumbnail posters, dropdown menus, genre tags |
| `medium` | `16.dp` | Anime content cards, list item containers, search inputs |
| `large` | `24.dp` | Standard buttons, floating action buttons, pill containers |
| `extraLarge` | `32.dp` | Dialog windows, bottom sheet top corners, full-screen sheets |

---

### 3.5 Spacing & Dimensions Scale

Access via `MaterialTheme.spacing` (from `com.gokova.myanimelist.core.ui.theme.spacing`):

| Token | Size | Typical Usage |
| :--- | :--- | :--- |
| `none` | `0.dp` | Zero padding |
| `extraSmall` | `4.dp` | Gap between icon and text, micro chip spacing |
| `small` | `8.dp` | Gap between items in a row, dialog action button spacing |
| `medium` | `16.dp` | Card internal content padding, spacing between form fields |
| `large` | `24.dp` | Standard screen horizontal edge padding, dialog padding |
| `extraLarge` | `32.dp` | Section spacing between major screen blocks |
| `extraExtraLarge` | `48.dp` | Major visual spacing, minimum interactive touch target height/width |
| `huge` | `64.dp` | Hero spacing, empty-state top padding |
| `screenHorizontal`| `24.dp` | Universal screen edge margin |
| `minTouchTarget` | `48.dp` | Minimum touchable area for clickable elements (WCAG requirement) |
| `cardPadding` | `16.dp` | Default padding inside cards |

---

## 4. Component Specifications

### 4.1 Buttons
*   **Primary Button:** `MaterialTheme.colorScheme.primary` container with `onPrimary` text. Shape: `MaterialTheme.shapes.large` (`24.dp` pill). Height: `56.dp` for full-width primary CTA, `44.dp` for compact buttons.
*   **Tonal Button:** `MaterialTheme.colorScheme.secondaryContainer` with `onSecondaryContainer` text.
*   **Outlined Button:** Border `1.dp` in `outline`, transparent container.
*   **Text Button:** Zero elevation, `onSurfaceVariant` or `primary` text.
*   **States:**
    *   *Disabled:* Container `surfaceVariant.copy(alpha = 0.38f)`, content `onSurface.copy(alpha = 0.38f)`.
    *   *Loading:* Replace button text with a centered `CircularProgressIndicator(size = 24.dp)` matching content color. Maintain the button footprint.

### 4.2 Avatar & Profile Controls
*   **Visual Size:** `40.dp` circular badge (`CircleShape`).
*   **Touch Target:** Minimum `48.dp` interactive area (`MaterialTheme.spacing.minTouchTarget`).
*   **Background:** `MaterialTheme.colorScheme.surfaceVariant`.
*   **Content:** Profile image clipped to `CircleShape`; fallback to `Icons.Default.Person` tinted with `onSurfaceVariant`.

### 4.3 Navigation
*   **Phone (Bottom Navigation Bar):**
    *   Container: `MaterialTheme.colorScheme.surface` with zero shadow elevation.
    *   Indicator: `MaterialTheme.colorScheme.primary` with `onPrimary` icon.
    *   Unselected: `MaterialTheme.colorScheme.onSurfaceVariant`.
    *   Label: `labelLarge`, always visible or labeled clearly on selection.
*   **Tablet (Navigation Rail):**
    *   Screens with width \(\ge 600.dp\) transition from bottom bar to a vertical Navigation Rail on the start edge.
    *   Rail items must be icon-only (omitting text labels) with localized `contentDescription`s to avoid text wrapping and visual clutter in landscape and tablet layouts.

### 4.4 Dialogs & Bottom Sheets
*   **Shape:** `MaterialTheme.shapes.extraLarge` (`32.dp`). Bottom sheets round only `topStart` and `topEnd`.
*   **Surface & Elevation:** `MaterialTheme.colorScheme.surface` with subtle `tonalElevation = 6.dp`.
*   **Scrim:** Semi-transparent `MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)`.
*   **Button Ordering:** Cancel action on the left/start, Primary confirmation on the right/end.

### 4.5 Future Anime Cards & Media Components
*   **Anime Poster Aspect Ratio:** Standard `2:3` ratio (e.g., `width = 120.dp, height = 180.dp`).
*   **Banner Aspect Ratio:** Standard `16:9` ratio.
*   **Card Shape:** `MaterialTheme.shapes.medium` (`16.dp`).
*   **Score Badge:** `extraSmall` shape (`4.dp`), `tertiaryContainer` background with `onTertiaryContainer` bold text, paired with a star icon.

---

## 5. Accessibility & Interaction Rules

All screens must be verified against WCAG AA requirements:
1.  **Contrast Minimums:**
    *   Normal text: \(\ge 4.5:1\) against its background.
    *   Large text (\(\ge 18.sp\) or \(\ge 14.sp\) bold) & UI component boundaries: \(\ge 3.0:1\).
2.  **Minimum 48.dp Touch Target:**
    *   All interactive elements (buttons, icons, avatars, checkboxes) must have a clickable area of at least `48.dp \times 48.dp`.
    *   Use `Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` or center smaller visual elements inside a `48.dp` box.
3.  **Font Scaling:**
    *   UI must support dynamic font scaling up to `2.0x` without clipping or truncating critical actions.
    *   Do not hardcode fixed heights on text containers; prefer `wrapContentHeight()`.
4.  **Color-Independent Communication:**
    *   Statuses must never rely on color alone. Always provide text labels and distinctive icons.
5.  **TalkBack & Content Descriptions:**
    *   All interactive icon buttons must have localized `contentDescription` strings.
    *   Decorative images should pass `contentDescription = null`.

---

## 6. Responsive & System UI

1.  **Edge-to-Edge:** Enabled across all activities via `enableEdgeToEdge()`.
2.  **Window Insets:** Use `Scaffold` and `Modifier.padding(paddingValues)` or `Modifier.statusBarsPadding()` / `navigationBarsPadding()` so content is never obscured by camera notches or system gesture bars.
3.  **Landscape & Large Screens:**
    *   Compact (\(< 600.dp\)): Bottom Navigation Bar, single column lists.
    *   Medium & Expanded (\(\ge 600.dp\)): Navigation Rail, adaptive grid (2 to 4 columns for anime cards).
    *   **Grid Cell Sizing:** When using `GridCells.Adaptive`, set `minSize = 240.dp` (never \(\ge 300.dp\)) to ensure that screens at the \(600.dp\) breakpoint with an \(80.dp\) Navigation Rail and margins (\(\sim 488.dp\) available width) reliably render 2 columns instead of collapsing into 1 column.
    *   **Configuration Awareness:** Always calculate window size from `LocalWindowInfo.current.containerSize` converted with `LocalDensity.current` rather than `Configuration.screenWidthDp` to avoid stale configuration and preview rendering issues.

---

## 7. Compose Previews & UI Verification

All UI components and screens must include preview coverage using the standard multi-preview annotation:
*   **Standard Multi-Preview:** Annotate components with `@StandardPreviews` (`com.gokova.myanimelist.core.ui.preview.StandardPreviews`). This automatically verifies:
    *   Phone Light & Dark modes
    *   Tablet Light & Dark screen sizes
    *   Phone Light & Dark under 1.5x Accessibility font scale
    *   Phone Landscape orientation
*   **Preview Parameters:** Use `PreviewParameterProvider` to inject test states (e.g., Idle, Loading, Error, Content) rather than creating separate preview functions for each state.
*   **Descriptive Parameter Names:** Always override `getDisplayName(index: Int)` in `PreviewParameterProvider` implementations to return human-readable names (e.g., `"Loaded"`, `"Empty"`, `"Watching"`) rather than default indices (`uiState 0`, `uiState 1`).
*   **Stateless Previews:** Screens must expose a public stateless composable accepting state and lambdas so `@StandardPreviews` never instantiates Hilt ViewModel factories or relies on an Activity context.
