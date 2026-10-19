# LifeQuest

LifeQuest is a Kotlin application that turns real activities into RPG quests. Complete fitness, study, work, and personal quests to earn experience, improve character statistics, and unlock achievements. Schedule quests in Google Calendar and edit them from either application.

**Project status:** Implementation plan. The application has not been built yet.

## 1. Product scope

Build a Kotlin/JVM console application first, continuing the style of the previous RPG project while adding planning, persistent data, and networking. Keep the game logic independent of the console so a graphical desktop interface can be added later.

The first complete release must support:

- One local player profile with a name, cosmetic character archetype, XP, level, and category statistics.
- Creating, editing, searching, filtering, completing, reopening, and archiving quests.
- Difficulty levels, categories, notes, and optional schedules.
- An agenda, completion history, achievements, and daily/weekly progress summaries.
- Saving data automatically and working without an internet connection.
- Connecting one Google account to an app-created calendar named **LifeQuest**.
- Two-way synchronization of quest titles, notes, schedules, categories, difficulty, and completion status.
- Importing supported events created directly in the LifeQuest calendar as new quests.
- Durable pending changes, visible sync errors, and explicit conflict resolution.

Two-way synchronization is a required release feature. A graphical interface, Android app, multiplayer, inventory, combat, and a hosted sync service are later extensions.

### Example user journey

1. Create a character and a medium-difficulty Study quest called "Practice Kotlin collections."
2. Schedule it for an hour tomorrow and sync it to the LifeQuest calendar.
3. Open Google Calendar and drag that event to another time.
4. LifeQuest imports the revised schedule without creating a second quest.
5. Complete the quest in LifeQuest or through the event's LifeQuest description block.
6. Earn the quest's XP once, update the character, and retain the completion in history.

## 2. Kotlin assignment coverage

| Requirement | Planned demonstration |
| --- | --- |
| Mutable and immutable variables | Use `val` for quest IDs and dependencies; use `var` for editable state or menu state where appropriate. |
| Conditionals | Validate input, detect completion transitions, check unlock conditions, and handle offline state. |
| Loops | Run the main menu, display collections, process paginated responses, and retry eligible failures. |
| Functions | Implement operations such as `createQuest()`, `completeQuest()`, `calculateLevel()`, and `syncNow()`. |
| Classes | Create services such as `QuestService`, `ProgressionService`, and `SyncCoordinator`. |
| Collections stretch goal | Create and modify mutable lists/maps for quest results, achievement evaluations, and staged sync changes. |
| Additional: data classes | Represent `Quest`, `PlayerProfile`, `RewardEntry`, and sync results with data classes. |
| Additional: varied `when` uses | Use `when` for menu dispatch, enum-based XP calculations, schedule variants, and API error handling. |

Declare **collections** as the selected stretch goal, and document data classes and varied `when` expressions as additional demonstrations. SQLite persistence does not replace the requirement to demonstrate collection creation and modification in Kotlin.

