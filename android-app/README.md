# OneApp Android app

This is the first Android app shell for OneApp. It uses the existing live OneApp site and Supabase-backed flows, so the PWA remains unchanged while the Android experience is developed separately.

## Included in this first version

- Android launcher app named **OneApp**
- Native Android share action that shares the current OneApp URL
- Document picker support for uploads from the existing vault
- Android back navigation through web history
- HTTPS-only application entry point
- Separate development branch so this work does not alter the live PWA

## Build an APK

The GitHub Actions workflow **Build OneApp Android APK** builds a debug APK when Android app files change on the `android-app-development` branch, or when manually triggered from the Actions tab.

Download the `oneapp-android-debug-apk` artifact from a successful workflow run. Debug APKs are for testing and are not Play Store release builds.

## Current limitation

This first version displays the existing hosted OneApp app inside Android WebView. An internet connection is required, and the website remains the source of the app's screens and backend. Native screens and a signed Play Store release can be developed as a later step.
