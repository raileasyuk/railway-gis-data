import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageManager
import mil.nga.geopackage.db.GeoPackageDataType
import mil.nga.geopackage.features.user.FeatureColumn
import mil.nga.geopackage.features.user.FeatureDao
import mil.nga.geopackage.user.ContentValues
import mil.nga.geopackage.user.UserDao
import mil.nga.sf.LineString
import mil.nga.sf.Point
import models.Tiploc
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


private const val LinesTableName = "Track Lines"
private const val NodesTableName = "Track Line Nodes"
private const val TiplocsTableName = "Track Line TIPLOCs"

private fun loadGeopackage(path: String): GeoPackage {
    val gpkgFile = File(path)
    return GeoPackageManager.open(gpkgFile)
}

private fun createGeopackage(path: String): GeoPackage {
    val gpkgFile = File(path)
    return GeoPackageManager.open(GeoPackageManager.create(gpkgFile))
}

fun main() {
    println("Loading GeoPackage...")

    loadGeopackage("../Network Rail Track Lines Nodes and TIPLOCs.gpkg").use { geoPackage ->
        geoPackage.verifyWritable()

        println("Loading track lines...")
        val (linesProjection, lines) = loadGeometry<LineString>(geoPackage, LinesTableName)
        println("Loaded ${lines.size} track lines")

        println("Loading track nodes...")
        val (nodesProjection, nodes) = loadGeometry<Point>(geoPackage, NodesTableName)
        println("Loaded ${nodes.size} track nodes")

        println("Loading TIPLOCs...")
        val tiplocs =
            loadFeatureRows(geoPackage, TiplocsTableName).map { Tiploc.fromFeatureRow(it) }.associateBy { it.tiplocId }
        println("Loaded ${tiplocs.size} TIPLOCs")

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
        nodes.forEach { (id, node) ->
            linesIdsAtPositions[Pair(node.x, node.y)]?.let { linesIds ->
                nodeIdToLineIds[id] = linesIds
            } ?: unlinkedNodeIds.add(id)
        }

        println("Found ${unlinkedNodeIds.size} track nodes not linked to track lines")
        if (unlinkedNodeIds.isNotEmpty()) println("IDs: ${unlinkedNodeIds.joinToString()}")

        println("Processing TIPLOCs...")
        val tiplocsByPosition = mutableMapOf<Pair<Double, Double>, MutableList<String>>()
        tiplocs.values.forEach {
            tiplocsByPosition.getOrPut(Pair(it.geometry.x, it.geometry.y)) { mutableListOf() }.add(it.tiplocId)
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
        val unlinkedTiplocIds = tiplocs.keys - tiplocIdToLineIds.keys
        println("Found ${unlinkedTiplocIds.size} TIPLOCs not linked to track lines")
        if (unlinkedTiplocIds.isNotEmpty()) println("IDs: ${unlinkedTiplocIds.joinToString()}")

        val tiplocsWithMultipleLines = tiplocIdToLineIds.filterValues { it.size > 1 }
        println("Found ${tiplocsWithMultipleLines.size} TIPLOCs linked to multiple track lines")
        if (tiplocsWithMultipleLines.isNotEmpty()) println("IDs: ${tiplocsWithMultipleLines.keys.joinToString()}")

        println("Calculating line distances...")
        val lineSrsToWgs84Transform = getToWgs84Projection(linesProjection)
        val lineIdsToMetreDistances = mutableMapOf<Long, Double>()
        lines.forEach { (lineId, line) ->
            lineIdsToMetreDistances[lineId] = GeoUtils.polylineDistance(line.points.map {
                val latLng = lineSrsToWgs84Transform.transform(it.x, it.y)
                Pair(latLng[0], latLng[1])
            })
        }

        println("Writing data to GeoPackage")
        geoPackage.beginTransaction()

        run {
            // Write distances into line data
            val dao = geoPackage.getFeatureDao(LinesTableName)
            val col = "Dist_Metres"

            dao.dropAndRecreateColumn(col, GeoPackageDataType.DOUBLE)

            lineIdsToMetreDistances.forEach { (id, metres) ->
                dao.update(ContentValues().apply { put(col, metres) }, "fid = ?", arrayOf(id.toString()))
            }
        }

        run {
            // Write comma-separated line IDs associated with each node
            val dao = geoPackage.getFeatureDao(NodesTableName)
            val col = "Linked_Lines"

            dao.dropAndRecreateColumn(col, GeoPackageDataType.TEXT)

            nodeIdToLineIds.forEach { (id, ids) ->
                dao.update(ContentValues().apply { put(col, ids.joinToString(",")) }, "fid = ?", arrayOf(id.toString()))
            }
        }

        run {
            // Write comma-separated line IDs linked to each TIPLOC
            val dao = geoPackage.getFeatureDao(TiplocsTableName)
            val col = "Linked_Lines"

            dao.dropAndRecreateColumn(col, GeoPackageDataType.TEXT)

            tiplocIdToLineIds.forEach { (id, ids) ->
                dao.update(ContentValues().apply { put(col, ids.joinToString(",")) }, "stop_id = ?", arrayOf(id))
            }
        }

        geoPackage.endTransaction()
    }
}

internal fun FeatureDao.dropAndRecreateColumn(columnName: String, type: GeoPackageDataType) {
    if (this.columnNames.contains(columnName)) {
        this.dropColumn(columnName)
    }
    this.addColumn(FeatureColumn.createColumn(columnName, type))
}
