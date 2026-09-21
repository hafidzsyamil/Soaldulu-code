package id.soaldulu.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "soaldulu")

/**
 * Preferensi yang bertahan antar-sesi.
 *
 * CATATAN PENTING, diubah 22 September 2026: aplikasi sekarang menyimpan
 * NAMA responden, bukan lagi kode anonim R1-R5. Nama ini juga ikut di setiap
 * baris log penelitian. Teks di layar persetujuan sudah disesuaikan supaya
 * tidak lagi menjanjikan anonimitas yang tidak berlaku.
 */
object Preferensi {

    private val NAMA = stringPreferencesKey("nama")
    private val AVATAR = intPreferencesKey("avatar")
    private val SUDAH_SETUJU = booleanPreferencesKey("sudah_setuju")
    private val TEMA_GELAP = booleanPreferencesKey("tema_gelap")
    private val TEMA_IKUT_SISTEM = booleanPreferencesKey("tema_ikut_sistem")

    suspend fun nama(context: Context): String =
        context.dataStore.data.map { it[NAMA] ?: "" }.first()

    suspend fun simpanNama(context: Context, nama: String) {
        context.dataStore.edit { it[NAMA] = nama }
    }

    /** Indeks avatar terpilih. 0 kalau belum pernah memilih. */
    suspend fun avatar(context: Context): Int =
        context.dataStore.data.map { it[AVATAR] ?: 0 }.first()

    suspend fun simpanAvatar(context: Context, indeks: Int) {
        context.dataStore.edit { it[AVATAR] = indeks }
    }

    suspend fun sudahSetuju(context: Context): Boolean =
        context.dataStore.data.map { it[SUDAH_SETUJU] ?: false }.first()

    suspend fun simpanPersetujuan(context: Context, setuju: Boolean) {
        context.dataStore.edit { it[SUDAH_SETUJU] = setuju }
    }

    /**
     * null berarti ikut tema sistem — itu keadaan bawaannya. Terisi hanya
     * setelah responden menggeser sakelar di Settings.
     */
    suspend fun temaGelap(context: Context): Boolean? =
        context.dataStore.data.map { prefs ->
            if (prefs[TEMA_IKUT_SISTEM] != false) null else prefs[TEMA_GELAP]
        }.first()

    suspend fun simpanTema(context: Context, gelap: Boolean?) {
        context.dataStore.edit { prefs ->
            if (gelap == null) {
                prefs[TEMA_IKUT_SISTEM] = true
            } else {
                prefs[TEMA_IKUT_SISTEM] = false
                prefs[TEMA_GELAP] = gelap
            }
        }
    }
}
