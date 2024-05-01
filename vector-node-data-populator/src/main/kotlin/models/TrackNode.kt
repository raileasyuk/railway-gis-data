package models

import mil.nga.geopackage.features.user.FeatureRow
import mil.nga.sf.Point

internal data class TrackNode(
    val tiploc: String?,
    override val featureId: Long,
    override val geometry: Point,
): GeoDataObject {
    companion object {
        fun fromFeatureRow(featureRow: FeatureRow): TrackNode {
            val tiplocId = featureRow.getValue("stop_id") as String?
            val featureId = featureRow.id
            val geometry = featureRow.geometryValue as Point

            return TrackNode(tiplocId, featureId, geometry)
        }
    }
}