package id.soaldulu.app

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Menjalankan Compose di dalam jendela overlay WindowManager.
 *
 * Kenapa ini perlu: ComposeView mencari tiga "owner" di pohon view-nya —
 * Lifecycle, SavedStateRegistry, dan ViewModelStore. Di dalam Activity
 * ketiganya sudah terpasang sendiri. Jendela WindowManager tidak punya
 * Activity, jadi ketiganya harus dipasang manual, kalau tidak ComposeView
 * gagal begitu ditambahkan.
 *
 * Layar Gerbang di Fase 2 harus muncul di dalam overlay, bukan sebagai
 * Activity, jadi seluruh Fase 2 bergantung pada kelas ini.
 *
 * Sekali pakai: setelah ditutup, siklus hidupnya sudah DESTROYED dan tidak
 * bisa dihidupkan lagi. Buat instans baru untuk setiap gerbang — itu juga
 * yang membuat state tiap gerbang bersih dengan sendirinya.
 */
class OverlayCompose private constructor(
    private val context: Context,
) : SavedStateRegistryOwner, ViewModelStoreOwner {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    private var view: ComposeView? = null

    val sedangTampil: Boolean get() = view != null

    private fun pasang(isi: @Composable () -> Unit): Boolean {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val v = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayCompose)
            setViewTreeSavedStateRegistryOwner(this@OverlayCompose)
            setViewTreeViewModelStoreOwner(this@OverlayCompose)
            setContent(isi)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Sengaja tanpa FLAG_NOT_TOUCH_MODAL dan tanpa FLAG_NOT_FOCUSABLE:
            // seluruh sentuhan dan tombol Back ditangkap overlay.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        return try {
            wm.addView(v, params)
            view = v
            true
        } catch (e: Exception) {
            SpikeLog.tulis(context, "OVERLAY_COMPOSE_GAGAL ${e.javaClass.simpleName}: ${e.message}")
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
            viewModelStore.clear()
            false
        }
    }

    fun tutup() {
        val v = view ?: return
        view = null
        try {
            wm.removeView(v)
        } catch (_: IllegalArgumentException) {
            // Sudah lepas duluan.
        }
        v.disposeComposition()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }

    companion object {
        /** Kembalikan null kalau jendela gagal dipasang (izin overlay dicabut). */
        fun tampilkan(context: Context, isi: @Composable () -> Unit): OverlayCompose? {
            val host = OverlayCompose(context)
            return if (host.pasang(isi)) host else null
        }
    }
}
