# FrostLock vs Regain — complete feature inventory & gap analysis

Last updated: **2026-11-12**
FrostLock `com.frostlock.app` v1.0 · Regain `ai.regainapp` v75.1.2113

This file is the single source of truth for:

1. everything FrostLock currently does,
2. everything Regain does (from on-device inspection + its APK),
3. how the two differ and which is better at each thing,
4. everything Regain has that FrostLock does **not** have, and
5. what we deliberately refuse to copy.

Legend: `[x]` shipped in FrostLock · `[ ]` missing · `[~]` partial · `[!]` deliberately not doing.

---

## 1. Quick verdict

| Question | Answer |
| --- | --- |
| Which app blocks more surfaces? | **Regain.** It also blocks websites, YouTube Shorts/channels, notifications, the phone home screen, split screen, and floating windows. |
| Which app is better for privacy? | **FrostLock, by a wide margin.** No internet permission, no account, no ads, no analytics, no billing. |
| Which app is better for a free user? | **FrostLock.** Everything is free. Regain gates app-limit counts, unlimited schedules, premium stats, music and themes behind Pro. |
| Which app models app sets better? | **FrostLock.** Subjects own app sets; rules have their own app sets; one-off dates and overnight windows exist. Regain tags sessions but keeps the blocklist per rule/schedule. |
| Which app is more motivating? | **Regain** (groups, leaderboards, rewards, stamps, music, widgets). FrostLock is calmer and simpler. |
| Which app is easier to start using? | **FrostLock.** No sign-in wall; 10-slide onboarding then a timer. Regain requires Google sign-in to get past its intro. |
| Which is the better product overall? | **Regain is deeper and more polished; FrostLock is cleaner, fairer and private.** For our goal (offline, ice-themed, no accounts) FrostLock's direction is right — but its blocking depth is far behind Regain's. |

---

## 2. FrostLock — everything we have

### 2.1 Identity & platform

