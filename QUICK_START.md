# 🚀 Quick Start Guide - Ad Blocker v3.0.0

## ⚡ Get Running in 5 Minutes

### Step 1: Build the APK
```bash
cd /workspaces/solid-octo-guacamole

# Make wrapper executable (Linux/Mac)
chmod +x gradlew

# Build debug APK (takes 1-2 minutes)
./gradlew assembleDebug
```

**Success:** APK generated at `app/build/outputs/apk/debug/app-debug.apk`

### Step 2: Install on Device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Open & Setup
1. Open "Ad Blocker" app
2. Tap **"+ SELECT APPS"**
3. Search for an app (e.g., "YouTube")
4. Check the box to select it
5. Tap "OK" to save

### Step 4: Enable VPN
1. Tap **"START VPN"** button
2. Android will prompt for VPN permission
3. **Allow** the permission
4. App will ask about battery optimization (optional)
5. Status will change to **"VPN: ON"**

### Step 5: Done! 🎉
Your selected app now has ads blocked at the DNS level!

---

## 🔍 What Gets Blocked?

By default, blocks:
- ✅ Google Ads networks
- ✅ Facebook/Meta trackers
- ✅ YouTube ad domains
- ✅ Common ad networks (Criteo, Teads, Rubicon, etc.)
- ✅ Analytics trackers

---

## 🛑 Stop VPN

Simply tap **"STOP VPN"** button. Service stops immediately.

---

## 🔧 Building Release APK

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk`

Note: Release APK signs with debug keystore. For production, configure your signing config in `app/build.gradle`.

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| "VPN permission denied" | Allow permission when Android prompts |
| "Cannot connect after VPN starts" | Check if DNS is actually being queried |
| "App doesn't start VPN" | Grant BIND_VPN_SERVICE permission |
| "Ads still showing" | Add domain to blocklist via UI |
| "Slow performance" | Check device RAM (300MB+ recommended) |

---

## 📊 Architecture at a Glance

```
┌──────────────────────────────────┐
│   User Selects Apps (UI)        │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│   AppFilterManager (Storage)     │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│   VpnAdBlockService (VPN)        │
│   - Intercepts DNS (port 53)     │
│   - Calls DnsHandler             │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│   DnsHandler (DNS Protocol)      │
│   - Parses query                 │
│   - Builds response              │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│   BlocklistRepository            │
│   - Checks domain                │
└──────────────┬───────────────────┘
               │
┌──────────────▼───────────────────┐
│   BlocklistManager (Storage)     │
│   - Built-in list               │
│   - Custom domains              │
│   - Remote lists                │
└──────────────────────────────────┘
```

---

## 📝 Project Structure

```
solid-octo-guacamole/
├── app/src/main/
│   ├── kotlin/com/stickyapp/adblocker/
│   │   ├── ui/                      ← UI screens
│   │   ├── service/                 ← VPN service
│   │   ├── dns/                     ← DNS handling
│   │   └── blocklist/               ← Blocklist logic
│   └── res/
│       ├── layout/                  ← XML layouts
│       └── values/                  ← Resources
├── build.gradle                     ← Gradle config
├── settings.gradle                  ← Root config
└── .github/workflows/               ← CI/CD
```

---

## 🔑 Key Technologies

- **Kotlin 1.9.22** - Language
- **Android VpnService** - Core VPN
- **DatagramSocket** - DNS interception
- **Material Components** - UI design
- **Gradle 8.5** - Build system
- **Java 17 LTS** - JVM version

---

## 📋 Permissions Used

| Permission | Purpose |
|-----------|---------|
| INTERNET | Fetch remote blocklists |
| CHANGE_NETWORK_STATE | Setup VPN |
| BIND_VPN_SERVICE | VPN service |
| ACCESS_NETWORK_STATE | Query network status |
| QUERY_ALL_PACKAGES | List apps |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | Keep VPN running |

---

## 🔐 Security Notes

✅ **No Root** - Works on unrooted devices  
✅ **No MITM** - No certificate injection  
✅ **DNS Only** - No packet inspection  
✅ **Local Storage** - No cloud sync  
✅ **No Tracking** - No analytics  
✅ **Open Source** - Auditable code  

---

## 🚀 Advanced Usage

### Add Custom Blocklist URL
The app supports remote blocklist URLs. URLs must be:
- ✅ HTTPS only
- ✅ Plain text format (one domain per line)
- ✅ Valid domain names

Example format:
```
ad.doubleclick.net
googleadservices.com
ads.google.com
facebook.com
```

### Add Custom Domain
Manually add domains to the blocklist via the UI.

### Refresh Remote Lists
Periodically update remote blocklists via the "REFRESH BLOCKLISTS" button.

---

## 🎯 Next Steps

1. ✅ Build APK: `./gradlew assembleDebug`
2. ✅ Install: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. ✅ Open app
4. ✅ Select apps
5. ✅ Start VPN
6. ✅ Enjoy ad-free browsing!

---

## 📞 Need Help?

- **Build Issues**: Check Java version (`java -version` should show 17)
- **VPN Issues**: Restart device after installation
- **App Crashes**: Check Android version (min 8.0)
- **Blocked Domains**: App might not support certain blocking techniques

---

## 📄 Documentation Files

- **README.md** - Full feature documentation
- **GENERATION_REPORT.md** - Generation details
- **FILE_STRUCTURE.md** - Complete file inventory
- **QUICK_START.md** - This file

---

**Status**: ✅ Production Ready  
**Version**: 3.0.0  
**Generated**: January 12, 2026  

**Happy Ad-Free Browsing! 🎉**
