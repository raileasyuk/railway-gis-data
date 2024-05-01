import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    /**
     * Calculate the great circle distance between two points on the Earth's surface in metres.
     *
     * Implementation of https://en.wikipedia.org/wiki/Haversine_formula
     */
    private fun distance(point1: Pair<Double, Double>, point2: Pair<Double, Double>): Double {
        val (lat1, lon1) = point1
        val (lat2, lon2) = point2

        val earthRadius = 6371000.0 // metres
        val dLat = Math.toRadians((lat2 - lat1))
        val dLon = Math.toRadians((lon2 - lon1))
        val a =
            sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) *
                    sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun polylineDistance(points: List<Pair<Double, Double>>): Double {
        if (points.size < 2) return 0.0

        var lastPoint = points.first()

        return points.subList(1, points.size).sumOf {
            val dist = distance(lastPoint, it)
            lastPoint = it

            dist
        }
    }

    /**
     * Calculate the (approximate) bounds of a circle centred on a given latitude and longitude with
     * a given radius.
     */
    fun calculateBoundsFromLatLngAndRadius(
        initialLatLng: Pair<Double, Double>,
        radiusMetres: Double
    ): Pair<Pair<Double, Double>, Pair<Double, Double>> {
        // number of km per degree = ~111km (111.32 in Google Maps, but range varies
        // between 110.567km at the equator and 111.699km at the poles)
        val coef = radiusMetres / 111320.0

        val topRightPair =
            Pair(
                initialLatLng.first + coef,
                initialLatLng.second + coef / cos(Math.toRadians(initialLatLng.first)),
            )

        val bottomLeftPair =
            Pair(
                initialLatLng.first - coef,
                initialLatLng.second - coef / cos(Math.toRadians(initialLatLng.first)),
            )

        return Pair(topRightPair, bottomLeftPair)
    }

    fun metresToMiles(metres: Double): Double = metres / 1609.344f
}