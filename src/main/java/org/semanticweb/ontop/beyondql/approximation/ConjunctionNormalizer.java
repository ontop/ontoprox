package org.semanticweb.ontop.beyondql.approximation;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * This transformer implements the step 3 of the Compile and Rewrite
 * procedure. It takes as input a Horn-ALCHIQ ontology in normal_E form
 * and returns its extension. Namely, for each occurrence of conjunction of 
 * atomic concepts on the left-hand side of concept inclusions
 *  
 * <pre>
 * 		A1 \AND ... \AND An \ISA C
 * </pre>
 * 
 * it creates a fresh concept name "A1_and_..._and_An" and adds the following
 * axioms
 * 
 * <pre>
 * 		A1_and_..._and_An \ISA C
 * 		A1_and_..._and_An \ISA A_i
 * </pre>
 * 
 * To keep track of the fresh concept names, it stores a Multimap
 * newConceptsForConjunctions that maps each fresh concept name
 * A1_and_..._and_An to the set {A1,...,An}.  
 */
public class ConjunctionNormalizer extends OntologyTransformer {
	
	/**
	 * The map for the fresh names and the conjunction of atomic
	 * concepts it was introduced for.
	 */
	private Multimap<OWLClass,OWLClass> newConceptsForConjunctions;
	
	/**
	 * For the logging
	 */
	private Logger log = LoggerFactory.getLogger(ConjunctionNormalizer.class);

	/**
	 * 
	 * @param manager
	 */
	public ConjunctionNormalizer(OWLOntologyManager manager) {
		super(manager);

		newConceptsForConjunctions = null;
	}
	
	
	/**
	 * For each instance of this class, this method can be called only once.
	 * A side effect of this method is the map newConceptsForConjunctions that contains 
	 * the names of all fresh concepts introduced for conjunctions on the LHS and 
	 * the corresponding concepts participating in the conjunction.
	 */
	@Override
	public OWLOntology transform(OWLOntology ontology, IRI outputIRI)
	throws OWLOntologyCreationException
	{
		log.info("Constructing new axioms for the conjunctions of atomic concepts on the LHS of concept inclusions.");
				
		newConceptsForConjunctions = ArrayListMultimap.create();
		
		/**
		 * Go through the set of axioms in ontology, and collect the new axioms
		 * introduced for conjunction of atomic concepts on the LHS in
		 * newAxioms, and the map between the fresh atomic concept and the
		 * concepts in the conjunction in newConceptsForConjunctions.
		 */
		Set<OWLAxiom> newAxioms = new HashSet<>();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			if (hasConjunctionOnLHS(axiom)) {
				newAxioms.addAll(constructNewAxiomsForConjunctionOnLHS((OWLSubClassOfAxiom) axiom));
			} 
		}

		/**
		 * Copy all original axioms
		 */
		newAxioms.addAll(ontology.getAxioms());
		
		/**
		 * Create the output ontology from the set of axioms
		 */
		OWLOntology output_ont = ontologyManager.createOntology(newAxioms, outputIRI);
		log.info("Created output ontology : "
				+ output_ont.getOntologyID().getOntologyIRI() + " with " +
				output_ont.getAxiomCount() + " axioms");

		return output_ont;
	}

	/**
	 * Checks that axiom is a SubClassOfAxiom of the form
	 * <pre>
	 * 		A1 \AND ... \AND An \ISA C
	 * </pre>
	 * where Ai are concept names and n>0.
	 * 
	 * @param axiom
	 * @return
	 */
	private boolean hasConjunctionOnLHS(OWLAxiom axiom) {
		boolean hasConjunction = false;

		if (axiom instanceof OWLSubClassOfAxiom) {
			OWLClassExpression subClass = ((OWLSubClassOfAxiom) axiom)
					.getSubClass();

			if (subClass instanceof OWLObjectIntersectionOf) {
				hasConjunction = true;
				for (OWLClassExpression clazz : ((OWLObjectIntersectionOf) subClass)
						.getOperands()) {
					if (!(clazz instanceof OWLClass)) {
						hasConjunction = false;
						break;
					}
				}
			}
		}

		return hasConjunction;
	}

	/**
	 * We assume that axiom is of the form
	 * <pre>
	 * 		A1 \AND ... \AND An \ISA C
	 * </pre>
	 * We create a fresh concept named
	 * <pre>
	 * 		A1_and_..._and_An
	 * </pre>
	 * and add the axioms
	 * <pre>
	 * 		A1_and_..._and_An \ISA C
	 * 		A1_and_..._and_An \ISA A1
	 * 			...
	 * 		A1_and_..._and_An \ISA An
	 * </pre>
	 * to newAxioms and add the pairs
	 * <pre>
	 * 		(A1_and_..._and_An , Ai)
	 * </pre>
	 * to the map newConceptsForConjunctions.
	 * 
	 * @param axiom
	 * @return
	 */
	private Set<OWLAxiom> constructNewAxiomsForConjunctionOnLHS(
			OWLSubClassOfAxiom axiom) {
		OWLObjectIntersectionOf conjunction = (OWLObjectIntersectionOf) axiom.getSubClass();
		Set<OWLClassExpression> classes = conjunction.getOperands();
		OWLClassExpression superClass= axiom.getSuperClass();
		
		
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();

		/**
		 * Here we try to be consistent with the naming of fresh concepts
		 * by clipper, so that we do not create new names for something
		 * that already exists.
		 */
		
		/**
		 * Get the prefix of the first element in classes
		 */
		String prefix = "http://www.example.org/fresh#";//extractPrefix(classes.iterator().next().asOWLClass());
		/**
		 * Get the string of "_and_" separated concept names in conjunction 
		 */
		String newName = IRIUtils.extractConceptNamesFromConjunction(conjunction);
		/**
		 * Create fresh concept A1_and_..._and_An \ISA
		 */
		OWLClass freshClass = factory.getOWLClass(IRI.create(prefix + newName));

		
		Set<OWLAxiom> newAxioms = new HashSet<>();

        newAxioms.add(factory.getOWLDeclarationAxiom(freshClass));

		/**
		 * Add axiom A1_and_..._and_An \ISA C
		 */
		newAxioms.add(factory.getOWLSubClassOfAxiom(freshClass, superClass));
		/**
		 * Add axioms A1_and_..._and_An \ISA Ai
		 */
		for(OWLClassExpression classInConjunction : classes) {
			newAxioms.add(factory.getOWLSubClassOfAxiom(freshClass, classInConjunction));
		}
		
		/**
		 * Add the pairs (A1_and_..._and_An , Ai) to newConceptsForConjunctions
		 */
		for(OWLClassExpression classInConjunction : classes) {
			newConceptsForConjunctions.put(freshClass, classInConjunction.asOWLClass());
		}
		
		return newAxioms;
	}
	
	/**
	 * Getter for newConceptsForConjunctions. To be used to
	 * extend the mapping
	 * @return
	 */
	public Multimap<OWLClass,OWLClass> getNewConceptsForConjunctions() {
		return newConceptsForConjunctions;
	}




}
