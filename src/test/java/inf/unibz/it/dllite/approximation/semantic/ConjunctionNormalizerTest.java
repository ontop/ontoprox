package inf.unibz.it.dllite.approximation.semantic;

import static org.junit.Assert.*;
import inf.unibz.it.dllite.aproximation.semantic.ConjunctionNormalizer;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

public class ConjunctionNormalizerTest {
	String prefix = "http://my.org/ontology#";
	IRI iriA = IRI.create(prefix + "A");
	IRI iriB = IRI.create(prefix + "B");
	IRI iriC = IRI.create(prefix + "C");
	IRI iriD = IRI.create(prefix + "D");
	IRI iriR = IRI.create(prefix + "R");
	IRI iriS = IRI.create(prefix + "S");
	IRI iriQ = IRI.create(prefix + "Q");
	IRI ontologyIRI = IRI.create(prefix);
	IRI outputOntologyIRI = IRI.create(prefix+"norm");
	
	OWLClass classA;
	OWLClass classB;
	OWLClass classC;
	OWLClass classD;
	OWLObjectProperty roleR;
	OWLObjectProperty roleS;
	OWLObjectProperty roleQ;

	OWLOntologyManager manager;
	OWLDataFactory factory;

	@Before
	public void setUp() {
		// Create our ontology manager in the usual way.	
		manager = OWLManager.createOWLOntologyManager();
		factory = manager.getOWLDataFactory();
		
		classA = factory.getOWLClass(iriA);
		classB = factory.getOWLClass(iriB);
		classC = factory.getOWLClass(iriC);
		classD = factory.getOWLClass(iriD);
		roleR = factory.getOWLObjectProperty(iriR);
		roleS = factory.getOWLObjectProperty(iriS);
		roleQ = factory.getOWLObjectProperty(iriQ);
	}
	
	@Test
	public void test() throws OWLOntologyCreationException {
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						factory.getOWLObjectIntersectionOf(classA, classB, classC), 
						classD));
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		ConjunctionNormalizer normalizer = new ConjunctionNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		// this normalizer keeps all old axioms, so it should be 5
		assertEquals(5, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}

	@Test
	public void test2() throws OWLOntologyCreationException {
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						factory.getOWLObjectIntersectionOf(classA, classB, classC), 
						classD));
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						factory.getOWLObjectIntersectionOf(classC, classB, classA), 
						factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLThing())));
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		ConjunctionNormalizer normalizer = new ConjunctionNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		// this normalizer keeps all old axioms
		assertEquals(7, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}
	
	@Test
	public void test3() throws OWLOntologyCreationException {
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classA, 
						classD));
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		ConjunctionNormalizer normalizer = new ConjunctionNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		// this normalizer keeps all old axioms
		assertEquals(1, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}

}
