package dev.nono.companions

import android.content.*
import android.provider.Settings

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if(!Settings.canDrawOverlays(context) || !context.getSharedPreferences("setup",Context.MODE_PRIVATE).getBoolean("enabled",false)) return
        // BOOT_COMPLETED exemption; specialUse is not one of the prohibited boot types.
        try { context.startForegroundService(Intent(context,CompanionService::class.java)) } catch(_: IllegalStateException) { /* User can reopen launcher; no retry loop. */ } catch(_: SecurityException) { }
    }
}
