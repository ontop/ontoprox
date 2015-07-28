package inf.unibz.it.dllite.approximation.semantic;

import static org.junit.Assert.*;
import inf.unibz.it.dllite.aproximation.semantic.QualifiedExistentialNormalizer;

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
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

public class QualifiedExistentialNormalizerTest {
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
		
		OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(classA, factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLObjectIntersectionOf(classB, classC)));
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(axiom);
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		QualifiedExistentialNormalizer normalizer = new QualifiedExistentialNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		assertEquals(4, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}
	
	@Test
	public void test2() throws OWLOntologyCreationException {
		
		OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(classA, factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLThing()));
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(axiom);
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		QualifiedExistentialNormalizer normalizer = new QualifiedExistentialNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		assertEquals(1, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}


	@Test
	public void test3() throws OWLOntologyCreationException {
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classA, 
						factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLObjectIntersectionOf(classB, classB))));
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		QualifiedExistentialNormalizer normalizer = new QualifiedExistentialNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		assertEquals(3, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}

	@Test
	public void test4() throws OWLOntologyCreationException {
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classA, 
						factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLObjectIntersectionOf(classB, classC))));
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classD, 
						factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLObjectIntersectionOf(classB, classC))));
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		QualifiedExistentialNormalizer normalizer = new QualifiedExistentialNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		assertEquals(5, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}
	
	@Test
	public void test5() throws OWLOntologyCreationException {
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classA, 
						factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLObjectIntersectionOf(classB, classC))));
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classD, 
						factory.getOWLObjectSomeValuesFrom(roleS, factory.getOWLObjectIntersectionOf(classB, classC))));
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		QualifiedExistentialNormalizer normalizer = new QualifiedExistentialNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		assertEquals(8, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}
	
	@Test
	public void test6() throws OWLOntologyCreationException {
		
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classA, 
						factory.getOWLObjectSomeValuesFrom(roleR, factory.getOWLObjectIntersectionOf(classB, classC))));
		axioms.add(
				factory.getOWLSubClassOfAxiom(
						classD, 
						factory.getOWLObjectSomeValuesFrom(roleR, classB)));
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		QualifiedExistentialNormalizer normalizer = new QualifiedExistentialNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		assertEquals(7, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}


	@Test
	public void test7() throws OWLOntologyCreationException {
		
		OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(classA, classB);
		Set<OWLAxiom> axioms = new HashSet<>();
		axioms.add(axiom);
		
		OWLOntology ontology = manager.createOntology(axioms, ontologyIRI);
		
		
		QualifiedExistentialNormalizer normalizer = new QualifiedExistentialNormalizer(manager);
		OWLOntology outputOntology = normalizer.transform(ontology, outputOntologyIRI);
		
		assertEquals(1, outputOntology.getAxiomCount());
		
        DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
        System.out.println(renderer.render(outputOntology));
	}

}
