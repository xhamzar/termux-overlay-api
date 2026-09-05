# Termux Overlay API Custom

<p align="center">
  <strong>Layanan Floating Window & Android Overlay Engine Terintegrasi untuk Termux CLI melalui Binder IPC</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg" alt="Android API 26+" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-blue.svg" alt="Kotlin" />
  <img src="https://img.shields.io/badge/IPC-AIDL%20%2F%20Binder-orange.svg" alt="AIDL Binder IPC" />
  <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License MIT" />
</p>

---

## 📌 Pendahuluan

**Termux Overlay API Custom** adalah proyek open-source yang memperluas kemampuan ekosistem Termux di Android. Serupa dengan filosofi `Termux:API`, proyek ini memungkinkan skrip bash, script python, compiler, atau otomasi CLI di Termux untuk berinteraksi langsung dengan GUI sistem Android dan memunculkan **floating overlay window (jendela melayang)** tanpa perlu me-root perangkat Android Anda.

---

## 🏗️ Arsitektur Sistem

```text
Termux CLI ($PREFIX/bin/overlay)
      │
      │  Binder IPC (/dev/binder) & Intent Fallback
      ▼
AIDL Interface (IOverlayService.aidl)
      │
      │  Android Bound Service & Foreground Execution
      ▼
Overlay Binder Service (OverlayBinderService.kt)
      │
      │  Event Dispatcher & Drag Physics
      ▼
Overlay Manager (OverlayManager.kt)
      │
      │  WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      ▼
Floating Interactive UI (Text, Button, Image, Alpha, Draggable)
```

Untuk detail teknis mendalam mengenai Binder transaction code, lifecycle, dan keamanan signature, silakan baca [Dokumentasi Arsitektur](docs/ARCHITECTURE.md).

---

## 🚀 Fitur Utama

1. **AIDL Binder Interface**:
   - `showText(String text)`: Menampilkan output teks floating window dengan monospace typography.
   - `hide()`: Menutup dan menyembunyikan floating window dari layar.
   - `updateText(String text)`: Memperbarui konten teks secara langsung dan real-time.
   - `showButton(String label)`: Menampilkan tombol floating interaktif dengan haptic feedback.
   - `showImage(String path)`: Merender gambar lokal (JPG/PNG) dari penyimpanan perangkat.
   - `setPosition(int x, int y)`: Mengatur posisi koordinat overlay.
   - `setAlpha(float alpha)`: Mengatur transparansi (0.1 - 1.0).

