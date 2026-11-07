# Automated Screenshot Cleaner & High-Density Power-User Sweeper

A unified background cleaner combining APK downloads and aged screenshot purging with exact time-of-day execution schedules, a screenshot expiration leaderboard, and a high-density, thin-bordered power-user interface.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural direction has been aligned based on user preferences:
> - **Unified Schedule with Independent Cleaner Toggles**: A single periodic WorkManager scheduler triggers background runs, with granular toggles for APK cleaner and Screenshot cleaner.
> - **Automatic MediaStore Scanning**: Automatically queries system screenshot folders (`DCIM/Screenshots` and `Pictures/Screenshots`) via Android MediaStore.
> - **Segmented Power-User Layout**: High-density 3-tab layout (Overview, Screenshots Leaderboard, Schedule & Rules) replacing bloated low-density cards.
> - **Crisp 1.dp Borders & Compact Spacing**: Replacing thick padding and borderless blocks with 1.dp hairline borders, compact 8–10.dp padding, and monospace metric badges.

---

## 1. Overview & Core Concept

### What It Does
1. **Automated Screenshot Retention Cleaner**: Users configure retention days (e.g., keep screenshots for 1 day, 3 days, 7 days, 14 days, or custom). In background schedules or on demand, any screenshot older than the retention threshold is automatically purged.
2. **Exact Time-of-Day Interval Scheduling**: Interval schedules (e.g. Every 1 day, Every 3 days) now calculate initial delay so they execute precisely at the user-specified hour and minute (e.g., 03:30 AM every night).
3. **Expiring Screenshots "Leaderboard" (Ranked Queue)**: Displays upcoming/expired screenshots ranked by age (#1 oldest first), showing thumbnails, capture timestamps, file sizes, and time-until-deletion countdown tags.
4. **Power-User High-Density UI**: Dense information architecture with compact stat chips, 1.dp subtle borders, concise lists, and tabbed organization to minimize scrolling and eliminate dead space.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Aesthetic Direction**: Modern power-user utility (inspired by terminal monitors and developer dashboards).
- **Border Treatment**: Explicit 1.dp hairline borders (`color = outlineVariant.copy(alpha = 0.6f)`) on cards, chips, and table headers. No thick 2–3.dp borders and no borderless solid slabs.
- **Padding & Density**: Tight, deliberate padding (8.dp to 12.dp internal card padding, 6.dp to 8.dp item gaps). High information density with tabular metrics.
- **Typography & Accent**: JetBrains Mono / tabular numbers for file sizes, timestamps, and rank badges. Clean body text with high contrast.

```
┌────────────────────────────────────────────────────────┐
│ [APK Sweeper Pro]  ● Auto-Clean Active   03:00 Daily  ⚙ │
├────────────────────────────────────────────────────────┤
│ [ Tabs: [Overview]   [Screenshot Board]   [Rules] ]    │
├────────────────────────────────────────────────────────┤
│ METRICS BAR (Compact 3-column with 1.dp borders)        │
│ ┌──────────────┬──────────────────┬──────────────────┐ │
│ │ APKs: 4 (120MB)│ Expiring Shots: 12│ Next: Today 03:00│ │
│ └──────────────┴──────────────────┴──────────────────┘ │
│                                                        │
│ SCREENSHOTS PURGE LEADERBOARD (Oldest First)           │
│ ┌────────────────────────────────────────────────────┐ │
│ │ #1 [Thumb] Screenshot_20261020.png   2.4 MB        │ │
│ │    Captured 18d ago • EXPIRED (-17d)  [Purge] [Keep]│ │
│ ├────────────────────────────────────────────────────┤ │
│ │ #2 [Thumb] Screenshot_20261101.png   1.8 MB        │ │
│ │    Captured 5d ago  • EXPIRED (-4d)   [Purge] [Keep]│ │
│ ├────────────────────────────────────────────────────┤ │
│ │ #3 [Thumb] Screenshot_20261105.png   3.1 MB        │ │
│ │    Captured 20h ago • 4h remaining    [Purge] [Keep]│ │
│ └────────────────────────────────────────────────────┘ │
│ [ Clean Expired (3.4 GB) ]   [ Run Full Maintenance ]  │
└────────────────────────────────────────────────────────┘
```

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: MediaStore Images Scanning vs. Broad File Tree Traversal**
  - *Chosen Approach*: Query `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` filtering for screenshot relative paths (`Pictures/Screenshots` & `DCIM/Screenshots`).
  - *Why*: Fast indexing, instant thumbnail generation via ContentResolver/Coil, respects Android 11+ Scoped Storage without demanding intrusive full-disk permissions.
- **Decision 2: Exact Time Delay Calculation for WorkManager Interval**
  - *Chosen Approach*: Compute `initialDelayMillis` by finding the next clock occurrence of `targetHour:targetMinute`, then periodic repeat interval.
  - *Why*: Allows users to specify exact execution moments (e.g. 03:30 AM) while maintaining periodic power-efficient execution.
- **Decision 3: Independent Cleaner Selection**
  - *Chosen Approach*: `ScheduleConfig` holds `cleanApksEnabled` (Boolean), `cleanScreenshotsEnabled` (Boolean), and `screenshotMaxAgeDays` (Int, default 1).
  - *Why*: Gives complete control to users who want only APK cleaning, only screenshot cleaning, or both.

---

## 4. Technical Architecture & Data Strategy

### System Component Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI                     │
│  ┌───────────────┐  ┌──────────────────────┐  ┌─────────────┐ │
│  │ OverviewTab   │  │ ScreenshotBoardTab   │  │ RulesTab    │ │
│  └───────┬───────┘  └──────────┬───────────┘  └──────┬──────┘ │
│          └─────────────────────┼─────────────────────┘        │
│                                ▼                              │
│                         MainViewModel                         │
│            ┌───────────────────┴───────────────────┐          │
│            ▼                                       ▼          │
│   PreferencesManager                       Scanner Repositories│
│  (DataStore: schedule,              ┌──────────────┴────────┐ │
│   retention days, stats)            ▼                       ▼ │
│                                ApkScanner           ScreenshotScanner
│                                (File/SAF)           (MediaStore API)
└────────────────────────────────┼───────────────────────┼──────┘
                                 ▼                       ▼
                    ┌──────────────────────────────────────────┐
                    │ WorkManager: UnifiedCleanupWorker        │
                    │ - Triggers at exact targetHour:targetMin │
                    │ - Executes enabled cleaners              │
                    │ - Dispatches consolidated notification   │
                    └──────────────────────────────────────────┘
```

### Data Models & Preferences
1. **`ScreenshotFileInfo`**:
   - `id`: Long (MediaStore ID)
   - `displayName`: String
   - `contentUri`: Uri
   - `sizeBytes`: Long
   - `dateTaken`: Long
   - `ageDays`: Double
   - `isExpired`: Boolean
   - `remainingMillis`: Long
2. **`ScheduleConfig` Updates**:
   - `cleanApksEnabled`: Boolean (default true)
   - `cleanScreenshotsEnabled`: Boolean (default true)
   - `screenshotMaxAgeDays`: Int (default 1)
   - `targetHour`: Int (0..23)
   - `targetMinute`: Int (0..59)
   - `mode`: Interval / DayOfWeek with exact initial delay alignment
3. **`CleanupHistoryItem` Updates**:
   - `apksDeleted`: Int
   - `screenshotsDeleted`: Int
   - `bytesReclaimed`: Long
   - `timestamp`: Long
