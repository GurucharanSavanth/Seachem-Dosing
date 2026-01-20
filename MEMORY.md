# SeachemDosing Project - Memory & Context Document

**Last Updated:** 2026-01-20
**Project Status:** Active Development (Post Performance Optimization)
**Version:** 1.0.1 (Performance & Stability Release), v2.0 Roadmap Available

---

## Quick Reference

### Project Overview
**SeachemDosing** is a dual-platform aquarium supplement dosing calculator:
- **Android App:** Kotlin + Jetpack Compose/Fragment Navigation + Material Design 3
- **Web App:** HTML5/CSS3/Vanilla JavaScript ES6+ (no framework)
- **Shared Core:** Identical dosing math in both platforms (critical sync point)

### Key Statistics
- **Lines of Code:** ~4,000+ (Android), ~2,000+ (Web)
- **Supported Products:** 13 dosing calculators (KHCO₃, Equilibrium, Prime, Safe, etc.)
- **Profiles:** 3 (Freshwater, Saltwater, Pond)
- **Languages:** English + Kannada
- **Theme Support:** Light/Dark mode
- **Build System:** Gradle (Kotlin DSL)

---

## 1. ARCHITECTURE OVERVIEW

### Android Platform (`/app`)

**Tech Stack:**
- Language: Kotlin 2.0.21
- Min SDK: 24, Compile SDK: 34
- UI Framework: Fragment-based Navigation + ViewBinding
- State Management: LiveData + ViewModel (MVVM pattern)
- Material Design: Material Design 3 with dynamic colors

**Layered Structure:**
```
Presentation (4 Fragments + ViewModel)
├── DashboardFragment          (Water parameters, recommendations)
├── CalculatorsFragment        (13 dosing cards, batched init)
├── ProfileSelectionFragment   (Aquarium type selection)
├── SettingsFragment           (Theme, units, about)
└── MainViewModel              (Central state, 40+ LiveData properties, input validation)

Domain/Logic
├── Calculations.kt            (Pure math, edge case handling)
├── SeachemCalculations.kt     (Product-specific calculations)
└── SaltMixCalculations.kt     (Reef salt mixing)

Utilities
└── util/DebouncedTextWatcher.kt  (Debounced input, lifecycle-aware cleanup)

Data Layer
├── DataStore (preferences: theme, units, language)
└── (Future: Room DB + Retrofit for cloud)

AI Infrastructure (Currently Disabled)
├── AiModels.kt                (ChatMessage, ChatRole, AiInsightState)
├── GeminiClient.kt            (Stubbed Gemini integration)
└── Chat UI components         (item_chat_message.xml)
```

**Navigation Graph:**
- Entry: MainActivity
- Bottom nav: Calculators, Dashboard, Settings
- Toolbar action: Profile Selection
- Material Fade Through transitions

**Key ViewModel Properties (40+):**
- Volume settings (liters, gallons, dimensions)
- Water parameters (13 freshwater, 9 saltwater, 7 pond)
- Calculation results
- Profile selection
- Chat history & AI state
- UI state (errors, loading)

### Web Platform (`/Base_Template`)

**Tech Stack:**
- HTML5, CSS3, Vanilla JavaScript ES6+
- No framework dependencies
- Static hosting (no build system)
- i18n via translations.js

**Module Organization:**
```
index.html (Single Page App)
│
├── dosingCalculations.js  (13 calculation functions)
├── app.js                 (Main orchestration: doDosingCalculations())
├── uiHandlers.js          (DOM events, result display, parameter status)
├── utils.js               (DOM utilities, conversions, validation)
├── translations.js        (Multi-language strings)
└── styles.css             (27.5KB, responsive, dark mode)
```

**Features:**
- Responsive grid layout
- Dark mode via CSS media query
- Parameter status color coding (red/yellow/green)
- Water change percentage calculator
- Chat interface placeholder (disabled)

---

## 2. SHARED DOSING CALCULATIONS (CRITICAL SYNC POINT)

**Files that MUST stay synchronized:**
- Android: `app/src/main/java/com/example/seachem_dosing/logic/Calculations.kt`
- Web: `Base_Template/js/dosingCalculations.js`

**Coefficients (Must Match Exactly):**
```kotlin
COEFF_KHCO3_STOICH = 0.0357           // g/L per dKH
COEFF_EQUILIBRIUM = 0.0667            // g/L per dGH
COEFF_ACID = ~0.01339                 // Lowers KH/pH
COEFF_GOLD_FULL = 0.15                // pH up (full dose)
COEFF_NEUTRAL = varies by target pH
```

