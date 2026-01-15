package com.example.seachem_dosing

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.seachem_dosing.databinding.ActivityMainBinding
import com.example.seachem_dosing.ui.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels()
    private var systemBarInsets: Insets? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.overflowIcon =
            AppCompatResources.getDrawable(this, R.drawable.ic_more_vert)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            systemBarInsets = systemBars
            binding.appBar.updatePadding(top = systemBars.top)
            binding.navView.updatePadding(bottom = systemBars.bottom)
            updateNavHostPadding()
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)
        setupToolbarMenu()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.topAppBar.title = destination.label?.toString() ?: getString(R.string.app_name)
            binding.navView.visibility =
                if (destination.id == R.id.navigation_profile) View.GONE else View.VISIBLE
            binding.appBar.visibility =
                if (destination.id == R.id.navigation_profile) View.GONE else View.VISIBLE
            updateNavHostPadding()
        }

        viewModel.ghUnit.observe(this) { updateToolbarMenuChecks() }
        viewModel.khUnit.observe(this) { updateToolbarMenuChecks() }
        updateToolbarMenuChecks()
    }

    private fun setupToolbarMenu() {
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_switch_profile -> {
                    if (navController.currentDestination?.id != R.id.navigation_profile) {
                        val popped = navController.popBackStack(R.id.navigation_profile, false)
                        if (!popped) {
                            navController.navigate(R.id.navigation_profile)
                        }
                    }
                    true
                }
                R.id.action_settings -> {
                    if (navController.currentDestination?.id != R.id.navigation_settings) {
                        navController.navigate(R.id.navigation_settings)
                    }
                    true
                }
                R.id.menu_theme_system -> {
                    applyThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    true
                }
                R.id.menu_theme_light -> {
                    applyThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    true
                }
                R.id.menu_theme_dark -> {
                    applyThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                    true
                }
                R.id.menu_hardness_dh -> {
                    viewModel.updateHardnessUnit("dh")
                    updateToolbarMenuChecks()
                    true
                }
                R.id.menu_hardness_ppm -> {
                    viewModel.updateHardnessUnit("ppm")
                    updateToolbarMenuChecks()
                    true
                }
                else -> false
            }
        }
    }

    private fun applyThemeMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        updateToolbarMenuChecks()
    }

    private fun updateToolbarMenuChecks() {
        val menu = binding.topAppBar.menu
        val themeMode = AppCompatDelegate.getDefaultNightMode()
        menu.findItem(R.id.menu_theme_system)?.isChecked =
            themeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ||
                themeMode == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        menu.findItem(R.id.menu_theme_light)?.isChecked =
            themeMode == AppCompatDelegate.MODE_NIGHT_NO
        menu.findItem(R.id.menu_theme_dark)?.isChecked =
            themeMode == AppCompatDelegate.MODE_NIGHT_YES

        val ghUnit = viewModel.ghUnit.value ?: "dh"
        val khUnit = viewModel.khUnit.value ?: "dh"
        val commonUnit = if (ghUnit == khUnit) ghUnit else null
        menu.findItem(R.id.menu_hardness_dh)?.isChecked = commonUnit == "dh"
        menu.findItem(R.id.menu_hardness_ppm)?.isChecked = commonUnit == "ppm"
    }

    private fun updateNavHostPadding() {
        val systemBars = systemBarInsets ?: return
        val topInset = if (binding.appBar.visibility == View.GONE) systemBars.top else 0
        val bottomInset = if (binding.navView.visibility == View.GONE) systemBars.bottom else 0
        binding.navHostFragmentActivityMain.updatePadding(top = topInset, bottom = bottomInset)
    }
}
