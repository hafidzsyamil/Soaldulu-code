package id.soaldulu.app

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/**
 * Overlay gerbang versi spike.
 *
 * Tombol TUTUP hanya ada di sini. Di aplikasi asli overlay tidak boleh punya
 * jalan keluar sama sekali (handoff 3.5) — tombol ini semata supaya HP tidak
 * terkunci saat menguji.
 *
 * Semua pemanggilan harus dari main thread (WindowManager mengharuskannya).
 */
class OverlayGate(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null

    val sedangTampil: Boolean get() = view != null

    fun tampilkan(paketPemicu: String, latensiMs: Long, catatan: String, onTutup: () -> Unit) {
        if (view != null) return // cegah overlay bertumpuk

        val v = LayoutInflater.from(context).inflate(R.layout.overlay_gate, null)
        v.findViewById<TextView>(R.id.teks_paket).text = paketPemicu
        v.findViewById<TextView>(R.id.teks_latensi).text = "$latensiMs ms"
        v.findViewById<TextView>(R.id.teks_catatan).text = catatan
        v.findViewById<Button>(R.id.tombol_tutup).setOnClickListener {
            sembunyikan()
            onTutup()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Sengaja TANPA FLAG_NOT_TOUCH_MODAL dan TANPA FLAG_NOT_FOCUSABLE:
            // seluruh sentuhan dan tombol Back ditangkap overlay, sehingga
            // aplikasi di bawahnya benar-benar tidak bisa dipakai.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START

        try {
            wm.addView(v, params)
            view = v
        } catch (e: Exception) {
            // Paling mungkin: izin "tampil di atas aplikasi lain" dicabut
            // saat service sedang jalan. Itu temuan, bukan kegagalan diam-diam.
            SpikeLog.tulis(context, "OVERLAY_GAGAL ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun sembunyikan() {
        val v = view ?: return
        view = null
        try {
            wm.removeView(v)
        } catch (_: IllegalArgumentException) {
            // View sudah lepas duluan; tidak ada yang perlu dilakukan.
        }
    }
}
