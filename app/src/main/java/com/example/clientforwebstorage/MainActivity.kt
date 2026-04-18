package com.example.clientforwebstorage

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.ui.LoginScreen
import com.example.clientforwebstorage.ui.MainScreen
import com.example.clientforwebstorage.ui.RegisterScreen

class MainActivity : AppCompatActivity() {

    private var mainScreen: MainScreen? = null
    private var onFilesPickedCallback: ((List<Uri>) -> Unit)? = null

    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        onFilesPickedCallback?.invoke(uris)
        mainScreen?.onFilesPicked(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)

        if (TokenManager.isLoggedIn()) {
            showMain()
        } else {
            showLogin()
        }
    }

    private fun showLogin() {
        mainScreen = null
        val screen = LoginScreen(this,
            onSwitchToRegister = { showRegister() },
            onLoginSuccess = { showMain() }
        )
        setContentView(screen.createView())
    }

    private fun showRegister() {
        mainScreen = null
        val screen = RegisterScreen(this) {
            showLogin()
        }
        setContentView(screen.createView())
    }

    private fun showMain() {
        val screen = MainScreen(this,
            onLogout = { showLogin() },
            requestPickFiles = {
                pickFilesLauncher.launch(arrayOf("*/*"))
            }
        )
        mainScreen = screen
        setContentView(screen.createView())
    }
}