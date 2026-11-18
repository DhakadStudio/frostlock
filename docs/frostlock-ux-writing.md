# FrostLock UX writing & naming

**Date:** 2026-11-17
**Scope:** Vocabulary, screen titles, section headers, buttons, empty states, permission states, and status messages for the Focus / Shield / Insights / You redesign.
**Related:** `docs/frostlock-redesign-plan.md`, `docs/frostlock-vs-regain.md`

> **Status:** this document is the canonical vocabulary source for the redesign. Plan v3 adopts its terms in full. Two placement overrides apply: (1) You has no “Protection” section — home-screen blocking lives in **Shield → Apps → Advanced**, and **Floating timer** sits under **You → Focus**; (2) the bottom bar stays visible on standard detail screens and is hidden only on onboarding, Focusing, Session summary, full-screen editors and the block wall.

The goal is not to rename everything. It is to make one word mean one thing, remove the "Focus" prefix from every feature, and let the four tabs carry the mental model.

---

## 1. Voice

### 1.1 Four rules

| Rule | Sounds like | Avoid |
| --- | --- | --- |
| **Calm, not cold** | "Held until 17:00." | "Access denied." / "You cannot proceed." |
| **Clear, not clever** | "Block apps during focus." | "Freeze out the noise." / "Stay in the zone." |
| **Brief, not terse** | "No limits yet. Start with an app you open without thinking." | "No data." |
| **Private, not preachy** | "Stays on this device." | "We value your privacy." / "We never sell your data." |
| **Human, not cute** | "Session saved." | "Nice work, superstar!" / "Oops!" |

FrostLock should feel like a quiet tool that is already on your side. It states what is happening, what happens next, and what stays private. It does not congratulate, shame, or moralise.

### 1.2 Mechanics

- **Sentence case** for screen titles, section headers, buttons, labels, toggles, and status chips. Capitalise only proper nouns: FrostLock, YouTube, YouTube Music, Android, Pomodoro.
- **Second person, present tense.** "You" and "your". Use "FrostLock" as the actor. Avoid "we" except in a deliberate product promise.
- **Verbs first in actions.** "Start focus", "Add site", "Save and end".
- **No terminal punctuation** on buttons, labels, toggles, and one-line statuses. Use full stops in body sentences.
- **Contractions are fine.** "can't", "doesn't", "you're". Avoid "cannot" and "do not" in UI.
- **Numerals for quantities and time.** "5 min", "1h 20m", "3 sessions", "Day 4 of 7". Use "min" in compact UI and "minutes" in sentences.
- **Time ranges:** `09:00–17:00` (en dash, no spaces). **Dates:** `Mon 17 Nov` in compact UI, `17 Nov 2026` in detail.
- **No exclamation marks.** A question is allowed only when the user is genuinely choosing something ("What are you counting down to?"), never in an error or status.
- **Avoid "please", "sorry", "successfully", "simply", "just".** They add tone without meaning.
- **Avoid "tap here".** The button already says what it does.
- **Do not rely on colour alone.** Pair every state with a word: "On", "Off", "Held", "Reached".

### 1.3 Words to retire from UI

| Avoid | Use instead | Why |
| --- | --- | --- |
| App Blocker | Shield / Apps | A product label, not a place. |
| blocker, blocking tool | Shield | "Shield" is the space; "block" is the action. |
| rule, blocking rule | schedule | "Rule" is implementation language. |
| budget | limit | "Budget" sounds financial. |
| time sink, distraction | the apps you reach for | Neutral, not judgemental. |
| focus session | session | "Session" is the recorded unit; "focus" is the activity. |
| unlock (a feature) | open, turn on | "Unlock" implies a paywall. |
| configure, manage | set up, edit, choose | Plain verbs. |
| enable / disable | turn on / turn off | Friendlier and clearer. |
| grant | allow | Matches Android's own language. |
| local-only, on-device, offline-first | works offline, stays on this device | Plain language, same promise. |
| achievement | badge | Concrete and countable. |
| freeze level / freeze ladder | level / levels | Keeps the ice theme as flavour, not jargon. |
| deadline | countdown | Less stressful, same meaning. |
| Pro | everything included / free forever | There is no paid tier; "Pro" contradicts the promise. |

---

## 2. The naming model

### 2.1 Four spaces, one job each

