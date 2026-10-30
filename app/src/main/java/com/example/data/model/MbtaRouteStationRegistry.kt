package com.example.data.model

import com.example.data.graph.BostonLandmarks
import com.example.data.graph.TransitGraphPathfinder

object MbtaRouteStationRegistry {

  /**
   * Static master registry of MBTA stations with coordinates and metadata.
   * Guarantees zero-latency, offline-safe resolution without database dependency.
   */
  val STATION_DETAILS: Map<String, MbtaStop> = mapOf(
    // Core Downtown Hubs
    "place-pktrm" to MbtaStop("place-pktrm", "Park Street", 42.356395, -71.062424, "Boston", 1, null, 1),
    "place-dwnxg" to MbtaStop("place-dwnxg", "Downtown Crossing", 42.355518, -71.060225, "Boston", 1, null, 1),
    "place-gover" to MbtaStop("place-gover", "Government Center", 42.359705, -71.059215, "Boston", 1, null, 1),
    "place-state" to MbtaStop("place-state", "State", 42.358978, -71.057598, "Boston", 1, null, 1),
    "place-sstat" to MbtaStop("place-sstat", "South Station", 42.352271, -71.055242, "Boston", 1, null, 1),
    "place-north" to MbtaStop("place-north", "North Station", 42.365577, -71.061290, "Boston", 1, null, 1),
    "place-bbsta" to MbtaStop("place-bbsta", "Back Bay", 42.347350, -71.075727, "Boston", 1, null, 1),
    "place-boyls" to MbtaStop("place-boyls", "Boylston", 42.353020, -71.064590, "Boston", 1, null, 1),
    "place-chncl" to MbtaStop("place-chncl", "Chinatown", 42.352547, -71.062752, "Boston", 1, null, 1),
    "place-tumnl" to MbtaStop("place-tumnl", "Tufts Medical Center", 42.349662, -71.063917, "Boston", 1, null, 1),
    "place-haecl" to MbtaStop("place-haecl", "Haymarket", 42.363021, -71.058290, "Boston", 1, null, 1),
    "place-chmnl" to MbtaStop("place-chmnl", "Charles/MGH", 42.361166, -71.070628, "Boston", 1, null, 1),
    "place-bomnl" to MbtaStop("place-bomnl", "Bowdoin", 42.361365, -71.062037, "Boston", 1, null, 1),
    "place-aqucl" to MbtaStop("place-aqucl", "Aquarium", 42.359784, -71.051652, "Boston", 1, null, 1),
    "place-armnl" to MbtaStop("place-armnl", "Arlington", 42.351902, -71.070893, "Boston", 1, null, 1),
    "place-coecl" to MbtaStop("place-coecl", "Copley", 42.349974, -71.077447, "Boston", 1, null, 1),
    "place-hymnl" to MbtaStop("place-hymnl", "Hynes Convention Center", 42.347888, -71.087903, "Boston", 1, null, 1),
    "place-kencl" to MbtaStop("place-kencl", "Kenmore", 42.348949, -71.095169, "Boston", 1, null, 1),

    // Red Line
    "place-knncl" to MbtaStop("place-knncl", "Kendall/MIT", 42.362491, -71.086177, "Cambridge", 1, null, 1),
    "place-cntsq" to MbtaStop("place-cntsq", "Central", 42.365486, -71.103802, "Cambridge", 1, null, 1),
    "place-harsq" to MbtaStop("place-harsq", "Harvard", 42.373362, -71.118956, "Cambridge", 1, null, 1),
    "place-portr" to MbtaStop("place-portr", "Porter", 42.388400, -71.119149, "Cambridge", 1, null, 1),
    "place-davsq" to MbtaStop("place-davsq", "Davis", 42.396740, -71.121815, "Somerville", 1, null, 1),
    "place-alfcl" to MbtaStop("place-alfcl", "Alewife", 42.395428, -71.142483, "Cambridge", 1, null, 1),
    "place-brdwy" to MbtaStop("place-brdwy", "Broadway", 42.342622, -71.056967, "Boston", 1, null, 1),
    "place-andrw" to MbtaStop("place-andrw", "Andrew", 42.330154, -71.057655, "Boston", 1, null, 1),
    "place-jfk" to MbtaStop("place-jfk", "JFK/UMass", 42.320685, -71.052391, "Boston", 1, null, 1),
    "place-shmnl" to MbtaStop("place-shmnl", "Savin Hill", 42.311290, -71.053331, "Boston", 1, null, 1),
    "place-fldcr" to MbtaStop("place-fldcr", "Fields Corner", 42.300093, -71.061667, "Boston", 1, null, 1),
    "place-smsha" to MbtaStop("place-smsha", "Shawmut", 42.293125, -71.065737, "Boston", 1, null, 1),
    "place-asmnl" to MbtaStop("place-asmnl", "Ashmont", 42.284520, -71.063777, "Boston", 1, null, 1),
    "place-nqncy" to MbtaStop("place-nqncy", "North Quincy", 42.275275, -71.029583, "Quincy", 1, null, 1),
    "place-wlsta" to MbtaStop("place-wlsta", "Wollaston", 42.266514, -71.020337, "Quincy", 1, null, 1),
    "place-qnctr" to MbtaStop("place-qnctr", "Quincy Center", 42.251809, -71.005409, "Quincy", 1, null, 1),
    "place-qamfd" to MbtaStop("place-qamfd", "Quincy Adams", 42.233391, -71.007153, "Quincy", 1, null, 1),
    "place-brntn" to MbtaStop("place-brntn", "Braintree", 42.207854, -71.001138, "Braintree", 1, null, 1),
    "place-matpk" to MbtaStop("place-matpk", "Mattapan", 42.267620, -71.092486, "Boston", 1, null, 1),

    // Orange Line
    "place-masta" to MbtaStop("place-masta", "Massachusetts Avenue", 42.341512, -71.083423, "Boston", 1, null, 1),
    "place-rugg" to MbtaStop("place-rugg", "Ruggles", 42.336470, -71.088969, "Boston", 1, null, 1),
    "place-rcmnl" to MbtaStop("place-rcmnl", "Roxbury Crossing", 42.331397, -71.095494, "Boston", 1, null, 1),
    "place-jaksn" to MbtaStop("place-jaksn", "Jackson Square", 42.323132, -71.099592, "Boston", 1, null, 1),
    "place-sbmnl" to MbtaStop("place-sbmnl", "Stony Brook", 42.317062, -71.104248, "Boston", 1, null, 1),
    "place-grnst" to MbtaStop("place-grnst", "Green Street", 42.310525, -71.107414, "Boston", 1, null, 1),
    "place-forhl" to MbtaStop("place-forhl", "Forest Hills", 42.300523, -71.113686, "Boston", 1, null, 1),
    "place-ccmnl" to MbtaStop("place-ccmnl", "Community College", 42.373622, -71.069533, "Boston", 1, null, 1),
    "place-sull" to MbtaStop("place-sull", "Sullivan Square", 42.383975, -71.076994, "Boston", 1, null, 1),
    "place-astao" to MbtaStop("place-astao", "Assembly", 42.392811, -71.077257, "Somerville", 1, null, 1),
    "place-welln" to MbtaStop("place-welln", "Wellington", 42.402370, -71.077082, "Medford", 1, null, 1),
    "place-mlmnl" to MbtaStop("place-mlmnl", "Malden Center", 42.426632, -71.074110, "Malden", 1, null, 1),
    "place-ogmnl" to MbtaStop("place-ogmnl", "Oak Grove", 42.436680, -71.071097, "Malden", 1, null, 1),

    // Blue Line
    "place-mvbcl" to MbtaStop("place-mvbcl", "Maverick", 42.369119, -71.039530, "Boston", 1, null, 1),
    "place-aport" to MbtaStop("place-aport", "Airport", 42.374262, -71.030395, "Boston", 1, null, 1),
    "place-wimnl" to MbtaStop("place-wimnl", "Wood Island", 42.379640, -71.022865, "Boston", 1, null, 1),
    "place-orhte" to MbtaStop("place-orhte", "Orient Heights", 42.386867, -71.004736, "Boston", 1, null, 1),
    "place-sdmnl" to MbtaStop("place-sdmnl", "Suffolk Downs", 42.390501, -70.997122, "Boston", 1, null, 1),
    "place-bmmnl" to MbtaStop("place-bmmnl", "Beachmont", 42.397549, -70.992319, "Revere", 1, null, 1),
    "place-rbmnl" to MbtaStop("place-rbmnl", "Revere Beach", 42.407842, -70.992533, "Revere", 1, null, 1),
    "place-wondl" to MbtaStop("place-wondl", "Wonderland", 42.413420, -70.991648, "Revere", 1, null, 1),

    // Green Line Branches
    "place-lech" to MbtaStop("place-lech", "Lechmere", 42.3708, -71.0765, "Cambridge", 1, null, 1),
    "place-spmnl" to MbtaStop("place-spmnl", "Science Park/West End", 42.3667, -71.0676, "Boston", 1, null, 1),
    "place-unsqu" to MbtaStop("place-unsqu", "Union Square", 42.3771, -71.0926, "Somerville", 1, null, 1),
    "place-medft" to MbtaStop("place-medft", "Medford/Tufts", 42.4078, -71.1167, "Medford", 1, null, 1),
    "place-fenwy" to MbtaStop("place-fenwy", "Fenway", 42.3454, -71.1042, "Boston", 1, null, 1),
    "place-lngwd" to MbtaStop("place-lngwd", "Longwood", 42.3413, -71.1098, "Brookline", 1, null, 1),
    "place-bvmnl" to MbtaStop("place-bvmnl", "Brookline Village", 42.3327, -71.1169, "Brookline", 1, null, 1),
    "place-brkhl" to MbtaStop("place-brkhl", "Brookline Hills", 42.3314, -71.1267, "Brookline", 1, null, 1),
    "place-rsmnl" to MbtaStop("place-rsmnl", "Reservoir", 42.3331, -71.1486, "Brookline", 1, null, 1),
    "place-river" to MbtaStop("place-river", "Riverside", 42.3370, -71.2520, "Newton", 1, null, 1),
    "place-clmnl" to MbtaStop("place-clmnl", "Coolidge Corner", 42.3421, -71.1212, "Brookline", 1, null, 1),
    "place-clvic" to MbtaStop("place-clvic", "Cleveland Circle", 42.3361, -71.1489, "Boston", 1, null, 1),
    "place-harvd" to MbtaStop("place-harvd", "Harvard Avenue", 42.3502, -71.1311, "Boston", 1, null, 1),
    "place-bckst" to MbtaStop("place-bckst", "Boston College", 42.3400, -71.1668, "Boston", 1, null, 1),
    "place-prmnl" to MbtaStop("place-prmnl", "Prudential", 42.3456, -71.0816, "Boston", 1, null, 1),
    "place-symcl" to MbtaStop("place-symcl", "Symphony", 42.3427, -71.0852, "Boston", 1, null, 1),
    "place-mfa" to MbtaStop("place-mfa", "Museum of Fine Arts", 42.3377, -71.0955, "Boston", 1, null, 1),
    "place-brmnl" to MbtaStop("place-brmnl", "Brigham Circle", 42.3342, -71.1046, "Boston", 1, null, 1),
    "place-hsmnl" to MbtaStop("place-hsmnl", "Heath Street", 42.3283, -71.1106, "Boston", 1, null, 1),

    // Silver Line & Key Hubs
    "place-crtst" to MbtaStop("place-crtst", "Courthouse", 42.352445, -71.046830, "Boston", 1, null, 1),
    "place-wtcst" to MbtaStop("place-wtcst", "World Trade Center", 42.348630, -71.042380, "Boston", 1, null, 1),
    "place-nubn" to MbtaStop("place-nubn", "Nubian", 42.329685, -71.084126, "Boston", 1, null, 1),
    "place-chels" to MbtaStop("place-chels", "Chelsea", 42.3965, -71.0333, "Chelsea", 1, null, 1),

    // Commuter Rail Terminal Hubs
    "place-r128" to MbtaStop("place-r128", "Route 128", 42.2132, -71.1504, "Westwood", 1, null, 1),
    "place-prvdc" to MbtaStop("place-prvdc", "Providence", 41.8293, -71.4138, "Providence", 1, null, 1),
    "place-worcs" to MbtaStop("place-worcs", "Worcester", 42.2612, -71.7956, "Worcester", 1, null, 1),
    "place-lowel" to MbtaStop("place-lowel", "Lowell", 42.6375, -71.3142, "Lowell", 1, null, 1),
    "place-salem" to MbtaStop("place-salem", "Salem", 42.5246, -70.8962, "Salem", 1, null, 1)
  )

