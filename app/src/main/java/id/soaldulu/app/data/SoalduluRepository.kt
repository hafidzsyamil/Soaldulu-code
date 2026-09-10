package id.soaldulu.app.data

import android.content.Context
import id.soaldulu.app.GateConfig

/** Satu soal lengkap dengan semua yang dibutuhkan untuk menampilkannya. */
data class SoalLengkap(
    val butir: ButirEntity,
    val opsi: List<OpsiEntity>,
    /** null kalau soal berdiri sendiri tanpa bacaan. */
    val bacaan: BacaanEntity?,
)

sealed interface HasilSeed {
    data class Berhasil(val jumlahButir: Int, val jumlahAktif: Int, val versi: String) : HasilSeed
    data class Gagal(val kesalahan: List<String>) : HasilSeed
}

/**
 * Satu-satunya pintu antara UI dan database.
 *
 * Handoff Bagian 0.3: ViewModel + Repository + Room DAO sudah cukup untuk
 * seluruh aplikasi. Tidak ada use case class per aksi.
 */
class SoalduluRepository private constructor(private val dao: SoalduluDao) {

    // ── Bank soal ───────────────────────────────────────────────────────────

    /** Seed hanya kalau database masih kosong. Dipanggil saat aplikasi dibuka. */
    suspend fun seedBilaPerlu(context: Context): HasilSeed? =
        if (dao.paketTerpasang() == null) seedDariAssets(context) else null

    /** Ganti seluruh bank soal dari assets. Log penelitian tidak ikut terhapus. */
    suspend fun seedDariAssets(context: Context): HasilSeed =
        when (val hasil = BankSoalParser.bacaDariAssets(context)) {
            is HasilBacaPaket.Gagal -> HasilSeed.Gagal(hasil.kesalahan)
            is HasilBacaPaket.Berhasil -> {
                dao.seedUlang(hasil.paket, hasil.bacaan, hasil.butir, hasil.opsi)
                HasilSeed.Berhasil(
                    jumlahButir = hasil.butir.size,
                    jumlahAktif = hasil.paket.activeItemCount,
                    versi = hasil.paket.version,
                )
            }
        }

    suspend fun paketTerpasang(): PaketEntity? = dao.paketTerpasang()

    suspend fun jumlahButirAktif(): Int = dao.jumlahButirAktif()

    /**
     * Ambil soal untuk satu gerbang: acak, hanya butir aktif, tanpa
     * pengulangan sampai seluruh bank habis (handoff Bagian 5.4).
     */
    suspend fun soalUntukGerbang(
        kode: String,
        jumlah: Int = GateConfig.QUESTIONS_PER_GATE,
    ): List<SoalLengkap> {
        val belumDijawab = dao.butirBelumDijawab(kode, jumlah)
        // Kurang dari yang diminta berarti seluruh bank sudah habis dijawab
        // responden ini; mulai lagi dari awal.
        val terpilih =
            if (belumDijawab.size < jumlah) dao.butirAktifAcak(jumlah) else belumDijawab

        return terpilih.map { b ->
            SoalLengkap(
                butir = b,
                opsi = dao.opsiUntuk(b.id),
                bacaan = b.passageId?.let { dao.bacaan(it) },
            )
        }
    }

    // ── Log penelitian ──────────────────────────────────────────────────────

    suspend fun catatJawaban(
        kode: String,
        versiPaket: String,
        gateSessionId: Long,
        urutanDalamGerbang: Int,
        butir: ButirEntity,
        opsiDipilih: String,
        durasiDetik: Int,
        kreditDidapat: Int,
        paketPemicu: String,
    ): Long = dao.catatJawaban(
        LogJawabanEntity(
            respondentCode = kode,
            packageVersion = versiPaket,
            gateSessionId = gateSessionId,
            urutanDalamGerbang = urutanDalamGerbang,
            itemId = butir.id,
            subtest = butir.subtest,
            selectedOptionId = opsiDipilih,
            isCorrect = opsiDipilih == butir.correctOptionId,
            durationSeconds = durasiDetik,
            creditEarnedSeconds = kreditDidapat,
            triggeredByPackage = paketPemicu,
            timestamp = System.currentTimeMillis(),
        )
    )

    suspend fun mulaiSesiKredit(kode: String, gateSessionId: Long, detik: Int): Long =
        dao.catatKredit(
            LogKreditEntity(
                respondentCode = kode,
                gateSessionId = gateSessionId,
                creditGrantedSeconds = detik,
                creditStartedAt = System.currentTimeMillis(),
            )
        )

    /** alasan: EXPIRED | MANUAL | SERVICE_KILLED */
    suspend fun tutupSesiKredit(id: Long, alasan: String) {
        dao.tutupKredit(id, System.currentTimeMillis(), alasan)
    }

    /**
     * Dipanggil saat service baru start. Sesi kredit yang masih terbuka
     * berarti service sempat mati saat kredit sedang berjalan.
     */
    suspend fun tandaiKreditTergantung(): Int {
        val tergantung = dao.kreditBelumDitutup()
        tergantung.forEach { dao.tutupKredit(it.id, System.currentTimeMillis(), "SERVICE_KILLED") }
        return tergantung.size
    }

    suspend fun catatPeristiwa(kode: String, jenis: String, detail: String) {
        dao.catatPeristiwa(
            LogPeristiwaEntity(
                respondentCode = kode,
                eventType = jenis,
                detail = detail,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    // ── Statistik & ekspor ──────────────────────────────────────────────────

    suspend fun jumlahDijawab(kode: String): Int = dao.jumlahDijawab(kode)

    suspend fun jumlahBenar(kode: String): Int = dao.jumlahBenar(kode)

    suspend fun jawabanTerakhir(kode: String, jumlah: Int): List<LogJawabanEntity> =
        dao.jawabanTerakhir(kode, jumlah)

    suspend fun semuaJawaban(): List<LogJawabanEntity> = dao.semuaJawaban()

    suspend fun semuaKredit(): List<LogKreditEntity> = dao.semuaKredit()

    suspend fun semuaPeristiwa(): List<LogPeristiwaEntity> = dao.semuaPeristiwa()

    companion object {
        @Volatile
        private var instance: SoalduluRepository? = null

        fun ambil(context: Context): SoalduluRepository =
            instance ?: synchronized(this) {
                instance ?: SoalduluRepository(SoalduluDatabase.ambil(context).dao())
                    .also { instance = it }
            }
    }
}
