package com.android.hms.ui.activities

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.MenuItem
import android.view.Menu
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.model.Users
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.UserPreferences
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

// https://medium.com/@vinitagrawal91/android-colors-and-multiple-themes-fdfca3f75a15
abstract class BaseActivity: AppCompatActivity(), CoroutineScope by MainScope() {

    private var currentTheme = Globals.gDefaultTheme
    protected val STORAGE_PERMISSION_CODE = 100

    // protected val searchView: SearchView? = null

    internal val userPref get() = UserPreferences(applicationContext)
    internal val context get() = layoutInflater.context!!

    internal val isOwner get() = Users.isCurrentUserOwner()
    internal val isAdmin get() = Users.isCurrentUserAdmin()
    internal val isTenant get() = Users.isCurrentUserTenant()
    internal val isHelper get() = Users.isCurrentUserHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val progressBar = MyProgressBar(context)
        if (Users.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setObservers()
        // onBackPressedDispatcher.addCallback(this, callbackBackPressed)
        progressBar.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        initSearchView(menu)
        return true
    }

    open fun initSearchView(menu: Menu) {
        val searchItem: MenuItem? = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        initSearchView(searchView)
    }

    fun initSearchView(searchView: SearchView?, onSearch: (String) -> Unit = {}) {
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val searchText = (newText ?: "").trim()
                lifecycleScope.launch(Dispatchers.Main) {
                    val progressBar = MyProgressBar(context, "Searching for $searchText. Please wait...")
                    if (searchText.length > 1) filter(searchText, onSearch) else filter("", onSearch)
                    // onSearch(searchText)
                    progressBar.dismiss()
                }
                return true
            }
        })
    }

    open fun filter(searchText: String, onSearch: (String) -> Unit = {}) {
        onSearch(searchText)
        filter(searchText)
    }

    open fun filter(searchText: String) { }

    private fun onSearchNotInUse1(searchView: SearchView) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search query submission
                query?.let {
                    filter(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Handle search text change
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // onBackPressedDispatcher.onBackPressed() // Go back to the previous activity
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    open fun setObservers() { }

     /* open fun setObservers() {
         if (!User.getAllLive().hasObservers()) {
             User.getAllLive().observe(this, Observer { users ->
                 if (users != null) User.listAll = users
             })
         }

         if (!Voter.getAllLive().hasObservers()) {
             Voter.getAllLive().observe(this, Observer { voters ->
                 if (voters != null) Voter.listAll = voters
             })
         }
    } */

    fun setActionBarView(title: String = "") {
        val sActionBar = supportActionBar
        sActionBar?.setDisplayHomeAsUpEnabled(true)
        sActionBar?.setDisplayShowHomeEnabled(true)
        if (title.isNotBlank()) sActionBar?.title = title
    }

    fun setActionBarCustomView(customView: View) {
        val sActionBar = supportActionBar
        sActionBar?.setDisplayOptions(
            ActionBar.DISPLAY_SHOW_CUSTOM,
            ActionBar.DISPLAY_SHOW_CUSTOM or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_SHOW_TITLE
        )
        sActionBar?.setDisplayHomeAsUpEnabled(true)
        sActionBar?.setDisplayShowHomeEnabled(true)
        sActionBar?.setDisplayShowCustomEnabled(true)

        sActionBar?.customView = customView
    }

    private val callbackBackPressed = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Custom back press logic
            // Toast.makeText(this@MainActivity, "Back pressed", Toast.LENGTH_SHORT).show()
            // Optional: Perform some task or condition check before handling back press
//            super.onBackPressed() // Continue default back press behavior if needed
//            super.handleOnBackPressed()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        hideKeyboard()
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    open fun hideKeyboard() {
        try {
            CommonUtils.hideKeyboard(currentFocus ?: View(context))
        } catch (e: Exception) {
            CommonUtils.printException(e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) return // saveAndViewAsPDF()   // permission from popup was granted, call savePdf() method if needed
                else // permission from popup was denied, show error message
                    CommonUtils.toastMessage(context, "Permission is denied so will not be able to save as PDF files")
            }
        }
    }

    protected fun initDrawerHeaders(headerView: View?) {
        val currentUser = Users.currentUser ?: return
        headerView?.findViewById<TextView>(R.id.tvUsername)?.text = currentUser.name
        headerView?.findViewById<TextView>(R.id.tvEmailOrPhone)?.text = currentUser.phone.ifEmpty { currentUser.email }
    }

    protected fun logout() {
        val alertDialog = CommonUtils.confirmMessage(context, "Logout", "Are you sure you want to log out from this application?", "Logout Now")
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val progressBar = MyProgressBar(context, "Logging out from the application. Please wait...")
            lifecycleScope.launch {
                AuthUI.getInstance().signOut(context)
                FirebaseAuth.getInstance().signOut()
                UserPreferences(context).removeAll()
                alertDialog.dismiss()
                progressBar.dismiss()
                finish()
            }
        }
    }

    open fun getBitmap(): Bitmap? {
        return null
    }

    fun saveAndViewAsPDF(view: View, fileName: String) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) // permission was not granted, request it
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)

        val localBitMap = getBitmap()
        val bitmap =  if (localBitMap != null) localBitMap else {
            val bMap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val cTemp = Canvas(bMap)
            view.draw(cTemp)
            bMap
        }

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)
        val timeStamp = CommonUtils.getDateTextForFileName()
        val fileWithPath = File(context.getExternalFilesDir(null)!!.absolutePath + "/" + fileName + "-" + timeStamp +".pdf")
        try {
            document.writeTo(FileOutputStream(fileWithPath))
        } catch (e: Exception) {
            CommonUtils.showMessage(context, "Not able to save", "Not able to save as PDF. The error is $e")
            return
        }
        document.close()
        viewPDF(fileWithPath)
    }

    private fun viewPDF(file: File) {
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, applicationContext.packageName + ".provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                CommonUtils.showMessage(
                    context,
                    "Not able to open",
                    "Not able to open the generated PDF file. Please check whether any app is available to view PDF. The error is $e"
                )
            }
        }
    }
}