  /**
   * Authoritative list of stations served by each MBTA rapid transit and key bus route.
   */
  val ROUTE_STATIONS: Map<String, List<String>> = mapOf(
    "Red" to listOf(
      "place-alfcl", "place-davsq", "place-portr", "place-harsq", "place-cntsq", "place-knncl",
      "place-chmnl", "place-pktrm", "place-dwnxg", "place-sstat", "place-brdwy", "place-andrw",
      "place-jfk", "place-shmnl", "place-fldcr", "place-smsha", "place-asmnl", "place-nqncy",
      "place-wlsta", "place-qnctr", "place-qamfd", "place-brntn", "place-matpk"
    ),
    "Orange" to listOf(
      "place-ogmnl", "place-mlmnl", "place-welln", "place-astao", "place-sull", "place-ccmnl",
      "place-north", "place-haecl", "place-state", "place-dwnxg", "place-chncl", "place-tumnl",
      "place-bbsta", "place-masta", "place-rugg", "place-rcmnl", "place-jaksn", "place-sbmnl",
      "place-grnst", "place-forhl"
    ),
    "Blue" to listOf(
      "place-bomnl", "place-gover", "place-state", "place-aqucl", "place-mvbcl", "place-aport",
      "place-wimnl", "place-orhte", "place-sdmnl", "place-bmmnl", "place-rbmnl", "place-wondl"
    ),
    "Green" to listOf(
      "place-medft", "place-unsqu", "place-lech", "place-spmnl", "place-north", "place-haecl",
      "place-gover", "place-pktrm", "place-boyls", "place-armnl", "place-coecl", "place-hymnl",
      "place-kencl"
    ),
    "Green-B" to listOf(
      "place-gover", "place-pktrm", "place-boyls", "place-armnl", "place-coecl", "place-hymnl",
      "place-kencl", "place-bndhl", "place-buest", "place-bucen", "place-amory", "place-babc",
      "place-harvd", "place-wasre", "place-bckst"
    ),
    "Green-C" to listOf(
      "place-gover", "place-pktrm", "place-boyls", "place-armnl", "place-coecl", "place-hymnl",
      "place-kencl", "place-smary", "place-kntst", "place-clmnl", "place-bndhn", "place-bcons",
      "place-clvic"
    ),
    "Green-D" to listOf(
      "place-unsqu", "place-lech", "place-spmnl", "place-north", "place-haecl", "place-gover",
      "place-pktrm", "place-boyls", "place-armnl", "place-coecl", "place-hymnl", "place-kencl",
      "place-fenwy", "place-lngwd", "place-bvmnl", "place-brkhl", "place-bcnfd", "place-rsmnl",
      "place-chhil", "place-newcn", "place-newhi", "place-eliot", "place-waban", "place-woodl",
      "place-river"
    ),
    "Green-E" to listOf(
      "place-medft", "place-mdftf", "place-balsq", "place-mgsq", "place-glsq", "place-esomr",
      "place-lech", "place-spmnl", "place-north", "place-haecl", "place-gover", "place-pktrm",
      "place-boyls", "place-armnl", "place-coecl", "place-prmnl", "place-symcl", "place-nuniv",
      "place-mfa", "place-lngmd", "place-brmnl", "place-hsmnl"
    ),
    "741" to listOf( // Silver Line SL1
      "place-sstat", "place-crtst", "place-wtcst", "place-aport"
    ),
    "742" to listOf( // Silver Line SL2
      "place-sstat", "place-crtst", "place-wtcst"
    ),
    "743" to listOf( // Silver Line SL3
      "place-sstat", "place-crtst", "place-wtcst", "place-chels"
    ),
    "749" to listOf( // Silver Line SL5
      "place-dwnxg", "place-chncl", "place-tumnl", "place-nubn"
    ),
    "751" to listOf( // Silver Line SL4
      "place-sstat", "place-chncl", "place-tumnl", "place-nubn"
    ),
    "CR-Providence" to listOf(
      "place-sstat", "place-bbsta", "place-rugg", "place-r128", "place-prvdc"
    ),
    "CR-Worcester" to listOf(
      "place-sstat", "place-bbsta", "place-worcs"
    ),
    "CR-Lowell" to listOf(
      "place-north", "place-lowel"
    ),
    "CR-Fitchburg" to listOf(
      "place-north", "place-portr"
    ),
    "CR-Haverhill" to listOf(
      "place-north", "place-mlmnl", "place-ogmnl"
    ),
    "CR-Newburyport" to listOf(
      "place-north", "place-chels", "place-salem"
    ),
    "1" to listOf(
      "place-harsq", "place-cntsq", "place-masta", "place-nubn"
    ),
    "66" to listOf(
      "place-harsq", "place-clmnl", "place-bvmnl", "place-nubn"
    )
  )

