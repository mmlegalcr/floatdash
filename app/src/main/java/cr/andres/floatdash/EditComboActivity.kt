package cr.andres.floatdash

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class EditComboActivity : AppCompatActivity() {

    private lateinit var combo: Combo
    private lateinit var editor: LayoutEditorView
    private lateinit var etName: EditText
    private var appCache: List<AppInfo>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val store = ComboStore(this)
        val id = intent.getStringExtra("combo")
        combo = if (id != null) (store.get(id) ?: newCombo()) else newCombo()

        etName = findViewById(R.id.etName)
        etName.setText(combo.name)

        editor = findViewById(R.id.editor)
        val (sw, sh) = Geometry.screen(this)
        editor.setAspect(sw, sh)
        editor.setTiles(this, combo.tiles)
        editor.onEmptyTap = { idx -> pickApp(idx) }
        editor.onTileTap = { idx -> tileOptions(idx) }

        bindPreset(R.id.p1, "1")
        bindPreset(R.id.p2v, "2v")
        bindPreset(R.id.p2h, "2h")
        bindPreset(R.id.p3, "3")
        bindPreset(R.id.p4, "4")

        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save() }
    }

    private fun newCombo(): Combo {
        val tiles = Presets.apply("2v", emptyList())
        return Combo(ComboStore.newId(), "Combo", tiles)
    }

    private fun bindPreset(viewId: Int, preset: String) {
        findViewById<MaterialButton>(viewId).setOnClickListener {
            val merged = Presets.apply(preset, combo.tiles)
            combo.tiles.clear()
            combo.tiles.addAll(merged)
            editor.setTiles(this, combo.tiles)
        }
    }

    private fun apps(): List<AppInfo> {
        var c = appCache
        if (c == null) { c = Apps.launchable(this); appCache = c }
        return c
    }

    private fun pickApp(idx: Int) {
        val list = apps()
        val adapter = object : ArrayAdapter<AppInfo>(this, 0, list) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: layoutInflater.inflate(R.layout.item_app, parent, false)
                val a = getItem(position)!!
                v.findViewById<ImageView>(R.id.appIcon).setImageDrawable(a.icon)
                v.findViewById<TextView>(R.id.appLabel).text = a.label
                return v
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Elegí una app")
            .setAdapter(adapter) { _, which ->
                combo.tiles[idx].pkg = list[which].pkg
                editor.refresh(this)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun tileOptions(idx: Int) {
        AlertDialog.Builder(this)
            .setItems(arrayOf("Cambiar app", "Quitar app")) { _, which ->
                if (which == 0) pickApp(idx)
                else { combo.tiles[idx].pkg = null; editor.refresh(this) }
            }
            .show()
    }

    private fun save() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) { Toast.makeText(this, "Ponle un nombre", Toast.LENGTH_SHORT).show(); return }
        if (combo.tiles.none { !it.pkg.isNullOrEmpty() }) {
            Toast.makeText(this, "Asigná al menos una app", Toast.LENGTH_SHORT).show(); return
        }
        combo.name = name
        ComboStore(this).put(combo)
        Toast.makeText(this, "Combo guardado", Toast.LENGTH_SHORT).show()
        finish()
    }
}
