# Arsitektur Termux Overlay API

Termux Overlay API Custom dirancang dengan arsitektur modular yang menjembatani lingkungan Linux Termux dengan subsistem GUI Android melalui **Binder Inter-Process Communication (IPC)** dan **AIDL Interface**.

---

## 1. Diagram Alur Sistem (System Architecture)

```text
┌────────────────────────────────────────────────────────┐
│                   Termux Environment                   │
│  - Shell Script ($PREFIX/bin/overlay)                  │
│  - Java/DEX CLI runner (app_process / service call)    │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ 1. Binder IPC (/dev/binder)
                            │    Transaction Code: FIRST_CALL_TRANSACTION + N
                            ▼
┌────────────────────────────────────────────────────────┐
│             AIDL Interface (IOverlayService)           │
│  - IOverlayService.aidl                                │
│  - IOverlayService.Stub (Server-side Binder)           │
│  - IOverlayService.Stub.Proxy (Client-side Proxy)      │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ 2. Bound & Foreground Execution
                            ▼
┌────────────────────────────────────────────────────────┐
│            Android Service (OverlayBinderService)      │
│  - Android Bound Service (onBind -> IBinder)           │
│  - Foreground Service (NotificationChannel)            │
│  - Background Stability & Lifecycle Keeper             │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ 3. UI Thread Dispatcher
                            ▼
┌────────────────────────────────────────────────────────┐
│                 Engine (OverlayManager)                │
│  - Drag & Drop Physics (MotionEvent.ACTION_MOVE)       │
│  - Transparency & Dynamic Sizing Engine                │
│  - Text, Button, and ImageView Builders                │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ 4. WindowManager IPC
                            ▼
┌────────────────────────────────────────────────────────┐
│       Android OS WindowManagerService (WMS)            │
│  - Window Type: TYPE_APPLICATION_OVERLAY               │
│  - Flags: FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_IN_SCREEN   │
└───────────────────────────┬────────────────────────────┘
                            │
                            │ 5. SurfaceFlinger Composite
                            ▼
┌────────────────────────────────────────────────────────┐
│             Floating Draggable UI on Screen            │
└────────────────────────────────────────────────────────┘
```

---

## 2. Bagaimana Termux Menemukan & Memanggil Service

### A. Mekanisme Binder IPC Android
Android menggunakan kernel driver `/dev/binder` untuk pertukaran data antar proses yang aman dan efisien menggunakan zero-copy memory mapping (`mmap`).

1. **Service Registration & Binding**:
   - `OverlayBinderService` dideklarasikan di `AndroidManifest.xml` dengan intent-filter:
     ```xml
     <action android:name="com.example.IOverlayService" />
     ```
   - Aplikasi luar atau Termux dapat melakukan bind menggunakan komponen:
     `com.aistudio.termuxoverlay.api/com.example.service.OverlayBinderService`.

2. **Dukungan Dua Mode Komunikasi**:
   - **Mode 1: Pure AIDL Binder via `app_process` / `service call`**:
     Di Termux, binary Java dapat dijalankan dengan `app_process` untuk memanggil `IOverlayService.Stub.asInterface(binder)` secara native.
   - **Mode 2: Fast Shell Command via `am start-foreground-service`**:
     Script CLI `overlay` mengirimkan intent perintah langsung ke daemon service, yang kemudian dieksekusi secara instan oleh `OverlayManager` di thread utama.

---

## 3. Detail AIDL Interface (`IOverlayService.aidl`)

| Metode | Deskripsi |
| :--- | :--- |
| `void showText(String text)` | Menampilkan floating window dengan terminal output monospace. |
| `void hide()` | Menghapus floating window dari `WindowManager`. |
| `void updateText(String text)` | Memperbarui teks overlay secara live tanpa merusak view. |
| `void showButton(String label)` | Menampilkan tombol mengambang interaktif. |
| `void showImage(String path)` | Membaca file gambar dari path lokal dan merendernya di overlay. |
| `boolean isShowing()` | Mengembalikan true jika overlay sedang aktif di layar. |
| `void setPosition(int x, int y)` | Mengatur koordinat offset floating window. |
| `void setAlpha(float alpha)` | Mengatur tingkat transparansi jendela (0.1 - 1.0). |

---

## 4. Keamanan & Signature Verification

1. **UID Isolation**:
   Setiap aplikasi Android dan Termux berjalan di bawah Linux UID yang terisolasi (misalnya `u0_a185`).
2. **Permission Check**:
   Hanya aplikasi yang telah diberi izin khusus oleh pengguna Android (`SYSTEM_ALERT_WINDOW`) yang diizinkan memanggil `windowManager.addView(..., TYPE_APPLICATION_OVERLAY)`.
3. **Pemberian Hak Akses Termux**:
   - Tidak memerlukan ROOT.
   - Menggunakan IPC internal yang diizinkan dalam lingkup profil pengguna Android (`user 0`).
4. **Verifikasi Signature (Opsional)**:
   Pada produksi tingkat tinggi, `OverlayBinderService` dapat memverifikasi signature package pemanggil melalui `packageManager.getPackageInfo(callingPackage, GET_SIGNING_CERTIFICATES)` untuk memastikan hanya Termux atau client terotorisasi yang dapat mengirim perintah.

---

## 5. Overlay Window Lifecycle

1. **Inisialisasi**: Saat perintah pertama masuk, `ensureOverlayFrameCreated()` memverifikasi izin `Settings.canDrawOverlays(context)`.
2. **Layout Parameters**:
   - `TYPE_APPLICATION_OVERLAY` menjamin kompatibilitas dari Android 8.0 (API 26) hingga Android 14/15/16.
   - `FLAG_NOT_FOCUSABLE` memungkinkan pengguna tetap berinteraksi dengan aplikasi lain di bawah overlay tanpa keyboard terblokir.
3. **Draggable Engine**:
   - Menggunakan `View.OnTouchListener` pada title bar/header.
   - Menghitung delta pergeseran layar `dx` dan `dy` secara instan dan memperbarui koordinat melalui `windowManager.updateViewLayout()`.
