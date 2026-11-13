package cr.andres.floatdash

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle

/**
 * Activity invisible. La abren los accesos directos pin de Nova (con extra
 * "combo") y los alias Waze/Maps (con meta-data "target_pkg"). Arranca el
 * LaunchService y se cierra.
 */
class LaunchTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val svc = Intent(this, LaunchService::class.java)
        val comboId = intent?.getStringExtra("combo")
        val pkgExtra = intent?.getStringExtra("pkg")
        val metaPkg = readMetaPkg()

        when {
            !comboId.isNullOrEmpty() -> svc.putExtra("combo", comboId)
            !pkgExtra.isNullOrEmpty() -> svc.putExtra("pkg", pkgExtra)
            !metaPkg.isNullOrEmpty() -> svc.putExtra("pkg", metaPkg)
        }

        try {
            startService(svc)
        } catch (e: Exception) {
        }
        finish()
    }

    private fun readMetaPkg(): String? {
        return try {
            val info = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
            info.metaData?.getString("target_pkg")
        } catch (e: Exception) {
            null
        }
    }
}
