package com.example.seachem_dosing.domain.history

import com.example.seachem_dosing.core.numerics.StoredDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ADR-011 Gate D mandatory adjustments — pure write-path validation. */
class HistoryWriteValidatorTest {

    private fun q(v: String, u: UnitCode) = Quantity(StoredDecimal.parseNewValue(v), u)

    private fun validDose(
        administeredUnit: UnitCode = UnitCode.MILLILITER,
        administeredMeasureDefId: String? = null,
        calculated: Quantity? = null,
        calculatedMeasureDefId: String? = null,
        precision: PrecisionStatus = PrecisionStatus.NEW_EXACT_RECORD,
        app: String? = "1.0",
        engine: String? = "seachem-engine-1",
    ) = RecordDoseCommand(
        idempotencyKey = "k1", aquariumProfileId = "p1", sourceModuleCode = "calculator",
        occurredAtEpochMillis = 1000, createdAtEpochMillis = 1000, productId = "prime",
        administered = q("5.0", administeredUnit), tankVolume = q("100", UnitCode.LITER),
        userModifiedAmount = false, precisionStatus = precision, appVersion = app, engineVersion = engine,
        administeredMeasureDefinitionId = administeredMeasureDefId,
        calculated = calculated, calculatedMeasureDefinitionId = calculatedMeasureDefId,
    )

    private fun assertValid(v: List<String>) = assertTrue("expected valid, got $v", v.isEmpty())
    private fun assertViolates(v: List<String>, needle: String) =
        assertTrue("expected a '$needle' violation in $v", v.any { it.contains(needle) })

    @Test fun validDose_passes() = assertValid(HistoryWriteValidator.validate(validDose()))

    @Test fun dose_blankRequiredFields_violate() {
        assertViolates(HistoryWriteValidator.validate(validDose().copy(productId = "")), "productId")
        assertViolates(HistoryWriteValidator.validate(validDose().copy(aquariumProfileId = " ")), "aquariumProfileId")
        assertViolates(HistoryWriteValidator.validate(validDose().copy(idempotencyKey = "")), "idempotencyKey")
    }

    @Test fun dose_zeroTimestamps_violate() {
        assertViolates(HistoryWriteValidator.validate(validDose().copy(occurredAtEpochMillis = 0)), "occurredAt")
        assertViolates(HistoryWriteValidator.validate(validDose().copy(createdAtEpochMillis = -1)), "createdAt")
    }

    // conditional measure-definition fields
    @Test fun measureDefinitionUnit_requiresDefinitionId() {
        assertViolates(HistoryWriteValidator.validate(validDose(UnitCode.MANUFACTURER_SCOOP)), "requires a measure-definition id")
        assertValid(HistoryWriteValidator.validate(validDose(UnitCode.MANUFACTURER_SCOOP, administeredMeasureDefId = "scoop-A")))
    }

    @Test fun plainUnit_mustNotCarryMeasureDefinitionId() {
        assertViolates(
            HistoryWriteValidator.validate(validDose(UnitCode.MILLILITER, administeredMeasureDefId = "x")),
            "only allowed for a measure-definition unit",
        )
    }

    @Test fun calculatedMeasureDefinitionUnit_requiresDefinitionId() {
        val missing = validDose(calculated = q("3.0", UnitCode.MANUFACTURER_SCOOP))
        assertViolates(HistoryWriteValidator.validate(missing), "calculated MANUFACTURER_SCOOP requires a measure-definition id")
        val ok = validDose(calculated = q("3.0", UnitCode.MANUFACTURER_SCOOP), calculatedMeasureDefId = "scoop-A")
        assertValid(HistoryWriteValidator.validate(ok))
    }

    @Test fun calculatedMeasureDefinitionId_withoutCalculatedAmount_violates() {
        assertViolates(
            HistoryWriteValidator.validate(validDose(calculatedMeasureDefId = "x")),
            "calculated measure-definition id without an amount",
        )
    }

    // legacy_nullability
    @Test fun newExactDose_requiresAppAndEngineVersion() {
        assertViolates(HistoryWriteValidator.validate(validDose(app = null)), "appVersion")
        assertViolates(HistoryWriteValidator.validate(validDose(engine = " ")), "engineVersion")
    }

    @Test fun legacyDose_relaxesVersionRequirements() {
        val legacy = validDose(precision = PrecisionStatus.LEGACY_BINARY64_APPROXIMATION, app = null, engine = null)
        assertValid(HistoryWriteValidator.validate(legacy))
    }

    // parameters
    private fun validParam(precision: PrecisionStatus = PrecisionStatus.NEW_EXACT_RECORD, app: String? = "1.0") =
        RecordParameterCommand(
            idempotencyKey = "pk", aquariumProfileId = "p1", sourceModuleCode = "dashboard",
            occurredAtEpochMillis = 1000, createdAtEpochMillis = 1000,
            parameterType = ParameterType.NITRATE, measured = q("15", UnitCode.PPM_MG_PER_L),
            validationStatus = ParameterValidationStatus.VALIDATED, precisionStatus = precision, appVersion = app,
        )

    @Test fun validParameter_passes() = assertValid(HistoryWriteValidator.validate(validParam()))

    @Test fun newExactParameter_requiresAppVersion_butNotEngine() {
        assertViolates(HistoryWriteValidator.validate(validParam(app = null)), "appVersion")
        assertValid(HistoryWriteValidator.validate(validParam(precision = PrecisionStatus.LEGACY_BINARY64_APPROXIMATION, app = null)))
    }

    // correction / void
    @Test fun validCorrection_passes() {
        val c = CorrectionCommand("ck", "p1", "history", 1000, 1000, supersedesEventId = "evt-1", reason = "wrong dose")
        assertValid(HistoryWriteValidator.validate(c))
    }

    @Test fun correction_blankReasonOrTarget_violate() {
        assertViolates(HistoryWriteValidator.validate(CorrectionCommand("ck", "p1", "history", 1000, 1000, "", "r")), "supersedesEventId")
        assertViolates(HistoryWriteValidator.validate(CorrectionCommand("ck", "p1", "history", 1000, 1000, "evt-1", "")), "reason")
    }

    @Test fun correction_withInvalidReplacement_propagatesViolations() {
        val c = CorrectionCommand("ck", "p1", "history", 1000, 1000, "evt-1", "fix", replacement = validDose().copy(productId = ""))
        assertViolates(HistoryWriteValidator.validate(c), "productId")
    }

    @Test fun validVoid_passes() =
        assertValid(HistoryWriteValidator.validate(VoidCommand("vk", "p1", "history", 1000, 1000, voidsEventId = "evt-1", reason = "duplicate")))

    @Test fun void_blankFields_violate() {
        assertViolates(HistoryWriteValidator.validate(VoidCommand("vk", "p1", "history", 1000, 1000, "", "r")), "voidsEventId")
        assertEquals(emptyList<String>(), HistoryWriteValidator.validate(VoidCommand("vk", "p1", "history", 1000, 1000, "evt-1", "r")))
    }
}
