# Architecture

## Recommended boundary

Keep the clone as a separate app project until its core behaviors are stable.

Why:

- it avoids mixing experimental parity work into Lawnchair Lite
- it lets the clone use a narrower, Samsung-specific feature set
- it reduces the temptation to keep every Lawnchair feature

## Suggested layers

1. `prototype-android`
   Purpose:
   - fast UI iteration
   - motion experiments
   - settings and layout parity work

2. `docs`
   Purpose:
   - target definition
   - parity checklist
   - interaction rules

3. `backlog`
   Purpose:
   - implementation sequencing
   - PR-sized milestones

## Migration strategy

1. Prove visual shell parity
2. Prove settings parity
3. Prove home and drawer interaction parity
4. Decide whether to:
   - continue as a standalone launcher
   - port selected work back into Lawnchair Lite

## Guardrails

- Do not change the root repo settings to include the clone prototype by default
- Do not reuse Lawnchair-only features unless the Samsung clone actually needs them
- Keep app package names, build files, and source roots independent
