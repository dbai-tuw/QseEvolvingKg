# QseEvolvingKg

This project is a diploma thesis in the area of evolving knowledge graphs and contains three sub-projects. All parts of this work build on the QSE algorithm. The QSE version used is available at https://github.com/dkw-aau/qse/tree/shactor (PR merged on 25.10.2024). A Jar file from this version is available under /resources/qse-1.0-QSE-all.jar. This path can be changed in the pom.xml file. 

## Shape-Comparator
This application is based on Vaadin and provides a web interface to compare SHACL shapes from different QSE runs with each other. Graphs can be uploaded in .*nt format and can have different versions.

## SHACL-DiffExtractor
With this application QSE can be executed on a subsequent version of a graph by using only changesets. The config file `ConfigShaclDiffExtractor.properties` contains the following items:  
- `filePathInitialVersion`:  Specifies the full file path for the initial version on which QSE will be executed. The file must be available in **N-Triples format**.
- `initialVersionName`:   Defines the name of the dataset for the initial version, allowing the graph version to have a different name than the file name.
- `pruningThresholds`:   Sets the pruning thresholds for QSE in the format `{(confidence,support)}`. These thresholds apply during the first execution of QSE and while parsing changesets:
  - **Confidence**: A decimal between 0 and 1.
  - **Support**: Any non-negative number (default is 0).
- `doMetaComparison`:  
  A boolean value (`"true"` or `"false"`) that determines if the algorithm's results should be compared to the baseline:
  - **`true`**: Runs QSE on all versions provided in the configuration, ignoring changesets, and produces results from both the SHACL-DiffExtractor and QSE for comparison.
  - **`false`**: Runs only the SHACL-DiffExtractor without the QSE comparison.

A list of knowledge graph versions must be provided that the SHACL-DiffExtractor will evaluate. For each version, the parameters should be prefixed with the version name. Each file must be in **N-Triples format**. 
- `<versionName>.filePathAdded`:  
  Specifies the file path for triples added between the previous version and this version.
- `<versionName>.filePathDeleted`:  
  Specifies the file path for triples deleted between the previous version and this version.
- `<versionName>.filePathFullVersion` *(optional)*:  
  Required only if `doMetaComparison` is set to `"true"`. Provides the file path for the full version of the knowledge graph.

## SPARQL-ShapeChecker
This application checks the SHACL shapes for an initial graph version on a subsequent version. The graph versions must be available on a triplestore like GraphDB for this project. The config file `ConfigSparqlShapeChecker.properties` contains the following items:  
- `graphDbUrl`:  Specifies the URL through which the graph is accessible on a triple store. For example, if GraphDB is hosted locally, the URL could be something like `http://localhost:7200/`.
- `dataSetNameQSE`:   The name of the dataset for the initial version on which QSE will be executed. The results of this initial version will serve as the foundation for the SPARQL-ShapeChecker. The algorithm will use the URL provided by `graphDbUrl` to locate a dataset with this name.
- `dataSetsToCheck`:   A list of version names available through `graphDbUrl`. The SPARQL-ShapeChecker will be run on each of these versions, using the QSE results from `dataSetNameQSE` as a basis. You can list one or more dataset names, separated by commas.
- `pruningThresholds`:   Specifies pruning thresholds used by QSE in the format `{(confidence,support)}`:
  - **Confidence** should be a decimal number between 0 and 1.
  - **Support** can be any non-negative number (default is 0).
- `doMetaComparison`:   A boolean parameter (`"true"` or `"false"`) that determines whether to compare the algorithm’s output to QSE:
  - **`true`**: Executes QSE in parallel, generating results from both the SPARQL-ShapeChecker and QSE, along with a comparison of the two methods.
  - **`false`**: Generates results only from the SPARQL-ShapeChecker.

## Running the applications

The ShapeComparator is a standard Maven project. To run it from the command line,
type `mvnw` (Windows), or `./mvnw` (Mac & Linux), then open
http://localhost:8082 in your browser. The H2 database can be accessed via http://localhost:8082/h2-console/.  
The other projects can be run by using the classes "shacldiffextractor.ShaclDiffExtractor" and "sparqlshapechecker.SparqlShapeChecker".
For a successful execution on a Linux VM the following commands were run before:  
sudo mvn clean install  
sudo mvn dependency:copy-dependencies  
sudo java -cp "target/classes:target/dependency/`*`" shacldiffextractor.ShaclDiffExtractor   
or  
sudo java -cp "target/classes:target/dependency/`*`" sparqlshapechecker.SparqlShapeChecker     

## Project structure

- `graphs` contains a copy of all uploaded graph versions and the extracted SHACL shapes for the ShapeComparator application. There is a sub-folder called `pre-configured` where pre-configured .nt files can be uploaded which can then be used in the application.
- `Output` will contain the QSE output.
- `resources` contains scripts that QSE needs. There is a folder called `evaluation_scripts` which contains the scripts for the evaluation of the SHACL-DiffExtractor and the SPARQL-ShapeChecker on a Linux virtual machine.
- `src/main/java` conatins packages for all three sub projects.
- `src/main/test` contains tests for all sub projects. However, only some files are used for testing, the others are just used for debugging.

From the Vaadin documentation for the ShapeComparator application:
- `MainLayout.java` in `src/main/java/shape_comparator/views` contains the navigation setup (i.e., the
  side/top bar and the main menu).
- `views` package in `src/main/java/shape_comparator` contains the server-side Java views of your application.
- `views` folder in `frontend/` contains the client-side JavaScript views of your application.
- `themes` folder in `frontend/` contains the custom CSS styles.

