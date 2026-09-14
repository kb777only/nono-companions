package dev.nono.companions

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*

class DevActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("dev_overrides",MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); render() }
    private fun render() {
        val pad=(20*resources.displayMetrics.density).toInt()
        val column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(pad,pad,pad,pad) }
        fun text(value: String,size: Float=16f) { column.addView(TextView(this).apply { text=value; textSize=size; setPadding(0,12,0,12) }) }
        text("Menu développeur",26f)
        text("Chaque variable peut rester en Auto ou utiliser une valeur manuelle. Les changements sont immédiats et conservés après fermeture. Ils simulent les entrées des compagnons sans modifier le téléphone.")
        text("Auto : capteurs et heure du téléphone. La météo automatique est encore indisponible ; les essais météo manuels fonctionnent entièrement hors ligne. Une batterie simulée au-dessus de 43 °C garde la priorité sur les vêtements.",14f)
        column.addView(Button(this).apply { text="Tout remettre en Auto"; setOnClickListener { prefs.edit().clear().apply(); render() } })
        val settings=DevOverrides(prefs.all)
        DevVariable.entries.forEach { key ->
            val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,16,0,16) }
            column.addView(box)
            box.addView(TextView(this).apply { text=key.label; textSize=19f })
            var value=settings[key] ?: key.initial
            val mode=Switch(this).apply { text=if(settings[key]==null) "Auto" else "Manuel"; isChecked=settings[key]!=null }
            box.addView(mode)
            val label=TextView(this).apply { text="Valeur manuelle : ${key.display(value)}"; textSize=17f }
            box.addView(label)
            val control: View
            if(key.choices.isNotEmpty()) {
                control=Spinner(this).apply {
                    adapter=ArrayAdapter(this@DevActivity,android.R.layout.simple_spinner_dropdown_item,key.choices)
                    setSelection(value)
                    onItemSelectedListener=object: AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {
                            if(position==value) return
                            value=position; label.text="Valeur manuelle : ${key.display(value)}"
                            if(mode.isChecked) prefs.edit().putInt(key.name,value).apply()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            } else {
                control=SeekBar(this).apply {
                    max=key.max-key.min; progress=value-key.min
                    contentDescription=key.label
                    setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: SeekBar?,progress: Int,fromUser: Boolean) {
                            value=progress+key.min; label.text="Valeur manuelle : ${key.display(value)}"
                            if(fromUser && mode.isChecked) prefs.edit().putInt(key.name,value).apply()
                        }
                        override fun onStartTrackingTouch(bar: SeekBar?) {}
                        override fun onStopTrackingTouch(bar: SeekBar?) {}
                    })
                }
            }
            control.isEnabled=mode.isChecked; label.alpha=if(mode.isChecked) 1f else .45f
            box.addView(control)
            val exact=if(key.choices.isEmpty()) Button(this).apply {
                text="Saisir une valeur précise"; isEnabled=mode.isChecked
                setOnClickListener {
                    val input=EditText(this@DevActivity).apply {
                        inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                        setText(if(key==DevVariable.HOUR) value.toString() else (value/10f).toString())
                        selectAll()
                    }
                    val dialog=android.app.AlertDialog.Builder(this@DevActivity).setTitle(key.label)
                        .setMessage("De ${key.display(key.min)} à ${key.display(key.max)}")
                        .setView(input).setNegativeButton("Annuler",null).setPositiveButton("Appliquer",null).create()
                    dialog.setOnShowListener {
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val parsed=key.parse(input.text.toString())
                            if(parsed==null) input.error="Valeur hors limites ou invalide"
                            else {
                                value=parsed; (control as SeekBar).progress=value-key.min
                                label.text="Valeur manuelle : ${key.display(value)}"
                                prefs.edit().putInt(key.name,value).apply(); dialog.dismiss()
                            }
                        }
                    }
                    dialog.show()
                }
                box.addView(this)
            } else null
            mode.setOnCheckedChangeListener { _,manual ->
                mode.text=if(manual) "Manuel" else "Auto"; control.isEnabled=manual; exact?.isEnabled=manual; label.alpha=if(manual) 1f else .45f
                if(manual) prefs.edit().putInt(key.name,value).apply() else prefs.edit().remove(key.name).apply()
            }
        }
        text("Orientation : rotation et gravité des compagnons uniquement. Clavier simulé : hauteur de 40 % de l’écran. Les écrans verrouillés et protégés restent gérés par Android.",14f)
        column.addView(Button(this).apply { text="Retour aux compagnons"; setOnClickListener { finish() } })
        setContentView(ScrollView(this).apply {
            addView(column)
            setOnApplyWindowInsetsListener { view,insets ->
                val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.displayCutout())
                view.setPadding(bars.left,bars.top,bars.right,bars.bottom); insets
            }
        })
    }
}
