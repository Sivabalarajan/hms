package com.android.hms.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.android.hms.R

object GenericInputDialog {

    fun showInputDialog(
        context: Context,
        title: String,
        hint: String,
        onSubmit: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        // Inflate the custom layout
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_user_input, null)

        // Initialize views
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etUserInput = dialogView.findViewById<EditText>(R.id.etUserInput)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)

        // Set title and hint dynamically
        tvDialogTitle.text = title
        etUserInput.hint = hint

        // Create the dialog
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false) // Prevent closing without action
            .create()

        // Handle button clicks
        btnCancel.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val input = etUserInput.text.toString()
            if (input.isNotEmpty()) {
                onSubmit(input)
                dialog.dismiss()
            } else {
                etUserInput.error = "Please enter valid value"
            }
        }

        dialog.show()
    }
}