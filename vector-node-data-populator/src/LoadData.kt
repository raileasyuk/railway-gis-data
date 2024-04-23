import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageException
import mil.nga.geopackage.db.Result
import mil.nga.geopackage.features.user.FeatureRow
import mil.nga.geopackage.srs.SpatialReferenceSystem
import mil.nga.proj.Projection
import mil.nga.proj.ProjectionConstants
import mil.nga.proj.ProjectionTransform
import mil.nga.sf.Geometry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

data class GeometryHolder<T : Geometry>(
    val geometry: T,
    val srsId: Long,
)

@Throws(GeoPackageException::class)
internal inline fun <reified T : Geometry> loadGeometry(
    geoPackage: GeoPackage, featureTable: String
): Pair<Projection, MutableMap<Long, T>> {
    if (!geoPackage.featureTables.contains(featureTable)) {
        error("Table $featureTable not found in geopackage")
    }

    val featureDao = geoPackage.getFeatureDao(featureTable)
    val featureResultSet = featureDao.queryForAll()

    val lines = mutableMapOf<Long, T>()

    featureResultSet.use {
        featureResultSet.forEach { featureRow ->
            if (featureRow.geometryValue !is T) {
                error("Feature ${featureRow.id} in table ${featureRow.table.tableName} is not a ${T::class.simpleName}")
            }

            lines[featureRow.id] = featureRow.geometryValue as T
        }
    }

    return Pair(featureDao.geometryColumns.projection, lines)
}

@Throws(GeoPackageException::class)
internal fun loadFeatureRows(
    geoPackage: GeoPackage, featureTable: String
): List<FeatureRow> {
    if (!geoPackage.featureTables.contains(featureTable)) {
        error("Table $featureTable not found in geopackage")
    }

    val featureDao = geoPackage.getFeatureDao(featureTable)
    val featureResultSet = featureDao.queryForAll()

    featureResultSet.use {
        return featureResultSet.toList()
    }
}

private val srsCache = mutableMapOf<Long, SpatialReferenceSystem>()

@Throws(GeoPackageException::class)
internal fun getSrs(
    geoPackage: GeoPackage, id: Long
): SpatialReferenceSystem {
    return if (srsCache.containsKey(id)) {
        srsCache[id]!!
    } else {
        geoPackage.spatialReferenceSystemDao.queryForId(id)
    }
}

private val srsProjectionCache = mutableMapOf<Int, ProjectionTransform>()

@Throws(GeoPackageException::class)
internal fun getToWgs84Projection(
    projection: Projection,
): ProjectionTransform {
    val hash = projection.hashCode()
    if (srsProjectionCache.containsKey(hash)) {
        return srsProjectionCache[hash]!!
    }

    srsProjectionCache[hash] = projection.getTransformation(ProjectionConstants.EPSG_WORLD_GEODETIC_SYSTEM.toLong())
    return srsProjectionCache[hash]!!
}

@OptIn(ExperimentalContracts::class)
internal inline fun <T : Result?, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (e: Throwable) {
        exception = e
        throw e
    } finally {
        when {
            this == null -> {}
            exception == null -> close()
            else -> try {
                close()
            } catch (closeException: Throwable) {
                // ignore
            }
        }
    }
}