| Tab | Job | Belongs here | Never here |
| --- | --- | --- | --- |
| **Focus** | Start and finish one session. | Timer modes, length, intention, subject, strict mode, pauses, sound, last session. | Blocking toggles, stats grids, goal editing, permission hubs. |
| **Shield** | Decide what is held and see what is held now. | Apps, limits, schedules, websites, channels, notifications, permission prompts for protection. | Session timer, charts, diary, goals, theme. |
| **Insights** | Look back and reflect. | Today/week totals, streak, history, weekly review, reflections, badges, levels. | Blocking controls, settings, goal editing, start-focus CTA as the main action. |
| **You** | Set defaults and keep the app yours. | Daily target, strict mode, pauses, reminders, floating timer, home screen, theme, widgets, goals, countdown, privacy, about. | Protection lists, live session controls, stats dashboards, report/diary links. |

### 2.2 One concept, one word

| Concept | Canonical word | Not |
| --- | --- | --- |
| The protection space | **Shield** | App Blocker, Protection, Blocking |
| A recorded unit of focus | **session** | focus, focus session, timer |
| The activity / the first tab | **focus** | focus session, deep work |
| What FrostLock does in the moment | **held** | blocked, paused, frozen |
| The user's intent to stop something | **block** | hold, freeze, shield |
| A per-app daily cap | **limit** | budget, allowance, quota |
| A time-based blocking window | **schedule** | rule, routine, window |
| The notification feature | **Notifications** | Notification shield, alert blocker |
| The website feature | **Websites** | Website blocking, web filter |
| The channel feature | **Channels** | YouTube & Music, channel filter |
| The calendar/history screen | **History** | Focus calendar, stats calendar |
| The weekly reflection | **Weekly review** | Weekly report |
| The notes screen | **Reflections** | Study diary, journal |
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

### 2.3 The "block" / "held" distinction

This is the most important writing rule in the redesign.

- **Block** is what the user sets up. It appears on controls and in configuration copy.
  - "Block apps during focus."
  - "Blocked apps"
  - "Block list"
- **Held** is what FrostLock does in the moment. It appears in status, walls, and notifications.
  - "Instagram is held during focus."
  - "Held by Weekday study."
  - "3 apps held."

This gives the product a calm, non-violent verb for the moment of interruption, while keeping "block" for the user's own decision. It also prevents "blocked" from appearing in every sentence.

### 2.4 Never use "Focus" as a prefix

The tab already provides the context. Remove it from feature names:

| Current | New |
| --- | --- |
| Focus blocking | Block during focus |
| Focus deadline | Countdown |
| Focus goals | Goals |
| Focus calendar | History |
| Focus Guard | Reminders |
| Focus bubble | Floating timer |
| Focus ambience | Sound |

The same rule applies to "Shield": the tab is the shield, so sub-features are **Apps**, **Limits**, **Schedules**, **Websites**, **Channels**, **Notifications** — not "Notification shield" or "Shield settings".

---

## 3. Screen titles and section headers

### 3.1 Focus

| Screen | Title | Subtitle | Section headers | Primary action |
| --- | --- | --- | --- | --- |
| Focus | **Focus** | One task at a time. | Session · Intention · Subject · Session settings · Last session | **Start focus** |
| Active session | **Focusing** | *No subtitle; the timer is the content.* | — | **Pause** / **End** |
| Break | **Break** | Step away. The next round starts on its own. | — | **Skip break** |
| Session summary | **Session summary** | Here's what you finished. | Summary · Note · Streak | **Done** |

Notes:
- The mode control stays **Pomodoro / Timer / Stopwatch**.
- Use **Session length**, not "Timer length" or "Duration".
- Use **Intention** for the field. Placeholder: "What are you focusing on?" Example: "Finish chapter 4".
- Use **Subject** for the tag. Supporting copy: "Subjects keep their own app lists."
- Use **Session settings** for the bottom sheet that holds strict mode, pauses, and sound. This avoids four competing section headers.
- **Last session** is a single card, not a list. The full list belongs in Insights → History.

### 3.2 Shield

