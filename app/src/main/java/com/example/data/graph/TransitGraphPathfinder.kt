package com.example.data.graph

import com.example.data.model.Itinerary
import com.example.data.model.ItineraryLeg
import com.example.data.model.LegType
import com.example.data.model.MbtaRoute
import com.example.data.model.MbtaStop
import java.util.PriorityQueue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class GraphEdge(
  val toStopId: String,
  val travelMinutes: Int,
  val routeId: String,
  val routeName: String,
  val routeColorHex: String,
  val isTransferWalk: Boolean = false
)

object TransitGraphPathfinder {

  private val stations = mutableMapOf<String, MbtaStop>()
  private val adjacencyList = mutableMapOf<String, MutableList<GraphEdge>>()

  init {
    BostonLandmarks.ALL_MAJOR_STOPS.forEach { stop ->
      stations[stop.id] = stop
      adjacencyList[stop.id] = mutableListOf()
    }

    // RED LINE
    addBidirectionalTransitEdge("place-alfcl", "place-harsq", 6, "Red", "Red Line", "DA291C")
    addBidirectionalTransitEdge("place-harsq", "place-knncl", 4, "Red", "Red Line", "DA291C")
    addBidirectionalTransitEdge("place-knncl", "place-pktrm", 5, "Red", "Red Line", "DA291C")
    addBidirectionalTransitEdge("place-pktrm", "place-dwnxg", 2, "Red", "Red Line", "DA291C")
    addBidirectionalTransitEdge("place-dwnxg", "place-sstat", 2, "Red", "Red Line", "DA291C")
    addBidirectionalTransitEdge("place-sstat", "place-brntn", 18, "Red", "Red Line", "DA291C")

    // ORANGE LINE
    addBidirectionalTransitEdge("place-ogmnl", "place-north", 12, "Orange", "Orange Line", "ED8B00")
    addBidirectionalTransitEdge("place-north", "place-state", 4, "Orange", "Orange Line", "ED8B00")
    addBidirectionalTransitEdge("place-state", "place-dwnxg", 2, "Orange", "Orange Line", "ED8B00")
    addBidirectionalTransitEdge("place-dwnxg", "place-bbsta", 4, "Orange", "Orange Line", "ED8B00")
    addBidirectionalTransitEdge("place-bbsta", "place-forhl", 10, "Orange", "Orange Line", "ED8B00")

    // GREEN LINE
    addBidirectionalTransitEdge("place-north", "place-gover", 3, "Green", "Green Line", "00843D")
    addBidirectionalTransitEdge("place-gover", "place-pktrm", 2, "Green", "Green Line", "00843D")
    addBidirectionalTransitEdge("place-pktrm", "place-coecl", 4, "Green", "Green Line", "00843D")
    addBidirectionalTransitEdge("place-coecl", "place-kencl", 4, "Green", "Green Line", "00843D")

    // BLUE LINE
    addBidirectionalTransitEdge("place-gover", "place-state", 2, "Blue", "Blue Line", "003DA5")
    addBidirectionalTransitEdge("place-state", "place-aport", 6, "Blue", "Blue Line", "003DA5")
    addBidirectionalTransitEdge("place-aport", "place-wondl", 11, "Blue", "Blue Line", "003DA5")

    // IN-STATION TRANSFER EDGES (Park St, Downtown Crossing, Government Ctr, State, North Station)
    addBidirectionalTransferWalk("place-pktrm", "place-dwnxg", 4, "Concourse Walk (Red/Orange/Green)")
  }

  private fun addBidirectionalTransitEdge(
    u: String,
    v: String,
    minutes: Int,
    routeId: String,
    routeName: String,
    colorHex: String
  ) {
    adjacencyList.getOrPut(u) { mutableListOf() }.add(GraphEdge(v, minutes, routeId, routeName, colorHex, false))
    adjacencyList.getOrPut(v) { mutableListOf() }.add(GraphEdge(u, minutes, routeId, routeName, colorHex, false))
  }

