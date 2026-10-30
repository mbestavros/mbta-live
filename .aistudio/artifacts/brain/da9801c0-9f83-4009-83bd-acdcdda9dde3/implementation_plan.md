# Nearby Screen Redesign & Departure Consistency Hardening Plan

## Summary

This plan provides a comprehensive architectural redesign and bug fix pass for the **Nearby** screen in the MBTA Boston Transit Android application. It directly resolves the three user-reported inconsistencies:
1. **Deduplication**: Eliminating identical arrival times repeating between the next arrival and subsequent departures.
2. **Left-to-Right Chronological Flow**: Restructuring the departure layout so the primary next departure is prominent on the left, followed naturally by upcoming departures ("Then: 12m, 24m").
3. **Route-Specific Boarding Stop Resolution**: Ensuring the "Board at [Station]" label dynamically resolves to the actual station/platform serving that specific transit route rather than applying the single nearest physical bus stop across all routes.
4. **Google Play Version Bump**: Incrementing `versionCode` to `3` and updating `versionName` to `"3.0"` in `app/build.gradle.kts`.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following design decisions were confirmed via interactive clarification:
> * **Chronological Departure Flow**: Next arrival displayed prominently first (left-to-right), followed by upcoming times in natural reading order.
> * **Boarding Stop Display**: Each route card will display its route-specific boarding station and corresponding walking distance/time, preventing subway lines from claiming boarding at unrelated bus stops.
> * **Upcoming Departure Cap**: Display the primary next arrival plus up to **two strictly deduplicated** subsequent times.
> * **Version Increment**: Increment `versionCode` from `2` to `3` and set `versionName` to `"3.0"`.

---

## 1. Overview & Core Concept

* **The Problem**: In the current implementation:
  * In `NearbyRouteCard.kt`, the "Later" chips are positioned on the left while the primary next countdown is anchored on the right, inverting natural reading order.
  * In `TransitRepository.kt`, duplicate predictions returned by the MBTA API (e.g. child platforms or arrival/departure timestamps sharing the same minute) are not deduplicated, causing "Next: 4m" and "Later: 4m" to show simultaneously.
  * In `getNearbyRouteItems()`, `closestStop` is chosen based on the single stop closest to user GPS coordinates across all modes. This erroneously labels the Red Line subway with "Board at [Local Bus Stop]".
* **The Solution**: 
  * Rewrite `NearbyRouteCard.kt`'s `DirectionRow` to present:
    `[Headsign / Destination] ──> [Next Arrival Badge: "4 min"] ──> [Then: 12m, 24m] ──> [Track Live FAB]`
  * Implement strict prediction deduplication in repository domain mapping: group by unique, increasing `countdownMinutes` (`> nextMinutes`) and limit to 2 upcoming times.
  * Map predictions directly to the stop ID where the route is scheduled to arrive (`pred.stopId`), looking up the physical stop name from `rawStops` or cached Room stations so the boarding badge is 100% accurate.

---

## 2. User Experience & Visual Design

### 2.1 Card Header & Boarding Information
```
┌────────────────────────────────────────────────────────────────────────┐
│ [● RED LINE]  Ashmont / Braintree Branch         [Walk: 4 min (0.2 mi)]│
│ 📍 Board at Park Street Station • Underpass Entrance                   │
├────────────────────────────────────────────────────────────────────────┤
│ Inbound to Alewife                                                     │
│ ┌──────────────┐   ┌─────────┐ ┌─────────┐                             │
│ │  NOW (1 min) │   │ Then 6m │ │ Then 14m│               [🔔 Track]    │
│ └──────────────┘   └─────────┘ └─────────┘                             │
├────────────────────────────────────────────────────────────────────────┤
│ Outbound to Braintree                                                  │
│ ┌──────────────┐   ┌─────────┐                                         │
│ │    5 min     │   │Then 17m │                           [🔔 Track]    │
│ └──────────────┘   └─────────┘                                         │
└────────────────────────────────────────────────────────────────────────┘
```

* **Natural Reading Rhythm**:
  1. **Line & Destination**: Bold title with official line pill (e.g. Red Line `#DA291C`).
  2. **Accurate Boarding Landmark**: Clear location pin with specific station name and walk distance.
  3. **Immediate Arrival Pill**: High-contrast, large-text pill highlighting the very next train (e.g., green pulsating beacon when under 3 minutes).
  4. **Subsequent Time Badges**: Secondary subtle pills prefixed with "Then" (e.g. "Then 8m", "Then 19m").
  5. **Quick-Track Action**: Interactive bell/sensor icon for live background notification tracking.

---

## 3. Key Product Decisions & Trade-Offs

* **Decision 1: Stop Resolution via Prediction Metadata**
  * *Chosen Approach*: Associate each route card with the stop ID present in its earliest prediction (`firstPred.stopId`). Resolve the name by matching against `rawStops` or fallback to cached Room stops (`dao.searchStopsSync`).
  * *Why*: Guarantees that subway routes display subway stations (e.g. "Park Street") while bus routes display bus stops (e.g. "Mass Ave @ Newbury"), eliminating false boarding claims.
  * *Alternatives Considered*: Hardcoding stop lists per line; rejected because MBTA routes have complex branch splits and detours.

