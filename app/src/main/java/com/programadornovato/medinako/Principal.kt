package com.programadornovato.medinako

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
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
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class Principal : AppCompatActivity() {
    var tlListaResultados: TableLayout?=null
    var precargador: LinearLayout? = null
    var tvRes: TextView? = null
    lateinit var etDia: TextView
    lateinit var calDia: CalendarView
    var fecha = ""
    var yaPreguntoCambiarWifi=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal)
        tlListaResultados=findViewById(R.id.tlListaResultados)
        precargador = findViewById(R.id.precargador)
        tvRes = findViewById(R.id.tvRes)
        etDia = findViewById(R.id.etDia)
        calDia = findViewById(R.id.calDia)
        calDia.visibility = View.GONE
        val configuracion=Configuracion()
        //Si estamos conectados al wifi medidor preguntamos si queremos conectarnos a una red wifi especifica
        validaSiRedMedidorEstaConectado()
        //Si estamos conectados al wifi internet
        validaSiRedInternetEstaConectado()
        //Colocar en el textview (etDia) la fecha actual
        colocarLaFechaHoy()
        val ip = getSharedPreferences("medinako", Context.MODE_PRIVATE).getString("ip", null)
        //Si no hau IP preguntar si se quiere ir al crear un medidor
        if(ip == null){
            preguntaAbrirConfiguracionMedidor()
        }
        //Si la ip no es nula validamos que exista conexion a internet
        else{
            //Leemos la mac del medidor para guardar su medicion
            var queue = Volley.newRequestQueue(this)
            var url = "http://"+configuracion.ipServidor+"/medinako/listaMediciones.php"
            var stringObjectRequest = StringRequest(Request.Method.GET, url,
                { responseHayInternet ->
                    //Si hay conexion a internet cargamos las tablas
                    cargaTabla()
                },
                { errorHayInternet ->
                    //Si no hay conexion a internet preguntamos si podemos abrir wifi
                    if(yaPreguntoCambiarWifi==false) {
                        yaPreguntoCambiarWifi = true
                        preguntaAbrirConfiguracionWifi()
                    }
                })
            queue.add(stringObjectRequest)
        }
        //Click en el calendario colocamos la fecha en el textview
        calDia.setOnDateChangeListener(
            CalendarView.OnDateChangeListener { view, year, month, dayOfMonth ->
                val mes=String.format("%02d", month+1)
                val dia=String.format("%02d", dayOfMonth)
                fecha = "$year-$mes-$dia"
                etDia.text = "Resultados del dia "+fecha
                cargaTabla()
                calDia?.visibility = View.GONE
            }
        )
    }
    //Validamos si estamos conectados al wifi medidor y si es asi lo reseteamos y preguntamos si se quiere cambiar wifi
    fun validaSiRedMedidorEstaConectado(){
        var queue = Volley.newRequestQueue(this)
        var url = "http://192.168.4.1/ESPrestart"
        var stringObjectRequest = StringRequest(
            Request.Method.GET, url,
            { responseResetMedidor ->
                if(yaPreguntoCambiarWifi==false) {
                    yaPreguntoCambiarWifi=true
                    preguntaAbrirConfiguracionWifi()
                }
            },
            { errorResetMedidor ->
            })
        queue.add(stringObjectRequest)
    }
    fun validaSiRedInternetEstaConectado(){
        var configuracion=Configuracion()
        var queue = Volley.newRequestQueue(this)
        var url="http://"+configuracion.ipServidor+"/medinako/listaMediciones.php"
        var stringObjectRequest = StringRequest(
            Request.Method.GET, url,
            { response ->

            },
            { error ->
                if(yaPreguntoCambiarWifi==false) {
                    yaPreguntoCambiarWifi = true
                    preguntaAbrirConfiguracionWifi()
                }
            })
        queue.add(stringObjectRequest)
    }
    fun colocarLaFechaHoy(){
        val ahora = System.currentTimeMillis()
        val hoy = Date(ahora)
        val df: DateFormat = SimpleDateFormat("yyyy-MM-dd")
        fecha = df.format(hoy)
        etDia.text = "Resultados del dia "+fecha
    }
    fun clickRecargar(view: View){
        val mac = getSharedPreferences("medinako", Context.MODE_PRIVATE).getString("mac", null)
        if(mac!=null){
            cargaTabla()
        }else{
            Toast.makeText(this, "No fue posible ontener la MAC", Toast.LENGTH_LONG).show()
            textoAdvertencia("rojo", "No fue posible ontener la MAC")
        }
    }
    fun cargaTabla(){
        textoAdvertencia("", "")
        val configuracion=Configuracion()
        precargador?.visibility = View.VISIBLE
        tlListaResultados?.removeAllViews()
        val mac= getSharedPreferences("medinako", Context.MODE_PRIVATE).getString("mac", null)
        if(mac==null){
            precargador?.visibility = View.INVISIBLE
            preguntaAbrirConfiguracionMedidor()
            return
        }
        var queueListaMediciones=Volley.newRequestQueue(this)
        var url="http://"+configuracion.ipServidor+"/medinako/listaMediciones.php?idDispositivo=$mac&fecha=$fecha"
        //var url="http://"+configuracion.ipServidor+":5000/lista-mediciones?mac=$mac"
        var jsonObjectRequest=JsonObjectRequest(Request.Method.GET,url,null,
            { responseMediciones ->
                precargador?.visibility = View.INVISIBLE
                try {
                    var jsonArray=responseMediciones.getJSONArray("data")
                    for(i in 0 until jsonArray.length() ){
                        var jsonObject=jsonArray.getJSONObject(i)
                        val registro= LayoutInflater.from(this).inflate(R.layout.table_row_np,null,false)
                        val colEstado=registro.findViewById<View>(R.id.colEstado) as TextView
                        val colFechaCreacion=registro.findViewById<View>(R.id.colSSID) as TextView
                        colFechaCreacion.text=jsonObject.getString("fechaCreacion")
                        colEstado.text=jsonObject.getString("estado")
                        //Cambiar el color de la fila dependiendo del estado
                        if(jsonObject.getString("estado")=="Vacio"){
                            colEstado.setBackgroundColor(Color.argb(100,179,0,0))
                            colEstado.setTextColor(Color.WHITE)
                        }else if(jsonObject.getString("estado")=="Medio"){
                            colEstado.setBackgroundColor(Color.argb(100,255,255,0))
                            colEstado.setTextColor(Color.BLACK)
                        }else if(jsonObject.getString("estado")=="Lleno"){
                            colEstado.setBackgroundColor(Color.argb(100,0,118,0))
                            colEstado.setTextColor(Color.WHITE)
                        }
                        tlListaResultados?.addView(registro)
                    }
                    if(jsonArray.length()==0){
                        textoAdvertencia("amarillo", " No hay resultados para la fecha seleccionada " +
                                "\n  - Seleccione una nueva fecha " +
                                "\n  - O haga click en Recargar " +
                                "\n  - O en Tomar medida")
                    }
                }catch (e: JSONException){
                    e.printStackTrace()
                }
            }, { errorMediciones ->
                Toast.makeText(this,"No se encontro el servidor  "+errorMediciones.toString(),Toast.LENGTH_LONG).show()
                textoAdvertencia("rojo", "No se encontro el servidor  "+errorMediciones.toString())
                precargador?.visibility = View.INVISIBLE
                //Preguntamos si se quiere cambiar de red wifi
                if(yaPreguntoCambiarWifi==false) {
                    yaPreguntoCambiarWifi = true
                    preguntaAbrirConfiguracionWifi()
                }
            }
        )
        queueListaMediciones.add(jsonObjectRequest)
    }
    fun clickIrMedidor(view: MenuItem) {
        abrirCrearMedidor("menu")
    }
    fun abrirCrearMedidor(desde:String=""){
        val intent = Intent(this, CrearMedidor::class.java)
        if(desde=="menu") {
            intent.putExtra("desde", "menu");
        }
        startActivity(intent)
        finish()
    }
    fun abrirEntrenar(menu:MenuItem){
        val intent = Intent(this, Entrenar::class.java)
        startActivity(intent)
        finish()
    }

    fun clickTomarMedida(view: View) {
        precargador?.visibility = View.VISIBLE
        var queueEnciende = Volley.newRequestQueue(this)
        val mGetSharedPreferences = getSharedPreferences("medinako", Context.MODE_PRIVATE)
        val ip = mGetSharedPreferences.getString("ip", null)
        val mac = mGetSharedPreferences.getString("mac", null)
        var url = "http://$ip/encender"
        var stringRequestEncender = StringRequest(Request.Method.GET, url,
            { responseEncender ->
                Toast.makeText(this, "Tomando medida recargue en 20 segundos", Toast.LENGTH_LONG).show()
                textoAdvertencia("amarillo", "Tomando medida recargue en 20 segundos")
                sleep(18000)
                textoAdvertencia("", "")
                precargador?.visibility = View.INVISIBLE
                if(mac!=null){
                    cargaTabla()
                }else{
                    Toast.makeText(this, "No se encontro MAC del dispositivo porfavor reinicie la aplicacion", Toast.LENGTH_LONG).show()
                    textoAdvertencia("rojo", "No se encontro MAC del dispositivo porfavor reinicie la aplicacion")
                }
            }, { error ->
                Toast.makeText(this, "No se encontro el dispositivo "+error, Toast.LENGTH_LONG).show()
                textoAdvertencia("rojo", "No se encontro el dispositivo "+error)
                precargador?.visibility = View.INVISIBLE
                preguntaAbrirConfiguracionMedidor()
            })
        queueEnciende.add(stringRequestEncender)
    }
    fun preguntaAbrirConfiguracionMedidor(){
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("No se encontró el medidor")
            setMessage("Revise que el dispositivo este funcionando.\n¿Desea abrir la cofiguración?")
            setPositiveButton("Si",
                DialogInterface.OnClickListener { dialog, id ->
                    abrirCrearMedidor("menu")
                }
            )
            setNegativeButton("No",
                DialogInterface.OnClickListener { dialog, id ->
                }
            )
            setIcon(R.drawable.icon_config_azul)
        }
        builder.create().show()
    }
    fun preguntaAbrirConfiguracionWifi(){
        val drawableID: Int = this.getResources().getIdentifier("icon_wifi", "drawable", this.getPackageName())
        val ssid= getSharedPreferences("medinako", Context.MODE_PRIVATE).getString("ssid", null)
        var ssidTexto=""
        if(ssid!=null){
            ssidTexto="Conéctese a la red "+ssid
        }
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("No tiene acceso a internet")
            setMessage(ssidTexto+"\n¿Desea abrir la cofiguración wifi?")
            setPositiveButton("Si",
                DialogInterface.OnClickListener { dialog, id ->
                    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                }
            )
            setNegativeButton("No",
                DialogInterface.OnClickListener { dialog, id ->
                }
            )
            setIcon(R.drawable.icon_no_wifi_azul)
        }
        builder.create().show()

    }
    fun clickSalir(view: MenuItem) {
        finish()
    }
    fun clickIrConfig(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, CrearMedidor::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun clickDetalles(view: View){
        precargador?.visibility = View.VISIBLE
        var queueEnciende = Volley.newRequestQueue(this)
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
        queueEnciende.add(stringRequestDetalles)
    }
    fun clickIrEntrenar(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, Entrenar ::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun clickVerCalendario(view: View){
        calDia?.visibility = View.VISIBLE
    }

    fun textoAdvertencia(color:String="rojo",texto:String=""){
        tvRes?.text=texto
        if(color=="rojo"){
            tvRes?.setBackgroundColor(Color.RED)
            tvRes?.setTextColor(Color.WHITE)
        }else if(color=="verde"){
            tvRes?.setBackgroundColor(Color.GREEN)
            tvRes?.setTextColor(Color.BLACK)
        }else if(color=="amarillo"){
            tvRes?.setBackgroundColor(Color.YELLOW)
            tvRes?.setTextColor(Color.BLACK)
        } else if(color==""){
            tvRes?.setBackgroundColor(Color.WHITE)
            tvRes?.setTextColor(Color.BLACK)
        }
    }
}