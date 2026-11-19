# FrostLock Redesign — Edge-Case Audit & QA Plan

**Date:** 2026-11-17
**Scope:** `docs/frostlock-redesign-plan.md` (Focus / Shield / Insights / You)
**Method:** static audit of the current implementation (`app/src/main/java`) plus the redesign plan. The four-tab IA is **not implemented yet**; findings describe current behaviour that the redesign must either preserve, surface, or fix.
**Constraint reminder:** offline only, no new permissions, keep the existing permission footprint.

**Related docs:** `docs/frostlock-redesign-plan.md` (IA), `docs/frostlock-ux-writing.md` (final wording). This document specifies **states and behaviour**, not final copy. Where it names a state, the string comes from the UX-writing doc. Terminology follows that doc: **block** = what the user configures, **held** = what FrostLock does in the moment.

> **Status:** companion spec to plan v3. Plan v3 is authoritative for IA, routes and naming; this document is authoritative for edge cases, state tables and release gates. Canonical renames to apply when reading: Diary → **Reflections**, Focus calendar → **History**, Weekly report → **Weekly review**, Achievements → **Badges**, Freeze levels → **Levels**, Stats → **Trends**, App Blocking → **Apps**, Deadline → **Countdown**, Daily goal → **Daily target**, Focus Guard → **Reminders**, Focus bubble → **Floating timer**. The bottom bar stays visible on standard details.

---

## 0. Severity legend

| Level | Meaning |
| --- | --- |
| **P0** | User can believe they are protected while nothing is blocking, or loses data. Must fix before redesign ships. |
| **P1** | Confusing or broken edge state that a normal user can hit within a week. Fix in the redesign. |
| **P2** | Polish / resilience / accessibility issue. Fix before release if cheap, otherwise track. |

---

## 1. Executive summary

The redesign is mostly an information-architecture change, but it concentrates protection status onto one screen (**Shield**). That raises the cost of the current “silent degraded mode” behaviour: today a toggle can be ON while the underlying service or permission is missing. The audit found **four P0/P1 themes**:

1. **Permission truth is not modelled.** Accessibility, notification access, overlay, exact alarms, and `POST_NOTIFICATIONS` are checked ad hoc or not at all. Shield and Focus can claim protection that is not running. (`MainActivity.kt:41,58-66`, `FocusBlockAccessibilityService.isEnabled`, `BlockViewModel.kt:156-175`, `FocusSessionService.kt:196-199`.)
2. **Time boundaries are under-tested.** Daily limits, widgets, inbox “today”, and streaks all depend on `LocalDate.now()` but do not refresh at midnight or on `TIME_SET` / `TIMEZONE_CHANGED`. Widgets are only refreshed on app open, session save, or profile change. (`AppLimitStore.kt:126-164`, `WidgetSupport.kt:41-52`.)
3. **“Configured” and “effective” differ.** Empty study lists deliberately wall nothing; empty per-rule app sets fall back to the global focus list; channel filtering depends on parsing the owner row. Shield must show the *effective* set, not the stored set. (`ScheduleStore.kt:71-72`, `FocusBlockAccessibilityService.kt:190-199,657-680`, `NotificationPolicy.kt:47-77`.)
4. **The new surfaces are not responsive.** Fixed 280dp/52sp timer disc, fixed 38dp nav icon, no window-size classes or hinge handling, no reduced-motion handling. (`FocusScreen.kt:1064-1103`, `AppBottomBar.kt`.)

Current test coverage is **39 unit-test classes, 0 instrumentation tests**. There is no Compose UI test suite, no screenshot/golden test, and no permission-state test harness. That is the single largest QA gap for a redesign of this size.

---

## 2. Findings register

