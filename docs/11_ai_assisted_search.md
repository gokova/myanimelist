# Feature 11: AI-Assisted Search

| Field | Value |
| --- | --- |
| Status | Implemented |
| Primary module | :feature:search |
| Entry surface | Sparkle action in SearchTopBar |
| Related systems | :core:ai, :core:datastore, :core:network, :core:database, :core:ui, :feature:profile, :feature:details |

## Purpose

Empower users to discover anime through natural-language criteria (themes, moods, plot elements, or childhood nostalgia) using user-provided AI credentials (BYOK). The feature operates in a token-efficient, injection-resilient pipeline that combines structured AI candidate extraction with live MyAnimeList verification, personal taste matching, and contextual explanations.

## User-facing behavior

- **Entry & Mode Switching**:
  - The standard search bar features a trailing "sparkle" icon action (`AutoAwesome`).
  - Tapping the sparkle action expands the search bar into an elevated, multi-line natural language prompt card with a subtle AI-themed container tint, character counter (up to 500 characters), and explicit "Ask AI" submission button.
  - If no AI API key has been configured, tapping the sparkle icon triggers an explanatory dialog detailing the feature, offering a "Cancel" action and a "Configure Key" button that navigates directly to the Profile screen.
  - Tapping the toggle button again or tapping collapse smoothly transitions back to Standard Search without discarding the typed prompt or current AI results (dual-state preservation).

- **Prompt Input & Token Guardrails**:
  - The input field accepts up to 500 characters with an inline character counter and validation hints.
  - Prompts with fewer than 5 characters disable the submission button.
  - Submitting a prompt initiates the loading state while preserving input geometry.

- **AI Search Results**:
  - Results appear in a scrollable list of rich anime cards displaying MAL poster thumbnails, localized titles, media formats, episode counts, personal list status chips, and personal taste match percentages.
  - Each result card prominently features an AI badge with a concise "Why this fits" reason (e.g., *"Dark psychological sci-fi with robots similar to Pluto"*).
  - Above the results, a sticky header displays the AI's high-level intent interpretation and an info (`i`) button.
  - Tapping the info (`i`) button opens a dialog presenting the AI's explanation of its query understanding, criteria applied, model used, and any candidate titles that could not be resolved against the MAL catalog.

- **Profile Configuration (BYOK)**:
  - The Profile screen contains a dedicated "AI Search Settings" card with a masked password input field for the Gemini API key.
  - Features include a password visibility toggle, clear action, info button explaining BYOK and local Tink Keystore encryption, and a "Test Key" validation action.

- **Error States & Graceful Degradation**:
  - **Missing / Invalid Key (401/403)**: Clear contextual banner directing user to update credentials in Profile.
  - **Rate Limit Exceeded (429)**: Contextual guidance explaining provider quota limits.
  - **Partial Candidate Resolution**: Successfully resolved anime are displayed normally, with an omission note in the info dialog for unresolvable titles.
  - **Safety Policy / Off-Topic Refusal**: Explanatory dialog explaining why no candidates were generated.

## Architecture

| Layer | Responsibility |
| --- | --- |
| `:core:ai` | Domain contracts (`AiSearchClient`, `AiCandidate`, `AiSearchInterpretation`), Gemini REST API client via Retrofit/OkHttp, JSON schema definitions, and system prompts. |
| `:core:datastore` | `AiPreferences` backed by Tink Keystore AES256-GCM encryption, separate from session DataStore to survive MAL logout. |
| `:feature:search` presentation | Dual-state search screen (Standard vs. AI), animated search bar expansion, prompt validation, AI result cards, and explanation dialog. |
| `:feature:search` domain | `AiSearchAnimeUseCase`, candidate resolution logic, and deduplication against MAL catalog. |
| `:feature:search` data repository | Orchestrates `AiSearchClient` and `SearchRemoteDataSource` to resolve candidate titles into verified MAL entities with taste scores. |
| `:feature:profile` | UI and ViewModel integration for AI API key management, encryption, and testing. |
| `:feature:details` | Ephemeral detail navigation for AI-discovered anime without database pollution. |

## Data flow

