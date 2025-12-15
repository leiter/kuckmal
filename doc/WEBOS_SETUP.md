# webOS TV Setup Guide

This project now supports webOS TV using Kotlin/JS and Compose for Web.

## Project Structure

```
├── app/                    # Android app module
├── shared/                 # Shared Kotlin Multiplatform code
│   └── src/
│       ├── commonMain/     # Common code (models, utils)
│       ├── androidMain/    # Android-specific implementations
│       └── jsMain/         # JS/Web-specific implementations
└── webApp/                 # webOS/Web app module
    └── src/
        └── jsMain/
            ├── kotlin/     # Compose for Web UI
            └── resources/
                ├── index.html
                └── webos/
                    ├── appinfo.json
                    ├── icon.png        # 80x80 PNG (required)
                    └── largeIcon.png   # 130x130 PNG (recommended)
```

## Building

### Build for Web/Development
```bash
./gradlew :webApp:jsBrowserDevelopmentExecutableDistribution
```
Output: `webApp/build/distributions/`

### Build for Production
```bash
./gradlew :webApp:jsBrowserProductionExecutableDistribution
```

### Prepare webOS Package
```bash
./gradlew :webApp:prepareWebOSPackage
```
Output: `webApp/build/webos-package/`

## webOS TV Deployment

### Prerequisites
1. Install [webOS CLI](https://webostv.developer.lge.com/develop/tools/webos-tv-cli-dev-guide)
2. Enable Developer Mode on your LG TV
3. Register your TV with `ares-setup-device`

### Package and Install
```bash
# Build the web app
./gradlew :webApp:jsBrowserProductionExecutableDistribution

# Prepare webOS package
./gradlew :webApp:prepareWebOSPackage

# Package as IPK (requires webOS CLI)
cd webApp/build
ares-package webos-package -o webos-output

# Install on TV
ares-install -d <device-name> webos-output/cut.the.crap.webos_1.0.0_all.ipk

# Launch the app
ares-launch -d <device-name> cut.the.crap.webos
```

## Icon Requirements

### Required Icons for webOS
- `icon.png`: 80x80 pixels PNG
- `largeIcon.png`: 130x130 pixels PNG (recommended)

Place these in: `webApp/src/jsMain/resources/webos/`

## Development Server

Run a local development server:
```bash
./gradlew :webApp:jsBrowserDevelopmentRun
```
Opens at: http://localhost:8080/

## TV-Specific Considerations

### Remote Control Navigation
- Focus management via CSS `:focus` styles
- Support for D-pad navigation (up/down/left/right/enter/back keys)

### Screen Resolution
- Default: 1920x1080 (Full HD)
- App is configured for this resolution in `appinfo.json`

## References

- [LG webOS TV Developer](https://webostv.developer.lge.com/)
- [appinfo.json Documentation](https://webostv.developer.lge.com/develop/references/appinfo-json)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Kotlin/JS](https://kotlinlang.org/docs/js-overview.html)
