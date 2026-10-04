### 1. Siapkan repo penampung data

1. Buka https://github.com/new
2. Isi **Repository name**, misalnya `soaldulu-data`.
3. Pilih **Private**, karena isinya data responden.
4. Centang **Add a README file**. Ini wajib, karena repo kosong belum punya branch `main` dan pengiriman akan gagal.
5. Klik **Create repository**.

### 2. Buat token

1. Buka https://github.com/settings/personal-access-tokens/new

   Jalur manualnya: foto profil kanan atas → **Settings** → **Developer settings** → **Personal access tokens** → **Fine-grained tokens** → **Generate new token**.
2. **Token name:** misalnya `soaldulu-uji-coba`.
3. **Resource owner:** pilih akun pribadimu.
4. **Expiration:** pilih tanggal tidak lama setelah uji coba selesai.
5. **Repository access:** pilih **Only select repositories**, lalu pilih repo data tadi saja.
6. **Permissions** → **Repository permissions** → cari **Contents**, pilih **Read and write**. Biarkan izin lain **No access**. Baris **Metadata** otomatis jadi *Read-only*, itu wajar.
7. Klik **Generate token**.
8. Salin tokennya sekarang juga. Bentuknya `github_pat_` diikuti huruf acak. GitHub hanya menampilkannya sekali.

---

## Isi file yang tidak ikut ke GitHub

Beberapa file sengaja tidak di-upload karena terdaftar di `.gitignore`. Hampir semuanya dibuat otomatis oleh Android Studio dan Gradle, jadi tidak perlu dibuat sendiri:

| File atau folder | Isinya | Perlu dibuat sendiri? |
|---|---|---|
| `local.properties` | Lokasi Android SDK dan token GitHub | Ya, kalau mau memakai fitur kirim laporan |
| `.gradle/`, `build/`, `app/build/` | Hasil build | Tidak, dibuat saat build |
| `*.iml`, `.idea/workspace.xml`, `.idea/caches/` dan sejenisnya | Pengaturan Android Studio di laptop masing-masing | Tidak, dibuat Android Studio |
| `captures/`, `.externalNativeBuild/`, `.cxx/` | File sementara Android Studio | Tidak |

### local.properties

File ini ada di folder paling luar proyek, sejajar dengan `settings.gradle.kts`. Android Studio biasanya sudah membuatnya dengan baris `sdk.dir=...`. Jangan hapus baris itu, cukup tambahkan tiga baris di bawahnya:

```properties
sdk.dir=C\:\\Users\\NAMAPENGGUNA\\AppData\\Local\\Android\\Sdk

# Tujuan pengiriman laporan penelitian.
soaldulu.github.repo=NAMAAKUN/soaldulu-data
soaldulu.github.token=github_pat_xxxxxxxxxxxxxxxxxxxxxxxx
soaldulu.github.branch=main
```

| Kunci | Isinya |
|---|---|
| `sdk.dir` | Lokasi Android SDK. Diisi otomatis oleh Android Studio |
| `soaldulu.github.repo` | Nama akun dan repo data, ditulis `akun/repo`, bukan `https://...` |
| `soaldulu.github.token` | Token dari langkah di atas |
| `soaldulu.github.branch` | Nama branch repo data, biasanya `main` |

Aturan penulisan:

- Tanpa tanda kutip.
- Tanpa spasi sebelum dan sesudah tanda `=`.
- Tidak ada spasi tertinggal di ujung baris.

Setelah disimpan, build ulang APK-nya. Token dibaca saat build, jadi APK lama tidak ikut berubah. Kalau berhasil, di layar **Settings** aplikasi muncul baris **Kirim catatan ke peneliti**.

Panduan yang lebih rinci, termasuk cara menguji pengiriman dan arti pesan gagal, ada di [docs/kirim-data-ke-github.md](docs/kirim-data-ke-github.md).

---

## Setelah uji coba selesai

Hapus tokennya di https://github.com/settings/personal-access-tokens → klik nama token → **Delete**. Setelah itu APK yang sudah dibagikan tidak bisa mengirim apa pun lagi.

Jangan pernah meng-upload APK yang masih membawa token aktif, baik ke repo maupun ke Releases.