| Screen | Title | Subtitle | Section headers | Primary action |
| --- | --- | --- | --- | --- |
| Shield home | **Shield** | What's held while you focus. | Held right now · Protection | **Choose apps** |
| Apps | **Apps** | Choose what stays closed during focus. | Block during focus · Blocked apps · All apps | **Choose apps** |
| Limits | **Limits** | A daily cap for any app. | Your limits · Suggested limits · All apps | **Set a limit** |
| Schedules | **Schedules** | Block apps at set times. | Your schedules · Coming up | **New schedule** |
| Websites | **Websites** | Hold distracting sites during focus. | Mode · Blocked sites · Allowed sites | **Add site** |
| Channels | **Channels** | Hold distracting channels during focus. | Mode · Blocked channels · Allowed channels | **Add channel** |
| Notifications | **Notifications** | Hold alerts until focus ends. | Held today · Inbox · Most held apps | **Allow notifications** |

Notes:
- Shield home is a status page, not a settings page. Each row answers "what is held right now?" and links to its detail.
- Use **Held right now** as the status section. It is warmer and more precise than "Active now", which is also used for schedules.
- Use **Protection** for the list of controls. Do not call it "Tools" or "Features".
- The Apps toggle is **Block during focus**, with status "On · 4 apps held" or "Off". Do not call it "Focus blocking".
- The Limits screen uses **Limits** as the title. "Daily" appears in supporting copy and in "Resets at midnight", not in the title.
- The Schedules screen uses **schedule** for both the object and the list. "Rule" disappears from UI.
- The Notifications toggle is **Hold notifications**. The screen title is **Notifications**, not "Notification shield".
- Websites and Channels both use **Mode** with **Block list** and **Study list**. This is the one place a little repetition is useful, because the two screens behave the same way.

### 3.3 Insights

| Screen | Title | Subtitle | Section headers | Primary action |
| --- | --- | --- | --- | --- |
| Insights home | **Insights** | Your focus, over time. | Today · This week · Recent sessions · By subject · Keep going | **Start focus** |
| History | **History** | Every session, day by day. | This month · Sessions | **Start focus** |
| Weekly review | **Weekly review** | Your week, in focus. | Summary · Daily rhythm · By subject · Compared with your first week | **Start focus** |
| Reflections | **Reflections** | Notes from your sessions. | This week · Earlier | **Add a note** |
| Badges | **Badges** | Milestones from your focus history. | Collected · Next up | **Start focus** |
| Levels | **Levels** | Titles earned from lifetime focus. | Your level · All levels | **Share level** |

Notes:
- **Insights** replaces "Stats". It is a place to understand, not a dashboard to admire.
- **History** replaces "Focus calendar". The calendar is the main view; "History" also covers the session list.
- **Weekly review** replaces "Weekly report". Keep the existing line "Your focus, one week at a time." as the subtitle if a longer sentence is needed.
- **Reflections** replaces "Study diary". The item is a **note**; the collection is **Reflections**.
- **Badges** replaces "Achievements". "Badge" is countable and concrete; "achievement" is abstract and slightly gamified.
- **Levels** replaces "Freeze levels". Use "Level 4 · Frostbite" and "Title" for the name.
- "Keep going" is the home for Weekly review, Badges, and Levels. It is an invitation, not a command.

### 3.4 You

| Screen | Title | Subtitle | Section headers | Primary action |
| --- | --- | --- | --- | --- |
| You home | **You** | Your settings, your data. | Focus · Protection · Planning · Appearance · Privacy · About | — |
| Goals | **Goals** | Targets for exams and projects. | Your goals | **New goal** |
| Countdown | **Countdown** | A countdown that stays on your device. | Target · Date | **Set countdown** |

Notes:
- **You** replaces "Profile". It is broader than a profile and avoids the social/account implication.
- **Focus** section: Daily target, Strict mode, Pauses, Reminders, Sound.
- **Protection** section: Home screen, Floating timer. Permission prompts for these stay inline.
- **Planning** section: Goals, Countdown.
- **Appearance** section: Theme, Widgets.
- **Privacy** section: "Works offline", "Stays on this device", data controls, and a plain explanation of access. Do not duplicate the actual permission toggles here; link to the context where they are used.
- **About** section: version, licences, "Free forever".
- **Daily target** replaces "Daily goal". This is the minutes-per-day setting. **Goals** are the named long-term targets. The distinction prevents "goal" from meaning two things.
- **Countdown** replaces "Focus deadline". The date field is **Target date**.
- **Reminders** replaces "Focus Guard". The toggle is **Gentle reminders**.
- **Floating timer** replaces "Focus bubble".
- **Sound** replaces "Focus ambience".
- Remove "FrostLock Pro". Use **Everything included** or **Free forever**.

