package dev.nono.companions

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState) }
    override fun onResume() { super.onResume(); render() }
    private fun render() {
        val pad=(24*resources.displayMetrics.density).toInt()
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(pad,pad,pad,pad) }
        fun text(s: String, size: Float=17f) { column.addView(TextView(this).apply { text=s; textSize=size; setPadding(0,12,0,20) }) }
        fun button(s: String, action: () -> Unit) { column.addView(Button(this).apply { text=s; setOnClickListener { action() } }) }
        text("NoNo Companions",30f)
        button("Menu développeur") { startActivity(Intent(this,DevActivity::class.java)) }
        text("A little love. A little chaos.\nAnd absolutely no safe snacks.",20f)
        text("Your two companions live over other apps. Tap either for a reaction; drag to move them. They take turns teasing, sharing snacks and keeping you company.")
        val allowed=Settings.canDrawOverlays(this)
        text(if(allowed) "✓ Display over other apps is allowed." else "First, allow display over other apps. Only small character windows receive touch; the rest of your phone stays usable.")
        if(!allowed) button("Allow companions over apps") { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))) }
        val enabled=getSharedPreferences("setup",MODE_PRIVATE).getBoolean("enabled",false)
        if(allowed && !enabled) button("Welcome our companions") {
            getSharedPreferences("setup",MODE_PRIVATE).edit().putBoolean("enabled",true).apply()
            startCompanions(); render()
        }
        if(allowed && enabled) { startCompanions(); text("Your companions are enabled. Android may stop them to protect battery or hide them on protected screens. Reopening this app restores them when allowed.") }
        button("Optional app & keyboard awareness") {
            AlertDialog.Builder(this).setTitle("Local, optional keyboard awareness").setMessage(getString(R.string.context_description)).setNegativeButton("Not now",null).setPositiveButton("Open Android settings") { _,_ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.show()
        }
        if(android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) button("Allow service notification") { requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),7) }
        text("Température de la batterie : utilisée localement. Au-dessus de 43 °C, tenue légère, éventails et ventilateurs ; retour à la normale à 42 °C. Ces animations ne refroidissent pas réellement le téléphone.",15f)
        text("Pyjama de 22 h à 7 h, sauf si la batterie chauffe. Météo en direct non activée dans cette version : autorisation de partage de la ville encore en attente.",14f)
        text("Companion moods and screen context stay on this phone. Live weather is not enabled in this build; no town or coordinates are sent. No accounts, remote commands or screen-text collection. Android provides permission revocation and its active-app service controls. There is no floating toolbar or pause button.",15f)
        text("If Samsung or HyperOS stops the companions, review this app’s battery/background settings. Exact options vary by phone. Screen-off and locked time stays quiet.",15f)
        setContentView(ScrollView(this).apply {
            addView(column)
            setOnApplyWindowInsetsListener { view, insets ->
                val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
                view.setPadding(bars.left,bars.top,bars.right,bars.bottom); insets
            }
        })
    }
    private fun startCompanions() { try { startForegroundService(Intent(this,CompanionService::class.java)) } catch(_: IllegalStateException) { Toast.makeText(this,"Android could not start the companions. Try reopening the app.",Toast.LENGTH_LONG).show() } }
}
