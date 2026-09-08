# Design System (Soft & Modern)

This document outlines the design language and visual guidelines for the MyAnimeList Android application. All UI components and screens must adhere to these principles to maintain a consistent look and feel across the app.

## Core Philosophy
The application follows a **"Soft & Modern"** aesthetic. The goal is to create a calming, friendly, and highly approachable user experience. This is achieved through:
*   A Pastel Blue primary color.
*   Large, friendly rounded corners.
*   Flat surfaces with subtle tonal color variations instead of harsh drop shadows.
*   Clean typography with good contrast.

## Light & Dark Theming
To maintain the "soft" aesthetic across both modes, we avoid pure whites (`#FFFFFF`) as backgrounds where possible, and strictly avoid pure blacks (`#000000`) in dark mode. 

### Light Theme Palette
*   **Primary:** Soft Blue (`#4A90E2`) - Used for primary actions, active tabs, and highlights.
*   **On Primary:** White (`#FFFFFF`) - Text/Icons on top of primary elements.
*   **Background:** Off-White / Soft Gray (`#F8F9FA`) - The main app background.
*   **Surface:** Pure White (`#FFFFFF`) or Light Gray-Blue (`#F1F3F5`) - Used for cards and containers. The contrast against the off-white background creates depth without shadows.
*   **Text:** Dark Slate (`#2C3E50`) - Softer than pure black.

### Dark Theme Palette
*   **Primary:** Light Pastel Blue (`#82B1FF`) - Lightened for better visibility and contrast against dark backgrounds.
*   **On Primary:** Dark Navy (`#001C3A`) - Text/Icons on top of primary elements.
*   **Background:** Dark Gray-Blue (`#1A1C1E`) - Avoids the harshness of pure black, maintaining a soft, atmospheric feel.
*   **Surface:** Elevated Dark Gray (`#202429`) - Used for cards. Slightly lighter than the background to establish hierarchy.
*   **Text:** Soft White / Silver (`#E3E3E3`) - Easier on the eyes than stark white.

## Accents & Semantic Colors
If secondary colors are needed (e.g., for different anime statuses like Watching vs. Completed), stick to pastel variants in both themes:
*   **Success/Watching:** Soft Green (`#81C784`)
*   **Warning/On-Hold:** Soft Yellow (`#FFD54F`)
*   **Error/Dropped:** Soft Red (`#E57373`)

## Shapes & Geometry
Avoid sharp edges. Everything should feel organic, soft, and modern.
*   **Buttons:** Fully rounded (pill-shaped) or large rounded corners (e.g., `24.dp`).
*   **Cards & Containers:** Large rounded corners (e.g., `16.dp` to `24.dp`).
*   **Images (Anime Posters, Avatars):** Must have rounded corners. User avatars should be completely circular.

## Elevation & Shadows
*   **Zero to Minimal Elevation:** We avoid traditional Material Design drop shadows.
*   **Tonal Variation:** To separate elements (like a card floating above the background), use the designated Surface color against the Background color. This "tonal elevation" keeps the UI looking flat but distinct.

## Component Specifics
*   **Top App Bar:** Prominently feature the title "MAL" in the center. The user's profile avatar is placed on the right corner. The app bar blends seamlessly with the background (zero elevation).
*   **Bottom Navigation:** Tabs ("My List", "My Taste", "Recommendations") use soft icons. Active tabs are highlighted with the Primary color.

## Compose Previews & UI Testing
All UI components and screens must be visually testable without running the app.
*   **Standard Multipreview:** Use the @StandardPreviews annotation (located in :core:ui:preview) on all UI components to automatically test across:
    *   Light & Dark modes
    *   Phone & Tablet screen sizes
    *   Normal & Accessibility (1.5x) font scales
*   **Preview Parameters:** Use PreviewParameterProvider to inject mock UI states (e.g., Idle, Loading, Error, Success) into your previews rather than creating multiple manual @Preview functions.
*   **Modularity:** Keep Compose functions small. If a screen becomes too long, extract elements into discrete stateless components (e.g., Header, ActionArea) to make previewing easier and prevent Detekt \LongMethod\ failures.

