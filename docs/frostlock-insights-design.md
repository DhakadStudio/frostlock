# FrostLock Insights — Data Visualization & Insights Experience

**Date:** 2026-11-17
**Status:** Design proposal for the Insights tab of the FrostLock redesign
**Scope:** Information hierarchy, metric ownership, chart system, screen specs, empty states, offline/Compose implementation notes
**Related:** `docs/frostlock-redesign-plan.md`, `app/src/main/java/com/frostlock/app/ui/screens/stats/`, `report/`, `calendar/`, `home/`

> **Status:** companion spec to plan v3. Plan v3 is authoritative for IA, routes and naming; this document is authoritative for data visualisation and chart implementation. Where labels differ, use the plan’s canonical vocabulary:
>
> | In this document | Canonical |
> |---|---|
> | Diary | **Reflections** (notes archive) |
> | Focus calendar | **History** (calendar + full session list) |
> | Weekly report | **Weekly review** |
> | Achievements | **Badges** |
> | Freeze levels | **Levels** |
> | Stats / Statistics | **Trends** |
> | Focus mix | Focus mix (unchanged) |
>
> Also superseded: the assumption that drill-downs hide the bottom bar (they keep it), and “Diary owns the full session archive” (History does; Reflections owns notes).

---

## 1. Executive summary

Insights should answer four questions in this order:

1. **Now** — “How am I doing today?” (today focus, goal, current streak)
2. **Rhythm** — “Is this week working?” (this week's bars, week-over-week change)
3. **Pattern** — “What should I change?” (subjects, months, calendar, best days)
4. **Reward / reflection** — “What did I earn, and what did I learn?” (freeze level, badges, diary)

The design uses **progressive disclosure**, not a dashboard of every number:

- **InsightsHome** is a summary: 1 hero, 1 week chart, 1 focus-mix strip, 1 consistency strip, 2 small momentum cards, 3 recent sessions.
- **Drill-downs** own the detail: **Trends** (patterns), **Weekly report** (one week + improvement), **Focus calendar** (when), **Achievements**, **Freeze levels**, **Diary**.
- **One metric has one owner.** A parent may restate a metric as a compact teaser, but the same metric at the same altitude must never appear twice.

Everything is computed from Room + DataStore on the device. No network, no account, no new permissions, no third-party chart library. Charts are Compose layouts and `Canvas`.

---

## 2. Design principles

### 2.1 One metric, one owner

Every metric gets a canonical screen and a canonical definition. Other screens may show a **summary** of it only when that summary is the entry point to the owner.

- Allowed: InsightsHome shows “6h 40m this week” → Weekly report shows the same week with daily detail.
- Not allowed: Stats shows “Sessions 120” and Home also shows “Sessions 120” as two competing tiles.
- Not allowed: the same all-time subject breakdown on both InsightsHome and Weekly report.

### 2.2 Altitude rule

| Altitude | Screen | Job | Content density |
|---|---|---|---|
| 0 | InsightsHome | Status + this week + entry points | 4–6 numbers max |
| 1 | Trends / Weekly report / Calendar | Explain one dimension | One chart + one table |
| 2 | Diary / Achievements / Levels | Archive or reward | Full list / full cabinet |

If a number appears at altitude 0, altitude 1 may restate it only as context for its own dimension. It may not introduce a second version of the same number.

### 2.3 One question per card

Each card answers exactly one question. No card mixes “focus time + unlocks + achievements + a navigation link”. That was the core problem with the old Stats screen.

### 2.4 Show change, not just totals

A total without a comparison is trivia. Every headline number should have a reference where one exists:

- Today vs daily goal
- This week vs last week
- Current week vs the user's first 7 days (baseline)
- Month vs previous month

### 2.5 Zero is a state, not a chart

Never render a wall of zero-height bars as if it were data. A brand-new user sees a purposeful baseline card, not an empty dashboard. A returning user with a quiet week sees the week chart with an explicit “No focus this week” caption and a CTA.

### 2.6 Local-first, no fake data

- No sample/demo bars.
- No skeleton charts that look like real data.
- No “syncing” or “cloud” language.
- Sharing is user-initiated via `ACTION_SEND`; nothing leaves the device automatically.

### 2.7 Color means something

- Ice gradient = focus / current.
- Subject colors = identity only.
- `SuccessGreen` / `DangerRed` = improvement deltas only.
- Low-focus days are neutral, never red. Missing a day is not an error.

---

## 3. Information architecture and navigation

### 3.1 Bottom navigation

Four destinations, unchanged:

**Focus · Shield · Insights · You**

Insights is the only bottom-tab entry point for progress, reports, diary, achievements and levels.

### 3.2 Route tree

```
Insights (tab)
├── insights/trends              Trends: 7D / 30D / 6M, subjects
├── insights/report?week=YYYY-MM-DD   Weekly report (week switcher)
├── insights/calendar?month=YYYY-MM   Focus calendar heat-map
├── insights/achievements        Badge cabinet
├── insights/levels              Freeze ladder
└── insights/diary?from=&to=     Study diary (optional week filter)
```

Bottom bar visible only on `insights`. Drill-downs show a back arrow and hide the bottom bar, matching the redesign plan's hidden-route list.

### 3.3 Entry points

| From | To | Trigger |
|---|---|---|
| InsightsHome hero | Focus tab | “Start a session” / empty CTA |
| InsightsHome week card | Weekly report | Card tap or “Weekly report →” |
| InsightsHome focus mix | Trends | “See all subjects →” |
| InsightsHome consistency strip | Calendar | Card tap |
| InsightsHome level card | Freeze levels | Card tap |
| InsightsHome next badge | Achievements | Card tap |
| InsightsHome recent sessions | Diary | “Diary →” |
| Weekly report top subject | Trends | “Subject trends →” |
| Weekly report reflections | Diary (week-filtered) | “Open diary →” |
| Trends 6M month bar | Calendar (that month) | Bar tap + “Open month” |
| Trends 7D bar | Weekly report | Selected-day action (optional) |
| Calendar day | Day detail inline | Cell tap |

No entry point from Profile, Home or Stats. The old navigation hubs are removed as part of the redesign.

---

## 4. Metric ownership matrix

This is the contract that prevents duplicate stats.

| Metric | Canonical definition | Full-detail owner | Summary appearances | Removed from |
|---|---|---|---|---|
| **Today focus** | Sum of focus seconds whose day bucket is today (see §5.1) | InsightsHome hero | — | Stats, Home, Focus history |
| **Daily goal** | `FocusSettings.dailyGoalMinutes`; goal hit = today ≥ goal | InsightsHome hero | — | Profile (setting only, not a stat) |
| **Current streak** | Consecutive local days with >0 focus, ending today or yesterday | InsightsHome hero chip | — | Stats, Weekly report, Focus dialog (keep only the post-save confirmation) |
| **This week focus** | Monday 00:00 → now, local | Weekly report hero | InsightsHome week card | Stats, Home |
| **Weekly bars** | Mon–Sun daily totals, future days zero | Weekly report | InsightsHome mini bar chart | Stats “Last 7 days” duplicate |
| **Today session count** | Saved sessions ending today | InsightsHome hero subtitle | Weekly report / Trends for their periods | “Sessions” all-time tile |
| **Lifetime session count** | All saved sessions with duration > 0 | Achievements (session badges) | Levels context only if needed | Home, Stats |
| **Average session** | Period seconds ÷ period sessions | Weekly report | — | Stats tile |
| **Monthly rhythm** | Last 6 calendar months, monthly totals | Trends (6M period) | — | Stats separate card |
| **Subject breakdown** | Group by tag, top 5 + Other, durations and shares | Trends | InsightsHome 100% composition strip; Weekly report “Top subject” one-liner | Stats, Weekly report full list |
| **Focus calendar** | Month heat-map, 5 intensity levels | Focus calendar | InsightsHome 12-week consistency strip (different period) | Stats calendar link card only |
| **Recent sessions** | Newest saved sessions | Diary (full archive) | InsightsHome 3 rows | Stats recent list, Focus persistent list |
| **Weekly baseline/improvement** | First 7 days vs latest 7 days, non-overlapping | Weekly report improvement card | InsightsHome week card delta is **week-over-week**, a different metric | Stats |
| **Achievements** | 16 badges from lifetime focus, best streak, session count | Achievements | InsightsHome “next badge” only | Stats 4-badge preview |
| **Freeze level** | Lifetime minutes → 7-title ladder | Freeze levels | InsightsHome level + progress | Stats level card (replace) |
| **Reflections** | Sessions with non-blank note in period | Diary / Weekly report count | Weekly report count + link | Stats |
| **Best day** | Highest-focus day in the period | Weekly report (week), Calendar (month) | — | Home |
| **Active days** | Days with >0 focus in period | Trends / Weekly report / Calendar month | Consistency strip “active days in 12 weeks” | Home tiles |
| **Phone unlocks** | UnlockStore count | **Shield** (protection outcome) | — | Insights entirely |
| **Completed sessions** | `completed = true` | Trends/Report detail only | — | Home |
| **Lifetime focus total** | Sum of all focus seconds | Freeze levels | — | Home, Stats caption |

### 4.1 Why phone unlocks move out

“Unlocks today” is a protection metric, not a focus insight. It belongs on ShieldHome next to limits and blocking status. Keeping it in Insights mixes two mental models and was one of the old Stats screen's five concerns.

### 4.2 Why “Sessions” disappears as a tile

An all-time session count is not actionable. Session count is useful only in context:

- Today: “3 sessions today” inside the hero.
- This week: Weekly report.
- Lifetime: a badge requirement in Achievements.

A standalone “Sessions 120” tile adds no decision.

---

## 5. Canonical data contract

Before building charts, fix the definitional drift. Today the same user can see different totals on different screens.

### 5.1 Day bucketing (the important fix)

Current behaviour:

- `StatsViewModel.buildDays` groups by `startedAt`.
- `WeeklyReport` groups by `endedAt` (`weekDate()`).
- `Calendar` groups by `startedAt`.
- `MonthlyStats` groups by `startedAt`.
- `computeStreak` uses `startedAt`; `ImprovementReport` uses `endedAt`.

A session that crosses midnight can therefore count on different days in different screens.

**Decision:** introduce one canonical helper, `FocusDayBuckets`, used by every insight.

- **Duration** is split across local midnights. A 23:30–00:30 session contributes 30 minutes to each day.
- **Session count** is attributed to the day the session **ended**, matching the diary's “finished on” mental model.
- **Streak / active day** uses days with duration > 0 after splitting.
- All screens use this helper; no screen groups raw `startedAt` or `endedAt` independently.

If splitting is too large for the first migration, the fallback is “attribute the whole session to the end day” everywhere. Do not ship a mix of conventions.

### 5.2 Week

- Monday 00:00 to Sunday 23:59, local time.
- “This week” includes future days as zero-value days so the bar chart has a stable 7-column shape.
- A rolling “last 7 days” exists only in Trends and must be labelled **“Last 7 days”**, never “This week”.
- Week start is fixed Monday for consistency with the current report. If locale-aware weeks are added later, the label must state the range.

### 5.3 Session

- A **saved session** is a row in `focus_sessions` with `durationSeconds > 0`.
- `completed` is a separate flag (planned duration reached). Do not call every saved session “completed”.
- Discarded sessions are not saved and never appear in insights.

### 5.4 Streak

- **Current streak:** consecutive days with focus, counting back from today; if today has no focus yet but yesterday did, the streak is still alive (matches current `computeStreak`).
- **Best streak:** longest run in history. Used only by Achievements and the improvement report.
- Labels must distinguish them: “Current streak” vs “Best streak”.

### 5.5 Goal and active day

- Goal seconds = `dailyGoalMinutes × 60`, default from `FocusSettings`.
- Goal hit = day seconds ≥ goal seconds.
- Active day = day seconds > 0, not “goal hit”.

### 5.6 Subject

- `tag.trim()`; blank → `"Untagged"`.
- Top 5 by duration, remainder aggregated as `"Other"`.
- Percentages use total seconds; display rounds to whole percent; shares may not sum to 100% due to rounding — do not force them.
- Colors come from `SubjectStore`; unknown tags use the ice accent.

### 5.7 Rounding and formatting

- Store and aggregate in seconds; format only at the edge with `formatDuration`.
- `formatDuration(0)` returns `"0s"`; **do not use it for empty stats**. Use `"—"` or hide the value.
- Percentages and deltas round with `roundToInt`.
- Durations under a minute may display `<1m` in charts where space is tight.

### 5.8 Timezone, DST, midnight

- Use `ZoneId.systemDefault()` for all local dates.
- Split sessions with `Instant` + `ZonedDateTime`, not fixed 86 400-second arithmetic.
- Recompute at midnight and on app resume. The current ViewModels capture `todayStart` once at construction; an app left open overnight shows yesterday's “Today”. Fix with a `todayFlow`/resume refresh.

### 5.9 Data freshness and query limits

- A session insert must update all insight flows.
- Do not use `recentSessions(2000)` as the only source for calendar/monthly views; a heavy user's older months become incomplete. Use range queries (`sessionsBetween(start, end)`) or a full history query for lifetime aggregates.
- The current Stats uses `recentSessions(500)` for monthly/streak/achievements; that silently caps history. Replace with period queries plus SQL `SUM`/`COUNT` for lifetime totals.

### 5.10 Proposed shared model

```kotlin
data class InsightsSnapshot(
    val generatedAt: Instant,
    val today: DayFocus,
    val week: PeriodFocus,
    val last7: PeriodFocus,
    val months: List<MonthFocus>,       // last 6, oldest → newest
    val subjects: List<SubjectFocus>,   // period-scoped, top 5 + Other
    val days: Map<LocalDate, DayFocus>, // canonical buckets, bounded window
    val recent: List<SessionEntity>,    // 3–10 for summaries
    val level: FocusLevel,
    val achievements: List<Achievement>,
    val improvement: ImprovementReport,
    val reflections: Int
)
```

One `InsightsRepository` produces this from a single session read plus `FocusSettings`/`SubjectStore`. ViewModels derive screen-specific states; they do not re-query and re-aggregate independently.

---

## 6. Visual and chart system

### 6.1 Layout tokens

| Token | Value |
|---|---|
| Screen horizontal padding | 20dp |
| Card internal padding | 16dp |
| Card gap | 16dp |
| Section gap | 24dp |
| Card shape | `MaterialTheme.shapes.medium` (20dp) |
| Hero shape | `MaterialTheme.shapes.large` (26dp) |
| Chart height | 140–170dp |
| Bar corner | 8dp top |
| Bar gap | 8dp (7 bars), 4dp (30 bars) |
| Heat-map cell | 40–44dp (month), 12dp (12-week strip) |
| Touch target | ≥48dp for every interactive chart element |

### 6.2 Typography

Use the existing theme:

- Hero number: `displayMedium` (32sp, bold)
- Card title: `titleLarge` (17sp)
- Section eyebrow: `labelMedium` (11sp, uppercase, letter-spaced)
- Supporting text: `bodyMedium` (13sp)
- Chart labels: `labelMedium` / `bodySmall`
- Do not use `displayLarge` for a stat inside a card; it dominates the screen.

### 6.3 Chart inventory

| Question | Chart | Where | Why this chart |
|---|---|---|---|
| How close am I to today's goal? | Radial progress ring | InsightsHome hero | One bounded value; ring reads instantly |
| What did my week look like? | 7 vertical bars, today highlighted | InsightsHome, Weekly report | 7 discrete days; zeros are meaningful; easy to tap |
| What is my long-term rhythm? | 6 monthly bars | Trends (6M) | Discrete months; comparison across months |
| What did I work on? | 100% stacked bar + ranked horizontal bars | InsightsHome (summary), Trends (detail) | Part-to-whole first, then precise comparison |
| When do I focus? | Month heat-map, 5 levels | Focus calendar | Calendar shape reveals weekday/weekend patterns |
| Am I consistent? | 12-week dot strip | InsightsHome | Long horizon in a small space; entry to calendar |
| Am I improving? | Delta chips + paired baseline/current bars | Weekly report | Change is the message; paired bars anchor it |
| What have I earned? | Badges + linear progress | Achievements, Levels | Progress, not a chart |
| What did I learn? | Grouped list | Diary | Qualitative, not quantitative |

**Avoid:** pie/donut charts (hard to compare, poor accessibility), dual-axis charts, 3D, gauges other than the single goal ring, and line charts for 7 points.

### 6.4 Color

- **Focus/current:** `PrimaryIce` → `GlacierBlue` gradient.
- **Other bars:** `PrimaryIce.copy(alpha = 0.55f)` / `GlacierBlue.copy(alpha = 0.35f)`.
- **Heat-map ramp:** `SurfaceHigh` (0) → `PrimaryIce.copy(alpha = .35f)` (1) → `.55f` (2) → `.75f` (3) → full `PrimaryIce`/`AccentFrost` (4). Five steps, never a continuous rainbow.
- **Subjects:** saved subject color; unknown/untagged = `AccentFrost`.
- **Deltas:** `SuccessGreen` up, `DangerRed` down, `TextSecondary` flat.
- **Never** color a zero/low day red; it is neutral.

### 6.5 Interaction

- **Week bars:** tap selects a day; a readout below the chart updates (“Wed · 1h 20m · 2 sessions”). Default selection = today. Do not navigate on bar tap; the card footer navigates.
- **Month bars:** tap selects/opens that month in Calendar.
- **Calendar cells:** tap selects; selected day detail appears inline below the grid.
- **Period segmented control (Trends):** `AnimatedContent` crossfade; preserve selection in `SavedStateHandle`.
- **No hover-only or long-press-only data.** Every value is available by tap and to screen readers.

### 6.6 Motion

- Bars animate height once on first composition: 400–500ms spring/tween.
- Period switch: 200ms crossfade + slide 8dp.
- Selected day readout: 150ms fade.
- No looping/pulsing charts. Respect the system animator duration scale; if animations are disabled, render final values immediately.

### 6.7 Accessibility

- Each bar/cell has a `contentDescription` with full date, duration and sessions.
- The chart container has a summary description (“This week: 6 hours 40 minutes across 5 days”).
- Never rely on color alone: heat-map includes a “Less → More” legend and selected-day text.
- Minimum 48dp touch targets; bars can be visually 8dp wide but the tap column is the full 48dp height.
- Support font scale 200%: chart labels outside the plot area; use `maxLines = 1`, ellipsis, and `contentDescription` for truncated labels.
- Contrast: white text on `PrimaryIce` must use `InkOnIce` where required.

### 6.8 Responsive

- Phone: single column, full-width cards.
- ≥600dp: content max width 720dp centered; momentum cards stay 2-up; charts can grow to 200dp height.
- Do not put two charts side by side on a phone.
- Use `LazyColumn` for screens with lists; never nest vertical scrolling.

---

## 7. InsightsHome — screen spec

**Route:** `insights`
**Purpose:** Answer “how am I doing now, and is this week working?” and route to detail.
**Bottom bar:** visible.
**Data:** `InsightsHomeViewModel` from `InsightsRepository`.

### 7.1 Wireframe

```
┌──────────────────────────────────────┐
│ Insights                    Nov 17   │
│ Your focus, in perspective.          │
│                                      │
│ ┌──────────────────────────────────┐ │
│ │ TODAY                 12-day ❄  │ │
│ │        ╭────────╮                │ │
│ │        │ 2h 15m │  goal ring     │ │
│ │        ╰────────╯                │ │
│ │  of 3h goal · 3 sessions         │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌ This week ────────────── 6h 40m ┐ │
│ │  +12% vs last week               │ │
│ │  ▁ ▃ ▅ ▂ ▇ ▄ ▁                   │ │
│ │  M T W T F S S                   │ │
│ │  Wed · 1h 20m · 2 sessions       │ │
│ │  Weekly report →                 │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌ Focus mix ───────────── See all ┐ │
│ │  ███████████░░░░░░               │ │
│ │  ● Physics 45%  ● Math 30%       │ │
│ │  ● Work 15%     ● Other 10%      │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌ Consistency ────────────────────┐ │
│ │  ▪▪▫▪▪▪▫ ▪▪▫▪▪▪▫ … 12 weeks   │ │
│ │  18 active days · last 12 weeks  │ │
│ └──────────────────────────────────┘ │
│                                      │
│ ┌ Freeze level ──┐ ┌ Next badge ──┐ │
│ │ Lv 3 Glacier   │ │ Focus 10h    │ │
│ │ █████░░ 42m    │ │ 7h20 / 10h   │ │
│ └────────────────┘ └──────────────┘ │
│                                      │
│ Recent sessions            Diary →  │
│ [session] [session] [session]        │
│                                      │
│ All insights stay on this device.    │
└──────────────────────────────────────┘
```

### 7.2 Section-by-section

#### A. Header

- Eyebrow: current date, e.g. `MONDAY, 17 NOVEMBER` (`labelMedium`, `AccentFrost`).
- Title: `Insights` (`headlineMedium`).
- Subtitle: `Your focus, in perspective.` (`bodyMedium`, `TextSecondary`).
- No filter chips here; period switching belongs in Trends.

#### B. Today hero

- Container: gradient hero, `shapes.large`, 20dp padding, 1dp `StrokeSoft`.
- Left/top: eyebrow `TODAY`; streak chip top-right: `❄ 12-day streak`.
- Center: `GoalRing` 140–160dp, 12dp stroke, gradient arc; center shows today duration in `displayMedium`, e.g. `2h 15m`.
- Under the ring: `of 3h goal · 3 sessions today` (`bodyMedium`, `AccentFrost`).
- Goal reached: arc full, copy `Goal reached · 3 sessions today`; no confetti, no extra card.
- Today zero but history exists: `0m` + `Start a session to keep your 12-day streak` + primary CTA.
- Streak chip is a **chip**, not a second ring. The current `StreakRing` conflates streak and goal progress; split it into `GoalRing` + `StreakChip`.

#### C. This week card

- Title row: `This week` (`titleLarge`) + total `6h 40m` (`titleLarge`, `AccentFrost`).
- Second line: range `11–17 Nov` (`bodySmall`) + `DeltaChip` (`+12% vs last week`). If last week = 0, show `Fresh start`; never `+∞%`.
- Chart: 7 bars, 150dp height, Monday-first, today highlighted with full gradient, future days as empty tracks. Zero days show a 2dp baseline track, not a zero-height invisible bar.
- Selected-day readout under the chart: `Wed · 1h 20m · 2 sessions`.
- Footer: `Weekly report →` (`TextButton`), entire card clickable to the report.
- Empty (history exists, no focus this week): bars remain with zero tracks; readout becomes `No focus this week · Last focus 3 days ago`; footer still opens the report. Do not hide the card, because the week is a real period.

#### D. Focus mix card

- Purpose: a one-glance answer to “what did I work on?”, not a replacement for Trends.
- 100% stacked bar, 10dp height, rounded 5dp, segments in subject colors, 2dp gaps.
- Legend: up to 3 subjects + `Other`, each as `● Name 45%`. **No durations** in the home legend; durations belong to Trends.
- Footer: `See all subjects →` opens Trends.
- All sessions untagged: show a single `Untagged` segment and hint `Tag sessions to see your mix.`; do not hide the card if there is focus time.
- No focus in the selected period (week): show the card with an empty state? Prefer hiding the card when the week has zero focus; the week card already explains the quiet week.

#### E. Consistency strip

- Title `Consistency`; caption `18 active days · last 12 weeks`.
- 12 columns × 7 rows of 12dp cells (3dp gap), Monday-first; columns = weeks, rows = weekdays.
- Intensity uses the same 5-step ramp as the calendar.
- Footer/whole card opens Calendar.
- This is a **different period** from the month calendar, so it is not duplicate data.
- Empty (no sessions ever): hide.

#### F. Momentum row

Two compact cards side by side (stack at large font scale):

1. **Freeze level**
   - Icon `AcUnit`; eyebrow `FREEZE LEVEL`; `Level 3 · Glacier`; linear progress bar; `42m to level 4`; tap → Levels.
   - Do not show lifetime total here; Levels owns it.
2. **Next badge**
   - Badge icon; eyebrow `NEXT BADGE`; title `Focus 10h`; progress `7h 20m / 10h`; small `3 of 16`; tap → Achievements.
   - Only the **next** badge, not a 4-badge preview. This removes the old duplication with the cabinet.

#### G. Recent sessions

- Section header `Recent sessions` + `Diary →`.
- Show 3 `SessionRow`s (compact variant: mode/tag, time, duration; note only if present).
- Tap a row → Diary (or future session detail).
- Empty: `No sessions yet. Your first one will appear here.` + `Start focus` CTA.
- The full list lives in Diary; do not add “View all sessions” that duplicates Diary's job.

#### H. Footer

- `All insights are calculated on this device.` (`bodySmall`, `TextSecondary`).
- No sync icon, no “last updated” timestamp.

### 7.3 Home states

| State | What renders | What is hidden |
|---|---|---|
| Loading | Header + 2 skeleton cards (no fake bars) | Charts, numbers |
| Brand new (0 sessions) | Header + `BaselineCard` + `What you'll see` checklist + recent empty | Hero ring, week, mix, consistency, momentum |
| History, no focus today | Hero 0m + streak message; all other cards | — |
| History, no focus this week | Week zero tracks + quiet-week caption; mix hidden; consistency visible | Mix |
| No subjects | Mix shows Untagged + hint | Subject legend rows |
| Local DB error | Error card `Couldn't read your local history.` + Retry | All charts |

### 7.4 Baseline card (brand-new)

- Eyebrow `YOUR BASELINE`.
- Title `Your baseline starts today`.
- Body `Focus for 7 days and FrostLock will show how your focus time, sessions and streak improve. Everything stays on this device.`
- Primary CTA `Start your first session`.
- Secondary checklist, not fake charts:
  - `Today's focus and goal`
  - `Your weekly rhythm`
  - `A calendar of every focus day`
- No level/badge card until the first session; level 1 with zero progress is not motivating.

---

## 8. Drill-down screen specs

### 8.1 Trends

**Route:** `insights/trends`
**Purpose:** Explain patterns over time and by subject. Replaces the old “Statistics” screen.
**Owner of:** monthly rhythm, full subject breakdown, period totals (7D/30D/6M).

**Layout**

1. Header: back arrow, `Trends`, subtitle `Patterns behind your focus.`
2. Period segmented control: `7 days` · `30 days` · `6 months`. Default `7 days`.
3. Summary strip: `Total` / `Active days` / `Sessions` / `Avg / active day`. Four values, no tiles with icons; a single row of `MetricValue` blocks separated by whitespace. Avoid `StatTile` card-in-card.
4. Main chart:
   - 7D: 7 bars, day labels, today highlighted.
   - 30D: 30 thin bars, labels every 5 days, selected-day readout; no per-bar values.
   - 6M: 6 month bars, current month highlighted; tap a month → Calendar at that month.
   - Chart title and total change with period: `Last 7 days`, `Last 30 days`, `Last 6 months`.
5. `By subject` card:
   - 100% stacked bar.
   - Ranked rows: color dot, subject, duration, percent; top 5 + Other.
   - Rows are not clickable in v1; a future filter can be added.
6. `Best day` card (period-scoped): date, duration, sessions. In 6M, best month instead of best day.
7. Footer: offline note.

**Rules**

- Do not show “Today” in Trends; today is the home hero.
- Do not show a separate “Monthly rhythm” card in addition to the 6M chart. The 6M period **is** the monthly rhythm.
- Do not show current streak or achievements here.
- `Avg session` is not shown; it belongs to the Weekly report.

**Empty states**

| Period | Condition | State |
|---|---|---|
| 7D | No focus in last 7 days but history exists | Chart zero tracks + `No focus in the last 7 days. Last focus 3 days ago.` + CTA |
| 30D | Same | `No focus in the last 30 days.` + `Open calendar` |
| 6M | No focus in 6 months | `Your monthly rhythm will appear after your first session.` |
| Subjects | All untagged | `Untagged` row + `Tag sessions to compare subjects.` |
| Brand new | 0 sessions | One baseline card + CTA; hide segmented control and charts |

### 8.2 Weekly report

**Route:** `insights/report?week=YYYY-MM-DD`
**Purpose:** One week, in depth, with week-over-week and baseline comparison.
**Owner of:** week total, daily bars, avg session, active days, reflections count, best day, baseline/improvement.

**Layout**

1. Header: back arrow, `Weekly report`, subtitle `Your focus, one week at a time.`
2. Week switcher card: `‹ This week ›`, range `11–17 Nov`, next disabled on current week.
3. Hero: `FOCUSED THIS WEEK`, total `6h 40m`, `5 sessions across 4 days`, `ChangeChip` vs previous week. If previous week = 0, `Fresh start`.
4. Improvement card (current week only):
   - Brand new: baseline invitation + CTA.
   - Building baseline: `Day 3 of 7` + progress.
   - Baseline saved, comparison locked: `First comparison unlocks in 4 days.`
   - Ready: `Compared with your first 7 days` + 5 metric rows (focus time, sessions, active days, goal hit, best streak) with delta chips. Focus time row may include paired baseline/current mini bars; other rows stay text + chip.
5. `Daily rhythm` card: 7 bars, best day highlighted, values above bars (there is room in the full report), today highlighted if current week.
6. Stats row: `Sessions`, `Avg session`, `Active days`, `Reflections`. These are week-scoped and owned here. Two 2-up rows on phone.
7. `Best day` card: date, duration, sessions.
8. `Top subject` one-liner: `Top subject: Physics · 2h 10m · 45%` + `Subject trends →` (Trends). Do **not** repeat the full subject breakdown here.
9. Reflections card: `3 reflections this week` + `Open diary →` (week-filtered if supported).
10. Footer: offline note.

**Empty states**

- Brand new current week: baseline card only; no hero, no bars.
- Past week with no sessions: `No focus logged that week.` + `Start a focus session`; week switcher remains so the user can browse.
- Current week with no sessions but history: hero 0m + `No focus this week yet.` + CTA; improvement card still shows baseline status.
- No reflections: `No reflections yet. Add a note after your next session.` + Diary link.

### 8.3 Focus calendar

**Route:** `insights/calendar?month=YYYY-MM`
**Purpose:** Answer “when do I focus?” and let the user browse any day.
**Owner of:** month heat-map, month totals, selected-day sessions.

**Layout**

1. Header: back, `Focus calendar`, subtitle `Every day you froze out the noise.`
2. Month card:
   - `‹ November 2026 ›`; next disabled at current month.
   - Summary line: `6h 40m · 4 active days`.
   - Weekday header `M T W T F S S`.
   - 6×7 grid; 40–44dp cells; intensity ramp; today outlined; selected 2dp `AccentFrost` border.
   - Legend: `Less` · 5 swatches · `More`.
3. Selected-day header: date (`Wednesday, 12 November`), total, session count; `Best day` chip if it is the month's best.
4. Day sessions: `SessionRow` list; empty message `No focus on this day. Pick another day or start a session.`
5. Footer: offline note.

**Rules**

- Do not show weekly bars or subject breakdown here.
- Month summary is allowed because its scope is the calendar month.
- Tapping a leading/trailing day moves to that month (existing behaviour).
- Initial month comes from the `month` argument; default current month.

**Empty states**

- Month with no focus: grid still renders (a calendar is useful even when empty), caption `No focus in November. Last focus was 12 October.` + CTA.
- Brand new: grid + `Your first focus day will light up here.` + CTA.
- Future month: not reachable (next disabled).
- Day with no sessions: message + CTA.

### 8.4 Achievements

**Route:** `insights/achievements`
**Purpose:** Reward cabinet and next milestone.
**Owner of:** 16 badges, best streak, lifetime focus/session badge progress.

**Layout**

1. Header: back, `Achievements`, subtitle `Milestones from your focus history.`
2. Hero: `7 of 16 unlocked`; linear progress; `Next: Focus 10h · 3h 20m to go`.
3. Category sections: `Focus milestones` (7), `Streak milestones` (5), `Session milestones` (4). Grid of 3 columns; unlocked = full color, locked = desaturated with progress ring/bar.
4. Badge detail dialog: title, description, progress `7h 20m / 10h`, unlocked date is not stored — do not invent one. Share button for unlocked badges.
5. Footer: offline note.

**Rules**

- Labels say `Best streak`, never `Current streak`.
- Do not show current streak here; home owns it.
- Do not show freeze level here.

**Empty state**

- 0 sessions: `Your first badge is one session away.` + progress `0 / 1 session` + CTA. Show locked badges as silhouettes, not hidden.

### 8.5 Freeze levels

**Route:** `insights/levels`
**Purpose:** Long-term identity and progress.
**Owner of:** lifetime focus total, level ladder, share text.

**Layout**

1. Header: back, `Freeze levels`, subtitle `Titles earned from lifetime focus.`
2. Hero: current level `Level 3 · Glacier`; lifetime total `5h 20m`; progress bar; `42m to level 4`.
3. Share button: `Share my level` (user-initiated `ACTION_SEND`).
4. Ladder: 7 rows, each with title, minutes required, reached check or lock. Current row highlighted.
5. Explanation: `Each title needs more focus time than the last. Progress is measured from every saved session on this device.`
6. Footer: offline note.

**Rules**

- Home shows level + next-level progress only; Levels owns lifetime total and the full ladder.
- No achievements on this screen.

**Empty state**

- Level 1, 0m: hero `Level 1 · Frostbite`, `1h to level 2`, ladder visible. CTA `Start your first session`.

### 8.6 Diary

**Route:** `insights/diary?from=&to=`
**Purpose:** Qualitative archive; not an analytics screen.
**Owner of:** full session list, reflections, search, photos.

**Rules**

- Remove `focusedSeconds` from the Diary hero if it duplicates InsightsHome/Weekly report; keep `X reflections across Y days`.
- Diary is the only full session archive. Home shows 3; Weekly report links here.
- Week-filtered entry from the report is optional but recommended (`from`/`to` args), so “3 reflections this week” opens the right subset.
- Empty: `No reflections yet. Sessions without notes still count. Add a note after your next session.` + `All sessions` toggle + CTA.

---

## 9. Empty, partial, loading and error states

### 9.1 Principles

1. Distinguish **never had data** from **no data in this period**.
2. Never show a chart frame full of zeros for a brand-new user.
3. Always offer one primary action: start focus.
4. No guilt copy (“You failed”, “You broke your streak”).
5. No fake sample data.
6. Loading is not empty: use a skeleton with neutral blocks, not zero values.

### 9.2 State matrix

| Screen | Condition | Copy | Visual | CTA |
|---|---|---|---|---|
| InsightsHome | 0 sessions ever | `Your baseline starts today` | Baseline hero + checklist | Start your first session |
| InsightsHome | 0 today, history exists | `Start a session to keep your 12-day streak` | Hero 0m, ring empty | Start focus |
| InsightsHome | 0 this week, history exists | `No focus this week · Last focus 3 days ago` | Week zero tracks | Start focus |
| InsightsHome | No subjects | `Tag sessions to see your mix.` | Untagged segment | Start a tagged session |
| Trends 7D | 0 in period | `No focus in the last 7 days. Last focus 3 days ago.` | Zero tracks | Start focus / Open calendar |
| Trends 30D | 0 in period | `No focus in the last 30 days.` | Zero tracks | Open calendar |
| Trends 6M | 0 in period | `Your monthly rhythm will appear after your first session.` | Empty state card | Start focus |
| Trends subjects | All untagged | `Untagged` row + hint | Single row | Start a tagged session |
| Weekly report | 0 ever | `Your baseline starts today` | Baseline card | Start focus |
| Weekly report | 0 that week | `No focus logged that week.` | Empty card, week switcher | Start focus |
| Weekly report | No reflections | `No reflections yet. Add a note after your next session.` | Small empty row | Open diary |
| Calendar | Month empty | `No focus in November. Last focus was 12 October.` | Grid + caption | Start focus |
| Calendar | Day empty | `No focus on this day.` | Day header + empty card | Start focus |
| Achievements | 0 sessions | `Your first badge is one session away.` | Locked silhouettes + 0/1 | Start focus |
| Levels | 0 minutes | `Level 1 · Frostbite` / `1h to level 2` | Ladder | Start focus |
| Diary | No notes | `No reflections yet. Sessions without notes still count.` | Empty card + All sessions toggle | Start focus |
| Any | DB read error | `Couldn't read your local history.` | Error card | Retry |
| Any | Loading | — | Neutral skeleton | — |

### 9.3 Copy rules

- Use “focus” not “productivity” or “performance”.
- Use “session” for a saved focus block.
- Use “current streak” / “best streak” explicitly.
- Avoid “You should”; prefer “Start a session to…”.
- Date ranges use the app locale; duration uses `formatDuration`.
- Pluralize correctly: `1 session` / `2 sessions`, `1 day` / `2 days`.

---

## 10. Offline and Compose implementation

### 10.1 Offline guarantees

- Room (`focus_sessions`) and DataStore (`FocusSettings`, `SubjectStore`, `UnlockStore`) are the only sources.
- No network calls, no accounts, no uploads, no remote chart images.
- Sharing uses the system share sheet only when the user taps Share.
- No new permissions; charts are pure Compose.
- Data survives process death and app restarts; insights are recomputed, never stored separately (so they cannot drift).

### 10.2 Package layout

```
ui/screens/insights/
  InsightsHomeScreen.kt
  InsightsHomeViewModel.kt
  TrendsScreen.kt
  TrendsViewModel.kt
  InsightsNavigation.kt          // routes/args if not in Screen.kt
ui/components/insights/
  GoalRing.kt
  StreakChip.kt
  WeeklyBarChart.kt
  MonthBarChart.kt
  SubjectMixBar.kt
  SubjectBreakdownList.kt
  ConsistencyStrip.kt
  MonthHeatmap.kt
  DeltaChip.kt
  MetricComparisonRow.kt
  InsightCard.kt
  EmptyInsightCard.kt
domain/insights/
  InsightsRepository.kt
  InsightsSnapshot.kt
  FocusDayBuckets.kt
  PeriodAggregator.kt
```

Reuse `AppCard`, `SectionHeader`, `SessionRow`, `AchievementBadge`, theme tokens.

### 10.3 State model

```kotlin
sealed interface InsightsHomeState {
    data object Loading : InsightsHomeState
    data object Empty : InsightsHomeState
    data class Ready(val snapshot: InsightsSnapshot) : InsightsHomeState
    data class Error(val message: String) : InsightsHomeState
}
```

Use `Loading` until the first Room emission. Do not initialize with `todaySeconds = 0` and render an empty state immediately; that causes a flash of “no data”.

### 10.4 Repository sketch

```kotlin
@Singleton
class InsightsRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val subjectStore: SubjectStore,
    private val focusSettings: FocusSettings,
    private val clock: Clock
) {
    val snapshot: Flow<InsightsSnapshot> = combine(
        sessionDao.allSessions(),          // or bounded range queries
        focusSettings.dailyGoalMinutes,
        subjectStore.subjects(),
        clock.todayFlow()                  // emits at midnight + on resume
    ) { sessions, goal, subjects, today ->
        withContext(Dispatchers.Default) {
            buildSnapshot(sessions, goal, subjects, today)
        }
    }.distinctUntilChanged()
}
```

- One read, one aggregation pass, many view states.
- Replace the current pattern of 4–5 separate DAO flows in `HomeViewModel`, `StatsViewModel`, and `WeeklyReportViewModel`.
- Add `SessionDao.sessionsBetween(start, end)` for period screens; keep `SUM`/`COUNT` queries for lifetime.
- Inject a `Clock`/`TimeProvider` so tests can freeze “today” and verify midnight/DST behaviour.

### 10.5 Chart implementation notes

- **Prefer layout-based bars over Canvas.** A `Row` of weighted `Column`s with `Box` bars gives free tap targets, semantics, and font scaling. Use `Canvas` only for the goal ring and optional paired comparison bars.
- Bars:
  ```kotlin
  val fraction by animateFloatAsState(
      targetValue = day.seconds / maxSeconds,
      animationSpec = tween(450),
      label = "bar"
  )
  Box(
      Modifier
          .fillMaxWidth()
          .fillMaxHeight(fraction.coerceAtLeast(0.02f))
          .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
          .background(brush)
          .semantics { contentDescription = day.a11yLabel }
  )
  ```
- Heat-map: a `Column` of 6 `Row`s of 7 `Box`es is enough; no Canvas needed. Use `focusIntensity(seconds)` from `FocusCalendar.kt`, but feed it canonical day buckets.
- Ring: `Canvas` + `drawArc` with `StrokeCap.Round`; animate sweep with `Animatable`; center text in a `Box`.
- `LazyColumn` for all screens with lists; `item(key = ...)` for sessions.
- `AnimatedContent` for period switching; `Crossfade` for empty/ready transitions.
- Do not nest `verticalScroll` inside `LazyColumn`.
- Use `Modifier.heightIn(min = 48.dp)` on interactive chart regions.
- Use `WindowSizeClass`/`BoxWithConstraints` for max width; `FlowRow` for momentum cards at large font scale.
- Respect `LocalAccessibilityManager`/animator scale where available; otherwise gate long animations behind a `reduceMotion` flag from `Settings.Global.ANIMATOR_DURATION_SCALE`.

### 10.6 Performance

- Aggregate off the main thread (`Dispatchers.Default`).
- Keep the aggregation window bounded: today + this week + 6 months + 12 weeks + recent 10. Do not load all sessions for every screen unless the history is small.
- Cache the snapshot in the repository with `stateIn(WhileSubscribed(5_000))`.
- Avoid recomputing charts on unrelated state changes; derive per-card state with `remember`/`derivedStateOf` only where it measurably helps.
- Charts are fixed-height and do not allocate per-frame objects in `Canvas` draw lambdas.
- Target: first meaningful Insights frame under 1s on a mid-range device with 5 years of daily sessions.

### 10.7 Testing

- Unit tests for `FocusDayBuckets` (midnight crossing, DST, 23:59/00:01).
- Unit tests for period aggregation (week boundaries, empty weeks, baseline non-overlap).
- Golden/screenshot tests: brand-new, partial week, full week, 6M, empty calendar, all-untagged.
- Compose semantics tests: every bar/cell exposes a description; touch targets ≥48dp.
- Regression test: the same session produces the same day totals in Home, Trends, Weekly report and Calendar.

---

## 11. Duplication cleanup checklist

| Current location | Duplicated content | New owner | Action |
|---|---|---|---|
| `HomeScreen.kt:116-137` | Today, streak, goal ring | InsightsHome hero | Remove Home screen as a tab; keep only Focus/Shield/Insights/You |
| `HomeScreen.kt:150-161` | This week, sessions tiles | InsightsHome week card / hero | Delete tiles |
| `HomeScreen.kt:191-205` | Recent sessions | InsightsHome (3) + Diary (full) | Delete list; Focus shows only a post-save confirmation |
| `HomeScreen.kt:185-189`, `212-256` | Deadline | You → Deadline | Move |
| `HomeScreen.kt:164-183` | Block/Schedules quick links | Shield | Move |
| `StatsScreen.kt:112-189` | Today + streak hero | InsightsHome hero | Delete from Stats |
| `StatsScreen.kt:200-230` | Week, sessions, unlocks, avg session tiles | Week card / Trends / Shield / Weekly report | Delete tiles |
| `StatsScreen.kt:232` | Monthly rhythm | Trends 6M | Delete separate card |
| `StatsScreen.kt:235-270` | Calendar link card | InsightsHome consistency strip | Replace with a real preview, not a link-only card |
| `StatsScreen.kt:273-316` | 4-badge achievements preview | InsightsHome next badge | Replace |
| `StatsScreen.kt:318-352` | Last 7 days chart | InsightsHome week card + Weekly report | Delete duplicate |
| `StatsScreen.kt:354-365` | Full subject breakdown | Trends | Delete from Stats; home gets composition strip |
| `StatsScreen.kt:367-389` | Recent sessions | InsightsHome (3) + Diary | Delete from Stats |
| `StatsScreen.kt:192`, `440-520` | Freeze level card | InsightsHome momentum card + Levels | Replace |
| `StatsScreen.kt:218-230` | Unlocks today | ShieldHome | Move |
| `WeeklyReportScreen.kt:194-207` | Full subject breakdown | Trends | Replace with top-subject one-liner |
| `WeeklyReportScreen.kt:146-192` | Week stats tiles | Weekly report (owner) | Keep; remove duplicates from Stats |
| `CalendarScreen.kt:180-182` | Month summary | Calendar (owner) | Keep |
| `FocusScreen.kt:138-145` | Post-save streak dialog | Focus (moment only) | Keep as confirmation; no persistent streak/list |
| `ProfileScreen.kt:868-958` | Diary / Weekly report links | Insights | Remove |
| `ProfileScreen.kt:778-820` | Deadline | You | Move |
| `ProfileScreen.kt:823` | Goals | You | Move |
| `ProfileScreen.kt:438` | Channels | Shield | Move |

---

## 12. Rollout plan

1. **Data contract first.** Add `FocusDayBuckets`, `InsightsRepository`, `Clock`, range queries. Write unit tests for day/week/streak parity.
2. **Chart primitives.** Build `GoalRing`, `WeeklyBarChart`, `MonthBarChart`, `SubjectMixBar`, `ConsistencyStrip`, `MonthHeatmap`, `DeltaChip`, `EmptyInsightCard`.
3. **InsightsHome.** Ship with empty/partial/ready states behind the new tab.
4. **Drill-downs.** Trends, Weekly report, Calendar, Achievements, Levels, Diary entry points.
5. **Remove old surfaces.** Delete Stats, Home stats, Profile hubs, duplicate subject breakdown, duplicate recent lists, duplicate unlock tile.
6. **Polish.** Screenshot tests, accessibility pass, font scale 200%, reduce-motion, tablet max-width.

### Acceptance criteria

- [ ] No metric appears at the same altitude on two screens.
- [ ] Today/week/month/streak totals are identical across Home, Trends, Report and Calendar for the same session set.
- [ ] A session crossing midnight is handled by one documented convention everywhere.
- [ ] Brand-new user sees no zero-filled charts.
- [ ] Every empty state has one primary CTA.
- [ ] Every chart value is reachable by tap and screen reader.
- [ ] No network calls, no new permissions, no chart library.
- [ ] InsightsHome has at most 6 cards and at most 6 headline numbers.
- [ ] App left open past midnight refreshes “Today” on resume.
- [ ] No duplicate subject breakdown or recent-session list outside its owner.

---

## 13. Open decisions

1. **Midnight split vs end-day attribution.** Recommended: split durations, count sessions on the end day. If migration risk is too high, use end-day everywhere; never mix.
2. **Focus mix on InsightsHome.** Recommended: keep the 100% composition strip as a summary, with no durations; Trends owns the table. Remove it if the home feels too long.
3. **Week start.** Fixed Monday for now. If locale-aware weeks are introduced, the range label must always be visible.
4. **Session detail.** A future `session/{id}` screen would let recent rows open detail instead of Diary. Not required for v1.
5. **Phone unlocks.** Recommended: ShieldHome only. If the product wants it in Insights, it needs a separate “Protection” section, not a focus stat.
6. **Year heat-map.** The 12-week strip + month calendar cover the need. Add a year view only if users ask for it.
7. **Widgets.** The existing focus/week widgets should use the same `InsightsRepository` snapshot so the widget and app never disagree.

---

## 14. Source references

- `docs/frostlock-redesign-plan.md` — IA, four-tab navigation, Insights hierarchy.
- `app/src/main/java/com/frostlock/app/ui/screens/stats/StatsScreen.kt` — current hub, duplicate tiles, chart implementations.
- `app/src/main/java/com/frostlock/app/ui/screens/stats/StatsViewModel.kt` — current aggregation and capped history.
- `app/src/main/java/com/frostlock/app/ui/screens/report/WeeklyReportScreen.kt` — report structure, baseline/improvement.
- `app/src/main/java/com/frostlock/app/ui/screens/calendar/CalendarScreen.kt` — month heat-map and day detail.
- `app/src/main/java/com/frostlock/app/ui/util/FocusCalendar.kt` — `focusIntensity`, `buildFocusMonth`.
- `app/src/main/java/com/frostlock/app/ui/util/WeeklyReport.kt` — week aggregation (uses `endedAt`).
- `app/src/main/java/com/frostlock/app/ui/util/MonthlyStats.kt` — month rollup (uses `startedAt`).
- `app/src/main/java/com/frostlock/app/ui/util/ImprovementReport.kt` — baseline/current windows.
- `app/src/main/java/com/frostlock/app/ui/util/Achievements.kt` — badge definitions.
- `app/src/main/java/com/frostlock/app/ui/util/FocusLevel.kt` — freeze ladder.
- `app/src/main/java/com/frostlock/app/data/local/db/SessionDao.kt` — current queries and limits.
- `app/src/main/java/com/frostlock/app/ui/theme/` — colors, type, shapes, spacing.
