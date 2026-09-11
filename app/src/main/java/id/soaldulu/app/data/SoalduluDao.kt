package id.soaldulu.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Satu DAO untuk seluruh aplikasi.
 *
 * Handoff Bagian 0.3: ViewModel + Repository + Room DAO sudah cukup untuk
 * seluruh aplikasi ini. Tidak ada DAO per tabel, tidak ada use case class.
 *
 * Sengaja abstract class, bukan interface: @Transaction pada method
 * bertubuh di interface Kotlin bergantung pada perilaku default method JVM
 * yang tidak dijamin lintas versi.
 */
@Dao
abstract class SoalduluDao {

    // ── Seeding bank soal ───────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun simpanPaket(paket: PaketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun simpanBacaan(bacaan: List<BacaanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun simpanButir(butir: List<ButirEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun simpanOpsi(opsi: List<OpsiEntity>)

    @Query("DELETE FROM opsi")
    abstract suspend fun hapusSemuaOpsi()

    @Query("DELETE FROM butir")
    abstract suspend fun hapusSemuaButir()

    @Query("DELETE FROM bacaan")
    abstract suspend fun hapusSemuaBacaan()

    @Query("DELETE FROM paket")
    abstract suspend fun hapusSemuaPaket()

    /**
     * Ganti seluruh isi bank soal dalam satu transaksi.
     * Log penelitian TIDAK ikut terhapus — itu data KTI.
     */
    @Transaction
    open suspend fun seedUlang(
        paket: PaketEntity,
        bacaan: List<BacaanEntity>,
        butir: List<ButirEntity>,
        opsi: List<OpsiEntity>,
    ) {
        hapusSemuaOpsi()
        hapusSemuaButir()
        hapusSemuaBacaan()
        hapusSemuaPaket()
        simpanPaket(paket)
        simpanBacaan(bacaan)
        simpanButir(butir)
        simpanOpsi(opsi)
    }

    @Query("SELECT * FROM paket LIMIT 1")
    abstract suspend fun paketTerpasang(): PaketEntity?

    @Query("SELECT COUNT(*) FROM butir WHERE active = 1")
    abstract suspend fun jumlahButirAktif(): Int

    // ── Pemilihan soal (handoff Bagian 5.4) ─────────────────────────────────

    /**
     * Butir acak yang aktif DAN belum pernah dijawab responden ini.
     * Memakai log jawaban sebagai penanda "sudah terpakai", jadi tidak perlu
     * tabel tambahan.
     */
    @Query(
        """
        SELECT * FROM butir
        WHERE active = 1
          AND id NOT IN (SELECT itemId FROM log_jawaban WHERE respondentCode = :kode)
        ORDER BY RANDOM()
        LIMIT :jumlah
        """
    )
    abstract suspend fun butirBelumDijawab(kode: String, jumlah: Int): List<ButirEntity>

    /** Cadangan saat seluruh bank sudah habis dijawab responden ini. */
    @Query("SELECT * FROM butir WHERE active = 1 ORDER BY RANDOM() LIMIT :jumlah")
    abstract suspend fun butirAktifAcak(jumlah: Int): List<ButirEntity>

    @Query("SELECT * FROM opsi WHERE itemId = :itemId ORDER BY urutan")
    abstract suspend fun opsiUntuk(itemId: String): List<OpsiEntity>

    @Query("SELECT * FROM bacaan WHERE id = :id")
    abstract suspend fun bacaan(id: String): BacaanEntity?

    // ── Menulis log ─────────────────────────────────────────────────────────

    @Insert
    abstract suspend fun catatJawaban(baris: LogJawabanEntity): Long

    @Insert
    abstract suspend fun catatKredit(baris: LogKreditEntity): Long

    @Insert
    abstract suspend fun catatPeristiwa(baris: LogPeristiwaEntity): Long

    @Query("UPDATE log_kredit SET creditEndedAt = :selesai, endReason = :alasan WHERE id = :id")
    abstract suspend fun tutupKredit(id: Long, selesai: Long, alasan: String)

    /**
     * Sesi kredit yang belum ditutup. Kalau ini terisi saat service baru
     * start, artinya service sempat mati saat kredit masih berjalan —
     * ditandai SERVICE_KILLED.
     */
    @Query("SELECT * FROM log_kredit WHERE creditEndedAt IS NULL")
    abstract suspend fun kreditBelumDitutup(): List<LogKreditEntity>

    // ── Statistik untuk Home ────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM log_jawaban WHERE respondentCode = :kode")
    abstract suspend fun jumlahDijawab(kode: String): Int

    @Query("SELECT COUNT(*) FROM log_jawaban WHERE respondentCode = :kode AND isCorrect = 1")
    abstract suspend fun jumlahBenar(kode: String): Int

    /** Untuk menghitung "hari berjalan" di Home. null kalau belum pernah menjawab. */
    @Query("SELECT MIN(timestamp) FROM log_jawaban WHERE respondentCode = :kode")
    abstract suspend fun waktuJawabanPertama(kode: String): Long?

    @Query(
        "SELECT * FROM log_jawaban WHERE respondentCode = :kode " +
            "ORDER BY timestamp DESC LIMIT :jumlah"
    )
    abstract suspend fun jawabanTerakhir(kode: String, jumlah: Int): List<LogJawabanEntity>

    // ── Ekspor CSV ──────────────────────────────────────────────────────────

    @Query("SELECT * FROM log_jawaban ORDER BY timestamp")
    abstract suspend fun semuaJawaban(): List<LogJawabanEntity>

    @Query("SELECT * FROM log_kredit ORDER BY creditStartedAt")
    abstract suspend fun semuaKredit(): List<LogKreditEntity>

    @Query("SELECT * FROM log_peristiwa ORDER BY timestamp")
    abstract suspend fun semuaPeristiwa(): List<LogPeristiwaEntity>
}
