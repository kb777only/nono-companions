package dev.nono.companions

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.pm.ApplicationInfo
import android.os.SystemClock

/** Never accesses event.text, source, rootInActiveWindow, or any accessibility tree. */
class ContextService : AccessibilityService() {
    private var lastAt=0L
    private var lastSignal=ContextSignal.UNKNOWN
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val now=SystemClock.elapsedRealtime()
        if(now-lastAt < 3000) return
        lastAt=now
        val name=event.packageName?.toString() ?: return
        if(name==packageName || event.isPassword) return
        val category=try { packageManager.getApplicationInfo(name,0).category } catch(_: Exception) { -1 }
        val signal=when(category) { ApplicationInfo.CATEGORY_GAME -> ContextSignal.GAME; ApplicationInfo.CATEGORY_AUDIO,ApplicationInfo.CATEGORY_VIDEO -> ContextSignal.MEDIA; ApplicationInfo.CATEGORY_NEWS -> ContextSignal.READING; ApplicationInfo.CATEGORY_PRODUCTIVITY -> ContextSignal.WORK; else -> ContextSignal.UNKNOWN }
        if(signal != lastSignal) { lastSignal=signal; CompanionService.live?.contextSignal(signal) }
    }
    override fun onInterrupt() { lastSignal=ContextSignal.UNKNOWN }
    override fun onUnbind(intent: android.content.Intent?): Boolean { CompanionService.live?.contextSignal(ContextSignal.UNKNOWN); return super.onUnbind(intent) }
}
