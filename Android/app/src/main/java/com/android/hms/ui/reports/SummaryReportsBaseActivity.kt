package com.android.hms.ui.reports

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.hms.R
import com.android.hms.ui.activities.BaseActivity
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFSheet
import java.io.File
import java.io.FileOutputStream
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Environment
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import com.android.hms.model.Users
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFRow
import java.io.OutputStream

abstract class SummaryReportsBaseActivity: ReportsBaseActivity() {

    protected lateinit var reportTableLayout: TableLayout
    protected lateinit var recyclerView: RecyclerView
    protected var monthsList = listOf<String>()

    private val borderDrawable get() = ContextCompat.getDrawable(context, R.drawable.table_row_cell_border)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary_report)
        setActionBarView(getReportName())
        recyclerView = findViewById(R.id.recyclerViewSummaryReport)
        reportTableLayout = findViewById(R.id.reportTableLayout)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun initSearchView(menu: Menu) {
        menu.findItem(R.id.action_search)?.isVisible = false // search feature is not available for summary reports
    }

    abstract fun prepareMonthsList()

    protected fun createHeaderAmountViews() : List<TextView> {
        val dateViews = mutableListOf<TextView>()
        val totalViews = monthsList.size
        dateViews.addAll(List(totalViews) { createTVNumHeader("") })   // repeat(totalViews) { dateViews.add(createTV("")) }
        return dateViews
    }
    protected fun createAmountViews() : List<TextView> {
        val dateViews = mutableListOf<TextView>()
        val totalViews = monthsList.size
        dateViews.addAll(List(totalViews) { createTVNum("") })   // repeat(totalViews) { dateViews.add(createTV("")) }
        return dateViews
    }

    protected fun createEmptyTextTableRow(columns: Int): TableRow {
        val tableRow = TableRow(context)
        repeat(columns) { tableRow.addView(createTV("")) }
        return tableRow
    }

    protected fun createEmptyTableRow(): TableRow {
        val tableRow = TableRow(context)
        tableRow.addView(createEmptyTV())
        return tableRow
    }

    protected fun createHeaderTableRow(columnValue: String): TableRow {
        val tableRow = TableRow(context)
        tableRow.addView(createTVHeader(columnValue))
        return tableRow
    }

    protected fun createTableRow(columnValue: String): TableRow {
        val tableRow = TableRow(context)
        tableRow.addView(createTV(columnValue))
        return tableRow
    }

    protected fun createTableRow(columnName: String, columnValue: String): TableRow {
        val tableRow = TableRow(context)
        tableRow.addView(createTV(columnName))
        tableRow.addView(createTV(" : ")) // add some delimiter with space
        tableRow.addView(createTV(columnValue))
        return tableRow
    }

    protected fun createTVNum(text: String, border: Boolean = true): TextView {
        val textView = TextView(context)
        textView.text = text
        if (border) textView.background = borderDrawable
        textView.setPadding(10 )
        textView.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
        // textView.setPadding(5, 5, 5, 5)
        return textView
    }

    protected fun createTVNumHeader(text: String, border: Boolean = true): TextView {
        val textView = createTVHeader(text)
        textView.setTypeface(null, Typeface.BOLD)
        if (border) textView.background = borderDrawable
        textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        textView.setPadding(10)
        return textView
    }

    protected fun createTV(text: String, border: Boolean = true): TextView {
        val textView = TextView(context)
        textView.text = text
        if (border) textView.background = borderDrawable
        textView.setPadding(10)
        textView.textAlignment = TextView.TEXT_ALIGNMENT_TEXT_START
        // textView.setPadding(2, 2,2,2)
        return textView
    }

    protected fun createTVHeader(text: String): TextView {
        val textView = createTV(text)
        textView.setTypeface(null, Typeface.BOLD)
        textView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        textView.setPadding(10)
        return textView
    }

    protected fun createEmptyTV(): TextView {
        val textView = TextView(context)
        textView.setPadding(10)
        // textView.setPadding(5, 5, 5, 5)
        return textView
    }

    protected fun highlightWhenTappedOld(tv: TextView) {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        tv.setBackgroundResource(typedValue.resourceId)
    }

    protected fun highlightWhenTapped(tv: TextView) {
        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        val rippleDrawable = AppCompatResources.getDrawable(tv.context, typedValue.resourceId)

        // val layers = arrayOf<Drawable>(rippleDrawable!!, borderDrawable!!)
        val layers = if (tv.background == null) arrayOf<Drawable>(rippleDrawable!!) else arrayOf<Drawable>(rippleDrawable!!, tv.background)
        tv.background = LayerDrawable(layers)
    }
}