package id.soaldulu.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Database lokal. Satu versi, tanpa migrasi.
 *
 * exportSchema = false karena uji coba hanya berjalan beberapa hari dan
 * tidak akan ada versi kedua yang perlu bermigrasi.
 */
@Database(
    entities = [
        PaketEntity::class,
        BacaanEntity::class,
        ButirEntity::class,
        OpsiEntity::class,
        LogJawabanEntity::class,
        LogKreditEntity::class,
        LogPeristiwaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SoalduluDatabase : RoomDatabase() {

    abstract fun dao(): SoalduluDao

    companion object {
        @Volatile
        private var instance: SoalduluDatabase? = null

        fun ambil(context: Context): SoalduluDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoalduluDatabase::class.java,
                    "soaldulu.db",
                ).build().also { instance = it }
            }
    }
}
