package id.soaldulu.app.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import id.soaldulu.app.GateConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Menulis seluruh log penelitian ke satu berkas CSV.
 *
 * Satu berkas dengan pemisah bagian, bukan tiga berkas terpisah — responden
 * cukup mengirim satu lampiran lewat WhatsApp.
 *
 * Tiap baris waktu ditulis dua kali: epoch untuk diolah, dan teks yang bisa
 * dibaca manusia untuk diperiksa sekilas.
 */
object EksporCsv {

    private val waktuBaca = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    suspend fun tulis(context: Context, repo: SoalduluRepository, kode: String): File {
        val paket = repo.paketTerpasang()
        val jawaban = repo.semuaJawaban()
        val kredit = repo.semuaKredit()
        val peristiwa = repo.semuaPeristiwa()

        val sb = StringBuilder()

        sb.appendLine("# SOALDULU — EKSPOR LOG PENELITIAN")
        sb.appendLine("# kode_responden,${csv(kode)}")
        sb.appendLine("# versi_paket,${csv(paket?.version.orEmpty())}")
        sb.appendLine("# paket,${csv(paket?.packageName.orEmpty())}")
        sb.appendLine("# diekspor,${csv(waktuBaca.format(Date()))}")
        sb.appendLine("# jumlah_jawaban,${jawaban.size}")
        sb.appendLine("# jumlah_sesi_kredit,${kredit.size}")
        sb.appendLine("# jumlah_peristiwa,${peristiwa.size}")
        sb.appendLine()

        sb.appendLine("## BAGIAN 1 — LOG JAWABAN")
        sb.appendLine(
            "id,respondentCode,packageVersion,gateSessionId,urutanDalamGerbang,itemId," +
                "subtest,selectedOptionId,isCorrect,durationSeconds,creditEarnedSeconds," +
                "triggeredByPackage,timestamp,waktu"
        )
        jawaban.forEach { b ->
            sb.appendLine(
                listOf(
                    b.id, b.respondentCode, b.packageVersion, b.gateSessionId,
                    b.urutanDalamGerbang, b.itemId, b.subtest, b.selectedOptionId,
                    if (b.isCorrect) 1 else 0, b.durationSeconds, b.creditEarnedSeconds,
                    b.triggeredByPackage, b.timestamp, waktuBaca.format(Date(b.timestamp)),
                ).joinToString(",") { csv(it.toString()) }
            )
        }
        sb.appendLine()

        sb.appendLine("## BAGIAN 2 — LOG SESI KREDIT")
        sb.appendLine(
            "id,respondentCode,gateSessionId,creditGrantedSeconds,creditStartedAt," +
                "waktuMulai,creditEndedAt,waktuSelesai,endReason"
        )
        kredit.forEach { b ->
            sb.appendLine(
                listOf(
                    b.id, b.respondentCode, b.gateSessionId, b.creditGrantedSeconds,
                    b.creditStartedAt, waktuBaca.format(Date(b.creditStartedAt)),
                    b.creditEndedAt ?: "",
                    b.creditEndedAt?.let { waktuBaca.format(Date(it)) } ?: "",
                    b.endReason ?: "",
                ).joinToString(",") { csv(it.toString()) }
            )
        }
        sb.appendLine()

        sb.appendLine("## BAGIAN 3 — LOG PERISTIWA SISTEM")
        sb.appendLine("id,respondentCode,eventType,detail,timestamp,waktu")
        peristiwa.forEach { b ->
            sb.appendLine(
                listOf(
                    b.id, b.respondentCode, b.eventType, b.detail, b.timestamp,
                    waktuBaca.format(Date(b.timestamp)),
                ).joinToString(",") { csv(it.toString()) }
            )
        }

        val folder = File(context.filesDir, "ekspor").apply { mkdirs() }
        val berkas = File(
            folder,
            GateConfig.EXPORT_FILENAME_TEMPLATE.format(kode.ifBlank { "tanpa-kode" }),
        )
        berkas.writeText(sb.toString())
        return berkas
    }

    /**
     * Bungkus nilai yang mengandung koma, tanda kutip, atau baris baru.
     * Kolom detail peristiwa bisa berisi apa saja, jadi ini bukan kehati-hatian
     * berlebihan — tanpa ini satu koma bisa menggeser seluruh kolom.
     */
    private fun csv(nilai: String): String =
        if (nilai.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + nilai.replace("\"", "\"\"") + "\""
        } else {
            nilai
        }

    /** Buka lembar berbagi sistem. Respondenlah yang memilih tujuannya. */
    fun intentBagikan(context: Context, berkas: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            berkas,
        )
        val kirim = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, berkas.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(kirim, "Kirim log ke peneliti")
    }
}
