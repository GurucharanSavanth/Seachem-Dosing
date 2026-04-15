package com.example.seachem_dosing.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun hardnessSync_keepsCalculatorInputsInDegreesAfterPpmPreference() {
        val viewModel = MainViewModel(SavedStateHandle())

        viewModel.updateHardnessUnit("ppm")

        assertEquals(4.0, viewModel.getInput("equilibrium_current").value ?: -1.0, 0.001)
        assertEquals(6.0, viewModel.getInput("alkaline_buffer_current").value ?: -1.0, 0.001)
        assertEquals(0.0, viewModel.getInput("reef_builder_current").value ?: -1.0, 0.001)
    }

    @Test
    fun alkalinitySync_updatesReefAlkalinityCalculatorsOnly() {
        val viewModel = MainViewModel(SavedStateHandle())

        viewModel.setAlkalinity(8.5)

        assertEquals(8.5, viewModel.getInput("reef_builder_current").value ?: -1.0, 0.001)
        assertEquals(8.5, viewModel.getInput("reef_buffer_current").value ?: -1.0, 0.001)
        assertEquals(8.5, viewModel.getInput("reef_carbonate_current").value ?: -1.0, 0.001)
        assertEquals(8.5, viewModel.getInput("reef_fusion2_current").value ?: -1.0, 0.001)
        assertEquals(0.0, viewModel.getInput("khco3_current").value ?: -1.0, 0.001)
    }

    @Test
    fun exportData_includesSaltwaterParametersAndVolumeMode() {
        val viewModel = MainViewModel(SavedStateHandle())
        viewModel.setProfile(MainViewModel.AquariumProfile.SALTWATER)
        viewModel.setVolumeMode("lbh")

        val json = viewModel.generateExportData()

        assertTrue(json.contains("\"volumeMode\": \"lbh\""))
        assertTrue(json.contains("\"salinity\""))
        assertTrue(json.contains("\"alkalinity\""))
        assertTrue(json.contains("\"magnesium\""))
    }
}
