package dev.nono.companions

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.pm.ApplicationInfo
import android.os.SystemClock

/** Never accesses event.text, source, rootInActiveWindow, or any accessibility tree. */
class ContextService : AccessibilityService() {
    companion object { var keyboard: Pair<Boolean,Int?>?=null; private set }
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private var pending=false
    private var imeIds=emptySet<Int>()
    private val inspectWindows=Runnable {
        pending=false
        val list=try { windows } catch(_: SecurityException) { emptyList() }
        if(list.isEmpty()) { keyboard=null; CompanionService.live?.keyboardWindow(null,null) }
        else {
            val ime=list.filter { it.type==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            imeIds=ime.map { it.id }.toSet()
            val top=ime.map { val bounds=android.graphics.Rect(); it.getBoundsInScreen(bounds); bounds.top }.minOrNull()
            keyboard=(ime.isNotEmpty() to top)
            CompanionService.live?.keyboardWindow(ime.isNotEmpty(),top)
        }
    }
    override fun onServiceConnected() { handler.post(inspectWindows) }
    private var lastAt=0L
    private var lastSignal=ContextSignal.UNKNOWN
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val changed=event?.eventType==AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            ((event.windowChanges and (AccessibilityEvent.WINDOWS_CHANGE_ADDED or AccessibilityEvent.WINDOWS_CHANGE_REMOVED))!=0 || event.windowId in imeIds)
        if((changed || event?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) && !pending) {
            pending=true; handler.postDelayed(inspectWindows,120)
        }
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
    override fun onUnbind(intent: android.content.Intent?): Boolean { handler.removeCallbacksAndMessages(null); keyboard=null; CompanionService.live?.keyboardWindow(null,null); CompanionService.live?.contextSignal(ContextSignal.UNKNOWN); return super.onUnbind(intent) }
}
