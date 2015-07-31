package org.semanticweb.ontop.beyondql.hornshiq;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.exception.InvalidPredicateDeclarationException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConnection;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLResultSet;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLStatement;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

public class CompileAndRewriteTest {

	private Connection conn;

	private static final String sqlfile = "src/test/resources/approx/counter_and_recursion.sql";
	private static final String ontologyFile = "src/test/resources/approx/counter_and_recursion.owl";
	private static final String obdaFile = "src/test/resources/approx/counter_and_recursion.obda";
	private static final String rewrittenOntologyFile = "src/test/resources/approx/counter_and_recursion_dlliter.owl";
	private static final String rewrittenOBDAFile = "src/test/resources/approx/counter_and_recursion_extended.obda";

	@Before
	public void setUp() throws Exception {
		/*
		 * Initializing an H2 database with the synthetic data
		 */
		// String driver = "org.h2.Driver";
		String url = "jdbc:h2:mem:questjunitdb";
		String username = "sa";
		String password = "";

		conn = DriverManager.getConnection(url, username, password);
		Statement st = conn.createStatement();

		FileReader reader = new FileReader(sqlfile);
		BufferedReader in = new BufferedReader(reader);
		StringBuilder bf = new StringBuilder();
		String line = in.readLine();
		while (line != null) {
			bf.append(line);
			line = in.readLine();
		}

		st.executeUpdate(bf.toString());
		conn.commit();
		
	}

	@Test
	public void runTests() throws Exception {

		HSHIQOBDAToDLLiteROBDARewriter rewriter = new HSHIQOBDAToDLLiteROBDARewriter(ontologyFile, obdaFile, 5);
        rewriter.rewrite();
		
        OBDAModel newModel = rewriter.getRewrittenOBDAModel();
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(rewrittenOBDAFile);


        OWLOntology rewrittenOntology = rewriter.getRewrittenOntology();
        OWLManager.createOWLOntologyManager().saveOntology(rewrittenOntology,
                new RDFXMLOntologyFormat(),
                new FileOutputStream(rewrittenOntologyFile));

//		org.h2.tools.Server.startWebServer(conn);

		// Creating a new instance of the reasoner
		QuestOWLFactory factory = new QuestOWLFactory();
		factory.setOBDAController(newModel);

		QuestPreferences p = new QuestPreferences();
		p.setCurrentValueOf(QuestPreferences.REWRITE, QuestConstants.TRUE);
		p.setCurrentValueOf(QuestPreferences.REFORMULATION_TECHNIQUE, QuestConstants.TW);
		factory.setPreferenceHolder(p);

		QuestOWL reasoner = factory.createReasoner(rewrittenOntology, new SimpleConfiguration());

		// Now we are ready for querying
		QuestOWLConnection questConn = reasoner.getConnection();
		QuestOWLStatement st = questConn.createStatement();

		String query = "PREFIX : <http://www.semanticweb.org/counter#> "
				+ "SELECT ?x WHERE { ?x :counter ?y. "
				+ "?y :counter ?z."
				+ "?z :counter ?w."
				+ "?w :counter ?v."
				+ "?v a :End. }";
//		String query = "PREFIX : <http://www.semanticweb.org/counter#> "
//				+ "SELECT ?x WHERE { ?x :counter ?y. }";
		
		try {
			
			QuestOWLResultSet rs = st.executeTuple(query);
			assertTrue(rs.nextRow());
			OWLIndividual ind1 = rs.getOWLIndividual("x");
			assertEquals("<http://www.semanticweb.org/counter#1>", ind1.toString());
			

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			try {

			} catch (Exception e) {
				st.close();
				throw e;
			}
			questConn.close();
			reasoner.dispose();
		}
	}

}
