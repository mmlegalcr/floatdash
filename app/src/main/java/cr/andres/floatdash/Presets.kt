package cr.andres.floatdash

object Presets {

    /** Regiones base por preset; se conservan los pkg existentes por índice. */
    fun rects(preset: String): List<FloatArray> = when (preset) {
        "1" -> listOf(floatArrayOf(0.5f, 0f, 1f, 1f))
        "2h" -> listOf(floatArrayOf(0f, 0f, 1f, 0.5f), floatArrayOf(0f, 0.5f, 1f, 1f))
        "3" -> listOf(
            floatArrayOf(0f, 0f, 0.5f, 1f),
            floatArrayOf(0.5f, 0f, 1f, 0.5f),
            floatArrayOf(0.5f, 0.5f, 1f, 1f)
        )
        "4" -> listOf(
            floatArrayOf(0f, 0f, 0.5f, 0.5f),
            floatArrayOf(0.5f, 0f, 1f, 0.5f),
            floatArrayOf(0f, 0.5f, 0.5f, 1f),
            floatArrayOf(0.5f, 0.5f, 1f, 1f)
        )
        else -> listOf(floatArrayOf(0f, 0f, 0.5f, 1f), floatArrayOf(0.5f, 0f, 1f, 1f)) // "2v"
    }

    fun apply(preset: String, current: List<Tile>): MutableList<Tile> {
        val rs = rects(preset)
        val out = ArrayList<Tile>()
        rs.forEachIndexed { i, r ->
            val pkg = current.getOrNull(i)?.pkg
            out.add(Tile(pkg, r[0], r[1], r[2], r[3]))
        }
        return out
    }
}
