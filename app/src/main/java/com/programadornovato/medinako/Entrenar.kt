package com.programadornovato.medinako

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import java.lang.Thread.sleep

class Entrenar : AppCompatActivity() {
    var lvEstados: ListView? = null
    var tlListaEstados: TableLayout? = null
    var trVacio: TableRow? = null
    var trMedio: TableRow? = null
    var trLleno: TableRow? = null
    var precargador: LinearLayout? = null
    var tvResultado: TextView? = null
    var estado=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entrenar)
        tlListaEstados = findViewById(R.id.tlListaEstados)
        trVacio = findViewById(R.id.trVacio)
        trMedio = findViewById(R.id.trMedio)
        trLleno = findViewById(R.id.trLleno)
        precargador = findViewById(R.id.precargador)
        tvResultado = findViewById(R.id.tvResultado)
        val ip = getSharedPreferences("medinako", Context.MODE_PRIVATE).getString("ip", null)
        if(ip == null){
            preguntaAbrirConfiguracion()
        }
    }
    fun clickLeer(view: View) {
        precargador?.visibility = View.VISIBLE
        textoAdvertencia("amarillo","Tomando medida, espere 20 segundos para poder entrenar")
        var queueEnciende = Volley.newRequestQueue(this)
        val mGetSharedPreferences = getSharedPreferences("medinako", Context.MODE_PRIVATE)
        val ip = mGetSharedPreferences.getString("ip", null)
        val mac = mGetSharedPreferences.getString("mac", null)
        if(estado.isEmpty()){
            textoAdvertencia("rojo","Selecciona un estado")
            Toast.makeText(this, "Selecciona un estado", Toast.LENGTH_SHORT).show()
            precargador?.visibility = View.INVISIBLE
            return
        }
        //Hacemos que el esp82 vibre y mande los datos al servidor
        var url = "http://$ip/entrenar?estado=$estado"
        var stringRequestEncender = StringRequest(
            Request.Method.GET, url,
            { responseEncender ->
                textoAdvertencia("amarillo","Tomando medida, espere 20 segundos para poder entrenar")
                precargador?.visibility = View.INVISIBLE
                if(mac!=null){
                    precargador?.visibility = View.VISIBLE
                    sleep(25000)
                    precargador?.visibility = View.INVISIBLE
                    textoAdvertencia("verde","Se ha leido el estado $estado, lea otros esados para entrenar o proceda a ENTRENAR")
                }else{
                    textoAdvertencia("rojo","No se encontro MAC del dispositivo porfavor reinicie la aplicacion")
                }
            }, { error ->
                textoAdvertencia("rojo","No se ha podido conectar al medidor")
                precargador?.visibility = View.INVISIBLE
                preguntaAbrirConfiguracion()
            })
        queueEnciende.add(stringRequestEncender)
    }
    fun clickEntrenar(view: View) {
        val configuracion = Configuracion()
        precargador?.visibility = View.VISIBLE
        var queueValidaArchivos = Volley.newRequestQueue(this)
        val mGetSharedPreferences = getSharedPreferences("medinako", Context.MODE_PRIVATE)
        val mac = mGetSharedPreferences.getString("mac", null)
        if(mac==null){
            precargador?.visibility = View.INVISIBLE
            preguntaAbrirConfiguracion()
        }
        var urlValidaArchivos = "http://"+configuracion.ipServidor+":5000/listo-entrenar?mac=$mac"
        var JsonObjectValidaArchivos = JsonObjectRequest(
            Request.Method.GET, urlValidaArchivos,null,
            { responseValidaArchivos ->
                try {
                    var jsonArrayEstados = responseValidaArchivos.getJSONArray("data")
                    var jsonObject = jsonArrayEstados.getJSONObject(0)
                    val Vacio = jsonObject.getInt("Vacio")
                    val Medio = jsonObject.getInt("Medio")
                    val Lleno = jsonObject.getInt("Lleno")
                    var mensaje = ""
                    if(Vacio<10){
                        mensaje = mensaje + "Vacio, "
                    }
                    if(Medio<10){
                        mensaje = mensaje +"Medio, "
                    }
                    if(Lleno<10){
                        mensaje = mensaje +"Lleno, "
                    }
                    if(mensaje.isEmpty()){

                        var queueEntrenar = Volley.newRequestQueue(this)
                        val retryPolicy: RetryPolicy = DefaultRetryPolicy(10000,3,3F)
                        //Nos conectamos al servidor python para que entrene el modelo
                        var urlEntrenar = "http://"+configuracion.ipServidor+":5000/entrenar?mac=$mac"
                        var stringEntrenar = StringRequest(
                            Request.Method.GET, urlEntrenar,
                            { responseEntrenar ->
                                if(responseEntrenar=="1" || responseEntrenar=="1.0" ){
                                    textoAdvertencia("verde","Entrenamiento finalizado EXITOSAMENTE")
                                    precargador?.visibility = View.INVISIBLE
                                }else{
                                    textoAdvertencia("rojo","No se ha podido entrenar el modelo (intentelo de nuevo)")
                                    precargador?.visibility = View.INVISIBLE
                                }
                            }, { error ->
                                textoAdvertencia("rojo","No hay conexión a internet")
                                precargador?.visibility = View.INVISIBLE
                            })
                        stringEntrenar.retryPolicy = retryPolicy
                        queueEntrenar.add(stringEntrenar)


                    }else{
                        //Remover la ultima coma
                        mensaje = mensaje.substring(0, mensaje.length-2)
                        textoAdvertencia("rojo","Falta entrenar $mensaje")
                        precargador?.visibility = View.INVISIBLE
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    precargador?.visibility = View.INVISIBLE
                }


            }, { error ->
                textoAdvertencia("rojo","No se pudo entrenar (Falta subir archivos) "+error.toString())
                precargador?.visibility = View.INVISIBLE
            })
        queueValidaArchivos.add(JsonObjectValidaArchivos)
    }
    fun clickRegistro(view: View){
        trVacio?.setBackgroundColor(resources.getColor(R.color.white))
        trMedio?.setBackgroundColor(resources.getColor(R.color.white))
        trLleno?.setBackgroundColor(resources.getColor(R.color.white))
        view.setBackgroundColor(resources.getColor(R.color.teal_700))
        if (view.id == R.id.trVacio) {
            estado = "Vacio"
        } else if (view.id == R.id.trMedio) {
            estado = "Medio"
        } else if (view.id == R.id.trLleno) {
            estado = "Lleno"
        }
    }
    //Abrir la actividad de entrenar
    fun abrirConfigurarMedidor(menu: MenuItem){
        val intent = Intent(this, CrearMedidor::class.java)
        startActivity(intent)
        finish()
    }
    fun abrirPrincipal(menu:MenuItem){
        val intent = Intent(this, Principal::class.java)
        startActivity(intent)
        finish()
    }
    fun Salir(menu:MenuItem){
        finish()
    }
    fun preguntaAbrirConfiguracion(){
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("No se encontró el medidor")
            setMessage("Revise que el dispositivo este funcionando.\n¿Desea abrir la cofiguración?")
            setPositiveButton("Si",
                DialogInterface.OnClickListener { dialog, id ->
                    abrirCrearMedidor("menu")
                })
            setNegativeButton("No",
                DialogInterface.OnClickListener { dialog, id ->
                })
        }
        builder.create().show()
    }
    fun abrirCrearMedidor(desde:String=""){
        val intent = Intent(this, CrearMedidor::class.java)
        if(desde=="menu") {
            intent.putExtra("desde", "menu");
        }
        startActivity(intent)
        finish()
    }
    fun clickIrConfig(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, CrearMedidor::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun clickIrEntrenar(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, Entrenar ::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
    }
    fun irPrincipal(view: View){
        //Abrir la actividad principal
        var intent = Intent(this, Principal::class.java)
        startActivity(intent)
        //Cerrar esta actividad
        finish()
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
    fun textoAdvertencia(color:String="rojo",texto:String=""){
        tvResultado?.text=texto
        if(color=="rojo"){
            tvResultado?.setBackgroundColor(Color.RED)
            tvResultado?.setTextColor(Color.WHITE)
        }else if(color=="verde"){
            tvResultado?.setBackgroundColor(Color.GREEN)
            tvResultado?.setTextColor(Color.BLACK)
        }else if(color=="amarillo"){
            tvResultado?.setBackgroundColor(Color.YELLOW)
            tvResultado?.setTextColor(Color.BLACK)
        } else if(color==""){
            tvResultado?.setBackgroundColor(Color.WHITE)
            tvResultado?.setTextColor(Color.BLACK)
        }
    }

}