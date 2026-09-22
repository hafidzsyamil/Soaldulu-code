# Mengirim catatan responden ke GitHub

Aplikasi mengirim laporan tiap responden ke satu repo GitHub milik peneliti,
supaya tidak perlu meminta berkas CSV satu per satu. Dokumen ini langkah
demi langkah; ikuti berurutan.

Waktu yang dibutuhkan sekitar 15 menit, sekali saja.

---

## Yang perlu kamu tahu lebih dulu

**Token GitHub ikut terpasang di dalam APK, dan bisa dibaca siapa pun yang
memegang berkas APK-nya.** Tidak ada cara mengirim ke GitHub tanpa menaruh
kredensial di aplikasi. Karena itu:

- Pakai repo **khusus data ini saja**. Jangan pakai repo yang sudah berisi
  hal lain.
- Repo-nya **private**.
- Tokennya **fine-grained**, hanya untuk repo itu, hanya izin *Contents*.
- Beri **tanggal kedaluwarsa** sekitar seminggu setelah uji coba selesai.
- **Cabut tokennya** setelah uji coba (Langkah 8).

Kalau tokennya bocor, yang bisa disentuh hanya repo data itu: orang bisa
membaca, menulis, atau menghapus isinya. Tidak bisa menyentuh repo lain
atau akun GitHub-mu.

---

## Langkah 1 — Buat repo data

1. Buka <https://github.com/new>
2. **Repository name:** `soaldulu-data`
3. Pilih **Private**
4. Centang **Add a README file** — ini penting. Repo yang benar-benar kosong
   belum punya branch `main`, dan aplikasi akan gagal mengirim.
5. Klik **Create repository**

Catat nama lengkapnya, bentuknya `namaakunmu/soaldulu-data`.

## Langkah 2 — Buat token

1. Buka <https://github.com/settings/personal-access-tokens/new>
   (Settings → Developer settings → Personal access tokens → Fine-grained tokens)
2. **Token name:** `soaldulu-uji-coba`
3. **Expiration:** pilih tanggal sekitar seminggu setelah uji coba selesai
4. **Repository access:** pilih **Only select repositories**, lalu pilih
   `soaldulu-data`
5. **Repository permissions** → cari **Contents** → ubah menjadi
   **Read and write**

   Biarkan izin lainnya *No access*. *Metadata: Read-only* akan tercentang
   sendiri, itu wajar.
6. Klik **Generate token**
7. **Salin tokennya sekarang** — GitHub hanya menampilkannya satu kali.
   Bentuknya diawali `github_pat_`.

## Langkah 3 — Masukkan ke local.properties

Buka `C:\Users\Hafid\Project\Soaldulu\local.properties`, tambahkan tiga baris
di bawah isi yang sudah ada:

```properties
soaldulu.github.repo=namaakunmu/soaldulu-data
soaldulu.github.token=github_pat_xxxxxxxxxxxxxxxxxxxx
soaldulu.github.branch=main
```

Ganti `namaakunmu` dan tokennya dengan milikmu sendiri.

Berkas ini **tidak ikut git** (sudah ada di `.gitignore`), jadi tokenmu tidak
akan pernah ter-commit. Jangan menaruh token di berkas lain.

## Langkah 4 — Bangun ulang APK

```
./gradlew assembleDebug
```

APK-nya ada di `app/build/outputs/apk/debug/app-debug.apk`.

Kalau ketiga baris tadi kosong atau salah tulis, fitur kirim mati sendiri dan
aplikasi tetap berjalan normal — barisnya tidak muncul di Settings.

## Langkah 5 — Uji dari HP-mu sendiri

1. Pasang APK, buka aplikasinya
2. Masuk **Settings**
3. Pastikan ada baris **Kirim catatan ke peneliti** dan sakelarnya menyala
4. Tekan **Kirim sekarang**
5. Tunggu beberapa detik, lalu tarik ke bawah — keterangannya berubah jadi
   `Terkirim <tanggal jam>`
6. Buka repo `soaldulu-data` di GitHub. Harus ada berkas
   `data/<nama>-<8 huruf acak>.json`

Kalau gagal, keterangannya berisi sebabnya. Yang sering:

| Pesan | Sebabnya |
|---|---|
| `GitHub menolak (401)` | Token salah tulis, atau sudah kedaluwarsa |
| `GitHub menolak (404)` | Nama repo salah, atau token tidak diberi akses ke repo itu |
| `GitHub menolak (403)` | Izin *Contents* belum *Read and write* |
| `GitHub menolak (409)` atau `(422)` | Branch `main` belum ada — ulangi Langkah 1 nomor 4 |
| `UnknownHostException` | HP sedang tidak ada internet |

## Langkah 6 — Pasang ke HP responden

APK yang sama dipasang ke lima HP responden. Tiap HP menulis berkasnya
sendiri, jadi tidak akan saling menimpa walaupun ada dua responden bernama
sama.

Pengiriman berjalan otomatis: paling sering sekali per jam, dijalankan
layanan gerbang yang memang sudah hidup terus, dan juga setiap responden
membuka aplikasi. Responden tidak perlu melakukan apa pun.

## Langkah 7 — (Disarankan) daftar indeks otomatis

Supaya kamu punya satu berkas ringkasan semua responden, pasang GitHub
Actions di repo data:

1. Di repo `soaldulu-data`, klik **Add file → Create new file**
2. Nama berkas: `.github/workflows/indeks.yml`
3. Isi dengan isi berkas [`github-workflow-index.yml`](github-workflow-index.yml)
   yang ada di folder ini
4. **Commit**

Setiap kali ada laporan baru masuk, Actions memperbarui `index.json` di akar
repo. Isinya satu baris per responden: nama, jumlah soal, persen benar,
menit mengerjakan soal, menit memakai media sosial, jumlah gerbang, berapa
kali tiap aplikasi dibuka, dan berapa hari aktif.

Kalau langkah ini dilewati, datanya tetap lengkap di `data/*.json`; kamu
hanya tidak punya ringkasan gabungannya.

## Langkah 8 — Setelah uji coba selesai

1. Cabut tokennya: <https://github.com/settings/personal-access-tokens> →
   pilih `soaldulu-uji-coba` → **Revoke**
2. Unduh isi repo (**Code → Download ZIP**) untuk arsip skripsimu
3. Repo boleh dibiarkan private, atau dihapus setelah datanya diarsipkan

---

## Isi berkas laporan

Satu berkas per responden, di `data/`:

| Bagian | Isi |
|---|---|
| `responden` | Nama yang diisi responden, dan id pemasangan |
| `perangkat` | Merek, model, versi Android |
| `aplikasi` | Versi aplikasi, versi bank soal, jumlah butir aktif |
| `ringkasan` | Semua angka yang biasanya kamu butuhkan, lihat di bawah |
| `pemakaianAplikasiDetik` | Berapa detik tiap aplikasi benar-benar dipakai |
| `izinSaatIni` | Empat izin, menyala atau tidak |
| `jawaban` | Seluruh baris jawaban, satu per soal |
| `sesiKredit` | Setiap pemberian kredit dan kapan habisnya |
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

## Kalau ada responden yang mematikan pengiriman

Sakelar di Settings boleh dimatikan responden kapan saja — persetujuan yang
tidak bisa ditarik bukan persetujuan. Kalau itu terjadi:

- Laporan yang sudah masuk tetap ada
- Laporan baru berhenti terkirim
- Peristiwa `SENDING_DISABLED` tercatat di laporan terakhirnya
- Kamu masih bisa meminta responden itu mengekspor CSV lewat Settings