**13 Calculation Functions:**
1. `calculateKhco3Grams(dKh, volumeL)` - Stoichiometric KHCO₃
2. `calculateEquilibriumGrams(dGh, volumeL)` - GH booster
3. `calculateNeutralRegulatorGrams(...)` - pH down with KH factor
4. `calculateAcidBufferGrams(...)` - Lowers KH/pH
5. `calculateGoldBufferGrams(..., halfDose)` - pH up
6. `calculateSafeGrams(ammonia_or_nitrite, volumeL)` - Emergency detox
7. `calculatePrimeDose(volumeL)` - Dechlorinator (mL)
8. `calculateStabilityDose(volumeL)` - Bacteria (mL)
9. `calculateAptCompleteDose(...)` - Macro nutrients
10. `convertUnits(value, from, to)` - Volume conversions
11. `convertHardness(value, from, to)` - ppm ↔ dGH
12. `calculateVolumeFromDimensions(...)` - L × W × H

**Unit Conversions:**
- Volume: Liters, US Gallons, UK Gallons, Cubic cm/inches/feet
- Hardness: ppm, dGH (°GH)
- Temperature: Celsius, Fahrenheit

---

## 3. RECENT PERFORMANCE UPDATE (2026-01-20)

**Title:** "Performance & Stability Optimization"
**Focus:** Frame drop fixes, memory leak prevention, input validation

### Changes Made:

1. **New Utility: DebouncedTextWatcher** (`util/DebouncedTextWatcher.kt`)
   - `DebouncedTextWatcher` - Delays callback by configurable ms (default 250ms)
   - `DebouncedStringTextWatcher` - Returns raw string instead of Double
   - `TextWatcherManager` - Tracks and cleans up multiple watchers
   - Lifecycle-aware with automatic cleanup on `onDestroy`

2. **Fragment Performance Fixes**
   - `DashboardFragment`: Added TextWatcherManager, debounced all inputs
   - `CalculatorsFragment`: Batched initialization with `setupCardBatch()` + `yield()`
   - Both fragments now properly clean up in `onDestroyView()`

3. **MainViewModel Input Validation**
   - `coerceNonNegative()` - Prevents negative parameter values
   - `coercePh()` - Clamps pH to 0-14 range
   - `coerceTemperature()` - Clamps to -5°C to 50°C
   - `coercePercentage()` - Clamps to 0-100%
   - `coerceSalinity()` - Clamps to 0-50 PPT
   - Changed `_calcInputs` to `ConcurrentHashMap` for thread safety

4. **Calculation Edge Case Fixes**
   - `Calculations.kt`: Added `MIN_PURITY` constant (0.01), volume <= 0 checks
   - `SeachemCalculations.kt`: Input validation for `calculateGravel()`
   - `SaltMixCalculations.kt`: NaN/Infinity checks, salinity range validation
   - All calculation functions now return 0 for invalid inputs

5. **UI/UX Improvements**
   - Safe `GradientDrawable` cast with fallback to `backgroundTintList`
   - Replaced hardcoded strings with string resources
   - Fixed null safety in `SettingsFragment` reset dialog

### Performance Impact:
| Before | After |
|--------|-------|
| 32-85 frames skipped on init | Batched init with yields |
| Rapid-fire calculations | 250ms debounced input |
| TextWatcher memory leaks | Lifecycle-aware cleanup |
| No input validation | Full range validation |

---

## 4. PREVIOUS MAJOR UPDATE (Commit: 9c632a7)

**Title:** "Core Feature Update"
**Changes:** +3751 lines, 74 files modified

### Android Changes:
1. **Multi-Profile System**
   - Added `ProfileType` enum: FRESHWATER, SALTWATER, POND
   - Profile-specific parameter defaults
   - ProfileSelectionFragment for switching

2. **Dashboard Redesign** (+735 lines)
   - Parameter grid layout (responsive)
   - Status indicators for each parameter
   - Real-time recommendations
   - Gradient backgrounds (light & dark variants)

3. **Calculator UI Enhancement** (+272 lines)
   - Card-based layout for 13 products
   - Material Design 3 styling
   - Collapsible/expandable cards
   - Input validation with error display

4. **ViewModel Expansion** (+273 lines)
   - Now manages: profiles, parameters, calculations, chat, AI state
   - Sync methods: `syncGhFromParams()`, `syncKhFromParams()`, `syncPhFromParams()`
   - LiveData observers for reactive updates

