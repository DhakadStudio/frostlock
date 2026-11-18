# FrostLock UI/UX Redesign — Complete Plan

**Version:** 3.0
**Date:** 2026-11-18
**Status:** Implemented and on-device QA'd — Phases 0–8 landed (`dcd4d3c`…`f2fb398`). Tests green: 374 tests / 55 suites. On-device QA pass on 2026-11-18 (Realme RMX5101, Android 16): all four tabs, all Shield/Insights/You detail screens, and the full session loop verified; logcat clean; instrumented smoke suite (`app/src/androidTest`, `b6e0ff7`) green; onboarding re-verified on 2026-11-19. Realme's device-care auto-uninstalled the debug build twice (§9 incident notes) — explicit Auto Backup rules shipped (`c4feb10`). Docs screenshots pending the PiP-free device state.
**Scope:** Full product redesign — information architecture, naming, navigation, screen consolidation, visual system, interaction, accessibility, data contract, implementation, migration and QA.
**Constraints:** Fully offline, local-only, no new permissions, keep the existing privacy promise and permission footprint.

**Companion specs (detail behind this plan):**
- `docs/frostlock-ux-writing.md` — full voice, strings, empty/permission/status copy.
- `docs/frostlock-insights-design.md` — Insights data visualisation, metric ownership, chart implementation.
- `docs/frostlock-redesign-qa-audit.md` — edge-case register, state tables, QA matrix, release gates.

This plan is authoritative for **IA, naming and placement**. The companions are authoritative for **copy detail, chart detail and test detail**. Where an older companion uses a retired word, the vocabulary in §4.2 wins.

---

## 0. Why this plan exists

FrostLock grew feature by feature. The result is a capable app that is hard to navigate:

- The same feature appears in two or three places.
- Different features are mixed into one screen.
- Settings, navigation and marketing share one long Profile page.
- Home and Stats repeat the same numbers.
- Blocking controls are split between Block and Profile.
- Review features (Reflections, Weekly review, Badges) are reachable from three different tabs.

This plan fixes the structure first, then the visuals. It defines **one obvious place for every feature**, a **four-tab mental model**, a **Material 3 Expressive design system**, a **single data contract**, and a **phased implementation path** that keeps the app working while it is migrated.

---

## 1. Current-state audit

### 1.1 Current navigation

Bottom bar: **Home · Block · Focus · Stats · Profile**

Hidden destinations: Schedules, Achievements, Levels, Diary, Calendar, Deadline, Goals, GoalEditor, WeeklyReport, Notifications, Websites, Channels, DesignPreview.

The bottom bar is a route-string blacklist. It is hand-maintained and breaks as soon as tabs become nested graphs.

### 1.2 Duplication matrix

| Feature | Current locations | Problem | New single home |
|---|---|---|---|
| Countdown (deadline) | Home hero, Profile card, Deadline screen | Same object in three places | **You → Countdown** |
| Schedules | Home quick action, Block card, Schedule screen, Profile quick action | Two entry points plus a hidden route | **Shield → Schedules** |
| Reflections (diary) | Stats “Diary” button, Profile “Reflect & review”, Weekly report diary card | Three entry points | **Insights → Reflections** |
| Weekly review | Stats “Report” button, Profile “Reflect & review”, Weekly Report screen | Three entry points | **Insights → Weekly review** |
| Recent sessions | Home list, Stats list | Identical list in two tabs | **Insights → History** (3-row teaser on Insights home) |
| Daily target | Profile setting, Home progress | Setting and display are separated | Setting in **You**, progress in **Insights** |
| Streak | Home ring, Stats hero | Same metric twice | **Insights** |
| This week / sessions | Home stat tiles, Stats stat tiles | Same data twice | **Insights** |
| Subject breakdown | Stats card, Weekly Report card | Same analysis twice | **Insights → Trends**; Weekly review shows one top-subject line |
| History / calendar | Stats card, Calendar screen, Profile widget list | Entry point and widget help mixed | **Insights → History**; widget pinning in **You → Widgets** |
| Levels | Stats card, Levels screen | Hidden route | **Insights → Levels** |
| Badges | Stats preview, Achievements screen | Hidden route | **Insights → Badges** |
| Home-screen blocking | Profile toggle | Blocking control in settings | **Shield → Apps → Advanced** |
| YouTube home feed | Profile toggle | Blocking control in settings | **Shield → Websites** |
| Play Store search | Profile toggle | Blocking control in settings | **Shield → Websites** |
| YouTube/Music channels | Profile card, Channels screen | Blocking control in settings | **Shield → Channels** |
| Website blocking | Block card, Websites screen | Mixed with app blocking | **Shield → Websites** |
| Notifications | Block card, Notifications screen | Mixed with app blocking | **Shield → Notifications** |
| Sound (ambience) | Profile card, Focus info pill | Setting and status mixed | Setting in **You → Focus**; status chip on Focus |
| Strict mode | Profile switch, Focus info | Setting and status mixed | Setting in **You → Focus**; status chip on Focus |
| Pause allowance | Profile card, Focus info pill | Setting and status mixed | Setting in **You → Focus**; status chip on Focus |
| Pomodoro breaks | Profile card, Focus info pill | Setting and status mixed | Setting in **You → Focus**; status chip on Focus |
| Reminders (Focus Guard) | Profile switch | Hidden in settings | **You → Focus** |
| Floating timer (bubble) | Profile switch | Hidden in settings | **You → Focus** |
| Theme | Profile card | Fine, but buried | **You → Appearance** |
| Widgets | Profile card | Fine, but long list | **You → Widgets** |
| Goals | Profile card, Goals screen | Hidden route | **You → Goals** |

### 1.3 Mixed screens

**BlockScreen** is titled “App Blocker” but hosts:
- Focus blocking toggle
- Accessibility permission
- Blocking rules / schedules
- Notification shield
- Website blocking
- App picker for focus blocking
- Daily limits tab
- Suggested limits
- Limit picker

That is five unrelated concerns on one screen.

**ProfileScreen** mixes:
- Pure settings (theme, sound, daily goal, strict mode, pause allowance, breaks, reminders, floating timer)
- Blocking controls (home screen, YouTube home, channels, Play Store)
- Navigation hubs (countdown, goals, diary, weekly report)
- Marketing (Pro badge, coming soon)
- Widget pinning
- About

**StatsScreen** mixes:
- Analytics
- Badges preview
- Levels card
- Calendar entry
- Diary entry
- Weekly Report entry
- Start Focus CTA

**HomeScreen** mixes:
- Overview
- Primary CTA
- Quick actions
- Countdown
- Recent sessions

### 1.4 Visual-system audit

Current tokens:

| Token | Current value | Issue |
|---|---|---|
| Shapes | 14 / 20 / 26 / 100dp | Pill token is used as `extraLarge`, so cards and pills collide |
| Typography | 10 styles, no emphasized variants | No editorial hierarchy; everything is bold or normal |
| Spacing | 4 / 8 / 12 / 16 / 24 / 32 | Missing 20 and 48; screens hardcode 16/20 |
| Colors | 4 dark palettes, global mutable state | Palette mutation during composition; no semantic container roles |
| Elevation | Cards 0dp, bottom bar 12dp | Inconsistent depth |
| Motion | Mostly tweens, one infinite glow | No spring system, no reduced-motion handling |
| Components | `AppCard`, `PrimaryButton`, `SectionHeader` etc. | No variants, no status semantics, no shared scaffold |

### 1.5 Accessibility audit

| Area | Current risk | Required fix |
|---|---|---|
| Contrast | Text secondary on dark cards may fall below 4.5:1 in some palettes | Verify every palette; enforce semantic role pairs |
| Touch targets | Some chips and icon buttons are 30–38dp | Minimum 48×48dp |
| Screen reader | Charts, rings and icon-only buttons lack descriptions | Add content descriptions and summary semantics |
| Switch rows | Switch has no label association | Use `toggleable` row with `Role.Switch` and state description |
| Color-only status | “Active now”, “Overlaps” use color | Add text/icon, not color alone |
| Reduced motion | Infinite glow and snow drift ignore system setting | Respect `ANIMATOR_DURATION_SCALE`; provide static fallback |
| Large text | Fixed 280dp timer disc and 52–64sp timer can clip | Use scalable layouts and `TextAutoSize`/max lines |
| Focus order | Long Profile page has no headings | Use `heading()` semantics and logical order |
| Charts | Bar charts are decorative to TalkBack | Provide textual summary and per-bar descriptions |

---

## 2. Research synthesis

### 2.1 Material 3 Expressive — what to adopt

