package com.android.hms.utils

/**
 * Created by SivaMalini on 24-02-2018.
 */

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.format.DateUtils
import android.text.style.SuperscriptSpan
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.model.Users
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.messaging
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object CommonUtils {
    private const val TAG = "CommonUtils"

    fun printException(e: Exception) {
        println("Exception occurred and below are the details: ${Date()}")
        println(e.message)
        println(e.localizedMessage)
        println(e.stackTrace)
        e.printStackTrace()
        Log.d(TAG, e.message ?: "")
    }

    fun printMessage(message: String) {
        println("${Date()} :: $message")
        Log.d(TAG, message)
    }

    // val currentTime get() = Date().time
    val currentTime get() = System.currentTimeMillis()

    private fun getMillisecondsSinceStartOfToday(): Long {
        val now = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return now.timeInMillis - startOfDay.timeInMillis
    }

    /* fun getMillisecondsSinceStartOfToday(): Long {
        val now = LocalDateTime.now()
        val startOfDay = now.toLocalDate().atStartOfDay()
        return ChronoUnit.MILLIS.between(startOfDay, now)
    } */

    fun getMillisecondsTillYesterday(): Long {
        return currentTime - getMillisecondsSinceStartOfToday()
    }

    fun getLastWeekTime(): Long {
        return currentTime - DateUtils.WEEK_IN_MILLIS
    }

    fun oneMonthTime(): Long {
        return (30 * DateUtils.DAY_IN_MILLIS)
    }

    fun getLastMonthTime(): Long {
        return currentTime - oneMonthTime()
    }

    fun calculateDiffDays(dateInMillis: Long): Int {
        return TimeUnit.MILLISECONDS.toDays(currentTime - dateInMillis).toInt()
    }

    fun getDaysBetweenDates(dateStartInMs: Long, dateEndInMs: Long): Int {
        val dateStart = Date(dateStartInMs)
        val dateEnd = Date(dateEndInMs)
        return TimeUnit.DAYS.convert(dateEnd.time - dateStart.time, TimeUnit.MILLISECONDS).toInt()
    }

    fun getDateTimeDisplayText(date: Long): String {
        return getDateTimeDisplayText(Date(date))
    }

    private fun getDateTimeDisplayText(date: Date): String {
        if (DateUtils.isToday(date.time))
            return SimpleDateFormat(Globals.gTimeFormat, Locale.US).format(date) // today

        return if (DateUtils.isToday(date.time + DateUtils.DAY_IN_MILLIS)) "Yesterday" else SimpleDateFormat(Globals.gShortDayMonthFormat, Locale.US).format(date)
    }

    fun getDate(dateInMS: Long): Calendar {
        val date = Date()
        date.time = if (dateInMS < 0) 0 else dateInMS
        val calendar = Calendar.getInstance()
        calendar.time = date
        return calendar
    }

    fun getDate(date: String): Long {
        val format = SimpleDateFormat(Globals.gFullDateTimeFormat, Locale.US)
        try {
            return format.parse(date)!!.time
        } catch (e: Exception) {
            printException(e)
        }
        return Date().time
    }

    private fun getDateInMillis(selectedYear: Int, selectedMonth: Int, selectedDay: Int): Long {
        val selectedDate = Calendar.getInstance()
        selectedDate.set(selectedYear, selectedMonth, selectedDay)
        return selectedDate.timeInMillis
    }

    fun getTimeInText(time: Int, includeMinSecText: Boolean = true): String {
        val timeL = time.toLong()
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeL)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeL) - TimeUnit.MINUTES.toSeconds(minutes)
        val text0 = if (seconds < 10) "0" else ""  // add 0 if it is a single digit -> to make it double digit like 03
        return if (includeMinSecText) "$minutes min, $seconds sec" else "$minutes:$text0$seconds" // "$seconds sec"
        // return "$minutes:$seconds" // else "$seconds sec"
    }

    fun getDateTextForFileName() : String {
        return SimpleDateFormat(Globals.gFullDateTimeFormatWithSecondsForFile, Locale.US).format(Date())
    }

    fun getFullDayDateFormatText(dateInMS: Long): String {
        if (dateInMS == 0L) return ""
        val date = Date()
        date.time = dateInMS
        val sdf = SimpleDateFormat(Globals.gFullDayDateFormat, Locale.US)
        return sdf.format(date)
    }

    fun getFullDayDateTimeFormatText(): String {
        return getFullDayDateTimeFormatText(currentTime)
    }

    fun getFullDayDateTimeFormatText(dateInMS: Long): String {
        if (dateInMS == 0L) return ""
        val date = Date()
        date.time = dateInMS
        val sdf = SimpleDateFormat(Globals.gFullDayDateTimeFormat, Locale.US)
        return sdf.format(date)
    }

    fun getDayMonthFormatText(dateInMS: Long): String {
        if (dateInMS == 0L) return ""
        val date = Date()
        date.time = dateInMS
        val sdf = SimpleDateFormat(Globals.gDayMonthFormat, Locale.US)
        return sdf.format(date)
    }

    fun getMonthYearOnlyFormatText(dateInMS: Long): String {
        if (dateInMS == 0L) return ""
        val date = Date()
        date.time = dateInMS
        val sdf = SimpleDateFormat(Globals.gMonthYearOnlyFormat, Locale.US)
        return sdf.format(date)
    }

    fun getShortDayMonthFormatText(dateInMS: Long): String {
        if (dateInMS == 0L) return ""
        val date = Date()
        date.time = dateInMS
        val sdf = SimpleDateFormat(Globals.gShortDayMonthFormat, Locale.US)
        return sdf.format(date)
    }

    fun getShortDayMonthYearFormatText(dateInMS: Long): String {
        if (dateInMS == 0L) return ""
        val date = Date()
        date.time = dateInMS
        val sdf = SimpleDateFormat(Globals.gShortDayMonthYearFormat, Locale.US)
        return sdf.format(date)
    }

    fun setDate(etDate: EditText, dateInMillis: Long) {
        etDate.setText("Date: ${getFullDayDateFormatText(dateInMillis)}")
        etDate.tag = dateInMillis
    }

    fun pickAndSetDate(etDate: EditText) { // , setDateInMillis:(dateInMillis: Long) -> Unit) {
        etDate.tag = 0L

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, selectedYear, selectedMonth, selectedDay ->
            setDate(etDate, getDateInMillis(selectedYear, selectedMonth, selectedDay))
            // setDateInMillis(dateInMillis)
        }

        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(etDate.context, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.datePicker.maxDate = currentTime
            datePickerDialog.show()
        }
    }

    fun pickAndSetDate(textView: TextView, dateInMS: Long = Date().time) {
        textView.tag = 0L
        val date = Date()
        date.time = dateInMS
        val calendar = Calendar.getInstance()
        calendar.time = date
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(textView.context, DatePickerDialog.OnDateSetListener { _, yearSelected, monthSelected, daySelected ->
            calendar.set(yearSelected, monthSelected, daySelected)
            val sdf = SimpleDateFormat(Globals.gDayMonthFormat, Locale.US)
            textView.text = sdf.format(calendar.time)
            textView.tag = calendar.timeInMillis
        }, year, month, day)

        datePickerDialog.show()
    }

    fun getMonthsBetweenDates(startDateMillis: Long, endDateMillis: Long): List<String> {
        val startDate = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val endDate = Calendar.getInstance().apply { timeInMillis = endDateMillis }
        val months = mutableListOf<String>()
        val dateFormat = SimpleDateFormat(Globals.gMonthYearOnlyFormat, Locale.getDefault())

        while (startDate.before(endDate) || startDate.get(Calendar.MONTH) == endDate.get(Calendar.MONTH) && startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR)) {
            months.add(dateFormat.format(startDate.time))
            startDate.add(Calendar.MONTH, 1)
        }
        return months
    }

    fun getMonthsBetweenDatesReverse(startDateMillis: Long, endDateMillis: Long): List<String> {
        val startDate = Calendar.getInstance().apply { timeInMillis = endDateMillis } // Start with endDate
        val endDate = Calendar.getInstance().apply { timeInMillis = startDateMillis } // End with startDate
        val months = mutableListOf<String>()
        val dateFormat = SimpleDateFormat(Globals.gMonthYearOnlyFormat, Locale.getDefault())

        while (startDate.after(endDate) || startDate.get(Calendar.MONTH) == endDate.get(Calendar.MONTH) && startDate.get(Calendar.YEAR) == endDate.get(Calendar.YEAR)) {
            months.add(dateFormat.format(startDate.time))
            startDate.add(Calendar.MONTH, -1) // Decrement by one month
        }

        return months
    }

    fun toastMessage(context: Context, message: String, isLongDuration: Boolean = true) {
        Toast.makeText(context, message, if (isLongDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    fun showMessage(context: Context, title: String, message: String) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle(title)
        dialogBuilder.setMessage(message)
        dialogBuilder.setCancelable(true)
        dialogBuilder.setPositiveButton("OK", null)
        dialogBuilder.show()
    }

    fun confirmMessage(context: Context, title: String, message: String, confirm: String = "Confirm", cancel: String = "Cancel"): AlertDialog {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setMessage(message)
        dialogBuilder.setCancelable(false)
        dialogBuilder.setPositiveButton(confirm, null)
        dialogBuilder.setNegativeButton(cancel, null)
        val alert = dialogBuilder.create()
        alert.setTitle(title)
        alert.show()
        return alert
    }

    fun hideKeyboard(view: View) {
        try {
            val imm =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            printException(e)
        }
    }

    fun formatIntList(intList: List<Int>): String {
        return when (intList.size) {
            0 -> ""
            1 -> intList[0].toString()
            2 -> "${intList[0]} and ${intList[1]}"
            else -> intList.dropLast(1).joinToString(", ") + " and " + intList.last()
        }
    }

    fun formatBoolToText(boolean: Boolean): String {
        return if (boolean) "Yes" else "No"
    }

    fun formatNumToText(num: Int): String {
        return "%,d".format(Locale.US, num)
    }

    fun formatNumToText(num: Float): String {
        return if (num % 1 == 0.0f) {
            "%,.0f".format(Locale.US, num)
        } else {
            val decimalPart = num.toString().split(".")[1]
            if (decimalPart.length == 1) {
                "%,.1f".format(Locale.US, num)
            } else {
                "%,.2f".format(Locale.US, num)
            }
        }
    }

    fun formatNumToText(num: Double): String {
        return if (num % 1 == 0.0) {
            "%,.0f".format(Locale.US, num)
        } else {
            val decimalPart = num.toString().split(".")[1]
            if (decimalPart.length == 1) {
                "%,.1f".format(Locale.US, num)
            } else {
                "%,.2f".format(Locale.US, num)
            }
        }
    }

    fun writeNumToText(num: Int): String {
        if (num == 0) return "zero"
        if (num < 0) return "minus ${writeNumToText(-num)}"

        val belowTwenty = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"
        )
        val tens = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
        val thousandPowers = arrayOf("", "thousand")

        fun helper(n: Int): String {
            return when {
                n < 20 -> belowTwenty[n]
                n < 100 -> tens[n / 10] + (if (n % 10 != 0) " ${belowTwenty[n % 10]}" else "")
                n < 1000 -> belowTwenty[n / 100] + " hundred" + (if (n % 100 != 0) " and ${helper(n % 100)}" else "")
                else -> ""
            }
        }

        var numText = ""
        var remainingNum = num
        var index = 0

        while (remainingNum > 0) {
            val chunk = remainingNum % 1000
            if (chunk != 0) {
                val chunkText = helper(chunk)
                numText = "$chunkText ${thousandPowers[index]} $numText".trim()
            }
            remainingNum /= 1000
            index++
        }

        return numText.trim()
    }


    fun numToText(num: Int): String {
        val million = 1000000
        if (num >= million) return "${(num / million)}m"

        val thousand = 1000
        if (num >= thousand) return "${(num / thousand)}k"

        return "$num"
    }

    /* fun isGuestWithMessage(context: Context): Boolean {
        if (Users.isCurrentUserGuest) {
            showMessage(context, "Guest Access", "Your role is not configured hence please contact system administrator")
            return true
        }
        return false
    } */

    fun isNotAdminWithMessage(context: Context): Boolean {
        if (!Users.isCurrentUserAdmin()) {
            showMessage(context, "Unauthorized", "You are not authorized to view this. Please contact system administrator")
            return true
        }
        return false
    }

    fun isValidEmail(emailId: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(emailId).matches()
    }

    fun isConnectedToNetworkWithWarningMessage(context: Context): Boolean {
        if (!isConnectedToNetwork(context)) {
            toastMessage(context, "Any changes will not be applied as not connected to network (data or wifi)", true)
            return false
        }
        return true
    }

    fun isConnectedToNetworkWithMessage(context: Context): Boolean {
        if (!isConnectedToNetwork(context)) {
            toastMessage(context, "Please check internet connectivity (data or wifi)", true)
            return false
        }
        return true
    }

    // @Suppress("DEPRECATION")
    fun isConnectedToNetwork(context: Context): Boolean {
        var result = false
        try {
            val cm = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?) ?: return false
            cm.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                        else -> false
                    }
                }
            }
        } catch (e: Exception) {
            printException(e)
        }
        return result
    }

    fun subscriptText(text: String, subText: String): SpannableStringBuilder {
        val superscriptSpan = SuperscriptSpan()
        val builder = SpannableStringBuilder(text)
        try {
            builder.setSpan(
                superscriptSpan,
                text.indexOf(subText),
                text.indexOf(subText) + subText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } catch (exp: Exception) {
            printException(exp)
        }
        return builder
    }

    fun setMenuIconsVisible(context: Context, popupMenu: PopupMenu) {
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
            val size = popupMenu.menu.size() - 1
            for (index in 0..size) {
                val menuItem = popupMenu.menu.getItem(index) ?: continue
                setTintColor(menuItem.icon, ContextCompat.getColor(context, R.color.default_text))
            }
        } catch (e: Exception){
            toastMessage(context, "Error in showing menu icons: $e")
        } finally {
            popupMenu.show()
        }
    }

    /* fun setMenuIconsVisible(context: Context, menu: Menu, tintColor: Int = R.color.default_text) {
        // Force icons to be displayed
        val menuHelper = MenuPopupHelper(context, menu as MenuBuilder, itemView)
        menuHelper.setForceShowIcon(true)
        menuHelper.show()

        if (menu is MenuBuilder) menu.setOptionalIconsVisible(true)
        val size = menu.size() - 1
        for (index in 0..size) {
            val menuItem = menu.getItem(index) ?: continue
            setTintColor(menuItem.icon, ContextCompat.getColor(context, tintColor))
        }
    } */

    fun getDrawable(context: Context, iconResource:Int, tintColor: Int = 0) : Drawable? {
        val drawable = ContextCompat.getDrawable(context, iconResource) ?: return null
        setTintColor(drawable, if (tintColor == 0) ThemeColor.colorPrimary(context) else tintColor)
        return drawable
    }

