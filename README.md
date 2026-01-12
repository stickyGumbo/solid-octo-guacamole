# AdBlocker - Production-Ready Android VPN DNS Ad Blocker

A high-performance, production-ready Android ad-blocking app using VPNService with DNS-based blocking. Works on Android 8+ without root access and without HTTPS MITM attacks.

## Features

- **VPN-Based DNS Filtering**: Uses Android's VpnService to intercept DNS queries
- **App-Level Filtering**: Select which apps to filter (e.g., YouTube)
- **No Root Required**: Works on standard unrooted devices
- **No HTTPS MITM**: Only DNS-level blocking, no certificate interception
- **Extensive Blocklist**: Built-in ad network blocklist
- **Custom Domains**: Add/remove custom domains from blocklist
- **Remote Blocklists**: Import blocklists from remote URLs
- **Background Service**: Runs as foreground service with persistent notification
- **Battery Optimization**: Requestable exception for battery optimization
- **Material Design UI**: Modern Material Components 3 design

## Technical Stack

- **Language**: Kotlin 1.9.22
- **Android SDK**: compileSdk = 34, targetSdk = 34, minSdk = 26
- **Build System**: Gradle 8.5 with Android Gradle Plugin 8.2.2
- **Java**: OpenJDK 17 LTS
- **UI Framework**: Material Components for Android

## Architecture

```
app/
├── src/main/kotlin/com/stickyapp/adblocker/
│   ├── ui/
│   │   ├── MainActivity.kt          # Main UI activity
│   │   └── AppSelectorDialog.kt     # App selection dialog
│   ├── service/
│   │   ├── VpnAdBlockService.kt    # VPN service implementation
│   │   └── AppFilterManager.kt     # App selection management
│   ├── dns/
│   │   └── DnsHandler.kt            # DNS query parsing & response building
│   ├── blocklist/
│   │   ├── BlocklistManager.kt     # Blocklist operations
│   │   └── BlocklistRepository.kt  # Blocklist data layer
│   └── AndroidManifest.xml
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── dialog_add_domain.xml
│   │   └── item_app.xml
│   └── values/
│       ├── strings.xml
│       └── themes.xml
└── build.gradle
```

## How It Works

### DNS Interception Flow

1. User selects apps to filter in UI
2. VpnService is started with selected apps
3. VPN intercepts all network traffic from selected apps
4. DNS queries (port 53 UDP) are captured
5. Domain is extracted from DNS query packet
6. Domain is checked against blocklist
7. If blocked: DNS response with NXDOMAIN (127.0.0.1 for IPv4, :: for IPv6)
8. If allowed: Query is forwarded to upstream DNS (8.8.8.8)

### Default Blocklist

Includes major ad networks:
- Google Ads (DoubleClick, AdSense, Google Analytics)
- Facebook/Meta
- YouTube ads
- Criteo, Teads, Rubicon, OpenX, PubMatic
- And more...

## Building

### Prerequisites

- Android SDK (API 34)
- Java 17 LTS
- Gradle 8.5 (included via wrapper)

### Build Debug APK

```bash
chmod +x gradlew
./gradlew assembleDebug
```

APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

```bash
./gradlew assembleRelease
```

## Installation

1. Build the APK: `./gradlew assembleDebug`
2. Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Open the AdBlocker app
4. Grant VPN permission when prompted
5. Select apps to filter
6. Tap START VPN button

## Usage

### Enable VPN

1. Tap the "START VPN" button
2. Accept the VPN permission dialog
3. The app will start filtering DNS for selected apps

### Disable VPN

1. Tap the "STOP VPN" button
2. The VPN service will stop immediately

### Select Apps

1. Tap the "+ SELECT APPS" button
2. Search or scroll to find apps
3. Check/uncheck to toggle filtering for each app
4. Tap "OK" to save selection

### Manage Blocklist

- The app includes a comprehensive default blocklist
- Add custom domains using the app menu
- Import remote blocklists from URLs
- Refresh remote blocklists periodically

## Permissions

The app requests:
- `INTERNET`: For DNS queries and remote blocklist fetching
- `CHANGE_NETWORK_STATE`: For VPN setup
- `BIND_VPN_SERVICE`: To create VPN interface
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`: To ensure continuous operation
- `QUERY_ALL_PACKAGES`: To list apps for selection

## GitHub Actions CI/CD

A working GitHub Actions workflow is provided at `.github/workflows/android-build.yml`:

- Automatically builds APK on push/PR
- Uses Ubuntu latest with OpenJDK 17
- Runs `./gradlew assembleDebug`
- Uploads APK as build artifact

## Performance Considerations

- DNS queries are handled via DatagramSocket
- Blocklist is loaded into memory for O(1) lookups
- Coroutines used for I/O operations
- ProGuard minification enabled in release builds

## Known Limitations

- Only intercepts DNS queries, not HTTP/HTTPS traffic
- YouTube ads may use other blocking techniques
- Some apps may detect VPN and behave differently
- Root access not exploited (intentionally for security)

## Security

- No certificate injection or HTTPS MITM
- DNS filtering only
- Blocklist stored locally
- No data collection or analytics
- Open-source implementation

## License

MIT License - See LICENSE file

## Support

For issues, feature requests, or contributions, please visit the GitHub repository.

---

**Version 3.0.0** - Production Ready
