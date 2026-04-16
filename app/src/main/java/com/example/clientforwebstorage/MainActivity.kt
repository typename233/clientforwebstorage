package com.example.clientforwebstorage

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.ui.LoginScreen
import com.example.clientforwebstorage.ui.RegisterScreen
import com.example.clientforwebstorage.ui.ResourceScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)

        if (TokenManager.isLoggedIn()) {
            showResources()
        } else {
            showLogin()
        }
    }

    private fun showLogin() {
        val screen = LoginScreen(this,
            onSwitchToRegister = { showRegister() },
            onLoginSuccess = { showResources() }
        )
        setContentView(screen.createView())
    }

    private fun showRegister() {
        val screen = RegisterScreen(this) {
            showLogin()
        }
        setContentView(screen.createView())
    }

    fun showResources() {
        val screen = ResourceScreen(this) {
            showLogin()
        }
        setContentView(screen.createView())
    }
}