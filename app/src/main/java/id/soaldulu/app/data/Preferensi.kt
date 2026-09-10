package id.soaldulu.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "soaldulu")

/**
 * Preferensi yang bertahan antar-sesi.
 *
 * Isinya sedikit dan tidak akan bertambah banyak: kode responden, penanda
 * persetujuan, dan sisa kredit. Tidak ada nama, email, atau nomor telepon
 * yang disimpan (handoff Bagian 8 nomor 3).
 */
object Preferensi {

    private val KODE_RESPONDEN = stringPreferencesKey("kode_responden")
    private val SUDAH_SETUJU = booleanPreferencesKey("sudah_setuju")

    suspend fun kodeResponden(context: Context): String =
        context.dataStore.data.map { it[KODE_RESPONDEN] ?: "" }.first()

    suspend fun simpanKodeResponden(context: Context, kode: String) {
        context.dataStore.edit { it[KODE_RESPONDEN] = kode }
    }

    suspend fun sudahSetuju(context: Context): Boolean =
        context.dataStore.data.map { it[SUDAH_SETUJU] ?: false }.first()

    suspend fun simpanPersetujuan(context: Context, setuju: Boolean) {
        context.dataStore.edit { it[SUDAH_SETUJU] = setuju }
    }
}
