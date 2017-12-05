package com.psychapps.assistivetouch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BroadcastHandler : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sp = context.getSharedPreferences(context.getString(R.string.pref_file), Context.MODE_PRIVATE)
        when(intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("boot_receiver", "BOOT_COMPLETED")
                val state = sp.getBoolean(context.getString(R.string.service_state), false)
                if(state) {
                    val touchIntent = Intent(context, AssistiveTouchService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(touchIntent)
                    } else {
                        context.startService(touchIntent)
                    }
                    Log.d("boot_receiver", "SERVICE_STARTED")
                }
            }
        }
    }
}