* **Decision 2: Strict Departure Deduplication Algorithm**
  * *Chosen Approach*: 
    ```kotlin
    val nextMinutes = firstPred.countdownMinutes
    val subsequentTimes = preds
      .map { it.countdownMinutes }
      .filter { it > nextMinutes }
      .distinct()
      .sorted()
      .take(2)
    ```
  * *Why*: Eliminates duplicate 4m/4m countdowns and prevents showing past or negative minutes.

* **Decision 3: Left-to-Right Departure Layout**
  * *Chosen Approach*: Place the primary next countdown chip immediately adjacent to or right under the direction headsign, followed horizontally by upcoming badges. The Track action button remains right-aligned.
  * *Why*: Aligns with western LTR reading psychology (Most Urgent -> Upcoming -> Actions).

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        MbtaApiClient API Response                      │
│    Stops: [place-pktrm (Subway), stop-1234 (Bus)]                      │
│    Predictions: Route "Red" @ place-pktrm, Route "1" @ stop-1234       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     TransitRepository Data Pipeline                    │
│   1. Group predictions by Route ID ("Red", "1")                        │
│   2. Resolve Stop: lookup pred.stopId in rawStops / Room DB            │
│   3. Direction Departures Deduplication:                               │
│      - nextMinutes = preds.first().countdownMinutes                    │
│      - upcomingMinutes = preds.map().filter(> next).distinct().take(2) │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        NearbyRouteCard (Compose)                       │
│   - Header: Route badge + Walking distance badge                       │
│   - Boarding text: "Board at ${item.nearestStop.name}" (Accurate!)     │
│   - DirectionRow: [Headsign] -> [NEXT CHIP] -> [THEN CHIPS] -> [ACTION]│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Detailed Implementation Tasks

### Task 1: Version Code Bump (`app/build.gradle.kts`)
* Increment `versionCode = 3`
* Set `versionName = "3.0"`

### Task 2: Route-to-Stop Resolution & Deduplication (`TransitRepository.kt`)
* In `getNearbyRouteItems()`:
  * For each `routeId`:
    * Identify the stops serving this route from `preds.mapNotNull { it.stopId }.distinct()`.
    * Match the stop ID against `rawStops` or query `dao.searchStopsSync` to get the actual `MbtaStop`. If multiple stops match, select the one closest to the user's coordinates.
    * For Direction 0 and Direction 1:
      * Sort predictions by `countdownMinutes`.
      * Deduplicate predictions by distinct `countdownMinutes`.
      * Take the first prediction as `nextArrival`.
      * Filter subsequent predictions to those with strictly greater countdown minutes: `.filter { it.countdownMinutes > nextArrival.countdownMinutes }.take(2)`.

### Task 3: Redesign `NearbyRouteCard.kt` Layout
* Refactor `DirectionRow` composable:
  * Stack destination headsign above a horizontal chronological timeline.
  * Render **Primary Next Arrival Badge**:
    * Large font, filled surface with route accent or live green border.
    * Displays `"Arriving now"` if `<= 1 min`, or `"${nextMinutes} min"`.
  * Render **Subsequent Arrival Pills**:
    * Rendered immediately to the right of the next arrival badge.
    * Labelled with `"Then ${m}m"`, distinct background, zero duplicates.
  * Right-align the Track Live action button with touch target `>= 48dp`.

---

## 6. Comprehensive Unit Testing Plan

### 6.1 `NearbyDeparturesConsistencyTest.kt` (New Suite)
We will add a dedicated unit test suite in `app/src/test/java/com/example/ui/components/NearbyDeparturesConsistencyTest.kt` verifying:
1. **Deduplication Test**: Given raw predictions with duplicate countdowns `[3, 3, 7, 12, 12, 18]`, verify that `nextMinutes == 3` and upcoming times are strictly `[7, 12]`, containing zero occurrences of `3` and zero duplicate `12`s.
2. **Route-Specific Stop Mapping Test**: Given Subway and Bus predictions at distinct stop IDs, verify that the Red Line card's `nearestStop` resolves to "Park Street" (subway) and Bus 1 resolves to "Mass Ave @ Newbury" (bus stop).
3. **Empty / Single Departure Edge Case**: When only one prediction exists (e.g. `[5]`), verify `nextMinutes == 5` and upcoming times list is empty (no crash or bogus "Later" chips).
4. **Immediate Arrival Labeling**: Verify that countdown of 0 or 1 minute maps to "Now" / "1 min" format with live status preserved.

---

## Verification Protocol

1. **Phase 1**: Update `app/build.gradle.kts` with `versionCode = 3`, `versionName = "3.0"`.
2. **Phase 2**: Refactor `TransitRepository.kt` stop resolution and prediction deduplication.
3. **Phase 3**: Redesign `NearbyRouteCard.kt` layout for LTR chronological clarity.
4. **Phase 4**: Add and run `NearbyDeparturesConsistencyTest.kt` via `gradle :app:testDebugUnitTest`.
5. **Phase 5**: Run `compile_applet` to confirm the full applet builds cleanly.
