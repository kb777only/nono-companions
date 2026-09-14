package dev.nono.companions

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object AccessibilitySetup {
    fun status(activity: Activity): String {
        val manager=activity.getSystemService(AccessibilityManager::class.java)
        fun ours(info: AccessibilityServiceInfo)=info.resolveInfo.serviceInfo.let {
            it.packageName==activity.packageName && it.name==ContextService::class.java.name
        }
        val installed=manager.installedAccessibilityServiceList.any(::ours)
        val enabled=manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any(::ours)
        val manual=DevOverrides(activity.getSharedPreferences("dev_overrides",Activity.MODE_PRIVATE).all).keyboard
        return "Détection du clavier : " + when {
            manual!=null -> "mode manuel (${if(manual) "ouvert" else "fermé"}). Remettez Clavier en Auto dans le menu développeur."
            !installed -> "service absent de la liste Android. Réinstallez cette mise à jour, puis vérifiez les paramètres d’accessibilité."
            !enabled -> "service reconnu par Android, mais désactivé."
            !ContextService.connected -> "autorisé, mais non connecté. Désactivez puis réactivez NoNo dans Accessibilité."
            else -> "service connecté — ${if(ContextService.keyboard?.first==true) "clavier détecté" else "en attente du clavier"}."
        }
    }
    fun open(activity: Activity) {
        // Standard system page; component extra lets supporting vendors highlight this service.
        val intent=Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .putExtra(Intent.EXTRA_COMPONENT_NAME,ComponentName(activity,ContextService::class.java))
        activity.startActivity(intent)
    }
}
