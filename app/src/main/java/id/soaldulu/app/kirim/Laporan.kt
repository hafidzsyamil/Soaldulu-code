package id.soaldulu.app.kirim

import android.content.Context
import android.os.Build
import id.soaldulu.app.DaftarAplikasi
import id.soaldulu.app.Pemakaian
import id.soaldulu.app.SaldoKredit
import id.soaldulu.app.bacaStatusIzin
import id.soaldulu.app.data.LogJawabanEntity
import id.soaldulu.app.data.LogKreditEntity
import id.soaldulu.app.data.LogPeristiwaEntity
import id.soaldulu.app.data.SoalduluRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Laporan satu responden, dalam JSON.
 *
 * Isinya dua lapis: `ringkasan` untuk dibaca langsung tanpa mengolah apa pun,
 * dan seluruh baris log mentah di bawahnya supaya analisis tetap bisa
 * dilakukan sendiri. Berkas inilah yang dikirim ke repo GitHub peneliti dan
 * menggantikan ekspor CSV satu per satu — CSV-nya tetap ada sebagai cadangan
 * kalau HP responden tidak pernah online.
 */
object Laporan {

    const val SKEMA = 1

    suspend fun susun(
        context: Context,
        repo: SoalduluRepository,
        nama: String,
        versiAplikasi: String,
    ): JSONObject {
        val jawaban = repo.semuaJawaban()
        val kredit = repo.semuaKredit()
        val peristiwa = repo.semuaPeristiwa()
        val paket = repo.paketTerpasang()
        val pemakaian = Pemakaian.semuaDetik(context)
        val izin = bacaStatusIzin(context)

        return JSONObject().apply {
            put("skemaLaporan", SKEMA)
            put("dikirimPada", waktuLokal(System.currentTimeMillis()))
            put("dikirimPadaEpochMs", System.currentTimeMillis())

            put(
                "responden",
                JSONObject().apply {
                    put("nama", nama)
                    put("idPerangkat", StatusKirim.idPerangkat(context))
                },
            )

            put(
                "aplikasi",
                JSONObject().apply {
                    put("versiAplikasi", versiAplikasi)
                    put("versiPaketSoal", paket?.version.orEmpty())
                    put("butirAktif", paket?.activeItemCount ?: 0)
                    put("skemaLaporan", SKEMA)
                },
            )

            put(
                "perangkat",
                JSONObject().apply {
                    put("pabrikan", Build.MANUFACTURER)
                    put("model", Build.MODEL)
                    put("android", Build.VERSION.SDK_INT)
                },
            )

            put("ringkasan", ringkasan(jawaban, kredit, peristiwa, pemakaian, context, izin.semuaAktif))
            put("pemakaianAplikasiDetik", JSONObject(pemakaian.mapValues { it.value }))
            put(
                "izinSaatIni",
                JSONObject().apply {
                    put("aksesPenggunaan", izin.usageAccess)
                    put("tampilDiAtas", izin.overlay)
                    put("notifikasi", izin.notifikasi)
                    put("abaikanOptimasiBaterai", izin.baterai)
                },
            )
            put("aplikasiDipantau", JSONArray(DaftarAplikasi.semua(context).sorted()))

            put("jawaban", JSONArray(jawaban.map(::barisJawaban)))
            put("sesiKredit", JSONArray(kredit.map(::barisKredit)))
            put("peristiwa", JSONArray(peristiwa.map(::barisPeristiwa)))
        }
    }

