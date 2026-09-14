package dev.nono.companions

import kotlin.math.*

enum class DevVariable(val label: String,val min: Int,val max: Int,val initial: Int) {
    WEATHER_KIND("État météo",0,7,0),
    DEVICE_TEMP("Température batterie (°C)",-100,800,450),
    WEATHER_TEMP("Température extérieure (°C)",-400,550,100),
    HOUR("Heure de la journée",0,23,12),
    ORIENTATION("Orientation des compagnons",0,3,0),
    KEYBOARD("Clavier simulé",0,1,1),
    CONTEXT("Contexte d’application",0,4,0);
    val choices: List<String> get()=when(this) {
        WEATHER_KIND -> listOf("Soleil","Nuages","Brouillard","Pluie","Neige","Orage","Inconnu")
        ORIENTATION -> listOf("0°","90°","180°","−90°")
        KEYBOARD -> listOf("Fermé","Ouvert")
        CONTEXT -> listOf("Inconnu","Lecture","Jeu","Média","Travail")
        else -> emptyList()
    }
    fun parse(text: String): Int? {
        val number=text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val scaled=if(this==DEVICE_TEMP || this==WEATHER_TEMP) number*10 else number
        if(scaled<min || scaled>max || (this==HOUR && scaled!=floor(scaled))) return null
        return scaled.roundToInt().takeIf(::valid)
    }
    fun valid(value: Int)=value in min..max && (choices.isEmpty() || value<choices.size)
    fun display(value: Int)=when(this) {
        DEVICE_TEMP,WEATHER_TEMP -> String.format(java.util.Locale.FRANCE,"%.1f °C",value/10f)
        HOUR -> "%02d:00".format(value)
        else -> choices.getOrElse(value) { "Inconnu" }
    }
}

/** Null means Auto. Manual values replace only their own input, never the real sensor cache. */
class DevOverrides(raw: Map<String,*>) {
    private val values=DevVariable.entries.mapNotNull { key ->
        (raw[key.name] as? Int)?.takeIf(key::valid)?.let { key to it }
    }.toMap()
    operator fun get(key: DevVariable)=values[key]
    val kind get()=this[DevVariable.WEATHER_KIND]?.let { WeatherKind.entries[it] }
    val temperature get()=this[DevVariable.WEATHER_TEMP]?.div(10f)
    fun battery(real: Int?)=this[DevVariable.DEVICE_TEMP] ?: real
    fun hour(real: Int)=this[DevVariable.HOUR] ?: real
    val angle get()=this[DevVariable.ORIENTATION]?.let { listOf(0f,90f,180f,-90f)[it] }
    val gravity: GravityVector? get()=angle?.let {
        val radians=Math.toRadians(it.toDouble()); GravityVector(-sin(radians).toFloat(),cos(radians).toFloat())
    }
    val keyboard get()=this[DevVariable.KEYBOARD]?.let { it==1 }
    val context get()=this[DevVariable.CONTEXT]?.let { ContextSignal.entries[it] }
}
