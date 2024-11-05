package sparqlshapechecker.utils;

import cs.qse.common.ShapesExtractor;
import cs.utils.Constants;
import cs.utils.FilesUtil;
import de.atextor.turtle.formatter.FormattingStyle;
import de.atextor.turtle.formatter.TurtleFormatter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.XSD;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import shape_comparator.data.NodeShape;
import shape_comparator.data.PropertyShape;
import sparqlshapechecker.SparqlShapeChecker;
import shape_comparator.data.ShaclOrListItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GraphDbUtils {
    private static final Logger LOGGER = Logger.getLogger(SparqlShapeChecker.class.getName());

    public static void checkShapesInNewGraph(String url, String repositoryName, List<NodeShape> nodeShapes) {
        RepositoryManager repositoryManager = new RemoteRepositoryManager(url);
        try {
            Repository repo = repositoryManager.getRepository(repositoryName);
            repo.init();
            try (RepositoryConnection conn = repo.getConnection()) {
                checkNodeShapesInNewGraph(conn, nodeShapes);
                checkPropertyShapesInNewGraph(conn, nodeShapes);
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Exception occurred", ex);
            } finally {
                repo.shutDown();
            }
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception occurred", ex);
        } finally {
            repositoryManager.shutDown();
        }
    }

    public static void checkNodeShapesInNewGraph(RepositoryConnection conn, List<NodeShape> nodeShapes) {
        var targetClasses = nodeShapes.stream().map(ns -> ns.targetClass.toString()).collect(Collectors.toList()); //each nodeshape can only have on targetclass
        var filterString = String.join("> <", targetClasses);

        String sparql = "SELECT DISTINCT ?class (COUNT(DISTINCT ?s) AS ?classCount) FROM <http://www.ontotext.com/explicit> where {\n" +
                "\t?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n" +
                "VALUES ?class { <"+filterString+"> } }\n" +
                "Group by ?class";
        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparql);

        nodeShapes.forEach(n -> n.support = 0);

        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                var shapeIri = (IRI) bindingSet.getValue("class");
                var support = Integer.parseInt(bindingSet.getValue("classCount").stringValue());
                var nodeShapeOptional = nodeShapes.stream().filter(ns -> ns.targetClass.equals(shapeIri)).findFirst();
                if(nodeShapeOptional.isPresent()) {
                    var nodeShape = nodeShapeOptional.get();
                    nodeShape.support = support;
                }
                else
                    LOGGER.warning("Could not find node shape " + shapeIri.toString());
            }
        }
    }

    public static void checkPropertyShapesInNewGraph(RepositoryConnection conn, List<NodeShape> nodeShapes) {
        for(var nodeShape : nodeShapes) {
            if (nodeShape.support != 0) { //performance
                var targetClass = nodeShape.targetClass.toString();
                for (var propertyShape : nodeShape.propertyShapes) {
                    if (propertyShape.nodeKindAsIri != null && propertyShape.nodeKindAsIri.toString().equals("http://www.w3.org/ns/shacl#Literal")) {
                        propertyShape.support = getSupportForLiteralPropertyShape(propertyShape.pathAsIri, propertyShape.dataTypeOrClassAsIri, targetClass, conn);
                        setConfidence(nodeShape, propertyShape);
                    }
                    else if(propertyShape.nodeKindAsIri != null && propertyShape.nodeKindAsIri.toString().equals("http://www.w3.org/ns/shacl#IRI")) {
                        propertyShape.support = getSupportForIriPropertyShape(propertyShape.pathAsIri, propertyShape.dataTypeOrClassAsIri, targetClass, conn);
                        setConfidence(nodeShape, propertyShape);
                    }
                    else if (propertyShape.nodeKindAsIri == null) {
                        //Ignore special case when nodeKind is null, but there are also no nested items (QSE error)
                        if(propertyShape.orItems != null)  {
                            for(var orItem : propertyShape.orItems) {
                                if (orItem.nodeKind.toString().equals("http://www.w3.org/ns/shacl#Literal")) {
                                    orItem.support = getSupportForLiteralPropertyShape(propertyShape.pathAsIri, orItem.dataTypeOrClass, targetClass, conn);
                                    setConfidence(nodeShape, orItem);
                                }
                                else if(orItem.nodeKind.toString().equals("http://www.w3.org/ns/shacl#IRI")) {
                                    orItem.support = getSupportForIriPropertyShape(propertyShape.pathAsIri, orItem.dataTypeOrClass, targetClass, conn);
                                    setConfidence(nodeShape, orItem);
                                }
                            }
                        }
                        else {
                            propertyShape.errorDuringGeneration = true;
                        }
                    }
                }
            }
        }
    }

    private static void setConfidence(NodeShape nodeShape, PropertyShape propertyShape) {
        propertyShape.confidence = ((double) propertyShape.support / (double) nodeShape.support);
    }
    private static void setConfidence(NodeShape nodeShape, ShaclOrListItem orItem) {
        orItem.confidence = ((double) orItem.support / (double) nodeShape.support);
    }

    public static String deleteOrListAndConnectToParentNode(String shape, String parentIri, int newSupport, double newConfidence) {
        var model = ModelFactory.createDefaultModel();
        //problem with "," in confidence, this is read as two statements
        shape = shape.replaceAll("(?<=\\d),(?=\\d)", ".");

        model.read(new java.io.StringReader(shape), null, "TURTLE");
        Resource propertyShape = ResourceFactory.createResource(parentIri);

        String queryString = String.format("SELECT ?orList ?p ?o WHERE { " +
                "    <%s> <http://www.w3.org/ns/shacl#or> ?orList ." +
                "   ?orList <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> ?f. " +
                "   ?f ?p ?o. }", parentIri);

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        var statements = new ArrayList<Statement>();
        Resource orListItem = null;

        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                var o = soln.get("o");
                var p = soln.get("p").as(Property.class);
                orListItem = soln.getResource("orList");
                Statement s = ResourceFactory.createStatement(propertyShape, p, o);
                statements.add(s);
            }
        } finally {
            qexec.close();
        }

        //connect all connected statements to parent node
        model.add(statements);

        //Delete or list and all recursive statements
        model.removeAll(null, null, orListItem);
        removeRecursively(model, orListItem);
        var iriConfidence = ResourceFactory.createProperty("http://shaclshapes.org/confidence");
        var iriSupport = ResourceFactory.createProperty("http://shaclshapes.org/support");
        Literal confidenceLiteral = model.createTypedLiteral(newConfidence, XSD.xdouble.getURI());
        Literal supportLiteral = model.createTypedLiteral(newSupport);

        //set confidence and new support
        setSupportOrConfidence(model, propertyShape, iriConfidence, confidenceLiteral);
        setSupportOrConfidence(model, propertyShape, iriSupport, supportLiteral);

        TurtleFormatter formatter = new TurtleFormatter(FormattingStyle.DEFAULT);
        OutputStream outputStream = new ByteArrayOutputStream();
        formatter.accept(model, outputStream);
        return outputStream.toString().replaceAll("\n+$", "").replace("\r", "").replace("\r\n", "\n");
    }

    private static void setSupportOrConfidence(Model model, Resource propertyShape, Property iri, Literal newLiteral) {
        StmtIterator iterConfidence = model.listStatements(propertyShape, iri, (RDFNode) null);
        List<Statement> statementsToRemove = new ArrayList<>();
        while (iterConfidence.hasNext()) {
            Statement stmt = iterConfidence.nextStatement();
            statementsToRemove.add(stmt);
        }
        iterConfidence.close();
        for (Statement stmt : statementsToRemove) {
            model.remove(stmt);
        }
        model.add(propertyShape, iri, newLiteral);
    }

    private static void removeRecursively(Model model, Resource resourceToDelete) {
        var statementQueue = model.listStatements(resourceToDelete, null, (RDFNode) null).toList();
        while (!statementQueue.isEmpty()) {
            var nextStatement = statementQueue.get(0);
            if(nextStatement.getObject().isAnon()) {
                statementQueue.addAll(model.listStatements((Resource) nextStatement.getObject(), null, (RDFNode)null ).toList());
            }
            model.remove(nextStatement);
            statementQueue.remove(nextStatement);
        }
    }

    private static int getSupportForIriPropertyShape(IRI path, IRI classIri, String targetClass, RepositoryConnection conn) {
        String sparql;
        //special case where object does not have a type
        if(classIri.toString().equals("http://shaclshapes.org/undefined")) {
            sparql = "PREFIX onto: <http://www.ontotext.com/>\n" +
                    "SELECT ( COUNT( DISTINCT ?s) AS ?count) FROM onto:explicit WHERE {\n" +
                    "    ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n" +
                    "    ?s <" + path + "> ?obj .\n" +
                    " VALUES ?class { <" + targetClass + "> } \n" +
                    "    FILTER NOT EXISTS {?obj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?objDataType}\n" +
                    "}";
        }
        else {
            //special case where object is actually literal but still has datatype e.g. 3,2^^kilometre
            sparql = "SELECT ( COUNT( DISTINCT ?s) AS ?count) \n" +
                    "FROM <http://www.ontotext.com/explicit> WHERE { \n" +
                    " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class .\n" +
                    " ?s <" + path + "> ?obj . \n" +
                    " optional{?obj <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?dataTypeRdfType}. \n" +
                    " BIND (datatype(?obj) AS ?dataTypeLiteral) \n" +
                    " VALUES ?class { <" + targetClass + "> }" +
                    " FILTER (?dataTypeRdfType = <"+classIri+"> || ?dataTypeLiteral = <"+classIri+">)}";
        }
        return getCountFromSparqlQuery(conn, sparql);
    }

    private static int getCountFromSparqlQuery(RepositoryConnection conn, String sparql) {
        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparql);

        try (TupleQueryResult result = query.evaluate()) {
            if (result.hasNext()) {
                BindingSet bindingSet = result.next();
                return Integer.parseInt(bindingSet.getValue("count").stringValue());
            }
            throw new Exception("No support returned");
        }
        catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Exception occurred", ex);
            return 0;
        }
    }

    private static int getSupportForLiteralPropertyShape(IRI path, IRI dataType, String targetClass, RepositoryConnection conn) {
        String sparql = "SELECT ( COUNT( DISTINCT ?s) AS ?count) " +
                "FROM <http://www.ontotext.com/explicit> WHERE { " +
                " ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?class ." +
                " ?s <" + path + "> ?obj . " +
                " FILTER(dataType(?obj) = <" + dataType + "> )" +
                " VALUES ?class { <" + targetClass + "> }}";
        return getCountFromSparqlQuery(conn, sparql);
    }

    public static void constructDefaultShapes(String path) {
        Repository db = new SailRepository(new NativeStore(new File(path)));
        ShapesExtractor shapesExtractor = new ShapesExtractor();
        try (RepositoryConnection conn = db.getConnection()) {
            conn.setNamespace("shape", Constants.SHAPES_NAMESPACE);
            conn.setNamespace("shape", Constants.SHACL_NAMESPACE);

            String outputFilePath = shapesExtractor.writeModelToFile("QSE_FULL", conn);
            shapesExtractor.prettyFormatTurtle(outputFilePath);
            FilesUtil.deleteFile(outputFilePath);
        } finally {
            db.shutDown();
        }
    }
}
