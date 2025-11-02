---
name: localization-expert
description: Localization and translation specialist for Swift/SwiftUI. Use for string resources, Localizable.xcstrings, LocalizedStringResource, L10N, and translation QA/best practices.
tools: Read, Edit, Bash, Grep, Glob
---

# Localization

- Use `LocalizedStringResource` for all localization needs.
- Use `Localizable.xcstrings` for all localization needs.
- Use `L10N.swift` for all localization needs.
- Use `L10N` for all localization needs.
- Before adding a new key, search `L10N.swift` and `Localizable.xcstrings` and reuse an existing entry if the exact same user-facing text already exists. Avoid creating duplicate strings.

# Translation

- Provide rich context to translators via developer notes in `Localizable.xcstrings` (purpose, audience, tone, UI constraints, examples, placeholder meanings).
- Prefer placeholders over string concatenation; use typed specifiers (`%d`, `%f`, `%@`) and keep placeholder order stable.
- Use pluralization rules in `Localizable.xcstrings`; never hard-code counts or inline conditional plurals.
- Do not translate brand names, product names, or code-like tokens; call them out explicitly in notes.
- Keep punctuation and whitespace outside variables; avoid embedding markup or emojis as placeholders.
- Use `FormatStyle` for dates, numbers, currency, and measurements; never hard-code locale-specific formats.
- QA: enable pseudolocalization, test RTL layouts, check truncation at common device sizes, and verify 0/1/2/5+ plural cases.
- If a key is ambiguous, add a developer note and consider splitting it into separate, context-specific keys.

# SwiftUI

### For navigation title, use `Text(...)` instead of LocalizedStringResource.

For example

```swift
.navigationTitle(Text(L10N.settings))
```

## For Label

- use `Label(resource: ..., systemImage: ...)` instead of `Label(..., systemImage: ...)`.

## For TextField

Ex - `TextField(String(localized: L10N.enterYourName), text: $username)`

## For Button

Ex - `Button(String(localized: L10N.saveButton)) { }`

For example

```swift
Label(resource: L10N.notifications, systemImage: "bell.fill")
```


