# Panduan Pembuatan Keystore & Signing Release APK

Panduan resmi untuk membuat custom keystore, menandatangani APK release, dan memverifikasi integritas signature pada project Termux Overlay API.

---

## 1. Membuat Keystore Sendiri

Gunakan utilitas `keytool` bawaan Java JDK:

```bash
keytool -genkey -v \
  -keystore my-release-key.jks \
  -alias termuxoverlay \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Parameter yang Digunakan:
- `-keystore`: Nama file output keystore (contoh: `my-release-key.jks`).
- `-alias`: Nama alias kunci penandatanganan (contoh: `termuxoverlay`).
- `-keyalg`: Algoritma enkripsi kriptografi (`RSA`).
- `-keysize`: Panjang kunci 2048-bit yang aman dan direkomendasikan Google Play.
- `-validity`: Masa berlaku kunci dalam hari (10.000 hari ≈ 27 tahun).

Masukkan password yang kuat saat diminta, serta data identitas pengembang (CN, OU, O, L, ST, C).

---

## 2. Signing Release APK

### Metode A: Menggunakan Gradle Otomatis (Direkomendasikan)

Ekspor environment variable kredensial keystore:

```bash
export KEYSTORE_PATH="/path/ke/my-release-key.jks"
export STORE_PASSWORD="password_keystore_anda"
export KEY_PASSWORD="password_key_anda"
```

Jalankan perintah build release:

```bash
./gradlew assembleRelease
```

Hasil APK release yang telah ditandatangani akan berada di:
`app/build/outputs/apk/release/app-release.apk`

---

### Metode B: Signing Manual Menggunakan `apksigner`

Jika ingin menandatangani APK yang belum ditandatangani secara manual:

1. **Zipalign APK**:
   ```bash
   zipalign -v -p 4 app-release-unsigned.apk app-release-aligned.apk
   ```

2. **Tandatangani Menggunakan apksigner**:
   ```bash
   apksigner sign --ks my-release-key.jks \
     --ks-key-alias termuxoverlay \
     --out app-release-signed.apk \
     app-release-aligned.apk
   ```

---

## 3. Verifikasi Signature APK

Untuk memastikan bahwa APK telah ditandatangani dengan skema signature modern (v2, v3, v4) dan tidak cacat:

```bash
apksigner verify --verbose --print-certs app-release.apk
```

**Contoh Output Berhasil:**
```text
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Verified using v4 scheme: false
Number of signers: 1
Signer #1 certificate DN: CN=Termux Developer, O=OpenSource, C=ID
Signer #1 key algorithm: RSA
Signer #1 key size (bits): 2048
Signer #1 SHA-256 digest: 8f4a3b...
```

Untuk melihat fingerprint sertifikat secara langsung:
```bash
keytool -printcert -jarfile app-release.apk
```