### 3.5 Onboarding

The onboarding can keep more of the ice voice, but the slide names should be plain.

| Current slide | New title |
| --- | --- |
| Welcome | **Welcome** |
| Distraction Radar | **What pulls you away** |
| Focus toolkit | **Your timer** |
| App control | **What gets held** |
| Automatic routine | **Your schedule** |
| Visible progress | **Your progress** |
| Goals | **Your goals** |
| Triggers | **Your triggers** |
| Reason | **Your reason** |
| Commitment | **Ready** |

- "Skip intro" → **Skip** or **Set up later**.
- "Hold for two seconds" stays; it describes the gesture.
- "No account. Nothing leaves this phone." → "No account. Nothing leaves your device."
- Keep "FOCUS, FROZEN IN PLACE" as a brand line on the welcome slide only. Do not use it as a screen title or button.

### 3.6 Widgets

Widget labels should match the tab vocabulary.

| Current | New |
| --- | --- |
| Focus today | **Today** |
| Daily goal | **Daily target** |
| This week | **This week** |
| Focus calendar | **History** |
| Daily usage | **Usage** |
| Phone unlocks | **Unlocks** |

---

## 4. Buttons and actions

### 4.1 Standard labels

| Action | Label | Notes |
| --- | --- | --- |
| Begin a session | **Start focus** | Not "Start session" or "Let's focus". |
| Pause | **Pause** | |
| Resume | **Resume** | |
| End early | **End** | The confirmation explains the consequence. |
| Save a finished session | **Save and end** | Avoid "Save & end" in UI; the ampersand is noisy. |
| Throw away a session | **Discard** | Destructive style. |
| Stay in the session | **Keep focusing** | Used in the end-confirmation dialog. |
| Add a reflection | **Add a note** | |
| Finish a flow | **Done** | |
| Choose blocked apps | **Choose apps** | |
| Set a limit | **Set a limit** | |
| Create a schedule | **New schedule** | Not "Add rule". |
| Add a website | **Add site** | |
| Add a channel | **Add channel** | |
| Open the inbox | **Open inbox** | |
| Allow notification access | **Allow notifications** | Matches Android. |
| Open system accessibility | **Open accessibility settings** | System wording; keep it exact. |
| Allow overlay | **Allow floating timer** | Friendlier than "Allow display over other apps". |
| Change a value | **Change** | Not "Edit" when it is a single value. |
| Remove one item | **Remove** | For limits, sites, channels, widgets. |
| Delete a schedule or goal | **Delete** | Destructive; use in a confirmation. |
| Clear a collection | **Clear inbox** / **Clear all** | "Clear all" is acceptable inside a section with a count. |
| See more | **See all** | Not "View all". |
| Try again | **Try again** | |
| Dismiss a prompt | **Not now** | Not "Later" (implies a scheduled return). |

### 4.2 Destructive confirmations

Pattern: **verb + object?** → consequence → destructive action / Cancel.

- "Delete this schedule?" / "It will stop holding apps. This can't be undone." / **Delete** · **Cancel**
- "Discard this session?" / "It won't appear in your history or reflections." / **Discard** · **Keep focusing**
- "Clear the inbox?" / "Held alerts will be deleted from this device." / **Clear** · **Cancel**

Do not use "Are you sure?" It asks for reassurance instead of explaining the consequence.

### 4.3 Permission buttons

Use **Allow** for the user's decision and the exact system name for the destination.

- **Allow notifications**
- **Open accessibility settings**
- **Allow floating timer** (then the system screen says "Display over other apps")
- **Open settings** when the destination is generic

Avoid "Grant", "Enable", "Turn on access", and "Continue" for permissions.

---

## 5. Empty states

Pattern: **what is true now** + **the next useful action** + **one button**. One or two sentences. No exclamation, no apology, no "Oops".

