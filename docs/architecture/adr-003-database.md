# ADR-003: Local Persistence — Room

**Status:** Accepted
**Date:** 2026-05-09
**Deciders:** Gurucharan.S (delegated decision to Claude)
**Decision:** C2 (Room)

**Current implementation note (2026-06-29):** the Room decision remains current, but the
v1 schema sketch below is historical. The committed app now uses the ADR-011 v2
append-only event schema (`history_event`, `dose_event_detail`,
`parameter_event_detail`) plus non-destructive `Migration(1,2)`.

## Context

Current persistence: only `SavedStateHandle` (process-death survival of single ViewModel) + `SharedPreferences` for selected profile (`ProfileSelectionFragment.kt:88-97`) + `DataStore Preferences 1.0.0` already in classpath but unused.

Phase 7 of v2.0 plan adds:
- **Parameter history** — time-series of pH/KH/GH/NH₃/NO₂/NO₃ over time, with charts (per Phase 7.1 of project prompt).
- **Dosing logs** — record of every dose computed/administered with timestamp, product, amount, volume snapshot.
- **Multi-aquarium support** (potential v2.1) — multiple `AquariumProfile` instances each with their own history.

These are time-series queries (`SELECT * FROM parameter_log WHERE profile_id = ? AND timestamp BETWEEN ? AND ? ORDER BY timestamp`). Charts need date-range aggregation. Dosing logs need product-grouped recent-N queries.

## Decision

Adopt **Room** with KSP code generation.

## Options Considered

### Option B — DataStore + in-memory
Store current state in DataStore Preferences, no history persisted, charts show only session data.

| Dimension | Score (1-5) |
|---|---|
| Query power | 1 (key-value only) |
| Migration tooling | 4 (DataStore migrations exist) |
| Offline support | 5 |
| Sync capability | 1 |

**Verdict:** Kills history feature. Self-contradicts Phase 7.1.

### Option C1 — ObjectBox
NoSQL embedded DB, fast, sync-ready (paid).

| Dimension | Score (1-5) |
|---|---|
| Query power | 4 (ObjectBox query DSL) |
| Migration tooling | 3 |
| Offline support | 5 |
| Sync capability | 5 (ObjectBox Sync, paid tier) |

**Pros:** Faster than Room for large datasets. NoSQL flexibility.
**Cons:** Niche; smaller community vs Room. Requires native libraries (~3MB APK bloat). Paid for sync. We have no sync requirement and our datasets are small (years of dosing logs ≈ MB max).

### Option C2 — Room (CHOSEN)
AndroidX SQL ORM, KSP-compiled DAOs.

| Dimension | Score (1-5) |
|---|---|
| Query power | 5 (full SQL + Flow) |
| Migration tooling | 5 (auto-migrations, schema export) |
| Offline support | 5 |
| Sync capability | 3 (manual via Repository) |

**Pros:** AndroidX standard. Compile-time SQL verification (`@Query` parsed at build). Native `Flow` integration → matches StateFlow stack. KSP faster than KAPT. Migration tooling stable. No native libs (smaller APK than ObjectBox). Time-series queries trivial in SQL.
**Cons:** SQL boilerplate vs ObjectBox query builder. Migration writing requires care.

## Trade-off Analysis

ObjectBox advantages (sync, raw speed) don't apply: no sync requirement stated, datasets are MB-scale max. Room's compile-time SQL check + native Flow + AndroidX backing + smaller APK win.

## Historical Schema Sketch (Superseded By ADR-011)

```kotlin
@Entity(tableName = "parameter_log")
data class ParameterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,                 // freshwater | saltwater | pond
    val timestamp: Long,                   // epoch millis
    val ammonia: Double?,
    val nitrite: Double?,
    val nitrate: Double?,
    val gh: Double?,
    val kh: Double?,
    val ph: Double?,
    val temperature: Double?,
    val salinity: Double?,
    val alkalinity: Double?,
    val calcium: Double?,
    val magnesium: Double?,
    val phosphate: Double?,
    val dissolvedOxygen: Double?,
    val potassium: Double?,
    val iron: Double?,
    val strontium: Double?,
    val iodide: Double?,
    val volumeLitres: Double
)

@Entity(tableName = "dosing_log")
data class DosingLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: String,
    val timestamp: Long,
    val product: String,                   // Product enum name
    val amount: Double,
    val unit: String,                      // g | mL | tsp | caps
    val volumeLitresAtDose: Double,
    val notes: String?
)

@Dao
interface ParameterLogDao {
    @Insert suspend fun insert(log: ParameterLogEntity): Long
    @Query("SELECT * FROM parameter_log WHERE profileId = :profile AND timestamp >= :since ORDER BY timestamp")
    fun observeSince(profile: String, since: Long): Flow<List<ParameterLogEntity>>
}
```

At ADR creation this sketch assumed DB version 1. ADR-011 supersedes that sketch:
the committed database is version 2 and has a required migration from v1.

## Consequences

**Easier:**
- Time-series queries with `ORDER BY timestamp` + `BETWEEN` are one-liners.
- Charts can subscribe to `Flow<List<ParameterLogEntity>>` and update reactively.
- Repository pattern (per ADR-002) cleanly wraps DAOs.

**Harder:**
- First migration (when adding columns) needs careful manual SQL or auto-migration setup.
- Schema export to `app/schemas/` adds a build directory; must commit for migration safety.

**Revisit if:**
- Dataset size exceeds 100MB (suggests need for archive/rotation).
- Multi-device sync becomes a requirement (then evaluate Firebase Firestore or ObjectBox Sync).

## Action Items

1. Add to `gradle/libs.versions.toml`:
   - `androidx.room:room-runtime:2.6.1`
   - `androidx.room:room-ktx:2.6.1`
   - `androidx.room:room-compiler:2.6.1` (ksp)
2. Add KSP plugin: `com.google.devtools.ksp:2.0.21-1.0.27`.
3. Create `data/local/database/AppDatabase.kt` with version 1. Superseded by ADR-011 v2.
4. Configure schema export: `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.
5. Add `@Database(entities = [...], version = 1, exportSchema = true)` annotation.
6. Wire Room into Koin via `dataModule`: `single { Room.databaseBuilder(...).build() }`.
7. Add `app/schemas/` to git tracking.

## Rollback Criteria

Revert to ObjectBox if:
- Room migration painfulness blocks shipping a v2.x release.
- Query performance on parameter_log >10k rows degrades below 100ms p95 even with indexes.
- Schema export causes merge conflicts in 3+ PRs in succession.