5. **Material Design 3 Implementation**
   - Dynamic color system using MaterialColors
   - Material Fade Through transitions
   - 50+ new drawable resources (night mode variants)
   - Updated layout XML files (18 total)

6. **AI Infrastructure Added** (Currently Disabled)
   - AiModels.kt with data classes: ChatMessage, ChatRole, AiInsightState
   - GeminiClient stub (returns "AI disabled for now.")
   - Chat message list management
   - item_chat_message.xml layout

7. **Additional Calculators**
   - Prime dose calculator
   - Stability dose calculator
   - Water change percentage calculator

### Web Application Changes:
- Minor updates to calculation functions
- Improved UI handlers for parameter status
- Chat interface skeleton added

---

## 4. KEY TECHNOLOGIES & DEPENDENCIES

### Android Dependencies

**Core Jetpack:**
- `androidx.lifecycle:lifecycle-viewmodel-ktx` (2.7.0) - State management
- `androidx.lifecycle:lifecycle-livedata-ktx` (2.7.0) - Observable streams
- `androidx.navigation:navigation-fragment-ktx` (2.7.7) - Fragment navigation
- `androidx.core:core-ktx` (1.10.1) - Extension functions
- `androidx.appcompat:appcompat` (1.6.1) - Material components
- `androidx.constraintlayout:constraintlayout` (2.1.4) - Layout engine
- `androidx.datastore:datastore-preferences` (1.0.0) - Config storage

**Material Design:**
- `com.google.android.material:material` (latest) - Material 3 components

**Testing:**
- `junit:junit` (4.13.2) - Unit tests
- `androidx.test.ext:junit` (1.1.5) - Android JUnit runner
- `androidx.test.espresso:espresso-core` (3.5.1) - UI tests

**Build:**
- Gradle 8.13.2 with Kotlin DSL
- ProGuard/R8 enabled for minification
- Kotlin Coroutines (implied via lifecycle)

### Web Dependencies
- **None** - Vanilla JavaScript ES6+

---

## 5. CURRENT DEVELOPMENT STATE

### ✅ Fully Implemented
- Core dosing calculations (both platforms)
- 13 product calculators
- Multi-profile system (Freshwater, Saltwater, Pond)
- Water parameter tracking (13+ parameters per profile)
- Light/Dark theme toggle
- Unit conversions (volume, hardness, temperature)
- Fragment-based navigation (Android)
- Material Design 3 UI (Android)
- Responsive web layout
- i18n support (English + Kannada)

### 🔄 Partially Implemented
- AI integration (infrastructure exists, disabled)
- Chat interface (UI exists, no backend)

### ❌ Not Yet Implemented
- Cloud synchronization
- Multi-device sync
- Computer Vision (water test strip detection)
- IoT integration (automated dosing pumps)
- Rule-based recommendations engine
- Push notifications
- Offline data persistence (beyond preferences)
- Unit tests (empty test directories)
- Instrumentation tests

### ⚠️ Known Issues
- Debug keystore used for release builds (needs production keystore before shipping)
- No test coverage
- AI/Chat features disabled pending backend integration
- Shallow git history (only 4 commits)

---

## 6. FILE LOCATIONS REFERENCE

### Android Key Files
```
MainActivity.kt                         - Entry point, navigation setup
logic/Calculations.kt                   - ALL dosing math (CRITICAL SYNC POINT)
ui/MainViewModel.kt                     - Central state (40+ LiveData properties)
ui/dashboard/DashboardFragment.kt       - Parameter input & status display
ui/calculators/CalculatorsFragment.kt   - Dosing card layout
ui/profile/ProfileSelectionFragment.kt  - Profile type switching
ui/settings/SettingsFragment.kt         - Theme, units, language
ai/AiModels.kt                          - Chat data classes
ai/GeminiClient.kt                      - Gemini API stub
res/layout/                             - 18+ XML layout files
res/values/strings.xml                  - Text strings (localizable)
res/drawable/                           - Light theme assets
res/drawable-night/                     - Dark theme assets
```

### Web Key Files
```
Base_Template/index.html                - Single page app entry point
Base_Template/js/dosingCalculations.js  - ALL dosing math (CRITICAL SYNC POINT)
Base_Template/js/app.js                 - Main orchestration (doDosingCalculations())
Base_Template/js/uiHandlers.js          - DOM events, result display
Base_Template/js/utils.js               - Utility functions, conversions
Base_Template/js/translations.js        - i18n strings
Base_Template/css/styles.css            - Styling (27.5KB)
```

