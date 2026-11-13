package cr.andres.floatdash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.btnNew).setOnClickListener {
            startActivity(Intent(this, EditComboActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnOverlay).setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
        findViewById<MaterialButton>(R.id.btnWaze).setOnClickListener { quickFloat("com.waze") }
        findViewById<MaterialButton>(R.id.btnMaps).setOnClickListener { quickFloat("com.google.android.apps.maps") }
    }

    override fun onResume() {
        super.onResume()
        updateOverlayBanner()
        buildComboList()
    }

    private fun quickFloat(pkg: String) {
        startService(Intent(this, LaunchService::class.java).putExtra("pkg", pkg))
    }

    private fun updateOverlayBanner() {
        val ok = Settings.canDrawOverlays(this)
        val banner = findViewById<TextView>(R.id.overlayText)
        val btn = findViewById<MaterialButton>(R.id.btnOverlay)
        if (ok) {
            banner.text = "Permiso \"Mostrar sobre otras apps\": concedido"
            btn.visibility = View.GONE
        } else {
            banner.text = "Para abrir varias apps a la vez, dale permiso \"Mostrar sobre otras apps\"."
            btn.visibility = View.VISIBLE
        }
    }

    private fun buildComboList() {
        val container = findViewById<LinearLayout>(R.id.comboList)
        container.removeAllViews()
        val store = ComboStore(this)
        val combos = store.list()
        findViewById<TextView>(R.id.emptyHint).visibility =
            if (combos.isEmpty()) View.VISIBLE else View.GONE

        combos.forEach { combo ->
            val row = layoutInflater.inflate(R.layout.row_combo, container, false)
            row.findViewById<ImageView>(R.id.comboIcon).setImageBitmap(IconFactory.buildIcon(this, combo))
            row.findViewById<TextView>(R.id.comboName).text = combo.name
            val apps = combo.tiles.filter { !it.pkg.isNullOrEmpty() }.joinToString(" · ") { Apps.label(this, it.pkg) }
            row.findViewById<TextView>(R.id.comboApps).text =
                "${combo.tiles.count { !it.pkg.isNullOrEmpty() }} apps · $apps"

            row.findViewById<ImageView>(R.id.actPlay).setOnClickListener {
                startService(Intent(this, LaunchService::class.java).putExtra("combo", combo.id))
            }
            row.findViewById<ImageView>(R.id.actEdit).setOnClickListener {
                startActivity(Intent(this, EditComboActivity::class.java).putExtra("combo", combo.id))
            }
            row.findViewById<ImageView>(R.id.actPin).setOnClickListener {
                IconFactory.pin(this, combo)
            }
            row.findViewById<ImageView>(R.id.actDelete).setOnClickListener {
                AlertDialog.Builder(this)
                    .setMessage("¿Borrar \"${combo.name}\"?")
                    .setPositiveButton("Borrar") { _, _ -> store.delete(combo.id); buildComboList() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            container.addView(row)
        }
    }
}
