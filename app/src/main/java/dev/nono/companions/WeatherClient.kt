package dev.nono.companions

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

data class WeatherTown(val name: String,val latitude: Double,val longitude: Double)
object WeatherApi {
    private fun get(url: String): JSONObject {
        val connection=URL(url).openConnection() as HttpsURLConnection
        connection.connectTimeout=8000; connection.readTimeout=8000
        try {
            check(connection.responseCode==200)
            val bytes=connection.inputStream.use { stream ->
                val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(4096)
                while(true) { val count=stream.read(buffer); if(count<0) break; check(output.size()+count<=131072); output.write(buffer,0,count) }
                output.toByteArray()
            }
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }
    fun towns(query: String): List<WeatherTown> {
        val data=get("https://geocoding-api.open-meteo.com/v1/search?count=5&language=fr&format=json&name="+URLEncoder.encode(query.take(100),"UTF-8"))
        val rows=data.optJSONArray("results") ?: return emptyList()
        return (0 until rows.length()).map { i -> val r=rows.getJSONObject(i)
            WeatherTown(listOf(r.getString("name"),r.optString("admin1"),r.optString("country")).filter { it.isNotBlank() }.distinct().joinToString(", "),r.getDouble("latitude"),r.getDouble("longitude")) }
    }
    fun current(town: WeatherTown): WeatherReading {
        require(town.latitude.isFinite() && town.longitude.isFinite() && town.latitude in -90.0..90.0 && town.longitude in -180.0..180.0)
        val r=get("https://api.open-meteo.com/v1/forecast?latitude=${town.latitude}&longitude=${town.longitude}&current=temperature_2m,weather_code,rain&temperature_unit=celsius&timeformat=unixtime").getJSONObject("current")
        return WeatherReading(r.getLong("time")*1000,r.getDouble("temperature_2m").toFloat(),r.getInt("weather_code"),r.optDouble("rain",0.0).toFloat())
    }
}
class WeatherClient(context: Context) {
    private val prefs=context.getSharedPreferences("weather",Context.MODE_PRIVATE)
    private val worker=Executors.newSingleThreadExecutor()
    private val main=Handler(Looper.getMainLooper())
    private var busy=false; private var closed=false; private var next=0L; private var place=""
    var reading: WeatherReading?=cached(); private set
    private fun cached(): WeatherReading? = if(prefs.contains("at")) WeatherReading(prefs.getLong("at",0),prefs.getFloat("temp",Float.NaN),prefs.getInt("code",-1),prefs.getFloat("rain",0f)) else null
    fun refresh(now: Long) {
        if(closed || busy) return
        val selected=prefs.getString("town","").orEmpty()
        if(selected!=place) { place=selected; reading=cached(); next=0 }
        if(selected.isEmpty() || now<next) return
        val town=WeatherTown(selected,prefs.getString("lat","")?.toDoubleOrNull() ?: return,prefs.getString("lon","")?.toDoubleOrNull() ?: return)
        busy=true; next=now+600_000
        worker.execute {
            val result=try { WeatherApi.current(town).takeIf { it.fresh(System.currentTimeMillis()) } } catch(_: Exception) { null }
            main.post {
                busy=false
                if(!closed && prefs.getString("town","")==selected && result!=null) {
                    reading=result; next=System.currentTimeMillis()+1_800_000
                    prefs.edit().putLong("at",result.at).putFloat("temp",result.celsius).putInt("code",result.code).putFloat("rain",result.rainMm).apply()
                }
            }
        }
    }
    fun close() { closed=true; worker.shutdownNow(); main.removeCallbacksAndMessages(null) }
}