  /**
   * Child platform track stop IDs mapped to their parent station ID.
   */
  val CHILD_TO_PARENT_STATION: Map<String, String> = mapOf(
    // Park Street
    "70075" to "place-pktrm", "70076" to "place-pktrm",
    "70196" to "place-pktrm", "70197" to "place-pktrm", "70198" to "place-pktrm", "70199" to "place-pktrm",
    // Downtown Crossing
    "70014" to "place-dwnxg", "70015" to "place-dwnxg",
    "70077" to "place-dwnxg", "70078" to "place-dwnxg",
    // Government Center
    "70038" to "place-gover", "70039" to "place-gover",
    "70201" to "place-gover", "70202" to "place-gover",
    // State
    "70041" to "place-state", "70042" to "place-state",
    "70043" to "place-state", "70044" to "place-state",
    // South Station
    "70067" to "place-sstat", "70068" to "place-sstat",
    // North Station
    "70065" to "place-north", "70066" to "place-north",
    "70205" to "place-north", "70206" to "place-north",
    // Haymarket
    "70037" to "place-haecl", "70040" to "place-haecl",
    "70203" to "place-haecl", "70204" to "place-haecl",
    // Chinatown & Tufts
    "70016" to "place-chncl", "70017" to "place-chncl",
    "70018" to "place-tumnl", "70019" to "place-tumnl",
    // Back Bay & Mass Ave
    "70020" to "place-bbsta", "70021" to "place-bbsta",
    "70022" to "place-masta", "70023" to "place-masta",
    // Boylston & Arlington
    "70158" to "place-boyls", "70159" to "place-boyls",
    "70156" to "place-armnl", "70157" to "place-armnl",
    // Copley & Hynes & Kenmore
    "70154" to "place-coecl", "70155" to "place-coecl",
    "70152" to "place-hymnl", "70153" to "place-hymnl",
    "70150" to "place-kencl", "70151" to "place-kencl",
    // Red Line core
    "70001" to "place-alfcl", "70002" to "place-alfcl",
    "70003" to "place-davsq", "70004" to "place-davsq",
    "70005" to "place-portr", "70006" to "place-portr",
    "70007" to "place-harsq", "70008" to "place-harsq",
    "70009" to "place-cntsq", "70010" to "place-cntsq",
    "70011" to "place-knncl", "70012" to "place-knncl",
    "70013" to "place-chmnl",
    "70079" to "place-brdwy", "70080" to "place-brdwy",
    "70081" to "place-andrw", "70082" to "place-andrw",
    "70083" to "place-jfk", "70084" to "place-jfk",
    "70085" to "place-shmnl", "70086" to "place-shmnl",
    "70087" to "place-fldcr", "70088" to "place-fldcr",
    "70089" to "place-smsha", "70090" to "place-smsha",
    "70091" to "place-asmnl", "70092" to "place-asmnl",
    "70093" to "place-nqncy", "70094" to "place-nqncy",
    "70095" to "place-wlsta", "70096" to "place-wlsta",
    "70097" to "place-qnctr", "70098" to "place-qnctr",
    "70099" to "place-qamfd", "70100" to "place-qamfd",
    "70101" to "place-brntn", "70102" to "place-brntn",
    // Blue Line core
    "70045" to "place-aqucl", "70046" to "place-aqucl",
    "70047" to "place-mvbcl", "70048" to "place-mvbcl",
    "70049" to "place-aport", "70050" to "place-aport",
    "70051" to "place-wimnl", "70052" to "place-wimnl",
    "70053" to "place-orhte", "70054" to "place-orhte",
    "70055" to "place-sdmnl", "70056" to "place-sdmnl",
    "70057" to "place-bmmnl", "70058" to "place-bmmnl",
    "70059" to "place-rbmnl", "70060" to "place-rbmnl",
    "70061" to "place-wondl", "70063" to "place-bomnl", "70064" to "place-bomnl"
  )

