# Mengirim catatan responden ke GitHub — panduan rinci

Aplikasi mengirim laporan tiap responden ke satu repo GitHub milikmu, supaya
kamu tidak perlu meminta berkas CSV satu per satu.

Dikerjakan sekali, sekitar 15 menit. Ikuti berurutan, jangan melompat.

Tampilan GitHub berbahasa Inggris, jadi nama tombol di bawah ditulis apa
adanya dalam bahasa Inggris.

---

## Sebelum mulai

- Punya akun GitHub dan sudah login di peramban.
- Proyek Soaldulu ada di `C:\Users\Hafid\Project\Soaldulu`.

**Yang harus kamu tahu:** token GitHub ikut terpasang di dalam APK dan bisa
dibaca siapa pun yang memegang berkas APK-nya. Tidak ada cara mengirim ke
GitHub tanpa menaruh kredensial di aplikasi. Panduan ini membatasi
akibatnya: repo khusus, izin paling sempit, dan tokennya dicabut setelah
uji coba.

---

# LANGKAH 1 — Buat repo penampung data

1. Buka <https://github.com/new>
2. **Owner:** biarkan akun pribadimu.
3. **Repository name:** ketik `soaldulu-data`
4. **Description:** boleh dikosongkan.
5. Pilih **Private**. Jangan Public — isinya nama dan catatan responden.
6. Di bagian *Initialize this repository with*, **centang `Add a README file`**.

   Ini wajib. Repo yang benar-benar kosong belum punya branch `main`, dan
   pengiriman dari aplikasi akan gagal dengan pesan 404 atau 409.
7. *Add .gitignore* dan *Choose a license*: biarkan **None**.
8. Klik tombol hijau **Create repository**.

Setelah jadi, kamu berada di halaman repo. Perhatikan dua hal:

- Alamat di peramban: `https://github.com/NAMAAKUN/soaldulu-data`.
  **Catat `NAMAAKUN/soaldulu-data`** — nanti dipakai di Langkah 3.
- Di kiri atas daftar berkas ada tombol bertuliskan **`main`**. Itu nama
  branch-nya. Kalau tertulis `master`, catat itu, nanti dipakai di Langkah 3.

---

# LANGKAH 2 — Buat token

1. Buka <https://github.com/settings/personal-access-tokens/new>

   Jalur manualnya: klik foto profil kanan atas → **Settings** → gulir ke
   bawah, menu kiri paling bawah **Developer settings** → **Personal access
   tokens** → **Fine-grained tokens** → tombol **Generate new token**.
2. **Token name:** ketik `soaldulu-uji-coba`
3. **Resource owner:** pilih **akun pribadimu** (bukan organisasi, kalau ada).
4. **Expiration:** pilih tanggal sekitar seminggu setelah uji coba selesai.
   Kalau pilihannya terbatas, pilih **Custom** lalu tentukan tanggalnya.
5. **Description:** boleh dikosongkan.
6. **Repository access:** pilih **Only select repositories**.

   Muncul kotak **Select repositories** → klik → cari dan pilih
   **`soaldulu-data`**. Pastikan hanya repo itu yang terpilih.
7. **Permissions** → buka bagian **Repository permissions**.

   Daftarnya panjang. Cari baris **Contents** (urut abjad, di bagian atas).
   Klik dropdown di kanannya, pilih **Read and write**.

   Biarkan semua izin lain **No access**. Baris **Metadata** akan berubah
   sendiri menjadi *Read-only* dan tidak bisa dimatikan — itu wajar dan
   memang diperlukan.
8. Gulir ke paling bawah, klik **Generate token**.
9. Tokennya tampil sekali di layar dengan latar hijau, bentuknya
   `github_pat_` diikuti huruf acak panjang. Klik ikon salin di sebelahnya.

   **Salin sekarang juga.** Setelah halaman ini ditutup, GitHub tidak akan
   menampilkannya lagi. Kalau telanjur tertutup, ulangi Langkah 2 dari awal
   untuk membuat token baru.
10. Tempel sementara di Notepad kalau perlu, jangan di tempat yang ikut
    terkirim ke orang lain.

---

# LANGKAH 3 — Masukkan ke local.properties

1. Buka berkas `C:\Users\Hafid\Project\Soaldulu\local.properties`
   dengan Notepad atau Android Studio.
2. Isinya sudah ada satu baris `sdk.dir=...`. **Jangan dihapus.**
3. Tambahkan tiga baris di bawahnya:

```properties
soaldulu.github.repo=NAMAAKUN/soaldulu-data
soaldulu.github.token=github_pat_xxxxxxxxxxxxxxxxxxxxxxxx
soaldulu.github.branch=main
```

4. Ganti `NAMAAKUN` dengan nama akun GitHub-mu, dan tokennya dengan yang
   tadi disalin.
5. Kalau nama branch di Langkah 1 ternyata `master`, tulis `master`.

Aturan penulisan, sering jadi penyebab gagal:

- **Tanpa tanda kutip** di sekeliling nilainya.
- **Tanpa spasi** sebelum dan sesudah tanda `=`.
- Pastikan tidak ada spasi tertinggal di ujung baris.
- Nama repo ditulis lengkap `akun/repo`, bukan alamat `https://...`.

6. Simpan berkasnya.

Berkas ini sudah terdaftar di `.gitignore`, jadi tokenmu tidak akan pernah
ikut ter-commit.

---

# LANGKAH 4 — Bangun ulang APK

Token dibaca saat APK dibangun, jadi APK lama tidak akan mengirim apa pun.

Lewat terminal, di folder proyek:

```
./gradlew assembleDebug
```

Atau di Android Studio: menu **Build** → **Rebuild Project**, lalu **Run**.

Hasilnya: `app/build/outputs/apk/debug/app-debug.apk`

---

# LANGKAH 5 — Uji dari HP-mu sendiri

1. Pasang APK-nya ke HP, lalu buka aplikasinya.
2. Masuk **Settings**.
3. Harus muncul baris baru **Kirim catatan ke peneliti** dengan sakelar
   menyala.

   **Kalau barisnya tidak muncul:** ketiga baris di Langkah 3 belum terbaca.
   Periksa ejaannya, simpan, lalu ulangi Langkah 4.
4. Tekan baris **Kirim sekarang**.
5. Tunggu beberapa detik. Keterangan di baris **Kirim catatan ke peneliti**
   berubah menjadi `Terkirim <tanggal> <jam>`.

   Kalau gagal, keterangannya berisi sebabnya — lihat tabel di bawah.
6. Buka repo `soaldulu-data` di GitHub, muat ulang halamannya. Harus ada
   folder **`data`** berisi berkas seperti
   `syamil-3f9a2b1c.json`.
7. Klik berkas itu. Di bagian atas ada `"ringkasan"` berisi angka-angkanya.

## Kalau gagal

| Pesan di HP | Sebabnya | Perbaikannya |
|---|---|---|
| Baris Settings tidak muncul | local.properties belum terbaca | Periksa ejaan, bangun ulang APK |
| `Repo tujuan belum disetel` | Sama seperti di atas | Ulangi Langkah 3 dan 4 |
| `GitHub menolak (401)` | Token salah salin, atau sudah kedaluwarsa | Buat token baru, Langkah 2 |
| `GitHub menolak (403)` | Izin **Contents** belum *Read and write* | Perbaiki izin token, Langkah 2 nomor 7 |
| `GitHub menolak (404)` | Nama repo salah, atau token tidak diberi akses ke repo itu | Periksa Langkah 3 baris `repo`, dan Langkah 2 nomor 6 |
| `GitHub menolak (409)` / `(422)` | Branch tidak ada — repo dibuat tanpa README | Buat berkas apa pun di repo, atau ulangi Langkah 1 |
| `UnknownHostException` | HP sedang tanpa internet | Nyalakan data, tekan Kirim sekarang lagi |
| `SocketTimeoutException` | Jaringan lambat | Coba lagi; pengiriman otomatis juga akan mengulang |

---

# LANGKAH 6 — Pasang daftar indeks otomatis

Supaya ada satu berkas ringkasan semua responden, bukan lima berkas terpisah.

1. Buka repo `soaldulu-data` di GitHub.
2. Klik tombol **Add file** (kanan atas daftar berkas) → **Create new file**.
3. Di kotak nama berkas, ketik persis:

   ```
   .github/workflows/indeks.yml
   ```

   Saat kamu mengetik tanda `/`, GitHub otomatis membuat foldernya.
4. Buka berkas `docs/github-workflow-index.yml` di proyek Soaldulu, salin
   **seluruh isinya**, tempel ke kotak editor di GitHub.
5. Gulir ke bawah, klik **Commit changes...** → **Commit changes**.
6. Klik tab **Actions** di atas. Akan ada pekerjaan berjalan bernama
   *Susun indeks responden*. Tunggu sampai centang hijau.
7. Kembali ke tab **Code**. Sekarang ada berkas **`index.json`** di akar repo.

