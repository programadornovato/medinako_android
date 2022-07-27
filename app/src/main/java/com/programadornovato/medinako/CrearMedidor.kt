package com.programadornovato.medinako

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import java.lang.Thread.sleep


class CrearMedidor : AppCompatActivity() {
    var tlListaRedes: TableLayout? = null
    var imgRedesWifi: VideoView? = null
    var etPasswordWifi: TextView? = null
    var btnGuadarWifi: Button? = null
    var precargador: LinearLayout? = null
    var btnCargarRedes: Button? = null
    var btnBorrarMedidor: Button? = null
    var rlContenedorPass: RelativeLayout? = null
    var btnBuscarRedMedidor: Button? = null
    var btnIrTomarMedidas: Button? = null
    var tvIndicacion: TextView? = null
    var ssidSeleccionado=""
    var cantcargarRedes=0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crear_medidor)
        tlListaRedes = findViewById(R.id.tlListaRedes)
        imgRedesWifi = findViewById(R.id.imgRedesWifi)
        imgRedesWifi?.setOnPreparedListener { it.isLooping = true }
        etPasswordWifi = findViewById(R.id.etPasswordWifi)
        btnGuadarWifi = findViewById(R.id.btnGuadarWifi)
        precargador = findViewById(R.id.precargador)
        btnCargarRedes = findViewById(R.id.btnCargarRedes)
        btnBorrarMedidor = findViewById(R.id.btnBorrarMedidor)
        rlContenedorPass = findViewById(R.id.rlContenedorPass)
        btnBuscarRedMedidor = findViewById(R.id.btnBuscarRedMedidor)
        btnIrTomarMedidas = findViewById(R.id.btnIrTomarMedidas)
        tvIndicacion = findViewById(R.id.tvIndicacion)
        cargarRedes()
    }
    fun clickCargarRedes(view: View){
        cargarRedes()
    }
    fun cargarRedes() {
        if(cantcargarRedes==0){
            precargador?.visibility = View.VISIBLE
        }
        tlListaRedes?.removeAllViews()
        val mGetSharedPreferences = getSharedPreferences("medinako", Context.MODE_PRIVATE)
        val ssidShared = mGetSharedPreferences.getString("ssid", null)
        val PasswordWifiShared = mGetSharedPreferences.getString("PasswordWifi", null)
        val ip = mGetSharedPreferences.getString("ip", null)
        etPasswordWifi?.setText(PasswordWifiShared)
        var queue = Volley.newRequestQueue(this)
        var url="http://192.168.4.1/listawifi"
        var jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { responseListaWifi ->
                imgRedesWifi?.visibility = View.INVISIBLE
                btnCargarRedes?.visibility = View.INVISIBLE
                btnBuscarRedMedidor?.visibility = View.INVISIBLE
                rlContenedorPass?.visibility = View.VISIBLE
                btnGuadarWifi?.visibility = View.VISIBLE
                tvIndicacion?.visibility = View.VISIBLE
                btnBorrarMedidor?.visibility = View.VISIBLE
                if(ip!=null){
                    btnIrTomarMedidas?.visibility = View.VISIBLE
                    textoAdvertencia("verde","Medidor conectado correctamente")
                    preguntaIrPrincipal()
                }
                btnBorrarMedidor?.visibility = View.VISIBLE
                try {
                    var jsonArrayListaRedes = responseListaWifi.getJSONArray("data")
                    val cantRedes=jsonArrayListaRedes.length()
                    //Hacemos un ciclo para recorrer todos los elementos de la red y mostrarlos en el TableLayout
                    for (i in 0 until cantRedes-1) {
                        var jsonObject = jsonArrayListaRedes.getJSONObject(i)
                        val registro = LayoutInflater.from(this).inflate(R.layout.table_row_redes, null, false)
                        val colSSID = registro.findViewById<View>(R.id.colSSID) as TextView
                        val ssid = jsonObject.getString("ssid")
                        colSSID.text = ssid
                        tlListaRedes?.addView(registro)
                        if(ssid == ssidShared) {
                            registro.setBackgroundColor(registro.resources.getColor(R.color.teal_700))
                            ssidSeleccionado = ssid
                        }
                    }
                    btnCargarRedes?.visibility = View.INVISIBLE
                    btnBuscarRedMedidor?.visibility = View.INVISIBLE
                    precargador?.visibility = View.INVISIBLE
                    textoAdvertencia("","")
                } catch (e: JSONException) {
                    e.printStackTrace()
                    precargador?.visibility = View.INVISIBLE
                }
            }, { errorListaWifis ->
                imgRedesWifi?.visibility = View.VISIBLE
                val path = "android.resource://" + packageName + "/" + R.raw.video
                imgRedesWifi?.setVideoURI(Uri.parse(path))
                imgRedesWifi?.start()
                btnCargarRedes?.visibility = View.INVISIBLE
                btnBuscarRedMedidor?.visibility = View.VISIBLE
                precargador?.visibility = View.INVISIBLE
                textoAdvertencia("amarillo","Verifique que esté conectado a la red WiFi Medidor")
            }
        )
        queue.add(jsonObjectRequest)
    }
    fun clickBorrarMedidor(view: View){
        precargador?.visibility = View.VISIBLE
        var queue = Volley.newRequestQueue(this)
        var mSharedPreferences=getSharedPreferences("medinako", Context.MODE_PRIVATE).edit()
        mSharedPreferences.remove("ip").apply()
        mSharedPreferences.remove("ssid").apply()
        mSharedPreferences.remove("PasswordWifi").apply()
        mSharedPreferences.remove("mac").apply()
        var url="http://192.168.4.1/EEPROMborrar"
        var stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                if(response.equals("1")){
                    Toast.makeText(this, "Medidor borrado", Toast.LENGTH_SHORT).show()
                    var queueReset = Volley.newRequestQueue(this)
                    var urlReset = "http://192.168.4.1/ESPrestart"
                    var stringRequestReset = StringRequest(Request.Method.GET, urlReset,
                        { responseReset ->
                            precargador?.visibility = View.INVISIBLE
                            etPasswordWifi?.text=""
                            //Colocamos todos los registros a color blanco
                            resetearColorLista()
                            notificar()
                            btnIrTomarMedidas?.visibility = View.VISIBLE
                        }, { error ->
                            println(error.toString())
                            precargador?.visibility = View.INVISIBLE
                        }
                    )
                    queueReset.add(stringRequestReset)
                }else{
                    Toast.makeText(this, "Error al borrar el medidor", Toast.LENGTH_SHORT).show()
                    precargador?.visibility = View.INVISIBLE
                }
            }, { error ->
                println(error.toString())
                precargador?.visibility = View.INVISIBLE
            }
        )
        queue.add(stringRequest)
    }
    fun irPrincipal(){
        //Abrir la actividad principal
        var intent = Intent(this, Principal::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun clickIrEntrenar(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, Entrenar::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun clickReresar(view: View){
        irPrincipal()
    }
    fun clickIrPrincipal(view: View){
        irPrincipal()
    }
    fun clickDetalles(view: View){
        precargador?.visibility = View.VISIBLE
        var queueDetalle = Volley.newRequestQueue(this)
        val mGetSharedPreferences = getSharedPreferences("medinako", Context.MODE_PRIVATE)
        val ip = mGetSharedPreferences.getString("ip", null)
        val mac = mGetSharedPreferences.getString("mac", null)
        var urlDetalles = "http://$ip/detalles"
        var stringRequestDetalles = StringRequest(Request.Method.GET, urlDetalles,
            { responseDetalles ->
                val builder = AlertDialog.Builder(this)
                builder.apply {
                    setTitle("Detalles de su dispositivo")
                    setMessage(responseDetalles.replace("\\n","\n"))
                    setPositiveButton("Ok",
                        DialogInterface.OnClickListener { dialog, id ->
                            precargador?.visibility = View.INVISIBLE
                            textoAdvertencia("", "")
                        }
                    )
                    setIcon(R.drawable.icon_config_azul)
                }
                builder.create().show()
            }, { error ->
                Toast.makeText(this, "No se encontro el dispositivo "+error, Toast.LENGTH_LONG).show()
                textoAdvertencia("rojo", "No se encontro el dispositivo "+error)
                precargador?.visibility = View.INVISIBLE
            })
        queueDetalle.add(stringRequestDetalles)
    }
    fun clickGuadarWifi(view: View){
        val mGetSharedPreferences = getSharedPreferences("medinako", Context.MODE_PRIVATE)
        val ip = mGetSharedPreferences.getString("ip", null)
        //Llamamos a la funcion que guardara la ip que no entrega el dispositivo esp8266
        var mSharedPreferences=getSharedPreferences("medinako", Context.MODE_PRIVATE).edit()
        mSharedPreferences.putString("PasswordWifi",etPasswordWifi?.text.toString())
        mSharedPreferences.putString("ssid",ssidSeleccionado)
        mSharedPreferences.commit()
        if(ssidSeleccionado.isEmpty()){
            Toast.makeText(this, "Seleccione una red WiFi", Toast.LENGTH_SHORT).show()
            return
        }
        if(etPasswordWifi?.text.toString().isEmpty()){
            Toast.makeText(this, "Ingrese una contraseña", Toast.LENGTH_SHORT).show()
            return
        }
        textoAdvertencia("","")
        precargador?.visibility = View.VISIBLE
        //Enviamos ssid y contraseña de la red seleccionada
        var queue = Volley.newRequestQueue(this)
        var url = "http://192.168.4.1/setting?pass="+etPasswordWifi?.text.toString()+"&ssid="+ssidSeleccionado
        var jsonObjectRequest = StringRequest(Request.Method.GET, url,
            { responseSetting ->
            if(responseSetting.equals("(IP unset)")) {
                textoAdvertencia("rojo","Verifique que la contraseña sea correcta e intente de nuevo")
                Toast.makeText(this, "Intente de nuevo", Toast.LENGTH_SHORT).show()
                var queueReset = Volley.newRequestQueue(this)
                var urlReset = "http://192.168.4.1/ESPrestart"
                var stringRequestReset = StringRequest(Request.Method.GET, urlReset,
                    { responseReset ->
                        precargador?.visibility = View.INVISIBLE
                        btnIrTomarMedidas?.visibility = View.VISIBLE
                        return@StringRequest
                    }, { error ->
                        println(error.toString())
                        sleep(3000)
                        btnIrTomarMedidas?.visibility = View.VISIBLE
                        precargador?.visibility = View.INVISIBLE
                    }
                )
                queueReset.add(stringRequestReset)
                precargador?.visibility = View.INVISIBLE
            }
            //Guardamos la ip a la que nos vamos a conectar
            mSharedPreferences.putString("ip",responseSetting.toString())
            mSharedPreferences.commit()
            precargador?.visibility = View.INVISIBLE
            btnBorrarMedidor?.visibility = View.VISIBLE
            var queueMAC = Volley.newRequestQueue(this)
            var urlMAC="http://192.168.4.1/mac"
            var stringRequestMAC = StringRequest(Request.Method.GET, urlMAC,
                { responseMAC ->
                    var mSharedPreferences=getSharedPreferences("medinako", Context.MODE_PRIVATE).edit()
                    mSharedPreferences.putString("mac",responseMAC.toString())
                    mSharedPreferences.commit()
                    var queueReset = Volley.newRequestQueue(this)
                    var urlReset="http://192.168.4.1/ESPrestart"
                    var stringRequestReset = StringRequest(Request.Method.GET, urlReset,
                        { responseReset ->
                            precargador?.visibility = View.INVISIBLE
                            btnIrTomarMedidas?.visibility = View.VISIBLE
                            textoAdvertencia("verde","Medidor conectado correctamente")
                            preguntaIrPrincipal()
                        }, { errorReset ->
                            println(errorReset.toString())
                            sleep(3000)
                            btnIrTomarMedidas?.visibility = View.VISIBLE
                            precargador?.visibility = View.INVISIBLE
                            textoAdvertencia("verde","Medidor conectado correctamente")
                            preguntaIrPrincipal()
                        }
                    )
                    queueReset.add(stringRequestReset)
                }, { errorNoHayMac ->
                    precargador?.visibility = View.INVISIBLE
                    Toast.makeText(this, "No se pudo obtener la MAC", Toast.LENGTH_SHORT).show()
                    textoAdvertencia("rojo","No se pudo obtener la MAC, intente de nuevo")
                }
            )
            queueMAC.add(stringRequestMAC)
           }, { errorSetting ->
               precargador?.visibility = View.INVISIBLE
               textoAdvertencia("rojo","No se pudo conectar a la red, intente de nuevo")
               //Si ya esta guardada la ip mandamos hacia la activity principal
               if(ip!=null) {
                   irPrincipal()
               }
               //Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
               println(errorSetting.toString())
           }
        )
        queue.add(jsonObjectRequest)
    }
    //Colocamos todos los registros a color blanco
    fun resetearColorLista(){
        //Colocamos todos los registros a color blanco
        for (i in 0 until tlListaRedes!!.childCount) {
            val child = tlListaRedes!!.getChildAt(i)
            if (child is TableRow) {
                val row = child
                for (x in 0 until row.childCount) {
                    val view = row.getChildAt(x)
                    view.setBackgroundColor(resources.getColor(R.color.white))
                }
            }
        }
    }
    //Funcion que se ejecuta cuando se selecciona una red de la lista (en table_row_redes.xml)
    fun clickListaRedes(view: View) {
        //Colocamos todos los registros a color blanco
        resetearColorLista()
        //obtener texto de la caja de texto
        var ssid = view.findViewById<View>(R.id.colSSID) as TextView
        var ssidText=ssid.text
        ssid.setBackgroundColor(resources.getColor(R.color.teal_700))
        //Colocamos en la variable global el ssid seleccionado para despues gurdarlo en el esp8266
        ssidSeleccionado=ssidText.toString()
    }
    fun ShowHidePass(view: View){
        if(etPasswordWifi?.getTransformationMethod()!!.equals(PasswordTransformationMethod.getInstance())){
            etPasswordWifi?.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }
        else{
            etPasswordWifi?.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
    }
    fun clickBuscarRedMedidor(view: View){
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        sleep(1500)
        btnCargarRedes?.visibility = View.VISIBLE
    }
    fun preguntaIrPrincipal(){
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Se a cofigurado exitosamente el medidor")
            setMessage("¿Deseas ir a tomar medidas?")
            setPositiveButton("Si",
                DialogInterface.OnClickListener { dialog, id ->
                    irPrincipal()
                }
            )
            setNegativeButton("No",
                DialogInterface.OnClickListener { dialog, id ->
                    // User cancelled the dialog
                }
            )
            setIcon(R.drawable.icon_inicio_azul)
        }
        builder.create().show()
    }
    fun clickSalir(view: MenuItem) {
        finish()
    }
    fun textoAdvertencia(color:String="rojo",texto:String=""){
        tvIndicacion?.text=texto
        if(color=="rojo"){
            tvIndicacion?.setBackgroundColor(Color.RED)
            tvIndicacion?.setTextColor(Color.WHITE)
        }else if(color=="verde"){
            tvIndicacion?.setBackgroundColor(Color.GREEN)
            tvIndicacion?.setTextColor(Color.BLACK)
        }else if(color=="amarillo"){
            tvIndicacion?.setBackgroundColor(Color.YELLOW)
            tvIndicacion?.setTextColor(Color.BLACK)
        } else if(color==""){
            tvIndicacion?.setBackgroundColor(Color.WHITE)
            tvIndicacion?.setTextColor(Color.BLACK)
        }
    }
    fun notificar(){
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Medidor eliminado exitosamente")
            setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, id ->
                })
        }
        builder.create().show()
    }

}