| Feature | Detail |
| --- | --- |
| App identity | FrostLock, `com.frostlock.app`, ice/freezer theme, custom frozen-lock launcher + round icon |
| Offline guarantees | Only permissions: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`. **No INTERNET.** No ads, analytics, billing or accounts |
| Storage | Room (sessions, streaks, profile) + SharedPreferences (limits, schedules, subjects, blocking, focus settings) + DataStore (onboarding done) |
| Backup | Android local backup only (`allowBackup`, extraction rules) |
| Localisation | English only (no `values-xx` folders) |
| APK size | ~21 MB debug APK (Regain base APK is ~58 MB + split APKs) |

### 2.2 Onboarding — 10 slides

| # | Slide | What it shows |
| --- | --- | --- |
| 1 | Welcome | Rotating orbit, layered snowflake emblem, “Focus, frozen in place”, pills: Offline / No account / Private |
| 2 | **Distraction Radar** | Animated icy radar with 10 real brand logos (Instagram, TikTok, YouTube, Snapchat, X, Reddit, Netflix, Facebook, Spotify, Pinterest); TikTok + YouTube carry lock badges |
| 3 | Focus toolkit | Timer mock: Pomodoro/Timer/Stopwatch, strict lock, 24:58 ring, break + round chips |
| 4 | App control | Card with **real installed app icons** (YouTube, Chrome, YouTube Music on this emulator), 2 limits + 1 blocked row, plus mini brand strip and “+4 more” |
| 5 | Automatic routine | Weekly schedule mock (morning study / deep work / evening reading) |
| 6 | Visible progress | 7-day bar chart, 7-day streak, 4 achievement hexagons |
| 7 | Goals | Multi-select: study, deep work, cut social, sleep, build habit |
| 8 | Triggers | Multi-select: notifications, social, boredom, stress, late nights, other |
| 9 | Reason | Free text, 10–300 characters, shown during focus |
| 10 | Commitment | Summary of answers + hold-for-2-seconds gradient button |

Also: animated aurora backdrop, 10-segment progress bar, “Skip intro” to personalisation, per-slide fade/slide-in, validation (goals/triggers/reason required), saves `ProfileEntity`, starts the first streak, sets `onboarding_done`.

### 2.3 Focus timer

| Feature | Detail |
| --- | --- |
| Modes | Pomodoro, Timer (countdown), Stopwatch |
| Length | 1 minute – 8 hours; last chosen duration remembered |
| Pomodoro | Rounds + configurable breaks (1–30 min), skip break, “Round N”, break countdown |
| Pause / resume | Available in normal mode; disabled in strict mode |
| Strict mode | No pause, no early exit until the timer ends; hold-to-end override |
| Session intent | “One thing for this session”, visible on timer + immersive screen |
| Subject/tag | Pick a subject for the session; each subject owns an app set |
| Post-session reflection | Note (max 280 chars), save, delete/discard last session |
| Focus Guard | Optional reminder every 10 minutes during a session |
| Immersive mode | Frosted disc, snowdrifts, ambient animation, minimisable |
| Background | Foreground service keeps timer + blocking alive with screen off |
| Persistence | Every finished session saved to Room; updates streak and widget |

### 2.4 App blocking

- Accessibility-service blocker with an in-app wall (`BlockActivity`) for blocked apps.
- Two independent tools:
  - **Focus blocking** — selected apps lock during a focus session (global list or subject's list).
  - **Daily limits** — per-app budgets enforced all day.
- Per-app picker with search, “Selected only / Show all”, app count and icons from the device.
- Block wall has three reasons: focus session, daily limit, schedule.
- Blocking toggle is separate from schedules (schedules keep running even if manual focus blocking is off).
- Schedule checks run on window changes **and** on a periodic ticker, so an app already open gets blocked when its window starts.

### 2.5 Daily app limits

| Feature | Detail |
| --- | --- |
| Budget | Per-app minutes, set from the app picker |
| Usage tracking | Foreground time measured by the accessibility service (no UsageStats permission) |
| Warning | Heads-up notification once per app per day at 80% of budget |
| Enforcement | Blocks at 100% until midnight; wall says limit reached |
| Reset | Automatic at midnight via date-keyed counters; old counters pruned |
| Suggestions | One-tap “Add 20m” for up to 4 installed distractor apps (YouTube, Instagram, Chrome, TikTok, Facebook, X, Reddit, Snapchat, Netflix, Spotify, YouTube Music) |
| Independence | Limits keep applying outside focus sessions |

### 2.6 Schedules (auto-blocking rules)

| Feature | Detail |
| --- | --- |
| Rule fields | Name, start/end time, weekdays, specific one-off dates, app set, enabled flag |
| Overnight windows | Supported (e.g. 22:00–02:00) |
| Per-rule apps | Every rule blocks its own app set; falls back to the focus-session list if empty |
| Copy apps | “Copy apps from a subject” |
| UI | List of rules, “Active now” badge, edit/delete, empty state |
| Reminders | Local notification when a rule starts **and** ends. Exact alarms when the OS allows them, with an inexact allow-while-idle fallback. Re-armed after boot, app update, clock/timezone changes and app launch. One toggle on the rules screen; no server involved |
| Enforcement | Runs independently of the manual focus switch |

### 2.7 Stats & achievements

| Feature | Detail |
| --- | --- |
| Home stats | Today’s focus, this week, session count, current streak, recent sessions |
| Stats screen | Today / last 7 days chart, total sessions, streak, **breakdown by subject**, recent sessions |
| Achievements | 16 offline badges: focus hours (1, 3, 5, 10, 25, 50, 100h), streaks (3, 7, 14, 30, 100 days), sessions (1, 10, 50, 100) |
| Badge UI | Milestone cabinet, hexagon badges, progress labels, unlocked/locked states |
| Notes | Post-session note saved to the DB, editable later in the study diary |

### 2.8 Home, profile, widget, notifications

| Area | Detail |
| --- | --- |
| Home | Greeting, today/week stats, quick actions (Block apps, Schedules), recent sessions |
| Profile | Daily goal (15 min – 8 h), Strict Mode, Pomodoro breaks, Focus Guard, widget help, “FrostLock Pro — free forever”, Coming soon list, About |
| Widget | One home-screen widget: today’s focus time, daily-goal progress, streak; tap opens the app; cached values |
| Notifications | Ongoing focus notification, 80% limit warning, Focus Guard nudge, schedule start/end reminders |
| Accessibility setup | In-app permission card with shortcut to system settings |

### 2.9 Engineering

- 62 unit tests across onboarding, schedules, schedule reminder triggers, app-limit warnings, suggested limits, showcase logos, achievements, stats, study diary, weekly reports and session summaries.
- Verified on emulator `Medium_Phone_API_37.0` including blocking and full onboarding.
- Key commits: rename/ice theme `c5920b2`, `7a690d7`, `a0dc222`; achievements `faf4fd9`; suggested limits `899f6ef`; immersive timer `a9af201`; onboarding redesign `fdc4ba5`; real installed icons `2e2c92e`; radar + real brand logos `45a6bb2`; study diary `24c8b39`; weekly report `1755d97`; session summary `cda5f82`.

---

## 3. Regain — everything it has

Sources: installed APK v75.1.2113 (manifest activities/services, 96k resource lines, 48k unique strings), on-device splash/onboarding, and the earlier research notes in git history (`ab74654`, `fb74d3e`).

### 3.1 Onboarding & account

- Conversational onboarding with a mascot (“Rega”).
- Asks exam type (engineering, medical, law, banking, railway, UPSC, board, college, etc.), exam date/month, biggest distraction, daily focus goal, and commitment strictness.
- Builds a recommended blocklist from the answers, then tours one feature per screen: Shorts/Reels, notification blocking, Strict Mode, YouTube channels.
- Permission wizard explains accessibility, usage access, overlay, notification access.
- **Google sign-in required** to get past the intro; account creation, email, restore backup, remote logout.
- Social proof (“Trusted by 5M+ students”, “500,000+ students increased YouTube study time by 2x”).

### 3.2 Focus timer

- Focus timer, Pomodoro, stopwatch; custom durations.
- Breaks between rounds; max-pause limits; “you cannot end during focus”, end-only-during-break rule.
- Strict mode (“no cheating till timer ends”), “make this session stricter”.
- Session summary at the end; session tags; skip/discard session.
- Focus music/ambience (rain, deep-focus, “science-backed”) and premium themes (“Crystal lock” UI).
- Floating focus bubble/button and a focus widget.
- Scheduled-session alarms (“alarm for your scheduled focus sessions”).
- “C68 focus experience” (special full-screen timer experience).

### 3.3 App blocking

- Choose distracting apps; block them during focus.
- Strict block for apps, with “block completely once the limit ends”.
- Block floating windows and split screen for blocked apps.
- Block the phone home screen during focus (requires login).
- Uninstall protection via device admin (“you cannot uninstall Regain while a session is running”).
- Blocks Play Store searches for blocked apps (“blocks you the second you search Instagram on Play Store”).
- Accessibility-based; overlay wall with “show block overlay”.

### 3.4 App limits

- Per-app daily usage limits (UsageStats permission).
- Strict mode for limits; reminders before the limit; suggested limits based on usage.
- “Block completely once the daily limit ends”; unlimited limits only with Pro.
- Limit streaks and rewards for staying under the limit.
- Snooze / increase-limit flows.

### 3.5 Website & content blocking

- Block distracting websites and adult websites.
- Strict website mode.
- Allow only “study websites” (allowlist) while blocking everything else.
- Block YouTube Shorts, Instagram/Facebook Reels, Snapchat Spotlight.
- Block the YouTube home page.
- Block distracting YouTube channels; allow only “study channels”.
- Per-site confirmation phrases, reset flows, “report as study site/channel”.

### 3.6 Notification blocking

- Notification access + NotificationListener.
- Block distracting notifications during focus and view them later in a Regain inbox.
- Per-app notification ranking (“apps that send me most notifications”).
- Pause notification blocking; schedule it; allowed-notification examples.
- “Block notification & view later”, “block notifications from blocked apps”.
- Wake-up notifications from group members; “Focus Guilt Trip” and usage-standing notifications.

### 3.7 Schedules / Zen mode

- Recurring focus schedules (“Zen mode”) with days, time windows, own blocked-app sets, habit apps, colours, emoji, break rules and strict flags.
- Alarm reminders when a scheduled session starts. **FrostLock has this** (local start/end notifications, no account needed).
- Calendar view of focus sessions and a monthly focus calendar widget.
- Upcoming sessions, session-overlap detection, morning/afternoon/evening/night sessions.
- Exam date aware (“Only during exam”, “Exam month”); session streak separate from daily streak.

### 3.8 Stats & reports

- Today / this week / this month focus time; total focus; average focus per session; session count.
- Weekly reports (“Your weekly report is ready”), view report, past days (Pro), monthly data (Pro).
- Usage widgets, phone-unlock count and tracking.
- Weekly leaderboard (Pro/social) with region-based reset times.

### 3.9 Streaks, achievements & rewards

- Current streak, best streak, session streak, schedule streak, streak-loss warnings.
- Achievement badges, “Your Achievements”, share achievement, achievement unlocked screens.
- Focus stamps: add a photo after a session and collect stamps.
- Milestones and invite milestones; rewards for focusing; creator rewards.
- Difficulty levels, levels, avatar creation (“Creating your avatar”), focus wizard offers.

### 3.10 Social

- Focus groups: create, join, invite links, group image/description/category, pin, report, delete, leave.
- Weekly group leaderboards; “X is focusing now”, “X paused their timer”.
- Wake-up nudges between members; “no one focused this week” states.
- Regional group discovery and reset times; group visibility/privacy.
- Friends invites, referral rewards, “invite 5 friends” milestones.
- Group chat/discussion strings exist (not verified in UI).

### 3.11 Widgets

Nine receivers / eight widget types: Focus, Usage (small + large), Phone Unlock Count, Rega Goal, Study Goal, Study Calendar, Pro Pass (2×2 and 4×2).

### 3.12 Monetisation, platform & tech

- Pro subscription, free trial, Pro pass widgets, limited free app limits/schedules/sessions, premium stats/music/themes.
- Razorpay + Google Play Billing, referral offers, creator rewards.
- Android permissions: usage stats, accessibility, overlay, notification listener, device admin, exact alarms, boot, camera, internet, billing, ads/analytics (Facebook SDK, Firebase, AppsFlyer/Adjust).
- Multi-language resources (dozens of locales).
- Heavy analytics/experiments (“Focus Badge Test: ON/OFF”, “Sell Block Shorts: Control/Treatment”).
- Email/cloud reports, backup/restore, remote logout.

---

## 4. Side-by-side matrix

| # | Capability | FrostLock | Regain | Difference | Better |
| --- | --- | --- | --- | --- | --- |
| 1 | Focus timer (Pomodoro/countdown/stopwatch) | Yes | Yes | Regain adds skip/discard and session summary | Tie |
| 2 | Configurable breaks | Yes (1–30 min) | Yes | Similar | Tie |
| 3 | Strict mode (no early exit) | Yes + hold-to-end | Yes, “end only during break” | Similar intent | Tie |
| 4 | Subject/tags per session | Subject owns an app set | Tag label only | **FrostLock stronger** | FrostLock |
| 5 | App blocking during focus | Yes | Yes | Both accessibility-based | Tie |
| 6 | Per-rule app sets | Yes | Yes | Similar | Tie |
| 7 | One-off date rules | Yes | No (recurring + exam dates) | FrostLock more flexible | FrostLock |
| 8 | Overnight windows | Yes | Yes (time windows) | Similar | Tie |
| 9 | Daily app limits | Yes, tracked locally | Yes, via UsageStats | Regain has strict/snooze/limit streaks; FrostLock simpler and no extra permission | Regain (depth) / FrostLock (privacy) |
| 10 | Suggested limits | One-tap 20-min for 4 apps | Usage-based suggestions | Different approaches | Tie |
| 11 | Website blocking | **No** | Yes + adult + allowlist | Big gap | Regain |
| 12 | Shorts / Reels / Spotlight blocking | **No** | Yes | Big gap | Regain |
| 13 | YouTube channel filtering | **No** | Yes + study channels | Big gap | Regain |
| 14 | YouTube home-page blocking | **No** | Yes | Gap | Regain |
| 15 | Notification blocking + inbox | **No** | Yes | Big gap | Regain |
| 16 | Home-screen blocking | **No** | Yes (login required) | Gap | Regain |
| 17 | Split-screen / floating-window blocking | **No** | Yes | Gap | Regain |
| 18 | Uninstall protection | **No** | Yes (device admin) | Gap; also anti-user | Regain (effectiveness) |
| 19 | Schedule start alarms / reminders | **Yes** — local notifications at start and end, with exact-alarm fallback | Yes | Similar | Tie |
| 20 | Schedule calendar view | **No** (list only) | Yes | Gap | Regain |
| 21 | Upcoming sessions / overlap detection | **No** | Yes | Gap | Regain |
| 22 | Exam-aware onboarding | Goals/triggers only | Exam, date, region | Gap | Regain |
| 23 | Focus groups / friends | **No** | Yes | Gap | Regain |
| 24 | Weekly leaderboard | **No** | Yes | Gap | Regain |
| 25 | Wake-up nudges / accountability | Focus Guard only | Group nudges + guilt trips | Different | Regain (social) |
| 26 | Streaks | Daily streak + best | Daily/session/schedule streak | Similar | Tie |
| 27 | Achievements | 16 offline badges | Badges + share + rewards | Regain more gamified | Regain (motivation) / FrostLock (offline) |
| 28 | Rewards / levels / stamps | **No** | Yes | Gap | Regain |
| 29 | Focus music / themes | **No** | Yes (premium) | Gap | Regain |
| 30 | Widgets | 1 (focus/goal/streak) | 8 types incl. usage, unlock count, calendar | Big gap | Regain |
| 31 | Phone-unlock tracking | **No** | Yes (widget + stats) | Gap | Regain |
| 32 | Weekly report | **Yes** — week navigation, comparison, daily bars, best day, subjects | Yes (Pro for past days) | Similar; FrostLock keeps it free | Tie / FrostLock |
| 33 | Session notes | Yes — editable from the diary and the post-session summary | Yes + study stamps/photos | FrostLock surfaces them too; Regain adds photos | Tie |
| 34 | Study diary | **Yes** — day grouping, search, reflective notes | Yes (notes + stamps) | FrostLock matches the notes side; Regain adds stamps | Tie / Regain (stamps) |
| 35 | Focus bubble / floating timer | **No** | Yes | Gap | Regain |
| 36 | Accounts / cloud sync | **No** (local only) | Yes + backup/restore | Opposite design choices | FrostLock (privacy) |
| 37 | Ads / analytics / tracking | **No** | Yes (SDKs) | Opposite | FrostLock |
| 38 | Subscription / paywall | **No** — everything free | Pro tier + free caps | Opposite | FrostLock |
| 39 | Works offline | **Yes, fully** | Mostly, but login/groups/reports need network | Opposite | FrostLock |
| 40 | Onboarding effort | 10 quick slides | Long conversational + permissions + login | FrostLock faster | FrostLock |
| 41 | APK size | ~21 MB debug | ~58 MB base + splits | FrostLock smaller | FrostLock |
| 42 | Localisation | English only | Many languages | Gap | Regain |
| 43 | Permission footprint | 3 permissions | 15+ incl. usage, notifications, admin | FrostLock safer | FrostLock |
| 44 | Usage tracking method | Accessibility foreground time | UsageStats permission | Both valid; FrostLock avoids a permission | FrostLock (privacy) |
| 45 | Adult-content blocking | **No** | Yes | Gap | Regain |
| 46 | Study websites/channels allowlist | **No** | Yes | Gap | Regain |
| 47 | Session summary screen | Reflection dialog only | Full summary | Gap | Regain |
| 48 | Pause limits | Strict disables pause | Max pauses per session | Gap | Regain |
| 49 | Multi-language UI | **No** | Yes | Gap | Regain |
| 50 | Data export / backup beyond OS | **No** | Cloud backup | Different | FrostLock (privacy) |

---

## 5. What we adopted from Regain (and when)

| Idea from Regain | What we built | Commit |
| --- | --- | --- |
| Achievement badges | 16 offline badges + milestone cabinet | `faf4fd9` |
| Smart/suggested limits | One-tap 20-minute limits for installed distractors | `899f6ef` |
| Immersive focus experience | Frosted disc + snowdrifts timer overlay | `a9af201` |
| Feature-tour onboarding | 4 showcase slides with mock UI + goals/triggers/reason | `fdc4ba5` |
| “Wall of distracting apps” at the start | Distraction Radar with 10 real brand logos | `45a6bb2` |
| Real app icons in the flow | App Control slide reads launcher icons from the device | `2e2c92e` |
| Session intention | “One thing for this session”, shown while focusing | `3753155` |
| Research discipline | Earlier Regain research notes (since removed in rename) | `ab74654`, `fb74d3e` |

Superseded: `20b18ab` added hand-drawn logo vectors; user rejected them, and `2e2c92e` replaced them with real icons.

---

## 6. Everything Regain has that FrostLock does NOT

### A. Blocking coverage

- [ ] Website blocking (domains, social sites, adult content)
- [ ] Study-website allowlist
- [ ] Adult-content blocking
- [ ] YouTube Shorts blocking
- [ ] Instagram/Facebook Reels + Snapchat Spotlight blocking
- [ ] YouTube home-page blocking
- [ ] YouTube channel blocking
- [ ] Study-channel allowlist
- [ ] Notification blocking + blocked-notification inbox
- [ ] Per-app notification ranking
- [ ] Pause/schedule notification blocking
- [ ] Phone home-screen blocking during focus
- [ ] Split-screen blocking
- [ ] Floating-window blocking
- [ ] Uninstall protection / device admin
- [ ] Play Store search blocking for blocked apps

### B. Routine, reminders & planning

- [x] Alarm/notification when a scheduled session starts (and ends)
- [ ] Calendar view of sessions
- [ ] Upcoming-session list
- [ ] Session-overlap detection
- [ ] Exam date/month awareness
- [ ] Morning/afternoon/evening/night session presets
- [ ] Schedule streak (separate from daily streak)
- [ ] Session skip/discard + max-pause rules
- [x] Full session summary screen

### C. Social & accountability

- [ ] Accounts (Google sign-in)
- [ ] Focus groups (create/join/invite)
- [ ] Weekly leaderboards
- [ ] Group member “focusing now / paused” states
- [ ] Wake-up nudges between friends
- [ ] Referral / invite milestones
- [ ] Regional group discovery

### D. Habit, motivation & content

- [ ] Focus music / ambience
- [ ] Premium themes (Crystal lock etc.)
- [ ] Rewards, levels, difficulty levels
- [ ] Focus stamps (photo after session)
- [x] Study diary (day grouping, search, editable reflections)
- [ ] Phone-unlock tracking
- [ ] Focus bubble / floating timer
- [ ] Eight widget types (usage, unlock count, goal, calendar, Pro pass)
- [ ] Achievement sharing

### E. Data, accounts & platform

- [ ] Cloud backup / restore
- [ ] Remote logout
- [ ] Weekly email/in-app report
- [ ] Monthly stats and past-day browsing
- [ ] Multi-language UI
- [ ] UsageStats-based tracking
- [ ] Pro subscription / free-tier caps
- [ ] Ads / analytics / experimentation

---

## 7. What FrostLock has that Regain does not (or does better)

- **Fully offline.** No INTERNET permission; blocking, stats, badges and onboarding all work in airplane mode.
- **No account.** No sign-in wall; the app is usable seconds after install.
- **No ads, analytics, billing, or Pro tier.** “Free forever” is literal.
- **3 permissions vs 15+.** No usage-access, notification-listener, device-admin or camera permissions.
- **Subject profiles own app sets.** “Physics” and “Work” can block different apps; sessions reuse them.
- **One-off date rules.** Block only on an exam day, not every week.
- **Per-rule app sets + overnight windows.** More flexible than Regain's schedule model in these two respects.
- **One-tap suggested limits** for the noisiest installed apps.
- **Focus Guard** gentle 10-minute reminders during a session.
- **16 offline achievement badges** with a milestone cabinet.
- **Smaller APK (~21 MB)** and a simpler, calmer UI.
- **Distinct ice identity** rather than a generic green productivity theme.

---

## 8. Deliberately NOT copying (and why)

| Regain feature | Why we are not doing it |
| --- | --- |
| Accounts / Google sign-in | Breaks the offline, no-account promise; adds a server dependency for zero core value |
| Focus groups / leaderboards / friends | Requires network, accounts, moderation and privacy handling; explicitly rejected for FrostLock |
| Pro subscription / free-tier caps | We promised free forever; paywalling limits or schedules would contradict the product |
| Ads / analytics / A/B experiments | Privacy-first product; no tracking SDKs, no AD_ID, no INTERNET |
| AI coach / mascot chat | Needs network/model; adds noise to a calm tool |
| Aggressive guilt-trip notifications | Manipulative; FrostLock uses gentle, optional nudges only |
| Uninstall protection / device admin | Anti-user and risky; we should not prevent someone from removing the app |
| Cloud backup / remote logout | Local-only data model; Android OS backup is enough |
| Exam data collection (exam type/date/region) | Not needed for core blocking; can be a local, optional goal instead |

---

## 9. Which is better, and where

### Regain is better when the user wants…

- maximum distraction coverage (sites, Shorts/Reels, notifications, home screen, split screen),
- accountability and competition (groups, leaderboards, nudges),
- motivation and ritual (music, themes, rewards, stamps, many widgets),
- exam-oriented planning (exam dates, study channels/sites, calendar),
- a polished, content-rich experience and is willing to pay/sign in.

### FrostLock is better when the user wants…

- privacy and offline operation,
- no account, no ads, no subscription,
- a simple timer + app blocker that just works,
- flexible app-set modelling (subjects, per-rule lists, one-off dates),
- a calm, non-gamified tool.

### Honest summary

Regain is the stronger **blocker** and the stronger **habit/social product**.
FrostLock is the stronger **privacy product** and the cleaner **core tool**.
FrostLock's biggest weakness is not quality — it is **coverage**: it only blocks app launches, not websites, notifications, Shorts/Reels, or system surfaces.

---

## 10. Roadmap — what to build next (offline-first only)

### P0 — close the most painful gaps without breaking privacy

- [x] **Schedule start/end alarms + notifications** (`AlarmManager` + boot receiver). Rules currently act silently.
- [x] **Study diary screen** to surface saved session notes (already in DB, no UI).
- [ ] **Notification blocking** via `NotificationListenerService` + a local “blocked notifications” inbox, with per-app pause and allow rules.
- [ ] **More widgets**: usage today, phone-unlock count (needs a counter), study goal, calendar.
- [x] **Weekly report screen** (local aggregation of existing sessions).
- [x] **Session summary** (planned vs actual, subject, note, streak impact).

### P1 — deeper blocking (needs design decisions)

- [ ] **Website blocking.** Options: local VPN service (real blocking, extra permission) or accessibility URL reading (fragile). Decide before building; a VPN is the honest solution.
- [ ] **Shorts/Reels detection.** Accessibility title/view-id heuristics per app; brittle, needs per-app maintenance.
- [ ] **YouTube channel/home filtering.** Heuristics only; document limitations.
- [ ] **Home-screen blocking** and **split-screen/floating-window blocking** on top of the existing accessibility service.
- [ ] **Focus music/ambience** with bundled local audio (no streaming).
- [ ] **Theme variants** (more ice palettes).

### P2 — motivation & polish

- [ ] Focus stamps / session photos (local only, optional camera permission).
- [ ] Reward/level layer on top of the existing badges (no currency, no store).
- [ ] Focus bubble / floating timer.
- [ ] Exam/goal profiles stored locally (no exam-data upload).
- [ ] Schedule streak.
- [ ] Multi-language resources.

### Never

- Accounts, cloud sync, groups, leaderboards, Pro/subscriptions, ads, analytics, AI coach, uninstall protection.

---

## 11. Verified facts & commands

- Build: `JAVA_HOME=/snap/android-studio/250/jbr ANDROID_HOME=/home/vishal/Android/Sdk ./gradlew :app:assembleDebug`
- Tests: `:app:testDebugUnitTest` → **42 tests, 0 failures**
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Accessibility component: `com.frostlock.app/com.frostlock.app.blocking.FocusBlockAccessibilityService`
- Re-enable service:
  ```bash
  adb shell settings put secure enabled_accessibility_services com.frostlock.app/com.frostlock.app.blocking.FocusBlockAccessibilityService
  adb shell settings put secure accessibility_enabled 1
  ```
- Regain APK inspected at `/tmp/opencode/regain.apk` (v75.1.2113, 58 MB base).
- FrostLock permissions: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` only.
- FrostLock manifest: accessibility service, focus foreground service, one widget receiver, no boot receiver, no alarm receiver, no notification listener.
- Test files: 8 (`OnboardingViewModelTest`, `SuggestedLimitsTest`, `OnboardingShowcaseTest`, schedule/limit/achievement/stats tests).
- Current onboarding page count: 10 (`OnboardingPages.COUNT`).

---

## 12. Decision log / open questions

1. **Website blocking:** is a local VPN acceptable? It is the only reliable offline way. If not, website blocking stays out.
2. **Notification blocking:** `NotificationListenerService` is powerful but sensitive; do we want it? Regain does. It would close a major gap.
3. **Schedule alarms:** adding `AlarmManager` + boot receiver is low risk and high value.
4. **Diary:** the note field already exists; surfacing it is a small UI task.
5. **Keep the strict no-network promise** even for widgets/reports — everything must remain local.
6. **Do not drift into Regain's scope**: social, accounts, paywalls and ads stay out by design.

---

_Update this file whenever a feature is added or a gap is deliberately closed. It is the memory of what FrostLock is and is not._
