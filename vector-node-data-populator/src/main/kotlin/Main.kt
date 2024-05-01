import kotlinx.serialization.json.Json
import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageManager
import mil.nga.sf.LineString
import models.TrackNode
import java.io.File
import kotlin.math.round

private const val LinesTableName = "Track Lines"
private const val NodesAndTiplocsTableName = "Track Nodes"

private fun loadGeopackage(path: String): GeoPackage {
    val gpkgFile = File(path)
    return GeoPackageManager.open(gpkgFile)
}

fun main() {
    println("Loading GeoPackage...")

    loadGeopackage("../Tracks and Nodes.gpkg").use { geoPackage ->
        geoPackage.verifyWritable()

        println("Loading track lines...")
        val (linesProjection, lines) = loadGeometry<LineString>(geoPackage, LinesTableName)
        println("Loaded ${lines.size} track lines")

        println("Loading nodes incl. TIPLOCs...")
        val (nodesProjection, nodes) = loadFeatureRows(
            geoPackage, NodesAndTiplocsTableName
        ).let { (projection, featureRows) ->
            Pair(projection, featureRows.map { TrackNode.fromFeatureRow(it) })
        }
        println("Loaded ${nodes.size} nodes...")
        val tiplocNodes = nodes.filter { it.tiploc != null }.associateBy { it.tiploc!! }
        println("...of which ${tiplocNodes.size} are TIPLOCs")

        println("Creating track lines id map by positions...")
        val linesIdsAtPositions: MutableMap<Pair<Double, Double>, MutableList<Long>> = mutableMapOf()
        lines.forEach { (id, line) ->
            val firstNode = line.points.first()
            val lastNode = line.points.last()

            val firstPosition = Pair(firstNode.x, firstNode.y)
            val lastPosition = Pair(lastNode.x, lastNode.y)

            linesIdsAtPositions.getOrPut(firstPosition) { mutableListOf() }.add(id)
            linesIdsAtPositions.getOrPut(lastPosition) { mutableListOf() }.add(id)
        }

        println("Locating track lines connected to each track node")
        val unlinkedNodeIds = mutableListOf<Long>()
        val nodeIdToLineIds: MutableMap<Long, MutableList<Long>> = mutableMapOf()
        nodes.forEach { node ->
            linesIdsAtPositions[Pair(node.geometry.x, node.geometry.y)]?.let { linesIds ->
                nodeIdToLineIds[node.featureId] = linesIds
            } ?: unlinkedNodeIds.add(node.featureId)
        }
        val lineIdToNodes = nodeIdToLineIds.flatMap { (nodeId, lineIds) ->
            lineIds.map { it to nodeId }
        }.groupBy({ it.first }, { it.second })

        println("Found ${unlinkedNodeIds.size} track nodes not linked to track lines")
        if (unlinkedNodeIds.isNotEmpty()) println("IDs: ${unlinkedNodeIds.joinToString()}")

        println("Processing TIPLOCs...")
        val tiplocsByPosition = mutableMapOf<Pair<Double, Double>, MutableList<String>>()
        tiplocNodes.values.forEach {
            tiplocsByPosition.getOrPut(Pair(it.geometry.x, it.geometry.y)) { mutableListOf() }.add(it.tiploc!!)
        }

        println("Finding track lines connected to each TIPLOC...")
        val tiplocIdToLineIds: MutableMap<String, MutableList<Long>> = mutableMapOf()
        lines.forEach { (lineId, line) ->
            line.points.forEach {
                tiplocsByPosition[Pair(it.x, it.y)]?.let { tiplocIds ->
                    tiplocIds.forEach { tiplocId ->
                        tiplocIdToLineIds.getOrPut(tiplocId) { mutableListOf() }.add(lineId)
                    }
                }
            }
        }
        val unlinkedTiplocIds = tiplocNodes.keys - tiplocIdToLineIds.keys
        println("Found ${unlinkedTiplocIds.size} TIPLOCs not linked to track lines")
        if (unlinkedTiplocIds.isNotEmpty()) println("IDs: ${unlinkedTiplocIds.joinToString()}")

        println("Calculating line distances...")
        val lineSrsToWgs84Transform = getToWgs84Projection(linesProjection)
        val lineIdsToLatLonPolyLine = lines.mapValues { (_, line) ->
            line.points.map {
                val latLng = lineSrsToWgs84Transform.transform(it.x, it.y)
                Pair(latLng[1], latLng[0])
            }
        }
        val lineIdsToMetreDistances = lineIdsToLatLonPolyLine.mapValues { (_, points) ->
            round(GeoUtils.polylineDistance(points) * 100).toULong()
        }

        println("Building network graph...")
        var failed = false
        val nodeGraph = nodeIdToLineIds.mapValues { (nodeId, lineIds) ->
            lineIds.mapNotNull { lineId ->
                val lineNodes = lineIdToNodes[lineId]!!
                val otherNodeId = lineNodes.firstOrNull { it != nodeId }

                if (otherNodeId == null) {
                    println("Line $lineId has no node other than $nodeId")
                    failed = true
                    null
                } else LinkedTrackNode(otherNodeId, lineId)
            }
        }

        if (failed) {
            println("Failed to build network graph")
            return@use
        }

        val tiplocToNodeIds = tiplocNodes.mapValues { (_, node) -> node.featureId }

        val nodeSrsToWgs84Transform = getToWgs84Projection(nodesProjection)
        val nodeMetadata = nodes.associate { node ->
            val transformed = nodeSrsToWgs84Transform.transform(node.geometry.x, node.geometry.y)
            node.featureId to NodeMetadata(transformed[1], transformed[0])
        }

        val trackMetadata = lineIdsToMetreDistances.mapValues { (id, length) ->
            TrackMetadata(length, lineIdsToLatLonPolyLine[id]!!.map { LatLon(it.first, it.second) })
        }

        println("Writing output to file...")

        val output = OutputSchema(nodeGraph, tiplocToNodeIds, nodeMetadata, trackMetadata)
        val json = Json // { prettyPrint = true }
        val jsonString = json.encodeToString(output)
        File("../networkGraph.json").writeText(jsonString)

        println("Done!")
    }
}