| Screen | Empty state | Action |
| --- | --- | --- |
| Focus · last session | "No sessions yet. Your first focus will show up here." | **Start focus** |
| Shield · Apps | "No apps blocked yet. Choose the apps that pull you away." | **Choose apps** |
| Shield · Limits | "No limits yet. Start with an app you open without thinking." | **Set a limit** |
| Shield · Schedules | "No schedules yet. Add one for study, work, or wind-down." | **New schedule** |
| Shield · Websites | "No sites blocked yet. Add a site, or start with the suggested list." | **Add site** |
| Shield · Channels | "No channels blocked yet. Add a channel to hold it during focus." | **Add channel** |
| Shield · Notifications (off) | "Notifications are off. Turn them on to hold alerts during focus." | **Turn on** |
| Shield · Notifications (on, nothing held) | "Nothing held yet. The next alert from a blocked app will wait here." | **Open inbox** |
| Shield · Notifications (no access) | "Access is off. Allow notifications and held alerts will appear here." | **Allow notifications** |
| Insights · home | "No focus yet. Your first session will start your history." | **Start focus** |
| Insights · History | "No sessions on this day. Pick another day, or start a focus." | **Start focus** |
| Insights · Weekly review | "No focus this week. Start a session to see your week." | **Start focus** |
| Insights · Reflections | "No reflections yet. Add a note after your next session." | **Start focus** |
| Insights · Badges | "Your first badge is one session away." | **Start focus** |
| Insights · Levels | "Your first level starts with one session." | **Start focus** |
| You · Goals | "No goals yet. Set a target for an exam, project, or habit." | **New goal** |
| You · Countdown | "No countdown set. Add a target date to keep it in view." | **Set countdown** |
| Search · no match | "No apps match “insta”." / "No sites match “news”." | — |
| Widgets · unsupported | "Widgets aren't available on this launcher. You can still use FrostLock in the app." | **Close** |

Notes:
- Name the user's query in search empty states when possible. It confirms what was searched.
- "Nothing held yet" is a good state. Keep it; it is calm and precise.
- Do not use "No data" or "Empty" as a user-facing phrase.

---

## 6. Permission states

Pattern: **Access needed** (eyebrow) → **Allow [feature]** (title) → one sentence of why → one sentence of what FrostLock does *not* do → button.

| Feature | When access is off | Prompt title | Body | Button |
| --- | --- | --- | --- | --- |
| Apps (accessibility) | "App blocking is off until access is allowed." | **Allow app blocking** | "FrostLock needs Accessibility to see when a blocked app opens. It doesn't read or store what you type." | **Open accessibility settings** |
| Websites (accessibility) | "Websites is off until access is allowed." | **Allow website blocking** | "FrostLock reads the browser address bar to match your list. It doesn't store pages or history." | **Open accessibility settings** |
| Channels (accessibility) | "Channels is off until access is allowed." | **Allow channel filtering** | "FrostLock reads the channel name on a watch page to match your list. It doesn't store what you watch." | **Open accessibility settings** |
| Notifications | "Notifications is on, but access is off. Alerts will still arrive." | **Allow notifications** | "FrostLock needs notification access to hold alerts from blocked apps. Held alerts stay on this device." | **Allow notifications** |
| Floating timer (overlay) | "Floating timer is off until access is allowed." | **Allow floating timer** | "FrostLock needs permission to show the timer over other apps. It only draws the timer." | **Allow floating timer** |
| System notifications | "Notifications are off. Your timer still runs, but the ongoing alert won't show." | **Turn on notifications** | "FrostLock uses a quiet ongoing alert so you can return to the timer." | **Turn on** |

Notes:
- Put the prompt where the feature lives. Do not build a separate "Permissions" hub that repeats the same three cards.
- You → Privacy can explain the permission model in one paragraph, with links to the relevant Shield screens. It should not contain duplicate toggles.
- Use "Access is off", not "Permission denied". The second sounds like blame.
- State the limit of the access explicitly. The existing "doesn't store pages or history" line is the model: one sentence of scope, one sentence of restraint.
- Do not say "FrostLock needs you to..." It is the app's requirement, not the user's failing. "FrostLock needs Accessibility to..." is enough.

---

## 7. Status messages

### 7.1 Session

