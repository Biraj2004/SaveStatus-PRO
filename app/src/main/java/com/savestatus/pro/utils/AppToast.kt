package com.savestatus.pro.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import com.savestatus.pro.R

/**
 * Modern animated toast replacing default Android Toast.
 * Capsule-shaped, dark translucent, with icon support and smooth animations.
 * Positioned above the floating bottom nav capsule.
 */
object AppToast {

    enum class Type { SUCCESS, ERROR, INFO, PLAIN }

    private var currentToast: Toast? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Show a custom toast with animation.
     * @param context  Activity or Application context
     * @param message  The message to display
     * @param type     SUCCESS / ERROR / INFO / PLAIN
     * @param duration Toast.LENGTH_SHORT or Toast.LENGTH_LONG
     */
    fun show(
        context: Context,
        message: String,
        type: Type = Type.PLAIN,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        mainHandler.post {
            // Cancel any existing toast immediately
            currentToast?.cancel()

            val inflater = LayoutInflater.from(context.applicationContext)
            @Suppress("InflateParams")
            val view = inflater.inflate(R.layout.layout_custom_toast, null)

            val tvIcon = view.findViewById<TextView>(R.id.tvToastIcon)
            val tvMsg  = view.findViewById<TextView>(R.id.tvToastMessage)

            tvMsg.text = message

            when (type) {
                Type.SUCCESS -> { tvIcon.visibility = View.VISIBLE; tvIcon.text = "+"; tvIcon.setTextColor(0xFF22C55E.toInt()) }
                Type.ERROR   -> { tvIcon.visibility = View.VISIBLE; tvIcon.text = "!"; tvIcon.setTextColor(0xFFEF4444.toInt()) }
                Type.INFO    -> { tvIcon.visibility = View.VISIBLE; tvIcon.text = "i"; tvIcon.setTextColor(0xFF3B82F6.toInt()) }
                Type.PLAIN   -> tvIcon.visibility = View.GONE
            }

            // Apply enter animation
            try {
                view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.toast_enter))
            } catch (_: Exception) {}

            @Suppress("DEPRECATION")
            val toast = Toast(context.applicationContext).also {
                it.duration = duration
                @Suppress("DEPRECATION")
                it.view = view
                val yOffset = (context.resources.displayMetrics.density * 130).toInt()
                it.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, yOffset)
            }
            toast.show()
            currentToast = toast

            // Apply exit animation before auto-dismiss
            val displayMs = if (duration == Toast.LENGTH_SHORT) 2000L else 3500L
            mainHandler.postDelayed({
                try {
                    view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.toast_exit))
                } catch (_: Exception) {}
            }, displayMs - 350)
        }
    }

    fun success(context: Context, message: String) = show(context, message, Type.SUCCESS)
    fun error(context: Context, message: String)   = show(context, message, Type.ERROR)
    fun info(context: Context, message: String)    = show(context, message, Type.INFO)
    fun plain(context: Context, message: String)   = show(context, message, Type.PLAIN)

    /** Cancel any currently showing toast */
    fun cancel() { mainHandler.post { currentToast?.cancel() } }
}
