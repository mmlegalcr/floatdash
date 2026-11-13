package cr.andres.floatdash

import android.content.Context
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.WindowManager

object Geometry {

    /** (ancho, alto) reales de la pantalla en píxeles. */
    fun screen(ctx: Context): Pair<Int, Int> {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        return dm.widthPixels to dm.heightPixels
    }

    /** Convierte una región en fracciones (0..1) a un Rect en píxeles. */
    fun toPixels(ctx: Context, left: Float, top: Float, right: Float, bottom: Float): Rect {
        val (w, h) = screen(ctx)
        val l = (left.coerceIn(0f, 1f) * w).toInt()
        val t = (top.coerceIn(0f, 1f) * h).toInt()
        val r = (right.coerceIn(0f, 1f) * w).toInt()
        val b = (bottom.coerceIn(0f, 1f) * h).toInt()
        // tamaño mínimo razonable para freeform
        val minW = (w * 0.18f).toInt()
        val minH = (h * 0.18f).toInt()
        val rr = if (r - l < minW) l + minW else r
        val bb = if (b - t < minH) t + minH else b
        return Rect(l, t, rr.coerceAtMost(w), bb.coerceAtMost(h))
    }
}
