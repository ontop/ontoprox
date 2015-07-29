package org.semanticweb.ontop.beyondql.hornshiq;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
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
import it.unibz.krdb.obda.owlrefplatform.core.QuestPreferences;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWL;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLConnection;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLFactory;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLResultSet;
import it.unibz.krdb.obda.owlrefplatform.owlapi3.QuestOWLStatement;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

public class CompileAndRewriteTest {

	private OBDADataFactory obdaFactory;
	private Connection conn;

	private OBDAModel obdaModel;
	private OWLOntology ontology;

	final String owlfile = "src/test/resources/approx/counter_and_recursion_step4.owl";
	final String obdafile = "src/test/resources/approx/counter_and_recursion.obda";
	final String sqlfile = "src/test/resources/approx/counter_and_recursion.sql";

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
		
		// Loading the OWL file
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		ontology = manager.loadOntologyFromOntologyDocument(new File(owlfile));

		// Loading the OBDA data
		obdaFactory = OBDADataFactoryImpl.getInstance();
		obdaModel = obdaFactory.getOBDAModel();
		
		ModelIOManager ioManager = new ModelIOManager(obdaModel);
		ioManager.load(obdafile);
	}

	@Test
	public void runTests() throws Exception {

		// Creating a new instance of the reasoner
		QuestOWLFactory factory = new QuestOWLFactory();
		factory.setOBDAController(obdaModel);

		QuestPreferences p = new QuestPreferences();
		factory.setPreferenceHolder(p);

		QuestOWL reasoner = factory.createReasoner(ontology, new SimpleConfiguration());

		// Now we are ready for querying
		QuestOWLConnection questConn = reasoner.getConnection();
		QuestOWLStatement st = questConn.createStatement();

		String query = "PREFIX : <http://www.semanticweb.org/counter#> "
				+ "SELECT ?x WHERE { ?x :counter ?y. "
				+ "?y :counter ?z."
				+ "?z :counter ?w."
				+ "?w :counter ?v."
				+ "?v a :End. }";
		
		try {
			
			QuestOWLResultSet rs = st.executeTuple(query);
			assertTrue(rs.nextRow());
			OWLIndividual ind1 = rs.getOWLIndividual("x");
			assertEquals("1", ind1.toString());
			

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
