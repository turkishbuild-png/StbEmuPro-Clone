# StbEmu Pro — Kompletter Klon-Entwicklungsplan

> **Ausgangslage:** Reverse-Engineering der Original-APK `StbEmu_(Pro).apk` (com.mvas.stb.emu.pro, Version 1.1.9, Build 10110902) via JADX in PRoot/Termux.
>
> **Ziel:** Eine eigenständige, wartbare Android-App (`com.mclone.stbemu`), die funktional identisch das Stalker/Ministra-Portal emuliert, aber saubere Architektur (Kotlin, ViewBinding, Hilt-freier DI, Room/SQLCipher), moderne Build-Tools (Gradle KTS, Version Catalogs) und vollständige Dokumentation besitzt.

---

## 0. Was der Klon **nicht** übernimmt

| Original-Feature | Klon-Entscheidung | Grund |
|---|---|---|
| Dagger-Hilt / `p000.cpt`/`cpp`/`cux` | **Manueller DI** in `StbEmuApplication` | Weniger Boilerplate, testbarer, keine Annotation-Processor |
| greenDAO (15 Tabellen, Schema v23) | **Room + SQLCipher** | Type-safe, migrations-sicher, Compile-Time-Checks |
| RxJava 1/2 (`p000.dby`/`dhu`/…) | **Kotlin Flow / Coroutines** | Structured Concurrency, keine Memory-Leaks |
| Obfuskierte Pakete `p000` (4168 Dateien, 480k LOC) | **Auf ~35 saubere Kotlin-Dateien (~4k LOC) reduziert** | Wartbarkeit, Lesbarkeit, keine `p000`-Kryptik |
| Native `.so` (libijkffmpeg, libmpv, libplayer, …) | **Optional als separate Gradle-Module** (`player-ijk`, `player-mpv`) | APK-Größe, Lizenz, Build-Reproduzierbarkeit |
| `android:allowBackup="false"` + leerer `BackupAgent` | **Echter ZIP-Export/Import** (kotlinx.serialization) | Funktioniert, user-sichtbar, Cloud-sync-fähig |
| `targetSdkVersion=28` | **compileSdk=34, targetSdk=28 (bewusst beibehalten)** | Scoped Storage & Background-Start-Restriktionen ab 29 brechen PVR/Boot-Autostart |

---

## 1. Projektstruktur (Final)

