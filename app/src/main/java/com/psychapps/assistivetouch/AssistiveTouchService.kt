package com.psychapps.assistivetouch

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat


class AssistiveTouchService : Service() {

    private val CLICK_COOLDOWN: Long = 100

    private var mLastClickTime: Long = 0

    private var mRootView: View? = null

    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var compName: ComponentName

    private lateinit var mPackageManager: PackageManager
    private val ONGOING_NOTIFICATION_ID = 10

    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val CHANNEL_DEFAULT_IMPORTANCE = "low"
        val notification = NotificationCompat.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.drawable.ic_touch)
                .setContentIntent(pendingIntent)
                .setTicker(getText(R.string.ticker_text))
                .setChannelId(getString(R.string.cid))
                .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        mRootView = LayoutInflater.from(this).inflate(R.layout.assistivetouch_view, null)

        deviceManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this,MyAdmin::class.java)


        mRootView?.let {
            createOverlay(it) {
                if (System.currentTimeMillis() - mLastClickTime > CLICK_COOLDOWN) {
                    mLastClickTime = System.currentTimeMillis()
                    val touchOptions: LinearLayout = it.findViewById(R.id.touch_options)
                    if(touchOptions.visibility == View.GONE) {
                        touchOptions.visibility = View.VISIBLE
                    } else {
                        touchOptions.visibility = View.GONE
                    }

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mRootView != null) {
            windowManager.removeView(mRootView)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }


    private fun createOverlay(root: View, clickListener: (View) -> Unit?) {
        val windowLayoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
        } else {
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT)
        }
        windowLayoutParams.apply {
            gravity = Gravity.CENTER or Gravity.LEFT
            x = 0
            y = 10
        }

        val windowManager = windowManager
        windowManager.addView(root, windowLayoutParams)

        val lockButton: ImageButton = root.findViewById(R.id.lockButton)
        lockButton.setOnClickListener{_ ->
            val touchOptions: LinearLayout = root.findViewById(R.id.touch_options)
            if(deviceManager.isAdminActive(compName)) deviceManager.lockNow()
            else Toast.makeText(baseContext, getString(R.string.admin_required), Toast.LENGTH_SHORT).show()
            touchOptions.visibility = View.GONE
        }


        val container:ImageView = root.findViewById(R.id.overlay_btn_image)
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0.toFloat()
            private var initialTouchY: Float = 0.toFloat()
            private var isClick = false

            // catch a click on overlay button - not something trivial
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = windowLayoutParams.x
                        initialY = windowLayoutParams.y

                        //get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                    }
                    MotionEvent.ACTION_UP -> {
                        val dX = Math.abs((event.rawX - initialTouchX).toInt())
                        val dY = Math.abs((event.rawY - initialTouchY).toInt())

                        //The check for dX <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        isClick = dX < 1 && dY < 1
                    }
                    MotionEvent.ACTION_MOVE -> {
                        //Calculate the X and Y coordinates of the view.
                        windowLayoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        windowLayoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        //Update the layout with new X & Y coordinate
                        windowManager.updateViewLayout(root, windowLayoutParams)
                    }
                    else -> {
                    }
                }


                if (isClick) {
                    isClick = false
                    clickListener(root)
                }
                return true
            }
        })
    }


    private val windowManager: WindowManager
        get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

}