  private fun addBidirectionalTransferWalk(u: String, v: String, minutes: Int, note: String) {
    adjacencyList.getOrPut(u) { mutableListOf() }.add(GraphEdge(v, minutes, "WALK", note, "888888", true))
    adjacencyList.getOrPut(v) { mutableListOf() }.add(GraphEdge(u, minutes, "WALK", note, "888888", true))
  }

  fun getClosestStation(lat: Double, lng: Double): MbtaStop {
    return stations.values.minByOrNull { stop ->
      calculateDistanceMiles(lat, lng, stop.latitude, stop.longitude)
    } ?: BostonLandmarks.PARK_STREET
  }

  fun calculateDistanceMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 3958.8 // Earth radius in miles
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
  }

  fun findShortestPath(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
    fromLabel: String,
    toLabel: String
  ): Itinerary {
    val startStation = getClosestStation(originLat, originLng)
    val endStation = getClosestStation(destLat, destLng)

    val walkToStartMiles = calculateDistanceMiles(originLat, originLng, startStation.latitude, startStation.longitude)
    val walkToEndMiles = calculateDistanceMiles(destLat, destLng, endStation.latitude, endStation.longitude)

    val walkToStartMins = (walkToStartMiles * 18.0).toInt().coerceAtLeast(1)
    val walkToEndMins = (walkToEndMiles * 18.0).toInt().coerceAtLeast(1)

    // If destination is right next to origin station
    if (startStation.id == endStation.id) {
      val directWalkMins = ((walkToStartMiles + walkToEndMiles) * 15.0).toInt().coerceAtLeast(2)
      return Itinerary(
        id = "itin_direct_walk",
        fromName = fromLabel,
        toName = toLabel,
        totalDurationMinutes = directWalkMins,
        walkTimeMinutes = directWalkMins,
        transferCount = 0,
        legs = listOf(
          ItineraryLeg(
            legType = LegType.WALK,
            fromStopName = fromLabel,
            toStopName = toLabel,
            durationMinutes = directWalkMins,
            instructions = "Walk directly to destination (${String.format("%.1f", walkToStartMiles + walkToEndMiles)} mi)"
          )
        )
      )
    }

    // Dijkstra across transit graph
    data class PathNode(val stopId: String, val costMinutes: Int, val prevEdge: GraphEdge?, val prevNode: PathNode?) : Comparable<PathNode> {
      override fun compareTo(other: PathNode): Int = costMinutes.compareTo(other.costMinutes)
    }

    val distances = mutableMapOf<String, Int>()
    val priorityQueue = PriorityQueue<PathNode>()

    distances[startStation.id] = 0
    priorityQueue.add(PathNode(startStation.id, 0, null, null))

    var targetNode: PathNode? = null

    while (priorityQueue.isNotEmpty()) {
      val current = priorityQueue.poll() ?: break

      if (current.stopId == endStation.id) {
        targetNode = current
        break
      }

      if (current.costMinutes > (distances[current.stopId] ?: Int.MAX_VALUE)) continue

      val edges = adjacencyList[current.stopId] ?: emptyList()
      for (edge in edges) {
        // Transfer penalty if switching transit lines
        val transferPenalty = if (current.prevEdge != null && !edge.isTransferWalk && current.prevEdge.routeId != edge.routeId) 4 else 0
        val newCost = current.costMinutes + edge.travelMinutes + transferPenalty

        if (newCost < (distances[edge.toStopId] ?: Int.MAX_VALUE)) {
          distances[edge.toStopId] = newCost
          priorityQueue.add(PathNode(edge.toStopId, newCost, edge, current))
        }
      }
    }

    // Reconstruct path
    val legs = mutableListOf<ItineraryLeg>()
    legs.add(
      ItineraryLeg(
        legType = LegType.WALK,
        fromStopName = fromLabel,
        toStopName = startStation.name,
        durationMinutes = walkToStartMins,
        instructions = "Walk to ${startStation.name} (${String.format("%.1f", walkToStartMiles)} mi)"
      )
    )

    if (targetNode != null) {
      val rawSegments = mutableListOf<Pair<String, GraphEdge>>()
      var curr: PathNode? = targetNode
      while (curr?.prevEdge != null && curr.prevNode != null) {
        rawSegments.add(0, Pair(curr.prevNode!!.stopId, curr.prevEdge!!))
        curr = curr.prevNode
      }

      // Consolidate segments by continuous transit line
      var currentRouteId: String? = null
      var currentRouteName = ""
      var currentColor = "00843D"
      var segmentStartStop = ""
      var segmentEndStop = ""
      var segmentDuration = 0
      var stopsInLeg = 0

      for ((fromId, edge) in rawSegments) {
        val fromName = stations[fromId]?.name ?: fromId
        val toName = stations[edge.toStopId]?.name ?: edge.toStopId

        if (edge.isTransferWalk) {
          // Flush existing transit leg
          if (currentRouteId != null) {
            legs.add(
              ItineraryLeg(
                legType = LegType.TRANSIT,
                routeId = currentRouteId,
                routeName = currentRouteName,
                routeColorHex = currentColor,
                fromStopName = segmentStartStop,
                toStopName = segmentEndStop,
                durationMinutes = segmentDuration,
                instructions = "Ride $currentRouteName from $segmentStartStop to $segmentEndStop",
                stopsCount = stopsInLeg
              )
            )
            currentRouteId = null
          }
          // Add transfer leg
          legs.add(
            ItineraryLeg(
              legType = LegType.TRANSFER,
              fromStopName = fromName,
              toStopName = toName,
              durationMinutes = edge.travelMinutes,
              instructions = "Transfer via pedestrian connection ($toName)"
            )
          )
        } else if (edge.routeId != currentRouteId) {
          if (currentRouteId != null) {
            legs.add(
              ItineraryLeg(
                legType = LegType.TRANSIT,
                routeId = currentRouteId,
                routeName = currentRouteName,
                routeColorHex = currentColor,
                fromStopName = segmentStartStop,
                toStopName = segmentEndStop,
                durationMinutes = segmentDuration,
                instructions = "Ride $currentRouteName from $segmentStartStop to $segmentEndStop",
                stopsCount = stopsInLeg
              )
            )
            legs.add(
              ItineraryLeg(
                legType = LegType.TRANSFER,
                fromStopName = segmentEndStop,
                toStopName = fromName,
                durationMinutes = 4,
                instructions = "Transfer to ${edge.routeName} (Platform connection)"
              )
            )
          }
          currentRouteId = edge.routeId
          currentRouteName = edge.routeName
          currentColor = edge.routeColorHex
          segmentStartStop = fromName
          segmentEndStop = toName
          segmentDuration = edge.travelMinutes
          stopsInLeg = 1
        } else {
          segmentEndStop = toName
          segmentDuration += edge.travelMinutes
          stopsInLeg += 1
        }
      }

      if (currentRouteId != null) {
        legs.add(
          ItineraryLeg(
            legType = LegType.TRANSIT,
            routeId = currentRouteId,
            routeName = currentRouteName,
            routeColorHex = currentColor,
            fromStopName = segmentStartStop,
            toStopName = segmentEndStop,
            durationMinutes = segmentDuration,
            instructions = "Ride $currentRouteName from $segmentStartStop to $segmentEndStop",
            stopsCount = stopsInLeg
          )
        )
      }
    }

    legs.add(
      ItineraryLeg(
        legType = LegType.WALK,
        fromStopName = endStation.name,
        toStopName = toLabel,
        durationMinutes = walkToEndMins,
        instructions = "Walk to $toLabel (${String.format("%.1f", walkToEndMiles)} mi)"
      )
    )

    val totalDuration = legs.sumOf { it.durationMinutes }
    val walkDuration = legs.filter { it.legType == LegType.WALK }.sumOf { it.durationMinutes }
    val transfers = legs.count { it.legType == LegType.TRANSFER }

    return Itinerary(
      id = "itin_${System.currentTimeMillis()}",
      fromName = fromLabel,
      toName = toLabel,
      totalDurationMinutes = totalDuration,
      walkTimeMinutes = walkDuration,
      transferCount = transfers,
      legs = legs
    )
  }
}
