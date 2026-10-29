# திருக்குறள் (Thirukkural) — Native Android App (Kotlin + Jetpack Compose)

A 100% offline-first **Native Android** application (`com.nltsravi.kurals`) built with **Kotlin**, **Jetpack Compose**, and **Material 3** for exploring, searching, practicing pronunciation (Olineri Tamil SDK), organizing, sharing, and exporting all **1,330 Thirukkural couplets** across **133 Chapters (அதிகாரங்கள்)** and **3 Sections (முப்பால்)**.

---

## ✨ Complete Feature Set

### 1. Complete Offline Dataset & 5 Scholar Commentaries
- **1,330 Kurals** (`assets/data/thirukkural.json`) with Tamil couplets, English translations, G.U. Pope couplets, and English transliterations.
- **5 Classical & Modern Tamil Commentaries (உரைகள்)**:
  1. **மு. வரதராசனார் (Mu. Varadarajan — `mv`)**
  2. **சாலமன் பாப்பையா (Solomon Pappaiah — `sp`)**
  3. **மு. கருணாநிதி (Kalaignar M. Karunanidhi — `mk`)**
  4. **பரிமேலழகர் (Parimelazhagar — `prm`)**
  5. **மணக்குடவர் (Manakkudavar — `mnk`)**
- **133 Chapters & 3 Sections** (`assets/data/detail.json`):
  - **அறத்துப்பால் (Virtue)** — Chapters 1–38 (Kurals 1–380)
  - **பொருட்பால் (Wealth)** — Chapters 39–108 (Kurals 381–1080)
  - **காமத்துப்பால் (Love)** — Chapters 109–133 (Kurals 1081–1330)

### 2. Strict 4+3 Word Venpa Couplet Formatting (`KuralCoupletText`)
- Enforces the classical Kural Venpa structure: **4 words (சீர்கள்) on Line 1** and **3 words (சீர்கள்) on Line 2**.
- Dynamically computes a single uniform font size based on Tamil typographic visual width so both lines render at the exact same font size without wrapping or truncating with `...`.

### 3. Olineri (ஒலிநெறி) Tamil Pronunciation Learning Mode (`feature/learn-kural`)
- **First-Launch Prompt & Settings Enablement**: Prompts the user to enable **Kural Learning Mode** powered by the [Olineri Tamil Pronunciation SDK (`nltsravi/olineri-core`)](https://github.com/nltsravi/olineri-core).
- **Strictly Tamil-Only Artifact Downloader**: Downloads only the 6 Tamil (`ta-IN`) SDK artifacts (`phoneme_inventory.json`, `strings_ta.json`, `strings_en.json`, `test_cases.json`, `types.ts`, `index.ts`) and caches them locally.
- **Enable-Only Policy**: Once enabled, Learning Mode remains permanently active.
- **Tamil Brahmic G2P + Needleman-Wunsch Phoneme Alignment + Acoustic Waveform Analyzer**:
  - Converts Tamil Unicode text into IPA phoneme targets (`/ɻ/`, `/ɭ/`, `/l/`, `/r/`, `/ɾ/`, `/ʈ/`, `/t̪/`, `/ɳ/`, `/n/`, `/n̪/`, kuril/nedil vowels) with UTF-8 byte offsets and Akshara cluster indices.
  - Captures live 16 kHz PCM microphone audio (`AudioRecord`) and Tamil speech recognition (`SpeechRecognizer` `ta-IN`).
  - Provides interactive Akshara chips, overall 0–100% GOP score, and bilingual articulatory tongue-position guidance for special Tamil sounds (`ழ`, `ள`, `ல`, `ற`, `ர`, `ண`, `ன`, `ந`).
  - Practice scopes: Full Kural (7 words), Line 1 (4 words), Line 2 (3 words), or any of the 7 individual words (சீர்கள்), plus diagnostic phonetic contrast simulation (`ழ/ல`, `ற/ர`) and manual text test input.

### 4. Instant 10-Tier Relevance Search Engine (`SearchService`)
- 200ms debounced search across all 1,330 Kurals by Kural number (`1–1330`), Chapter number (`1–133`), Chapter name (Tamil/English/Transliteration), Section/Iyal name, Tamil couplet words, commentary text, English translation, or transliteration.

### 5. Custom Kural Collections (`PreferencesAndCollectionStorage`)
- Pre-populated with starter collections (`❤️ My Favorite Kurals`, `🌅 Morning Reading`).
- Create custom collections with 6 accent colors, inline rename, add/remove Kurals via `AddToCollectionModal`, reorder Kurals up/down, and delete with confirmation.

### 6. PDF & Word (`.docx`) Document Export (`ExportService`)
- Export any single Kural, full Chapter (10 Kurals), or custom Collection to:
  - **PDF (`.pdf`)**: Multi-page A4 document with app header icon, centered standing Thiruvalluvar outline watermark, styled Kural cards with Tamil couplet + transliteration, and footer with `© Neolumina Technology Solutions` and `Page X of Y`.
  - **Word (`.docx`)**: Native OpenXML `.docx` ZIP package with headers, footers, styles, and formatted Kural paragraphs.

### 7. High-Resolution Social Status Card & Text Sharing (`ShareModal` & `KuralImageCard`)
- **Image Card (`படம்`)**:
  - Live preview and 1080p PNG rendering in `9:16` (WhatsApp/Instagram Status), `4:5` (Feed), and `1:1` (Square) aspect ratios.
  - Supports all 7 meaning options (`mv`, `sp`, `mk`, `prm`, `mnk`, `translation`, `both`).
  - Optional Card Image Customization (toggled in Settings): choose from 4 Thiruvalluvar portraits, App Icon, or pick a custom photo from the device gallery (`PickVisualMedia`) inside the golden arch of `traditional_template_bg.png`, or use the classic velvet-green plaque (`traditional_template_no_image_bg.png`).
- **Text Post (`உரை`)**: Formatted social text with optional English transliteration, hashtags, character counter (`X ✓`), Quick Copy, and direct sharing to WhatsApp, Instagram, X/Twitter, Threads, Facebook, or the system share sheet.

### 8. Daily Kural Notifications (`NotificationService`)
- Configurable daily reminder (`daily-thirukkural` channel) with 7 quick time presets + custom hour/minute/AM-PM picker, "Send Test Notification Now" button, and deep-linking to the Kural Detail screen on tap.

### 9. Bilingual UI (`தமிழ்` & `English`) & Adaptive Themes
- Complete Tamil (default) and English UI translations (`Translations.kt`).
- Warm parchment Light Mode, Dark Mode, and System Default themes (`AppColors.kt`).

---

## 🛠️ Build & Test Commands

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug
```