### Documentation Files
```
GEMINI.md                               - Project overview (tech stacks, architecture)
AGENTS.md                               - Development guidelines & conventions
features and new archtectruall approch.txt  - v2.0 roadmap (Clean Arch, MVI, Next.js)
MEMORY.md                               - This file (for future context)
```

---

## 7. DEVELOPMENT GUIDELINES

From `AGENTS.md` - Key Rules to Follow:

### Calculation Changes
**CRITICAL:** When modifying dosing math, update BOTH platforms:
1. `app/src/main/java/com/example/seachem_dosing/logic/Calculations.kt`
2. `Base_Template/js/dosingCalculations.js`

Coefficients must match exactly. Use decimal precision consistently.

### Naming Conventions
- **Android Resources:** snake_case (e.g., `activity_main.xml`, `@+id/nav_host`)
- **Android Code:** PascalCase classes, camelCase functions/properties
- **Web JS:** camelCase files (e.g., `app.js`), UPPER_SNAKE_CASE constants
- **Web CSS:** kebab-case class names (e.g., `.calculator-card`)

### Code Style
- Kotlin: 4-space indentation, extension functions preferred
- JavaScript: Airbnb style guide recommended (no linter enforced)
- Keep code consistent with nearby code
- No formatter/linter enforced; follow Android Studio defaults

### Testing
- Android unit tests: `app/src/test/java/` (empty - needs population)
- Android UI tests: `app/src/androidTest/java/` (empty)
- Web tests: Not yet configured (Jest mentioned in GEMINI.md but not set up)

### Build Commands
```bash
# Android
./gradlew assembleDebug           # Build debug APK
./gradlew installDebug            # Deploy to device
./gradlew test                    # Run unit tests
./gradlew connectedDebugAndroidTest  # Run UI tests

# Web
# Open Base_Template/index.html directly in browser
# Or serve with: python3 -m http.server --directory Base_Template
```

---

## 8. FUTURE ROADMAP (v2.0)

### Android v2.0 Targets
**Architecture:** Migrate from MVVM → Clean Architecture + MVI pattern

**Structure:**
- `:app` (DI root, navigation)
- `:core:designsystem` (Compose theme, typography)
- `:core:model` (Shared domain entities)
- `:feature:calculator`
- `:feature:dashboard`
- `:feature:journal`

**Tech Stack:**
- Hilt for dependency injection (replacing manual DI)
- Room database with FTS4 for historical logs
- Retrofit + OkHttp for cloud API
- Kotlin Coroutines for async operations
- Proto DataStore for type-safe config

**Features:**
- Local data persistence
- Historical log browsing
- Export functionality

### Web v2.0 Targets
**Migration:** Vanilla JS → Next.js 14+ with TypeScript

**Stack:**
- Framework: Next.js 14+ (App Router)
- Language: TypeScript (strict mode)
- State: Zustand or TanStack Query
- Styling: Tailwind CSS + shadcn/UI (Radix Primitives)
- PWA: Workbox service workers + IndexedDB

**Improvements:**
- Type safety across codebase
- Component library (Radix-based)
- Stale-While-Revalidate caching
- Virtual scrolling for large lists (react-window)

### Backend Infrastructure (Supabase/Firebase)
**Auth:** OAuth 2.0 (Google/Apple Sign-in)
**Database:** PostgreSQL (relational) or Firestore
**Realtime:** WebSocket subscriptions for multi-device sync
**Tables/Collections:**
- `profiles` (tank_name, volume, dimensions)
- `measurements` (pH, GH, KH, NO₃, PO₄, etc.)
- `dosing_events` (product, amount, timestamp)

### Advanced Features (Phase 3)
1. **Computer Vision** - OpenCV water test strip detection
   - CameraX (Android) / MediaDevices API (Web)
   - CIELAB Delta-E color matching
   - Automatic parameter extraction

2. **IoT Integration** - Automated dosing
   - MQTT over WebSocket
   - ESP32-based peristaltic pump control
   - mTLS authentication

3. **Smart Recommendations** - Rule engine
   - Trend analysis (NO₃/PO₄ trajectories)
   - Schedule optimization (daily → alternate days)
   - Heuristic rules

### CI/CD Pipeline
**GitHub Actions:**
- Lint: Detekt (Kotlin) + ESLint (TS)
- Tests: JUnit 5 + Maestro (UI automation)
- Build: Gradle Build Cache + Vercel Build
- Deploy: Fastlane → Play Store, Vercel → Web

---

## 9. DEVELOPMENT WORKFLOW

### Starting a New Feature

1. **Read Documentation First**
   - Check AGENTS.md for guidelines
   - Review GEMINI.md for architecture
   - Check MEMORY.md (this file) for context