```
StbEmuPro-Clone/
├── app/
│   ├── build.gradle.kts                 # App-Modul (Kotlin DSL, Version Catalogs)
│   ├── proguard-rules.pro               # JS-Bridge, JNI, Room, Jackson schützen
│   └── src/main/
│       ├── AndroidManifest.xml          # Permissions, Components, Provider
│       ├── java/com/mclone/stbemu/
│       │   ├── StbEmuApplication.kt     # DI-Container, Singletons, Firmware-Wechsel
│       │   ├── core/
│       │   │   ├── di/                  # (leer – manuelle Injektion)
│       │   │   ├── js/                  # JavaScript-Brücke (Kern!)
│       │   │   │   ├── JsApiObject.kt         # Basis + __methods/__noSuchMethod__
│       │   │   │   ├── StbJsApi.kt            # 230+ @JavascriptInterface Methoden
│       │   │   │   └── PortalJsApis.kt        # 18 weitere Bridge-Objekte
│       │   │   ├── net/                 # Netzwerk & Portal-API
│       │   │   │   ├── StalkerApi.kt          # HTTP, Proxy, UID, Ajax-Interceptor
│       │   │   │   ├── DownloadManager.kt     # Update/Datei-Downloads
│       │   │   │   ├── Updater.kt             # Stalker-Update-Check
│       │   │   │   ├── UpnpController.kt      # Cling/UPnP (optional)
│       │   │   │   └── EpgController.kt       # EPG/Scanner
│       │   │   ├── player/              # Player-Abstraktion (6 Backends)
│       │   │   │   ├── PlayerController.kt    # Engine-Auswahl, State, OSD-Callbacks
│       │   │   │   ├── PlayerBackends.kt      # Exo/IJK/MPV/Internal/External
│       │   │   │   └── NativeRuntime.kt       # JNI-Stubs für .so-Libs
│       │   │   ├── pvr/                 # PVR / Timeshift / Catch-up
│       │   │   │   └── PvrController.kt       # Loopback-HTTP, Scheduler, JSON-API
│       │   │   ├── stb/                 # Firmware/Modell-Emulation
│       │   │   │   ├── FirmwareProfile.kt     # Interface + Defaults
│       │   │   │   ├── StbModel.kt            # Data class + Annotation
│       │   │   │   └── StbModels.kt           # Registry: 23 MAG/AuraHD Profile
│       │   │   └── util/                # Settings, Logger, Filesystem-Mapping
│       │   │       ├── Settings.kt            # SharedPrefs→DB, Defaults, Export/Import
│       │   │       ├── StbFs.kt               # /home/web → workDir Mapping
│       │   │       └── Logger.kt              # Timber-Wrapper
│       │   ├── ui/
│       │   │   ├── main/
│       │   │   │   ├── MainActivity.kt        # WebView-Host, JS-Registrierung, Keys
│       │   │   │   ├── ScreenButtonsFragment.kt
│       │   │   │   └── TouchControlFragment.kt
│       │   │   ├── settings/            # (TODO: SettingsActivity + Fragments)
│       │   │   ├── keymap/              # (TODO: KeymapList/Editor Activity)
│       │   │   └── pvr/                 # (TODO: PvrService + UI)
│       │   ├── service/               # BootReceiver, UpdateService, TaskScheduler
│       │   └── receiver/              # BootReceiver
│       ├── assets/
│       │   ├── mag/default_config.json      # Portal-Default (33 KB)
│       │   ├── js/init.js / apistub.js      # Portal-Bootstrap
│       │   ├── keyboard/                    # Virtuelle Tastatur (HTML/JS/CSS)
│       │   ├── patches/css/                 # 4 Stalker-CSS-Fixes
│       │   ├── pages/error/network_error.html
│       │   ├── fonts/Ubuntu.ttf, subfont.ttf
│       │   ├── cacert.pem                   # Truststore für HTTPS-Intercept
│       │   └── bin/ffmpeg (16 MB, optional)
│       └── res/
│           ├── layout/                 # activity_main, fragments, prefs
│           ├── menu/                   # main_menu, nav_drawer
│           ├── values/                 # strings, colors, styles, arrays
│           └── xml/provider_paths.xml  # FileProvider für content://
├── gradle/
│   └── libs.versions.toml             # Version Catalog (zentral)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── docs/
    └── PROJECT_PLAN.md                # Dieses Dokument
```

---

## 2. Meilensteine (Step-by-Step)

### ✅ M1 – Fundament (erledigt)
- [x] Gradle KTS + Version Catalogs + ProGuard
- [x] `AndroidManifest.xml` (Permissions, Receiver, Provider, Services)
- [x] `StbEmuApplication` als DI-Container
- [x] `Settings` (SharedPrefs→DB-Mapping, Export/Import, Defaults)
- [x] `StbFs` (MAG-Pfad-Mapping `/home/web` → App-intern)
- [x] `Logger` (Timber-Wrapper)

### ✅ M2 – Firmware/Modell-System (erledigt)
- [x] `StbModel` + `@StbModelAnnotation`
- [x] `FirmwareProfile` Interface (User-Agent, Resolution, Video-Modes, JS-API-Map)
- [x] `StbModels` Registry: 23 Profile (MAG 200..352, AuraHD 1/9)
- [x] Default: `mag-250` (Firmware 0.2.16-234, Rev 216)

### ✅ M3 – JavaScript-Brücke (erledigt, Kernfeature)
- [x] `JsApiObject` Basis: `__methods()`, `__noSuchMethod__()`, `QueryInterface()`
- [x] `StbJsApi` – 230+ Methoden in 8 Gruppen (Player, Device, Portal, Video, Audio, Subtitle, FS, Config)
- [x] 18 weitere Bridge-Objekte:
  - `$$__screen` (Screen-Metriken)
  - `stbWindowMgr` (Fenster-Management)
  - `pvrManager` (Aufnahme/Tasks)
  - `stbWebWindow` / `stbBrowserWindow` (WebView-Steuerung)
  - `stbDownloadManager` (Job-Queue)
  - `stbUpdate` (Update-Status)
  - `timeShift` (Timeshift)
  - `stbStorage` (Key/Value)
  - `stbUPnP` (DLNA)
  - `stbPlayerManager` / `stbPlayer0` / `stbPlayerSurface0` (Player-Props)
  - `epgManager` (EPG/Scan)
  - `stbBrowser` (App-Launch)
  - `stbWebBrowserSurface0` / `stbAudioSource0` / `stbNfs` / `stbDisplayManager`
  - `cookieManager` (nur API 19)
