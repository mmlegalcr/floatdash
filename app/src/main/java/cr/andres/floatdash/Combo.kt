package cr.andres.floatdash

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Una app ubicada en una región de la pantalla (fracciones 0..1). */
data class Tile(
    var pkg: String?,
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float
)

/** Un combo = nombre + varias apps en sus regiones. */
data class Combo(
    val id: String,
    var name: String,
    val tiles: MutableList<Tile>
)

/** Persistencia simple de combos en SharedPreferences como JSON. */
class ComboStore(ctx: Context) {

    private val prefs = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(): MutableList<Combo> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        val out = mutableListOf<Combo>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            out.add(fromJson(arr.getJSONObject(i)))
        }
        return out
    }

    fun get(id: String): Combo? = list().firstOrNull { it.id == id }

    fun put(combo: Combo) {
        val all = list()
        val idx = all.indexOfFirst { it.id == combo.id }
        if (idx >= 0) all[idx] = combo else all.add(combo)
        save(all)
    }

    fun delete(id: String) {
        val all = list().filter { it.id != id }
        save(all)
    }

    private fun save(all: List<Combo>) {
        val arr = JSONArray()
        all.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(c: Combo): JSONObject {
        val o = JSONObject()
        o.put("id", c.id)
        o.put("name", c.name)
        val tiles = JSONArray()
        c.tiles.forEach { t ->
            val to = JSONObject()
            to.put("pkg", t.pkg ?: JSONObject.NULL)
            to.put("l", t.left.toDouble())
            to.put("t", t.top.toDouble())
            to.put("r", t.right.toDouble())
            to.put("b", t.bottom.toDouble())
            tiles.put(to)
        }
        o.put("tiles", tiles)
        return o
    }

    private fun fromJson(o: JSONObject): Combo {
        val tiles = mutableListOf<Tile>()
        val arr = o.optJSONArray("tiles") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val to = arr.getJSONObject(i)
            val pkg = if (to.isNull("pkg")) null else to.optString("pkg", null)
            tiles.add(
                Tile(
                    pkg,
                    to.optDouble("l", 0.0).toFloat(),
                    to.optDouble("t", 0.0).toFloat(),
                    to.optDouble("r", 1.0).toFloat(),
                    to.optDouble("b", 1.0).toFloat()
                )
            )
        }
        return Combo(o.optString("id", UUID.randomUUID().toString()), o.optString("name", "Combo"), tiles)
    }

    companion object {
        const val PREFS = "floatdash"
        const val KEY = "combos"
        fun newId(): String = UUID.randomUUID().toString()
    }
}
