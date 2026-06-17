package mx.utng.ich.safecare.designsystem

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