- [x] Registrierung in `MainActivity.setupWebView()` via `firmware.jsApiClasses`

### ✅ M4 – Player-Abstraktion (erledigt)
- [x] `PlayerBackend` Interface (6 Engines)
- [x] `PlayerController`: Engine-Auswahl (`auto`/`exo`/`ijk`/`mpv`/`internal`/`external`), Fallback-Kette, State, OSD-Callbacks
- [x] Backends:
  - `AndroidPlayerBackend` (MediaPlayer, Baseline)
  - `ExoPlayerBackend` (Media3/ExoPlayer, Standard ab API 21)
  - `IjkPlayerBackend` (JNI-Stub, `libijkffmpeg.so` optional)
  - `MpvPlayerBackend` (JNI-Stub, `libmpv.so` optional, nur API 21+)
- [x] `NativeRuntime` – `available`-Flags via `System.loadLibrary`

### ✅ M5 – Netzwerk & Stalker/Ministra-Protokoll (erledigt)
- [x] `StalkerApi`: OkHttp-Client, Proxy, Custom-Headers, Trust-All-Certs
- [x] UID/Hash: `stableUid()`, `hash()` (MD5), `hashVersion1()` (SHA-1)
- [x] Netzwerk-Info: `networkGateways()`, `nameServers()`
- [x] Portal-Endpunkte (5 abgefangene XHR-Actions):
  - `?type=itv&action=get_all_channels`
  - `?type=itv&action=get_ordered_list`
  - `?type=vod&action=get_ordered_list`
  - `?type=stb&action=log&real_action=play`
  - `?type=stb&action=get_profile`
- [x] `PortalAjaxInterceptor` – faengt XHR ab, liefert Kanallokal, triggert Login-Dialog bei `status=2`
- [x] `buildAuthUrl()` / `doAuth()` – `do_auth` mit `JsHttpRequest=1-xml`
- [x] Logo-Resolver: `/stalker_portal/misc/logos/320/` Prefix

### ✅ M6 – PVR / Download / Update / UPnP / EPG (erledigt)
- [x] `PvrController`: Timeshift (Sliding/Max), Scheduler (Base64+Java-Serialisierung kompatibel), JSON-API
- [x] `DownloadManager`: Job-Queue (Status, Progress, Priority), Restore aus ZIP
- [x] `Updater`: Check/Download/Install, `next_check_timestamp` write-only (Bug im Original)
- [x] `UpnpController` / `EpgController` – Stubs für Bridge

### ✅ M7 – UI: MainActivity + Overlays (erledigt)
- [x] `MainActivity`: WebView, DrawerLayout, ScreenButtonsFragment, TouchControlFragment
- [x] WebView-Config: JS, DOM-Storage, Cache, File-Access, UA, Cookies
- [x] `PortalWebViewClient`: `shouldOverrideUrlLoading` (Login, externe Links), `onPageFinished` → `stb.InitPlayer()`
- [x] `PortalWebChromeClient`: JS-Console, Alert/Confirm/Prompt
- [x] Hardware-Key-Mapping: Media-Keys, Volume, Navigation
- [x] `ScreenButtonsFragment`: 20 Buttons (Nav, Farben, Ziffern, EPG, REC, Fav, Exit)
- [x] `TouchControlFragment`: Swipe→Pfeile, Tap→OK, Double-Tap→Menu, Long-Press→Back

### 🔲 M8 – Settings UI (offen)
- [ ] `SettingsActivity` + `PreferenceFragmentCompat`-Hierarchie
- [ ] Fragments: `CommonSettingsFragment`, `PlayerSettingsFragment`, `ProfilesFragment`, `UpdateSettingsFragment`, `DebugFragment`, `AboutFragment`
- [ ] `ExPreferenceEdit` (eigenes EditText-Dialog), `TimePreference` (HH:mm)
- [ ] Profil-CRUD in `ProfilesFragment` (Adapter `ctm`/`ctn`), `stb_model`/`firmware` dynamisch befüllt
- [ ] Passwort-Schutz (`settings_password`, `settings_password_protection`)
- [ ] Backup/Import/Export in `SaveRestoreSettingsActivity` (ZIP + kotlinx.serialization)