| Moment | Message |
| --- | --- |
| Running | "Focusing · 24:58 left" |
| Pomodoro round | "Round 2 of 4 · 24:58 left" |
| Paused | "Paused · 12:04 left" |
| Break | "Break · 4:12 left" |
| Strict mode | "Strict mode is on. This session can't be paused or ended early." |
| Time up | "Time's up. Session saved." |
| Saved | "Session saved." |
| Discarded | "Session discarded." |
| End confirmation | "End this session?" / "Save it to your history and reflections, or discard it so it leaves no trace." |
| No notification permission | "Notifications are off. Your timer still runs, but the ongoing alert won't show." |

### 7.2 Shield

| Moment | Message |
| --- | --- |
| Shield on | "Shield is on." |
| Shield off | "Shield is off." |
| Apps held | "4 apps held during focus." |
| Apps off | "Apps are not held during focus." |
| Schedule active | "Weekday study is active until 17:00." |
| Next schedule | "Next: Weekday study · Mon 09:00." |
| No schedule active | "No schedule is active right now." |
| Limit in progress | "Instagram · 18 of 30 min used today." |
| Limit nearly reached | "Instagram · 5 min left today." |
| Limit reached | "Instagram limit reached. Resets at midnight." |
| Notifications holding | "Holding alerts from blocked apps." |
| Notifications off | "Alerts arrive normally." |
| Held today | "3 held today." |
| Websites on | "12 sites held during focus." |
| Channels on | "5 channels held during focus." |

### 7.3 Block wall

The wall is the most important status surface. It should be factual, calm, and give one clear way back.

| Reason | Title | Body | Countdown label | Actions |
| --- | --- | --- | --- | --- |
| Focus session | **Held during focus** | "Instagram is held while you focus." | "24:58 left in this session" | **Back to focus** · **Open FrostLock** |
| Schedule | **Held by a schedule** | "Instagram is held by “Weekday study.”" | "12:04 left in this schedule" | **Go home** · **Open FrostLock** |
| Daily limit | **Limit reached** | "Instagram limit reached. 30 min used today." | "Resets at midnight" | **Go home** · **Open FrostLock** |
| Website block list | **Held during focus** | "youtube.com is on your block list." | "24:58 left in this session" | **Back to focus** · **Open FrostLock** |
| Website study list | **Not on your study list** | "youtube.com isn't on your study list. Only study sites open during focus." | "24:58 left in this session" | **Back to focus** · **Open FrostLock** |
| YouTube home | **Held during focus** | "YouTube home is held while you focus. Search and subscriptions still work." | "24:58 left in this session" | **Back to focus** · **Open FrostLock** |
| Channel | **Held during focus** | "This channel is held while you focus. Study channels still play." | "24:58 left in this session" | **Back to focus** · **Open FrostLock** |
| Play Store search | **Held during focus** | "This search is held while you focus." | "24:58 left in this session" | **Back to focus** · **Open FrostLock** |
| No countdown | **Held during focus** | "Instagram is held until this session ends." | "Until you end this session" | **Back to focus** · **Open FrostLock** |

Notes:
- Replace "Stay in the zone" with **Held during focus**. The old line motivates; the new one explains.
- Replace "Back to work" with **Back to focus**. Not every session is work.
- Use **Held by “Weekday study.”** with the schedule name in curly quotes. If the name is long, truncate with an ellipsis in the chip and keep the full name in the body.
- "Held by your focus session." / "Held by a daily limit." are the two generic reason chips.
- Do not put a "Disable protection" or "Unblock" action on the wall. The way out is to end the session or change the rule in Shield, not to undermine it in the moment.

### 7.4 Insights

| Moment | Message |
| --- | --- |
| Today | "Today · 1h 20m" |
| This week | "This week · 6h 40m" |
| Streak | "7 day streak" / "No streak yet" |
| Baseline start | "Your baseline starts today." |
| Baseline progress | "Day 4 of 7. Keep focusing; your first comparison unlocks in 3 days." |
| Baseline saved | "Baseline saved." |
| Comparison | "Your focus is up 12% from your first week." |
| No focus | "No focus logged this week." |
| Best day | "Best day · Wednesday" |
| Subject | "By subject" with rows such as "Physics · 3h 20m" |

### 7.5 Goals and Countdown

| Moment | Message |
| --- | --- |
| Goal on track | "On track · 3h 20m of 10h" |
| Goal slightly behind | "A little behind · 3h 20m of 10h" |
| Goal complete | "Complete · 10h of 10h" |
| Goal time left | "12 days left" |
| Goal date passed | "Target date passed" |
| Countdown | "42 days to go" |
| Countdown today | "Today" |
| Countdown passed | "Date passed" |
| Countdown cleared | "Countdown cleared." |