| ID | Sev | Area | Evidence | Impact | Required change |
| --- | --- | --- | --- | --- | --- |
| F1 | **P0** | Notification permission | `MainActivity.kt:41,58-66` requests `POST_NOTIFICATIONS` on cold start before onboarding, and the result callback is empty | User can deny at the welcome screen with no context. Afterwards the foreground-service notification, schedule reminders, 80% limit warning, and break-over alert are all invisible. App never explains why. | Defer the request to a contextual moment (after first session or when a reminder/limit is configured). Add a persistent capability card in **You → Permissions** and a Shield banner when notifications are off. |
| F2 | **P0** | Accessibility off vs toggle on | `BlockViewModel.kt:156-175` checks the service only when Block screen loads; `FocusViewModel.startBlockingService()` starts a session regardless; `BlockingPreferences.blockingEnabled` is independent | Focus/Shield can show “Blocking on” while no app is actually blocked. This is the worst possible failure for this product. | Model `AccessibilityState` (granted / denied / service crashed). ShieldHome banner + Focus pre-start interstitial. Disable “start with blocking” or start in “timer only” mode with explicit copy. |
| F3 | **P1** | Midnight / timezone / widgets | `WidgetSupport.kt:41-52` refreshes only on app open, session save, profile change; no `DATE_CHANGED` / `TIME_SET` receiver in `widget/`; `AppLimitStore.kt:143-164` keys usage by `LocalDate.now()` at read/commit | Daily widgets show yesterday’s “today” after midnight; streak/goal ring stale; timezone travel shifts day buckets. | Add a `DateChangeReceiver` (`ACTION_DATE_CHANGED`, `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`) that refreshes widgets, re-arms reminders, and invalidates limit caches. Refresh on `ON_RESUME` of every tab. |
| F4 | **P1** | Daily limit across midnight | `AppLimitStore.commitActive()` computes `usageKey(pkg)` at commit time, and credits up to `MAX_SINGLE_CREDIT_MS` (60s) | A limited app left foreground across midnight has its last minute credited to the new day; the pre-midnight segment is lost. Counter can look wrong right after midnight. | Split the credit at local midnight, or reset `activeSince` on date change. Add a unit test with a fixed clock. |
| F5 | **P1** | Inbox duplicates / cap | `FrostNotificationListenerService.kt:40-88` archives every `onNotificationPosted` without dedupe by `sbn.key`; `NotificationArchiveStore.kt:50-52` truncates to 200 silently | Reposts/updates create duplicate rows; oldest entries vanish with no “older items removed” notice; group summary passes while children are held, leaving an orphan summary. | Dedupe by `key` + `postTime` (update in place), or at least collapse identical title/body within a short window. Show “Showing latest 200” when capped. Decide group-summary behaviour explicitly. |
| F6 | **P1** | “Held today” stale | `BlockViewModel.kt:78-97` computes `heldToday` once when the combine emits; inbox day grouping is computed at render time | If the app stays open across midnight, “Today” counts and grouping are wrong until re-subscribe. | Recompute on `ON_RESUME` and on a date-change signal. |
| F7 | **P1** | Overlap reason label | `FocusBlockAccessibilityService.kt:190-199` uses `firstOrNull` for the wall label, while `activeBlockedPackages()` unions every active rule | Two overlapping rules block correctly, but the wall names only the first rule. ShieldHome must not repeat this. | Wall/Shield list **all** active rule labels that cover the package; show “2 rules active”. |
| F8 | **P1** | Empty study list semantics | Documented: empty channel study list “never walls every channel”; empty website study list “walls nothing” | Users may enable study mode, add nothing, and believe everything is blocked. | Shield must show “Study list is empty — nothing is being held” with an Add action. Never show a green “Websites protected” state for an empty allowlist. |
| F9 | **P1** | Large text / display size | `FocusScreen.kt:1064-1103` fixed 280dp disc and 52sp timer text; `AppBottomBar.kt` fixed 38dp icon + 11sp label | At 200% font scale the timer clips, nav labels truncate, stat tiles overflow. | Use `BoxWithConstraints` + `maxLines`/`autoSize` or a smaller timer style above a font-scale threshold. Test at 1.0/1.3/1.6/2.0. |
| F10 | **P1** | Tablet / foldable | No `WindowSizeClass`, no `WindowLayoutInfo`, no `NavigationRail`; single-column layouts and a full-width pill bar | Stretched cards, huge empty bands, bottom bar too wide; content can sit under a fold hinge. | Add `currentWindowAdaptiveInfo()`: compact → bottom nav, medium/expanded → nav rail or left rail; hinge-aware padding; max content width (~600–720dp). |
| F11 | **P1** | Discard wipes intention | `FocusViewModel.kt:404-424,435-459,462-478` call `clearIntent()` on discard/stop/reset | Discarding a session erases “one thing for this session”, so the next attempt starts blank. Surprising. | Keep the intent across discard/stop; clear only on explicit “clear” or successful completion (product decision). |
| F12 | **P2** | Delete session leaves stale streak | `FocusViewModel.kt:315-333` deletes the row, resets only the displayed `lastSessionStreak`, refreshes widgets | Insights/streak may show a value that no longer matches the DB until reload. | Recompute streak after delete and emit it to Insights. |
| F13 | **P2** | Strict + stopwatch | `FocusViewModel.kt:361` forces `strictMode = false` when mode is `STOPWATCH` | The Strict toggle can look enabled while stopwatch ignores it. | Disable the toggle for Stopwatch with “Strict Mode applies to timed sessions”. |
| F14 | **P2** | Reboot during session | Timer state persists in prefs, but there is no boot receiver for focus sessions; `restoreSession()` only runs when the app opens | After reboot the session appears frozen/absent until launch; Strict Mode cannot survive force-stop. | Document and surface: on app open, show “Session ended while FrostLock was closed” summary or resume. Do not silently resume a stale wall-clock session. |
| F15 | **P2** | Onboarding write ordering | `OnboardingViewModel.kt:96-121` inserts profile + streak, then sets `onboarding_done` | A crash between writes can duplicate profile/streak on retry. | Set the flag last (already) but make inserts idempotent (single transaction / upsert). |
| F16 | **P2** | Widget palette drift | `CalendarWidgetRenderer.kt:103-107`, `WidgetSupport.kt:358` hard-code Frost colours | Aurora/Midnight/Crystal users get mismatched widgets. | Pass palette colours into `RemoteViews` or accept documented Frost-only widgets. |
| F17 | **P2** | RTL / locale | Manifest declares `supportsRtl="true"`; UI is English-only, many `Row`/padding assumptions | Pseudo-RTL and 12/24h locale can break layout and `formatClock` (fixed `Locale.US`). | Run pseudo-locale + RTL QA. Decide 12/24h behaviour. |
| F18 | **P2** | Reduced motion | No `MotionDurationScale` / animator-scale handling; spring and infinite onboarding animations | Users who disable animations still get motion; battery/perf cost. | Respect system animator duration scale; provide a “reduce motion” fallback for the frosted disc. |

---

## 3. Edge-case audit by scenario

Each row: **expected** behaviour, **current** behaviour/gap, and **QA check**. These feed the checklist in §4.

### 3.1 First run & onboarding

| Case | Expected | Current / gap | QA check |
| --- | --- | --- | --- |
| Fresh install, airplane mode | Full 10-slide onboarding works, no network calls, no blank logos | Brand logos bundled; likely OK | Enable airplane mode, clear data, complete onboarding. Verify no ANR and all 10 slides render. |
| No launchable apps installed | Onboarding “App control” slide still renders a useful mock | `onboardingShowcaseApps(installed)` is called; verify fallback is non-empty | Use a minimal emulator image with few apps; check slide 4 and Shield app picker. |
| Skip intro | Jumps to personalisation (goals), not straight to the app | Verify `Skip intro` target | Tap skip on slide 1; confirm goals slide and that back does not re-open skipped slides. |
| Goals / triggers / reason validation | Cannot proceed without ≥1 goal, ≥1 trigger, 10–300 char reason | Implemented in `canProceed()` | Try empty, 9 chars, 10 chars, 300 chars, 301 chars. |
| Force-quit at commitment | Restart returns to onboarding without duplicate profile/streak | Insert order is profile → streak → flag | Kill app during commitment (dev: stop process), relaunch, check DB counts. |
| Notification permission on first launch | Asked in context, not over the welcome slide | `MainActivity` asks immediately | Fresh install: observe dialog timing. Deny; verify app remains usable and a later path exists to grant. |
| Onboarding on tablet / landscape | Content centred, no clipping, CTA reachable | No adaptive layout evidence | Run on tablet + landscape; check 2.0 font scale. |
| Onboarding back / system back | Does not trap or silently exit mid-flow | `onExit` finishes activity | Press system back on each slide; verify predictable behaviour and no lost answers. |

### 3.2 Focus session lifecycle