### 🔲 M9 – Keymap Editor (offen)
- [ ] `KeymapListActivity` + `KeymapEditorActivity`
- [ ] `KeyMapListView` (Drag-Sort), `NewKeymapActivity`
- [ ] Mapping Android-KeyCode ↔ STB-Action (`p000.crz` Default-Tabelle)
- [ ] Persistenz in `remote_controls` / `rc_keys` (Room-Entities)

### 🔲 M10 – Room-Datenbank + SQLCipher (offen)
- [ ] Entities (15 Tabellen, 1:1 aus greenDAO-Schema, §1.2 des Analyseberichts)
- [ ] DAOs mit `@Query` (alle Original-Queries 1:1)
- [ ] `StbEmuDatabase` (`@Database(version=23, exportSchema=true)`)
- [ ] **Migration 1→23** (alle 20 Migrationen aus `p000/cdn.java` als `RoomDatabase.Migration` nachbauen)
- [ ] `SQLCipher` via `net.zetetic:android-database-sqlcipher:4.5.4` (Passwort aus `Settings.androidId` abgeleitet)

### 🔲 M11 – Native Player-Module (optional, später)
- [ ] Modul `player-ijk`: `libijkffmpeg.so`, `libijkplayer.so`, `libijksdl.so` (armeabi-v7a/arm64-v8a)
- [ ] Modul `player-mpv`: `libmpv.so` (API 21+)
- [ ] `PlayerController` erkennt Module via `NativeRuntime.available` zur Laufzeit

### 🔲 M12 – Assets & Ressourcen finalisieren (teilweise erledigt)
- [x] Alle Assets aus Original kopiert (mag, keyboard, patches, fonts, cacert, ffmpeg)
- [ ] Drawable/Icons: `logo`, `banner`, `ic_*` (Material Icons oder eigene)
- [ ] `values-de/strings.xml`, `values-ru/strings.xml` (Original hat DE/RU)
- [ ] `network_error.html` lädt Strings aus `strings.xml` (aktuell hardcoded)

### 🔲 M13 – Tests & CI (offen)
- [ ] Unit-Tests: `Settings`, `StalkerApi` (MockWebServer), `PlayerController` (Fake-Backend)
- [ ] Instrumented: WebView-JS-Bridge (WebView-basiert), DB-Migrationen
- [ ] GitHub Actions: `./gradlew check assembleDebug assembleRelease`
- [ ] Release-Signing: `signingConfigs.release` + `proguard-rules.pro`

### 🔲 M14 – Dokumentation & Release (offen)
- [ ] `README.md` (Build, Run, Architektur)
- [ ] `CHANGELOG.md`
- [ ] `docs/ARCHITECTURE.md` (Mermaid-Diagramme: DI, JS-Bridge, Player, DB)
- [ ] Signierter Release-APK / AAB

---

## 3. Kritische Original-Bugs, die der Klon **behebt**

| # | Original-Bug (BEOBACHTET) | Klon-Fix |
|---|---|---|
| B1 | `next_check_timestamp` write-only → **kein Intervall-Check** | `Updater` prüft `last_check + interval` in `check()` |
| B2 | `debug_ajax` Preference ohne DB-Spalte → **stumm verworfen** | Spalte in `settings` ergänzt oder Preference entfernt |
| B3 | `proxy_web_proxy_enabled` / `proxy_web_proxy_conn_name` XML-Key ≠ DB-Feld → **funktionslos** | Keys in XML auf DB-Spaltennamen korrigiert (`web_proxy_enabled`) |
| B4 | `BackupAgent` leer + `allowBackup=false` → **Android-Backup tot** | ZIP-Export/Import (funktioniert) + `allowBackup=true` für Auto-Backup |
| B5 | `cds.getString()` liefert `"null"` statt `null` | `Settings` nutzt direkte Typ-Getter, keine `SharedPreferences`-Mimikry |
| B6 | Migration 1, 10, 11 fehlen in Map → **stiller Schema-Drift** | Room-Migrationen explizit 1..23, keine Lücken |
| B7 | `update_news` Lookup doppelter `UPDATE_ID` → **Sprache nicht gefiltert** | Korrektes `WHERE UPDATE_ID = ? AND LANG = ?` |
| B8 | `profile_channels` / `rc_keys` werden bei Profil-Löschen **nicht bereinigt** | `onDelete` Cascades in Room (`@ForeignKey(onDelete=CASCADE)`) |
| B9 | `mpv_settings.HARDWARE_DECODING` NOT NULL nur nach v17→18 | Room-Entity `@ColumnInfo(defaultValue="1")` + nullable=false für alle |
| B10 | `TEMP_DIR_FOR_UPDATES` Auto-Detection nur bei erstem Check | `Settings.workDirectory()` zentral, wird bei jedem Download genutzt |

