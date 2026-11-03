


package com.huertadelvalle.app

object Configuracion {
    // Dirección del servidor.
    private const val IP = "192.168.1.222"
    private const val BASE_URL = "http://$IP/api"

    // URLs de la API
    const val URL_LOGIN          = "$BASE_URL/login.php"
    const val URL_REGISTRO       = "$BASE_URL/registro.php"
    const val URL_PROYECTOS      = "$BASE_URL/proyectos.php"
    const val URL_NUEVO_PROYECTO = "$BASE_URL/nuevo_proyecto.php"
    const val URL_CONFIGURACION_CULTIVO = "$BASE_URL/configuracion_cultivo.php"
    const val URL_ELIMINAR_PROYECTO = "$BASE_URL/eliminar_proyecto.php"
    const val URL_CANCELAR_PROYECTO = "$BASE_URL/cancelar_proyecto.php"
    const val URL_FINALIZAR_PROYECTO = "$BASE_URL/finalizar_proyecto.php"
    const val URL_ULTIMA_MEDICION = "$BASE_URL/ultima_medicion.php"
    const val URL_LISTAR_USUARIOS   = "$BASE_URL/listar_usuarios.php"
    const val URL_MODIFICAR_USUARIO = "$BASE_URL/modificar_usuario.php"
    const val URL_ELIMINAR_USUARIO  = "$BASE_URL/eliminar_usuario.php"
    const val URL_OBTENER_CONFIGURACION = "$BASE_URL/obtener_configuracion.php"
    const val URL_ESTADO_VALVULAS   = "$BASE_URL/estado_valvulas.php"
    const val URL_CONTROLAR_VALVULA = "$BASE_URL/controlar_valvula.php"
    const val URL_ESTADO_BOMBAS   = "$BASE_URL/estado_bombas.php"
    const val URL_CONTROLAR_BOMBA = "$BASE_URL/controlar_bomba.php"
    const val URL_OBTENER_CONFIG_TANQUE = "$BASE_URL/obtener_configuracion_tanque.php"
    const val URL_ACTUALIZAR_CONFIG_TANQUE = "$BASE_URL/actualizar_configuracion_tanque.php"
    const val URL_OBTENER_EVALUACION = "$BASE_URL/obtener_evaluacion_diaria.php"
    const val URL_GUARDAR_EVALUACION = "$BASE_URL/guardar_evaluacion_diaria.php"
    const val URL_HISTORIAL_EVALUACIONES = "$BASE_URL/historial_evaluaciones.php"
    const val URL_INFORME_PARCIAL = "$BASE_URL/informe_parcial.php"
    const val URL_INFORME_FINAL = "$BASE_URL/informe_final.php"

    fun urlArchivo(ruta: String): String {
        // Completa la ruta de la foto.
        if (ruta.startsWith("http://") || ruta.startsWith("https://")) return ruta
        return "$BASE_URL/${ruta.trimStart('/')}"
    }

}