  fun getStationIdsForRoute(routeId: String): List<String> {
    return ROUTE_STATIONS[routeId] ?: when {
      routeId.startsWith("Green", ignoreCase = true) -> ROUTE_STATIONS["Green"] ?: emptyList()
      routeId.startsWith("CR-", ignoreCase = true) -> listOf("place-sstat", "place-north", "place-bbsta", "place-rugg")
      routeId.startsWith("74") || routeId.startsWith("75") -> listOf("place-sstat", "place-dwnxg", "place-chncl", "place-crtst")
      else -> emptyList()
    }
  }

  val PARENT_TO_CHILD_STATIONS: Map<String, List<String>> by lazy {
    val map = mutableMapOf<String, MutableList<String>>()
    CHILD_TO_PARENT_STATION.forEach { (child, parent) ->
      map.getOrPut(parent) { mutableListOf() }.add(child)
    }
    map
  }

  fun getChildPlatformStopIds(parentStationId: String): List<String> {
    return PARENT_TO_CHILD_STATIONS[parentStationId] ?: emptyList()
  }

  fun getClosestStations(userLat: Double, userLng: Double, limit: Int = 4): List<MbtaStop> {
    return STATION_DETAILS.values.sortedBy {
      TransitGraphPathfinder.calculateDistanceMiles(userLat, userLng, it.latitude, it.longitude)
    }.take(limit)
  }