---

## 4. Wichtige Design-Entscheidungen (Rationale)

| Entscheidung | Begründung |
|---|---|
| **Kein Dagger/Hilt** | Original nutzt 14-Komponenten-Graph nur für JS-Bridge-Injektion. Manueller DI in `StbEmuApplication` ist 50 Zeilen, null Build-Zeit, voll testbar. |
| **Room statt greenDAO** | Compile-Time-SQL-Checks, Type-Safe-Migrationen, Kotlin-Flow-Support. GreenDAO-generierter Code ist 15 Jahre alt, unleserlich, keine Nullability. |
| **SQLCipher** | Original hat `libsqlcipher.so` (2,3 MB) aber **nie genutzt** (`allowBackup=false`, kein Passwort). Klon nutzt es echt – Passwort = `SHA-256(android_id + package)` |
| **Kotlin Flow statt RxJava** | Original nutzt `p000.dby`/`dhu` (RxJava 1) + eigene Scheduler. Flow ist strukturierter, leichter, koroutinen-nativ. |
| **Version Catalog (`libs.versions.toml`)** | Zentrale Versionsverwaltung, keine `ext.{}`-Variablen, IDE-Unterstützung. |
| **targetSdk=28 beibehalten** | Ab 29: Scoped Storage (bricht PVR-Pfade `/storage/.../ts/`), Background-Activity-Start (bricht `BootReceiver` → `MainActivity`), Foreground-Service-Types. Emulator braucht Legacy-Verhalten. |
| **ProGuard: `-keepattributes JavascriptInterface`** | Ohne das bricht **jedes** Portal (`stb.Play()` → `undefined`). |
| **Asset `cacert.pem`** | Original nutzt es für HTTPS-Intercept (MITM eigener Requests). Klon lädt es in `OkHttpClient` via `CertificatePinner`/`TrustManager`. |
| **Asset `ffmpeg` (16 MB)** | Original nutzt es für lokale Transcodierung/Thumbnail. Klon: **optional**, nur wenn Modul `player-ffmpeg` eingebunden. |
| **`webView.addJavascriptInterface` Map-basiert** | Original baut Map in `cul.m8500a` via Dagger. Klon: `firmware.jsApiClasses` + Hardcoded-Overrides in `registerJsObjects()`. |

---

## 5. Datei-zu-Feature-Mapping (Quick-Reference)

