package inf.unibz.it.dllite.aproximation.semantic;

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

public class ConjunctionNormalizer extends OntologyTransformations {
	
	private Multimap<OWLClass,OWLClass> newConceptsForConjunctions;
	
	// For the logging
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

		// copy all axioms
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
	 * 		A1,A2,..,An
	 * </pre>
	 * and add the axioms
	 * <pre>
	 * 		A1,A2,..,An \ISA C
	 * 		A1,A2,..,An \ISA A1
	 * 			...
	 * 		A1,A2,..,An \ISA An
	 * </pre>
	 * to newAxioms and add the pairs
	 * <pre>
	 * 		(A1,A2,..,An , Ai)
	 * </pre>
	 * to the map newConceptsForConjunctions.
	 * 
	 * @param axiom
	 * @return
	 */
	private Set<OWLAxiom> constructNewAxiomsForConjunctionOnLHS(
			OWLSubClassOfAxiom axiom) {
		OWLObjectIntersectionOf conjunction = (OWLObjectIntersectionOf)((OWLSubClassOfAxiom) axiom)
				.getSubClass();
		Set<OWLClassExpression> classes = conjunction.getOperands();
		OWLClassExpression superClass= ((OWLSubClassOfAxiom) axiom).getSuperClass();
		
		
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();

		// Get the prefix of the first element in classes
		String prefix = extractPrefix(classes.iterator().next().asOWLClass());
		// Get the string of comma separated concept names in conjunction 
		String newName = extractConceptNamesFromConjunction(conjunction);
		// Create fresh concept A1,A2,..,An \ISA
		OWLClass freshClass = factory.getOWLClass(IRI.create(prefix + newName));

		
		Set<OWLAxiom> newAxioms = new HashSet<>();
		// Add axiom A1,A2,..,An \ISA C
		newAxioms.add(factory.getOWLSubClassOfAxiom(freshClass, superClass));
		// Add axioms A1,A2,..,An \ISA Ai
		for(OWLClassExpression classInConjunction : classes) {
			newAxioms.add(factory.getOWLSubClassOfAxiom(freshClass, classInConjunction));
		}
		
		// Add the pairs (A1,A2,..,An , Ai) to newConceptsForConjunctions
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