    private fun ringkasan(
        jawaban: List<LogJawabanEntity>,
        kredit: List<LogKreditEntity>,
        peristiwa: List<LogPeristiwaEntity>,
        pemakaian: Map<String, Long>,
        context: Context,
        semuaIzinAktif: Boolean,
    ): JSONObject {
        val benar = jawaban.count { it.isCorrect }
        val totalDetikMengerjakan = jawaban.sumOf { it.durationSeconds }
        val gerbang = jawaban.groupBy { it.gateSessionId }
        val dipicuMedsos = gerbang.filterValues { baris ->
            baris.first().triggeredByPackage !in PEMICU_BUKAN_MEDSOS
        }
        // Berapa kali tiap aplikasi dibuka: dari peristiwa APP_OPENED yang
        // dicatat service setiap aplikasi terpantau naik ke depan.
        val dibuka = peristiwa.filter { it.eventType == "APP_OPENED" }
            .groupingBy { it.detail }.eachCount()
        val perSubtes = jawaban.groupBy { it.subtest }.mapValues { (_, baris) ->
            JSONObject().apply {
                put("dijawab", baris.size)
                put("benar", baris.count { it.isCorrect })
            }
        }
        val zona = ZoneId.systemDefault()
        val hariAktif = jawaban.map {
            Instant.ofEpochMilli(it.timestamp).atZone(zona).toLocalDate().toString()
        }.distinct().sorted()

        return JSONObject().apply {
            put("soalDikerjakan", jawaban.size)
            put("jawabanBenar", benar)
            put("jawabanSalah", jawaban.size - benar)
            put("persenBenar", if (jawaban.isEmpty()) 0 else benar * 100 / jawaban.size)
            put("totalDetikMengerjakanSoal", totalDetikMengerjakan)
            put("totalMenitMengerjakanSoal", totalDetikMengerjakan / 60)
            put(
                "rataRataDetikPerSoal",
                if (jawaban.isEmpty()) 0 else totalDetikMengerjakan / jawaban.size,
            )

            put("jumlahGerbang", gerbang.size)
            put("gerbangDipicuMedsos", dipicuMedsos.size)
            put("gerbangDibukaSendiri", gerbang.size - dipicuMedsos.size)
            put(
                "gerbangPerAplikasi",
                JSONObject(
                    dipicuMedsos.values.groupingBy { it.first().triggeredByPackage }.eachCount(),
                ),
            )
            put("aplikasiDibukaBerapaKali", JSONObject(dibuka))
            put("perambanDibuka", peristiwa.count { it.eventType == "BROWSER_OPENED" })

            put("kreditDidapatDetik", kredit.sumOf { it.creditGrantedSeconds })
            put("kreditDidapatMenit", kredit.sumOf { it.creditGrantedSeconds } / 60)
            put("totalDetikPakaiAplikasi", pemakaian.values.sum())
            put("totalMenitPakaiAplikasi", pemakaian.values.sum() / 60)
            put("sisaSaldoDetik", SaldoKredit.sisaDetik(context))

            put("perSubtes", JSONObject(perSubtes))
            put("hariAktif", hariAktif.size)
            put("tanggalAktif", JSONArray(hariAktif))
            put("aktivitasPertama", jawaban.minOfOrNull { it.timestamp }?.let(::waktuLokal))
            put("aktivitasTerakhir", jawaban.maxOfOrNull { it.timestamp }?.let(::waktuLokal))

            put("modeDaruratDipakai", peristiwa.count { it.eventType == "EMERGENCY_STARTED" })
            put("aplikasiDitambah", peristiwa.count { it.eventType == "APP_ADDED" })
            put("aplikasiDihapus", peristiwa.count { it.eventType == "APP_REMOVED" })
            put("aplikasiDimatikan", peristiwa.count { it.eventType == "APP_DISABLED" })
            put("serviceRestart", peristiwa.count { it.eventType == "SERVICE_RESTARTED" })
            put("soalDilaporkan", peristiwa.count { it.eventType == "ITEM_REPORTED" })
            put("semuaIzinAktif", semuaIzinAktif)
        }
    }

    private fun barisJawaban(b: LogJawabanEntity) = JSONObject().apply {
        put("id", b.id)
        put("responden", b.respondentCode)
        put("versiPaket", b.packageVersion)
        put("idSesiGerbang", b.gateSessionId)
        put("urutanDalamGerbang", b.urutanDalamGerbang)
        put("itemId", b.itemId)
        put("subtest", b.subtest)
        put("opsiDipilih", b.selectedOptionId)
        put("benar", b.isCorrect)
        put("durasiDetik", b.durationSeconds)
        put("kreditDidapatDetik", b.creditEarnedSeconds)
        put("dipicuOleh", b.triggeredByPackage)
        put("waktu", waktuLokal(b.timestamp))
        put("waktuEpochMs", b.timestamp)
    }

    private fun barisKredit(b: LogKreditEntity) = JSONObject().apply {
        put("id", b.id)
        put("responden", b.respondentCode)
        put("idSesiGerbang", b.gateSessionId)
        put("kreditDiberikanDetik", b.creditGrantedSeconds)
        put("mulai", waktuLokal(b.creditStartedAt))
        put("selesai", b.creditEndedAt?.let(::waktuLokal))
        put("alasanSelesai", b.endReason)
    }

    private fun barisPeristiwa(b: LogPeristiwaEntity) = JSONObject().apply {
        put("id", b.id)
        put("responden", b.respondentCode)
        put("jenis", b.eventType)
        put("detail", b.detail)
        put("waktu", waktuLokal(b.timestamp))
        put("waktuEpochMs", b.timestamp)
    }

    private fun waktuLokal(epochMs: Long): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
            Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()),
        )

    /** Nilai triggeredByPackage yang bukan nama paket media sosial. */
    private val PEMICU_BUKAN_MEDSOS = setOf("MANUAL", "NOTIFICATION", "REMOVE_APP")
}