| Case | Expected | Current / gap | QA check |
| --- | --- | --- | --- |
| No sessions yet | Focus is usable immediately; Insights shows baseline invite, not `0s / 0d` | Insights baseline logic exists on Stats; new InsightsHome must inherit it | Clear data → complete onboarding → open Insights. No zero-hero. |
| Idle timer | Last duration remembered; mode, subject, intent restored | Implemented in `FocusViewModel.init` | Change duration/mode, kill app, reopen. |
| Running | Timer survives tab switch, rotation, process death | Foreground service + `restoreSession()` | Start 5m, switch tabs, rotate, `adb shell am kill`, reopen. Timer and blocking continue. |
| Paused | Shows remaining pauses; resume keeps used count | `PauseAllowance` implemented | Pause twice with allowance 2; third pause disabled. Change allowance mid-session; verify old cap holds. |
| Pause allowance Off | Pause button disabled, no bypass | `maxPauses = 0` | Set Off, start, try pause. |
| Pause allowance No limit | Unlimited pauses, label “No pause limit” | `UNLIMITED = -1` | Set No limit; pause 10×. |
| Strict mode | No pause, no early exit; hold-to-end only | `strictActive`; hold button | Start strict, try pause/stop; hold-to-end works; confirm dialog copy. |
| Strict + Stopwatch | Toggle disabled with explanation | Forced off in code, UI may not explain | Select Stopwatch; check toggle state and copy. |
| Stop <5s | Silently discarded, no diary/stat entry | `stopInternal` saves only ≥5s | Stop at 4s; verify no session row. |
| Stop ≥5s | Save/discard dialog; discard leaves no trace | `EndSessionDialog` | Stop at 6s; choose Save and Discard in separate runs; verify DB, diary, widget, streak. |
| Discard | Clears timer, does not erase the intention (recommended) | `clearIntent()` wipes it | Set intent, start, discard; check intent field. |
| Pomodoro round end | Round saved, break starts, blocking lifts during break | `startBreak()` clears session | Complete 1 round with 1m break; open blocked app during break (should open); break-over alert. |
| Skip break | Returns to focus phase, round increments | `skipBreak()` | Skip; verify round N+1 and no duplicate session. |
| Break end while backgrounded | Alert + timer returns to focus phase | Service posts alert; UI `finishBreak` on tick | Lock screen through break; verify alert and state on return. |
| Stopwatch | Open-ended, no planned duration | `plannedSeconds = 0` for stopwatch | Run stopwatch 10s, save; summary “planned 0” copy. |
| Session summary | Planned vs actual, subject, note, photo, streak impact | Implemented | Verify each field, 280-char note limit, photo attach/remove, discard. |
| Process death / reboot | Session either resumes or is clearly closed, never silently stale | No boot receiver for sessions | Reboot mid-session; reopen app; verify copy and no phantom wall. |
| FGS start failure | Clear error, timer does not pretend to run | `ContextCompat.startForegroundService` unguarded | Test on Android 14/15 with battery restrictions; simulate exception. |
| Ambience | Persists per session, stops on pause/end/break | Service-owned | Toggle each bed; pause/end; verify audio stops. |
| Bubble | Only shows when overlay granted; hidden during break | `bubbleEnabled()` checks `canDrawOverlays` | Deny overlay, enable bubble, start; verify no bubble and no crash. Grant while running; verify next session. |

### 3.3 Shield / protection

| Case | Expected | Current / gap | QA check |
| --- | --- | --- | --- |
| All protection off | Calm empty state, one primary action, no scary red | Needs design | Fresh install → Shield. Verify hierarchy and no “protected” claim. |
| Accessibility denied | Banner: “FrostLock can’t block apps until Accessibility is on”; toggles reflect inert state | Toggle can be ON while service off | Revoke accessibility in settings, return to Shield/Focus. Verify banner and disabled start-with-blocking. |
| Accessibility service killed by OEM | Detect and prompt to re-enable | `isEnabled` only checked on load | `adb shell am force-stop` / disable service; reopen Shield. |
| No apps installed | App picker empty state with explanation | BlockScreen has an empty branch | Minimal device; open App Blocking. |
| No blocked apps | “No apps selected — focus sessions won’t block anything” | Needs copy | Clear selection; start focus; verify warning, not silence. |
| Daily limit reached | Wall at 100%, reason “Daily limit”, resets at midnight | Implemented | Set 1m limit, use app 1m; verify wall; change device date past midnight; verify reset. |
| 80% warning | One heads-up per app per day | `shouldWarn` + `warningSent` | Use 80% of a 10m limit; verify single notification; cross midnight, verify re-arm. |
| Limit across midnight | Time split correctly; new day starts at 0 | F4 | Keep app foreground across midnight; check counter. |
| Schedule active | “Active now” badge, correct end countdown, wall reason = rule name | Implemented | Create rule covering now; open blocked app. |
| Overlapping schedules | Both active; union of apps blocked; wall/Shield names all rules | Blocker unions; wall names first | Create two overlapping rules with different apps; verify both blocked and both labels shown in Shield. |
| Overnight window (22:00–02:00) | Active 22:00–23:59 and 00:00–01:59; weekday uses the start day for the tail | `isActiveAt` handles previous day | Set Monday overnight; test Mon 23:00 and Tue 01:00; test Tue 23:00 inactive. |
| Overnight + one-off date | Tail belongs to the one-off date | `appliesOn(now.minusDays(1))` | One-off Monday 22:00–02:00; test Tue 01:00 active, Wed 01:00 inactive. |
| Overnight + exam window | Tail respects `fromDate`/`untilDate` | Range checked in both paths | Exam window ending Monday; test Tue 01:00 inactive. |
| Zero-length window | Rejected or clearly invalid | `startMinutes == endMinutes` → inactive | Try equal start/end in editor. |
| DST spring-forward / fall-back | Window does not vanish or double-fire | `atZone` resolves gaps; untested | Set device TZ to a DST zone; test 02:00–03:00 window on transition day. |
| Rule with empty app set | Falls back to global focus list; UI says so | `blocks()` fallback; editor copy exists | Create rule with no apps; verify effective list in Shield. |
| Rule disabled | Not active, no reminder | `enabled` checked | Disable rule during its window; verify wall lifts. |
| Exact alarm denied | Inexact fallback, reminder may be late; UI discloses | `ScheduleReminderScheduler.kt:120-135` | Revoke exact-alarm permission (Android 12+); verify reminder still arrives and You shows “approximate”. |
| Boot / time change | Reminders re-armed | Boot receiver handles boot/update/time/timezone | Reboot, change timezone, verify reminders. |
| Notification shield off | No listener work, inbox still readable | Implemented | Toggle off; post from blocked app; verify notification passes and inbox unchanged. |
| Shield on, access denied | Banner + “Grant access”, no false “active” | `notificationsActive` exists; needs Shield surface | Enable shield, deny access; verify state. |
| Shield access revoked mid-session | Detect on resume, banner, held items stop | No live listener for revocation | Revoke in settings, return; verify. |
| Inbox empty | Empty state differs for off / denied / granted | `EmptyInbox` exists | Test all three. |
| Inbox full (200) | “Showing latest 200” notice; clear-all confirm | Cap silent; clear-all may be immediate | Generate 210 entries (test harness); verify cap copy and clear confirmation. |
| Inbox midnight | Today/Yesterday/date groups update | Static until recomposition (F6) | Keep inbox open across midnight; verify grouping. |
| Inbox duplicates | One row per real notification | No dedupe (F5) | Post/update same notification; verify rows. |
| Calls/alarms/ongoing | Always pass | `NotificationPolicy` | Call the device; verify notification passes. |
| Website block mode | Listed domains wall during focus/schedule only; allowed domains pass | Implemented | Test listed + allowed + unlisted domains. |
| Website study mode, empty list | Walls nothing; Shield says so | Deliberate; must be surfaced | Enable study mode, empty list; browse; verify no wall and Shield copy. |
| Website study mode, populated | Only allowed hosts open; wall names host | Implemented | Allow wikipedia.org; open it and youtube.com. |
| Unsupported browser | No false wall, no crash; Shield discloses coverage | Address-bar parsing is browser-dependent | Test Chrome, Firefox, Samsung Internet, DuckDuckGo. |
| Channel block list | Listed channels wall on watch page | Implemented | Open a listed channel; verify wall. |
| Channel study list, empty | Nothing walled | Deliberate | Enable study list, empty; verify. |
| Channel study list, populated | Only listed channels play | Implemented | Test listed + unlisted. |
| Channel parsing failure | No false positive; playback continues | No signal available | Use a YouTube layout/region where owner row differs; verify no crash. |
| YouTube home feed | Home tab walls; search/subscriptions/playback work | Implemented | Test each surface. |
| Shorts/Reels | Only short-form surface walls, not a home shelf | Implemented | Test YouTube/Instagram/Facebook/Snapchat. |
| Play Store search | Blocked app names held; ordinary browsing passes | Implemented | Search a blocked app, a non-blocked app, browse. |
| Home-screen blocking | Launcher walled during focus/schedule; schedule-only wall holds | Implemented | Enable, start session, press Home; schedule-only variant. |
| Split-screen / floating window | Blocked app walled while visible beside allowed app | Implemented | Split screen with blocked + allowed; freeform window. |
| Offline / airplane | Everything except nothing works; no network calls/ANR | No INTERNET permission | Airplane mode: start session, block, inbox, stats, widgets, schedules. |

