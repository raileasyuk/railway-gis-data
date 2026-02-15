# Railway GIS data

This combines GIS data from various sources to create a "good enough", simplified representation of the GB railway network for **public-facing applications**.

## Usage

Import the geopackage (`rail-map/merged.gpkg`) into your favourite GIS software (e.g. QGIS, ArcGIS Pro), and start exploring!

## Data sources

- [Network Rail geospatial data, licensed under Open Government License](https://github.com/openraildata/network-rail-gis)
- TIPLOC geospatial information from [GB Railway Data Ltd, licensed under CC-BY-SA 4.0](https://railmap.azurewebsites.net/Downloads), with some locations manually moved or removed.

## Documentation

For documention about editing the data model, please see the dedicated README file: [rail-map/README.md](./rail-map/README.md).

## Licensing

This repository and its contents are licensed under the Creative Commons Attribution-ShareAlike 4.0 International License (CC-BY-SA 4.0).

This means you are free to share and adapt the material for any purpose, even commercially, as long as you provide appropriate credit, indicate if changes were made, and distribute your contributions under the same license.

You must attribute the [data sources listed above](#data-sources), and provide attribution to Raileasy Ltd for your use of this data.

### License compatibility

OGL3, the license of the Network Rail track geometry, is [compatible with CC-BY 4.0](https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/#:~:text=These%20terms%20are%20compatible%20with%20the%20Creative%20Commons%20Attribution%20License%204.0), and thus the entire dataset can be made available under CC-BY-SA 4.0.