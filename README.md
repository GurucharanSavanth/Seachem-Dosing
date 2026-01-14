# Seachem Dosing Calculator

A dual-platform application (Android & Web) designed to help aquarium enthusiasts calculate precise dosing amounts for Seachem supplements based on water volume and desired parameters.

## 📋 Overview

This project implements a dosing calculator with a shared core logic for two platforms:
1.  **Native Android App**: Built with modern Android development standards (Kotlin, Jetpack Compose, MVVM).
2.  **Web Application**: A lightweight, responsive static web app (HTML/CSS/Vanilla JS).

Both platforms utilize identical dosing coefficients and algorithms to ensure consistency.

## ✨ Features

*   **Volume Calculation**: Calculate tank volume based on dimensions.
*   **Dosing Calculations**: Determine exact amounts for various Seachem products.
*   **Parameter Tracking**: Input and track key water parameters (Ammonia, Nitrite, Nitrate, GH, KH).
*   **Dual Platform**: Access the tool via a native mobile experience or any web browser.

## 🛠 Tech Stack

### Android Application (`app/`)
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose (Material Design 3)
*   **Architecture**: MVVM (Model-View-ViewModel) with Unidirectional Data Flow.
*   **Dependency Injection**: Hilt
*   **Navigation**: Jetpack Compose Navigation
*   **Build System**: Gradle (Kotlin DSL)

### Web Application (`Base_Template/`)
*   **Core**: HTML5, CSS3, JavaScript (ES6+)
*   **Testing**: Jest
*   **Linting**: ESLint
*   **Styling**: Custom CSS

## 🚀 Getting Started

### Prerequisites
*   **Android**: Android Studio Koala or newer, JDK 17+.
*   **Web**: Node.js (v18+) and npm.

### Android Setup
1.  Clone the repository.
2.  Open the project in Android Studio.
3.  Sync Gradle files.
4.  Run the app on an emulator or connected device:
    ```bash
    ./gradlew installDebug
    ```

### Web Setup
1.  Navigate to the web directory:
    ```bash
    cd Base_Template
    ```
2.  Install dependencies:
    ```bash
    npm ci
    ```
3.  Run tests:
    ```bash
    npm test
    ```
4.  Open `index.html` in your browser to run the app.

## 📂 Project Structure

```
SeachemDosing/
├── app/                  # Android Application source code
│   ├── src/main/java/    # Kotlin source files (Compose UI, ViewModels)
│   └── src/test/         # Unit tests
├── Base_Template/        # Web Application source code
│   ├── js/               # JavaScript logic (Calculations, UI)
│   └── css/              # Stylesheets
├── gradle/               # Gradle configuration
└── GEMINI.md             # Project documentation and context
```

## 🤝 Contributing

Contributions are welcome! Please ensure that any changes to the calculation logic (`app/src/main/java/.../logic/Calculations.kt` and `Base_Template/js/dosingCalculations.js`) are kept in sync to maintain consistency across platforms.