Isi `index.json`: satu baris per responden — nama, jumlah soal dikerjakan,
persen benar, menit mengerjakan soal, menit memakai media sosial, jumlah
gerbang, berapa kali tiap aplikasi dibuka, hari aktif, dan kapan terakhir
mengirim. Diperbarui sendiri setiap ada laporan baru masuk.

---

# LANGKAH 7 — Bagikan ke responden

APK yang sama dipasang ke lima HP responden.

- Tiap HP menulis berkasnya sendiri (`nama-idperangkat.json`), jadi dua
  responden bernama sama tidak akan saling menimpa.
- Pengiriman berjalan sendiri: paling sering sekali per jam oleh layanan
  gerbang yang memang hidup terus, dan juga setiap responden membuka
  aplikasi. Responden tidak perlu melakukan apa pun.
- Kalau HP responden sedang tanpa internet, pengiriman berikutnya mengejar
  ketertinggalan — isi laporannya selalu seluruh catatan dari awal, bukan
  hanya yang baru.

---

# LANGKAH 8 — Memantau selama uji coba

- Buka `index.json` di repo untuk melihat kemajuan kelima responden
  sekaligus.
- Kolom `aktivitasTerakhir` dan `dikirimPada` menunjukkan HP mana yang
  berhenti mengirim.
- Kalau ada responden yang tidak muncul lebih dari sehari, kemungkinan:
  layanan gerbangnya mati (minta dia membuka aplikasinya), HP-nya tanpa
  internet, atau dia mematikan sakelar pengiriman.

---

# LANGKAH 9 — Setelah uji coba selesai

1. **Unduh datanya:** di repo, tombol hijau **Code** → **Download ZIP**.
   Simpan untuk lampiran KTI.
2. **Cabut tokennya:** <https://github.com/settings/personal-access-tokens>
   → klik `soaldulu-uji-coba` → **Revoke** → konfirmasi.

   Setelah dicabut, APK yang beredar tidak bisa menulis apa pun lagi.
3. Repo boleh dibiarkan private, atau dihapus setelah datanya diarsipkan:
   **Settings** → gulir paling bawah → **Delete this repository**.

---

# Isi berkas laporan

Satu berkas per responden di folder `data/`:

| Bagian | Isi |
|---|---|
| `responden` | Nama yang diisi responden, id pemasangan |
| `perangkat` | Merek, model, versi Android |
| `aplikasi` | Versi aplikasi, versi bank soal, jumlah butir aktif |
| `ringkasan` | Angka-angka siap pakai, lihat daftar di bawah |
| `pemakaianAplikasiDetik` | Berapa detik tiap aplikasi benar-benar dipakai |
| `izinSaatIni` | Empat izin, menyala atau tidak |
| `aplikasiDipantau` | Daftar aplikasi yang dijaga di HP itu |
| `jawaban` | Seluruh baris jawaban, satu per soal |
| `sesiKredit` | Tiap pemberian kredit dan kapan habisnya |
| `peristiwa` | Aplikasi dibuka, mode darurat, izin dicabut, dan lainnya |

Isi `ringkasan`:

- `soalDikerjakan`, `jawabanBenar`, `jawabanSalah`, `persenBenar`
- `totalMenitMengerjakanSoal`, `rataRataDetikPerSoal`
- `totalMenitPakaiAplikasi` — berapa menit media sosial benar-benar dipakai
- `aplikasiDibukaBerapaKali` — berapa kali tiap aplikasi dibuka
- `jumlahGerbang`, `gerbangDipicuMedsos`, `gerbangDibukaSendiri`,
  `gerbangPerAplikasi`
- `kreditDidapatMenit`, `sisaSaldoDetik`
- `perSubtes` — dijawab dan benar untuk tiap subtes
- `hariAktif`, `tanggalAktif`, `aktivitasPertama`, `aktivitasTerakhir`
- `modeDaruratDipakai`, `aplikasiDitambah`, `aplikasiDihapus`,
  `aplikasiDimatikan`, `serviceRestart`, `soalDilaporkan`, `perambanDibuka`

---

# Kalau responden mematikan pengiriman

Sakelar di Settings boleh dimatikan responden kapan saja — persetujuan yang
tidak bisa ditarik bukan persetujuan. Kalau itu terjadi:

- Laporan yang sudah masuk tetap ada.
- Laporan baru berhenti terkirim.
- Peristiwa `SENDING_DISABLED` tercatat di laporan terakhirnya, jadi kamu
  tahu kapan berhentinya.
- Kamu masih bisa meminta responden itu mengekspor CSV lewat Settings.
