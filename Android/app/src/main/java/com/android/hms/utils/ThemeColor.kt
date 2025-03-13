package com.android.hms.utils

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import com.android.hms.R

object ThemeColor {

    private fun getColor1(context: Context, colorAttr: Int): Int {
        val attrs = intArrayOf(colorAttr)
        val typedArray = context.obtainStyledAttributes(attrs)
        val result = typedArray.getString(0)
        typedArray.recycle()
        return if (result.isNullOrBlank()) 0 else Color.parseColor(result)
    }

    private fun get(context: Context, colorAttr: Int): Int {
        val value = TypedValue()
        context.theme.resolveAttribute(colorAttr, value, true)
        return value.data
    }

    private fun getColor2(context: Context, colorAttr: Int): Int {
        /*val colorAttr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) android.R.attr.colorAccent
            else context.resources.getIdentifier("colorAccent", "attr", context.packageName) // Get colorAccent defined for AppCompat */

        val value = TypedValue()
        val a = context.obtainStyledAttributes(value.data, intArrayOf(colorAttr))
        val color = a.getColor(0, 0)

        //    val color1 = getColor1(context, colorAttr)
        //    val color2 = get(context, colorAttr)

        a.recycle()

        return color
    }

    fun colorPrimary(context: Context): Int {
        return get(context, com.google.android.material.R.attr.colorPrimary) // androidx.appcompat.R.attr.colorPrimary
    }

    fun colorButtonBorder(context: Context): Int {
        return colorPrimary(context)
        // return get(context, R.attr.colorPrimary)
    }

    fun colorUserPhoto(context: Context): Int {
        return colorPrimary(context)
        // return get(context, R.attr.colorPrimary)
    }

    fun colorButton(context: Context): Int {
        return colorPrimary(context)
        // return get(context, R.attr.colorPrimary)
    }

    fun colorBackground(context: Context): Int {
        return get(context, android.R.color.background_dark)
    }

    fun colorButtonSend(context: Context): Int {
        return colorPrimary(context)
        // return get(context, R.attr.colorPrimary)
    }


    /*
    fun colorUnread(context: Context): Int {
        return get(context, R.attr.colorPrimaryDark)
    }

    fun colorDrawerPhoto(context: Context): Int {
        return get(context, R.attr.colorPrimaryDark)
    }

    fun colorPrimaryDark(context: Context): Int {
        return get(context, R.attr.colorPrimaryDark)
    }

    fun colorAccent(context: Context): Int {
        return get(context, R.attr.colorAccent)
    } */
}