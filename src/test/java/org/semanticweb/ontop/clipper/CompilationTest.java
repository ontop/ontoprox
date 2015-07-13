package org.semanticweb.ontop.clipper;


import com.google.common.collect.ImmutableSet;
import it.unibz.krdb.obda.exception.DuplicateMappingException;
import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConnection;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLResultSet;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLStatement;
import it.unibz.krdb.obda.renderer.DatalogProgramRenderer;
import org.junit.Test;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.semanticweb.ontop.clipper.Ontology2MappingCompilation.compileHSHIQtoMappings;


public class CompilationTest {

    private static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    String ontologyFile =  "src/test/resources/npd-v2.owl";
    String datalogFile =  "src/test/resources/npd-v2.dl";
    String extendedObdaFile = "src/test/resources/extended-npd-v2-ql_a_postgres.obda";
    String extendedOntologyFile = "src/test/resources/extended-npd-v2-ql_a_postgres.owl";

    @Test
    public void testClipperRewriting() throws OWLOntologyCreationException, IOException {
        rewriteOntology(ontologyFile, datalogFile);
    }

    @Test
    public void testRewriteUOBM() throws OWLOntologyCreationException, IOException {
        rewriteOntology("src/test/resources/uobm/univ-bench-dl.owl", "src/test/resources/uobm/univ-bench-dl.dl");
    }

    @Test
    public void testRewriteNPD() throws OWLOntologyCreationException, IOException {
        rewriteOntology("src/test/resources/npd-v2.owl", "src/test/resources/npd-v2.dl");
    }

    @Test
    public void testRewriteNPDQL() throws OWLOntologyCreationException, IOException {
        rewriteOntology("src/test/resources/npd_ql/npd-v2-ql_a.owl", "src/test/resources/npd_ql/npd-v2-ql_a.dl");
    }

    @Test
    public void testRewriteLUBM() throws OWLOntologyCreationException, IOException {
        rewriteOntology("src/test/resources/lubm/univ-benchQL.owl", "src/test/resources/lubm/univ-benchQL.dl");
    }

    @Test
    public void testRewriteFabio() throws OWLOntologyCreationException, IOException {
        rewriteOntology("src/test/resources/fabio.owl", "src/test/resources/fabio.dl");
    }

    @Test
    public void testRewritePeriodic() throws OWLOntologyCreationException, IOException {
        rewriteOntology("src/test/resources/periodic.owl", "src/test/resources/periodic.dl");
    }

    private void rewriteOntology(String ontologyFile, String datalogFile) throws OWLOntologyCreationException, IOException {
        long t1 = System.currentTimeMillis();

        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(ontologyFile));

        /** create a Clipper Reasoner instance */
        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();

        /** feed the ontology to Clipper reasoner */
        qaHornSHIQ.setOntologies(ImmutableSet.of(ontology));

        /** rewrite the ontology to a datalog program represented in Clipper Native API */
        List<CQ> program = qaHornSHIQ.rewriteOntology();

        /** convert the datalog program to Ontop Native API */
        DatalogProgram ontopProgram = ClipperRuleToOntopRuleTranslator.translate(program);

        long t2 = System.currentTimeMillis();
        System.err.println("Datalog Generation time: " + (t2-t1) +  "ms");

        String datalogString = DatalogProgramRenderer.encode(ontopProgram);