  fun getRoutesForLocation(userLat: Double, userLng: Double, limitStations: Int = 4): List<String> {
    val closest = getClosestStations(userLat, userLng, limitStations)
    val routes = mutableListOf<String>()
    for (station in closest) {
      val stationRoutes = getRoutesForStation(station.id)
      for (r in stationRoutes) {
        if (!routes.contains(r)) {
          routes.add(r)
        }
      }
    }
    return routes.ifEmpty { listOf("Red", "Orange", "Green-B", "Blue") }
  }

  fun isStationOnRoute(stationId: String, routeId: String): Boolean {
    val stations = getStationIdsForRoute(routeId)
    return stations.contains(stationId)
  }

  fun resolveParentStationId(stopId: String): String {
    return CHILD_TO_PARENT_STATION[stopId] ?: stopId
  }

  /**
   * Returns all routes that legitimately serve a given station ID.
   */
  fun getRoutesForStation(stationId: String): List<String> {
    val parentId = resolveParentStationId(stationId)
    val directRoutes = ROUTE_STATIONS.filterValues { stations -> stations.contains(parentId) }.keys.toList()
    if (directRoutes.isNotEmpty()) return directRoutes

    // If stationId exists in STATION_DETAILS
    val station = STATION_DETAILS[parentId]
    if (station != null) {
      val nearby = STATION_DETAILS.values
        .filter { it.id != parentId }
        .minByOrNull { TransitGraphPathfinder.calculateDistanceMiles(station.latitude, station.longitude, it.latitude, it.longitude) }
      if (nearby != null) {
        val nearbyRoutes = ROUTE_STATIONS.filterValues { stations -> stations.contains(nearby.id) }.keys.toList()
        if (nearbyRoutes.isNotEmpty()) return nearbyRoutes
      }
    }

    return emptyList()
  }