2. **Understand Current State**
   - If modifying calculations: Review both Calculations.kt AND dosingCalculations.js
   - If modifying UI: Check current MainViewModel structure
   - If adding feature: Verify no partial implementation exists

3. **Make Changes Consistently**
   - Android changes? Update both app and web if math-related
   - Fragment changes? Remember to update navigation graph
   - Calculations? Update BOTH platforms' files
   - UI strings? Check if localization needed (translations.js)

4. **Test Across Platforms**
   - Build Android APK: `./gradlew assembleDebug`
   - Test web: Open in multiple browsers
   - Verify calculations match between platforms
   - Check light/dark theme (if UI change)

5. **Commit with Clear Messages**
   - Format: `[scope] description` (e.g., "calc: Add APT Complete dosing")
   - Call out multi-platform changes explicitly
   - Include screenshots for UI changes
   - Reference calculation changes prominently

### Common Tasks

**To Add a New Calculator:**
1. Add calculation function to both `Calculations.kt` and `dosingCalculations.js`
2. Create card layout XML (Android) and card HTML (Web)
3. Add UI binding in ViewModel
4. Update calculator fragment/page
5. Test calculation accuracy

**To Change Theme:**
1. Update Material Design 3 colors in Android resources
2. Modify CSS variables in styles.css
3. Test dark mode toggle
4. Verify all components render correctly

**To Add Parameter:**
1. Add to profile-specific lists (Freshwater/Saltwater/Pond)
2. Update MainViewModel LiveData
3. Add to DashboardFragment/Dashboard UI
4. Update calculation sync methods if needed

---

## 10. KNOWN CONSTRAINTS & CONSIDERATIONS

### Performance
- Android: Target low-memory devices (API 24+)
- Web: Optimize for slow networks
- Calculations: Must use consistent decimal precision

### Security
- Local preferences only (no sensitive data)
- Ready for future: JWT auth, TLS pinning
- ProGuard/R8 enabled for production

### Compatibility
- Android: Min API 24 (Android 7.0)
- Web: ES6+ support required (no IE 11 support)
- Cross-platform: Calculation logic must be identical

### Accessibility
- Web: WCAG 2.2 AA compliance (color contrast, ARIA labels)
- Android: Material Design 3 built-in accessibility
- Dark mode improves accessibility for many users

---

## 11. USEFUL COMMANDS & SNIPPETS

### Android Development
```bash
# Clean and build
./gradlew clean assembleDebug

# Install and run
./gradlew installDebug
adb shell am start -n com.example.seachem_dosing/.MainActivity

# View logs
adb logcat | grep seachem_dosing

# Run specific test
./gradlew testDebugUnitTest --tests "*.CalculationsTest"
```

### Git Operations
```bash
# View recent changes
git log --oneline -10

# Check status
git status

# View changes in specific file
git diff app/src/main/java/com/example/seachem_dosing/logic/Calculations.kt
```

### Web Development
```bash
# Start simple HTTP server
cd Base_Template && python3 -m http.server 8000

# Or use live server (if available)
live-server Base_Template/
```

---

## 12. QUICK FACT SHEET

| Aspect | Android | Web |
|--------|---------|-----|
| **Language** | Kotlin 2.0.21 | Vanilla JS ES6+ |
| **UI Framework** | Fragments + Material Design 3 | Plain HTML/CSS |
| **State Management** | LiveData + ViewModel | Direct DOM manipulation |
| **Navigation** | Fragment Navigation Graph | Single page (inline) |
| **Themes** | Light/Dark (Material) | Light/Dark (CSS media query) |
| **Localization** | Android resources | translations.js |
| **Testing** | JUnit (empty) | Jest (empty) |
| **Build System** | Gradle | None (static files) |
| **Hosting** | APK/Play Store | Static hosting |
| **Build Time** | ~30-60s debug | N/A |

---

## 13. QUESTIONS TO ASK WHEN UNSURE

1. **Is it a calculation change?** → Update BOTH platforms
2. **Does it affect multiple fragments?** → Update MainViewModel
3. **Is it a UI change?** → Check light/dark theme support
4. **Does it need localization?** → Update translations.js & strings.xml
5. **Is it a new feature?** → Plan for v2.0 multi-module structure
6. **Does it break existing features?** → Add unit tests

---

**END OF MEMORY DOCUMENT**

*This document should be updated whenever:*
- *Major features are added*
- *Architecture changes*
- *Build system updates*
- *New dependencies added*
- *Development guidelines change*
