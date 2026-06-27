package com.example.seachem_dosing.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.seachem_dosing.R
import com.example.seachem_dosing.ui.MainViewModel.AquariumProfile
import com.example.seachem_dosing.ui.theme.SeachemTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device parity gate for the Compose [ProfileSelectionScreen] (ADR-001).
 * Asserts the XML behaviour: Continue is disabled until a profile is selected,
 * then enabling it and tapping fires onContinue with that profile.
 */
@RunWith(AndroidJUnit4::class)
class ProfileSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private fun str(id: Int) = ctx.getString(id)

    @Test
    fun continue_disabled_until_selection_then_fires_callback() {
        var continued: AquariumProfile? = null
        composeTestRule.setContent {
            SeachemTheme {
                ProfileSelectionScreen(initialSelected = null, onContinue = { continued = it })
            }
        }

        val continueLabel = str(R.string.profile_select_continue)
        composeTestRule.onNodeWithText(continueLabel).assertIsDisplayed().assertIsNotEnabled()

        // Select Freshwater → Continue becomes enabled → tap fires the callback.
        composeTestRule.onNodeWithText(str(R.string.profile_freshwater)).performClick()
        composeTestRule.onNodeWithText(continueLabel).assertIsEnabled().performClick()

        assertEquals(AquariumProfile.FRESHWATER, continued)
    }

    @Test
    fun all_three_profiles_are_shown() {
        composeTestRule.setContent {
            SeachemTheme { ProfileSelectionScreen(initialSelected = null, onContinue = {}) }
        }
        composeTestRule.onNodeWithText(str(R.string.profile_freshwater)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.profile_saltwater)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.profile_pond)).assertIsDisplayed()
    }
}
