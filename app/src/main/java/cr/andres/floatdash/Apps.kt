package cr.andres.floatdash

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/** App instalada que se puede lanzar. */
data class AppInfo(val pkg: String, val label: String, val icon: Drawable?)

object Apps {

    /** Lista de apps con ícono en el drawer, ordenadas por nombre. */
    fun launchable(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val res = pm.queryIntentActivities(intent, 0)
        val seen = HashSet<String>()
        val out = ArrayList<AppInfo>()
        for (ri in res) {
            val pkg = ri.activityInfo.packageName
            if (pkg == ctx.packageName) continue
            if (!seen.add(pkg)) continue
            val label = ri.loadLabel(pm)?.toString() ?: pkg
            val icon = try { ri.loadIcon(pm) } catch (e: Exception) { null }
            out.add(AppInfo(pkg, label, icon))
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    fun label(ctx: Context, pkg: String?): String {
        if (pkg == null) return "—"
        return try {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) { pkg }
    }

    fun icon(ctx: Context, pkg: String?): Drawable? {
        if (pkg == null) return null
        return try { ctx.packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }
    }
}
