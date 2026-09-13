package dev.nono.companions

import android.content.Context
import java.util.Properties
import java.io.Reader
import java.io.Writer

object MoodCodec {
    fun write(world: World, at: Long, out: Writer) {
        val p = Properties(); p.setProperty("version","1"); p.setProperty("at",at.toString()); p.setProperty("bond",world.bond.toString())
        world.pets.forEach { pet -> val n=pet.needs; listOf(n.hunger,n.energy,n.affection,n.mischief).forEachIndexed { i,v -> p.setProperty("${pet.who.name}.$i",v.toString()) } }
        p.store(out,"NoNo local needs; no app or screen data")
    }
    fun read(world: World, now: Long, input: Reader) {
        val p=Properties(); p.load(input)
        if(p.getProperty("version") != "1") return
        fun number(key: String, default: Float) = p.getProperty(key)?.toFloatOrNull()?.takeIf { it.isFinite() } ?: default
        world.bond=number("bond",25f)
        world.pets.forEach { pet -> pet.needs.apply { hunger=number("${pet.who.name}.0",45f); energy=number("${pet.who.name}.1",75f); affection=number("${pet.who.name}.2",65f); mischief=number("${pet.who.name}.3",55f) } }
        val saved=p.getProperty("at")?.toLongOrNull() ?: now
        world.restoreElapsed(if(saved > now) 0 else ((now-saved).coerceAtLeast(0)/1000))
    }
}
class MoodStore(context: Context) {
    private val file=android.util.AtomicFile(java.io.File(context.filesDir,"moods.properties"))
    fun load(world: World) { try { file.openRead().bufferedReader().use { MoodCodec.read(world,System.currentTimeMillis(),it) } } catch(_: Exception) { world.restoreElapsed(0) } }
    fun save(world: World) {
        var stream: java.io.FileOutputStream? = null
        try { stream=file.startWrite(); val writer=java.io.OutputStreamWriter(stream,Charsets.UTF_8); MoodCodec.write(world,System.currentTimeMillis(),writer); writer.flush(); file.finishWrite(stream) } catch(_: Exception) { file.failWrite(stream) }
    }
}
