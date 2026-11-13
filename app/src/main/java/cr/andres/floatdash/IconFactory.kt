package cr.andres.floatdash

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Build
import android.widget.Toast

object IconFactory {

    /** Dibuja los íconos reales de las apps en sus regiones, sobre un fondo oscuro. */
    fun buildIcon(ctx: Context, combo: Combo): Bitmap {
        val s = 192
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0B1220") }
        c.drawRoundRect(RectF(0f, 0f, s.toFloat(), s.toFloat()), 36f, 36f, bg)

        val pad = 14f
        val inner = s - pad * 2
        combo.tiles.filter { !it.pkg.isNullOrEmpty() }.forEach { t ->
            val icon = Apps.icon(ctx, t.pkg) ?: return@forEach
            val l = pad + t.left * inner
            val top = pad + t.top * inner
            val r = pad + t.right * inner
            val b = pad + t.bottom * inner
            // icono cuadrado centrado dentro de la región
            val side = minOf(r - l, b - top) * 0.92f
            val cx = (l + r) / 2f
            val cy = (top + b) / 2f
            icon.bounds = Rect((cx - side / 2).toInt(), (cy - side / 2).toInt(), (cx + side / 2).toInt(), (cy + side / 2).toInt())
            icon.draw(c)
        }
        return bmp
    }

    /** Crea un acceso directo en la pantalla de inicio (Nova) para el combo. */
    fun pin(ctx: Context, combo: Combo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(ctx, "No soportado en esta versión.", Toast.LENGTH_SHORT).show()
            return
        }
        val sm = ctx.getSystemService(ShortcutManager::class.java)
        if (sm == null || !sm.isRequestPinShortcutSupported) {
            Toast.makeText(ctx, "Tu launcher no acepta anclar accesos directos.", Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(ctx, LaunchTrampolineActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("combo", combo.id)
        }
        val info = ShortcutInfo.Builder(ctx, "combo_" + combo.id)
            .setShortLabel(combo.name)
            .setLongLabel(combo.name)
            .setIcon(Icon.createWithBitmap(buildIcon(ctx, combo)))
            .setIntent(intent)
            .build()
        sm.requestPinShortcut(info, null)
    }
}
