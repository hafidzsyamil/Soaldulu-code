package id.soaldulu.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Versi skema tertinggi yang dimengerti aplikasi ini. */
const val SCHEMA_VERSION_DIDUKUNG = 1

sealed interface HasilBacaPaket {

    data class Berhasil(
        val paket: PaketEntity,
        val bacaan: List<BacaanEntity>,
        val butir: List<ButirEntity>,
        val opsi: List<OpsiEntity>,
    ) : HasilBacaPaket

    data class Gagal(val kesalahan: List<String>) : HasilBacaPaket
}

/**
 * Membaca bank_soal_v1.json apa adanya. Tidak menambah, tidak mengubah,
 * tidak "melengkapi" butir soal (handoff Bagian 0.2).
 *
 * Memakai org.json bawaan Android — tanpa pustaka tambahan. Skemanya tetap
 * dan hanya dibaca sekali saat seeding, jadi tidak sepadan menambah plugin
 * serialisasi.
 *
 * Semua kesalahan dikumpulkan lalu dilaporkan sekaligus, bukan satu per satu:
 * pengguna menyunting 150 butir dengan tangan, dan memperbaiki 20 kesalahan
 * satu per satu berarti 20 kali build ulang.
 */
object BankSoalParser {

    fun bacaDariAssets(
        context: Context,
        namaBerkas: String = id.soaldulu.app.GateConfig.BUNDLED_PACKAGE_ASSET,
    ): HasilBacaPaket {
        val teks = try {
            context.assets.open(namaBerkas).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return HasilBacaPaket.Gagal(
                listOf(
                    "Berkas '$namaBerkas' tidak ada di folder assets. " +
                        "Taruh bank soal di app/src/main/assets/$namaBerkas."
                )
            )
        }
        return bacaDariTeks(teks)
    }

    fun bacaDariTeks(teks: String): HasilBacaPaket {
        val root = try {
            JSONObject(teks)
        } catch (e: Exception) {
            return HasilBacaPaket.Gagal(listOf("JSON tidak bisa dibaca: ${e.message}"))
        }

        // schemaVersion diperiksa lebih dulu dan menghentikan segalanya:
        // kalau skemanya lebih baru, sisa pemeriksaan tidak ada artinya.
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion < 0) {
            return HasilBacaPaket.Gagal(listOf("Field 'schemaVersion' tidak ada di berkas."))
        }
        if (schemaVersion > SCHEMA_VERSION_DIDUKUNG) {
            return HasilBacaPaket.Gagal(
                listOf(
                    "schemaVersion berkas ($schemaVersion) lebih tinggi dari yang " +
                        "didukung aplikasi ini ($SCHEMA_VERSION_DIDUKUNG). Perbarui aplikasi."
                )
            )
        }

        val kesalahan = mutableListOf<String>()

        val version = root.optString("version")
        if (version.isEmpty()) {
            // Dicatat di setiap baris log; tanpa ini data responden tidak bisa
            // dihubungkan ke versi paket.
            kesalahan += "Field 'version' kosong. Nilai ini masuk ke setiap baris log."
        }

