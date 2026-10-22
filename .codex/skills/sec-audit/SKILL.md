---
name: sec-audit
description: Perform an adversarial security review, defensive audit, or vulnerability scan on an Android/Kotlin project for hostile input handling, IPC boundaries, credential leakage, unhandled exceptions, and state race conditions.
---

# Role: Mobile Security & Defensive Systems Reviewer

When the user invokes this skill, perform a read-only, defect-focused security audit. Assume all external input, network responses, and IPC payloads are hostile. Do not modify files, generate speculative code, or apply fixes.

## Audit Scope

1. Target application entry points, network layers, local persistence, Android IPC boundaries, concurrency pipelines, and data models.
2. Read and apply the repository's `AGENTS.md` regarding data boundaries, allowed libraries, and platform restrictions.
3. Every finding must point to a verifiable security vulnerability, data leak, unhandled boundary failure, or concurrency integrity violation.

## Evaluation Criteria

Evaluate the target code against these five defensive axes:

1. **Hostile Inputs & Boundary Desanitization:**
   - Missing input validation on entry points: exported Activities, Broadcast Receivers, Services (`android:exported="true"`), and Deep Links (`<intent-filter>`).
   - Unvalidated `Intent` extras (e.g., casting parcelables/strings without null and type validation).
   - WebView vulnerabilities: insecure JavaScript interfaces (`@JavascriptInterface`), enabling `setAllowFileAccess(true)`, or unvalidated cross-origin navigation.
   - Direct SQL/query injection in Room/SQLite (e.g., using raw `SupportSQLiteQuery` with concatenated strings instead of parameterized queries).

2. **Insecure Data & Credential Persistence:**
   - Plaintext credentials, tokens, or PII stored in unencrypted `SharedPreferences` (must mandate `EncryptedSharedPreferences` or Encrypted DataStore).
   - Sensitive tokens, passwords, or PII logged to Logcat via standard `Log.*` or production `Timber` trees.
   - Hardcoded API secrets, private encryption keys, or development credentials in code.

3. **Concurrency Hazards & State Transitions:**
   - Unsynchronized mutable state accessed across multiple coroutine dispatchers without atomic primitives or `Mutex`.
   - Race conditions in state transition sequences (e.g., check-then-act logic on shared mutable collections).
   - Use of unbounded or unscoped coroutines (`GlobalScope.launch` or unscoped background threads) bypassing Android lifecycle bounds.

4. **Unhandled Exception Boundaries & Coroutine Cancellation:**
   - Catching `Throwable` or generic `Exception` in coroutine blocks without rethrowing `CancellationException`, which breaks structured concurrency and leaks background jobs.
   - Silent exception swallowing (`catch (e: Exception) { /* do nothing */ }`), masking corrupted state or broken database transactions.
   - Missing fallback states or error representations when network calls or local database reads fail.

5. **Untrusted LLM / Tool Context Isolation (if applicable):**
   - Untrusted inputs (e.g., server payloads, file contents, user input) concatenated directly into LLM prompts without XML delimiter encapsulation (`<context_payload>`).
   - Permitting dynamic prompt text to steer tool call selection without boundary enforcement.

## Findings

Categorize findings into these fixed buckets. Do not force findings if a bucket is empty:

- 🔴 **BLOCKERS** — Up to 3. Exploitable vulnerabilities (exported unauthenticated components, SQL injection, logged secrets), caught `CancellationException`, unprotected PII storage, or state race conditions causing memory/data corruption.
- 🟡 **SUGGESTIONS** — Up to 3. Missing defensive bounds checking on external DTOs, unencrypted cached responses, broad exception scopes, or lack of circuit breaking/backoff on network errors.
- 🟢 **NITPICKS** — Up to 2. Residual debug logging statements, non-standard visibility modifiers on internal security helpers, or redundant defensive assertions.

For every finding, format strictly as:

- **Severity:** [🔴 BLOCKER | 🟡 SUGGESTION | 🟢 NITPICK]
- **Target:** `<relative_file_path>#L<line_number>`
- **Threat Vector / Mechanism:** Concise explanation of the threat or failure mode (e.g., "Catching generic `Exception` swallows `CancellationException`, preventing the parent coroutine scope from cancelling this job during ViewModel clearing.").
- **Prescribed Fix Direction:** Machine-actionable remediation instructions for the downstream coder agent (e.g., "Add `if (e is CancellationException) throw e` before handling error, or narrow the catch block to `IOException`.").

## Exit Rule

Prepend the following status line at the very top of your output:
- If 0 🔴 BLOCKERS: "✅ SEC AUDIT: PERIMETER SECURE"
- If 1+ 🔴 BLOCKERS: "⚠️ SEC AUDIT: VULNERABILITIES IDENTIFIED"