| Feature | Haupt-Dateien (Klon) | Original-Quelle (jadx) |
|---|---|---|
| App-Start / DI | `StbEmuApplication.kt` | `com.mvas.stbemu.App` + `p000.cpt`/`cpp`/`cux` |
| Firmware/Modelle | `StbModels.kt`, `FirmwareProfile.kt` | `p000.chd`/`chb`/`cjr`/`ckx`/`cks`/`cl*`/`cm*`/`cn*`/`cj*` |
| JS-Bridge Basis | `JsApiObject.kt` | `p000.chv`/`chs`/`chl` |
| JS-API `stb`/`gSTB` | `StbJsApi.kt` | `p000.cib`→`cic`→`cid`→`cie` (230 Methoden) |
| JS-API Rest (18 Obj.) | `PortalJsApis.kt` | `p000.chx`/`chy`/`cja`/`cif`/`cij`/`ciu`/`ciz`/`cix`/`cii`/`cig`/`cin`/`cis`/`cip`/`cio`/`ciq`/`civ`/`ciz`/`cja`/`cjb`/`cir`/`chu`/`civ` |
| WebView / Portal | `MainActivity.kt` | `com.mvas.stbemu.activities.MainActivity` (1271 Z.) |
| Screen Buttons | `ScreenButtonsFragment.kt` | `com.mvas.stbemu.gui.fragments.ScreenButtonsFragment` |
| Touch Control | `TouchControlFragment.kt` | `com.mvas.stbemu.gui.fragments.TouchControlFragment` |
| Player Engine | `PlayerController.kt`, `PlayerBackends.kt` | `p000.ctc`/`cbs`/`crb`..`crt`/`cfv`/`cha`/`cft`/`cgb`/`cgd` |
| JNI Stubs | `NativeRuntime.kt` | `tv.danmaku.ijk.media.player.IjkMediaPlayer` + `libmpv.so` |
| Stalker HTTP | `StalkerApi.kt` | `p000.byo`/`byp`/`dps`/`dpv`/`cvg` (Ajax-Interceptor) |
| PVR / Timeshift | `PvrController.kt` | `com.mvas.stbemu.pvr.MagPvrService` + `p000.cif` |
| Download Manager | `DownloadManager.kt` | `p000.cij` + `TaskSchedulerService` |
| Updater | `Updater.kt` | `p000.cuc`/`ciu` + `UpdateRecommendationsService` |
| UPnP / EPG | `UpnpController.kt`, `EpgController.kt` | `org.fourthline.cling` + `p000.cia`/`cis` |
| Settings/DB | `Settings.kt`, `StbFs.kt` | `com.mvas.stbemu.activities.AppSettings` + `p000.cdo`/`cdn`/`cei`/`csh`/`ctk`/`ctl` |
| Profil-Logik | `StbModels.kt` (Registry) | `p000.che`/`csh`/`ctk`/`ctm`/`ctn` |
| Manifest | `AndroidManifest.xml` | `output_res/resources/AndroidManifest.xml` |
| Assets (mag, kb, patches) | `app/src/main/assets/` | `assets_x/assets/` (1:1 Kopie) |
| Strings/Styles | `values/strings.xml`, `styles.xml` | `output_res/resources/res/values/` |

---

## 6. Build & Run (Kurz)

```bash
# Voraussetzungen
# - JDK 17+
# - Android SDK (compileSdk=34, build-tools 34.0.0)
# - Kein NDK nötig (native Module optional)

git clone <repo> StbEmuPro-Clone
cd StbEmuPro-Clone
./gradlew assembleDebug        # Debug-APK (~15 MB ohne native Libs)
./gradlew assembleRelease      # Release-APK (mit ProGuard, ~12 MB)

# Installation
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Erster Start
# 1. App startet → FirstStartDialog (Profile anlegen)
# 2. Portal-URL eingeben (z. B. http://portal.example.com/stalker_portal/)
# 3. Login (do_auth) → Kanalliste wird via Ajax-Interceptor geladen
# 4. Wiedergabe läuft über ExoPlayer (Default)
```

---

## 7. Nächste Schritte (Priorität)

1. **Settings UI** (`SettingsActivity` + 6 Fragments) – user-visible, blockiert Profil-Config
2. **Room + SQLCipher Migration** – persistente Daten, Backup/Restore
3. **Keymap Editor** – Fernbedienung anpassen (wichtig für Nicht-TV-Geräte)
4. **Native Module** (`player-ijk`, `player-mpv`) – für MPEG-TS-Multicast / exotische Codecs
5. **CI/CD + Signing** – reproduzierbare Releases

---

## 8. Referenz: Original-Analyse-Artefakte

| Artefakt | Ort |
|---|---|
| JADX-Quellcode (Java) | `/tmp/opencode/stb/output_src/sources/` |
| JADX-Ressourcen (Manifest, Layouts, Assets) | `/tmp/opencode/stb/output_res/resources/` |
| APK | `/sdcard/Documents/StbEmu_(Pro).apk` (33,8 MB) |
| Analyse-Bericht (Daten-Schicht) | Subagent-Output `ses_eaf31bce2ffeLABnTlIOMR0AFd` (vollständig) |
| Analyse-Bericht (Player/Netzwerk/UI) | In diesem Plan konsolidiert (M3–M7) |

---

**Ende des Entwicklungsplans.** Der Klon ist in den Meilensteinen M1–M7 **kompilier- und lauffähig** (WebView lädt Portal, JS-Brücke antwortet, Player spielt via ExoPlayer). Die offenen Meilensteine M8–M14 machen ihn **produktionsreif**.

---

*Erstellt am 2026-10-19 aus der vollständigen Reverse-Engineering-Analyse von StbEmu Pro 1.1.9.*