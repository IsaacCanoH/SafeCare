package mx.utng.ich.safecare.designsystem

/**
 * Interfaz de comunicaciÃ³n JNI (Java Native Interface) para interactuar con bibliotecas compiladas en C/C++.
 *  * Permite la ejecuciÃ³n de algoritmos de procesamiento de seÃ±ales o tareas intensivas con un rendimiento nativo superior.
 */
class NativeLib {

    /**
     * A native method that is implemented by the 'designsystem' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'designsystem' library on application startup.
        init {
            System.loadLibrary("designsystem")
        }
    }
}