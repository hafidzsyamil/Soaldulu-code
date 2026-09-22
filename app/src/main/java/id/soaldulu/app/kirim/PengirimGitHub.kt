package id.soaldulu.app.kirim

import android.content.Context
import android.util.Base64
import id.soaldulu.app.BuildConfig
import id.soaldulu.app.GateConfig
import id.soaldulu.app.SpikeLog
import id.soaldulu.app.data.SoalduluRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Mengirim laporan responden ke satu berkas JSON di repo GitHub peneliti.
 *
 * Memakai HttpURLConnection dan org.json bawaan Android, tanpa pustaka HTTP
 * tambahan: hanya ada dua permintaan, dan keduanya sederhana.
 *
 * PENTING — token ikut terpasang di dalam APK dan bisa dibaca siapa pun yang
 * memegang berkas APK-nya. Karena itu tokennya harus fine-grained, hanya
 * untuk satu repo khusus data, hanya izin Contents: Read and write, dan
 * kedaluwarsa setelah uji coba selesai. Lihat docs/kirim-data-ke-github.md.
 *
 * Satu HP menulis satu berkas miliknya sendiri, jadi lima responden tidak
 * pernah bertabrakan. Daftar indeks seluruh responden dibangun di repo data
 * oleh GitHub Actions, bukan oleh aplikasi.
 */
object PengirimGitHub {

    sealed interface Hasil {
        data class Berhasil(val path: String, val byte: Int) : Hasil
        data class Gagal(val pesan: String) : Hasil
    }

    suspend fun kirim(
        context: Context,
        repo: SoalduluRepository,
        nama: String,
        versiAplikasi: String,
    ): Hasil = withContext(Dispatchers.IO) {
        if (!StatusKirim.disetel) return@withContext gagal(context, "Repo tujuan belum disetel")
        if (!StatusKirim.aktif(context)) return@withContext gagal(context, "Pengiriman dimatikan")

        try {
            val laporan = Laporan.susun(context, repo, nama, versiAplikasi)
            val isi = laporan.toString(2).toByteArray(Charsets.UTF_8)
            val path = "data/${berkasUntuk(context, nama)}"

            val sha = shaBerkas(path)
            val badan = JSONObject().apply {
                put("message", "Laporan $nama · ${laporan.optString("dikirimPada")}")
                put("content", Base64.encodeToString(isi, Base64.NO_WRAP))
                put("branch", BuildConfig.GITHUB_BRANCH)
                if (sha != null) put("sha", sha)
            }

            val (kode, balasan) = permintaan("PUT", path, badan.toString())
            if (kode !in 200..299) {
                return@withContext gagal(context, "GitHub menolak ($kode): ${ringkas(balasan)}")
            }

            StatusKirim.catatHasil(context, berhasil = true, pesan = "${isi.size} byte terkirim")
            SpikeLog.tulis(context, "KIRIM_BERHASIL path=$path byte=${isi.size}")
            Hasil.Berhasil(path, isi.size)
        } catch (e: Exception) {
            gagal(context, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Nama berkas per HP: nama responden + id pemasangan, supaya tidak saling menimpa. */
    private fun berkasUntuk(context: Context, nama: String): String {
        val bersih = nama.lowercase()
            .map { if (it.isLetterOrDigit()) it else '-' }
            .joinToString("")
            .trim('-')
            .take(24)
            .ifBlank { "responden" }
        return "$bersih-${StatusKirim.idPerangkat(context)}.json"
    }

    /** sha berkas yang sudah ada; null kalau belum pernah dikirim. */
    private fun shaBerkas(path: String): String? {
        val (kode, balasan) = permintaan("GET", "$path?ref=${BuildConfig.GITHUB_BRANCH}", null)
        if (kode == HttpURLConnection.HTTP_NOT_FOUND) return null
        if (kode !in 200..299) return null
        return JSONObject(balasan).optString("sha").ifBlank { null }
    }

    private fun permintaan(metode: String, path: String, badan: String?): Pair<Int, String> {
        val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_REPO}/contents/$path")
        val koneksi = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = metode
            connectTimeout = GateConfig.KIRIM_TIMEOUT_SECONDS * 1000
            readTimeout = GateConfig.KIRIM_TIMEOUT_SECONDS * 1000
            setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Soaldulu")
            if (badan != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            badan?.let { koneksi.outputStream.use { aliran -> aliran.write(it.toByteArray()) } }
            val kode = koneksi.responseCode
            val teks = (if (kode in 200..299) koneksi.inputStream else koneksi.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            kode to teks
        } finally {
            koneksi.disconnect()
        }
    }

    private fun gagal(context: Context, pesan: String): Hasil {
        StatusKirim.catatHasil(context, berhasil = false, pesan = pesan)
        SpikeLog.tulis(context, "KIRIM_GAGAL $pesan")
        return Hasil.Gagal(pesan)
    }

    private fun ringkas(balasan: String): String =
        JSONObject(runCatching { balasan }.getOrDefault("{}"))
            .let { runCatching { it.optString("message") }.getOrDefault("") }
            .ifBlank { balasan.take(120) }
}
