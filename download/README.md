# Download APK & Client Termux

File APK telah selesai di-build dan siap diunduh:

- **Nama File**: `TermuxOverlay.apk` (ukuran ~22 MB)
- **Lokasi**: `/download/TermuxOverlay.apk`

---

## 📲 Cara Install di Perangkat Android

1. Unduh file `TermuxOverlay.apk` dari folder `download/` melalui file explorer AI Studio (klik kanan > Download, atau export project).
2. Install APK di perangkat Android Anda.
3. Buka aplikasi **Termux Overlay**, lalu:
   - Berikan izin **Display over other apps** (`SYSTEM_ALERT_WINDOW`).
   - (Opsional) Nonaktifkan optimasi baterai agar service tetap aktif di background.
   - Ketuk tombol **Start Service**.

---

## 💻 Pasang Command di Termux

Jalankan perintah ini di dalam Termux:

```bash
# Salin script CLI ke $PREFIX/bin
mkdir -p $PREFIX/bin
curl -o $PREFIX/bin/overlay https://raw.githubusercontent.com/.../termux-cli/overlay # atau salin isi file termux-cli/overlay
chmod +x $PREFIX/bin/overlay

# Tes overlay
overlay show "Hello from Termux!"
```
