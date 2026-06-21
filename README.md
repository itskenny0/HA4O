# HA4O

**Home Assistant for Old.** A deliberately tiny Home Assistant client that runs on **Android 2.3 Gingerbread** (API 9), for giving a genuinely ancient phone a second life as a wall remote.

It is a companion to [R1HA](https://github.com/itskenny0/R1HA), not a port of it. R1HA is a full Jetpack Compose app with a minimum of Android 6.0; none of that can run on Gingerbread, so HA4O is written from scratch against the 2010-era Android View toolkit. The UI is plain and a little janky on purpose. It works.

## What it does

- Connects to your Home Assistant over the WebSocket API using a long-lived access token.
- Lists every entity with its live state, updated in real time.
- Tap an entity to toggle it (lights, switches, fans, input booleans, automations, humidifiers, sirens) or to fire a scene/script. Anything else opens a read-only attribute view.
- Long-press an entity to favourite it (marked with a star), and use the menu to filter the list to favourites only, so you don't have to scroll the whole house.

That is the whole app. No dashboards, history, energy, automations editor, cameras, voice, or settings beyond the connection. If you want those, use R1HA on a device that can run it.

## Hard limits (please read before expecting miracles)

- **Local network, plain HTTP only.** Android 2.3 speaks only TLS 1.0 with 2010-era ciphers and a 2010 certificate trust store, so it cannot connect to any modern HTTPS endpoint: no Nabu Casa, no reverse proxy with a cert, no Let's Encrypt, no remote access. HA4O talks to `http://<your-ha>:8123` over `ws://` on the same network. That is the only thing that works, and it is by design of the OS, not a bug here.
- **Long-lived token only.** Gingerbread's WebView cannot render Home Assistant's login page, so OAuth is not offered. Create a long-lived access token in your HA profile and paste it on the first screen.
- **No TLS, frozen HTTP stack.** Networking uses okhttp 3.12.x, the last release supporting API 9. It has been security-frozen since 2018, which is acceptable precisely because the only traffic is plaintext on your LAN.

## Build

JDK 17+ and an Android SDK with `platforms;android-35` / `build-tools;35.0.0`.

There are two build flavors, both from the same code and `minSdk 9`:

- **legacy** — `targetSdk 10`, the Gingerbread-first build.
- **modern** — `targetSdk 35`, so Android 14+ allows the install (it refuses APKs whose `targetSdk` is below 23). It still runs everywhere down to API 9; cleartext HTTP is enabled in the manifest because the app only ever talks plain `http`/`ws` to a local HA.

```bash
./gradlew :app:assembleLegacyDebug
adb install app/build/outputs/apk/legacy/debug/app-legacy-debug.apk

# or, to install on a current phone:
./gradlew :app:assembleModernDebug
adb install app/build/outputs/apk/modern/debug/app-modern-debug.apk
```

Each tagged release publishes both: `ha4o-<version>.apk` (legacy) and `ha4o-<version>-modern.apk`. The app depends only on the framework, Kotlin, and okhttp 3.12. JSON is parsed with the built-in `org.json`. There is no AndroidX, no AppCompat, and no Compose, which is what keeps it runnable on Dalvik.

## Status

Built and unit-tested (JSON parsing, the URL/WebSocket derivation, and the tap-to-service mapping). It has **not** been verified on real Gingerbread hardware or an emulator yet, so treat first runs as experimental and report what breaks.

## License

Released into the public domain under [The Unlicense](LICENSE).
