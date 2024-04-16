import mil.nga.geopackage.GeoPackage
import mil.nga.geopackage.GeoPackageException
import mil.nga.geopackage.db.Result
import mil.nga.geopackage.features.user.FeatureRow
import mil.nga.sf.Geometry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Throws(GeoPackageException::class)
internal inline fun <reified T : Geometry> loadGeometry(
    geoPackage: GeoPackage, featureTable: String
): MutableMap<Long, T> {
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

    return lines
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
