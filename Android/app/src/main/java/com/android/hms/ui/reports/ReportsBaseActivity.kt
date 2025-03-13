package com.android.hms.ui.reports

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Repair
import com.android.hms.model.Users
import com.android.hms.ui.activities.BaseActivity
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.MyProgressBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCell
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFRichTextString
import org.apache.poi.xssf.usermodel.XSSFRow
import org.apache.poi.xssf.usermodel.XSSFSheet
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

abstract class ReportsBaseActivity : BaseActivity() {

    abstract fun getReportName(): String
    abstract fun saveToOutputStream(outputStream: OutputStream)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_reports_activity, menu)
        initSearchView(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_export_to_excel -> saveReportToExcel()
            R.id.action_export_to_excel_and_share -> exportAndShare()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri: Uri? ->
        uri?.let {
            val progressBar = MyProgressBar(this, "Exporting Report to Excel... Please wait...")
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { exportReportToUri(it) }
                CommonUtils.toastMessage(context, "Report exported successfully.")
                progressBar.dismiss()
            }
        }
    }

    private fun saveReportToExcel() {
        openFilePicker()
        /*  val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "RepairSummaryReport.xlsx").absolutePath
        ExportReport.exportRepairReportToExcel(repairReportData, filePath)

        val reportData = generateReport(repairs)
        val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "RepairReport.xlsx").absolutePath
        exportReportToExcel(reportData, filePath) */
    }

    protected fun addCommentToExcelCell(sheet: XSSFSheet, cell: Cell, commentText: String) { // Add a comment to the building cell
        if (commentText.isEmpty()) return
        val drawing = sheet.createDrawingPatriarch()
        val comment = drawing.createCellComment(XSSFClientAnchor()).apply {
            string = XSSFRichTextString(commentText)
            author = "Author"
        }
        cell.cellComment = comment
    }

    private fun getBoldCenteredCellStyle(workbook: Workbook): XSSFCellStyle {  // XSSFCellStyle { // Create a cell style for bold and centered text
        return workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            setFont(workbook.createFont().apply {
                bold = true
            })
        } as XSSFCellStyle
    }

    private fun getNumberFormatCellStyle(row: XSSFRow): XSSFCellStyle {
        val workbook = row.sheet.workbook
        val dataFormat = workbook.createDataFormat()
        val cellStyle = workbook.createCellStyle()
        cellStyle.dataFormat = dataFormat.getFormat("#,##0")
        return cellStyle
    }

    private fun cellFormat(row: XSSFRow, columnIndex: Int) {
        val cell = row.createCell(columnIndex)
        cell.setCellValue(1234.56)

        val cellStyle = cell.sheet.workbook.createCellStyle()

        val font = cell.sheet.workbook.createFont()
        font.bold = true
        cellStyle.setFont(font)

        val dataFormat = cell.sheet.workbook.createDataFormat()
        cellStyle.dataFormat = dataFormat.getFormat("#,##0")

        cellStyle.alignment = HorizontalAlignment.CENTER
        cellStyle.verticalAlignment = VerticalAlignment.CENTER

        cell.cellStyle = cellStyle
    }

    private fun getCurrencyFormatCellStyle(row: XSSFRow): XSSFCellStyle {
        val workbook = row.sheet.workbook
        val dataFormat = workbook.createDataFormat()
        val cellStyle = workbook.createCellStyle()
        cellStyle.dataFormat = dataFormat.getFormat("\u20B9#,##0")
        return cellStyle
    }

    private fun cellFillColorWithRed(cell: XSSFCell) {
        val workbook = cell.sheet.workbook
        val cellStyle = if (cell.cellStyle != null) cell.cellStyle else workbook.createCellStyle() as XSSFCellStyle

        // Set the fill foreground color index
        cellStyle.setFillForegroundColor(IndexedColors.RED.index)
        cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        // Apply the style to the cell
        cell.cellStyle = cellStyle
    }

    fun cellTextColorAsRed(cell: XSSFCell) {
        val workbook = cell.sheet.workbook
        val cellStyle = workbook.createCellStyle() as XSSFCellStyle
        val font = workbook.createFont()

        // Set the font color
        font.color = IndexedColors.RED.index
        cellStyle.setFont(font)

        // Apply the style to the cell
        cell.cellStyle = cellStyle
    }

    protected fun createExcelCell(row: XSSFRow, colIndex: Int, content: String): XSSFCell {
        val cell = row.createCell(colIndex)
        cell.setCellValue(content)
        return cell
    }

    protected fun createExcelCell(row: XSSFRow, colIndex: Int, content: Float): XSSFCell {
        val formattedContent = CommonUtils.formatNumToText(content).toDouble()
        return createExcelCell(row, colIndex, formattedContent)
    }

    protected fun createExcelCell(row: XSSFRow, colIndex: Int, content: Double): XSSFCell {
        val cell = row.createCell(colIndex)
        cell.setCellValue(content)
        return cell
    }

    protected fun createExcelCell(row: XSSFRow, colIndex: Int, content: Int): XSSFCell {
        return createExcelCell(row, colIndex, content.toDouble())
    }

    protected fun createExcelCell(row: XSSFRow, colIndex: Int, content: Boolean): XSSFCell {
        return createExcelCell(row, colIndex, CommonUtils.formatBoolToText(content))
    }

    protected fun createExcelCell(row: XSSFRow, colIndex: Int, content: Long, isDate: Boolean = true): XSSFCell {
        return if (isDate) createExcelCell(row, colIndex, CommonUtils.getFullDayDateFormatText(content))
        else createExcelCell(row, colIndex, content.toDouble())
    }

    protected fun createHeaderExcelCell(row: XSSFRow, colIndex: Int, content: String): XSSFCell {
        val cell = createExcelCell(row, colIndex, content)
        cell.cellStyle = getBoldCenteredCellStyle(row.sheet.workbook)
        return cell
    }

    protected fun createHeaderExcelCell(row: XSSFRow, colIndex: Int, content: Double): XSSFCell {
        val cell = createExcelCell(row, colIndex, content)
        cell.cellStyle = getBoldCenteredCellStyle(row.sheet.workbook)
        return cell
    }

    protected fun createHeaderExcelCell(row: XSSFRow, colIndex: Int, content: Int): XSSFCell {
        return createHeaderExcelCell(row, colIndex, content.toDouble())
    }

    protected fun autoAdjustRowsColumnsHeight(sheet: XSSFSheet) {
        // Find the maximum number of cells in any row
        var maxCellsCount: Short = 0
        for (i in 0 until sheet.physicalNumberOfRows) {
            val row = sheet.getRow(i) ?: continue
            if (row.lastCellNum > maxCellsCount) maxCellsCount = row.lastCellNum
        }

        // Adjust the width of each column to fit the content
        for (i in 0 until maxCellsCount) {
            var maxWidth = 0
            for (j in 0 until sheet.physicalNumberOfRows) {
                val row = sheet.getRow(j) ?: continue
                val cell = row.getCell(i) ?: continue
                val cellContent = cell.toString()
                val cellWidth = cellContent.length * 256 // Approximate width calculation
                if (cellWidth > maxWidth) maxWidth = cellWidth
            }
            sheet.setColumnWidth(i, maxWidth)
        }

        // Adjust the height of each row to fit the content
        for (i in 0 until sheet.physicalNumberOfRows) {
            val row = sheet.getRow(i)
            row?.height = -1 // Auto size row height
        }
    }

    private fun openFilePicker() {
        createDocumentLauncher.launch(getReportExcelFileName())
    }

    private fun exportReportToUri(uri: Uri) {
        contentResolver.openOutputStream(uri)?.use { outputStream -> saveToOutputStream(outputStream) }
    }

    private fun getReportExcelFileName(): String {
        return "${getReportName()} ${CommonUtils.getDateTextForFileName()}.xlsx"
    }

    private fun exportAndShare() {
        val progressBar = MyProgressBar(this, "Preparing report for export as an Excel document. Please wait while the file is being generated...")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val file = saveReportTemporarily()
                shareReport(file)
                // deleteReportFile(file)
            }
            progressBar.dismiss()
        }
    }

    private fun saveReportTemporarily(): File {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), getReportExcelFileName())
        FileOutputStream(file).use { outputStream ->
            saveToOutputStream(outputStream) // // Populate the sheet with data (similar to exportReportToUri)
        }
        return file
    }

    private fun shareReport(file: File) {
        val uri: Uri = FileProvider.getUriForFile(this, "${context.packageName}.provider", file)
        val today = CommonUtils.getFullDayDateTimeFormatText()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_SUBJECT, "${getReportName()} - $today")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("${Users.currentUser?.email}"))
            putExtra(Intent.EXTRA_TEXT, "Hi,\n\nPlease find attached ${getReportName()} as of today $today.\n\nBest regards,\nSystem Administrator")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Sharing report file..."))
    }

    private fun deleteReportFile(file: File) {
        if (file.exists()) file.delete()
    }

    protected fun createRepairSheet(sheet: XSSFSheet, repairsList: List<Repair>, rowToStart: Int = 0, columnToStart: Int = 0): Int {
        if (repairsList.isEmpty()) {
            sheet.createRow(rowToStart).createCell(columnToStart).setCellValue("No repairs available.")
            return rowToStart + 1
        }
        // create header row and columns
        var rowIndex = rowToStart
        var colIndex = columnToStart
        val headerRow = sheet.createRow(rowIndex++)
        //createHeaderExcelCell(headerRow, colIndex++, "Name")
        createHeaderExcelCell(headerRow, colIndex++, "Description")
        // createHeaderExcelCell(headerRow, colIndex++, "Category")
        createHeaderExcelCell(headerRow, colIndex++, "Status")
        createHeaderExcelCell(headerRow, colIndex++, "Amount")
        createHeaderExcelCell(headerRow, colIndex++, "Building")
        createHeaderExcelCell(headerRow, colIndex++, "House")
        createHeaderExcelCell(headerRow, colIndex++, "Tenant")
        createHeaderExcelCell(headerRow, colIndex++, "Tenant Paid")
        createHeaderExcelCell(headerRow, colIndex++, "Paid Type")
        createHeaderExcelCell(headerRow, colIndex++, "Raised On")
        createHeaderExcelCell(headerRow, colIndex++, "Raised By")
        createHeaderExcelCell(headerRow, colIndex++, "Fixed On")
        createHeaderExcelCell(headerRow, colIndex++, "Fixed By")
        createHeaderExcelCell(headerRow, colIndex++, "Paid On")
        createHeaderExcelCell(headerRow, colIndex++, "Days to Fix")
        createHeaderExcelCell(headerRow, colIndex++, "Days to Pay after Fix")

        var total = 0.0
        repairsList.forEach { repair ->
            colIndex = 0
            val row = sheet.createRow(rowIndex++)
            val nameCell = createExcelCell(row, colIndex++, repair.desc)
            // createExcelCell(row, colIndex++, repair.desc)
            // createExcelCell(row, colIndex++, repair.category)
            createExcelCell(row, colIndex++, repair.status)
            createExcelCell(row, colIndex++, repair.amount)
            createExcelCell(row, colIndex++, repair.bName)
            createExcelCell(row, colIndex++, repair.hName)
            createExcelCell(row, colIndex++, repair.tName)
            createExcelCell(row, colIndex++, repair.tPaid)
            createExcelCell(row, colIndex++, repair.paidType)
            createExcelCell(row, colIndex++, repair.raisedOn)
            createExcelCell(row, colIndex++, repair.raisedBy)
            createExcelCell(row, colIndex++, repair.fixedOn)
            createExcelCell(row, colIndex++, repair.fixedBy)
            createExcelCell(row, colIndex++, repair.paidOn)
            createExcelCell(row, colIndex++, CommonUtils.getDaysBetweenDates(repair.raisedOn, repair.fixedOn))
            createExcelCell(row, colIndex++, CommonUtils.getDaysBetweenDates(repair.fixedOn, repair.paidOn))
            addCommentToExcelCell(sheet, nameCell, repair.notes)

            total += repair.amount
        }
        sheet.createRow(rowIndex++)
        val totalRow = sheet.createRow(rowIndex++)
        createHeaderExcelCell(totalRow, 0, "Total")
        createHeaderExcelCell(totalRow, 4, total)
        return rowIndex
    }
}