1. User enters natural language prompt (e.g., *"Dark psychological robot anime like Pluto"*) and taps "Ask AI".
2. `SearchViewModel` emits `AiSearching` state and delegates to `AiSearchAnimeUseCase`.
3. `AiSearchAnimeUseCase` retrieves the encrypted Gemini API key from `AiPreferences`.
4. `GeminiAiClient` in `:core:ai` dispatches a `POST` request to Gemini 3.5 Flash-Lite with structured output schema, rigid system prompt, and user prompt as user content.
5. Gemini responds with strict JSON: `intentSummary`, `explanation`, and 5–10 candidate objects (`title`, `expectedYear`, `reason`).
6. `SearchRepository` iterates candidates with request pacing, calling MAL's `searchAnime(q = candidate.title, limit = 3)`.
7. Candidates are resolved against returned MAL nodes (matching title/year), deduplicated by MAL ID, and filtered against the user's `ignored_animes` table.
8. Resolved anime are matched with their respective AI reasons and evaluated with `SearchTasteMatcher` for personal taste affinity.
9. `SearchViewModel` presents the enriched candidate list and intent summary.
10. Tapping any anime navigates to `AnimeDetailsRoute` with ephemeral caching.

## Implementation map

| Path | Responsibility |
| --- | --- |
| `docs/11_ai_assisted_search.md` | Living technical specification for the feature. |
| `core/ai/` | Module containing `AiSearchClient`, `GeminiAiClient`, DTOs, and DI bindings. |
| `core/datastore/src/main/java/com/gokova/myanimelist/core/datastore/AiPreferences.kt` | Encrypted DataStore contract and implementation for AI provider keys. |
| `feature/search/presentation/SearchViewModel.kt` | MVI ViewModel managing dual state (standard and AI), query inputs, and events. |
| `feature/search/presentation/SearchUiState.kt` | Extended UI state model accommodating standard and AI modes. |
| `feature/search/presentation/components/SearchTopBar.kt` | Search bar supporting mode toggle and standard search input. |
| `feature/search/presentation/components/AiSearchComponents.kt` | AI prompt card, candidate picks, info dialog, and missing key interceptor. |
| `feature/search/presentation/components/SearchAnimeCard.kt` | Search result card displaying the "Why this fits" reason tag. |
| `feature/profile/presentation/components/AiSettingsCard.kt` | Profile screen card for API key entry, visibility toggle, and testing. |
| `feature/search/domain/usecase/AiSearchAnimeUseCase.kt` | Use case orchestrating AI suggestion and MAL verification. |
| `feature/search/data/mapper/AiCandidateResolver.kt` | Resolves AI candidate titles against live MAL search results. |

## Constraints

- **Strict BYOK Isolation**: The app never embeds default provider API keys. User keys are stored locally via Tink AES256-GCM Keystore encryption and excluded from backups.
- **MAL Logout Independence**: Clearing MAL auth tokens during logout must NOT clear `AiPreferences`.
- **Zero Hallucinated Metadata**: Displayed anime titles, images, episode counts, and scores MUST originate exclusively from verified live MAL API responses. AI suggestions provide candidate title strings and reason tags only.
- **Token Efficiency & Limits**: Maximum prompt length is capped at 500 characters. Generation tokens are hard-capped at 1024. Candidates are strictly capped at 5–10 items.
- **Prompt Injection Defense**: User prompts are strictly passed inside the request `contents` array as user role text; behavioral boundaries and JSON schema rules are enforced via `system_instruction` and `generationConfig.response_schema`.
- **Volatile Search Results**: AI search results are strictly in-memory and must not insert records into the Room `animes` table unless explicitly added to the user's list.
- **Detekt & Style Enforcement**: All new files must comply with Detekt limits (max 60 lines for composables, max 10 functions per file, max 100 character line length) and zero KtLint violations.

## Verification

- **Unit Tests (`:core:ai`)**:
  - Structured JSON response parsing and schema validation.
  - Error response handling (HTTP 400, 401, 403, 429, 500).
  - System prompt injection resistance and parameter formatting.
- **Unit Tests (`:core:datastore`)**:
  - `AiPreferencesImpl` Tink encryption, decryption, and persistence across session clearing.
- **Unit Tests (`:feature:search`)**:
  - `AiSearchAnimeUseCase` candidate resolution, deduplication, and omission tracking.
  - `SearchViewModel` state transitions between Standard and AI modes with dual-state preservation.
- **Unit Tests (`:feature:profile`)**:
  - Profile AI key validation, masking, and event handling.
- **Static Analysis**:
  - `./gradlew test` (100% pass)
  - `./gradlew detekt` (0 violations)
  - `./gradlew ktlintCheck` (100% compliant)
