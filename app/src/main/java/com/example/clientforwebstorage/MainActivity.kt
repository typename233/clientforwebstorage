package com.example.clientforwebstorage

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.clientforwebstorage.network.TokenManager
import com.example.clientforwebstorage.ui.agent.AgentFragment
import com.example.clientforwebstorage.ui.files.FilesFragment
import com.example.clientforwebstorage.ui.groups.GroupsFragment
import com.example.clientforwebstorage.ui.profile.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var currentFilesFragment: FilesFragment? = null
    private var currentProfileFragment: ProfileFragment? = null
    private var currentAgentFragment: AgentFragment? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null
    private lateinit var bottomNav: BottomNavigationView

    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        currentFilesFragment?.let { frag ->
            uris.forEach { uri -> frag.handleUpload(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TokenManager.init(this)
        setContentView(R.layout.activity_main)

        if (TokenManager.isLoggedIn()) {
            showMain()
        } else {
            showLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        if (TokenManager.isLoggedIn()) {
            Thread { TokenManager.refreshToken() }.start()
            TokenManager.startPeriodicRefresh()
        }
    }

    override fun onPause() {
        super.onPause()
        TokenManager.stopPeriodicRefresh()
    }

    private fun showLogin() {
        val screen = com.example.clientforwebstorage.ui.LoginScreen(this,
            onSwitchToRegister = { showRegister() },
            onSwitchToForgetPassword = { showForgetPassword() },
            onLoginSuccess = { showMain() }
        )
        setContentView(screen.createView())
    }

    private fun showRegister() {
        val screen = com.example.clientforwebstorage.ui.RegisterScreen(this) { showLogin() }
        setContentView(screen.createView())
    }

    private fun showForgetPassword() {
        val screen = com.example.clientforwebstorage.ui.ForgetPasswordScreen(this) { showLogin() }
        setContentView(screen.createView())
    }

    private fun showMain() {
        setContentView(R.layout.activity_main)

        bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_files -> switchFragment(currentFilesFragment!!)
                R.id.nav_groups -> switchFragment(GroupsFragment())
                R.id.nav_agent -> switchFragment(currentAgentFragment!!)
                R.id.nav_profile -> switchFragment(currentProfileFragment!!)
            }
            true
        }

        currentFilesFragment = FilesFragment().apply {
            setRequestPickFiles { pickFilesLauncher.launch(arrayOf("*/*")) }
        }
        currentProfileFragment = ProfileFragment().apply {
            setLogoutCallback { showLogin() }
        }
        currentAgentFragment = AgentFragment().apply {
            setNavigationCallbacks(
                onFiles = { bottomNav.selectedItemId = R.id.nav_files },
                onUpload = {
                    bottomNav.selectedItemId = R.id.nav_files
                    pickFilesLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        switchFragment(currentFilesFragment!!)

        supportFragmentManager.addOnBackStackChangedListener {
            updateBottomNavigationVisibility()
        }

        setupBackHandler()
    }

    private fun switchFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        supportFragmentManager.executePendingTransactions()
        updateBottomNavigationVisibility()
    }

    private fun updateBottomNavigationVisibility() {
        if (!::bottomNav.isInitialized) return

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is com.example.clientforwebstorage.ui.profile.RecycleBinFragment ||
            currentFragment is com.example.clientforwebstorage.ui.profile.SharesFragment ||
            currentFragment is com.example.clientforwebstorage.ui.profile.ActivitiesFragment ||
            currentFragment is com.example.clientforwebstorage.ui.groups.ChatFragment) {
            bottomNav.visibility = View.GONE
        } else {
            bottomNav.visibility = View.VISIBLE
        }
    }

    fun hideBottomNav() {
        if (::bottomNav.isInitialized) {
            bottomNav.visibility = View.GONE
        }
    }

    fun showBottomNav() {
        if (::bottomNav.isInitialized) {
            bottomNav.visibility = View.VISIBLE
        }
    }

    private fun setupBackHandler() {
        onBackPressedCallback?.remove()
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFrag = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFrag is FilesFragment && currentFrag.handleBack()) return@handleOnBackPressed
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)
    }
}
