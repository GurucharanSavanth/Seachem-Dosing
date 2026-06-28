package com.example.seachem_dosing.domain.history

import com.example.seachem_dosing.core.numerics.StoredDecimal

/**
 * Typed write commands for the history audit log (ADR-011 §1, Gate D). Using [StoredDecimal] +
 * registry enums (not raw String) is the decimal/unit validation boundary: a malformed decimal or
 * unknown unit cannot be constructed, so it can never reach the DAO. Cross-field rules (conditional
 * measure-definition ids, pair integrity) are enforced by [HistoryWriteValidator].
 *
 * Separate command types give structural event-detail integrity: a dose command can only produce a
 * dose detail, a parameter command only a parameter detail (never UI-trusted).
 */

/** A measured quantity: a validated value plus its explicit unit (a unit is never inferred). */
data class Quantity(val value: StoredDecimal, val unit: UnitCode)

/** Rounding provenance for a computed amount (both or neither — pair integrity via the type). */
data class RoundingInfo(val modeCode: String, val scale: Int)

/**
 * Record an actually-administered dose (never auto-created from a displayed calculation). When an
 * amount's unit `requiresMeasureDefinition` (scoop/capful/calibrated spoon), the matching
 * `*MeasureDefinitionId` must reference an immutable measure definition.
 */
data class RecordDoseCommand(
    val idempotencyKey: String,
    val aquariumProfileId: String,
    val sourceModuleCode: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val productId: String,
    val administered: Quantity,
    val tankVolume: Quantity,
    val userModifiedAmount: Boolean,
    val precisionStatus: PrecisionStatus = PrecisionStatus.NEW_EXACT_RECORD,
    // optional / conditionally required
    val productVariantId: String? = null,
    val calculated: Quantity? = null,
    val calculatedMeasureDefinitionId: String? = null,
    val administeredMeasureDefinitionId: String? = null,
    val concentration: Quantity? = null,
    val route: AdministrationRoute? = null,
    val rounding: RoundingInfo? = null,
    val formulaRuleId: String? = null,
    val evidenceSourceId: String? = null,
    val warningsAcknowledged: String? = null,
    val appVersion: String? = null,
    val engineVersion: String? = null,
    val notes: String? = null,
)

/** Record a measured water-parameter reading. [tankVolume] is optional for new readings. */
data class RecordParameterCommand(
    val idempotencyKey: String,
    val aquariumProfileId: String,
    val sourceModuleCode: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val parameterType: ParameterType,
    val measured: Quantity,
    val validationStatus: ParameterValidationStatus,
    val precisionStatus: PrecisionStatus = PrecisionStatus.NEW_EXACT_RECORD,
    val tankVolume: Quantity? = null,
    val testMethod: String? = null,
    val sourceDeviceOrKit: String? = null,
    val appVersion: String? = null,
    val notes: String? = null,
)

/** Append a correction referencing an existing event (never mutates the original — ADR-011 §7). */
data class CorrectionCommand(
    val idempotencyKey: String,
    val aquariumProfileId: String,
    val sourceModuleCode: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val supersedesEventId: String,
    val reason: String,
    val replacement: RecordDoseCommand? = null,
    val notes: String? = null,
)

/** Append a void/retraction referencing an existing event (append-only — ADR-011 §7). */
data class VoidCommand(
    val idempotencyKey: String,
    val aquariumProfileId: String,
    val sourceModuleCode: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val voidsEventId: String,
    val reason: String,
    val notes: String? = null,
)