### 3.4 Insights & data

| Case | Expected | Current / gap | QA check |
| --- | --- | --- | --- |
| Zero sessions | Baseline invitation, no `0s / 0d` hero, no empty charts | Stats has baseline; carry into InsightsHome | Fresh install → Insights. |
| First session | KPIs appear, streak 1, weekly bar for today | Verify | Complete a 1m session. |
| Streak broken | Current 0, best retained | `computeStreak` | Skip a day (device date), verify. |
| Partial week | Bars for elapsed days only, average over 7 | `summarizeWeek` | Mid-week check. |
| Week navigation | Cannot go into future; earliest week bounded | Verify WeeklyReport | Navigate back to first week; forward past current. |
| Weekly report baseline | First week = baseline; comparison unlocks after two non-overlapping weeks | Implemented | New user: baseline copy. Add 8+ days of sessions: comparison card. |
| No subjects | Subject breakdown hidden or “No subject tags yet” | Needs state | Sessions with blank tag. |
| Diary empty | Empty state with “Finish a session to write here” | Verify | Fresh install → Diary. |
| Diary search no results | Clear no-results state, query retained | Verify | Search nonsense. |
| Achievements none / some / all | Locked states readable; share only unlocked | Implemented | Toggle data. |
| Freeze level 1 / 7 | Progress to next; max level shows “max” | Implemented | Large lifetime minutes. |
| Calendar empty month | Grid renders, no misleading heat | Implemented | New user; future month. |
| Large numbers | 999h+ does not overflow tiles | `formatWidgetDuration` / stats formatting | Seed 1000h. |
| Timezone change | Day buckets and “today” shift consistently | Untested | Travel across TZ; compare sessions. |
| Delete session | Stats/streak/widget update immediately | F12 | Delete last session; check Insights + widget. |
| Widget parity | Widget numbers equal Insights for same moment | Widget cache may lag | Compare after session, after midnight, after delete. |

### 3.5 You / settings

| Case | Expected | Current / gap | QA check |
| --- | --- | --- | --- |
| Defaults | Daily goal, Strict off, breaks on, pause 2, Frost palette | `FocusSettings` | Fresh install → You. |
| Daily goal bounds | 15m–8h, invalid rejected | Verify | Try 0, 14, 15, 480, 481. |
| Break bounds | 1–30m | Verify | Try 0, 1, 30, 31. |
| Theme switch | Instant, persists, all four palettes | `ThemeStore` | Switch each; kill/reopen. |
| Strict toggle | Persists; does not alter a running session | Read at start | Toggle during session; verify current session unchanged. |
| Pause allowance | Off/1/2/3/No limit; running session keeps old cap | Implemented | Change mid-session. |
| Focus Guard | 10-minute nudge only for timed sessions | Implemented | 25m session; verify at 10/20m; stopwatch no nudge. |
| Overlay denied | Bubble toggle explains and deep-links; no crash | Profile card exists | Deny/grant. |
| Notification access denied | Card with grant link | Block/Inbox cards exist | Revoke/grant. |
| Notification permission denied | Card with rationale and re-request | Missing (F1) | Deny on Android 13+. |
| Exact alarm denied | Card says reminders may be approximate | Missing | Revoke. |
| Widget pinning unsupported | Button disabled with explanation | `WidgetPinning.isSupported` | Use a launcher without pin support. |
| Goals empty | Empty state + Add | Verify | Fresh install → Goals. |
| Goal complete / behind / on-track | Correct state and progress | Implemented | Seed sessions in/out of window. |
| Deadline none / today / past | Clear countdown or “passed” state | Verify | Set each. |
| Clear data / privacy | If present, confirms and resets all stores | Verify scope | Clear; check onboarding returns. |

---

## 4. QA checklist

### 4.1 Test suites to add

| Suite | Type | Why |
| --- | --- | --- |
| `PermissionStateTest` | Unit + fake `Context` | Proves every capability state maps to the right UI state. |
| `MidnightRolloverTest` | Unit with fixed `Clock` | Limits, warnings, held-today, streak, widgets. |
| `ScheduleBoundaryTest` | Unit (extend existing) | Overnight tail, one-off + overnight, exam window + overnight, DST. |
| `FocusLifecycleTest` | Unit/VM | Pause caps, strict, discard, <5s, stopwatch, restore. |
| `NotificationInboxTest` | Unit | Dedupe, cap, group summary, day grouping, ranking. |
| `ShieldEffectiveStateTest` | Unit | Empty study list, empty rule apps fallback, overlapping rules union. |
| `WidgetFreshnessTest` | Unit/Robolectric | Date change, timezone change, palette. |
| Compose UI tests | Instrumentation | Empty/loading/degraded states per tab; large font; RTL. |
| Screenshot/golden tests | Instrumentation | Four palettes × four tabs × phone/tablet. |