Sources: [m3.material.io](https://m3.material.io), [Android Developers — Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3), [Ainur — Material 3 Expressive a year on](https://design.ainurhq.com/journal/material-3-expressive), [Agnidia `material3-expressive` skill](https://github.com/Agnidia/android-developer/blob/master/skills/compose-ui/material3-expressive/SKILL.md).

**Adopt:**
- **Springs, not curves.** Spatial springs for geometry, critically damped effects springs for color/opacity. Use `MotionScheme.expressive()` for hero moments and `MotionScheme.standard()` for routine navigation.
- **One hero moment per screen.** Compound shape + scale + color + emphasized type only where it earns attention: the Focus timer, the Shield status, the Insights hero.
- **Shape as state.** Selected tab, active filter, toggled control change corner radius as well as color. This helps users who do not read color well.
- **Emphasized typography.** One emphasized headline or number per screen; baseline everywhere else.
- **Surface containers, not alpha overlays.** Use five `surfaceContainer` levels for depth.
- **Restraint.** Do not make every card bouncy. Routine lists, settings and forms stay quiet.

**Leave out:**
- Cookie/clover/burst shapes as brand identity — they read as Android, not FrostLock.
- Bounce on every tap.
- 35 shapes everywhere. Pick one or two FrostLock shapes.

### 2.2 Competitor patterns

| App | Navigation | Home | Timer | Blocking | Stats |
|---|---|---|---|---|---|
| **Regain** | Home / Focus / Stats / Profile | Greeting, today/week stats, quick actions, deadline, recent sessions | Full-screen timer, Pomodoro/Timer/Stopwatch, strict mode | Blocklist per rule/schedule; YouTube study mode and channel filters as opt-in toggles | Today/week/month, weekly report, leaderboard (Pro) |
| **Forest** | Forest / Stats / Blocker / Profile | Living forest as hero, central start CTA | Tree grows as timer; leaving kills tree | Time Guard schedules + Deep Focus session blocker | Focus analytics, timeline, tags |
| **Opal** | Home / Focus / Stats / Settings | Focus Score, hourly bar chart, app breakdown | Duration picker, Deep Focus, intention, break picker | Rules are the single source of truth: apps/websites + schedule + difficulty | Focus report, app-by-app breakdown |
| **Freedom** | Home / Sessions / Blocklists / Settings | Session status + Start | Background session, status not a large countdown | Blocklists attached to sessions/schedules; “Block All Except” | Session history, minimal charts |
| **one sec** | Minimal | Single intervention | Breathing delay before opening an app | Per-app friction, not a full blocker | Minimal |

**Shared pattern:** one primary action per screen, configuration behind a settings/shield area, one rules list, one stats hub, no duplicate entry points.

### 2.3 Product principles for FrostLock

1. **Do a session.** The timer is the product. One tap from launch.
2. **Protect the session.** All protection lives in Shield, not scattered through settings.
3. **See progress.** Insights is the only place for stats, reviews, reflections and badges.
4. **Tune yourself.** You is the only place for preferences, planning and appearance.
5. **One feature, one home.** If a feature appears twice, one appearance is a summary, not a second entry point.
6. **Calm by default.** Expressive only at the hero moment; quiet everywhere else.
7. **Offline is a feature.** Every screen should make sense in airplane mode.
8. **Tell the truth about protection.** Never show “protected” when a permission or service is missing.

---

## 3. Personas and jobs-to-be-done

### Persona A — The student (primary)
- **Goal:** Study for an exam without opening YouTube, Instagram or games.
- **Job:** Start a 45-minute session, block distractions, see weekly progress, reflect on what worked.
- **Pain:** Too many settings before starting; cannot tell whether blocking is actually on.
- **Success:** Two taps to start; Shield shows exactly what is held.

### Persona B — The deep worker
- **Goal:** Protect long focus blocks during work hours.
- **Job:** Create a weekday schedule, block apps and websites automatically, review monthly focus time.
- **Pain:** Schedules hidden behind Block; limits and schedules feel like different products.
- **Success:** One protection list with clear active/upcoming states.

### Persona C — The privacy-first user
- **Goal:** Reduce screen time without accounts, ads or cloud.
- **Job:** Use local limits, notification holding and widgets; verify nothing leaves the device.
- **Pain:** Marketing and “Pro” language creates doubt.
- **Success:** Clear offline/privacy language, no sign-in, no network.

### Persona D — The returning user
- **Goal:** Resume a habit after a gap.
- **Job:** See streak, last sessions, start again, adjust the daily target.
- **Pain:** Home and Stats show different versions of the same numbers.
- **Success:** One consistent Insights dashboard.

---

## 4. New information architecture

### 4.1 Bottom navigation — four tabs

| Tab | Icon | Purpose | Default screen |
|---|---|---|---|
| **Focus** | Timer | Start, run and finish one session | Focus |
| **Shield** | Shield | Everything that blocks, limits or holds | Shield |
| **Insights** | Bar chart | Progress, reviews, reflections, badges | Insights |
| **You** | Person | Preferences, planning, appearance, widgets | You |

Why four:
- Material 3 navigation bars support 3–5 destinations.
- Four maps cleanly to the four jobs above.
- No tab is a settings dump.
- No tab is a second home.

### 4.2 Canonical vocabulary

One concept, one word. This table is the source of truth for every screen title, route, label and string.

| Concept | Canonical word | Retired |
|---|---|---|
| The protection space | **Shield** | App Blocker, Protection, Blocking |
| A recorded unit of focus | **session** | focus, focus session, timer |
| The activity / first tab | **focus** | deep work |
| What FrostLock does in the moment | **held** | blocked, paused, frozen |
| The user's intent to stop something | **block** | hold, freeze, shield |
| A per-app daily cap | **limit** | budget, allowance, quota |
| A time-based blocking window | **schedule** | rule, routine, window |
| The notification feature | **Notifications** | Notification shield, alert blocker |
| The website feature | **Websites** | Website blocking, web filter |
| The channel feature | **Channels** | YouTube & Music, channel filter |
| The calendar/history screen | **History** | Focus calendar, stats calendar |
| The weekly reflection | **Weekly review** | Weekly report |
| The notes screen | **Reflections** | Study diary, journal, Diary |
| The badge screen | **Badges** | Achievements |
| The level screen | **Levels** | Freeze levels, ladder |
| Minutes per day | **Daily target** | Daily goal |
| A named long-term target | **Goal** | Focus goal |
| A named date countdown | **Countdown** | Focus deadline, deadline |
| A gentle in-session nudge | **Reminders** | Focus Guard |
| The draggable timer overlay | **Floating timer** | Focus bubble, bubble |
| What you are focusing on | **Intention** | One thing, session name |
| The tag on a session | **Subject** | Category, tag |
| The text saved with a session | **note** | reflection (as an object) |
| The place notes live | **Reflections** | Diary |
| The app-blocking screen | **Apps** | App Blocking, App Blocker |

**The block/held distinction.** *Block* is what the user sets up (“Block apps during focus”, “Block list”). *Held* is what FrostLock does in the moment (“Instagram is held during focus”, “3 apps held”, “Held until 17:00”). This is the most important writing rule in the redesign.

**Never use “Focus” or “Shield” as a prefix.** The tab supplies the context: *Block during focus*, not *Focus blocking*; *Notifications*, not *Notification shield*.

### 4.3 Screen hierarchy

```
Onboarding (one-time)
└── Main
    ├── Focus
    │   ├── Focus (configure + start)
    │   ├── Focusing (immersive active session)
    │   ├── Break (Pomodoro)
    │   └── Session summary
    ├── Shield
    │   ├── Shield (what's held + protection list)
    │   ├── Apps
    │   ├── Limits
    │   ├── Schedules
    │   │   └── Schedule editor
    │   ├── Websites
    │   ├── Channels
    │   └── Notifications
    ├── Insights
    │   ├── Insights (summary)
    │   ├── Trends
    │   ├── History
    │   ├── Weekly review
    │   ├── Reflections
    │   ├── Badges
    │   └── Levels
    └── You
        ├── You (settings)
        ├── Goals
        │   └── Goal editor
        ├── Countdown
        └── Widgets
```

### 4.4 Feature placement map

| Feature | Single home | Secondary appearance (summary only) |
|---|---|---|
| Timer modes, session length, subject, intention | Focus | — |
| Active session, pause, strict end, break | Focusing / Break | — |
| Session summary, note, photo, save/discard | Session summary | — |
| Last session | Focus (single card) | — |
| Block during focus | Shield → Apps | Focus subject card |
| Focus-session app set | Shield → Apps | Focus subject chip |
| Subject app sets | Shield → Apps | Focus subject chip |
| Limits | Shield → Limits | Shield status |
| Suggested limits | Shield → Limits | — |
| Schedules / schedule editor | Shield → Schedules | Shield status |
| Upcoming schedules | Shield → Schedules | — |
| Schedule streak | Shield → Schedules | — |
| Schedule reminders | Shield → Schedules | — |
| Website blocking / study list | Shield → Websites | Shield status |
| YouTube home feed | Shield → Websites | — |
| Play Store search blocking | Shield → Websites | — |
| YouTube & Music channels | Shield → Channels | Shield status |
| Notifications + inbox | Shield → Notifications | Shield status |
| Home-screen blocking | Shield → Apps → Advanced | — |
| Split-screen / floating-window blocking | Shield → Apps → Advanced | — |
| Phone unlocks today | Shield (protection outcome) | — |
| Today / week / streak / sessions | Insights | — |
| Trends / monthly rhythm / subjects | Insights → Trends | Insights focus mix |
| History / calendar | Insights → History | Insights consistency strip |
| Weekly review / baseline / improvement | Insights → Weekly review | Insights this-week card |
| Reflections | Insights → Reflections | Session summary; Weekly review count |
| Badges | Insights → Badges | Insights next-badge card |
| Levels | Insights → Levels | Insights level card |
| Daily target | You → Focus | Insights progress |
| Strict mode | You → Focus | Focus chip |
| Pause allowance | You → Focus | Focus chip |
| Pomodoro breaks | You → Focus | Focus chip |
| Reminders | You → Focus | — |
| Floating timer | You → Focus | — |
| Sound | You → Focus | Focus chip |
| Session alerts | You → Focus | Focus notice |
| Theme | You → Appearance | — |
| Goals | You → Goals | — |
| Countdown | You → Countdown | — |
| Widgets | You → Widgets | — |
| Privacy / About | You → Privacy / About | — |
| Onboarding | Onboarding | — |
| Block wall | System overlay | — |

### 4.5 Route map

```
onboarding
main
  focus
    focus
    focus/session
    focus/summary/{sessionId}
  shield
    shield
    shield/apps
    shield/limits
    shield/schedules
    shield/schedules/editor?scheduleId={id}
    shield/websites
    shield/channels
    shield/notifications
  insights
    insights
    insights/trends
    insights/history?month=YYYY-MM
    insights/weekly?week=YYYY-MM-DD
    insights/reflections?from=&to=
    insights/badges
    insights/levels
  you
    you
    you/goals
    you/goals/editor?goalId={id}
    you/countdown
    you/widgets
```

Use nested graphs per tab. Only the tab roots are top-level destinations.

**Bottom bar visibility:** visible on tab roots and standard detail screens. Hidden on onboarding, Focusing, Session summary, full-screen editors (schedule, goal) and the block wall. This supersedes the older assumption in the Insights companion that drill-downs hide the bar; persistent navigation is more predictable and keeps tab state reachable.

### 4.6 Reconciliation decisions (v2 → v3)

| Conflict | Decision |
|---|---|
| Diary vs Reflections | **Reflections** is the notes archive. **History** owns the full session list and calendar. |
| Weekly Report vs Weekly review | **Weekly review**. |
| Achievements vs Badges | **Badges**. |
| Achievements & Levels combined vs separate | **Separate screens**: Badges and Levels, both under Insights. |
| Daily goal vs Daily target | **Daily target** for minutes/day; **Goals** for named long-term targets. |
| Deadline vs Countdown | **Countdown**. |
| Focus Guard vs Reminders | **Reminders**. |
| Focus bubble vs Floating timer | **Floating timer**. |
| Focus ambience vs Sound | **Sound**. |
| Stats Detail vs Trends | **Trends** (7 days / 30 days / 6 months + subjects). |
| Calendar vs History | **History** (month heat-map + day sessions). |
| App Blocking vs Apps | **Apps**. |
| Home-screen blocking in You vs Shield | **Shield → Apps → Advanced**. You has no blocking toggles. |
| Bottom bar on drill-downs | **Visible** on standard details; hidden only on immersive/full-screen flows. |
| Unlocks today in Insights vs Shield | **Shield** as a protection outcome. |
| Focus mix on Insights home | **Keep** as a one-glance summary; Trends owns the table. |
| Focus subject list on Focus | **Single “Last session” card**, not a history list. |

---

## 5. Screen specifications

### 5.1 Focus

**Purpose:** Configure and start a session in under 10 seconds.
**Route:** `focus`

```
┌─────────────────────────────────────┐
│ Focus                      Wed 18 Nov│
│ One task at a time.                 │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ Pomodoro | Timer | Stopwatch  │  │
│  └───────────────────────────────┘  │
│                                     │
│  [25m] [45m] [60m] [90m] [Custom]   │
│                                     │
│  Subject: [Physics] [Math] [+ New]  │
│  Blocks 6 apps · Edit               │
│                                     │
│  Intention                          │
│  ┌───────────────────────────────┐  │
│  │ What are you focusing on?     │  │
│  └───────────────────────────────┘  │
│                                     │
│         ╭───────────────╮           │
│         │               │           │
│         │     25:00     │           │
│         │     Ready     │           │
│         │               │           │
│         ╰───────────────╯           │
│                                     │
│  [        Start focus        ]      │
│                                     │
│  Strict · 2 pauses · Rain · 25m     │
│                                     │
│  Last session                       │
│  Physics · 45m · 2h ago             │
└─────────────────────────────────────┘
```

**Rules:**
- No stats grid, no recent-session list, no countdown on this screen.
- Session settings appear as read-only chips. Tapping a chip opens the **Session settings** bottom sheet (strict mode, pauses, sound).
- Start button is the only filled gradient element.
- If a session is active, this screen becomes Focusing.
- If no subjects exist, show “No subject” and a “Create subject” chip.
- “Blocks 6 apps” is a status line; tapping it opens Shield → Apps.

**States:** first run; no apps selected (“No apps selected — this session won’t block anything”); strict on (lock icon, “Strict session”); pause limit reached; accessibility off (inline warning before start, with “Fix” and “Start timer only”).

### 5.2 Focusing (active session)

**Route:** `focus/session`
**Purpose:** Only the work, the time and what is held.

```
┌─────────────────────────────────────┐
│ [Physics]                    [⌄]    │
│                                     │
│              FOCUSING               │
│         ╭───────────────╮           │
│         │               │           │
│         │     24:58     │           │
│         │  Round 1 of 4 │           │
│         │               │           │
│         ╰───────────────╯           │
│                                     │
│  🛡 6 apps held · Physics set        │
│                                     │
│  “Finish chapter 4”                 │
│                                     │
│  [Pause]          [End]             │
└─────────────────────────────────────┘
```

**Rules:**
- Full-screen, system bars hidden.
- Strict mode: no minimise, no pause, hold-to-end only; copy explains it.
- Stopwatch: elapsed up, strict disabled with explanation.
- Break: timer switches to break color, “Phones are allowed”, “Skip break”.
- Minimise returns to the Focus tab with a running-session mini-player above the nav bar.
- Session survives tab changes, rotation and process death (service-backed controller).
- If accessibility is off, the shield line reads “Not blocking — access is off” in warning tone, never “held”.
- Test-block action stays available for debugging but is hidden behind a long-press or debug flag in release.

### 5.3 Break

**Route:** `focus/session` (phase = BREAK)
**Copy:** “Break” / “Step away. The next round starts on its own.” / **Skip break**.
**Rules:** blocking lifts during break; no pause; break-over alert; returning to focus requires the next round to start explicitly unless auto-start is enabled in You → Focus.

### 5.4 Session summary

**Route:** `focus/summary/{sessionId}`
**Purpose:** Close the loop: planned vs actual, note, photo, streak impact.

Sections:
1. Result hero: “45 minutes focused” / “Session saved.”
2. Planned vs actual.
3. Subject and intention.
4. Streak impact.
5. Note (280 chars).
6. Optional photo.
7. Actions: **Save and end**, **Add photo**, **Done**, **Discard**.

This replaces the current reflection dialog and the separate summary surfaces. Discarding preserves the intention field for the next attempt.

### 5.5 Shield

**Route:** `shield`
**Purpose:** Answer “what is protecting me right now?”

```
┌─────────────────────────────────────┐
│ Shield                              │
│ What's held while you focus.        │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ Held right now                │  │
│  │ Instagram · daily limit       │  │
│  │ YouTube · Weekday study       │  │
│  │ x.com · website list          │  │
│  │ Today · 3 apps held · 12 unlocks│ │
│  └───────────────────────────────┘  │
│                                     │
│  Protection                         │
│  Apps            4 apps held     →  │
│  Limits          2 limits        →  │
│  Schedules       Active: Study   →  │
│  Websites        12 sites        →  │
│  Channels        5 channels      →  │
│  Notifications   3 held today    →  │
└─────────────────────────────────────┘
```

**Rules:**
- Status and navigation are the same row: each protection row shows its live status and opens its detail. No separate quick-action grid.
- “Held right now” is the single explanation list. It replaces separate status text on Block.
- If accessibility or notification access is off, a **permission banner** appears above the list and the affected rows read “Off — access needed”. Never show a green “protected” state.
- Empty study lists and empty rule app sets are labelled “Nothing is being held”.
- Overlapping schedules show all rule names and “2 schedules active”.
- No app picker on Shield home.

### 5.6 Apps

**Route:** `shield/apps`
**Purpose:** Choose which apps stay closed during focus and configure app-level protection.

Sections:
1. **Block during focus** switch with plain-language status (“On · 4 apps held” / “Off”).
2. **Access needed** card if accessibility is missing, with scope and restraint copy.
3. **Blocked apps** — search, selected-only filter, app list.
4. **Subject app sets** — list of subjects, each with app count and edit action.
5. **Advanced** — home-screen blocking; split-screen/floating-window note; effective-set explanation.

Notes:
- Subject app sets are edited here, not on Focus.
- The app picker is shared with Schedules and Limits.
- The old “Blocking rules” card is removed; schedules live in Schedules.
- If no apps are selected, the screen says so plainly before the user starts a session.

### 5.7 Limits

**Route:** `shield/limits`
**Purpose:** Per-app daily caps.

Sections:
1. Explanation: independent of focus; resets at midnight.
2. Suggested limits (one-tap 20 min for installed distractors).
3. Search.
4. Limit list with used / limit and progress bar.
5. Limit editor bottom sheet: stepper, presets, remove.

Rules:
- Limit editor is a bottom sheet, not a dialog, for reachability.
- 80% warning and 100% wall are explained once at the top.
- Usage is local, measured by the accessibility service.
- A limit that spans midnight is split at local midnight.

### 5.8 Schedules

**Route:** `shield/schedules`
**Purpose:** Automatic blocking windows.

Sections:
1. Start and end reminders switch.
2. Coming up (next 7 days, live countdowns).
3. Schedule streak.
4. Your schedules with “Active now” / “Overlaps” badges.
5. **New schedule**.

Schedule card:
- Name
- Days or one-off dates
- Optional exam window
- Time window
- Effective app set summary (or “Uses your focus app list”)
- Enable switch
- Edit / delete

Editor (full screen):
- Name
- Apps (shared picker, copy from subject)
- Repeat (days)
- One-off dates
- Exam window (from/until)
- Time window with Morning / Workday / Evening / Night presets
- Overlap warning
- Save

### 5.9 Websites

**Route:** `shield/websites`
**Purpose:** Hold distracting sites and surfaces during focus and schedules.

Sections:
1. **Websites** — mode (Off / Block list / Study list), domain list, add custom site, curated categories.
2. **YouTube home feed** — on/off; “Search and subscriptions still work.”
3. **Play Store search** — on/off; blocked app names are held.
4. Coverage note: which browsers are supported and what is read.

Rules:
- Each card states exactly when it applies: “during focus and schedules”.
- Study-list warning: “An empty study list walls nothing.” Never show a protected state for an empty allowlist.
- This screen replaces the Profile cards and the Block website card.

### 5.10 Channels

**Route:** `shield/channels`
**Purpose:** Hold distracting YouTube and YouTube Music channels during focus.

Sections:
1. Mode (Off / Block list / Study list).
2. Blocked channels or allowed channels.
3. Add channel.
4. Coverage note: channel name is read on a watch page; nothing watched is stored.

### 5.11 Notifications

**Route:** `shield/notifications`
**Purpose:** Hold alerts during focus and browse what was held.

Sections:
1. **Hold notifications** switch + access status.
2. **Allow notifications** card if access is missing.
3. Inbox: Today / Yesterday / date groups, app icon, time, title, body, reason chip.
4. Most held apps ranking.
5. Clear inbox.

Rules:
- Calls, alarms and ongoing system notifications always pass.
- Inbox dedupes by notification key; when capped, show “Showing latest 200”.
- Empty states differ for off / access missing / granted-but-empty.
- Group-summary behaviour is explicit, not accidental.

### 5.12 Insights

**Route:** `insights`
**Purpose:** One dashboard for progress, with drill-downs as the only detail.

```
┌─────────────────────────────────────┐
│ Insights                    Wed 18 Nov│
│ Your focus, in perspective.         │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ TODAY               ❄ 7-day   │  │
│  │        ╭────────╮             │  │
│  │        │ 1h 20m │  goal ring  │  │
│  │        ╰────────╯             │  │
│  │  of 2h target · 3 sessions    │  │
│  └───────────────────────────────┘  │
│                                     │
│  This week                 6h 40m   │
│  +12% vs last week                  │
│  ▁ ▃ ▅ ▂ ▇ ▄ ▁                      │
│  M T W T F S S                      │
│  Wed · 1h 20m · 2 sessions          │
│  Weekly review →                    │
│                                     │
│  Focus mix                See all → │
│  ███████████░░░░░░                  │
│  ● Physics 45% ● Math 30% ● Other   │
│                                     │
│  Consistency                        │
│  ▪▪▫▪▪▪▫ ▪▪▫▪▪▪▫ … 12 weeks        │
│  18 active days                     │
│                                     │
│  ┌ Level 3 Glacier ┐ ┌ Next badge ┐ │
│  │ █████░░ 42m     │ │ 7h20 / 10h │ │
│  └─────────────────┘ └────────────┘ │
│                                     │
│  Recent sessions          History → │
│  [session] [session] [session]      │
│                                     │
│  Reflections · 8 notes          →   │
│  All insights stay on this device.  │
└─────────────────────────────────────┘
```

**Rules:**
- This is the only place for today, week, streak, session count, recent sessions and trend summaries.
- New users see a baseline invitation instead of zeroes.
- Cards are entry points to drill-downs, not duplicate analytics.
- No Start focus button here; Focus is a tab.
- At most six headline numbers.

**States:** loading skeleton (no fake bars); brand new (baseline card + “what you’ll see” checklist, no charts); history but no focus today (0m + streak message); history but no focus this week (zero tracks + quiet-week caption; focus mix hidden); no subjects (Untagged segment + hint); local DB error (error card + Retry).

### 5.13 Trends

**Route:** `insights/trends`
**Purpose:** Explain patterns over time and by subject. Replaces the old Stats screen.

Sections:
1. Period segmented control: **7 days · 30 days · 6 months**.
2. Summary strip: total, active days, sessions, average per active day.
3. Main chart for the selected period.
4. **By subject**: 100% stacked bar + ranked rows (top 5 + Other).
5. **Best day** (or best month in 6M).
6. Offline note.

Rules:
- Today lives on Insights home; Trends starts at 7 days.
- The 6-month view **is** the monthly rhythm; no separate monthly card.
- No current streak or badges here.
- Average session belongs to Weekly review.

### 5.14 History

**Route:** `insights/history?month=YYYY-MM`
**Purpose:** Answer “when do I focus?” and browse every session.

Sections:
1. Month switcher (`‹ November 2026 ›`; next disabled at current month).
2. Summary: total, active days.
3. 6×7 heat-map, five intensity steps, today outlined, selected day bordered.
4. Legend: Less → More.
5. Selected-day header with date, total, session count, “Best day” chip when applicable.
6. Session rows for that day, including notes indicator.
7. Empty states for empty month and empty day.

Rules:
- A calendar is useful even when empty; render the grid.
- Tapping a leading/trailing day moves to that month.
- No weekly bars or subject breakdown here.

### 5.15 Weekly review

**Route:** `insights/weekly?week=YYYY-MM-DD`
**Purpose:** One week in depth, with week-over-week and baseline comparison.

Sections:
1. Week switcher (`‹ This week ›`, range label, next disabled on current week).
2. Hero: total, sessions, active days, change chip vs previous week (“Fresh start” when previous week is 0).
3. Improvement card: baseline invitation / building baseline / comparison ready, with five metric rows.
4. Daily rhythm bars; best day highlighted.
5. Week stats: sessions, average session, active days, reflections.
6. Best day card.
7. Top subject one-liner + “Subject trends →”.
8. Reflections count + “Open reflections →” (week-filtered).
9. Offline note.

Rules:
- The review is a scoped view of Insights data, not a second stats home.
- The full subject table stays in Trends.
- Baseline = first 7 days vs latest 7 days, non-overlapping.

### 5.16 Reflections

**Route:** `insights/reflections?from=&to=`
**Purpose:** Qualitative archive; notes and photos, not analytics.

Sections:
1. Search.
2. Week filter when opened from Weekly review.
3. Day groups.
4. Session rows with subject, duration, note, photo banner.
5. Edit note.
6. Full-screen photo viewer.

Rules:
- Reflections owns notes; History owns the full session archive.
- No “all sessions” toggle here; that would duplicate History.
- Empty: “No reflections yet. Sessions without notes still count.”

### 5.17 Badges

**Route:** `insights/badges`
**Purpose:** Reward cabinet and next milestone.

Sections:
1. Hero: “7 of 16 unlocked”, linear progress, next badge + remaining.
2. Categories: focus milestones, streak milestones, session milestones.
3. Grid of badges; unlocked full color, locked desaturated with progress.
4. Badge detail dialog: title, description, progress, share for unlocked badges.
5. Offline note.

Rules:
- Labels say **Best streak**, never “Current streak”.
- Current streak lives on Insights home.
- Levels are not shown here.

### 5.18 Levels

**Route:** `insights/levels`
**Purpose:** Long-term identity and progress.

Sections:
1. Hero: current level and title, lifetime total, progress bar, time to next level.
2. **Share level** (user-initiated system share).
3. Ladder of seven levels with required time and reached state.
4. Explanation: progress comes from every saved session on this device.
5. Offline note.

### 5.19 You

**Route:** `you`
**Purpose:** Settings, planning and appearance only.

```
┌─────────────────────────────────────┐
│ You                                 │
│ Your settings, your data.           │
│                                     │
│  Focus                              │
│  Daily target         2h        →   │
│  Strict mode          Off       →   │
│  Pauses               2         →   │
│  Reminders            Off       →   │
│  Floating timer       Off       →   │
│  Sound                Rain      →   │
│  Session alerts       On        →   │
│                                     │
│  Planning                           │
│  Goals                2 active  →   │
│  Countdown            12 days   →   │
│                                     │
│  Appearance                         │
│  Theme                Frost     →   │
│  Widgets                        →   │
│                                     │
│  Privacy                        →   │
│  About                          →   │
└─────────────────────────────────────┘
```

**Rules:**
- No Reflections, Weekly review, Channels or blocking toggles here.
- Settings rows open bottom sheets or detail screens.
- “Free forever” becomes a small line in About, not a hero card.
- “Coming soon” is removed from the main settings page; roadmap lives in About.
- Permission prompts stay with the feature they unlock. You → Privacy explains the model and links to Shield, but does not duplicate toggles.
- Floating timer row shows overlay access state inline; Session alerts row shows notification permission state inline.

### 5.20 Goals

**Route:** `you/goals`
Sections: active goals with progress, focused time, percent, days left, on-track / a little behind / complete; **New goal**; editor with name, target date, target hours (10/25/50/100 + custom).

### 5.21 Countdown

**Route:** `you/countdown`
Sections: current countdown with live days; name; target date; clear; explanation that it is stored only on this device and needs no calendar permission.

### 5.22 Widgets

**Route:** `you/widgets`
Sections: preview grid; pin buttons for **Today**, **Daily target**, **This week**, **History**, **Usage**, **Unlocks**; note that all data stays on device. Unsupported launchers show a disabled state with an explanation.

### 5.23 Onboarding

Keep the ten-slide structure, one idea and one visual per slide:

1. **Welcome** — offline / no account / private.
2. **What pulls you away** — real brand logos.
3. **Your timer** — timer mock.
4. **What gets held** — real installed icons.
5. **Your schedule** — schedule mock.
6. **Your progress** — chart + streak.
7. **Your goals** — multi-select.
8. **Your triggers** — multi-select.
9. **Your reason** — free text.
10. **Ready** — summary + hold-to-start.

Changes:
- “Skip intro” → **Skip** or **Set up later**, and it goes to personalisation, not straight to the app.
- After commitment, land on **Focus**, not a generic Home.
- Permission asks happen when the feature is first used, not in onboarding.
- “FOCUS, FROZEN IN PLACE” stays on the welcome slide only.

### 5.24 Block wall

Keep the four reason families, align copy:

- Focus session
- Daily limit
- Schedule
- Website / channel / home feed / Play Store / study list

Rules:
- One icon, one headline, one sentence, one action.
- Show remaining time when known.
- “Back to focus” / “Go home” and “Open FrostLock”.
- No stats, settings or “unblock” action on the wall.
- Never say “Stay in the zone”; say “Held during focus”.
- If two schedules overlap, name both.
- If access is off, the wall is not shown; Shield and Focus explain the degraded state instead.

---

## 6. Design system

### 6.1 Color roles

Use semantic roles, not raw colors:

| Role | Use |
|---|---|
| `background` | App background |
| `surfaceContainerLowest` | Recessed areas |
| `surfaceContainerLow` | Cards on background |
| `surfaceContainer` | Standard cards |
| `surfaceContainerHigh` | Raised cards, sheets |
| `surfaceContainerHighest` | Menus, dialogs |
| `primary` | Single key action |
| `onPrimary` | Text on primary |
| `secondary` | Supporting accent |
| `tertiary` | Informational accent |
| `outline` | Borders |
| `textPrimary` | Main text |
| `textSecondary` | Supporting text |
| `success` | Active / on |
| `warning` | Overlap / access missing |
| `danger` | Limit reached / destructive |

Palettes: Frost, Aurora, Midnight, Crystal. Keep all four. Each must pass contrast checks.

Rules:
- Gradient only for the Focus hero, the primary start button and the Insights goal ring.
- Status color always paired with text or icon.
- Low-focus days are neutral, never red.
- No pure white text on gradient without a contrast check.

### 6.2 Typography

| Token | Size / line | Weight | Use |
|---|---|---|---|
| `displayTimer` | 64 / 70 | 500 | Immersive timer |
| `displayLarge` | 40 / 46 | 700 | Hero number |
| `displayLargeEmphasized` | 40 / 46 | 800 | One hero number per screen |
| `headlineLarge` | 32 / 38 | 700 | Screen title |
| `headlineMedium` | 26 / 32 | 700 | Section hero |
| `titleLarge` | 20 / 26 | 600 | Card title |
| `titleMedium` | 16 / 22 | 600 | Row title |
| `bodyLarge` | 15 / 22 | 400 | Body |
| `bodyMedium` | 13 / 18 | 400 | Supporting |
| `labelLarge` | 14 / 18 | 600 | Buttons |
| `labelMedium` | 11 / 14 | 600 | Chips / overline |

Rules:
- One emphasized style per screen.
- Timer uses tabular figures (`FontFeatureSettings("tnum")`).
- No all-caps except small overlines.
- Support font scaling to 200%.
- Chart labels use `labelMedium`/`bodySmall`; never `displayLarge` inside a card.

### 6.3 Shape

| Token | Radius | Use |
|---|---|---|
| `extraSmall` | 8dp | Chips, small controls |
| `small` | 12dp | Inputs |
| `medium` | 16dp | Standard cards |
| `large` | 20dp | Primary cards |
| `extraLarge` | 28dp | Sheets, large cards |
| `hero` | 36dp | Focus hero |
| `heroLarge` | 48dp | Immersive timer container |
| `pill` | 100dp | Buttons, nav indicator |

Rules:
- Do not reuse the pill token for cards.
- Selected nav item morphs from pill to rounded square.
- Sheets use `extraLarge` top corners only.

### 6.4 Spacing

| Token | Value |
|---|---|
| `xs` | 4dp |
| `sm` | 8dp |
| `md` | 12dp |
| `lg` | 16dp |
| `xl` | 20dp |
| `xxl` | 24dp |
| `xxxl` | 32dp |
| `huge` | 48dp |

Semantic:
- Screen padding: 20dp compact, 24dp medium, 32dp expanded.
- Card padding: 16dp compact, 20dp expanded.
- Section gap: 24dp.
- List item gap: 8dp.
- Chart height: 140–170dp; bar corner 8dp top; heat-map cells 40–44dp.

### 6.5 Elevation and surfaces

| Layer | Surface | Elevation |
|---|---|---|
| App background | `background` | 0 |
| Standard card | `surfaceContainerLow` | 0 + 1dp outline |
| Raised card | `surfaceContainer` | 2dp |
| Sheet | `surfaceContainerHigh` | 8dp |
| Dialog | `surfaceContainerHighest` | 12dp |
| Bottom nav | `surfaceContainerHigh` | 8dp + outline |

Use surface containers instead of alpha overlays.

### 6.6 Motion

Use `MotionScheme.expressive()` for hero moments and `MotionScheme.standard()` for routine navigation.

| Token | Damping / stiffness | Use |
|---|---|---|
| Fast spatial | 0.6 / 800 | Small controls, chips |
| Default spatial | 0.8 / 380 | Cards, sheets, tab indicator |
| Slow spatial | 0.8 / 200 | Full-screen transitions |
| Fast effects | 1.0 / 3800 | Color, opacity |
| Default effects | 1.0 / 1600 | Color, opacity |
| Slow effects | 1.0 / 800 | Color, opacity |

Rules:
- Never use tweens for geometry.
- Color and opacity use critically damped effects springs.
- Respect `ANIMATOR_DURATION_SCALE == 0`: snap or 100ms fade.
- Timer ring progress uses a slow spatial spring; digits change without animation.
- Bars animate once on first composition (400–500ms), never loop.
- Haptics: light tick on tab select, start, pause, and limit reached; no haptics on scroll.

### 6.7 Iconography

- Material Symbols outlined for inactive, filled for active.
- 24dp default, 20dp inline, 48dp touch target.
- One icon per concept:
  - Focus = timer
  - Shield = shield
  - Insights = bar chart
  - You = person
  - Schedules = calendar
  - Limits = hourglass
  - Websites = globe
  - Channels = video library
  - Notifications = bell
  - Goals = flag
  - Countdown = event
  - Widgets = widgets

### 6.8 Component inventory

**Navigation**
- `FrostNavigationBar` — floating pill, 4 items, animated indicator.
- `FrostNavigationRail` — medium/expanded.
- `FrostTopAppBar` — large title for tab roots, small title + back for details.
- `FrostBottomSheet` — settings, pickers, limit editor.

**Surfaces**
- `FrostCard` — filled / outlined / elevated.
- `HeroCard` — gradient, 36–48dp radius.
- `StatusRow` — icon, title, live status, chevron.
- `PermissionCard` — warning tone, explanation, action.
- `FrostSectionHeader` — title + optional action.
- `FrostDivider`.

**Controls**
- `FrostButton` — primary / secondary / ghost / danger.
- `FrostSegmentedControl` — mode, period.
- `FrostSwitchRow` — full-row toggle with label and description.
- `FrostFilterChip`.
- `FrostSearchField`.
- `FrostStepper`.
- `FrostDatePicker`.

**Data**
- `FrostStatTile`.
- `GoalRing` (Canvas).
- `StreakChip`.
- `WeeklyBarChart`.
- `MonthBarChart`.
- `SubjectMixBar`.
- `SubjectBreakdownList`.
- `ConsistencyStrip`.
- `MonthHeatmap`.
- `DeltaChip`.
- `MetricComparisonRow`.
- `SessionRow`.
- `SubjectChip`.
- `Badge`.
- `CountdownPill`.
- `FrostEmptyState`.
- `FrostLoadingState`.
- `FrostErrorState`.

**Feedback**
- `FrostSnackbar`.
- `FrostDialog`.
- `FrostTooltip`.

---

## 7. Interaction patterns

### 7.1 Navigation
- Bottom bar visible on tab roots and standard details.
- Hidden on onboarding, Focusing, Session summary, full-screen editors and the block wall.
- Tapping the selected tab returns to its root.
- Back from a detail returns to the tab root.
- Predictive back enabled.
- Tab state is preserved per tab (`saveState`/`restoreState`).

### 7.2 Sheets vs dialogs
- **Bottom sheet:** session settings, limit editor, subject picker, duration picker, filters, single settings.
- **Dialog:** destructive confirmation, permission explanation, save/discard.
- **Full screen:** schedule editor, goal editor, Focusing, block wall.

### 7.3 Empty states
Every list has:
- Icon
- One-line title
- One-line explanation
- One action

Examples:
- No sessions: “No sessions yet. Your first focus will show up here.” → **Start focus**
- No apps: “No apps blocked yet. Choose the apps that pull you away.” → **Choose apps**
- No limits: “No limits yet. Start with an app you open without thinking.” → **Set a limit**
- No schedules: “No schedules yet. Add one for study, work, or wind-down.” → **New schedule**
- No subjects: “No subjects yet. Create one to keep its own app list.” → **Create subject**
- No badges: “Your first badge is one session away.” → **Start focus**
- Empty study list: “Study mode is on, but the list is empty. Nothing is being held.” → **Add site** / **Add channel**
- Empty rule app set: “This schedule uses your focus app list.” (not “nothing”)

### 7.4 Loading and error
- Loading: skeleton rows, not a spinner, for lists; neutral skeleton for charts (no fake bars).
- Error: inline card with **Try again**.
- Access missing: permission card, not a toast.
- Offline: no network UI; everything works.

### 7.5 Permission states
- Accessibility: Shield banner + Apps card + Focus pre-start warning.
- Notification access: Notifications card + Shield banner.
- Overlay: You → Focus → Floating timer row.
- Exact alarms: Schedules reminder card (“Reminders may be approximate”).
- `POST_NOTIFICATIONS`: You → Focus → Session alerts, requested in context after the first session or when a reminder/limit is configured.
- Each explains why, what is read, and what is never stored.

---

## 8. Accessibility requirements

### 8.1 Must-pass checklist
- [ ] Text contrast ≥ 4.5:1; large text ≥ 3:1.
- [ ] Touch targets ≥ 48×48dp.
- [ ] Every icon-only button has a content description.
- [ ] Every switch row is toggleable with `Role.Switch` and state description.
- [ ] Charts expose a textual summary and per-item descriptions.
- [ ] Status is never conveyed by color alone.
- [ ] Focus order matches visual order.
- [ ] Headings use `heading()` semantics.
- [ ] Reduced motion respected.
- [ ] Font scaling to 200% does not clip.
- [ ] Screen reader labels for timer, pause, stop, strict end.
- [ ] Live region for timer only on start/pause/complete, not every second.
- [ ] Haptic feedback is supplementary, never the only signal.

### 8.2 Screen-specific
- **Focus:** timer value announced on start/pause/complete, not every tick. Start button label includes mode and length.
- **Shield:** rows read “Apps, on, 4 apps held”; permission banner reads before the list.
- **Insights:** chart summary “This week: 6 hours 40 minutes across 5 days”; each bar/cell has full date, duration and sessions.
- **You:** switch rows read label, description and state.
- **Block wall:** headline and reason are announced together.

---

## 9. Content design

Full strings live in `docs/frostlock-ux-writing.md`. The rules that must be enforced in code review:

### 9.1 Voice
- Calm, clear, brief, private, human.
- Sentence case; proper nouns only.
- Second person, present tense.
- Verbs first in actions.
- No exclamation marks; no “please”, “sorry”, “successfully”, “simply”, “just”.
- Numerals for quantities: “5 min”, “1h 20m”, “3 sessions”.
- Time ranges `09:00–17:00`; dates `Mon 17 Nov` compact, `17 Nov 2026` detail.
- Pair every state with a word: On, Off, Held, Reached.

### 9.2 Standard labels

| Action | Label |
|---|---|
| Begin | **Start focus** |
| Pause / resume | **Pause** / **Resume** |
| End early | **End** |
| Save a finished session | **Save and end** |
| Throw away | **Discard** |
| Stay in session | **Keep focusing** |
| Add a note | **Add a note** |
| Choose apps | **Choose apps** |
| Set a limit | **Set a limit** |
| Create a schedule | **New schedule** |
| Add a site / channel | **Add site** / **Add channel** |
| Open the inbox | **Open inbox** |
| Allow notification access | **Allow notifications** |
| Open accessibility | **Open accessibility settings** |
| Allow overlay | **Allow floating timer** |
| Change a value | **Change** |
| Remove one item | **Remove** |
| Delete | **Delete** |
| Clear | **Clear inbox** / **Clear all** |
| See more | **See all** |
| Retry | **Try again** |
| Dismiss | **Not now** |

### 9.3 Status copy
- “On · 4 apps held”
- “Off — schedules and limits still apply”
- “Held until 17:00”
- “Held by “Weekday study””
- “3 held today”
- “Nothing held yet”
- “An empty study list walls nothing”
- “Stays on this device”

### 9.4 Error copy
Pattern: **Couldn’t [action]. [What to do next].**
- “Couldn’t save. Try again.”
- “Couldn’t open settings. Open Android Settings and allow access there.”
- “This app is no longer installed.”
- “That date has already passed. Choose a later date.”

### 9.5 Privacy copy
Approved: “Works offline.” “Stays on this device.” “Nothing leaves your device.” “No account. No ads. No tracking.”
Avoid: “local-only”, “offline-first”, “privacy-first”, “we never sell your data”, “bank-level security”.

---

## 10. Data and insight contract

This section prevents the same user seeing different totals on different screens.

### 10.1 Canonical day bucketing

Introduce one helper, `FocusDayBuckets`, used by every insight, widget and chart.

- **Duration is split across local midnights.** A 23:30–00:30 session contributes 30 minutes to each day.
- **Session count is attributed to the day the session ended.**
- **Streak and active day** use days with duration > 0 after splitting.
- Use `ZoneId.systemDefault()` with `Instant` + `ZonedDateTime`; never fixed 86 400-second arithmetic.
- Fallback if splitting is too large for the first migration: attribute the whole session to the end day **everywhere**. Never ship a mix.

### 10.2 Periods

- Week: Monday 00:00 to Sunday 23:59 local; future days are zero-value days so the chart has a stable 7-column shape.
- “This week” is never a rolling 7 days. A rolling window is labelled **“Last 7 days”** and exists only in Trends.
- Month: calendar month; six months for the rhythm chart.
- A saved session is a row with `durationSeconds > 0`. `completed` is a separate flag; do not call every saved session “completed”.
- Discarded sessions are never saved and never appear in Insights.

### 10.3 Streak and target

- **Current streak:** consecutive days with focus, counting back from today; if today is empty but yesterday had focus, the streak is still alive.
- **Best streak:** longest run in history; used by Badges and Weekly review only.
- Goal seconds = `dailyTargetMinutes × 60`.
- Goal hit = day seconds ≥ target seconds.
- Active day = day seconds > 0, not goal hit.

### 10.4 Subjects and formatting

- `tag.trim()`; blank → `"Untagged"`.
- Top 5 by duration; remainder aggregated as `"Other"`.
- Percentages use total seconds; round to whole percent; shares may not sum to 100% and must not be forced.
- Subject colours come from `SubjectStore`; unknown tags use the ice accent.
- Store seconds; format only at the edge with `formatDuration`.
- `formatDuration(0)` is never used for an empty stat; use `"—"` or hide the value.
- Durations under a minute may show `<1m` in tight charts.

### 10.5 Freshness

- A session insert must update every insight flow.
- Recompute at midnight, on `ON_RESUME`, and on `ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`.
- Do not capture `todayStart` once in a ViewModel; use a `Clock`/`todayFlow()` that emits at midnight and on resume.
- Do not use `recentSessions(500/2000)` as the only source for calendar, monthly or lifetime views. Add range queries (`sessionsBetween(start, end)`) and SQL `SUM`/`COUNT` for lifetime totals.
- Widgets read the same snapshot as the app so numbers never disagree.

### 10.6 Shared snapshot

```kotlin
data class InsightsSnapshot(
    val generatedAt: Instant,
    val today: DayFocus,
    val week: PeriodFocus,
    val last7: PeriodFocus,
    val months: List<MonthFocus>,        // last 6, oldest → newest
    val subjects: List<SubjectFocus>,    // period-scoped, top 5 + Other
    val days: Map<LocalDate, DayFocus>,  // canonical buckets, bounded window
    val recent: List<SessionEntity>,     // 3–10 for summaries
    val level: FocusLevel,
    val badges: List<Badge>,
    val improvement: ImprovementReport,
    val reflections: Int
)
```

One `InsightsRepository` produces this from a single session read plus `FocusSettings`/`SubjectStore`. ViewModels derive screen state; they do not re-query and re-aggregate independently. Cache with `stateIn(WhileSubscribed(5_000))` and aggregate off the main thread.

### 10.7 Metric ownership

| Metric | Owner | Allowed summary |
|---|---|---|
| Today focus | Insights home hero | — |
| Daily target progress | Insights home hero | — |
| Current streak | Insights home hero | — |
| This week total | Weekly review | Insights this-week card |
| Weekly bars | Weekly review | Insights mini bars |
| Today session count | Insights home hero | Weekly review / Trends for their periods |
| Lifetime session count | Badges | Levels context only |
| Average session | Weekly review | — |
| Monthly rhythm | Trends (6M) | — |
| Subject breakdown | Trends | Insights focus mix (no durations); Weekly review top-subject line |
| History heat-map | History | Insights consistency strip (different period) |
| Recent sessions | History | Insights 3 rows |
| Baseline / improvement | Weekly review | — |
| Badges | Badges | Insights next-badge card |
| Levels | Levels | Insights level card |
| Reflections | Reflections | Weekly review count; Session summary |
| Best day | Weekly review (week), History (month) | — |
| Active days | Trends / Weekly review / History | Insights consistency caption |
| Phone unlocks | Shield | — |
| Lifetime focus total | Levels | — |

**Rule:** a parent may restate a metric as a compact teaser, but the same metric at the same altitude must never appear twice. No all-time session tile, no duplicate subject table, no duplicate recent-session list.

---

## 11. Capability and degraded-state model

The redesign concentrates protection status on Shield. That makes silent degraded mode unacceptable.

### 11.1 Capability model

```kotlin
enum class Capability { GRANTED, DENIED, NOT_CONFIGURED, UNAVAILABLE }
```

- `UNAVAILABLE`: the OS/device does not support it (e.g. exact alarms pre-Android 12, widget pinning unsupported).
- `NOT_CONFIGURED`: access is fine but no apps/list/schedules exist yet.
- Never render a success/protected state for `DENIED` or `UNAVAILABLE`.

### 11.2 Capabilities to model

| Capability | Needed by | Surfaced in |
|---|---|---|
| Accessibility service | Apps, Websites, Channels, limits | Shield banner, Apps card, Focus pre-start |
| Notification access | Notifications | Notifications card, Shield banner |
| Overlay | Floating timer | You → Focus row |
| Exact alarms | Schedule reminders | Schedules reminder card |
| `POST_NOTIFICATIONS` | Session alert, reminders, limit warnings | You → Focus → Session alerts; contextual request |
| Widget pinning | Widgets | You → Widgets |

### 11.3 Truth rules

1. **No false protection.** If accessibility is off, Shield and Focus say so. A toggle may be configured but the feature is marked inactive.
2. **Effective, not stored.** Show the app set that will actually be held: empty study list → nothing held; empty schedule app set → uses focus list; overlapping schedules → union + all names.
3. **Access missing is a card, not a toast.** It appears where the feature lives, with scope and restraint copy.
4. **Revocation is detected on resume.** Re-check every capability on `ON_RESUME` and after returning from system settings.
5. **Midnight is boring.** Limits, warnings, held-today, inbox groups, streaks and widgets are correct across midnight, timezone change and manual clock change.
6. **No stale counts.** “Held today” and inbox day groups recompute on resume and date change.
7. **Inbox is honest.** Dedupe by notification key; show “Showing latest 200” when capped; define group-summary behaviour.

### 11.4 P0/P1 findings the redesign must close

| ID | Severity | Finding | Required change |
|---|---|---|---|
| F1 | P0 | `POST_NOTIFICATIONS` requested on cold start before onboarding, empty result callback | Defer to context; add Session alerts row and Shield banner |
| F2 | P0 | Toggle can be ON while accessibility service is off | Capability model + Shield banner + Focus pre-start warning |
| F3 | P1 | Widgets and limits do not refresh at midnight/timezone | Date-change receiver + resume refresh |
| F4 | P1 | Limit credit across midnight lands on the wrong day | Split at local midnight or reset `activeSince` |
| F5 | P1 | Inbox duplicates and silent 200-item cap | Dedupe by key; show cap notice |
| F6 | P1 | “Held today” stale across midnight | Recompute on resume/date change |
| F7 | P1 | Wall names only the first of several overlapping schedules | Name all active schedules |
| F8 | P1 | Empty study list looks protected | “Nothing is being held” state |
| F9 | P1 | Fixed timer disc and nav labels clip at large text | Scalable layout; test 1.0–2.0 |
| F10 | P1 | No adaptive layout | Window size classes + rail + max width |
| F11 | P2 | Discard wipes the intention | Preserve intention across discard |
| F12 | P2 | Delete session leaves stale streak | Recompute after delete |
| F13 | P2 | Strict toggle looks enabled for Stopwatch | Disable with explanation |
| F14 | P2 | Reboot mid-session leaves a stale wall-clock session | Surface “Session ended while FrostLock was closed” or resume explicitly |
| F15 | P2 | Onboarding writes are not idempotent | Single transaction / upsert |
| F16 | P2 | Widgets hard-code Frost palette | Pass palette or document Frost-only |
| F17 | P2 | RTL and 12/24h untested | Pseudo-RTL + clock format decision |
| F18 | P2 | Reduced motion ignored | Respect animator duration scale |

---

## 12. Adaptive layouts

| Width | Navigation | Content |
|---|---|---|
| < 600dp | Floating bottom bar | Single pane, 20dp padding |
| 600–839dp | Navigation rail | Single pane max 720dp; list/detail where useful |
| ≥ 840dp | Navigation rail, labels | Two-pane for Shield, Insights, You; Focus centered max 560dp |
| Tabletop | Rail | Timer disc above controls; avoid hinge |
| Foldable | Rail | Two-pane; respect folding feature |

Two-pane candidates:
- Shield: list + detail.
- Insights: dashboard + selected report.
- You: settings list + detail.

Focus is always a centered column; the timer is capped at 560dp.

---

## 13. Implementation plan

### Phase 0 — Data and capability foundations
1. Add `FocusDayBuckets`, `Clock`/`TimeProvider`, `InsightsRepository`, `InsightsSnapshot`.
2. Add `sessionsBetween(start, end)` and lifetime `SUM`/`COUNT` queries.
3. Add the `Capability` model and a `CapabilityRepository` that re-checks on resume.
4. Add `DateChangeReceiver` for date/time/timezone changes; refresh widgets, reminders and limit caches.
5. Split limit credit at midnight; dedupe inbox by key; define group-summary behaviour.
6. Unit tests for day buckets, week boundaries, streak parity, overlap union, overnight tails and midnight rollover.

### Phase 1 — Design foundations
1. Add design tokens: `FrostColors`, `FrostTypography`, `FrostShapes`, `FrostSpacing`, `FrostMotion`.
2. Replace global `FrostThemeState.palette` mutation with `CompositionLocal`.
3. Add `FrostScreen`, `FrostTabScaffold`, `FrostDetailScaffold`, `FrostImmersiveScaffold`.
4. Add `FrostNavigationBar` and `FrostNavigationRail`.
5. Add core components: `FrostCard`, `HeroCard`, `StatusRow`, `PermissionCard`, `FrostButton`, `FrostSwitchRow`, `FrostSegmentedControl`, `FrostSearchField`.
6. Add previews for all four palettes and compact/expanded.

### Phase 2 — Navigation shell
1. Add `Destinations.kt` and `TopLevelDestination.kt`.
2. Add `AppNavHost.kt` with onboarding vs main.
3. Add `MainNavGraph.kt` with four nested graphs.
4. Map existing screens temporarily:
   - Focus → `FocusScreen`
   - Shield → new `ShieldScreen` linking to old Block/Schedules/Websites/Channels/Notifications
   - Insights → `StatsScreen`
   - You → `ProfileScreen`
5. Fix `popUpTo(MainGraph)`; add `saveState`/`restoreState`.
6. Add navigation tests.

### Phase 3 — Shield decomposition
1. Extract `BlockScreen` into `AppsScreen` and `LimitsScreen`.
2. Build `ShieldScreen` status rows and permission banner.
3. Move Schedules, Websites, Channels, Notifications under Shield.
4. Move home-screen, YouTube home and Play Store controls into Shield.
5. Add effective-set states: empty study list, empty schedule app set, overlapping schedules.
6. Delete `BlockScreen` and `BlockViewModel`.

### Phase 4 — Insights consolidation
1. Build `InsightsScreen` from the snapshot (Home + Stats merged).
2. Build `TrendsScreen`, `HistoryScreen`, `WeeklyReviewScreen`, `ReflectionsScreen`, `BadgesScreen`, `LevelsScreen`.
3. Add chart primitives: `GoalRing`, `StreakChip`, `WeeklyBarChart`, `MonthBarChart`, `SubjectMixBar`, `SubjectBreakdownList`, `ConsistencyStrip`, `MonthHeatmap`, `DeltaChip`.
4. Add baseline, loading, empty, partial, degraded and error states.
5. Delete `HomeScreen` and `HomeViewModel`.

### Phase 5 — You cleanup
1. Profile becomes settings-only with sections Focus, Planning, Appearance, Privacy, About.
2. Move Goals, Countdown, Widgets.
3. Remove Reflections, Weekly review, Channels and blocking toggles.
4. Add capability rows for floating timer and session alerts.
5. Remove Pro hero and “coming soon” from the main page.

### Phase 6 — Focus and session
1. Extract `SessionSummaryScreen`.
2. Move the active session to `FocusSessionController` + service.
3. Add shell-level `ImmersiveSessionHost`.
4. Add the running-session mini-player above the nav bar.
5. Add hero start motion; add pre-start accessibility warning.
6. Preserve intention across discard.

### Phase 7 — Adaptive and accessibility — ✅ landed (`e2a4245`)
1. ✅ Add window size classes — custom `FrostWindowSizeClass` (COMPACT < 600dp / MEDIUM < 840dp / EXPANDED) with pure `windowWidthClassFor`; no experimental material3-window-size-class API.
2. ✅ Add rail and two-pane — navigation rail on MEDIUM/EXPANDED, floating bottom bar on COMPACT; `TwoPaneLayout`; Shield hub becomes two panes on EXPANDED.
3. ✅ Add foldable posture — `androidx.window:window:1.5.0`; tabletop posture (HALF_OPENED, horizontal hinge) stacks the panes.
4. ✅ Add max widths — detail scaffold caps at `contentMaxWidth` (720dp), `contentMaxWidthWide` (1440dp) token for two-pane.
5. ✅ Add `TextAutoSize`/scalable timer; test 1.0/1.3/1.6/2.0 — `TextAutoSize.StepBased` (24–64sp, 2sp steps) on all three timer readouts; behaviour asserted via `TimerAutoSize` unit tests.
6. ✅ Add reduced-motion handling and chart semantics — `rememberReducedMotion()` gates onboarding orbit/radar/aurora, immersive timer glow and StreakRing; chart `contentDescription` semantics verified present.

### Phase 8 — Cleanup — ✅ landed
1. ✅ Delete old routes, `Screen.kt`, `AppBottomBar` — already gone in earlier phases; DestinationsTest asserts the route set.
2. ✅ Remove deprecated aliases — `Color.kt` deleted; every legacy alias migrated to `FrostTheme.colors.*` (`glacier` promoted to a real `FrostColors` role); feedback colours inlined into `FrostColors.from()`.
3. ✅ Move `DesignPreviewScreen` to the debug source set — `app/src/debug/...`; release builds no longer contain it; About row removed.
4. ⚠️ Docs updated (this file). Screenshots: none exist in the repo; capturing them needs a device/emulator — tracked as a QA release-gate item, not a doc-edit item.

### 13.1 File-level change map

**New**
```
ui/navigation/Destinations.kt
ui/navigation/TopLevelDestination.kt
ui/navigation/AppNavHost.kt
ui/navigation/MainNavGraph.kt
ui/navigation/FocusNavGraph.kt
ui/navigation/ShieldNavGraph.kt
ui/navigation/InsightsNavGraph.kt
ui/navigation/YouNavGraph.kt
ui/shell/FrostAppScaffold.kt
ui/shell/FrostTabScaffold.kt
ui/shell/FrostDetailScaffold.kt
ui/shell/FrostImmersiveScaffold.kt
ui/shell/FrostNavigationSuite.kt
ui/shell/WindowLayout.kt
ui/shell/FrostScreen.kt
ui/components/navigation/FrostNavigationBar.kt
ui/components/navigation/FrostNavigationRail.kt
ui/components/layout/FrostSection.kt
ui/components/layout/TwoPaneLayout.kt
ui/components/surface/FrostCard.kt
ui/components/surface/HeroCard.kt
ui/components/surface/StatusRow.kt
ui/components/surface/PermissionCard.kt
ui/components/controls/FrostButton.kt
ui/components/controls/FrostSwitchRow.kt
ui/components/controls/FrostSegmentedControl.kt
ui/components/controls/FrostSearchField.kt
ui/components/data/GoalRing.kt
ui/components/data/StreakChip.kt
ui/components/data/WeeklyBarChart.kt
ui/components/data/MonthBarChart.kt
ui/components/data/SubjectMixBar.kt
ui/components/data/SubjectBreakdownList.kt
ui/components/data/ConsistencyStrip.kt
ui/components/data/MonthHeatmap.kt
ui/components/data/DeltaChip.kt
ui/components/data/FrostStatTile.kt
ui/components/feedback/FrostSheet.kt
ui/components/feedback/FrostEmptyState.kt
domain/insights/InsightsRepository.kt
domain/insights/InsightsSnapshot.kt
domain/insights/FocusDayBuckets.kt
domain/insights/PeriodAggregator.kt
domain/capability/Capability.kt
domain/capability/CapabilityRepository.kt
focus/FocusSessionController.kt
screens/shield/ShieldScreen.kt
screens/shield/ShieldViewModel.kt
screens/shield/AppsScreen.kt
screens/shield/LimitsScreen.kt
screens/insights/InsightsScreen.kt
screens/insights/InsightsViewModel.kt
screens/insights/TrendsScreen.kt
screens/insights/HistoryScreen.kt
screens/insights/WeeklyReviewScreen.kt
screens/insights/ReflectionsScreen.kt
screens/insights/BadgesScreen.kt
screens/insights/LevelsScreen.kt
screens/you/YouScreen.kt
screens/you/YouViewModel.kt
```

**Modified**
- `MainActivity.kt` — start destination, palette key, splash, permission timing.
- `NavGraph.kt` — replaced by `ui/navigation/*NavGraph.kt`.
- `Screen.kt` — replaced by `ui/shell/FrostTabScaffold.kt` and `FrostDetailScaffold.kt`.
- `AppBottomBar.kt` — replaced by `ui/components/navigation/FrostNavigationBar.kt`.
- `Theme.kt`, `FrostColors.kt`, `IcePalettes.kt`, `FrostShapes.kt` (was `Shape.kt`), `FrostSpacing.kt` (was `Spacing.kt`), `FrostTypography.kt` (was `Type.kt`), `FrostMotion.kt` — semantic tokens only; legacy top-level aliases and `Color.kt` deleted in Phase 8.
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — adaptive (`androidx.window:window:1.5.0`), lifecycle-runtime-compose, navigation-testing.
- `AndroidManifest.xml` — predictive back, date-change receiver.

**New since Phase 7 (not in the original map)**
- `ui/shell/WindowLayout.kt` — `FrostWindowSizeClass`, `windowWidthClassFor`, `rememberFrostWindowSizeClass`, `rememberIsTabletopPosture`.
- `ui/components/layout/TwoPaneLayout.kt` — side-by-side panes (tabletop: stacked).
- `ui/util/TimerAutoSize.kt` — `TextAutoSize.StepBased` config + pure sizing helpers.
- `app/src/debug/ui/screens/preview/DesignPreviewScreen.kt` — debug-source-set only; not a release feature.

**Moved**
- `screens/home/*` → `screens/insights/`.
- `screens/stats/StatsScreen.kt` → `screens/insights/TrendsScreen.kt`.
- `screens/block/BlockScreen.kt` → split into Apps + Limits.
- `screens/schedule/*` → `screens/shield/schedules/`.
- `screens/websites/*` → `screens/shield/websites/`.
- `screens/channels/*` → `screens/shield/channels/`.
- `screens/notifications/*` → `screens/shield/notifications/`.
- `screens/report/*` → `screens/insights/weekly/`.
- `screens/diary/*` → `screens/insights/reflections/`.
- `screens/calendar/*` → `screens/insights/history/`.
- `screens/profile/*` → `screens/you/`.
- `screens/goals/*`, `screens/deadline/*` → `screens/you/`.
- `screens/focus/FocusScreen.kt` → `FocusScreen.kt` + `SessionSummaryScreen.kt`.
- `screens/focus/FocusImmersiveOverlay.kt` → `shell/ImmersiveSessionHost.kt`.

---

## 14. Testing and QA

### 14.1 Test suites to add

| Suite | Type | Why |
|---|---|---|
| `PermissionStateTest` | Unit + fake Context | Every capability maps to the right UI state |
| `MidnightRolloverTest` | Unit with fixed Clock | Limits, warnings, held-today, streak, widgets |
| `ScheduleBoundaryTest` | Unit | Overnight tail, one-off + overnight, exam fence + overnight, DST |
| `FocusLifecycleTest` | Unit/VM | Pause caps, strict, discard, <5s, stopwatch, restore |
| `NotificationInboxTest` | Unit | Dedupe, cap, group summary, day grouping, ranking |
| `ShieldEffectiveStateTest` | Unit | Empty study list, empty schedule apps fallback, overlap union |
| `WidgetFreshnessTest` | Unit/Robolectric | Date change, timezone change, palette |
| `FocusDayBucketsTest` | Unit | Midnight crossing, DST, 23:59/00:01 |
| `PeriodAggregatorTest` | Unit | Week boundaries, empty weeks, baseline non-overlap |
| Compose UI tests | Instrumentation | Empty/loading/degraded per tab; large font; RTL |
| Screenshot tests | Instrumentation | Four palettes × four tabs × phone/tablet |

### 14.2 Smoke (every build)
- [ ] Fresh install → onboarding → Focus → 1-minute session → summary → Insights updates.
- [ ] Bottom bar has exactly Focus, Shield, Insights, You.
- [ ] Every sub-screen has a working back and does not lose tab state.
- [ ] No crash with all permissions denied.
- [ ] Airplane mode: full flow works.
- [ ] Cold start under 2s; no blank frame.

### 14.3 Release gates

Ship only when all are true:

1. **No false-protection state.** For every combination of accessibility, notification access, overlay, exact alarm and notification permission, Shield and Focus show the truth. Verified by a state-table test.
2. **Midnight is boring.** Limits, warnings, held-today, inbox groups, streaks and all six widgets are correct across midnight, timezone change and manual clock change.
3. **Overlaps and overnight are exact.** Unit tests cover overlap union, overnight tail weekday, one-off + overnight, exam fence + overnight and DST.
4. **Empty means empty.** Every list has an empty state; empty study lists and empty schedule app sets are labelled as not protecting.
5. **Every tab has Loading / Empty / Content / Degraded / Error.** Screenshot tests cover four palettes on phone and tablet.
6. **Accessibility passes.** TalkBack, 2.0 font, display size, bold text, RTL, reduce motion; timer and nav remain usable.
7. **Adaptive layout exists.** Compact bottom nav; medium/expanded rail or max-width content; hinge-aware.
8. **No new permissions.** Merged manifest still has no `INTERNET`; permission list unchanged.
9. **Instrumentation tests exist** for the four tab empty/degraded states. Zero instrumentation tests is a blocker.

### 14.4 Device matrix

| Class | Examples | Focus |
|---|---|---|
| Small phone | 5.0" 720p | Large text, nav labels, timer disc |
| Standard phone | 6.1" 1080p | Baseline |
| Large phone | 6.7" | One-hand reach, bottom bar |
| Tablet | 10"+ | Window size class, max width, rail |
| Foldable | folded + unfolded | Hinge, state preservation |
| Android 12 | exact alarms | Exact-alarm fallback |
| Android 13+ | `POST_NOTIFICATIONS` | Permission flow |
| Android 14/15 | FGS restrictions | Session start, background |
| OEM | Samsung/Xiaomi/Realme | Service kill, battery saver |
| Locale | English + pseudo-RTL, 12h/24h | Layout, clock |
| Accessibility | TalkBack, 2.0 font, bold, reduce motion | All tabs |

---

## 15. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Navigation rewrite breaks back stack | Users lose place | Nested graphs + tests + strangler migration |
| Focus timer lost on tab switch | Lost session | Service-backed `FocusSessionController` |
| Theme token migration regresses colors | Visual bugs | Deprecated aliases + palette previews |
| BlockScreen split breaks blocking | Core feature fails | Extract logic first, UI second; keep tests |
| Insights aggregation disagrees with old Stats | Trust loss | `FocusDayBuckets` + one repository before UI merge |
| Too much motion | Fatigue | Expressive only at hero moments; standard elsewhere |
| Accessibility regressions | Exclusion | Checklist + TalkBack pass per phase |
| Large diff | Review pain | Phase-by-phase commits, no big-bang |
| Widget data mismatch | Confusing | Shared snapshot and cache keys |
| Vocabulary drifts back to old names | Confusion | Canonical table in §4.2 + string review checklist |

---

## 16. Success metrics

- Time to start a focus session: < 10 seconds from launch.
- Time to find a blocking control: < 3 taps.
- Zero duplicate entry points for Reflections, Weekly review, Countdown, Schedules.
- Zero blocking toggles in You.
- TalkBack pass on all four tabs.
- All four palettes pass contrast.
- No new permissions.
- App still works in airplane mode.
- No screen claims protection while a required capability is off.
- Same session set produces identical day totals in Insights, Trends, Weekly review and History.
- 273+ existing tests stay green.

---

## 17. Open questions

Resolved in v3: vocabulary (§4.2), bottom-bar visibility (§4.5), separate Badges and Levels, Daily target vs Goals, Countdown, History, Trends, Reflections, Unlocks on Shield, no blocking toggles in You.

Remaining:

1. **Running session:** mini-player above the nav bar, or a banner on the Focus tab? Recommended: mini-player.
2. **Focus mix on Insights home:** keep the 100% strip, or move it to Trends only if the home feels long? Recommended: keep.
3. **Midnight splitting:** split duration across midnights, or attribute the whole session to the end day? Recommended: split; fallback to end-day everywhere if migration risk is high.
4. **Light theme:** dark-only for now, or add a light palette later? Recommended: dark-only now.
5. **Session detail:** add `insights/session/{id}` so recent rows open detail instead of History? Recommended: v1.1.
6. **Year heat-map:** 12-week strip + month history cover the need. Add a year view only if requested.
7. **Auto-start next Pomodoro round:** default on or off? Recommended: off, with a clear “Start next round” CTA.

---

## 18. Appendix — complete feature-to-screen map

| # | Feature | Screen |
|---|---|---|
| 1 | Pomodoro / Timer / Stopwatch | Focus |
| 2 | Session length presets / custom | Focus |
| 3 | Subject selection | Focus |
| 4 | Subject app set edit | Shield → Apps |
| 5 | Intention | Focus |
| 6 | Start / resume / pause | Focus / Focusing |
| 7 | Strict mode | You → Focus + Focus chip |
| 8 | Pause allowance | You → Focus + Focus chip |
| 9 | Pomodoro breaks | You → Focus + Focus chip |
| 10 | Reminders | You → Focus |
| 11 | Floating timer | You → Focus |
| 12 | Sound | You → Focus + Focus chip |
| 13 | Session alerts | You → Focus |
| 14 | Active session | Focusing |
| 15 | Break | Break |
| 16 | Session summary | Session summary |
| 17 | Session note | Session summary / Reflections |
| 18 | Session photo | Session summary / Reflections |
| 19 | Save / discard | Session summary |
| 20 | Last session | Focus |
| 21 | Block during focus | Shield → Apps |
| 22 | Focus session app picker | Shield → Apps |
| 23 | Accessibility permission | Shield → Apps |
| 24 | Home-screen blocking | Shield → Apps → Advanced |
| 25 | Split-screen / floating-window blocking | Shield → Apps → Advanced |
| 26 | Limits | Shield → Limits |
| 27 | Suggested limits | Shield → Limits |
| 28 | Limit editor | Shield → Limits sheet |
| 29 | Schedules | Shield → Schedules |
| 30 | Schedule editor | Shield → Schedules → Editor |
| 31 | Time presets | Schedule editor |
| 32 | One-off dates | Schedule editor |
| 33 | Exam window | Schedule editor |
| 34 | Overlap warnings | Schedule editor / Schedules |
| 35 | Coming up | Shield → Schedules |
| 36 | Schedule streak | Shield → Schedules |
| 37 | Schedule reminders | Shield → Schedules |
| 38 | Websites + study list | Shield → Websites |
| 39 | YouTube home feed | Shield → Websites |
| 40 | Play Store search | Shield → Websites |
| 41 | YouTube & Music channels | Shield → Channels |
| 42 | Notifications | Shield → Notifications |
| 43 | Notification inbox | Shield → Notifications |
| 44 | Most held apps | Shield → Notifications |
| 45 | Phone unlocks today | Shield |
| 46 | Today focus | Insights |
| 47 | Daily target progress | Insights |
| 48 | Current streak | Insights |
| 49 | This week | Insights / Weekly review |
| 50 | Weekly bars | Insights / Weekly review |
| 51 | Focus mix | Insights |
| 52 | Consistency strip | Insights |
| 53 | Recent sessions | Insights / History |
| 54 | Trends (7D / 30D / 6M) | Insights → Trends |
| 55 | Monthly rhythm | Insights → Trends |
| 56 | Subject breakdown | Insights → Trends |
| 57 | Best day / best month | Insights → Trends / Weekly review / History |
| 58 | History heat-map | Insights → History |
| 59 | Month totals | Insights → History |
| 60 | Day sessions | Insights → History |
| 61 | Weekly review | Insights → Weekly review |
| 62 | Baseline / improvement | Insights → Weekly review |
| 63 | Average session | Insights → Weekly review |
| 64 | Active days | Insights → Trends / Weekly review / History |
| 65 | Reflections | Insights → Reflections |
| 66 | Badges | Insights → Badges |
| 67 | Badge sharing | Insights → Badges |
| 68 | Levels | Insights → Levels |
| 69 | Level sharing | Insights → Levels |
| 70 | Daily target | You → Focus |
| 71 | Theme | You → Appearance |
| 72 | Goals | You → Goals |
| 73 | Goal editor | You → Goals → Editor |
| 74 | Countdown | You → Countdown |
| 75 | Widgets | You → Widgets |
| 76 | Privacy | You → Privacy |
| 77 | About | You → About |
| 78 | Onboarding | Onboarding |
| 79 | Block wall | System overlay |

---

## 19. Source notes

- Material 3 Expressive: [m3.material.io](https://m3.material.io), [Android Developers](https://developer.android.com/develop/ui/compose/designsystems/material3), [Ainur design journal](https://design.ainurhq.com/journal/material-3-expressive), [Agnidia material3-expressive skill](https://github.com/Agnidia/android-developer/blob/master/skills/compose-ui/material3-expressive/SKILL.md).
- Competitors: Regain, Forest, Opal, Freedom, one sec.
- Internal source of truth: `docs/frostlock-vs-regain.md`.
- Companion specs: `docs/frostlock-ux-writing.md`, `docs/frostlock-insights-design.md`, `docs/frostlock-redesign-qa-audit.md`.
- Code audit: `Screen.kt`, `NavGraph.kt`, `AppBottomBar.kt`, `HomeScreen.kt`, `BlockScreen.kt`, `ProfileScreen.kt`, `StatsScreen.kt`, `WeeklyReportScreen.kt`, `FocusScreen.kt`, `FocusImmersiveOverlay.kt`, `ScheduleScreen.kt`, theme files.

---

_This redesign plan has been implemented in full (Phases 0–8) and smoke-verified on a physical device (§9 of the QA audit). Remaining release work: capture docs screenshots, close the open §7 release gates in `docs/frostlock-redesign-qa-audit.md` — notably instrumentation tests, midnight behaviour verification, and the accessibility sweep._
