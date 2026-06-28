package com.example.seachem_dosing.domain.history

/**
 * Pure write-path validator for the history audit log (ADR-011 Gate D mandatory adjustments).
 * Returns the list of constraint violations (empty == valid). No I/O, no Room — fully unit-tested.
 *
 * `calculated`/`concentration` and `rounding` pair-integrity are guaranteed structurally by the
 * [Quantity] / [RoundingInfo] types (a non-null value always carries its unit). This validator
 * enforces what the types cannot: conditional measure-definition ids, identifier presence, and the
 * stricter NEW_EXACT_RECORD set.
 */
object HistoryWriteValidator {

    fun validate(cmd: RecordDoseCommand): List<String> = buildList {
        requireNotBlank(cmd.idempotencyKey, "idempotencyKey")
        requireNotBlank(cmd.aquariumProfileId, "aquariumProfileId")
        requireNotBlank(cmd.sourceModuleCode, "sourceModuleCode")
        requireNotBlank(cmd.productId, "productId")
        requirePositiveTimes(cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis)

        checkMeasureDef("administered", cmd.administered.unit, cmd.administeredMeasureDefinitionId)
        checkMeasureDef("calculated", cmd.calculated?.unit, cmd.calculatedMeasureDefinitionId)

        if (cmd.precisionStatus == PrecisionStatus.NEW_EXACT_RECORD) {
            requireNotBlank(cmd.appVersion, "appVersion (NEW_EXACT_RECORD)")
            requireNotBlank(cmd.engineVersion, "engineVersion (NEW_EXACT_RECORD dose came from an engine)")
        }
    }

    fun validate(cmd: RecordParameterCommand): List<String> = buildList {
        requireNotBlank(cmd.idempotencyKey, "idempotencyKey")
        requireNotBlank(cmd.aquariumProfileId, "aquariumProfileId")
        requireNotBlank(cmd.sourceModuleCode, "sourceModuleCode")
        requirePositiveTimes(cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis)
        // A reading is measured, not engine-computed — only app_version is required for new exact.
        if (cmd.precisionStatus == PrecisionStatus.NEW_EXACT_RECORD) {
            requireNotBlank(cmd.appVersion, "appVersion (NEW_EXACT_RECORD)")
        }
    }

    fun validate(cmd: CorrectionCommand): List<String> = buildList {
        requireNotBlank(cmd.idempotencyKey, "idempotencyKey")
        requireNotBlank(cmd.aquariumProfileId, "aquariumProfileId")
        requireNotBlank(cmd.sourceModuleCode, "sourceModuleCode")
        requireNotBlank(cmd.supersedesEventId, "supersedesEventId")
        requireNotBlank(cmd.reason, "reason")
        requirePositiveTimes(cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis)
        cmd.replacement?.let { addAll(validate(it)) }
    }

    fun validate(cmd: VoidCommand): List<String> = buildList {
        requireNotBlank(cmd.idempotencyKey, "idempotencyKey")
        requireNotBlank(cmd.aquariumProfileId, "aquariumProfileId")
        requireNotBlank(cmd.sourceModuleCode, "sourceModuleCode")
        requireNotBlank(cmd.voidsEventId, "voidsEventId")
        requireNotBlank(cmd.reason, "reason")
        requirePositiveTimes(cmd.occurredAtEpochMillis, cmd.createdAtEpochMillis)
    }

    /** A `requiresMeasureDefinition` unit needs a definition id; other units must not carry one. */
    private fun MutableList<String>.checkMeasureDef(label: String, unit: UnitCode?, defId: String?) {
        val needs = unit?.requiresMeasureDefinition == true
        when {
            unit == null && !defId.isNullOrBlank() ->
                add("conditional_unit_fields: $label measure-definition id without an amount")
            needs && defId.isNullOrBlank() ->
                add("conditional_unit_fields: $label $unit requires a measure-definition id")
            unit != null && !needs && !defId.isNullOrBlank() ->
                add("conditional_unit_fields: $label measure-definition id only allowed for a measure-definition unit")
        }
    }

    private fun MutableList<String>.requireNotBlank(value: String?, field: String) {
        if (value.isNullOrBlank()) add("required: $field is blank")
    }

    private fun MutableList<String>.requirePositiveTimes(occurredAt: Long, createdAt: Long) {
        if (occurredAt <= 0L) add("required: occurredAtEpochMillis must be > 0")
        if (createdAt <= 0L) add("required: createdAtEpochMillis must be > 0")
    }
}
