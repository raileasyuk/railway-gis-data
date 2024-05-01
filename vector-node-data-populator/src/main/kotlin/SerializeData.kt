import kotlinx.serialization.Serializable

@Serializable
data class OutputSchema(
    val nodesGraph: Map<Long, List<LinkedTrackNode>>,
    val tiplocToNodeIds: Map<String, Long>,
    val nodeMetadata: Map<Long, NodeMetadata>,
    val trackMetadata: Map<Long, TrackMetadata>
)

@Serializable
data class LinkedTrackNode(val nodeId: Long, val viaLineId: Long)

@Serializable
data class NodeMetadata(val lat: Double, val lon: Double)

@Serializable
data class LatLon(val lat: Double, val lon: Double)

@Serializable
data class TrackMetadata(val lengthCentimetres: ULong, val lineString: List<LatLon>)