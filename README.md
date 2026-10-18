# OneApp.in

Secure personal document vault with family sharing, Aadhaar card export, and emergency access.

**Live:** https://oneapp-in.vercel.app

## Features
- Document vault (Aadhaar and more)
- Family sharing & emergency access
- Share ID card as JPG/PDF (native share sheet in Capacitor APK / Chrome)
- Google sign-in with account picker
- PWA + Android (Capacitor)

## Stack
- Frontend: single-page app (`index.html`)
- Auth & data: Supabase
- Hosting: Vercel
- Android: Capacitor 6 (local project on builder machine)

## Android (Capacitor)
Project path on build machine: `oneapp-capacitor/`
```bash
npm install
npx cap sync android
# Open android/ in Android Studio → Run on device
```

Native plugins: `@capacitor/share`, `@capacitor/filesystem`, `@capacitor/browser`, `@capacitor/status-bar`.

## Deploy
Push to `main` → Vercel auto-deploys.
