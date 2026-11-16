# FrostLock vs Regain — complete feature inventory & gap analysis

Last updated: **2026-11-16**
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
| Which app blocks more surfaces? | **Close to a tie.** FrostLock now also filters YouTube videos by channel (block list or study allowlist), so the remaining Regain edge is channel handling on YouTube Music + study *sites*. FrostLock blocks websites, Shorts/Reels, the YouTube home feed, Play Store searches for blocked apps, the phone home screen, split screen, and floating windows during focus/schedule, and holds notifications from blocked apps. |
| Which app is better for privacy? | **FrostLock, by a wide margin.** No internet permission, no account, no ads, no analytics, no billing. |
| Which app is better for a free user? | **FrostLock.** Everything is free. Regain gates app-limit counts, unlimited schedules, premium stats, music and themes behind Pro. |
| Which app models app sets better? | **FrostLock.** Subjects own app sets; rules have their own app sets; one-off dates and overnight windows exist. Regain tags sessions but keeps the blocklist per rule/schedule. |
| Which app is more motivating? | **Regain** (groups, leaderboards, rewards, music, more widgets). FrostLock now adds session stamps but stays calmer and simpler. |
| Which app is easier to start using? | **FrostLock.** No sign-in wall; 10-slide onboarding then a timer. Regain requires Google sign-in to get past its intro. |
| Which is the better product overall? | **Regain is deeper and more polished; FrostLock is cleaner, fairer and private.** For our goal (offline, ice-themed, no accounts) FrostLock's direction is right — but its blocking depth is far behind Regain's. |

---

## 2. FrostLock — everything we have

### 2.1 Identity & platform

| Feature | Detail |
| --- | --- |
| App identity | FrostLock, `com.frostlock.app`, ice/freezer theme, custom frozen-lock launcher + round icon, four selectable ice palettes (Frost, Aurora, Midnight, Crystal) |
| Offline guarantees | Only permissions: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, exact alarms, boot, and an optional overlay permission for the floating timer. Photos use the system picker, so there is **no camera or storage permission**. **No INTERNET.** No ads, analytics, billing or accounts |
| Storage | Room (sessions, streaks, profile) + SharedPreferences (limits, schedules, subjects, blocking, focus settings) + DataStore (onboarding done) + app-private JPEGs for session photos |
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
| Pause / resume | Available in normal mode; capped by a per-session pause allowance (Off, 1, 2, 3, or no limit) |
| Pause allowance | Set on Profile; defaults to 2 pauses per session and resets each round. Strict Mode ignores it and blocks pausing outright |
| Strict mode | No pause, no early exit until the timer ends; hold-to-end override |
| Session intent | “One thing for this session”, visible on timer + immersive screen |
| Subject/tag | Pick a subject for the session; each subject owns an app set |
| Post-session reflection | Note (max 280 chars), save, delete/discard last session |
| End / discard | Stopping a 5s+ session asks whether to save it or discard it with no diary or stats entry |
| Focus Guard | Optional reminder every 10 minutes during a session |
| Focus ambience | Off, Deep focus, Rain, Wind or Snow; synthesised on-device and played by the focus service, with no audio assets or network |
| Immersive mode | Frosted disc, snowdrifts, ambient animation, minimisable |
| Background | Foreground service keeps timer + blocking alive with screen off |
| Persistence | Every finished session saved to Room; updates streak and widget |

### 2.4 App blocking

