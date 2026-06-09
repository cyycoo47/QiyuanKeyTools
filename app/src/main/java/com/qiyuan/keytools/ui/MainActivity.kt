package com.qiyuan.keytools.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.qiyuan.keytools.R
import com.qiyuan.keytools.databinding.ActivityMainBinding
import com.qiyuan.keytools.ui.detect.DetectFragment
import com.qiyuan.keytools.ui.mapping.MappingFragment
import com.qiyuan.keytools.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val detectFragment  by lazy { DetectFragment() }
    private val mappingFragment by lazy { MappingFragment() }
    private val settingsFragment by lazy { SettingsFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(detectFragment)
        }

        binding.bottomNav.setOnItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.nav_detect  -> { showFragment(detectFragment);  true }
                R.id.nav_mapping -> { showFragment(mappingFragment); true }
                R.id.nav_settings -> { showFragment(settingsFragment); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        val tag = fragment::class.java.simpleName
        val fm = supportFragmentManager
        val existing = fm.findFragmentByTag(tag)
        fm.beginTransaction().apply {
            fm.fragments.forEach { hide(it) }
            if (existing == null) {
                add(R.id.fragment_container, fragment, tag)
            } else {
                show(existing)
            }
        }.commit()
    }
}
