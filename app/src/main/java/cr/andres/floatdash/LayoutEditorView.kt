package cr.andres.floatdash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Lienzo con la proporción de la pantalla real. Cada Tile se dibuja como una
 * región que se puede arrastrar (mover) o redimensionar desde la esquina
 * inferior derecha. Un toque corto sobre una región llama onTileTap; sobre una
 * región sin app, onEmptyTap ayuda a asignarla.
 */
class LayoutEditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private class EItem(val tile: Tile, var label: String, var icon: Drawable?)

    private val items = ArrayList<EItem>()

    /** ancho/alto de la pantalla real, para la proporción del lienzo. */
    private var aspect = 16f / 9f

    var onTileTap: ((Int) -> Unit)? = null
    var onEmptyTap: ((Int) -> Unit)? = null

    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1.8f)
    }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(12f)
    }
    private val handleP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22344f"); style = Paint.Style.STROKE; strokeWidth = dp(0.6f)
    }

    private val palette = intArrayOf(
        0xFF1D9E75.toInt(), 0xFFE24B4A.toInt(), 0xFF378ADD.toInt(), 0xFF7F77DD.toInt()
    )

    private var activeIdx = -1
    private var resizing = false
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var moved = false
    private val slop = dp(6f)

    fun setAspect(screenW: Int, screenH: Int) {
        if (screenW > 0 && screenH > 0) {
            aspect = screenW.toFloat() / screenH.toFloat()
            requestLayout()
        }
    }

    fun setTiles(ctx: Context, tiles: List<Tile>) {
        items.clear()
        tiles.forEach { t ->
            items.add(EItem(t, Apps.label(ctx, t.pkg), Apps.icon(ctx, t.pkg)))
        }
        invalidate()
    }

    fun refresh(ctx: Context) {
        items.forEach { it.label = Apps.label(ctx, it.tile.pkg); it.icon = Apps.icon(ctx, it.tile.pkg) }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (w / aspect).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawColor(Color.parseColor("#060a12"))
        // rejilla ligera (tercios)
        canvas.drawLine(w / 3f, 0f, w / 3f, h, gridP)
        canvas.drawLine(2 * w / 3f, 0f, 2 * w / 3f, h, gridP)
        canvas.drawLine(0f, h / 2f, w, h / 2f, gridP)

        items.forEachIndexed { i, it ->
            val t = it.tile
            val r = RectF(t.left * w, t.top * h, t.right * w, t.bottom * h)
            val color = palette[i % palette.size]
            fillP.color = (color and 0x00FFFFFF) or 0x55000000
            strokeP.color = color
            val rad = dp(6f)
            canvas.drawRoundRect(r, rad, rad, fillP)
            canvas.drawRoundRect(r, rad, rad, strokeP)

            val cx = r.centerX()
            val cy = r.centerY()
            val icon = it.icon
            if (t.pkg.isNullOrEmpty() || icon == null) {
                textP.color = color
                textP.textSize = dp(22f)
                canvas.drawText("+", cx, cy, textP)
                textP.textSize = dp(11f)
                canvas.drawText("Añadir app", cx, cy + dp(18f), textP)
            } else {
                val sz = dp(34f).toInt().coerceAtMost((r.width() * 0.5f).toInt()).coerceAtMost((r.height() * 0.5f).toInt())
                if (sz > dp(10f)) {
                    icon.bounds = Rect((cx - sz / 2).toInt(), (cy - sz / 2 - dp(6f)).toInt(), (cx + sz / 2).toInt(), (cy + sz / 2 - dp(6f)).toInt())
                    icon.draw(canvas)
                }
                textP.color = Color.WHITE
                textP.textSize = dp(11f)
                canvas.drawText(ellipsize(it.label, r.width()), cx, cy + sz / 2f + dp(8f), textP)
            }

            // manija de redimensión (esquina inf. der.)
            handleP.color = color
            val hs = dp(12f)
            canvas.drawCircle(r.right - dp(3f), r.bottom - dp(3f), hs / 2f, handleP)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downX = event.x; downY = event.y
                lastX = event.x; lastY = event.y
                moved = false
                activeIdx = hitTile(event.x, event.y)
                resizing = activeIdx >= 0 && inHandle(activeIdx, event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (abs(event.x - downX) > slop || abs(event.y - downY) > slop) moved = true
                if (activeIdx >= 0 && moved) {
                    val dxF = (event.x - lastX) / w
                    val dyF = (event.y - lastY) / h
                    val t = items[activeIdx].tile
                    if (resizing) {
                        t.right = (t.right + dxF).coerceIn(t.left + 0.15f, 1f)
                        t.bottom = (t.bottom + dyF).coerceIn(t.top + 0.15f, 1f)
                    } else {
                        val tw = t.right - t.left
                        val th = t.bottom - t.top
                        var nl = (t.left + dxF).coerceIn(0f, 1f - tw)
                        var nt = (t.top + dyF).coerceIn(0f, 1f - th)
                        t.left = nl; t.top = nt; t.right = nl + tw; t.bottom = nt + th
                    }
                    invalidate()
                }
                lastX = event.x; lastY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!moved) {
                    if (activeIdx >= 0) {
                        if (items[activeIdx].tile.pkg.isNullOrEmpty()) onEmptyTap?.invoke(activeIdx)
                        else onTileTap?.invoke(activeIdx)
                    }
                }
                activeIdx = -1; resizing = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTile(x: Float, y: Float): Int {
        val w = width.toFloat(); val h = height.toFloat()
        for (i in items.indices.reversed()) {
            val t = items[i].tile
            if (x >= t.left * w && x <= t.right * w && y >= t.top * h && y <= t.bottom * h) return i
        }
        return -1
    }

    private fun inHandle(i: Int, x: Float, y: Float): Boolean {
        val w = width.toFloat(); val h = height.toFloat()
        val t = items[i].tile
        val hx = t.right * w; val hy = t.bottom * h
        val rad = dp(18f)
        return abs(x - hx) <= rad && abs(y - hy) <= rad
    }

    private fun ellipsize(s: String, maxW: Float): String {
        if (textP.measureText(s) <= maxW - dp(8f)) return s
        var str = s
        while (str.length > 1 && textP.measureText("$str…") > maxW - dp(8f)) str = str.dropLast(1)
        return "$str…"
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