Use "A little behind" rather than "Behind". It is honest without being a verdict.

### 7.6 System notifications

| Notification | Copy |
| --- | --- |
| Ongoing focus | "Focusing · 24:58 left" |
| Paused | "Paused · 12:04 left" |
| Break | "Break · 4:12 left" |
| Limit warning | "Instagram · 5 min left today" |
| Limit reached | "Instagram limit reached. Resets at midnight." |
| Schedule start | "Weekday study is on until 17:00." |
| Schedule end | "Weekday study ended." |
| Held summary | "3 alerts were held during focus." |
| Gentle reminder | "10 minutes left in this round." |
| Permission reminder | "Notifications are off. Turn them on to see your timer." |

Keep notifications quiet. No emoji, no exclamation, no streak guilt. "10 minutes left in this round." is a nudge; "You should be focusing" is not.

### 7.7 Errors

Pattern: **Couldn't [action]. [What to do next].**

- "Couldn't save. Try again."
- "Couldn't open settings. Open Android Settings and allow access there."
- "This app is no longer installed."
- "That date has already passed. Choose a later date."

No "Oops", "Uh oh", "Something went wrong", or error codes in the primary line.

---

## 8. Privacy and offline language

FrostLock's privacy promise is a product feature, not a badge. Say it plainly and only where it matters.

### 8.1 Approved lines

- "Works offline."
- "Stays on this device."
- "Nothing leaves your device."
- "No account. No ads. No tracking."
- "FrostLock doesn't use the internet."
- "Held alerts stay on this device."
- "FrostLock reads the address bar to match your list. It doesn't store pages or history."
- "Your countdown is stored only on this device. No calendar permission is needed."

### 8.2 Avoid

- "local-only", "on-device", "offline-first", "privacy-first" — these are engineering and marketing terms.
- "We never sell your data" — implies the industry norm and makes a promise the UI cannot prove.
- "Bank-level security", "military-grade encryption", "anonymous" — vague or untrue unless precisely implemented.
- "Sync", "cloud", "account", "sign in" — unless the sentence is explicitly saying they do not exist.

### 8.3 Where to put it

- **Shield permission prompts:** one sentence of scope, one sentence of restraint.
- **You → Privacy:** the full promise, in three short lines: works offline, stays on this device, no account.
- **Block wall:** no privacy copy. The user needs the reason and the way back.
- **Onboarding welcome:** the three pills "Offline / No account / Private" can stay; they are scannable and true.

---

## 9. Duplication guardrails

The redesign's biggest risk is writing the same information in two tabs. These rules prevent it.

| Concept | Single home | Cross-link copy |
| --- | --- | --- |
| Start a session | Focus | "Start focus" |
| Blocking controls | Shield | "Open Shield" |
| Live session state | Focus | "Back to focus" |
| Today/week totals | Insights | "See insights" |
| Full session history | Insights → History | "View history" |
| Weekly reflection | Insights → Weekly review | "Open weekly review" |
| Session notes | Insights → Reflections | "Open reflections" |
| Badges and levels | Insights → Badges / Levels | "View badges" / "View levels" |
| Daily target setting | You → Focus | "Change daily target" |
| Named goals | You → Planning → Goals | "Open goals" |
| Countdown | You → Planning → Countdown | "Open countdown" |
| Permissions | In context: Shield → Apps, Shield → Notifications, You → Floating timer | "Allow app blocking" / "Allow notifications" |
| Theme and widgets | You → Appearance | "Change theme" / "Add widget" |

Specific decisions:

- **Recent sessions** appears only in Insights. Focus shows **Last session**, a single card, so a user can resume without duplicating the history list.
- **Daily target** is set in You. Insights shows progress against it ("1h 20m of 3h") but never offers the setting.
- **Countdown** lives in You. It can appear as a single line on Insights only if it is framed as context, not a second editable card. Recommended: keep it out of Insights entirely.
- **Weekly review** lives in Insights. You should not have a "Weekly report" link.
- **Reflections** lives in Insights. You should not have a "Study diary" link.
- **Permission cards** live with the feature. You → Privacy explains, but does not duplicate the toggles.
- **Stats tiles** live in Insights. Focus and Shield should not show "This week" or "Sessions" tiles.