2. **Overlay Engine Tingkat Lanjut**:
   - Berbasis `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
   - **Draggable Touch Physics**: Dapat digeser dengan jari secara responsif di seluruh layar.
   - Desain modern bernuansa Terminal Dark Slate dengan status indikator hijau.
   - Tombol dismiss/tutup cepat.

3. **Background Daemon Service**:
   - Berjalan stabil sebagai **Foreground Service** dengan Notification Channel.
   - Otomatis auto-restart dengan `START_STICKY`.
   - Dukungan auto-start saat perangkat reboot melalui `BootReceiver`.

4. **Termux CLI Tool (`overlay`)**:
   - Skrip executable POSIX yang mudah dipanggil langsung dari terminal Termux.

---

## 📂 Struktur Repositori

```text
termux-overlay-api/
├── app/
│   ├── src/main/
│   │   ├── aidl/com/example/IOverlayService.aidl       # Definisi AIDL Binder IPC
│   │   ├── java/com/example/
│   │   │   ├── MainActivity.kt                         # Dashboard & Test Bench Compose
│   │   │   ├── overlay/
│   │   │   │   ├── OverlayManager.kt                   # Engine floating window & drag physics
│   │   │   │   └── OverlayEventBus.kt                  # Reactive log & event bus
│   │   │   ├── service/
│   │   │   │   ├── OverlayBinderService.kt             # Bound & Foreground service
│   │   │   │   └── BootReceiver.kt                     # Auto-start on boot receiver
│   │   │   └── util/
│   │   │       ├── PermissionHelper.kt                 # System alert window helper
│   │   │       └── PreferenceHelper.kt                 # Pengaturan user preference
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── termux-cli/
│   ├── overlay                                         # Executable CLI client untuk Termux
│   ├── install.sh                                      # Skrip instalasi otomatis ke $PREFIX/bin
│   ├── demo.sh                                         # Contoh demo interaktif Termux
│   └── binder-client/
│       └── TermuxOverlayBinderClient.java              # Client Java murni untuk AIDL Binder
├── docs/
│   ├── ARCHITECTURE.md                                 # Analisis arsitektur & IPC
│   └── SIGNING_GUIDE.md                                # Panduan membuat keystore & signing
└── README.md
```

---

## 🛠️ Cara Membangun APK (Build)

### Persyaratan:
- JDK 17+
- Android SDK (API 26 hingga API 36)

### Langkah Build:
1. Clone repositori:
   ```bash
   git clone https://github.com/example/termux-overlay-api.git
   cd termux-overlay-api
   ```

2. Build APK Debug:
   ```bash
   ./gradlew assembleDebug
   ```
   APK akan dihasilkan di: `app/build/outputs/apk/debug/app-debug.apk`

3. Build APK Release:
   ```bash
   ./gradlew assembleRelease
   ```
   *Lihat [Panduan Signing](docs/SIGNING_GUIDE.md) untuk panduan pembuatan keystore dan signing release.*

---

## 📲 Instalasi & Setup Pertama Kali

1. **Install APK di Android**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   Atau pasang file `.apk` secara manual di ponsel Anda.

2. **Berikan Izin Overlay (Wajib)**:
   - Buka aplikasi **Termux Overlay**.
   - Klik tombol **"Grant Overlay Permission"**.
   - Aktifkan toggle **"Display over other apps"** (Izinkan ditampilkan di atas aplikasi lain).

3. **Pasang CLI di Termux**:
   Buka Termux, lalu jalankan installer:
   ```bash
   bash /sdcard/termux-cli/install.sh
   ```
   Atau salin script secara manual:
   ```bash
   mkdir -p $PREFIX/bin
   cp termux-cli/overlay $PREFIX/bin/overlay
   chmod +x $PREFIX/bin/overlay
   ```

---

## 💻 Penggunaan & Contoh Perintah Termux

Setelah terpasang, perintah `overlay` langsung tersedia di Termux:

### 1. Menampilkan Teks
```bash
overlay show "Hello Android from Termux CLI!"
```

### 2. Memperbarui Teks secara Real-time (Misal Monitoring CPU)
```bash
overlay update "System Load: 24% | Memory: 4.1 GB / 8.0 GB"
```

### 3. Menampilkan Tombol Interaktif
```bash
overlay button "KLIK DISINI"
```

### 4. Menampilkan Gambar dari File
```bash
overlay image "/sdcard/DCIM/Screenshots/preview.png"
```

### 5. Mengatur Transparansi & Posisi
```bash
# Transparansi jendela 80%
overlay alpha 0.8

# Menggeser jendela ke posisi X: 100, Y: 250
overlay pos 100 250
```

### 6. Menyembunyikan / Menutup Jendela
```bash
overlay hide
```

### 7. Memeriksa Status Layanan
```bash
overlay status
```

---

## 💡 Integrasi Script Praktis

### Contoh 1: Notifikasi Selesai Build di Termux
```bash
#!/bin/bash
# Jalankan kompilasi proyek di Termux
make -j4
if [ $? -eq 0 ]; then
    overlay show "✅ Build Sukses!\nWaktu: $(date +%H:%M:%S)"
    sleep 5
    overlay hide
else
    overlay show "❌ Build Gagal!\nCek compiler log."
fi
```

### Contoh 2: Live Progress Bar
```bash
#!/bin/bash
for i in {1..100..10}; do
    overlay update "Mengunduh paket data... [$i%]"
    sleep 0.5
done
overlay button "SELESAI"
```

---

## ❓ Troubleshooting

| Masalah | Penyebab Umum | Solusi |
| :--- | :--- | :--- |
| `SYSTEM_ALERT_WINDOW permission missing` | Izin overlay belum diizinkan | Buka Settings Android -> Apps -> Termux Overlay -> Display over other apps -> Izinkan. |
| Perintah Termux tidak bereaksi | Service terhenti atau dibunuh sistem | Buka aplikasi Termux Overlay, pastikan service aktif. Nonaktifkan Battery Optimization untuk aplikasi. |
| Gambar tidak muncul (`showImage`) | File tidak ditemukan atau path salah | Pastikan path absolut diawali `/sdcard/` atau path valid. Berikan izin akses penyimpanan ke Termux jika perlu (`termux-setup-storage`). |
| Overlay terputus di MIUI/HyperOS | Fitur background pop-up MIUI memblokir | Di ponsel Xiaomi/Poco, aktifkan opsi **"Display pop-up windows while running in the background"** di menu izin aplikasi. |

---

## 📄 Lisensi
Didistribusikan di bawah lisensi MIT. Bebas digunakan dan dimodifikasi untuk kebutuhan komunitas Termux dan Android developer.
