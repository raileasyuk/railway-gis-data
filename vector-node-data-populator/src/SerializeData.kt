import com.fasterxml.jackson.databind.util.LinkedNode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class OutputSchema(val trackNodes: Map<Long, List<LinkedTrackNode>>)

@Serializable
data class LinkedTrackNode(val id: Long)