---

## 10. Before and after

| Current | Recommended | Why |
| --- | --- | --- |
| App Blocker / "Two independent tools..." | **Shield** / "What's held while you focus." | Names the space, not a tool. One job. |
| Focus blocking / "On — selected apps lock during focus sessions" | **Block during focus** / "On · 4 apps held" | Removes the prefix; uses the block/held distinction. |
| Blocking rules | **Schedules** | "Rule" is implementation language. |
| Daily limits | **Limits** | "Daily" is explained by "Resets at midnight". |
| Notification shield | **Notifications** | The tab is already Shield; do not repeat it. |
| Focus deadline | **Countdown** | Calmer, and matches the UI. |
| Focus goals | **Goals** | Removes the prefix; distinguishes from Daily target. |
| Study diary | **Reflections** | Broader than study, matches the existing copy. |
| Weekly report | **Weekly review** | A review is reflective; a report is administrative. |
| Stats | **Insights** | The tab's job is understanding, not statistics. |
| Profile | **You** | Broader than a profile, no account implication. |
| Focus calendar | **History** | Covers the calendar and the session list. |
| Achievements | **Badges** | Concrete and countable. |
| Freeze levels | **Levels** | Keeps the ice theme as flavour. |
| Focus Guard | **Reminders** | Plain, no brand jargon. |
| Daily goal | **Daily target** | Frees "goal" for named long-term targets. |
| Focus bubble | **Floating timer** | Describes what it is. |
| Website blocking | **Websites** | Names the surface, not the mechanism. |
| YouTube & Music channels | **Channels** | Shorter; the screen explains the surface. |
| "Nice work! Your session has been saved." | "Session saved." | Calm, no praise inflation. |
| "Permission needed" | **Access needed** / "Allow app blocking" | Explains what is needed and why. |
| "Grant notification access" | **Allow notifications** | Matches Android and sounds human. |
| "The usual time sinks." | "The apps you reach for without thinking." | Neutral, not judgemental. |
| "Freeze out the noise" | "One task at a time." | Clear promise for the Focus tab. |
| "Stay in the zone" | "Held during focus." | Explains the state. |
| "Back to work" | **Back to focus** | Not every session is work. |
| "FrostLock Pro" | **Everything included** / **Free forever** | There is no paid tier. |

---

## 11. Quick checklist for new strings

Before shipping a string, ask:

1. **Is this the one word we chose?** Check the canonical table. Do not introduce a synonym for variety.
2. **Does it repeat the tab name?** Remove "Focus" or "Shield" from sub-feature names.
3. **Is it an action or a state?** Actions use verbs ("Block apps"); states use "held" ("4 apps held").
4. **Could it shame the user?** Remove "failed", "behind", "cheating", "distraction", "time sink".
5. **Does it promise privacy without proof?** Prefer "Stays on this device" over "We respect your privacy".
6. **Is it a sentence where a label would do?** Buttons and toggles should be 1–3 words.
7. **Does it use sentence case?** Only proper nouns are capitalised.
8. **Is it duplicated elsewhere?** If yes, link to the canonical home instead of repeating the data.
9. **Does it work out of context?** Screen readers and notifications should understand it without the surrounding card.
10. **Would a calm person say it out loud?** If not, simplify.

---

## Appendix: voice at a glance

| Do | Don't |
| --- | --- |
| "Held until 17:00." | "Access denied." |
| "Session saved." | "Nice work! 🎉" |
| "No limits yet. Start with an app you open without thinking." | "No data found." |
| "Stays on this device." | "We take your privacy seriously." |
| "A little behind · 3h 20m of 10h" | "You failed your goal." |
| "Allow notifications" | "Grant notification access" |
| "Back to focus" | "Back to work" |
| "Works offline." | "Offline-first architecture." |
| "Schedules" | "Blocking rules" |
| "Reflections" | "Study diary" |
| "Weekly review" | "Weekly report" |
| "Daily target" | "Daily goal" |
| "Countdown" | "Focus deadline" |
| "Badges" | "Achievements" |
| "Levels" | "Freeze levels" |
| "Reminders" | "Focus Guard" |
