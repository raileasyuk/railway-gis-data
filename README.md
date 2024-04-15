# Railway GIS data

This combines GIS data from various sources to create a good enough compilation of data for the UK rail network.

## Data sources

- [Yet Another TIPLOC list](https://github.com/oweno-tfwm/YA_Tiploc_List)
  - [DfT NAPTAN dataset](https://beta-naptan.dft.gov.uk/) - OGL3
  - [RDG Industry Data](https://data.atoc.org/rail-industry-data) - CC BY 2.0 UK
  - [Network Rail TPS, BPLAN and CORPUS Open Data](https://www.networkrail.co.uk/who-we-are/transparency-and-ethics/transparency/open-data-feeds/network-rail-infrastructure-limited-data-feeds-licence) - OGL3
  - [Woodpecker](https://github.com/anisotropi4/woodpecker/blob/main/LICENSE) - Apache 2.0
  - [Rail Map](https://railmap.azurewebsites.net/Downloads) - CC-BY-SA 4.0
  - We have manually excluded the data from [UK2GTFS](https://github.com/ITSLeeds/UK2GTFS-data) due to its AGPL-3.0 license
- [Network Rail geospatial data, licensed under Open Government License](https://github.com/openraildata/network-rail-gis)

## Usage

Import the geopackage into your favourite GIS software, and start exploring.

## Contents

The geopackage file includes a copy of [Network Rail's VectorLinks and VectorNodes GIS data](https://github.com/openraildata/network-rail-gis), which represents the entire Network Rail (and more) managed rail infrastructure in the UK. It also includes the TIPLOC data from YATIPLOCL, which has had its nodes snapped to the Network Rail data.

`VectorNode`s are found at every point where two different track assets intersect. These can be used to create a graph of track assets, which can be used to calculate routes between points. It's important to make use of these `VectorNode`s rather than only using the polylines otherwise you may calculate routes that jump from a tunnel to a bridge where track passes over each other, for example.

## Known issues

- Newark Flat Crossing has `VectorNode`s for where the lines cross, but it's not possible for trains to move between the East Coast Main Line and the Nottingham &mdash; Lincoln line.
  - It may be decided to remove these nodes in the future to prevent inaccurate route calculations. Provided you use TIPLOCs as well, this shouldn't present issues.
- TIPLOCs may be snapped such that it is impossible to calculate a route from A to B, or doing so requires trains to use the wrong line.
  - Using the fake centre line data was initially explored, however the polylines do not always intersect cleanly in this data, presenting problems with calculating routes.
  - In the future, use of OpenStreetMap data instead may be preferred.

## Licensing

- OGL3 is [compatible with CC-BY 4.0](https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/#:~:text=These%20terms%20are%20compatible%20with%20the%20Creative%20Commons%20Attribution%20License%204.0)
- CC BY 2.0 UK is compatible with CC-BY 4.0.
- [Permission has been requested](https://github.com/anisotropi4/woodpecker/issues/4) to dual-license Woodpecker's data under CC-BY(-SA) or a compatible license.
- The AGPL-3.0 data sourced from UK2GTFS has been excluded from YATIPLOCL.