### 4.2 Smoke (every build)

- [ ] Fresh install → onboarding → Focus → 1-minute session → summary → Insights updates.
- [ ] Bottom bar has exactly **Focus, Shield, Insights, You**; no duplicate entry points.
- [ ] Every sub-screen has a working back and does not lose tab state.
- [ ] No crash with all permissions denied.
- [ ] Airplane mode: full flow works.
- [ ] App launches in under 2s on a cold start; no blank frame.

### 4.3 First run & permissions

- [ ] Onboarding completes on a device with 0–3 launchable apps.
- [ ] Skip intro goes to personalisation; answers survive rotation.
- [ ] Goals/triggers/reason validation boundaries.
- [ ] Notification permission is requested in context; denial is recoverable.
- [ ] Accessibility denied → Shield banner + Focus warning; no false “protected”.
- [ ] Notification access denied → Shield/Inbox banner; no false “active”.
- [ ] Overlay denied → bubble toggle explains; no bubble; no crash.
- [ ] Exact alarm denied → reminder still arrives; copy discloses approximation.
- [ ] Revoke each permission while the app is open; return via `ON_RESUME`; state updates.
- [ ] OEM battery saver / background restriction: session survives; copy explains if not.

### 4.4 Focus

- [ ] All three modes; duration 1m and 8h.
- [ ] Pause Off / 1 / 2 / 3 / No limit; cap holds across resume.
- [ ] Strict: no pause, no early exit, hold-to-end.
- [ ] Strict + Stopwatch: toggle disabled with copy.
- [ ] Stop at 4s (silent discard) and 6s (dialog).
- [ ] Save vs discard: DB, diary, stats, streak, widget all agree.
- [ ] Discard preserves intention (or product decision documented).
- [ ] Pomodoro: round save, break, skip, break-over alert, round increment.
- [ ] Process death, rotation, tab switch, reboot.
- [ ] Ambience and bubble toggles; bubble hidden during break.
- [ ] Timer legible at 1.0/1.3/1.6/2.0 font scale.
- [ ] Landscape and split-screen.

### 4.5 Shield

- [ ] Empty, partial, active, degraded, and error states for every card.
- [ ] Effective app set shown for focus, each rule, and union of overlaps.
- [ ] Overlapping rules: both names, union of apps.
- [ ] Overnight: before midnight, after midnight, wrong weekday, one-off, exam fence.
- [ ] DST transition.
- [ ] Daily limit: 80% warning once, 100% wall, midnight reset, cross-midnight credit.
- [ ] Schedule reminders: start, end, boot, timezone, exact-alarm denied.
- [ ] Notification shield: off / on+granted / on+denied / revoked.
- [ ] Inbox: empty × 3, populated, 200 cap, duplicates, midnight grouping, clear-all confirm, delete one.
- [ ] Websites: block mode, study mode empty, study mode populated, unsupported browser.
- [ ] Channels: off, block list, study empty, study populated, parsing failure.
- [ ] Home screen, split screen, floating window, Shorts/Reels, Play Store, YouTube home.
- [ ] Airplane mode.

### 4.6 Insights

- [ ] Zero sessions: baseline invite, no zero hero.
- [ ] First session; streak 1; weekly bar.
- [ ] Streak broken; best streak retained.
- [ ] Partial week; week navigation bounds.
- [ ] Weekly report baseline vs comparison.
- [ ] No subjects; diary empty; search no results.
- [ ] Achievements none/some/all; freeze level 1/7.
- [ ] Calendar empty/partial/future; day drill-down.
- [ ] Large numbers; timezone change.
- [ ] Delete session updates stats/streak/widget.
- [ ] Widget numbers equal Insights.

### 4.7 You

- [ ] Defaults and every setting boundary.
- [ ] Four palettes switch instantly and persist.
- [ ] Strict, pause allowance, breaks, Focus Guard, daily goal.
- [ ] Overlay, notification access, notification permission, exact alarm cards.
- [ ] Widget pin supported/unsupported.
- [ ] Goals empty/populated/complete/behind.
- [ ] Deadline none/today/past/future.
- [ ] Clear-data scope (if present).

### 4.8 Cross-cutting

- [ ] TalkBack: every control has a label; timer is not announced every tick; headings are navigable.
- [ ] Switch Access / keyboard: all actions reachable; focus order logical.
- [ ] Large text + display size + bold text.
- [ ] RTL pseudo-locale.
- [ ] Four palettes: contrast for body, secondary, chips, warning/error/success.
- [ ] Reduce motion / animator scale 0.
- [ ] Phone, small phone, tablet, foldable (folded/unfolded), landscape.
- [ ] Split-screen and freeform window.
- [ ] Dark mode system setting (app is dark-only; document).
- [ ] Cold start, warm start, process death.
- [ ] Low memory / Doze / battery saver.
- [ ] No network permission; verify no `INTERNET` in merged manifest.
- [ ] Widgets: first frame non-blank, midnight, timezone, palette, resize, pin.

---

## 5. State requirements per tab

Convention: every tab must explicitly implement **Loading**, **Empty**, **Content**, **Degraded**, and **Error** (where applicable). “Degraded” means the feature is configured but cannot act. Never render a success/protected state for a degraded feature.

Reusable capability model:

```kotlin
enum class Capability { GRANTED, DENIED, NOT_CONFIGURED, UNAVAILABLE }
```

`UNAVAILABLE` = OS/device does not support it (e.g., exact alarms pre-12 or pinning unsupported). `NOT_CONFIGURED` = permission fine but no apps/list/rules yet.

### 5.1 Focus tab

**Purpose:** one primary action — start/resume a session. No blocking toggles, no stats hub, no schedules.

