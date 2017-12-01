package com.psychapps.assistivetouch


import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName


class MainActivity : AppCompatActivity() {

    private val OVERLAY_PERMISSION_REQUEST_CODE = 42
    private val ADMIN_REQUEST_CODE = 1
    private var mHaveOverlayPermission = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainText.setOnClickListener {_ ->    //_ denotes view which is not used
            mainSwitch.toggle()
        }
        checkDrawOverlayPermission()
        checkAdminPermission()

        mainSwitch.setOnCheckedChangeListener{_, isChecked ->
            val touchIntent = Intent(this, AssistiveTouchService::class.java)
            if(isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(touchIntent)
                } else {
                    startService(touchIntent)
                }

            } else {
                stopService(touchIntent)
            }
        }


    }

    override fun onResume() {
        super.onResume()
        if(isMyServiceRunning(AssistiveTouchService::class.java) && !mainSwitch.isChecked) {
            mainSwitch.toggle()
        }
    }

    private fun checkAdminPermission() {
        val compName:ComponentName = ComponentName(this,MyAdmin::class.java)
        val intent = Intent(DevicePolicyManager
                .ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                compName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.sample_device_admin_description))
        startActivityForResult(intent, ADMIN_REQUEST_CODE)
    }

    private fun isMyServiceRunning(serviceClass: Class<AssistiveTouchService>): Boolean {
        val manager:ActivityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.getRunningServices(Integer.MAX_VALUE)
                .filter { serviceClass.name == it.service.className }
                .forEach { return true }
        return false
    }

    fun checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val dialogBuilder = AlertDialog.Builder(this);
            dialogBuilder.apply {
                setMessage(R.string.overlay_request)
                setCancelable(false)
                setNegativeButton(R.string.action_refuse) { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                setPositiveButton(R.string.action_proceed) { dialog, _ ->
                    dialog.dismiss()
                    // open system settings
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
                }
            }
            // show dialog
            dialogBuilder.create().show()
        } else {
            // We good from the start
            mHaveOverlayPermission = true
            //showToast(getString(R.string.permission_granted))
        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // results of permission checks
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mHaveOverlayPermission = Settings.canDrawOverlays(this)
                    showToast(if (mHaveOverlayPermission) getString(R.string.permission_granted) else getString(R.string.permission_refused))
                }
            }
            ADMIN_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    showToast(getString(R.string.permission_granted))
                } else {
                    showToast(getString(R.string.permission_refused))
                }
            }
            else -> {
            }
        }
    }

    private fun showToast(value: String) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show()
    }
}
