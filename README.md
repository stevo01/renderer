# renderer
Seamark layer tile renderer

The tile generation process is as follows:

1. A local instance of the OverPass server is searched for all objects that have a “seamark:type” tag.
2. The output from the search is compared with the previous search results to detect any changes.
3. For each object that has changed or has been created or deleted, all the nodes are extracted from both datasets.
4. The positions of those nodes is used to determine which tiles need to be re-rendered.
5. The Java program Jrender creates new tiles and deletes old tiles.
6. The tile server is updated.

The renderer is a Java program to be found here: https://josm.openstreetmap.de/osmsvn/applications/editors/josm/plugins/seachart/jrender

The bash script requires that jrender.jar & jsearch.jar are in the "work" directory together with a link to a local instance of Overpass
