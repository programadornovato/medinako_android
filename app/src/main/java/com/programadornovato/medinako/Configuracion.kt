package com.programadornovato.medinako

class Configuracion {
    var localOremoto: String = "remoto"
    var ipServidor=""
    init {
        if(localOremoto == "local")
        {
            ipServidor = "192.168.100.2"
        }
        else{
            ipServidor = "142.93.199.79"
        }
    }
}