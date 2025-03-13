package com.android.hms.utils

import android.app.AlertDialog
import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class MyProgressBar(private val context: Context, private val textToShow:String = "", show: Boolean = true) {
    private val progressBar = InnerProgressBar(context, textToShow)

    init {
        if (show) show()
    }

    fun show() {
        progressBar.show()
    }

    fun dismiss() {
        progressBar.dismiss()
    }

    inner class InnerProgressBar(private val context: Context, private val textToShow: String = "") {
        private val linearLayout = LinearLayout(context)
        private val progressBar = ProgressBar(context, null, android.R.attr.progressBarStyle)
        private val textView = TextView(context)
        private var alertDialog: AlertDialog? = null

        init {
            progressBar.isIndeterminate = true
        }

        fun show() {
            try {
                val llPadding = 30
                linearLayout.orientation = LinearLayout.HORIZONTAL
                linearLayout.setPadding(llPadding, llPadding, llPadding, llPadding)
                linearLayout.gravity = Gravity.CENTER

                var llParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                llParam.gravity = Gravity.CENTER
                linearLayout.layoutParams = llParam

                progressBar.setPadding(0, 0, llPadding, 0)
                progressBar.layoutParams = llParam

                linearLayout.addView(progressBar)

                if (textToShow.isNotBlank()) {
                    llParam = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    llParam.gravity = Gravity.CENTER
                    textView.text = textToShow
                    // textView.setTextColor(ThemeColor.colorPrimary(context)) // Color.DKGRAY
                    textView.textSize = 20f
                    textView.layoutParams = llParam
                    linearLayout.addView(textView)
                }

                val builder = AlertDialog.Builder(context)
                // builder.setCancelable(true)
                builder.setView(linearLayout)

                alertDialog = builder.create()
                alertDialog?.setCancelable(false) // Prevent closing without action
                alertDialog?.show()

                val window = alertDialog?.window
                if (window != null) {
                    val layoutParams = WindowManager.LayoutParams()
                    layoutParams.copyFrom(window.attributes)
                    layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                    layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                    window.attributes = layoutParams
                    window.setBackgroundDrawableResource(android.R.color.transparent)
                }
            } catch (exp: Exception) {
                CommonUtils.printException(exp)
            }
        }

        fun dismiss() {
            try {
                alertDialog?.dismiss()
            } catch (exp: Exception) {
                CommonUtils.printException(exp)
            }
        }
    }
}