        FileWriter writer = new FileWriter(datalogFile);
        writer.write(datalogString);
        writer.close();
    }


    @Test
    public  void testCompileNPD() throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException, DuplicateMappingException {
        String ontologyFile =  "src/test/resources/npd-v2.owl";
        String obdaFile = "src/test/resources/npd-v2.obda";
        String extendedObdaFile = "src/test/resources/extended-npd-v2-ql_a_postgres.obda";
        OBDAModel newModel = compileHSHIQtoMappings(ontologyFile, obdaFile);
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(extendedObdaFile);
    }

    @Test
    public  void testCompileNPDQL() throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException, DuplicateMappingException {
        String ontologyFile =  "src/test/resources/npd_ql/npd-v2-ql_a.owl";
        String obdaFile = "src/test/resources/npd_ql/npd-v2-ql_a_postgres.obda";
        String extendedObdaFile = "src/test/resources/npd_ql/extended-npd-v2-ql_a_postgres.obda";
        OBDAModel newModel = compileHSHIQtoMappings(ontologyFile, obdaFile);
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(extendedObdaFile);
    }

    @Test
    public  void testCompileUOBM() throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException, DuplicateMappingException {
        String ontologyFile = "src/test/resources/uobm/univ-bench-dl.owl";
        String obdaFile =     "src/test/resources/uobm/univ-bench-dl.obda";
        String extendedObdaFile = "src/test/resources/uobm/extended-univ-bench-dl.obda";
        OBDAModel newModel = compileHSHIQtoMappings(ontologyFile, obdaFile);
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(extendedObdaFile);
    }

    @Test
    public  void testCompileLUBM() throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException, DuplicateMappingException {
        String ontologyFile = "src/test/resources/lubm/univ-benchQL.owl";
        String obdaFile =     "src/test/resources/lubm/univ-benchQL.obda";
        String extendedObdaFile = "src/test/resources/lubm/extended-univ-benchQL.obda";
        OBDAModel newModel = compileHSHIQtoMappings(ontologyFile, obdaFile);
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(extendedObdaFile);
    }

    @Test
    public void testLoadBack() throws IOException, InvalidMappingException, OWLException, OBDAException {
        OBDAModel obdaModel = DATA_FACTORY.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        File obdafile = new File(extendedObdaFile);
        ioManager.load(obdafile);

        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(extendedOntologyFile));

        /*
		 * Prepare the configuration for the Quest instance. The example below shows the setup for
		 * "Virtual ABox" mode
		 */
        QuestPreferences preference = new QuestPreferences();
        preference.setCurrentValueOf(QuestPreferences.ABOX_MODE, QuestConstants.VIRTUAL);
        preference.setCurrentValueOf(QuestPreferences.SQL_GENERATE_REPLACE, QuestConstants.FALSE);

		/*
		 * Create the instance of Quest OWL reasoner.
		 */
        QuestOWLFactory factory = new QuestOWLFactory();
        factory.setOBDAController(obdaModel);
        factory.setPreferenceHolder(preference);
        QuestOWL reasoner = (QuestOWL) factory.createReasoner(ontology, new SimpleConfiguration());

		/*
		 * Prepare the data connection for querying.
		 */
        QuestOWLConnection conn = reasoner.getConnection();
        QuestOWLStatement st = conn.createStatement();

		/*
		 * Get the book information that is stored in the database
		 */
        String sparqlQuery =
                "PREFIX : <http://sws.ifi.uio.no/vocab/npd-v2#>\n" +
                        "PREFIX isc: <http://resource.geosciml.org/classifier/ics/ischart/>\n" +
                        "PREFIX nlxv: <http://sws.ifi.uio.no/vocab/norlex#>\n" +
                        "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "PREFIX npd: <http://sws.ifi.uio.no/data/npd-v2/>\n" +
                        "PREFIX void: <http://rdfs.org/ns/void#>\n" +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                        "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
                        "PREFIX ex: <http://example.org/ex#>\n" +
                        "PREFIX quest: <http://obda.org/quest#>\n" +
                        "PREFIX diskos: <http://sws.ifi.uio.no/data/diskos/>\n" +
                        "PREFIX nlx: <http://sws.ifi.uio.no/data/norlex/>\n" +
                        "PREFIX ptl: <http://sws.ifi.uio.no/vocab/npd-v2-ptl#>\n" +
                        "PREFIX npdv: <http://sws.ifi.uio.no/vocab/npd-v2#>\n" +
                        "PREFIX geos: <http://www.opengis.net/ont/geosparql#>\n" +
                        "PREFIX sql: <http://sws.ifi.uio.no/vocab/sql#>\n" +
                        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                        "PREFIX diskosv: <http://sws.ifi.uio.no/vocab/diskos#>\n" +
                        "SELECT DISTINCT ?licenceURI ?interest ?date\n" +
                        "WHERE {\n" +
                        "    ?licenceURI a npdv:ProductionLicence .\n" +
                        "    \t\t\n" +
                        "    [ ] a npdv:ProductionLicenceLicensee ;\n" +
                        "      \tnpdv:dateLicenseeValidFrom ?date ;\n" +
                        "      \tnpdv:licenseeInterest ?interest ;\n" +
                        "      \tnpdv:licenseeForLicence ?licenceURI .   \n" +
                        "   FILTER(?date > \"1979-12-31T00:00:00\"^^xsd:dateTime)\t\n" +
                        "}";

        try {
            long t1 = System.currentTimeMillis();
            QuestOWLResultSet rs = st.executeTuple(sparqlQuery);
            int columnSize = rs.getColumnCount();
            while (rs.nextRow()) {
                for (int idx = 1; idx <= columnSize; idx++) {
                    OWLObject binding = rs.getOWLObject(idx);
                    System.out.print(binding.toString() + ", ");
                }
                System.out.print("\n");
            }
            rs.close();
            long t2 = System.currentTimeMillis();
			/*
			 * Print the query summary
			 */
            QuestOWLStatement qst = (QuestOWLStatement) st;
            String sqlQuery = qst.getUnfolding(sparqlQuery);

            System.out.println();
            System.out.println("The input SPARQL query:");
            System.out.println("=======================");
            System.out.println(sparqlQuery);
            System.out.println();

            System.out.println("The output SQL query:");
            System.out.println("=====================");
            System.out.println(sqlQuery);

            System.out.println("Query Execution Time:");
            System.out.println("=====================");
            System.out.println((t2-t1) + "ms");

        } finally {

			/*
			 * Close connection and resources
			 */
            if (st != null && !st.isClosed()) {
                st.close();
            }
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
            reasoner.dispose();
        }
    }


}
