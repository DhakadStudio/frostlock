# Regain: product and design notes

Research for FocusFlow. Regain (package `ai.regainapp`, version 75.1.2113) was
installed on the emulator from its XAPK and inspected through onboarding. The
remaining screens are gated behind a sign-in / paywall, so the feature map below
was also checked against the strings, analytics events and Room schema embedded
in the APK.

## What the user sees

- A long, conversational onboarding with a mascot ("Rega") instead of a form.
  It asks exam, study location, current distractions and a daily focus goal, then
  reflects the answers back ("Cracking Advanced Placement takes two things…").
- The answers are used to recommend a blocklist. The flow then tours one feature
  per screen: Shorts/Reels blocking, notification blocking, Strict Mode, YouTube
  channel filtering. Each screen has a single action.
- Onboarding ends with permissions, social proof, a final paywall-style pitch
  ("Reels keep filling your book"), then account creation. There is a
  `Skip & continue` path for the account step.
- The main product identity is **one focus session**. Everything else is framed as
  support for that session.

## How Regain separates features

The APK is a modular app. Each concern is its own feature module:

| Module | Responsibility |
| --- | --- |
| `feature_focus_timer` | Focus timer, stopwatch, Pomodoro, sessions, tags, groups, leaderboard, schedules |
| `feature_zen_mode` | Recurring focus windows (schedule/zen mode) |
| `feature_app_limit` | Per-app daily limits, streaks, snooze, strict limit mode |
| `feature_block_shorts` | Blocks Shorts/Reels inside YouTube/Instagram/Facebook |
| `feature_notification_batching` | Blocks/batches notifications per app and channel |
| `feature_block_website` | Domain blocking |
| `feature_productive_mode` | "Productive" in-app/browser experience |
| `feature_study_goal_home` | Daily study goal, progress ring, home widget |
| `feature_app_usage_stats` / `feature_profile_stats` | Usage stats and profile stats |
| `feature_distracting_apps` / `feature_control_apps` | The blocked-app list and per-app settings |
| `feature_invite_flow` / groups | Social groups and leaderboards |
| `feature_subscription`, `feature_onboarding`, `feature_ai_coach` | Monetisation, onboarding, mascot prompts |

The database confirms the separation:

- `focus_timer_session` stores the timer run, its tag, break settings and an
  optional `zen_mode_config_id`.
- `focus_schedule_config` (and the newer `zen_mode_config`) owns its own
  `bad_app_ids`, `habit_app_ids`, name, colour, emoji, days, time window, break
  rules, strict-mode flags and `tag_id`.
- `ft_user_tag` is only a subject label plus colour. It is **not** an app set.
- `app_limit` is a separate table keyed by app, with its own daily budget,
  streak, snooze and strict-mode timestamps.
- `notification_batching` keeps per-app/channel tags, independently of focus
  sessions.

## Takeaways for FocusFlow

1. **One clear unit per feature.** A focus session, a blocking rule, a daily limit
   and a subject should not be modelled as the same object. FocusFlow already
   splits these; keep that separation visible in the UI.
2. **Per-timing app sets are expected.** Regain gives every schedule its own
   `bad_app_ids`. FocusFlow already does this through `FocusSchedule.packages`.
3. **Tags are labels, not app sets.** Regain tags a session but still blocks the
   schedule/timer app list. The user request is stronger: in FocusFlow a subject
   should own an app set so "Work" and "Study" block different apps. This is a
   deliberate improvement over Regain, not a copy of it.
4. **Make reuse one tap.** Because a rule can run at a different time than a
   subject profile, the schedule editor should offer "Copy apps from Study" rather
   than forcing the user to re-pick ten apps.
5. **Explain the fallback.** If a subject has no apps configured, say clearly that
   the global focus-session list is used. Never leave the user guessing why an app
   was (or was not) blocked.
6. **Keep onboarding actionable.** Regain's conversational onboarding is good at
   personalising a blocklist. FocusFlow should keep its faster setup but use the
   same idea in miniature: pick a subject, see the app count, edit it in place.

## FocusFlow design chosen from this research

- Subject profiles own app sets (`SubjectStore`).
- The Focus screen picks the subject for the session and blocks that subject's
  apps; an empty set falls back to the global focus-session list.
- Rules remain independent and can copy a subject's app set in the editor.
- Daily limits and schedules stay independent of the focus-blocking master switch.

## What FocusFlow adopted from the emulator review

These are deliberate, local-first adaptations rather than copies of Regain's paid
or social features:

| Regain idea | FocusFlow implementation |
| --- | --- |
| A session is the centre of the product | The immersive timer takes over the whole screen while a session runs, hiding the bottom bar and system bars |
| Immersive aurora timer | Animated teal/violet/rose backdrop, gradient progress ring, subject badge and pause/stop controls |
| Coloured tags | Every subject has a colour that follows it into Focus chips, the session badge, schedule copy chips and the Stats breakdown |
| "What did you study?" reflection | A post-session study diary dialog saves a 280-character note; notes appear under recent sessions and can be edited or deleted with the session |
| Break sheet | Pomodoro rounds still lead into a break, with a "Skip break" action and a clear statement that blocking pauses |
| Strict Mode | Press-and-hold to end a strict session; the timer hides normal early exits |
| Proving the block works | The focus screen shows how many apps are blocked and offers a "Test" action that opens a blocked app so the user can see the block screen |

A small extra that follows the same "one task" philosophy: the Focus screen asks
for **one thing for this session** (optional, 80 characters). It appears in quotes
on the immersive timer, so the user sees the outcome rather than only the clock.

## Deliberately not copied

- Social proof, leaderboards, groups and invite flows. FocusFlow stays offline and
  never invents testimonials.
- Subscriptions, paywalls and account creation.
- Server-side AI coaching. Any future coaching should work from local session data.
- Per-app daily limits, notification batching and website blocking are larger
  features; the data model already keeps schedules, subjects and sessions separate
  enough to add them later without coupling.

## Emulator verification

Verified on `emulator-5554` (Medium_Phone_API_37.0):

- Subject chips show Study (amber), Work (blue), Reading (pink), Coding (green) and
  Revision (orange).
- Setting Study's apps to Chrome made the running session report "Blocking 1 app /
  Study app set"; the overlay's Test action opened the block screen for Chrome.
- A subject with no apps reports the fallback explicitly: "Focus-session app list".
- The schedule editor's "copy apps from a subject" chip carries the same colour dot
  as the subject ("Study · 1").
- Stats shows coloured dots and bars per subject; sessions with no subject fall
  back to the violet accent.
- Saving a session opens the study diary; the typed intention ("Finish chapter 4")
  appeared on the immersive timer.
- Build, lint-free compile and 27 unit tests pass after each feature commit:
  `a803e6f`, `463a092`, `d0ac60e`, `26951e4`, `3753155`.

