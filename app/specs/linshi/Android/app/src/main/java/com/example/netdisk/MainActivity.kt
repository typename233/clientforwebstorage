package com.example.netdisk

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.netdisk.ui.agent.AgentFragment
import com.example.netdisk.ui.files.FilesFragment
import com.example.netdisk.ui.groups.GroupsFragment
import com.example.netdisk.ui.profile.ProfileFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            switchFragment(FilesFragment())
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_files -> switchFragment(FilesFragment())
                R.id.nav_groups -> switchFragment(GroupsFragment())
                R.id.nav_agent -> switchFragment(AgentFragment())
                R.id.nav_profile -> switchFragment(ProfileFragment())
            }
            true
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
