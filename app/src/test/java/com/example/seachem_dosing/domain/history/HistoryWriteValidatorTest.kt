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
        calibration: Calibration? = null,
        precision: PrecisionStatus = PrecisionStatus.NEW_EXACT_RECORD,
        app: String? = "1.0",
        engine: String? = "seachem-engine-1",
    ) = RecordDoseCommand(
        idempotencyKey = "k1", aquariumProfileId = "p1", sourceModuleCode = "calculator",
        occurredAtEpochMillis = 1000, createdAtEpochMillis = 1000, productId = "prime",
        administered = q("5.0", administeredUnit), tankVolume = q("100", UnitCode.LITER),
        userModifiedAmount = false, precisionStatus = precision, appVersion = app, engineVersion = engine,
        administeredCalibration = calibration,
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

    // conditional_unit_fields
    @Test fun scoopUnit_requiresScoopDefinitionId() {
        assertViolates(HistoryWriteValidator.validate(validDose(UnitCode.MANUFACTURER_SCOOP)), "scoopDefinitionId")
        assertValid(HistoryWriteValidator.validate(validDose(UnitCode.MANUFACTURER_SCOOP, Calibration(scoopDefinitionId = "scoop-A"))))
    }

    @Test fun scoopUnit_mustNotCarryCalibratedVolume() {
        val bad = validDose(UnitCode.MANUFACTURER_SCOOP, Calibration(scoopDefinitionId = "s", calibratedVolume = q("1.0", UnitCode.MILLILITER)))
        assertViolates(HistoryWriteValidator.validate(bad), "must not carry a calibratedVolume")
    }

    @Test fun calibratedSpoon_requiresCalibratedVolume() {
        assertViolates(HistoryWriteValidator.validate(validDose(UnitCode.USER_CALIBRATED_SPOON)), "requires administeredCalibration.calibratedVolume")
        assertValid(HistoryWriteValidator.validate(validDose(UnitCode.USER_CALIBRATED_SPOON, Calibration(calibratedVolume = q("4.93", UnitCode.MILLILITER)))))
    }

    @Test fun calibratedSpoon_mustNotCarryScoopId() {
        val bad = validDose(UnitCode.USER_CALIBRATED_SPOON, Calibration(scoopDefinitionId = "s", calibratedVolume = q("4.93", UnitCode.MILLILITER)))
        assertViolates(HistoryWriteValidator.validate(bad), "must not carry a scoopDefinitionId")
    }

    @Test fun plainUnit_mustNotCarryCalibration() {
        val bad = validDose(UnitCode.MILLILITER, Calibration(scoopDefinitionId = "s"))
        assertViolates(HistoryWriteValidator.validate(bad), "only allowed for scoop/calibrated-spoon")
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