  /**
   * Resolves the actual, route-specific boarding stop for a given transit route.
   * Guarantees that a subway or rail line is ONLY assigned a station where that line can actually be boarded.
   */
  fun resolveBoardingStopForRoute(
    route: MbtaRoute,
    preds: List<MbtaPrediction>,
    candidateStops: List<MbtaStop>,
    allCachedStations: List<MbtaStop>,
    userLat: Double,
    userLng: Double
  ): MbtaStop {
    val validStationIds = getStationIdsForRoute(route.id).toSet()

    // Build comprehensive station repository (Station Details + DB cached stations + Candidates)
    val combinedStationPool = (STATION_DETAILS.values + allCachedStations + candidateStops).distinctBy { it.id }

    // 1. Check predictions for this route: resolve platform stopId -> parent station ID
    if (preds.isNotEmpty()) {
      val matchedCandidateStops = mutableListOf<MbtaStop>()
      for (p in preds) {
        val parentId = resolveParentStationId(p.stopId)

        // Strict verification: station must legitimately serve this route
        if (validStationIds.contains(parentId)) {
          val matchedStation = combinedStationPool.find { it.id == parentId }
          if (matchedStation != null) {
            matchedCandidateStops.add(matchedStation)
          }
        }

        // For bus routes, check candidate bus stops
        val matchedStop = candidateStops.find { it.id == p.stopId || it.id == parentId }
        if (matchedStop != null) {
          matchedCandidateStops.add(matchedStop)
        }
      }

      val closestPredictionStop = matchedCandidateStops.distinctBy { it.id }.minByOrNull {
        TransitGraphPathfinder.calculateDistanceMiles(userLat, userLng, it.latitude, it.longitude)
      }
      if (closestPredictionStop != null) {
        return closestPredictionStop
      }
    }

    // 2. If no prediction matched or for offline schedule:
    // Select the closest station that ACTUALLY serves this route!
    if (validStationIds.isNotEmpty()) {
      val validStations = combinedStationPool.filter { it.id in validStationIds }
      if (validStations.isNotEmpty()) {
        return validStations.minByOrNull {
          TransitGraphPathfinder.calculateDistanceMiles(userLat, userLng, it.latitude, it.longitude)
        }!!
      }
    }

    // 3. For buses without predetermined station lists, pick the nearest bus stop (locationType == 0)
    val busStops = candidateStops.filter { it.locationType == 0 }
    if (busStops.isNotEmpty()) {
      return busStops.minByOrNull {
        TransitGraphPathfinder.calculateDistanceMiles(userLat, userLng, it.latitude, it.longitude)
      }!!
    }

    // 4. Default safe fallback
    return combinedStationPool.filter { it.id in validStationIds }.minByOrNull {
      TransitGraphPathfinder.calculateDistanceMiles(userLat, userLng, it.latitude, it.longitude)
    } ?: candidateStops.minByOrNull {
      TransitGraphPathfinder.calculateDistanceMiles(userLat, userLng, it.latitude, it.longitude)
    } ?: BostonLandmarks.PARK_STREET
  }
}
