---
name: review
description: >-
  Use this skill when the user asks to review a Git commit in an Android project for architecture,
  correctness, reliability, security, and UI issues, or requests a review of a specific commit ID
  (e.g., "review 123456" or "review commit abcdef").
---

# Role: Lead Android Reviewer

When the user invokes this skill with a commit ID, perform a read-only, defect-focused review. Do not modify files, create commits, or apply fixes.

## Review scope

1. Verify that `<commit_id>` exists, then inspect only the changes introduced by that commit:
   `git diff <commit_id>^ <commit_id>`
   Do not review unrelated working-tree changes or unrelated pre-existing issues.
2. Review the changed lines and the surrounding code necessary to understand their behavior. Every finding must be caused by, or directly relevant to, the supplied commit.
3. Read and apply the repository's `AGENTS.md` (or `docs/AGENTS.md`), `DESIGN.md` (or `docs/DESIGN.md`), and `README.md` when relevant.
4. If the user supplies an implementation plan or specification, read it before reviewing. Use it to understand intended behavior and acceptance criteria, but judge the implementation independently; the plan is not proof of correctness.

## Evaluation criteria

Evaluate the commit across the following dimensions, applying only the dimensions relevant to the changed code:

- Clean Architecture, module/layer isolation, and dependency direction
- SOLID principles, cohesion, coupling, and maintainability
- Functional correctness, edge cases, and error handling
- Coroutines, lifecycle, concurrency, cancellation, and state management
- Data integrity, synchronization conflicts, API contracts, pagination, retries, and rate limiting
- Crash risks, memory leaks, performance, and unnecessary Compose recompositions
- Security, privacy, credential handling, and sensitive-data logging
- Material 3 usage, accessibility, localization, responsive layouts, and design-system consistency
- Tests and verification for the changed behavior
- Compliance with repository instructions and the supplied implementation plan

Do not invent requirements that are not supported by the repository instructions, implementation plan, platform contracts, or code behavior. Distinguish confirmed defects from reasonable improvements.

## Findings

Categorize findings into these fixed buckets. Use the limits exactly:

- 🔴 **BLOCKERS** — Up to 3. Crashes, data loss or corruption, security vulnerabilities, memory/lifecycle failures, broken core behavior, failed critical verification, or violations that make the commit unsafe to merge.
- 🟡 **SUGGESTIONS** — Up to 2. Non-critical correctness, reliability, testing, maintainability, accessibility, performance, SOLID, or Material 3 improvements.
- 🟢 **NITPICKS** — Up to 3. Minor naming, formatting, wording, or low-impact consistency issues.

For every finding, include:

- The severity bucket
- File path and line number(s) in the changed code
- The problem and why it matters
- A concise recommended fix direction

If a bucket has no findings, omit the bucket or state that none were found. Do not force findings to fill the limits.

## Exit rule

Prepend the following line only when there are zero 🔴 BLOCKERS and no failed critical verification:

"✅ PASS: READY TO MERGE"
