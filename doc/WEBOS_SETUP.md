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

### Required Icons for webOS ✅ READY
- `icon.png`: 80x80 pixels PNG ✅ Available
- `largeIcon.png`: 130x130 pixels PNG ✅ Available
- `appinfo.json`: App manifest ✅ Configured

Location: `webApp/src/jsMain/resources/webos/`

## Backend API Server

The web app connects to the Flask API for media data.

### Starting the API Server
```bash
cd api
source venv/bin/activate  # or create with: python3 -m venv venv && pip install -r requirements.txt
python run.py --port 5000
```

### API Configuration
The API URL is configured in `index.html`:
```html
<script>
    window.KUCKMAL_API_URL = "http://localhost:5000";
</script>
```

For webOS deployment, update this to your server's IP address:
```html
<script>
    window.KUCKMAL_API_URL = "http://192.168.1.100:5000";
</script>
```

### Downloading Media Data
On first run, download the film list (681K+ entries):
```bash
curl -X POST http://localhost:5000/api/filmlist/download
```

## Development Server

Run both servers for development:

### Terminal 1 - API Server
```bash
cd api && source venv/bin/activate && python run.py --port 5000
```

### Terminal 2 - Web App
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