| State | Entry condition | Required UI | Must not | Actions |
| --- | --- | --- | --- | --- |
| Loading | Prefs/subjects/apps reading | Skeleton disc + disabled CTA | Flash “0:00” as if ready | — |
| Idle (first run) | No sessions, defaults | Hero disc, Pomodoro selected, 25m, empty intent/subject, “Start focus” | Show streak/stats clutter | Start, change mode/duration, add intent/subject |
| Idle (configured) | Last duration/mode/subject restored | Same + restored chips | Reset user’s last choice | Start |
| Running (timed) | `isRunning && phase=FOCUS` | Disc, remaining, round, pause/stop per policy, strict lock indicator | Show pause when strict/off | Pause, stop (dialog ≥5s), hold-to-end if strict |
| Running (strict) | strictActive | Lock icon, no pause, hold-to-end only | Offer a one-tap exit | Hold-to-end |
| Running (stopwatch) | mode=STOPWATCH | Elapsed up, open-ended copy, strict disabled | Show a countdown | Pause/stop |
| Paused | `!isRunning && hasStarted` | Frozen time, “Resume”, pauses left | Restart from zero silently | Resume, stop/discard |
| Pause exhausted | allowance 0 | Pause disabled + “No pauses left” | Hide the reason | Resume |
| Break | phase=BREAK | Break countdown, “Skip break”, “Next round N” | Block apps during break | Skip |
| Break finished | `breakFinished` | “Round N next”, start CTA | Auto-start without consent | Start next round |
| Summary | `lastSession != null` | Planned vs actual, subject, note, photo, streak impact, Save/Discard | Save twice | Save note, attach/remove photo, discard |
| Discarded | discard action | Toast/snackbar “Session discarded”, timer reset | Leave a diary row | Start again |
| Restored | process death/reopen | Correct running/paused/completed state | Show stopped while service blocks | Resume/stop |
| Degraded: accessibility off | Blocking requested but service off | Inline warning before start: “Apps won’t be blocked” + Fix | Claim protection | Start anyway (timer only), Fix |
| Degraded: notifications off | Permission denied | Small notice: session alert won’t show | Block starting | Open settings |
| Degraded: overlay denied | Bubble enabled, no permission | Toggle off + explanation | Silent no-op | Grant |
| No apps selected | Blocking on, empty set | “No apps selected — this session won’t block anything” | Show shield icon | Add apps |
| No apps installed | Picker empty | Empty picker copy | Crash | — |
| Error: service start | FGS failure | Error card + retry | Pretend running | Retry |

**Data requirements:** mode, target duration, elapsed, phase, round, pauses used/max, strict, ambience, bubble, subject, intent, session packages snapshot, last session, streak, capability states.

### 5.2 Shield tab

**Purpose:** one mental space for every protection control; show *effective* status.

| State | Entry condition | Required UI | Must not | Actions |
| --- | --- | --- | --- | --- |
| Loading | Stores reading | Card skeletons | Flash “all off” then “active” | — |
| All off | No focus, rules, limits, shield, web | Calm empty state, one primary “Set up protection” | Red alarms | Add apps, schedule, limit |
| Partial | Some configured, none active | Per-card status, next upcoming | Collapse into one ambiguous toggle | Edit each |
| Active focus | Session running | “Focus blocking active”, subject, app count, end time | Show a toggle that can’t affect the session | Open session |
| Active schedule (single) | Rule active | Rule name, window, apps, ends-in | Name only the first of several | Edit rule |
| Active schedules (overlap) | 2+ active | All names, union app count, “2 rules active”, overlap badge | Hide the second rule | View each |
| Overnight tail | After midnight | Correct “ends at 02:00”, weekday label from start day | Show wrong weekday | — |
| Limit warning 80% | usage ≥80% | Per-app progress + “warning sent” | Re-warn repeatedly | Adjust |
| Limit reached | 100% | Wall status, reset time | Offer “add 20m” if not supported | Adjust/remove |
| Notification shield off | toggle off | Off + inbox still accessible | Hide inbox | Enable |
| Shield on, access granted | listener granted | “Active”, held-today count | — | Open inbox |
| Shield on, access denied | not granted | Amber banner + Grant | Show “Active” | Grant |
| Shield access revoked | was granted, now not | Banner updates on resume | Keep stale “Active” | Grant |
| Inbox empty (off) | no items, disabled | Explain enable | — | Enable |
| Inbox empty (denied) | no items, no access | Explain grant | — | Grant |
| Inbox empty (granted) | no items | “Nothing held yet” | — | — |
| Inbox populated | items | Day groups, app icon, reason chip, delete one, clear all | Mix days | Delete, clear (confirm) |
| Inbox capped | 200 items | “Showing latest 200” | Silent loss | Clear |
| No schedules | empty | “No rules yet” + presets | — | Add/preset |
| No limits | empty | Suggested 20m chips | — | Add |
| No websites | empty | Explain block vs study | — | Add |
| Website study empty | study mode, 0 domains | “Nothing is being held” warning | Green protected | Add |
| Website study populated | >0 | Count + mode | — | Edit |
| Channels off / block / study empty / study populated | per mode | Mode label + effective warning | Claim filtering when off/empty | Edit |
| Accessibility denied | service off | Top banner; every blocking card marked “inactive” | Any “protected” claim | Fix |
| Exact alarm denied | no permission | “Reminders may be approximate” | — | Fix |
| Offline | always | No offline banner needed (offline is normal) | Show network error | — |

**Data requirements:** capability states; focus active + packages; each active rule (id, label, window, endsAt, effective packages); limit per app (used/limit/warned); shield enabled/access; inbox count/heldToday/cap; website mode/count; channel mode/counts; home/split/shorts/play-store toggles.

### 5.3 Insights tab

**Purpose:** progress and reflection; no duplicate entry points.

| State | Entry condition | Required UI | Must not | Actions |
| --- | --- | --- | --- | --- |
| Loading | DB query | KPI skeletons | Flash zeros | — |
| Zero sessions | no rows | Baseline invitation card (“Your baseline starts today”), hidden charts | `0s / 0d` hero, empty grid | Start focus |
| First session | 1 row | Today/week/sessions/streak KPIs, one bar | — | Open diary |
| Streak 0 (broken) | gap | Current 0, best retained | Shame copy | — |
| Partial week | mid-week | Bars for elapsed days, weekly total | Average as total | Week drill-down |
| No subjects | all tags blank | Hide breakdown or “No subjects yet” | Empty card | Add subject on Focus |
| Weekly report baseline | <2 non-overlapping weeks | Baseline card | Fake comparison | View report |
| Weekly report comparison | ≥2 weeks | Delta + improvement card | — | Navigate weeks |
| Week nav bounds | earliest/future | Disabled arrows | Navigate to future | — |
| Diary empty | no notes/photos | Empty state + CTA | — | Start focus |
| Diary search empty | query no match | “No matches” + clear | Blank screen | Clear |
| Achievements none | 0 unlocked | Locked cabinet readable | Hide cabinet | View |
| Achievements all | 16 unlocked | Share available | — | Share |
| Freeze level 1/7 | lifetime minutes | Progress to next; max copy | Overflow | View ladder |
| Calendar empty month | no data | Grid + “No focus this month” | Misleading heat | Day drill-down |
| Calendar future | future month | Disabled/empty | Fake data | — |
| Large numbers | 999h+ | Compact formatting | Overflow | — |
| Timezone changed | TZ change | Consistent day buckets | Duplicate/missing day | — |
| Error | DB failure | Error + retry | Silent zeros | Retry |

