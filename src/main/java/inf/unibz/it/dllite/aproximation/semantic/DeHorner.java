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
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class DeHorner extends OntologyTransformations {
	
	private Set<OWLAxiom> newAxioms;
	private Multimap<OWLClass,OWLClass> newConceptsForConjunctions;
	private OWLOntologyManager ontologyManager;
	
	
	public DeHorner(OWLOntologyManager manager) {
		super();

		ontologyManager = manager;
		newConceptsForConjunctions = null;
		newAxioms = null;
	}
	
	public void processConjunctionOnLHS(OWLOntology ontology){

		if( newAxioms != null && newConceptsForConjunctions != null )
			return;
		
		newAxioms = new HashSet<>();
		newConceptsForConjunctions = ArrayListMultimap.create();
		
		/**
		 * Go through the set of axioms in ontology, and collect the new axioms
		 * introduced for conjunction of atomic concepts on the LHS in
		 * newAxioms, and the map between the fresh atomic concept and the
		 * concepts in the conjunction in newConceptsForConjunctions.
		 */
		for (OWLAxiom axiom : ontology.getAxioms()) {
			if (hasConjunctionOnLHS(axiom)) {
				constructNewAxiomsForConjunctionOnLHS((OWLSubClassOfAxiom) axiom);
			} 
		}

	}

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
	private void constructNewAxiomsForConjunctionOnLHS(
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
	}
	
	public Set<OWLAxiom> getNewAxioms() {
		return newAxioms;
	}
	
	public Multimap<OWLClass,OWLClass> getNewConceptsForConjunctions() {
		return newConceptsForConjunctions;
	}



}