References: [Kotlin collections](https://kotlinlang.org/docs/collections-overview.html), [data classes](https://kotlinlang.org/docs/data-classes.html), and [conditions and loops](https://kotlinlang.org/docs/control-flow.html).

## 3. Technology and project structure

| Area | Planned choice |
| --- | --- |
| Language/runtime | Kotlin/JVM, targeting JDK 21. |
| Build | Gradle Wrapper and Kotlin build scripts; pin compatible stable versions during setup. |
| Initial interface | Console menus with validated input and readable progress displays. |
| Persistence | SQLite through JDBC, with versioned SQL migrations and transactions. |
| Structured snapshots | `kotlinx.serialization` for JSON sync snapshots and export files. |
| Calendar integration | Google's Java Calendar API client and OAuth libraries, called from Kotlin. |
| Background work | Kotlin coroutines with one serialized synchronization worker. |
| Time handling | `java.time` types and an injected `Clock`. |
| Verification | `kotlin.test` with JUnit 5, temporary SQLite databases, and a fake calendar adapter. |

The library adapter must support the desktop OAuth requirements described below, including PKCE. Check library support during the Calendar proof of concept before committing to a dependency version.

Proposed layout, to be created during implementation:

```text
LifeQuest/
  README.md
  build.gradle.kts
  settings.gradle.kts
  gradlew
  gradlew.bat
  gradle/wrapper/
  src/main/kotlin/lifequest/
    Main.kt
    ui/                 # Console menus, input parsing, presentation
    domain/             # Models, enums, progression rules
    service/            # Quest, progression, achievement, reporting services
    persistence/        # SQLite repositories and migrations
    calendar/           # OAuth and Google API adapter
    sync/               # Mapping, outbox, reconciliation, conflicts
  src/main/resources/db/migration/
  src/test/kotlin/lifequest/
```

Dependency direction: the console calls application services; services use domain models and repository/calendar interfaces. SQLite and Google implementations sit behind those interfaces. Domain rules must not depend on Google's event classes.

Store runtime data under `%LOCALAPPDATA%\LifeQuest` by default, with an optional `LIFEQUEST_DATA_DIR` override for tests and development. Keep the database, backups, tokens, and downloaded OAuth configuration outside version control.

## 4. Quest and progression rules

### Quest fields and validation

- Give each quest a permanent UUID. Titles are required and trimmed; notes are optional.
- Categories: `FITNESS`, `STUDY`, `WORK`, and `PERSONAL`.
- Difficulties and rewards: `EASY = 25`, `MEDIUM = 50`, `HARD = 100`, and `EPIC = 200` XP.
- Quest states: `ACTIVE`, `COMPLETED`, and `CANCELLED`. Archiving an unfinished quest sets it to `CANCELLED`; archived records remain available in history.
- Model schedules as distinct variants: unscheduled, timed, and all-day. A timed schedule requires a start, an end, and a time zone; end must be after start. All-day schedules use dates and an exclusive end date.
- Use `America/Denver` as the initial profile time zone, with a setting to change it. Store timed instants plus the IANA zone ID, and preserve all-day dates without converting them to UTC.
- Reject nonexistent local times during daylight-saving transitions; require an explicit offset choice for ambiguous times.
- An overdue quest remains active. Reaching an event's end time never marks a quest completed automatically.

### Rewards and history

- Calculate level as `1 + totalXp / 250` using integer division. Display progress toward the next 250-XP threshold.
- A first completion also grants one category-stat point: Fitness to Strength, Study to Focus, Work to Discipline, and Personal to Balance.
- Save the awarded XP and category in an immutable reward entry. Later edits to difficulty/category do not change an existing reward.
- Enforce a unique reward entry per quest UUID. Reopening and completing the same quest again cannot farm XP.
- Archiving or deleting a calendar event does not remove earned XP or erase completion history. Reward correction/reversal can be a later explicit feature.
- Apply quest completion and reward insertion in one database transaction, regardless of whether completion came from the console or Calendar.

Initial achievements: first completed quest, ten completed quests, five Study completions, and at least one completion in every category. Unlock each achievement once. Derive XP, statistics, and daily/weekly reports from the reward ledger; use the profile zone for report boundaries. Record Calendar-origin completion when LifeQuest first observes it, rather than guessing the user's actual completion time.

## 5. Persistent data model

| Record | Essential information |
| --- | --- |
| `PlayerProfile` | UUID, name, archetype, report time zone. |
| `Quest` | UUID, title, notes, category, difficulty, state, schedule, created/updated times, local revision, archival information. |
| `RewardEntry` | Quest UUID with a uniqueness constraint, XP awarded, credited category, first completion time. |
| `AchievementUnlock` | Profile UUID, achievement key, unlock time; unique per profile/key. |
| `CalendarConnection` | Local connection UUID, OAuth token-store reference, calendar ID, saved calendar time zone, sync token, last successful sync time. |
| `CalendarLink` | Quest UUID, connection/calendar/event IDs, latest remote ETag, last shared-field snapshot, last acknowledged local revision, deletion/unlink state, event creation generation. |
| `OutboxOperation` | Operation UUID, quest/connection IDs, intended action, local revision, stable event ID, retry state, next attempt time. |
| `SyncConflict` | Quest/link identity, baseline, local candidate, remote candidate, conflicting fields, resolution state. |
| `SyncIssue` | Durable warnings for unsupported or malformed remote events and actionable failures. |

Required database guarantees:

- Unique `(connectionId, calendarId, eventId)` for linked events; a quest has at most one active Calendar link.
- Save a local edit and its outbox operation in the same transaction.
- Keep tombstones for pending deletions until synchronization acknowledges them.
- Increment a local revision for each local change; acknowledge only the revision actually sent.
- Serialize database mutations or use explicit revision checks so a response cannot overwrite newer console edits.
- Run migrations before opening menus; provide a backup/export command and verify restoring into an isolated data directory.

## 6. Google Calendar setup and access

1. Create a Google Cloud project and enable the Google Calendar API.
2. Configure OAuth branding, audience, and data access. For a personal Gmail account, use an External testing audience and register the development account as a test user.
3. Create an OAuth client of type **Desktop app** and download its JSON configuration.
4. Implement authorization through the system browser, using a loopback redirect, PKCE, and state validation. Request offline access so the library can refresh access tokens.
5. Request `https://www.googleapis.com/auth/calendar.app.created`.
6. After authorization, create a secondary calendar named **LifeQuest** with the selected profile time zone, and persist its returned ID. Reuse that ID on later launches. If calendar creation has an uncertain outcome or the saved ID is lost, provide recovery using the calendar ID from Google Calendar settings before offering to create another calendar.
7. Tie tokens, links, cursors, and queued operations to the saved connection. Account changes require reconnecting explicitly; never send an old connection's queue to a different account/calendar.
8. Disconnecting stops synchronization and removes local authorization material while preserving local quests and existing Calendar events. If access is missing or the calendar is deleted, pause sync and show a reconnect/replacement action.

The selected scope permits creating secondary calendars and managing their events, which suits this dedicated-calendar design. The initial release does not import a user's primary calendar. See [Calendar scopes](https://developers.google.com/workspace/calendar/api/auth) and [creating a secondary calendar](https://developers.google.com/workspace/calendar/api/v3/reference/calendars/insert).

Keep refresh tokens in a user-restricted token store outside the repository; never log authorization headers or token contents. Use the desktop client libraries rather than collecting Google passwords. See [the Java quickstart](https://developers.google.com/workspace/calendar/api/quickstart/java) and [desktop OAuth requirements](https://developers.google.com/identity/protocols/oauth2/native-app).

External OAuth apps in Testing can receive refresh tokens that expire after seven days for these scopes. Treat expired or revoked authorization as a reconnect state without losing local work. See [Google's OAuth token-expiration guidance](https://developers.google.com/identity/protocols/oauth2/).

## 7. Two-way synchronization contract

### Timing and supported events

Synchronize on startup, on a manual **Sync now** command, shortly after a local change, and approximately every 60 seconds while the application is open and connected. Make the interval configurable and avoid overlapping runs. Show the last successful sync, pending-operation count, conflicts, and connection state.

A Calendar edit should normally appear by the next successful cycle. While LifeQuest is closed, Google retains the changes and LifeQuest imports them on the next launch. Always-on synchronization requires the optional hosted service described later.

Support ordinary, nonrecurring timed and all-day events in the dedicated LifeQuest calendar. Import events created there directly as quests, with `PERSONAL`, `MEDIUM`, and `ACTIVE` defaults unless valid LifeQuest metadata specifies otherwise. Use a visible fallback title for an untitled imported event.

Recurring series, recurring instances, and special Google event types are outside the first release. Record an understandable issue and leave them untouched. If an already linked event becomes recurring, suspend writes for that link until the user resolves it. Do not let an unsupported event block synchronization of other quests.

### Field mapping

| LifeQuest field | Google Calendar representation | Direction |
| --- | --- | --- |
| Title | `summary` | Both ways. |
| Notes | Human-written event-description text outside the managed block | Both ways; preserve surrounding text. |
| Timed schedule | `start.dateTime`, `end.dateTime`, and time zone | Both ways. |
| All-day schedule | `start.date`, `end.date` with exclusive end | Both ways. |
| Category, difficulty, quest state | Editable LifeQuest description block | Both ways. |
| Quest identity/version | Private extended properties such as `lifequestQuestId` and `lifequestSchemaVersion` | Application-managed. |
| Event identity/version | Calendar ID, event `id`, and `etag` | Stored locally for matching and concurrency. |
| XP, level, statistics, achievements | Local reward ledger and derived values | Local only. |

Google Calendar events have no LifeQuest completion checkbox. Allow Calendar-side completion through a small, documented description block:

```text
Practice lists, maps, and filtering exercises.

[LifeQuest]
status=ACTIVE
category=STUDY
difficulty=MEDIUM
[/LifeQuest]
```

Changing `status=ACTIVE` to `status=COMPLETED` in Google Calendar completes the quest at the next successful sync. Setting `status=CANCELLED` archives the quest and queues removal of its event. Parse enum values case-insensitively and validate every value. Missing metadata on an existing link preserves its last known RPG values; missing metadata on a new event uses the import defaults.

Reject an invalid or malformed metadata block as a unit: preserve the previous RPG fields, show an issue, and do not award XP from that block. Independent title/time changes can still sync. Preserve the original description and avoid rewriting a malformed block until it is corrected. Account for HTML line breaks and entities when parsing descriptions, and verify the block survives editing through Google Calendar's web interface.

Google's event `status` values describe event lifecycle, not quest completion. A Google `cancelled` event is handled as a deletion; `confirmed` does not mean a completed quest. The API's [event resource](https://developers.google.com/workspace/calendar/api/v3/reference/events) and [extended properties](https://developers.google.com/workspace/calendar/api/guides/extended-properties) document the underlying fields.

### Identity, imports, and safe writes

- Match known events by connection/calendar/event ID. Do not match by title or date.
- Store the quest UUID in private event metadata as a secondary identity check. An event copied in Calendar can carry copied metadata; it must become a distinct quest if its event ID differs from an existing link. Do not attach two events to one quest.
- When creating an event, persist a stable API-valid event ID and its pending Calendar link before sending the request. A UUID encoded as lowercase hexadecimal without hyphens, plus a persisted creation generation, fits the documented alphabet. Reuse the ID when retrying an uncertain insert; a pull must also recognize that pending link after an interrupted acknowledgment.
- If an insert reports an existing ID, retrieve and verify its identity before acknowledging success. A mismatched resource becomes a conflict.
- Patch only mapped fields; preserve unrelated attendees, locations, reminders, and other metadata. Do not generate invitations as part of quest synchronization.
- When switching between timed and all-day schedules, explicitly clear the mutually exclusive date/date-time fields in the outgoing representation.
- Normalize schedule and description values before comparing them. Receiving the app's own event update should update its baseline, not enqueue another equivalent write.

See the API's [event ID rules](https://developers.google.com/workspace/calendar/api/v3/reference/events/insert).

### Pull changes from Calendar

1. On first connection, list all supported events in the dedicated calendar. Follow every page and stage the results before applying them.
2. Process cancellations before accessing optional fields: deleted-event records can contain very little data.
3. Apply staged imports, updates, deletions, reward effects, conflicts, and issues in a transaction. Save `nextSyncToken` only after the final page and all corresponding local changes are durably recorded.
4. Subsequent pulls use the saved `syncToken` to retrieve changes. If interrupted before committing, repeat safely from the previous token.

This follows Google's [incremental synchronization model](https://developers.google.com/workspace/calendar/api/guides/sync).

Use a fixed, compatible query shape across full and incremental requests, with `singleEvents=false` and `showDeleted=true`. Do not combine incremental sync with incompatible filters such as `timeMin`, `timeMax`, `orderBy`, search text, or extended-property filtering. Filtering unsupported events happens locally. A new untagged event must remain discoverable. See [`events.list` restrictions](https://developers.google.com/workspace/calendar/api/v3/reference/events/list).

On a token-invalid `410`, discard the cursor and rebuild the remote event cache with a full pull. Preserve local quests, reward history, identity links, queued edits, and the baselines needed to compare pending work. Reconcile missing linked events only after a complete, successful scan of the same authorized calendar; a failed or partial scan is not evidence of deletion.

### Merge and push local changes

For each shared field, compare three values: the last synchronized baseline, the current local value, and the latest remote value.

| Changes since the baseline | Required result |
| --- | --- |
| Local only | Queue/push the local value. |
| Remote only | Apply the remote value locally. |
| Both changed to the same value | Accept it without a conflict or redundant write. |
| Different fields changed on each side | Merge the independent changes. |
| Same field changed differently | Save a conflict and pause writes for that quest. |
| A deletion competes with an edit | Save a conflict; preserve both candidates. |

Treat the complete schedule as one field so independently changed start/end values cannot form an invalid event. Treat notes and managed RPG fields separately after parsing the description. Do not use computer timestamps alone to decide which side wins.

One `SyncCoordinator` should perform a pull/merge first, then drain eligible outbox operations, then pull again to catch changes made during writes. Use `If-Match` with the latest event ETag for edits and deletes. A `412` requires refetching and merging, rather than overwriting blindly. See [conditional resource modifications](https://developers.google.com/workspace/calendar/api/guides/version-resources).

Persist the acknowledged baseline and outbox result after each successful write. Update only the local revision that was sent; if the user edits during an API request, keep the newer revision pending. Baselines must reflect remote values actually observed or acknowledged, never unsent local guesses. Network calls should not hold an open SQLite transaction.

Conflict review must display local and Calendar values and offer **Keep LifeQuest**, **Keep Calendar**, or a field-by-field resolution. Do not grant a new reward from an incoming completion while that quest has an unresolved conflict; rewards already earned through an explicit local completion remain recorded. Unrelated quests continue syncing, and the cursor may advance once the unresolved conflict and both candidates are stored durably.

### Deletion and unscheduling behavior

| User action | Planned behavior |
| --- | --- |
| Delete an event in Google Calendar | Archive/cancel the linked quest when no local change conflicts; retain earned rewards and history. |
| Archive a scheduled quest in LifeQuest | Record a local tombstone and queue removal of its linked event. |
| Unschedule a quest in LifeQuest | Remove its event, retain the active local quest, and record the intentional unlink so the deletion echo does not cancel it. |
| Restore a cancelled quest | Restore explicitly; if scheduling again, use a new event creation generation. Prior rewards remain awarded once. |
| Delete the entire LifeQuest calendar or lose access | Pause that connection; do not mass-archive quests or create a replacement calendar silently. |

Never recreate a remotely deleted event automatically. Tombstones and explicit operation intent distinguish a user deletion from an acknowledged local removal.

### Offline operation and failures

Every local operation saves immediately and adds/coalesces its pending sync intent. Use a single-instance database lock for the first release so two console processes cannot independently drain the same queue.

| Failure | Response |
| --- | --- |
| Offline, timeout, retryable server failure | Retain the queue; retry with bounded exponential backoff and jitter. |
| Rate limiting (`429` or quota-related `403`) | Back off; honor `Retry-After` when supplied. |
| Expired access token | Let the OAuth library refresh it and retry within a bounded limit. |
| Revoked/expired refresh authorization | Pause sync and offer reconnect; keep local work. |
| Permission-related `403` or calendar-level `404` | Pause and show the access problem; do not classify it as an event deletion. |
| Invalid sync-token `410` | Rebuild remote state as described above. |
| Already-removed event on delete | Acknowledge the idempotent removal after verifying the target context. |
| ETag mismatch `412` | Refetch, merge, or create a conflict. |
| Invalid request/unsupported event | Record an issue for the affected item; continue other work when safe. |

Inspect the endpoint and error reason, not just the HTTP code. Show actionable messages without exposing credentials. Background work must not interrupt partially typed console input; show notifications at the next safe menu refresh. See [Calendar API error handling](https://developers.google.com/workspace/calendar/api/guides/errors).

## 8. Console interface

```text
LifeQuest | Level 3 | 575 XP | Focus +4
Calendar: Connected | Last sync: 10:42 | Pending: 1 | Conflicts: 0

1. Dashboard and agenda
2. Create quest
3. Browse, search, and filter quests
4. Edit, complete, reopen, or archive a quest
5. Character and achievements
6. Progress reports and history
7. Calendar connection and Sync now
8. Resolve sync conflicts and issues
9. Settings and backup/export
0. Save and exit
```

Use stable quest identifiers when selecting records; never identify a quest by an unsorted list position that can change during background synchronization. Provide clear cancel/back actions and validation for dates, enum choices, and malformed input.

## 9. Implementation milestones

Implement milestones in order. Add focused verification with each feature rather than postponing all testing to the last milestone.

### Milestone 1: Runnable Kotlin foundation

- [ ] Create the Gradle project and wrapper, configure the JDK toolchain, and pin dependencies.
- [ ] Add domain enums, schedule variants, and data classes.
- [ ] Implement a minimal menu and an in-memory quest collection that can be created, modified, and displayed.
- [ ] Add initial validation and progression tests using an injected clock.

**Exit condition:** The application runs locally and visibly demonstrates the five core Kotlin requirements and collection modification.

### Milestone 2: Persistent quests and RPG progression

- [ ] Add SQLite schema migrations and repository interfaces/implementations.
- [ ] Implement player creation and quest CRUD with automatic persistence.
- [ ] Add transactional, once-per-quest reward recording and level/stat calculations.
- [ ] Create the Calendar connection/link, outbox, conflict, and issue storage needed by later milestones.
- [ ] Add backup/export and protect runtime files with appropriate ignore rules.

**Exit condition:** Quests and character progress survive restarting; repeating completion does not duplicate XP.

### Milestone 3: Calendar proof of concept

- [ ] Set up the Cloud project and desktop OAuth client.
- [ ] Verify browser authorization, loopback callback, PKCE, token persistence, and refresh handling.
- [ ] Create and persist the dedicated LifeQuest calendar using the selected scope.
- [ ] Send one test quest, edit its title/time in Calendar, and read those edits back through the adapter.
- [ ] Verify that an event created manually in that calendar is readable.
- [ ] Confirm timed/all-day mapping and description-block parsing before building the full sync worker.

**Exit condition:** One real round trip works with the planned permissions and libraries. Resolve integration assumptions here before adding optional game features.

### Milestone 4: Required two-way synchronization

- [ ] Implement full and incremental paginated pulls with durable cursor commits.
- [ ] Import new Calendar events and update existing quests through shared application services.
- [ ] Implement the durable outbox, stable insert IDs, conditional patches/deletes, and revision acknowledgments.
- [ ] Add three-way merging, conflict persistence, and the conflict-resolution menu.
- [ ] Handle remote deletion, local archive, unscheduling, and explicit restore.
- [ ] Add startup/manual/periodic sync, offline queue replay, error states, and token reset recovery.
- [ ] Prove that replaying remote changes cannot duplicate quests, rewards, or outbound writes.

**Exit condition:** Create, edit, reschedule, complete, and delete from either supported interface and see the specified result in the other after synchronization.

### Milestone 5: Complete the LifeQuest experience

- [ ] Finish the dashboard, agenda, search, sorting, category/status filters, and quest details.
- [ ] Add the four initial achievements and daily/weekly reports.
- [ ] Show meaningful sync status, errors, pending work, and conflict counts.
- [ ] Add sample local quests for an optional demo mode that does not touch Google Calendar.
- [ ] Refine console navigation and input validation.

**Exit condition:** A user can manage a full quest-to-reward cycle and understand both character progress and Calendar sync state.

### Milestone 6: Verification and course handoff

- [ ] Run the acceptance scenarios below and fix any failures.
- [ ] Verify build/test commands on a fresh checkout and an empty data directory.
- [ ] Add actual setup, run, OAuth configuration, backup, and troubleshooting instructions to this README.
- [ ] Document where every Kotlin requirement appears in the completed source.
- [ ] Add screenshots or a console walkthrough and any instructor-required demonstration link.
- [ ] Confirm that credentials, tokens, databases, and personal Calendar data are not tracked by Git.

**Exit condition:** The implemented release is reproducible, documented, and ready to demonstrate.

## 10. Verification and definition of done

Automate domain and synchronization tests using a fake Calendar adapter, deterministic IDs/clock, and temporary SQLite databases. Use real Google Calendar only for explicitly configured integration checks in the dedicated development calendar.

| Scenario | Required result |
| --- | --- |
| Create/edit a local scheduled quest | One linked event is created or updated, with matching shared fields. |
| Edit event title, notes, or time in Calendar | The same local quest receives those edits. |
| Create an event directly in the LifeQuest calendar | One local quest is imported with defaults or valid metadata. |
| Complete through the Calendar description block | State, XP, statistics, and achievements update exactly once. |
| Reopen/recomplete or replay the same response | No extra reward for the same quest UUID. |
| Edit different fields on both sides while offline | Nonconflicting values merge. |
| Edit the same field differently on both sides | Both candidates remain available until resolved. |
| Race a remote edit against a local write | ETag protection prevents silent overwrite. |
| Delete in Calendar | The quest is archived without losing earned rewards or being recreated. |
| Archive or unschedule locally | The event is removed and the intended local quest state is preserved. |
| Disconnect/reconnect or restart offline | Saved data and pending work survive without targeting a different connection. |
| Crash after a successful insert but before acknowledgment | Retrying recovers the same event rather than duplicating it. |
| Fail on page two of a pull | The saved cursor does not skip unapplied changes. |
| Invalidate the sync token | A full rebuild preserves pending edits, rewards, and conflict detection. |
| Sync timed/all-day events across a DST boundary | Instants, zones, and exclusive end dates remain correct. |
| Copy an event or remove/mangle its metadata | No identity collision, accidental reward, or unrelated data overwrite. |
| Encounter recurrence or lose calendar access | A visible issue/pause occurs without destroying unrelated quests. |
| Restore a backup into an isolated data directory | Quests, history, and sync bookkeeping load consistently. |

After implementation, the expected local verification commands from this folder are:

```powershell
.\gradlew.bat test
.\gradlew.bat run --console=plain
```

These commands require the Gradle project from Milestone 1; they are not available in the planning-only project yet.

The release is complete when all required product features and acceptance scenarios pass, Calendar edits are reflected in LifeQuest, local edits reach Calendar, and all Kotlin course requirements can be demonstrated from the source.

## 11. Optional extensions after the required release

- **Graphical desktop client:** Reuse the application services in a Kotlin desktop UI with a quest board, character panel, and agenda.
- **Habit/recurring quests:** Design a separate quest-instance identity and per-occurrence reward model before enabling Google recurring events.
- **RPG expansion:** Add equipment, cosmetic rewards, character abilities, and longer quest chains.
- **Health integration:** Add workout records or personal wellness goals inspired by the previous weight-loss website.
- **Always-on sync:** Run synchronization in a hosted Kotlin service if updates must be processed while the local application is closed.

For the hosted option, Google Calendar push notifications require a publicly reachable HTTPS receiver. Notifications signal that something changed; the service still fetches changes through incremental sync. Persist and renew expiring watch channels, validate channel tokens/resource identity, and retain periodic reconciliation for missed notifications. This adds hosting, secure credential storage, and coordination between the service and the local client. See [Google Calendar push notifications](https://developers.google.com/workspace/calendar/api/guides/push).
