package dev.nono.companions

import android.app.Activity
import android.app.AlertDialog
import android.widget.EditText
import android.widget.Toast
import java.util.concurrent.Executors

object WeatherSetup {
    fun show(activity: Activity) {
        val prefs=activity.getSharedPreferences("weather",Activity.MODE_PRIVATE)
        val input=EditText(activity).apply { hint="Ville ou commune"; setSingleLine(true) }
        AlertDialog.Builder(activity).setTitle("Météo locale")
            .setMessage("Choisissez une ville. Open-Meteo recevra cette recherche puis les coordonnées de la ville pour la météo, ainsi que votre adresse IP. Aucun GPS, texte d’écran ou message n’est envoyé. Les pyjamas suivent l’heure du téléphone (22 h–7 h).\n\nActuellement : "+prefs.getString("town","aucune ville"))
            .setView(input).setNegativeButton("Annuler",null)
            .setNeutralButton("Effacer la ville") { _,_ -> prefs.edit().clear().apply(); Toast.makeText(activity,"Météo désactivée ; horaires conservés.",Toast.LENGTH_SHORT).show() }
            .setPositiveButton("Chercher") { _,_ ->
                val query=input.text.toString().trim()
                if(query.length<2) { Toast.makeText(activity,"Indiquez une ville.",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                Toast.makeText(activity,"Recherche…",Toast.LENGTH_SHORT).show()
                val worker=Executors.newSingleThreadExecutor()
                worker.execute {
                    val towns=try { WeatherApi.towns(query) } catch(_: Exception) { emptyList() }
                    activity.runOnUiThread {
                        if(!activity.isDestroyed && !activity.isFinishing) {
                            if(towns.isEmpty()) Toast.makeText(activity,"Aucune ville trouvée ou connexion indisponible.",Toast.LENGTH_LONG).show()
                            else AlertDialog.Builder(activity).setTitle("Choisir la ville").setItems(towns.map { it.name }.toTypedArray()) { _,i ->
                                val town=towns[i]
                                prefs.edit().clear().putString("town",town.name).putString("lat",town.latitude.toString()).putString("lon",town.longitude.toString()).apply()
                                Toast.makeText(activity,"Météo activée ; mise à jour dans une minute.",Toast.LENGTH_LONG).show()
                            }.show()
                        }
                    }; worker.shutdown()
                }
            }.show()
    }
}
