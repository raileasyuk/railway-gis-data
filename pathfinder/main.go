package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/RyanCarrier/dijkstra"
	"github.com/paulmach/orb"
	"github.com/paulmach/orb/geojson"
	"golang.org/x/exp/maps"
	"log"
	"os"
	"pathfinder/types"
	"strings"
)

func importData(jsonPath string) (*types.OutputSchema, error) {
	// Read file
	data, err := os.ReadFile(jsonPath)
	if err != nil {
		return nil, err
	}

	// Unmarshal JSON file
	var output types.OutputSchema
	err = json.Unmarshal(data, &output)
	if err != nil {
		return nil, err
	}

	return &output, nil
}

func main() {
	// Import data
	output, err := importData("../networkGraph.json")
	if err != nil {
		panic(err)
	}

	graph, err := createGraph(*output)
	if err != nil {
		panic(err)
	}

	testPath := []string{
		"HYWRDSH",
		"STPXBOX",
	}

	path, err := findPathThroughTiplocs(graph, *output, testPath)
	if err != nil {
		panic(err)
	}

	print("Path: ")
	for _, node := range path.Path {
		print(node, " → ")
	}
	distMiles := float64(path.Distance) / 160900.0
	fmt.Printf("\nDistance: %.2f mi\n", distMiles)

	fc := geojson.NewFeatureCollection()

	// List all lat/lng points
	for i := range len(path.Path) - 1 {
		startNode := path.Path[i]
		endNode := path.Path[i+1]
		polyline := orb.LineString{}

		trackSegments := output.NodesGraph[int64(startNode)]
		var foundSegment *types.LinkedTrackNode = nil
		for _, trackSegment := range trackSegments {
			if trackSegment.NodeId == int64(endNode) {
				foundSegment = &trackSegment
				break
			}
		}

		if foundSegment == nil {
			panic(fmt.Errorf("no track segment found between nodes %d and %d", startNode, endNode))
		}

		trackMetadata := output.TrackMetadata[foundSegment.ViaLineId]

		for _, latLng := range trackMetadata.LineString {
			polyline = append(polyline, orb.Point{latLng.Lon, latLng.Lat})
		}

		fc.Append(geojson.NewFeature(polyline))
	}

	rawJSON, _ := fc.MarshalJSON()

	// output to file
	err = os.WriteFile(fmt.Sprintf("../route.geojson"), rawJSON, 0644)
}

func findPathThroughTiplocs(graph *dijkstra.Graph, data types.OutputSchema, tiplocs []string) (*dijkstra.BestPath, error) {
	var paths = make([]dijkstra.BestPath, len(tiplocs)-1)

	filteredTiplocs := make([]string, 0)
	for _, tiploc := range tiplocs {
		// Ensure the TIPLOC exists in the graph
		if _, ok := data.TiplocToNodeIds[tiploc]; !ok {
			log.Printf("TIPLOC %s not found in graph; skipping...", tiploc)
		} else {
			filteredTiplocs = append(filteredTiplocs, strings.ToUpper(tiploc))
		}
	}

	if len(filteredTiplocs) < 2 {
		return nil, fmt.Errorf("not enough valid TIPLOCs to find a path")
	}

	for i := range len(filteredTiplocs) - 1 {
		startTiploc := filteredTiplocs[i]
		endTiploc := filteredTiplocs[i+1]

		startNode := int(data.TiplocToNodeIds[startTiploc])
		endNode := int(data.TiplocToNodeIds[endTiploc])

		if startNode == 0 {
			return nil, fmt.Errorf("tiploc %s not found in graph", startTiploc)
		}
		if endNode == 0 {
			return nil, fmt.Errorf("tiploc %s not found in graph", endTiploc)
		}

		// Find path
		path, err := graph.Shortest(startNode, endNode)
		if err != nil {
			return nil, errors.Join(fmt.Errorf("failed to find route between %s and %s", startTiploc, endTiploc), err)
		}

		paths[i] = path
	}

	// Reduce paths
	var path []int
	distance := int64(0)
	for i := range len(paths) {
		if i > 0 {
			// Skip the first node as it is the same as the last node of the previous path
			path = append(path, paths[i].Path[1:]...)
		} else {
			path = append(path, paths[i].Path...)
		}

		distance += paths[i].Distance
	}

	return &dijkstra.BestPath{Distance: distance, Path: path}, nil
}

func createGraph(data types.OutputSchema) (*dijkstra.Graph, error) {
	graph := dijkstra.NewGraph()

	vertexIds := maps.Keys(data.NodesGraph)
	for _, id := range vertexIds {
		graph.AddVertex(int(id))
	}

	for id, linkedNodes := range data.NodesGraph {
		for _, linkedNode := range linkedNodes {
			err := graph.AddArc(int(id), int(linkedNode.NodeId), data.TrackMetadata[linkedNode.ViaLineId].LengthCentimetres)
			if err != nil {
				return nil, err
			}
			err = graph.AddArc(int(linkedNode.NodeId), int(id), data.TrackMetadata[linkedNode.ViaLineId].LengthCentimetres)
			if err != nil {
				return nil, err
			}
		}
	}

	return graph, nil
}
