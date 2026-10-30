package com.example.data.graph

import com.example.data.model.MbtaStop

data class LandmarkPreset(
  val name: String,
  val subtitle: String,
  val latitude: Double,
  val longitude: Double,
  val nearestStopId: String,
  val category: String = "Popular"
)

object BostonLandmarks {
  val PARK_STREET = MbtaStop(
    id = "place-pktrm",
    name = "Park Street",
    latitude = 42.356395,
    longitude = -71.062424,
    municipality = "Boston"
  )

  val DOWNTOWN_CROSSING = MbtaStop(
    id = "place-dwnxg",
    name = "Downtown Crossing",
    latitude = 42.355518,
    longitude = -71.060225,
    municipality = "Boston"
  )

  val SOUTH_STATION = MbtaStop(
    id = "place-sstat",
    name = "South Station",
    latitude = 42.352271,
    longitude = -71.055242,
    municipality = "Boston"
  )

  val NORTH_STATION = MbtaStop(
    id = "place-north",
    name = "North Station",
    latitude = 42.365577,
    longitude = -71.061290,
    municipality = "Boston"
  )

  val GOVERNMENT_CENTER = MbtaStop(
    id = "place-gover",
    name = "Government Center",
    latitude = 42.359705,
    longitude = -71.059215,
    municipality = "Boston"
  )

  val STATE = MbtaStop(
    id = "place-state",
    name = "State",
    latitude = 42.358978,
    longitude = -71.057598,
    municipality = "Boston"
  )

  val HARVARD = MbtaStop(
    id = "place-harsq",
    name = "Harvard",
    latitude = 42.373362,
    longitude = -71.118956,
    municipality = "Cambridge"
  )

  val KENDALL_MIT = MbtaStop(
    id = "place-knncl",
    name = "Kendall/MIT",
    latitude = 42.362491,
    longitude = -71.086177,
    municipality = "Cambridge"
  )

  val BACK_BAY = MbtaStop(
    id = "place-bbsta",
    name = "Back Bay",
    latitude = 42.350310,
    longitude = -71.075974,
    municipality = "Boston"
  )

  val COPLEY = MbtaStop(
    id = "place-coecl",
    name = "Copley",
    latitude = 42.349974,
    longitude = -71.077447,
    municipality = "Boston"
  )

  val KENMORE = MbtaStop(
    id = "place-kencl",
    name = "Kenmore",
    latitude = 42.348949,
    longitude = -71.095169,
    municipality = "Boston"
  )

  val AIRPORT = MbtaStop(
    id = "place-aport",
    name = "Airport",
    latitude = 42.374262,
    longitude = -71.030395,
    municipality = "Boston"
  )

  val FOREST_HILLS = MbtaStop(
    id = "place-forhl",
    name = "Forest Hills",
    latitude = 42.300523,
    longitude = -71.113686,
    municipality = "Boston"
  )

  val OAK_GROVE = MbtaStop(
    id = "place-ogmnl",
    name = "Oak Grove",
    latitude = 42.436680,
    longitude = -71.071097,
    municipality = "Malden"
  )

  val ALEWIFE = MbtaStop(
    id = "place-alfcl",
    name = "Alewife",
    latitude = 42.395428,
    longitude = -71.142483,
    municipality = "Cambridge"
  )

  val BRAINTREE = MbtaStop(
    id = "place-brntn",
    name = "Braintree",
    latitude = 42.207854,
    longitude = -71.001138,
    municipality = "Braintree"
  )

  val WONDERLAND = MbtaStop(
    id = "place-wondl",
    name = "Wonderland",
    latitude = 42.413420,
    longitude = -70.991648,
    municipality = "Revere"
  )

  val ALL_MAJOR_STOPS = listOf(
    PARK_STREET,
    DOWNTOWN_CROSSING,
    GOVERNMENT_CENTER,
    STATE,
    SOUTH_STATION,
    NORTH_STATION,
    BACK_BAY,
    HARVARD,
    KENDALL_MIT,
    COPLEY,
    KENMORE,
    AIRPORT,
    FOREST_HILLS,
    OAK_GROVE,
    ALEWIFE,
    BRAINTREE,
    WONDERLAND
  )

  val POPULAR_LANDMARKS = listOf(
    LandmarkPreset("TD Garden", "North Station • Sports & Concerts", 42.3662, -71.0621, "place-north"),
    LandmarkPreset("Fenway Park", "Kenmore / Fenway • Boston Red Sox", 42.3467, -71.0972, "place-kencl"),
    LandmarkPreset("Harvard Square", "Harvard • Historic Cambridge & Campus", 42.3736, -71.1190, "place-harsq"),
    LandmarkPreset("Boston Common", "Park Street / Boylston • Public Park", 42.3550, -71.0656, "place-pktrm"),
    LandmarkPreset("Logan Airport", "Airport • Terminals A, B, C, E", 42.3656, -71.0096, "place-aport"),
    LandmarkPreset("Faneuil Hall / Quincy Market", "Government Center / State", 42.3600, -71.0560, "place-gover"),
    LandmarkPreset("Museum of Fine Arts (MFA)", "MFA / Ruggles • Art Museum", 42.3394, -71.0940, "place-coecl"),
    LandmarkPreset("MIT / Kendall Square", "Kendall/MIT • Tech Hub", 42.3601, -71.0942, "place-knncl"),
    LandmarkPreset("Seaport District", "South Station / Silver Line SL1", 42.3510, -71.0435, "place-sstat"),
    LandmarkPreset("Newbury Street & Copley", "Copley • Shopping & Dining", 42.3503, -71.0770, "place-coecl")
  )
}
