package cr.andres.floatdash

import android.app.ActivityOptions
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast

/**
 * Lanza una o varias apps en ventanas flotantes (freeform).
 *
 * El problema en Android 10: tras abrir la primera app, este proceso pasa a
 * segundo plano y las siguientes llamadas a startActivity quedan bloqueadas por
 * la restricción de "background activity launch". Solución: mientras dura la
 * secuencia mostramos una ventana overlay mínima (TYPE_APPLICATION_OVERLAY).
 * Tener una ventana visible nos da la excepción que permite seguir lanzando.
 *
 * Extras aceptados:
 *   "combo" -> id de un combo guardado (varias apps)
 *   "pkg"   -> un solo paquete (acceso rápido), región por defecto mitad derecha
 */
class LaunchService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tiles = resolveTiles(intent)
        if (tiles.isEmpty()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val useOverlay = canOverlay()
        if (tiles.size > 1 && !canOverlay()) {
            Toast.makeText(
                this,
                "Para abrir varias apps dale permiso \"Mostrar sobre otras apps\" en FloatDash.",
                Toast.LENGTH_LONG
            ).show()
        }
        if (useOverlay) addOverlay()

        val step = 420L
        tiles.forEachIndexed { i, t ->
            handler.postDelayed({ launchOne(t) }, i * step)
        }
        handler.postDelayed({
            removeOverlay()
            stopSelf()
        }, tiles.size * step + 600L)

        return START_NOT_STICKY
    }

    private fun resolveTiles(intent: Intent?): List<Tile> {
        val comboId = intent?.getStringExtra("combo")
        if (comboId != null) {
            val combo = ComboStore(this).get(comboId) ?: return emptyList()
            return combo.tiles.filter { !it.pkg.isNullOrEmpty() }
        }
        val pkg = intent?.getStringExtra("pkg")
        if (!pkg.isNullOrEmpty()) {
            return listOf(Tile(pkg, 0.5f, 0f, 1f, 1f))
        }
        return emptyList()
    }

    private fun launchOne(t: Tile) {
        val pkg = t.pkg ?: return
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch == null) {
            Toast.makeText(this, "No instalada: $pkg", Toast.LENGTH_SHORT).show()
            return
        }
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        )
        val bounds = Geometry.toPixels(this, t.left, t.top, t.right, t.bottom)
        val opts = ActivityOptions.makeBasic()
        opts.launchBounds = bounds
        forceFreeform(opts)
        try {
            startActivity(launch, opts.toBundle())
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir $pkg: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Fuerza el modo de ventana freeform. En varias ROM (TS18/DoFun) las apps
     * se promueven a pantalla completa aunque se pasen bounds, porque no se fija
     * el "windowing mode". setLaunchWindowingMode es un método oculto del SDK,
     * así que lo invocamos por reflexión. WINDOWING_MODE_FREEFORM = 5.
     */
    private fun forceFreeform(opts: ActivityOptions) {
        try {
            val m = ActivityOptions::class.java.getMethod(
                "setLaunchWindowingMode", Int::class.javaPrimitiveType
            )
            m.invoke(opts, 5)
        } catch (e: Throwable) {
        }
    }

    private fun canOverlay(): Boolean = Settings.canDrawOverlays(this)

    private fun addOverlay() {
        if (overlay != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            2, 2, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        val v = View(this)
        try {
            wm.addView(v, lp)
            overlay = v
        } catch (e: Exception) {
            overlay = null
        }
    }

    private fun removeOverlay() {
        val v = overlay ?: return
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeView(v)
        } catch (e: Exception) {
        }
        overlay = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        super.onDestroy()
    }
}
