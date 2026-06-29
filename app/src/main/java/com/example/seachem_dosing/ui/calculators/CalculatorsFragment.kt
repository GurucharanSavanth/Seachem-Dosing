package com.example.seachem_dosing.ui.calculators

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.seachem_dosing.R
import com.example.seachem_dosing.core.numerics.StoredDecimal
import com.example.seachem_dosing.data.repository.HistoryWriteOutcome
import com.example.seachem_dosing.domain.history.Quantity
import com.example.seachem_dosing.domain.history.UnitCode
import com.example.seachem_dosing.domain.usecase.LogAdministeredDoseUseCase
import com.example.seachem_dosing.ui.MainViewModel
import com.example.seachem_dosing.ui.theme.SeachemTheme
import com.google.android.material.transition.MaterialFadeThrough
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Hosts the Compose [CalculatorsScreen] (ADR-001). A "Log as administered" action on each dose
 * result records the **physical g/mL** amount to history via [LogAdministeredDoseUseCase] (ADR-011
 * §11) — explicit user action only, never on display; idempotency-keyed per tap.
 */
class CalculatorsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val logDose: LogAdministeredDoseUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
        reenterTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { SeachemTheme { CalculatorsScreen(viewModel, onLogDose = ::onLogDose) } }
    }

    private fun onLogDose(request: DoseLogRequest) {
        val profileId = (viewModel.profile.value ?: MainViewModel.AquariumProfile.FRESHWATER).id
        val amount = Quantity(request.amount, request.unit)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = logDose(
                idempotencyKey = "calculator:${UUID.randomUUID()}",
                aquariumProfileId = profileId,
                productId = request.productId,
                administered = amount,
                tankVolume = Quantity(StoredDecimal.from(BigDecimal.valueOf(request.volumeLitres)), UnitCode.LITER),
                engineVersion = ENGINE_VERSION,
                userModifiedAmount = false,
                calculated = amount,
            )
            val recorded = outcome is HistoryWriteOutcome.Recorded || outcome is HistoryWriteOutcome.Duplicate
            val msg = if (recorded) R.string.dose_logged else R.string.dose_log_failed
            Toast.makeText(requireContext(), getString(msg), Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val ENGINE_VERSION = "seachem-calc-1"
    }
}
