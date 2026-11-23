package com.andymatt1.radionebulosa.updater

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.andymatt1.radionebulosa.MainActivity

/** Vuelve a abrir Radio Nebulosa justo después de instalar una actualización. */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        // Pequeña espera para que el PackageManager termine de registrar la versión nueva.
        Handler(Looper.getMainLooper()).postDelayed({
            context.startActivity(launchIntent)
        }, 1_200L)
    }
}