- Accessibility-service blocker with an in-app wall (`BlockActivity`) for blocked apps.
- Two independent tools:
  - **Focus blocking** — selected apps lock during a focus session (global list or subject's list).
  - **Daily limits** — per-app budgets enforced all day.
- Per-app picker with search, “Selected only / Show all”, app count and icons from the device.
- Block wall has four reasons: focus session, daily limit, schedule, website.
- **Website blocking** (opt-in) uses the same accessibility service to read the browser address bar. A local list holds curated social/video and adult domains plus any custom sites; matches show a “Site held” wall only while a focus session or schedule rule is active. No VPN, no INTERNET permission, no browsing history.
- **YouTube home-feed blocking** (opt-in, off by default) walls only YouTube's recommendation home feed while a session or rule is active. The signal is the selected bottom-navigation tab, so search, subscriptions and video playback stay usable; YouTube is skipped entirely when the whole app is blocked. The wall has its own “Home feed held” copy.
- **YouTube channel filtering** (opt-in, off by default) judges each watch page's owner channel — a block list holds listed channels, a “Study list” mode holds everything not allowlisted — while a session or rule is active. The channel is parsed from the watch page's owner-row accessibility labels only; a Channels screen under Profile manages both lists. The wall has its own “Channel held” copy.
- **Shorts/Reels hardening** — apps with a known nav tab (YouTube, Instagram, Facebook) are flagged only when that tab is selected or the short-form player surface (e.g. YouTube's `reel_*` view ids) is on screen. Snapchat, which exposes no nav tab, keeps the text heuristic. This stops a home feed's Shorts shelf from triggering the Shorts wall.
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
| UI | List of rules, “Active now” badge, edit/delete, empty state; add/edit opens a dedicated full-screen editor with named cards for name, apps, repeat and time window |
| Time presets | One-tap Morning (06:00–09:00), Workday (09:00–17:00), Evening (18:00–22:00) and Night (22:00–02:00) windows |
| Upcoming | A “Coming up” card lists the next 7 days of rule starts, with live countdowns and overnight-aware ends |
| Overlap warnings | The editor warns when a window collides with another rule (including overnight tails and one-off dates) and names the shared days/dates; the rules list badges every colliding rule. Informational only — both rules stay active |
| Reminders | Local notification when a rule starts **and** ends. Exact alarms when the OS allows them, with an inexact allow-while-idle fallback. Re-armed after boot, app update, clock/timezone changes and app launch. One toggle on the rules screen; no server involved |
| Enforcement | Runs independently of the manual focus switch |

### 2.7 Stats & achievements

| Feature | Detail |
| --- | --- |
| Home stats | Today’s focus, this week, session count, current streak, recent sessions |
| Stats screen | Today / last 7 days chart, total sessions, streak, **monthly rhythm** (six-month bars, active days, best day, average per active day), **breakdown by subject**, **focus calendar** (month heat-map + per-day drill-down), recent sessions |
| Achievements | 16 offline badges: focus hours (1, 3, 5, 10, 25, 50, 100h), streaks (3, 7, 14, 30, 100 days), sessions (1, 10, 50, 100) |
| Freeze levels | Seven lifetime titles (Frostbite → Absolute zero) with widening thresholds, a dedicated ladder page, progress to the next rung and text sharing |
| Badge UI | Milestone cabinet, hexagon badges, progress labels, unlocked/locked states |
| Notes | Post-session note saved to the DB, editable later in the study diary |

### 2.8 Home, profile, widget, notifications

| Area | Detail |
| --- | --- |
| Home | Greeting, today/week stats, quick actions (Block apps, Schedules), focus-deadline countdown, recent sessions |
| Profile | Daily goal (15 min – 8 h), Strict Mode, Pomodoro breaks, Focus Guard, focus deadline, focus goals, widget help, “FrostLock Pro — free forever”, Coming soon list, About |
| Focus deadline | Name an exam/launch/goal and pick a local date. A dedicated page previews the countdown live, Home shows the remaining days, Profile summarises it, and it can be cleared. Stored as an ISO date only; no calendar permission, account or upload |
| Focus goals | Local exam/project targets with a name, target date and target hours. A dedicated list shows progress bars, focused time, percent, days left and an on-track/behind/complete state; a full-screen editor handles name, quick 10/25/50/100-hour presets, a custom amount and the themed date dialog. Progress counts only sessions finished inside the goal window, all from local data |
| Widgets | Four home-screen widgets: **Focus today** (time, goal progress, streak), **Daily goal** (percent, time left, streak), **This week** (Monday-first bars, weekly total, average, best day) and **Focus calendar** (month heat-map with today ring and totals). Tap opens the app; values cached so the first frame is never blank; can be pinned from Profile |
| Notifications | Ongoing focus notification, 80% limit warning, Focus Guard nudge, schedule start/end reminders, held-notification alerts |
| Accessibility setup | In-app permission card with shortcut to system settings |

### 2.9 Notification shield

| Feature | Detail |
| --- | --- |
| Access | Optional, off by default. Needs Android “notification access”; the Block screen and inbox show whether it is granted and deep-link to the setting |
| What is held | Only notifications from apps that are blocked **right now** — an active rule (reason shows the rule name), a reached daily limit, or a manual focus session |
| Always passes | Calls, alarms, foreground-service/ongoing notifications, group summaries, non-clearable notifications and FrostLock’s own alerts |
| Inbox | Day grouping (Today / Yesterday / date), real app icon, time, title, body, reason chip, delete one, clear all; capped at 200 entries |
| Ranking | “Most held apps” ranks the top three senders with counts and share bars, computed locally from the archived entries |
| Storage | Local SharedPreferences JSON only. Nothing is uploaded, and clearing the inbox deletes it permanently |
| Scope | A single global toggle that follows focus sessions, rules and limits automatically — no separate schedule to maintain |
| Enforcement detail | The notification is cancelled before it reaches the shade and archived in the same pass, so the shade never shows a blocked alert |

### 2.10 Engineering

- 250 unit tests across 38 test classes covering onboarding, schedules, rule editor helpers and time presets, schedule reminder triggers, rule-overlap detection, schedule streaks, deadline countdowns, session stamps, freeze levels and the title ladder, focus-goal windows and pacing, website domain matching and address-bar parsing, achievement sharing, bubble labels, pause limits, session photo naming and downsampling, app-limit warnings, suggested limits, showcase logos, achievements, stats, monthly rollups, focus calendar, upcoming sessions, study diary, weekly reports, session summaries, ambience synthesis, notification decisions, inbox grouping and ranking, widget maths, home-screen blocking, multi-window detection, unlock counts, the YouTube/Shorts surface detectors, Play Store search matching, and YouTube channel-owner parsing.
- Verified on emulator `Medium_Phone_API_37.0` including blocking and full onboarding, plus a live signed-in Play Store search test on a retail device (Realme, Android 16): blocked-app search walls, non-blocked query and result-page browsing do not.
- Key commits: rename/ice theme `c5920b2`, `7a690d7`, `a0dc222`; achievements `faf4fd9`; suggested limits `899f6ef`; immersive timer `a9af201`; onboarding redesign `fdc4ba5`; real installed icons `2e2c92e`; radar + real brand logos `45a6bb2`; study diary `24c8b39`; weekly report `1755d97`; session summary `cda5f82`; notification shield `1279084`; extra widgets `18ef9fc`; widget full-height layout `7baee2b`; dedicated rule editor `67865e1`; focus calendar, upcoming sessions and rule presets `5b33fbc`; calendar widget `81d330d`; overlap warnings `091ba55`; focus deadline `31e69c3`; focus stamps `1c27ae4`; freeze level `249975b`; achievement sharing `8b0d3d5`; schedule streak `95de98b`; floating focus bubble `b00e586`; session photos `8915202`; pause limits `b45488f`; ice themes `d0a5075`; monthly stats `6153caa`; focus ambience `d0c5c8c`; notification ranking `1e6ed58`; freeze ladder `2a997a7`; focus goals `8be538d`; website blocking `2b0df05`; home-screen blocking `c260e88`; split-screen/floating-window blocking `36be05f`; YouTube home-feed blocking + Shorts hardening `d6cb3b6`; Play Store search blocking `0c20a9c`; YouTube channel filtering + study allowlist `bbf44b4`.

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
- Floating focus bubble/button and a focus widget. FrostLock now has an opt-in draggable bubble.
- Scheduled-session alarms (“alarm for your scheduled focus sessions”).
- “C68 focus experience” (special full-screen timer experience).

### 3.3 App blocking

- Choose distracting apps; block them during focus.
- Strict block for apps, with “block completely once the limit ends”.
- Block floating windows and split screen for blocked apps.
- Block the phone home screen during focus (requires login).
- Uninstall protection via device admin (“you cannot uninstall Regain while a session is running”).
- Blocks Play Store searches for blocked apps (“blocks you the second you search Instagram on Play Store”). FrostLock now matches the store's search box against the blocked apps' display names, offline.
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
- Block distracting YouTube channels; allow only “study channels”. FrostLock now does this offline via the watch page's owner row.
- Per-site confirmation phrases, reset flows, “report as study site/channel”.

### 3.6 Notification blocking

- Notification access + NotificationListener.
- Block distracting notifications during focus and view them later in a Regain inbox. **FrostLock has this** (local-only shield + inbox, enabled per blocked app).
- Per-app notification ranking (“apps that send me most notifications”). **FrostLock has this** as a local “Most held apps” card.
- Pause notification blocking; schedule it; allowed-notification examples.
- “Block notification & view later”, “block notifications from blocked apps”.
- Wake-up notifications from group members; “Focus Guilt Trip” and usage-standing notifications.

### 3.7 Schedules / Zen mode

- Recurring focus schedules (“Zen mode”) with days, time windows, own blocked-app sets, habit apps, colours, emoji, break rules and strict flags.
- Alarm reminders when a scheduled session starts. **FrostLock has this** (local start/end notifications, no account needed).
- Calendar view of focus sessions and a monthly focus calendar widget. **FrostLock has the calendar screen** (month heat-map + day drill-down); the monthly widget is still missing.
- Upcoming sessions, session-overlap detection, morning/afternoon/evening/night sessions. **FrostLock has the upcoming list, one-tap time presets and overlap warnings.**
- Exam date aware (“Only during exam”, “Exam month”); session streak separate from daily streak. **FrostLock has a local deadline countdown**; exam-tuned scheduling is still missing.

### 3.8 Stats & reports

- Today / this week / this month focus time; total focus; average focus per session; session count.
- Weekly reports (“Your weekly report is ready”), view report, past days (Pro), monthly data (Pro).
- Usage widgets, phone-unlock count and tracking.
- Weekly leaderboard (Pro/social) with region-based reset times.

### 3.9 Streaks, achievements & rewards

- Current streak, best streak, session streak, schedule streak, streak-loss warnings. FrostLock now shows a schedule streak on the rules screen.
- Achievement badges, “Your Achievements”, share achievement, achievement unlocked screens. FrostLock now shares unlocked badges through the system share sheet.
- Focus stamps and session photos: collect badges per session from its start time, weekday and length, and attach one local photo after a session. Photos use the system picker and stay in app-private storage.
- Levels: FrostLock derives a local freeze level from lifetime focus minutes, with no currency or store.
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

Nine receivers / eight widget types: Focus, Usage (small + large), Phone Unlock Count, Rega Goal, Study Goal, Study Calendar, Pro Pass (2×2 and 4×2). **FrostLock has four** (focus today, daily goal, week at a glance, focus calendar) with no usage/unlock permissions.

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
| 1 | Focus timer (Pomodoro/countdown/stopwatch) | Yes + save/discard on stop + pause cap | Yes | Regain adds skip/discard and session summary; FrostLock now has both plus a local pause allowance | Tie |
| 2 | Configurable breaks | Yes (1–30 min) | Yes | Similar | Tie |
| 3 | Strict mode (no early exit) | Yes + hold-to-end | Yes, “end only during break” | Similar intent | Tie |
| 4 | Subject/tags per session | Subject owns an app set | Tag label only | **FrostLock stronger** | FrostLock |
| 5 | App blocking during focus | Yes | Yes | Both accessibility-based | Tie |
| 6 | Per-rule app sets | Yes | Yes | Similar | Tie |
| 7 | One-off date rules | Yes | No (recurring + exam dates) | FrostLock more flexible | FrostLock |
| 8 | Overnight windows | Yes | Yes (time windows) | Similar | Tie |
| 9 | Daily app limits | Yes, tracked locally | Yes, via UsageStats | Regain has strict/snooze/limit streaks; FrostLock simpler and no extra permission | Regain (depth) / FrostLock (privacy) |
| 10 | Suggested limits | One-tap 20-min for 4 apps | Usage-based suggestions | Different approaches | Tie |
| 11 | Website blocking | **Partial** — accessibility address-bar detection with a local list (curated social/video + adult, plus custom domains) and a local allowlist; active only during focus or a rule; no VPN | Yes + adult + allowlist | FrostLock covers the common cases offline without a VPN | Tie / FrostLock (offline) |
| 12 | Shorts / Reels / Spotlight blocking | **Yes** — accessibility detection for YouTube, Instagram, Facebook and Snapchat while focus/schedule is active; apps with a known nav tab are flagged only when that tab is selected or the player surface is on screen | Yes | Similar; FrostLock is local and free | Tie |
| 13 | YouTube channel filtering | **Yes** — opt-in toggle; the watch page's owner row (Subscribe button, channel avatar) is parsed from accessibility descriptions; block list or study allowlist; only during focus/schedule; skipped when the whole app is blocked | Yes + study channels | Similar; FrostLock works offline with no account | Tie |
| 14 | YouTube home-feed blocking | **Yes** — opt-in toggle that walls only the home feed via the selected bottom-nav tab; search, subscriptions and playback stay usable | Yes | Similar intent; FrostLock is tab-based and local | Tie |
| 15 | Notification blocking + inbox | **Yes** — holds alerts from blocked apps, local inbox with reasons and a per-app ranking | Yes | Similar; FrostLock is free and fully local | Tie / FrostLock |
| 16 | Home-screen blocking | **Yes** — opt-in Profile toggle that blocks launchers while a focus session or schedule is active | Yes (login required) | Similar; FrostLock needs no account | Tie / FrostLock |
| 17 | Split-screen / floating-window blocking | **Yes** — scans accessibility windows for a blocked app that stays visible beside an allowed app | Yes | Similar; FrostLock is local and free | Tie |
| 18 | Uninstall protection | **No** | Yes (device admin) | Gap; also anti-user | Regain (effectiveness) |
| 19 | Schedule start alarms / reminders | **Yes** — local notifications at start and end, with exact-alarm fallback | Yes | Similar | Tie |
| 20 | Schedule calendar view | **Yes** — month heat-map with per-day drill-down | Yes | Similar; FrostLock keeps it free and local | Tie |
| 21 | Upcoming sessions / overlap detection | **Yes** — next 7 days of rule windows with countdowns, plus editor/list overlap warnings | Yes | Similar planning view; FrostLock keeps it local | Tie |
| 22 | Exam-aware onboarding | **Partial** — local deadline countdown plus named focus goals with target dates/hours; no exam-tuned scheduling | Exam, date, region | FrostLock covers local planning; Regain adds exam-specific scheduling | Tie / FrostLock (offline) |
| 23 | Focus groups / friends | **No** | Yes | Gap | Regain |
| 24 | Weekly leaderboard | **No** | Yes | Gap | Regain |
| 25 | Wake-up nudges / accountability | Focus Guard only | Group nudges + guilt trips | Different | Regain (social) |
| 26 | Streaks | Daily streak + best + schedule streak | Daily/session/schedule streak | FrostLock now has both daily and schedule streaks | Tie |
| 27 | Achievements | 16 offline badges, tap to share unlocked ones | Badges + share + rewards | FrostLock now shares too, but keeps it text-only and offline | Tie / FrostLock (offline) |
| 28 | Rewards / levels / stamps | **Yes** — five derived session stamps plus a seven-title freeze ladder; no currency, difficulty tiers or store | Yes | FrostLock covers stamps and levelling offline; Regain adds currency/store | Tie / FrostLock (offline) |
| 29 | Focus music / themes | **Yes** — four on-device ambience beds (deep drone, rain, wind, snow) plus 4 free ice palettes | Yes (premium) | FrostLock covers ambience and themes without assets, streaming or a paywall | FrostLock |
| 30 | Widgets | **4** (focus, daily goal, week at a glance, calendar) | 8 types incl. usage, unlock count, calendar | Smaller gap; FrostLock now covers the calendar and avoids usage/unlock permissions | Regain (breadth) / FrostLock (privacy) |
| 31 | Phone-unlock tracking | **Yes** — local daily unlock count via `ACTION_USER_PRESENT`, shown on Stats and in the focus widget | Yes (widget + stats) | Similar; FrostLock needs no usage-access permission | Tie / FrostLock (privacy) |
| 32 | Weekly report | **Yes** — week navigation, comparison, daily bars, best day, subjects | Yes (Pro for past days) | Similar; FrostLock keeps it free | Tie / FrostLock |
| 33 | Session notes | Yes — editable from the diary and the post-session summary, with an optional local photo | Yes + study stamps/photos | FrostLock now matches the photo side without camera permission | Tie |
| 34 | Study diary | **Yes** — day grouping, search, reflective notes, derived stamps, photo banners | Yes (notes + stamps/photos) | Similar; FrostLock keeps photos local | Tie |
| 35 | Session photos | **Yes** — attach from the post-session summary or the diary; copied into app-private storage with the system picker | Yes (study photos) | FrostLock matches it without camera or storage permission | Tie / FrostLock (privacy) |
| 36 | Focus bubble / floating timer | **Yes** — opt-in draggable overlay during focus | Yes | Similar; FrostLock asks for the overlay permission only when enabled | Tie |
| 37 | Accounts / cloud sync | **No** (local only) | Yes + backup/restore | Opposite design choices | FrostLock (privacy) |
| 38 | Ads / analytics / tracking | **No** | Yes (SDKs) | Opposite | FrostLock |
| 39 | Subscription / paywall | **No** — everything free | Pro tier + free caps | Opposite | FrostLock |
| 40 | Works offline | **Yes, fully** | Mostly, but login/groups/reports need network | Opposite | FrostLock |
| 41 | Onboarding effort | 10 quick slides | Long conversational + permissions + login | FrostLock faster | FrostLock |
| 42 | APK size | ~21 MB debug | ~58 MB base + splits | FrostLock smaller | FrostLock |
| 43 | Localisation | English only | Many languages | Gap | Regain |
| 44 | Permission footprint | A short list; overlay only if the floating bubble is enabled | 15+ incl. usage, notifications, admin | FrostLock safer | FrostLock |
| 45 | Usage tracking method | Accessibility foreground time | UsageStats permission | Both valid; FrostLock avoids a permission | FrostLock (privacy) |
| 46 | Adult-content blocking | **Yes** — curated adult domains sit in the same local website list and can be toggled per domain | Yes | Similar local coverage; no cloud categories | Tie / FrostLock (offline) |
| 47 | Study websites/channels allowlist | **Partial — channels only** — study-channel allowlist shipped with channel filtering; study *sites* still follow the block/allow domain model (an allowlist mode for websites is not built) | Yes | Similar for channels; Regain also flips websites to allowlist | Regain |
| 48 | Session summary screen | **Yes** — planned vs actual, subject, note, streak impact | Full summary | Similar | Tie |
| 49 | Pause limits | **Yes** — Off/1/2/3/no-limit per session; Strict Mode blocks pausing entirely | Max pauses per session | Similar, and FrostLock keeps it local | Tie |
| 50 | Multi-language UI | **No** | Yes | Gap | Regain |
| 51 | Data export / backup beyond OS | **No** | Cloud backup | Different | FrostLock (privacy) |
| 52 | Monthly stats | **Yes** — six-month rollup with active days, best day and average per active day | Yes (Pro) | Similar; FrostLock keeps it free | Tie / FrostLock |
| 53 | Play Store search blocking | **Yes** — opt-in toggle that matches the store's search box against the blocked apps' display names while focus/schedule is active | Yes | Similar; FrostLock works offline with no account | Tie |

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
| Notification shield | Local listener + held-notification inbox | `1279084` |
| Notification ranking | A “Most held apps” card ranks the top three senders with counts and share bars, computed from the local inbox | `1e6ed58` |
| Multiple widgets | Focus, daily goal and week-at-a-glance widgets, pinnable from Profile; focus/goal cards fill their full height so text never floats in empty bands | `18ef9fc`, `7baee2b` |
| Focus calendar + upcoming sessions | Month heat-map with day drill-down, plus a next-7-days “Coming up” panel on the rules screen | `5b33fbc` |
| Calendar home-screen widget | A locally drawn month heat-map widget with today ring and totals | `81d330d` |
| Morning/afternoon/evening/night presets | One-tap Morning, Workday, Evening and Night windows in the rule editor | `5b33fbc` |
| Rule overlap warnings | Amber heads-up in the editor and a badge on every colliding rule, including overnight and one-off cases | `091ba55` |
| Focus deadline | Local name + date countdown on Home and Profile, with a dedicated editor and themed date dialog | `31e69c3` |
| Focus goals | Named local goals with a target date and hours, plus an on-track/behind/complete progress list derived from saved sessions | `8be538d` |
| Website blocking | Opt-in local domain list (curated social/video + adult, plus custom sites) matched against the browser address bar; the wall names the site and only runs during focus or a rule | `2b0df05` |
| YouTube home-page blocking | Opt-in toggle that walls only the home feed via the selected bottom-nav tab; search and subscriptions stay usable | `d6cb3b6` |
| Play Store search blocking | Opt-in toggle that matches the store's editable search box against the blocked apps' display names and holds the matching search during focus or a rule | `0c20a9c` |
| YouTube channel filtering + study allowlist | Opt-in Channels screen with block-list and study-list modes; the video's owner channel is read from the watch page's Subscribe/avatar accessibility labels | `bbf44b4` |
| Focus stamps | Per-session badges derived from start hour, weekday and length, shown in the post-session summary and diary | `1c27ae4` |
| Freeze level | Lifetime focus minutes become widening levels with ice titles, shown on the stats screen with progress to the next | `249975b` |
| Freeze ladder | A dedicated page lists all seven titles and thresholds, marks reached rungs, and shares the current title as text | `2a997a7` |
| Schedule streak | Consecutive scheduled days with a session inside a rule window, shown on the rules screen | `95de98b` |
| Floating focus bubble | Opt-in draggable timer overlay during focus, drawn by the foreground service | `b00e586` |
| Session photos | One optional local photo per session, picked with the system picker, shown as a diary banner with a full-screen viewer | `8915202` |
| Pause limits + discard | A per-session pause allowance on Profile, plus a save/discard choice when stopping a partial session | `b45488f` |
| Monthly stats | Six-month rhythm bars plus active days, best day and average per active day on the stats screen | `6153caa` |
| Focus ambience | Four locally synthesised sound beds (deep drone, rain, wind, snow) streamed by the focus service; no audio assets or network | `d0c5c8c` |
| Ice theme variants | Four local palettes that restyle surfaces, accents and hero gradients instantly; choice persists in SharedPreferences | `d0a5075` |
| Achievement sharing | Tap an unlocked badge for a themed detail card and share it through the system sheet | `8b0d3d5` |
| Research discipline | Earlier Regain research notes (since removed in rename) | `ab74654`, `fb74d3e` |

Superseded: `20b18ab` added hand-drawn logo vectors; user rejected them, and `2e2c92e` replaced them with real icons.

---

## 6. Everything Regain has that FrostLock does NOT

### A. Blocking coverage

- [x] Website blocking (domains, social sites, adult content) — opt-in local list, detected through the browser address bar; only active during focus or a schedule.
- [x] Study-website allowlist — local allowed domains override the block list while focus is active.
- [x] Adult-content blocking — curated adult domains in the local list, individually toggleable.
- [x] YouTube Shorts blocking — accessibility scan for the Shorts surface while focus/schedule is active; apps with a known nav tab are flagged only when that tab is selected or the player surface is on screen.
- [x] Instagram/Facebook Reels + Snapchat Spotlight blocking — accessibility scan for Reels/Spotlight surfaces while focus/schedule is active.
- [x] YouTube home-feed blocking — opt-in toggle; the selected bottom-nav tab is the signal, so search and subscriptions stay usable.
- [x] YouTube channel blocking — opt-in toggle; the watch page's owner row (Subscribe button + channel avatar content-descs) is parsed while focus/schedule is active; recommendation chips are deliberately not parsed. Skipped when the whole YouTube app is blocked.
- [x] Study-channel allowlist — "Study list" mode on the Channels screen; only listed channels play during focus, and an empty study list never walls every channel.
- [x] Notification blocking + blocked-notification inbox
- [x] Per-app notification ranking — the inbox ranks the top senders with counts and share bars.
- [~] Pause/schedule notification blocking (the shield follows focus sessions and rules automatically)
- [x] Phone home-screen blocking during focus — opt-in toggle in Profile, blocks launchers when focus session or schedule is active. A schedule-only wall uses the rule window and label, so it holds instead of bouncing.
- [x] Split-screen blocking — blocked apps stay walled when pinned to half the screen.
- [x] Floating-window blocking — freeform/popup windows are scanned like any other visible window.
- [ ] Uninstall protection / device admin
- [x] Play Store search blocking — opt-in toggle; the store's editable search-box text is matched against the blocked apps' display names while focus/schedule is active. Ordinary store browsing is untouched.

### B. Routine, reminders & planning

- [x] Alarm/notification when a scheduled session starts (and ends)
- [x] Calendar view of sessions
- [x] Upcoming-session list
- [x] Session-overlap detection
- [~] Exam date/month awareness (a local deadline countdown and focus goals ship; exam-tuned scheduling is still missing)
- [x] Morning/afternoon/evening/night session presets (one-tap windows in the rule editor)
- [x] Schedule streak (separate from daily streak)
- [x] Session skip/discard + max-pause rules
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

- [x] Focus music / ambience (synthesised on-device; no bundled audio or streaming)
- [x] Ice theme variants (Frost, Aurora, Midnight, Crystal) with instant switching
- [~] Rewards, levels, difficulty levels (a freeze level ships; no currency, store or difficulty tiers)
- [x] Focus stamps (time, weekend and length badges)
- [x] Session photos (system photo picker, app-private storage, no camera or storage permission)
- [x] Study diary (day grouping, search, editable reflections)
- [x] Phone-unlock tracking — local daily count via ACTION_USER_PRESENT, shown on Stats; no usage-access permission.
- [x] Focus bubble / floating timer (opt-in overlay, hidden during breaks)
- [~] Eight widget types: **4 shipped** (focus today, daily goal, week at a glance, focus calendar); the focus widget now shows today's unlock count. Dedicated usage, unlock-count and Pro-pass widgets are still missing.
- [x] Achievement sharing (unlocked badges share plain text via the system sheet)

### E. Data, accounts & platform

- [ ] Cloud backup / restore
- [ ] Remote logout
- [ ] Weekly email/in-app report
- [x] Monthly stats and past-day browsing — a six-month rollup on Stats plus the Focus calendar for any past day.
- [ ] Multi-language UI
- [ ] UsageStats-based tracking
- [ ] Pro subscription / free-tier caps
- [ ] Ads / analytics / experimentation

---

## 7. What FrostLock has that Regain does not (or does better)

- **Fully offline.** No INTERNET permission; blocking, stats, badges and onboarding all work in airplane mode.
- **No account.** No sign-in wall; the app is usable seconds after install.
- **No ads, analytics, billing, or Pro tier.** “Free forever” is literal.
- **A short permission list vs 15+.** No usage-access, notification-listener, device-admin or camera permissions; overlay access is optional.
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
FrostLock's blocking coverage is now close on the common surfaces (apps, websites, Shorts/Reels, the YouTube home feed, YouTube channel filtering, Play Store searches for blocked apps, home screen, split screen, notifications); the remaining depth gaps are **study-site allowlist, uninstall protection and social/accountability**.

---

## 10. Roadmap — what to build next (offline-first only)

### P0 — close the most painful gaps without breaking privacy

- [x] **Schedule start/end alarms + notifications** (`AlarmManager` + boot receiver). Rules currently act silently.
- [x] **Study diary screen** to surface saved session notes (already in DB, no UI).
- [x] **Notification blocking** via `NotificationListenerService` + a local “blocked notifications” inbox. Scoped automatically to apps blocked by focus, a rule or a reached limit (no manual per-app allowlist yet).
- [~] **More widgets**: four shipped (focus today, daily goal, week at a glance, focus calendar) and the focus widget now carries today's unlock count; dedicated usage, unlock-count and Pro pass widgets remain.
- [x] **Weekly report screen** (local aggregation of existing sessions).
- [x] **Session summary** (planned vs actual, subject, note, streak impact).
- [x] **Focus calendar + upcoming sessions** — month heat-map with day drill-down and a next-7-days “Coming up” panel.
- [x] **Time-window presets** — Morning, Workday, Evening and Night one-tap windows in the rule editor.
- [x] **Rule overlap warnings** — the editor flags colliding windows and the list badges them.
- [x] Exam/goal profiles stored locally — a dedicated goals page with target date, target hours and progress derived from saved sessions; no exam-data upload.
- [~] Exam-tuned scheduling (e.g. “only during exam month”) is still missing.

### P1 — deeper blocking (needs design decisions)

- [x] **Website blocking** — opt-in local domain list (curated social/video + adult, plus custom sites) detected through the browser address bar by the existing accessibility service. The wall names the domain and lifts when the session or rule ends. No VPN, no INTERNET permission, no browsing history; browser-dependent by nature.
- [x] **Shorts/Reels detection.** Accessibility heuristics per app — nav-tab selection and player view ids where the app has a known tab, text otherwise — active during focus/schedule; brittle by nature but unit-tested.
- [x] **YouTube home-feed blocking** — opt-in toggle; the selected bottom-nav tab decides, so search and subscriptions stay usable.
- [x] **YouTube channel filtering + study allowlist** — opt-in toggle with two modes; the channel is read from the watch page's owner row only (Subscribe button and channel-avatar content-descs). Handles are normalized away, an empty study list never walls everything, and the scan is skipped when the whole app is blocked.
- [x] **Play Store search blocking** — opt-in toggle; the store's editable search box is matched against the blocked apps' PackageManager display labels with whole-word tokens, so "xbox" never lights up a short label like "X". The search must spell the app's visible name (e.g. "youtube music"); the wall follows website-wall lift semantics. Live E2E on a signed-in retail device (Realme, Android 16): searching "youtube" and "telegram" for blocked apps walled the store ("Search held" with the app's name and the rule countdown); the same query without the app blocked, and result-page browsing, did not. The same session also confirmed the app-blocker wall covers a freeform YouTube floating window on that device ("Scheduled focus / YouTube is blocked during “Focus”").
- [x] **Home-screen blocking** and **split-screen/floating-window blocking** on top of the existing accessibility service.
- [x] **Focus ambience** — Deep focus, Rain, Wind and Snow generated on-device at 44.1 kHz, so there are no audio assets or downloads.
- [x] **Theme variants** (more ice palettes) — Frost, Aurora, Midnight and Crystal, selectable on Profile.

### P2 — motivation & polish

- [x] Focus stamps — derived locally from start time, weekday and length, shown in the summary and diary.
- [x] Freeze level — local levelling from lifetime focus minutes, no currency or store.
- [x] Achievement sharing — unlocked badges share plain text via the system sheet.
- [x] Schedule streak — consecutive scheduled days with a session inside a rule window.
- [x] Session photos — attach from the summary or diary; copied into app-private storage via the system picker.
- [x] Reward/level layer on top of the existing badges — a seven-title freeze ladder with a dedicated page; no currency, no store.
- [x] Focus bubble / floating timer — opt-in draggable overlay, tap to return.
- [x] Exam/goal profiles stored locally (no exam-data upload) — named goals with a target date and target hours, tracked against saved sessions.
- [~] Exam-tuned schedules are still missing.
- [x] Schedule streak.
- [x] **Session skip/discard + max pauses** — stop asks save or discard, breaks can be skipped, and Profile caps normal-session pauses.
- [x] Monthly stats — six-month bar chart with active days, best day and average per active day, all computed from local sessions.
- [x] Per-app notification ranking — the inbox ranks the top held apps with counts and share bars.
- [ ] Multi-language resources.

### Never

- Accounts, cloud sync, groups, leaderboards, Pro/subscriptions, ads, analytics, AI coach, uninstall protection.

---

## 11. Verified facts & commands

- Build: `JAVA_HOME=/snap/android-studio/250/jbr ANDROID_HOME=/home/vishal/Android/Sdk ./gradlew :app:assembleDebug`
- Tests: `:app:testDebugUnitTest` → **250 tests, 0 failures**
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Accessibility component: `com.frostlock.app/com.frostlock.app.blocking.FocusBlockAccessibilityService`
- Re-enable service:
  ```bash
  adb shell settings put secure enabled_accessibility_services com.frostlock.app/com.frostlock.app.blocking.FocusBlockAccessibilityService
  adb shell settings put secure accessibility_enabled 1
  ```
- Regain APK inspected at `/tmp/opencode/regain.apk` (v75.1.2113, 58 MB base); it was not retained after the analysis.
- FrostLock permissions: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED`, `USE_EXACT_ALARM`, and an optional `SYSTEM_ALERT_WINDOW` for the floating bubble. Photos go through the system picker, so no camera or storage permission is declared. Still **no INTERNET**.
- FrostLock manifest: accessibility service, focus foreground service, schedule reminder receiver, notification listener service, four widget providers.
- Test files: 38 (`OnboardingViewModelTest`, `OnboardingShowcaseTest`, `SuggestedLimitsTest`, `ScheduleRuleTest`, `ScheduleEditorTest`, `ScheduleTriggersTest`, `ScheduleOverlapTest`, `ScheduleStreakTest`, `DeadlineCountdownTest`, `SessionStampsTest`, `FocusLevelTest`, `FocusGoalsTest`, `WebsiteBlockingTest`, `FocusBubbleTest`, `FocusPausesTest`, `SessionPhotosTest`, `LimitWarningTest`, `AchievementsTest`, `FocusStatsTest`, `DiaryTest`, `WeeklyReportTest`, `SessionSummaryTest`, `SubjectAppSetTest`, `FocusCalendarTest`, `UpcomingSessionsTest`, `NotificationPolicyTest`, `NotificationInboxTest`, `WidgetDataTest`, `IcePaletteTest`, `MonthlyStatsTest`, `AmbienceTest`, `HomeBlockingTest`, `UnlockStoreTest`, `MultiWindowDetectorTest`, `ShortFormDetectorTest`, `YouTubeSurfaceDetectorTest`, `PlayStoreDetectorTest`, `YouTubeChannelDetectorTest`).
- Current onboarding page count: 10 (`OnboardingPages.COUNT`).

---

## 12. Decision log / open questions

1. **Website blocking:** shipped as opt-in accessibility address-bar detection with a local domain list — no VPN, no INTERNET permission. It is browser-dependent and documented as such.
2. **Notification blocking:** shipped as an opt-in, local-only shield with an inbox and a local per-app ranking; no per-app training, nothing uploaded.
3. **YouTube home-feed blocking:** the selected bottom-navigation tab (exposed with a reliable `selected` state) is the only signal, so YouTube stays usable for search and subscriptions. When the whole app is blocked the feature stands down. Short-form apps with a known nav tab use the same tab signal plus player view ids; Snapchat falls back to text.
4. **Schedule alarms:** adding `AlarmManager` + boot receiver is low risk and high value.
5. **Diary:** the note field already exists; surfacing it is a small UI task.
6. **Keep the strict no-network promise** even for widgets/reports — everything must remain local.
7. **Do not drift into Regain's scope**: social, accounts, paywalls and ads stay out by design.
8. **Play Store search blocking:** the store exposes the typed query through its editable search box; the match runs against the blocked apps' display labels with whole-word tokens, so short labels cannot false-positive inside other words. Ordinary store browsing is untouched, and the wall lifts like a website wall (rule end or session heartbeat).
9. **Scheduled launcher walls:** with only a schedule active the home-screen wall uses `REASON_SCHEDULE` with the rule window and label instead of the heartbeat-based focus wall, which would dismiss itself and re-fire in a loop.
10. **YouTube channel signal:** this YouTube build exposes the video's owner channel only through accessibility descriptions of the owner row — the Subscribe button ("Subscribe to X.") and the channel avatar ("X, Official Artist Channel 4.56M subscribers"). Recommendation chips ("Go to channel X") describe other channels and are deliberately not parsed, so an unrelated recommendation can never wall an allowed video; the "@handle" visible on screen is not in the a11y tree, so matching relies on the display names. A second fix: the service constructs its own store instances whose StateFlows snapshot prefs at construction, and SharedPreferences does not notify other instances in-process — store configuration is therefore read live (`WebsiteBlockingStore.isEnabled()/blockedDomainList()/allowedDomainList()`, `YouTubeChannelStore.isEnabled()/allowOnlyMode()/blockedChannels()/allowedChannels()`) instead of through the flows.

---

_Update this file whenever a feature is added or a gap is deliberately closed. It is the memory of what FrostLock is and is not._
