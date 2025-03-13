package com.android.hms.ui.activities

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.android.hms.R
import com.android.hms.databinding.ActivityLoginBinding
import com.android.hms.model.User
import com.android.hms.model.Users
import com.android.hms.utils.CommonUtils
import com.android.hms.utils.Globals
import com.android.hms.utils.LaunchUtils
import com.android.hms.utils.MyProgressBar
import com.android.hms.utils.UserPreferences
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LoginActivity: AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityLoginBinding
    private var progressBar: MyProgressBar? = null

    private val firebaseSignInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { res ->
        this.onFirebaseSignInResult(res)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
            askNotificationPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val progressBar = MyProgressBar(this, "Please wait... auto login is being attempted...")
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { Globals.initialize() }
            if (autoLogin()) startMainActivity() else signInByFirebase()
            progressBar.dismiss()
        }

        title = getString(R.string.app_name)
        supportActionBar?.title = title

        binding.btnLogin.setOnClickListener { signInAsNewOwner() } // this should happen only for the first user
        processFCMToken()
        requestPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressBar?.dismiss()
    }

    private fun requestPermissions() {
        // In an Activity or Fragment (for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((this as Activity?)!!,
                arrayOf<String>(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        askForPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXIST);
        askForPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_EXIST);
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val READ_EXIST = 101
    private val WRITE_EXIST = 102
    private fun askForPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Should we show an explanation?

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this, permission
                )
            ) {
                //This is called if user has denied the permission before
                //In this case I am just asking the permission again

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(permission), requestCode
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(permission), requestCode
                )
            }
        } // else Toast.makeText(this, "$permission is already granted.", Toast.LENGTH_SHORT).show()
    }

    private fun processFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                CommonUtils.toastMessage(this,"Fetching FCM registration token failed. ${task.exception}")
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result // Log and toast val msg = getString(R.string.msg_token_fmt, token)
            UserPreferences(this).token = token
            Users.updateCurrentUserToken(token)
        })
    }

    private fun disableControls() {
        binding.etUsername.isEnabled = false
        binding.etEmail.isEnabled = false
        binding.etPhone.isEnabled = false
        binding.btnLogin.isEnabled = false
    }

    private fun signInByFirebase() {
        val providers = arrayListOf(
            // Choose authentication providers
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.PhoneBuilder().build(),
            //            AuthUI.IdpConfig.GoogleBuilder().build(),
            //            AuthUI.IdpConfig.FacebookBuilder().build(),
            //            AuthUI.IdpConfig.TwitterBuilder().build(),
        )

        val firebaseSignInIntent = AuthUI.getInstance() // Create and launch sign-in intent
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
//            .setLogo(R.drawable.ic_menu_slideshow)    // Custom logo
            .setTheme(R.style.Theme_HMS)  // Custom theme
            .build()

        firebaseSignInLauncher.launch(firebaseSignInIntent)
    }

    private fun onFirebaseSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        if (result.resultCode == RESULT_OK) { // Successfully signed in
            val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return // this can't be null
            binding.etUsername.setText(firebaseUser.displayName ?: "")
            val email = firebaseUser.email ?: ""
            val phone = firebaseUser.phoneNumber ?: ""
            if (email.isNotEmpty() && phone.isNotEmpty()) {
                CommonUtils.showMessage(this, "Not able to login", "Not able to login. Please contact your admin.")
                return
            }
            if (email.isNotEmpty()) {
                binding.etEmail.isEnabled = false
                binding.etEmail.setText(email)
                Users.currentUser = Users.getByEmail(email)
            }
            if (Users.currentUser == null && phone.isNotEmpty()) {
                binding.etPhone.isEnabled = false
                binding.etPhone.setText(phone)
                Users.currentUser = Users.getByPhone(phone)
            }
            if (Users.currentUser != null) {
                updateUserPreferences(Users.currentUser?.name ?: return, email, phone)
                Users.updateCurrentUserToken(UserPreferences(this).token)
                binding.btnLogin.text = "Loading... please wait..."
                binding.btnLogin.setTextColor(ContextCompat.getColor(this, R.color.ripple_effect))
                startMainActivity(false)
            }
            else {
                if (Users.getAll().isEmpty())
                    CommonUtils.showMessage(this, "Welcome", "Welcome new user (admin / owner). Please configure the buildings and houses after submitting your details.")
                else {
                    CommonUtils.showMessage(this, "Not authorized", "You are not authorized to use this app. Please contact your admin or house owner.")
                    disableControls()
                }
            }
        } else {
            // Sign in failed. If response is null the user canceled the
            // sign-in flow using the back button. Otherwise check
            // response.getError().getErrorCode() and handle the error.
            // ...
            CommonUtils.showMessage(this, "Not able to authenticate", "Please contact your administrator. The error is: " + result.idpResponse?.error?.message)
            disableControls()
        }
    }

    private fun autoLogin(): Boolean {
        val userPref = UserPreferences(this)
        if (userPref.phone.isNotEmpty()) Users.currentUser = Users.getByPhone(userPref.phone)
        if (Users.currentUser == null && userPref.email.isNotEmpty()) Users.currentUser = Users.getByEmail(userPref.email)
        Users.currentUser?.let  {
            binding.btnLogin.text = "Loading... please wait..."
            binding.btnLogin.setTextColor(ContextCompat.getColor(this, R.color.ripple_effect))
            binding.etUsername.setText(it.name)
            binding.etEmail.setText(it.email)
            binding.etPhone.setText(it.phone)
            disableControls()
        }
        return Users.isUserLoggedIn()
    }

    private fun startMainActivity(showMessage: Boolean = true) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (Users.isUserLoggedIn()) {
                if (Users.currentUser?.enable == true) {
                    progressBar = MyProgressBar(this@LoginActivity, "Loading... please wait...")
                    LaunchUtils.showMainActivity(this@LoginActivity)
                    finish()
                }
                else CommonUtils.showMessage(this@LoginActivity, "Not authorized", "You are not authorized to use this app. Please contact your admin or house owner.")
            } else {
                if (showMessage) CommonUtils.showMessage(this@LoginActivity, "Error in login", "Not able to get the current logged in user details. Please contact admin.")
            }
        }
    }

    // this should happen only for the first user
    private fun signInAsNewOwner() {
        val name = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        if (name.length < Globals.gMinNameChars) {
            CommonUtils.showMessage(this, "Username", "Please enter valid username.")
            return
        }
        if (phone.isEmpty() && email.isEmpty()) { // this condition should not occur
            CommonUtils.showMessage(this, "Phone / Email", "Please enter either valid phone or email Id.")
            return
        }

        val progressBar = MyProgressBar(this)
        val user = User(name = name, phone = phone, email = email, role = Users.Roles.OWNER.value, token = UserPreferences(this).token)
        lifecycleScope.launch(Dispatchers.Main)  {
            withContext(Dispatchers.IO) {
                Users.add(user) { success, error ->
                    lifecycleScope.launch(Dispatchers.Main)  {
                        if (success) {
                            updateUserPreferences(name, email, phone)
                            Users.currentUser = user
                            startMainActivity()
                        }
                        else CommonUtils.showMessage(this@LoginActivity, "Not able to login", "Not able to add new user $name. The error is: $error")
                        progressBar.dismiss()
                    }
                }
            }
        }
        // TO DO: get token and update the user object - Verify whether token gets updated automatically
    }

    private fun updateUserPreferences(name: String, email: String, phone: String) {
        val preferences = UserPreferences(this)
        preferences.name = name
        preferences.email = email
        preferences.phone = phone
    }
}
    /* private fun testMethod() {
        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber("+11234567890") // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d("AuthSuccess", "Verification successful: $credential")
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e("AuthError", "Error: ${e.localizedMessage}", e)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    Log.d("AuthCodeSent", "Code sent: $verificationId")
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)

    } */