        // ── Bacaan ──────────────────────────────────────────────────────────
        val bacaan = mutableListOf<BacaanEntity>()
        val idBacaanTerlihat = mutableSetOf<String>()
        val arrBacaan = root.optJSONArray("passages") ?: JSONArray()
        for (i in 0 until arrBacaan.length()) {
            val o = arrBacaan.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isEmpty()) {
                kesalahan += "passages[$i]: 'id' kosong."
                continue
            }
            if (!idBacaanTerlihat.add(id)) {
                kesalahan += "passages[$i]: id bacaan kembar '$id'."
            }
            val sumber = o.optJSONObject("source")
            bacaan += BacaanEntity(
                id = id,
                title = o.optString("title"),
                text = o.optString("text"),
                sourceType = sumber?.optString("type").orEmpty(),
                sourceReference = sumber?.optString("reference").orEmpty(),
            )
        }

        // ── Butir + opsi ────────────────────────────────────────────────────
        val butir = mutableListOf<ButirEntity>()
        val opsi = mutableListOf<OpsiEntity>()
        val idButirTerlihat = mutableSetOf<String>()
        val arrButir = root.optJSONArray("items") ?: JSONArray()
        for (i in 0 until arrButir.length()) {
            val o = arrButir.optJSONObject(i) ?: continue

            val id = o.optString("id")
            if (id.isEmpty()) {
                kesalahan += "items[$i]: 'id' kosong."
                continue
            }
            if (!idButirTerlihat.add(id)) {
                kesalahan += "items[$i]: id butir kembar '$id'."
            }

            val passageId =
                if (o.isNull("passageId")) null else o.optString("passageId").ifEmpty { null }
            if (passageId != null && passageId !in idBacaanTerlihat) {
                kesalahan += "$id: passageId '$passageId' merujuk bacaan yang tidak ada."
            }

            val arrOpsi = o.optJSONArray("options") ?: JSONArray()
            val idOpsi = mutableSetOf<String>()
            for (j in 0 until arrOpsi.length()) {
                val oo = arrOpsi.optJSONObject(j) ?: continue
                val optId = oo.optString("id")
                if (optId.isEmpty()) {
                    kesalahan += "$id: options[$j] tidak punya 'id'."
                    continue
                }
                if (!idOpsi.add(optId)) {
                    kesalahan += "$id: huruf opsi kembar '$optId'."
                }
                opsi += OpsiEntity(
                    itemId = id,
                    optionId = optId,
                    text = oo.optString("text"),
                    urutan = j,
                )
            }
            if (idOpsi.isEmpty()) {
                kesalahan += "$id: tidak punya satu pun opsi jawaban."
            }

            val correct = o.optString("correctOptionId")
            if (correct.isEmpty()) {
                kesalahan += "$id: 'correctOptionId' kosong."
            } else if (idOpsi.isNotEmpty() && correct !in idOpsi) {
                kesalahan += "$id: correctOptionId '$correct' tidak ada di dalam options " +
                    "(${idOpsi.sorted().joinToString(", ")})."
            }

            if (o.optString("stem").isEmpty()) {
                kesalahan += "$id: 'stem' (batang soal) kosong."
            }

            val sumber = o.optJSONObject("source")
            butir += ButirEntity(
                id = id,
                active = o.optBoolean("active", true),
                exam = o.optString("exam"),
                subtest = o.optString("subtest"),
                passageId = passageId,
                stem = o.optString("stem"),
                correctOptionId = correct,
                explanation = o.optString("explanation"),
                cognitiveLevel = o.optString("cognitiveLevel"),
                estimatedSeconds = o.optInt("estimatedSeconds", 0),
                sourceType = sumber?.optString("type").orEmpty(),
                sourceReference = sumber?.optString("reference").orEmpty(),
            )
        }

        // ── Cocokkan hitungan ───────────────────────────────────────────────
        val jumlahAktif = butir.count { it.active }
        val activeItemCount = root.optInt("activeItemCount", -1)
        if (activeItemCount >= 0 && activeItemCount != jumlahAktif) {
            kesalahan += "activeItemCount di berkas ($activeItemCount) tidak cocok dengan " +
                "jumlah butir active:true yang terbaca ($jumlahAktif)."
        }
        val itemCount = root.optInt("itemCount", -1)
        if (itemCount >= 0 && itemCount != butir.size) {
            kesalahan += "itemCount di berkas ($itemCount) tidak cocok dengan jumlah butir " +
                "yang terbaca (${butir.size})."
        }
        if (jumlahAktif == 0) {
            kesalahan += "Tidak ada satu pun butir dengan active:true."
        }

        if (kesalahan.isNotEmpty()) return HasilBacaPaket.Gagal(kesalahan)

        val paket = PaketEntity(
            packageId = root.optString("packageId"),
            packageName = root.optString("packageName"),
            version = version,
            schemaVersion = schemaVersion,
            releasedAt = root.optString("releasedAt"),
            changelog = root.optString("changelog"),
            itemCount = butir.size,
            activeItemCount = jumlahAktif,
            diseedPada = System.currentTimeMillis(),
        )
        return HasilBacaPaket.Berhasil(paket, bacaan, butir, opsi)
    }
}