//    fun getAnimatedDrawable(context: Context, tintColor: Int = 0) : Drawable? {
//        return if (tintColor == 0) MediaUtils.getProgressAnimation(context) else getDrawable(context, R.drawable.progress_animation, tintColor)
//    }

    fun setTextViewDrawableColor(textView: TextView?, color: Int = 0, useContextCompat: Boolean = true) {
        textView ?: return
        val context = textView.context
        val colorToUse = if (color == 0) ThemeColor.colorButton(context) else if (useContextCompat) ContextCompat.getColor(context, color) else color
        textView.compoundDrawables.forEach { setTintColor(it, colorToUse) }
        textView.compoundDrawablesRelative.forEach { setTintColor(it, colorToUse) }
    }

    fun setTintColor(drawable: Drawable?, tintColor: Int) : Drawable? {
        drawable ?: return null
        drawable.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        drawable.setTint(tintColor)
        drawable.mutate() // If we don't mutate the drawable, then all drawable's with this id will have a color filter applied to it.
        return drawable
    }

    fun isEllipsized(textView: TextView) : Boolean {
        if (textView.text.isBlank() || textView.maxLines == 0) return  false
        val layout = textView.layout ?: return false
        val lines = layout.lineCount
        if (lines > 0) return layout.getEllipsisCount(lines - 1) > 0
        return false
    }

    fun initializeRecyclerView(context: Context, recyclerView: RecyclerView) {
        val layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
    }

    fun getValueFromTextInputView(textInputView: TextInputEditText?): Double {
        return try {
            textInputView?.text?.trim().toString().toDouble()
        } catch (e: java.lang.Exception) {
            0.0
        }
    }

    fun initDateView(dateButton: Button?, dateSelected: Long) {
        dateButton?.tag = dateSelected
        dateButton?.text = getDayMonthFormatText(dateSelected)
        dateButton?.setOnClickListener { view -> pickAndSetDate(view as TextView, dateSelected) }
    }

    fun reEnableFCM() {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        Firebase.messaging.isAutoInitEnabled = true
    }

    fun getMonthName(month: Int): String {
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return if (month in monthNames.indices) monthNames[month] else "Invalid Month"
    }

    inline fun <reified T : Any> Intent.putExtra(key: String, value: T): Intent {
        return this.apply {
            when (value) {
                is String -> putExtra(key, value)
                is Int -> putExtra(key, value)
                is Boolean -> putExtra(key, value)
                is Parcelable -> putExtra(key, value)
                // Add other types if needed
            }
        }
    }
}

//    fun getVersion(): String {
//        return "v ${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}"
//    }


    /* fun isConnectedToNetwork(context: Context): Boolean {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            return if (connectivityManager is ConnectivityManager) {
                val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected ?: false
            } else false
        }
        catch (e: Exception) {
            printException(e)
        }
        return false
    } */