**Data requirements:** today/week/month seconds, session count, current/best streak, weekly bars, subjects, recent sessions, baseline window, freeze level, achievements, calendar month, diary entries, capability-free (no permissions needed).

### 5.4 You tab

**Purpose:** settings, planning, personalisation. No review features.

| State | Entry condition | Required UI | Must not | Actions |
| --- | --- | --- | --- | --- |
| Loading | prefs | Skeletons | Flash defaults | — |
| Defaults | fresh install | All defaults visible | Hide advanced | Edit |
| Theme | 4 palettes | Live preview, selected check | Apply only after restart | Select |
| Strict mode | toggle | Description of consequences | Claim it applies to stopwatch | Toggle |
| Pause allowance | Off/1/2/3/No limit | Current value + “applies next session” | Change running session | Select |
| Breaks | 1–30m | Bounds enforced | Accept 0/31 | Edit |
| Focus Guard | toggle | 10-minute description | Promise for stopwatch | Toggle |
| Daily goal | 15m–8h | Bounds + current | Accept invalid | Edit |
| Accessibility card | granted/denied | Status + Fix | Show granted when off | Open settings |
| Notification access card | granted/denied | Status + Fix | — | Open settings |
| Notification permission card | granted/denied | Status + rationale + re-request | Auto-request on open | Request |
| Exact alarm card | granted/denied/unavailable | “Approximate reminders” copy | — | Open settings |
| Overlay card | granted/denied | Status + Fix | — | Open settings |
| Widget pinning supported | launcher supports | Pin buttons | — | Pin |
| Widget pinning unsupported | launcher doesn’t | Disabled + explanation | Silent failure | — |
| Goals empty | no goals | Empty + Add | — | Add |
| Goal on-track / behind / complete | sessions in window | Progress, percent, days left, state | Mislabel | Edit |
| Deadline none | no deadline | Add CTA | Fake countdown | Add |
| Deadline today / past | date passed | “Today” / “Passed” | Negative days | Clear |
| Clear data (if present) | — | Confirm + scope list | Silent wipe | Confirm |

**Data requirements:** focus settings, theme key, pause allowance, goal minutes, capability states, widget pin support, goals, deadline.

---

## 6. Test fixtures & device matrix

**Fixtures**

- Empty DB (fresh install).
- One 1-minute completed session.
- 8 days of sessions across two weeks (baseline + comparison).
- 60 sessions across 6 months (calendar/monthly).
- 1000h lifetime (freeze level max, large-number formatting).
- Blocked apps: YouTube, Instagram, Chrome, YouTube Music, Play Store, launcher.
- Schedules: morning, workday, evening, night (22:00–02:00), one-off date, exam-fenced, two overlapping, empty app set, disabled.
- Limits: 1m reached, 10m at 80%, 0 (removed).
- Websites: block mode with 5 domains, study mode empty, study mode populated.
- Channels: off, block list, study empty, study populated.
- Inbox: 0, 5, 200, 210 entries; duplicate keys; group summary.
- Notifications: call, alarm, ongoing FGS, clearable, non-clearable, group summary.

**Device matrix**

| Class | Examples | Focus |
| --- | --- | --- |
| Small phone | 5.0" 720p | Large text, nav labels, timer disc |
| Standard phone | 6.1" 1080p | Baseline |
| Large phone | 6.7" | One-hand reach, bottom bar |
| Tablet | 10"+ | Window size class, max width, nav rail |
| Foldable | folded + unfolded | Hinge, state preservation |
| Android 12 | exact alarms | Exact-alarm fallback |
| Android 13+ | `POST_NOTIFICATIONS` | Permission flow |
| Android 14/15 | FGS restrictions | Session start, background |
| OEM | Samsung/Xiaomi/Realme | Service kill, battery saver, notification channels |
| Locale | English + pseudo-RTL, 12h/24h | Layout, clock |
| Accessibility | TalkBack, 2.0 font, bold, reduce motion | All tabs |

---

## 7. Release gates

Ship the redesign only when all of these are true:

1. **No false-protection state.** For every combination of (accessibility, notification access, overlay, exact alarm, notification permission), Shield and Focus show the truth. Verified by a state-table test.
2. **Midnight is boring.** Limits, warnings, held-today, inbox groups, streaks, and all six widgets are correct across midnight, timezone change, and manual clock change.
3. **Overlaps and overnight are exact.** Unit tests cover overlap union, overnight tail weekday, one-off + overnight, exam fence + overnight, and DST.
4. **Empty means empty.** Every list has an empty state; empty study lists and empty rule app sets are labelled as not protecting.
5. **Every tab has Loading / Empty / Content / Degraded / Error.** Screenshot tests cover the four palettes on phone and tablet.
6. **Accessibility passes.** TalkBack, 2.0 font, display size, bold text, RTL, reduce motion; timer and nav remain usable.
7. **Adaptive layout exists.** Compact bottom nav; medium/expanded rail or max-width content; hinge-aware.
8. **No new permissions.** Merged manifest still has no `INTERNET`; permission list unchanged.
9. **Instrumentation tests exist** for the four tab empty/degraded states; zero instrumentation tests is a blocker for a redesign this size.

---

## 8. Suggested order of work

1. Add the capability-state model and wire it into Shield + Focus (fixes F1, F2, F8).
2. Add date/time-change receiver and midnight-safe limit crediting (F3, F4, F6).
3. Fix inbox dedupe/cap/group behaviour (F5).
4. Implement the four tabs’ state tables with empty/degraded/error states.
5. Adaptive layout + typography scaling (F9, F10).
6. Widget palette/freshness (F3, F16).
7. Lifecycle polish (F11–F15) and cross-cutting QA.
---

## 9. On-device QA pass — 2026-11-18 (Realme RMX5101, Android 16, debug build)

Method: `adb`-driven manual QA on a physical device. `uiautomator` bounds for tap targets, screenshots for visual verification, logcat watch for crashes. This was a **smoke + navigation pass on real hardware, not a release-gate certification**; the gates in §7 remain open where noted below.

### Verified on device

