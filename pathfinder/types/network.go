package types

type OutputSchema struct {
	NodesGraph      map[int64][]LinkedTrackNode `json:"nodesGraph"`
	NodeMetadata    map[int64]NodeMetadata      `json:"nodeMetadata"`
	TrackMetadata   map[int64]TrackMetadata     `json:"trackMetadata"`
	TiplocToNodeIds map[string]int64            `json:"tiplocToNodeIds"`
}

type LinkedTrackNode struct {
	NodeId    int64 `json:"nodeId"`
	ViaLineId int64 `json:"viaLineId"`
}

type NodeMetadata struct {
	Lat float64 `json:"lat"`
	Lon float64 `json:"lon"`
}

type LatLon struct {
	Lat float64 `json:"lat"`
	Lon float64 `json:"lon"`
}

type TrackMetadata struct {
	LengthCentimetres int64    `json:"lengthCentimetres"`
	LineString        []LatLon `json:"lineString"`
}
