package com.programadornovato.medinako

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

class ConfiguracionServidor : AppCompatActivity() {
    var txtIpServidor: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion_servidor)
        //LLAMAOS LOS DATOS DE SHARED PREFERENCE
        val mGetSharedPreferences = getSharedPreferences("medinako", Context.MODE_PRIVATE)
        //OBTENEMOS LA IP DEL SEL SERVIDOR A DONDE SE ENVIARA EL AUDIO
        val shareIpServidor = mGetSharedPreferences.getString("ipServidor", null)
        txtIpServidor = findViewById(R.id.txtIpServidor)
        //COLOCAMOS LA IP DEL SERVIDOR EN EL EDITTEXT
        txtIpServidor?.text=shareIpServidor
    }
    fun clickGuardarIpServidor(view: View){
        var mSharedPreferences=getSharedPreferences("medinako", Context.MODE_PRIVATE).edit()
        mSharedPreferences.putString("ipServidor",txtIpServidor?.text.toString())
        mSharedPreferences.commit()
        Toast.makeText(this, "Ip guardada exitosamente ", Toast.LENGTH_LONG).show()
    }
    fun clickIrEntrenar(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, Entrenar ::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun clickIrConfig(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, CrearMedidor::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun irPrincipal(view: View){
        val intent = Intent(this, Principal::class.java)
        startActivity(intent)
        finish()
    }

}