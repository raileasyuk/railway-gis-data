package models

import mil.nga.sf.Geometry

internal interface GeoDataObject {
    val featureId: Long
    val geometry: Geometry
}