- **Focus tab** — cold launch renders (mode chips, duration chips, subject chips, intention field, timer dial). Start → immersive Focusing screen (bottom bar hidden, floating timer pill shown) → Pause (state label + Resume) → Resume → End → "End this session?" dialog (elapsed copy correct: "56s of 10m") → Save and end → Session summary ("Stopped early", planned vs actual, streak day) → back pops to Focus → Insights recomputed immediately (56s today, 1 session, week bar).
- **Shield tab** — hero status ("Access is off" while accessibility is off), permission banner, Protection rows; detail screens **Apps, Limits, Schedules ("Blocking rules"), Websites, Channels ("YouTube & Music"), Notifications** all render with correct empty states and usage. Bottom bar retained on detail screens as designed.
- **Insights tab** — TODAY hero (goal ring, streak chip), This week chart (bars, today highlighted, "16 Nov – 22 Nov", "Fresh start"), Consistency heatmap → **History** (active days marked; day detail), LEVEL card → **Levels** (ladder, progress 1m/1h), NEXT BADGE card → **Badges** (1 of 16, focus milestones grid), weekly card → **Weekly review** (1m, sessions, day-of-7 baseline), **Reflections** empty state, "All insights are calculated on this device." footer.
- **You tab** — Focus section (Daily target editor: bottom sheet, current value checked; opened/closed without changing settings), Strict mode, Pauses, Pomodoro breaks, Break length, Reminders, Floating timer, Sound; Planning section (Goals, Countdown); Appearance (4 palettes). **Widgets** (6 rows, each with a Pin action — Pins not exercised), **Privacy** (offline/storage/permissions copy), **About** (roadmap, "No account. No ads. No tracking.").
- **Logcat** — no FrostLock exceptions, no `FATAL`/`AndroidRuntime` crashes across the whole pass.

### Observations (non-blocking, not fixed)

1. Timer dial's lower half sits behind the floating bottom bar in the resting Focus state (scrolls into view; 48dp bottom padding).
2. Duration chip row overflows past the right edge (60m clipped by design; scrollable).
3. Mid-tab-restore can show a brief crossfade frame.
4. Scroll positions are restored per tab (intended).

### Incident note — 2026-11-18 evening: app uninstalled from the QA device

Between the instrumented test run (~19:08, 5/5 passed) and the font-scale check (~19:19), FrostLock disappeared from the device. System logs show the package reported as `REPLACED` and then `REMOVED` at 19:19:53, followed by `MultiApp: uninstallIfNeed com.frostlock.app` from system_server (19:21:51). Causality is unconfirmed — the user's app-lock screen ("Security test — Unable to find this app") was on screen at 19:20, and an on-device cleaner is a plausible system-side cause; the instrumented test runner does not uninstall the app under test. The user may also have tapped something in the lock screen. **Status: superseded — see "Root cause found" below. The device cleaner and the user were not responsible.**

**Data loss:** the uninstall wiped FrostLock's private storage (Room DB, prefs). On reinstall the app launches into onboarding. Lost: 2 test-era sessions (13s on 17 Nov, 56s on 18 Nov), the Day-2 streak, and any non-default preferences set during QA. No backup snapshot existed for this same-day debug install, and the sample `data_extraction_rules`/`backup_rules` contain only template comments (Auto Backup includes everything by default but had never run for this install window).

**Mitigations for the future:** keep `allowBackup="true"`; ship real (not sample) backup-rule XMLs; consider an in-app export; confirm the app-locker keeps listing FrostLock after reinstall. **Data recovery was not possible** and the deficit is documented here, not papered over.

### Incident follow-up — 2026-11-19 (second removal)

The reinstalled build (`app-debug.apk`, 17:29 build) ran fine (onboarding re-completed at 00:38 on 2026-11-19, verified by screenshot), but when the instrumented suite was re-run at ~00:44 the package was again absent from `pm list packages`, with `com.heytap.market/com.oplus.exp.installed.rec.core.InstalledRecommendActivity` (Realme App Market "new app" screen) holding focus the whole time. Each `connectedDebugAndroidTest` reinstalls the debug APK (`adb install -r`); both removals happened within ~25 minutes of a reinstall. Realme's device-care/app-market pipeline is the likelier culprit (the same class of system decision as `MultiApp: uninstallIfNeed` logged earlier); user action cannot be ruled out. **Code mitigation shipped:** explicit Auto Backup rules (`backup_rules.xml`, `data_extraction_rules.xml` — commit `c4feb10`) now include `database` + `sharedpref` for cloud and device transfer. **User mitigation required:** in Phone Manager / Security settings, stop the cleaner uninstalling or hiding FrostLock, or this will keep destroying local data. Until then, installing via adb keeps triggering the market's new-app flow. **Status: superseded — see "Root cause found" below. The Realme pipeline was not responsible.**

### Root cause found — 2026-11-19 ~12:05 (supersedes the suspicions above)

A controlled experiment with a live `adb logcat` capture identified the mechanism conclusively:

- **11:57** — plain `adb install -r`; the app stays installed and running for 5+ minutes. No removal.
- **12:03:33–12:04:19** — `./gradlew :app:connectedDebugAndroidTest` (one test). At **12:04:21** the device logs `android.intent.action.PACKAGE_REMOVED` for `com.frostlock.app` and `com.frostlock.app.test` with `dataRemove=true`, `replacing=false`, followed by `PACKAGE_FULLY_REMOVED for com.frostlock.app.test for u0`.

**The remover was the test harness, not the phone.** AGP's `connectedDebugAndroidTest` uninstalls both APKs after the run as tidy-up; every previous disappearance (19:19, 00:44, 01:28) followed a connected test run. The Realme cleaner, the App Market "new app" screens and `MultiApp: uninstallIfNeed` were coincidental / unrelated, and no user action was involved.

**Fix:** `android.injected.androidTest.leaveApksInstalledAfterRun=true` in `gradle.properties` (AGP 9.5.1 `BooleanOption`). Verified at 12:08: full `SmokeTest` suite **5/5 green**, and both packages remain installed with onboarding state intact after the run.

**Side effect to reverse (user's call):** the App Market scan toggles were turned off while chasing the wrong cause. They are not needed for the fix and can be re-enabled without breaking the dev loop.

**Data loss stands:** the uninstalls destroyed 2 QA-era sessions (13s on 17 Nov, 56s on 18 Nov) and the Day-2 streak during the first harness uninstall. Not recoverable; not re-created. Onboarding has been re-completed with placeholder answers (editable).

### Not verified on this device / still open

- Onboarding flow (device already past onboarding; state not wiped).
- Actual app blocking via accessibility (permission is OFF on this phone).
- Widget pinning, wallpaper/notification surfaces, break-timer alerts, reboot/process-death recovery.
- **§7 release gates remain open**, notably gate 9 (instrumentation/Compose UI tests do not exist yet — 374 unit tests / 55 suites are all JVM-local), gate 6 (TalkBack/RTL/reduce-motion/2.0 font on device), gates 1–4 (state-table truth model, midnight behaviour, overlap edge cases) beyond unit-test coverage, and tablet/foldable layouts (rail path is unit-tested for the size-class mapping only).
- No docs screenshots exist in the repo; capturing them remains a release-gate item.
- The QA pass created a 56-second "Stopped early" session in the user's history (streak day 2, week total 1m). Kept with the user's consent — it is honest data from a real test.
