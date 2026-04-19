# Seachem Dosing and Water Tester and Alert 

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen)](https://android-arsenal.com/api?level=24)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-757575?logo=material-design&logoColor=white)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A dual-platform application (Android & Web) for aquarium enthusiasts to calculate precise dosing amounts for Seachem supplements based on water volume and parameters.

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" alt="App Icon" width="120"/>
</p>

---

## Features

### Dosing Calculators
- **13+ Products Supported** - Flourish series, Reef series, buffers, conditioners
- **3 Aquarium Profiles** - Freshwater/Planted, Saltwater/Reef, Sand & Gravel
- **Real-time Calculations** - Instant results as you type (debounced for performance)
- **Smart Recommendations** - Automatic dosing suggestions based on readings

### Water Parameters
- Track 13+ parameters per profile (Ammonia, Nitrite, Nitrate, pH, GH, KH, etc.)
- Visual status indicators (Good/Warning/Danger)
- Profile-specific parameter sets

### User Experience
- Light/Dark theme with system-aware switching
- Multi-language support (English + Kannada)
- Unit conversions (Volume, Hardness, Temperature)
- Material Design 3 UI

---

## Screenshots

| Dashboard | Calculators | Settings |
|:---------:|:-----------:|:--------:|
| Water parameters & recommendations | Dosing cards | Theme & units |

---

## Tech Stack

### Android App
| | |
|---|---|
| **Language** | Kotlin 2.0.21 |
| **Min SDK** | 24 (Android 7.0) |
| **Architecture** | MVVM + LiveData |
| **UI** | Fragment Navigation + ViewBinding |
| **Design** | Material Design 3 |
| **Async** | Kotlin Coroutines |
| **Storage** | DataStore Preferences |

### Web App
| | |
|---|---|
| **Stack** | HTML5, CSS3, Vanilla JS (ES6+) |
| **Framework** | None (zero dependencies) |
| **Testing** | Jest + ESLint |

---

## Quick Start

### Android

```bash
# Clone
git clone https://github.com/GurucharanSavanth/Seachem-Calculatore.git
cd SeachemDosing

# Build & Install
./gradlew installDebug
```

### Web

```bash
cd Base_Template

# Option 1: Open directly
open index.html

# Option 2: Local server
python -m http.server 8000
```

---

## Project Structure

```
SeachemDosing/
├── app/src/main/java/com/example/seachem_dosing/
│   ├── MainActivity.kt
│   ├── logic/
│   │   ├── Calculations.kt          # Core math (SYNC POINT)
│   │   ├── SeachemCalculations.kt
│   │   └── SaltMixCalculations.kt
│   ├── ui/
│   │   ├── MainViewModel.kt         # State management
│   │   ├── dashboard/
│   │   ├── calculators/
│   │   ├── profile/
│   │   └── settings/
│   └── util/
│       └── DebouncedTextWatcher.kt  # Performance utility
│
├── Base_Template/
│   ├── index.html
│   └── js/
│       ├── dosingCalculations.js    # Core math (SYNC POINT)
│       ├── app.js
│       └── uiHandlers.js
│
└── docs/
    ├── MEMORY.md      # Project history
    └── AGENTS.md      # Dev guidelines
```

---

## Supported Products

<details>
<summary><b>Freshwater/Planted</b></summary>

- Flourish (comprehensive)
- Flourish Iron, Nitrogen, Phosphorus, Potassium, Trace
- Equilibrium (GH booster)
- Alkaline Buffer, Acid Buffer
- Neutral Regulator
- Potassium Bicarbonate (KHCO₃)

</details>

<details>
<summary><b>Saltwater/Reef</b></summary>

- Reef Advantage Calcium, Magnesium, Strontium
- Reef Buffer, Builder, Carbonate
- Reef Calcium, Complete
- Reef Fusion 1 & 2
- Reef Iodide, Strontium
- Salt Mix Calculator

</details>

<details>
<summary><b>Universal</b></summary>

- Prime (dechlorinator)
- Stability (beneficial bacteria)
- Safe (concentrated conditioner)
- Water Change Calculator

</details>

---

## Contributing

Contributions are welcome! Please read the guidelines:

### Before You Start
1. Check [Issues](https://github.com/GurucharanSavanth/Seachem-Calculatore/issues) for existing discussions
2. Read `AGENTS.md` for code style guidelines

### Important Rules

> **Calculation Sync Required**
> Changes to dosing math must update BOTH files:
> - `app/.../logic/Calculations.kt`
> - `Base_Template/js/dosingCalculations.js`

### Code Guidelines
- Use `DebouncedTextWatcher` for new text inputs (250ms default)
- Clean up resources in `onDestroyView()`
- Validate inputs at ViewModel level
- Follow existing code style

### Pull Request Process
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Install on device
./gradlew installDebug
```

---

## Acknowledgments

- [Seachem Laboratories](https://www.seachem.com/) - Product specifications
- [Bulk Reef Supply](https://www.bulkreefsupply.com/) - Reef chemistry guidance
- [Material Design 3](https://m3.material.io/) - Design system

---

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <b>Developed with ❤️ by <a href="https://github.com/GurucharanSavanth">Gurucharan.S</a></b>
</p>

<p align="center">
  <a href="https://github.com/GurucharanSavanth/Seachem-Calculatore/issues">Report Bug</a>
  ·
  <a href="https://github.com/GurucharanSavanth/Seachem-Calculatore/issues">Request Feature</a>
</p>
