package dev.nono.companions

enum class Outfit { DEFAULT, COLD, HOT, NIGHT }
enum class WeatherKind { CLEAR, CLOUD, FOG, RAIN, SNOW, STORM, UNKNOWN }
data class WeatherReading(val at: Long,val celsius: Float,val code: Int,val rainMm: Float) {
    fun fresh(now: Long)=celsius.isFinite() && celsius in -100f..70f && now-at in 0..10_800_000L
    val kind: WeatherKind get()=when(code) {
        0 -> WeatherKind.CLEAR
        1,2,3 -> WeatherKind.CLOUD
        45,48 -> WeatherKind.FOG
        51,53,55,56,57,61,63,65,66,67,80,81,82 -> WeatherKind.RAIN
        71,73,75,77,85,86 -> WeatherKind.SNOW
        95,96,99 -> WeatherKind.STORM
        else -> WeatherKind.UNKNOWN
    }
    val raining: Boolean get()=kind==WeatherKind.RAIN || kind==WeatherKind.STORM || (rainMm.isFinite() && rainMm>0)
}

class WeatherState {
    var outfit=Outfit.DEFAULT; private set
    var deviceHot=false; private set
    var raining=false; private set
    var temperature: Float?=null; private set
    var kind=WeatherKind.UNKNOWN; private set
    var hour=12; private set
    fun update(reading: WeatherReading?,now: Long,localHour: Int, deviceHot: Boolean=false, kindOverride: WeatherKind?=null, tempOverride: Float?=null): Boolean {
        val old=outfit to raining
        this.deviceHot=deviceHot
        hour=localHour.coerceIn(0,23)
        val current=reading?.takeIf { it.fresh(now) }
        temperature=tempOverride?.takeIf { it.isFinite() && it in -100f..70f } ?: current?.celsius; kind=kindOverride ?: current?.kind ?: WeatherKind.UNKNOWN
        raining=if(kindOverride!=null) kindOverride in listOf(WeatherKind.RAIN,WeatherKind.STORM) else current?.raining==true
        val celsius=temperature
        outfit=when {
            deviceHot -> Outfit.HOT
            hour>=22 || hour<7 -> Outfit.NIGHT
            kind==WeatherKind.SNOW -> Outfit.COLD
            celsius==null -> Outfit.DEFAULT
            celsius<12 || (outfit==Outfit.COLD && celsius<14) -> Outfit.COLD
            celsius>=26 || (outfit==Outfit.HOT && celsius>=24) -> Outfit.HOT
            else -> Outfit.DEFAULT
        }
        return old!=(outfit to raining)
    }
    fun line(who: Who): String = when {
        deviceHot -> if(who==Who.HUSBAND) "Le téléphone chauffe… éventail prêt !" else "Un peu d’air, mon cœur ?"
        outfit==Outfit.NIGHT -> if(who==Who.HUSBAND) "Pyjama… et dernier biscuit ?" else "Viens te blottir."
        raining -> if(who==Who.HUSBAND) "Parapluie déployé !" else "Viens sous mon parapluie."
        outfit==Outfit.COLD -> if(who==Who.HUSBAND) "${temperature?.toInt()} °C… capuche bien en place." else "Un câlin pour se réchauffer ?"
        outfit==Outfit.HOT -> if(who==Who.HUSBAND) "${temperature?.toInt()} °C… une glace ?" else "On cherche un peu d’ombre ?"
        hour<11 -> "Bonjour, toi."
        hour>=18 -> "Petite soirée à deux ?"
        kind==WeatherKind.SNOW -> "Des flocons !"
        kind==WeatherKind.FOG -> "On reste tout près."
        else -> "On est bien, là."
    }
}

/** Map existing action timing to consistent dressed poses, without changing the behavior controller. */
fun dressedFrame(frame: Int): Int = when(frame) {
    48,51 -> 9; 49,50 -> 6
    52,55 -> 0; 53,54 -> 5
    56 -> 0; 57,58 -> 10; 59 -> 1
    60,62 -> 2; 61,63 -> 3
    0,2 -> 2; 1,3 -> 3
    5,6 -> 1
    8,9,10,11,23 -> 8
    12,13,14 -> 5
    20,21,22 -> 10
    24,25,26,27 -> 4
    28,29,30,31 -> 6
    32,33,34,35 -> 11
    36,37,38 -> 7
    40,41 -> 9
    else -> 0
}
