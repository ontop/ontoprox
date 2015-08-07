package org.semanticweb.ontop.beyondql.approximation;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class is intended for optimization of ontologies produced by Clipper.
 * Clipper will create all possible conjunctions in the saturation step. 
 * Some of the conjunctions will be empty concepts due to the
 * disjointness axioms. This transformer will remove all such axioms from the ontology.
 * To know which conjunctions are empty, we use a reasoner.
 * 
 * This will optimize the steps 2-4 of the Compile and Rewrite procedure
 * as the number of fresh concept names introduced in steps 2 and 3 
 * will be smaller, therefore in step 4 the reasoner will need to
 * classify and traverse less concepts.
 *
 */
public class EmptyConjunctionRemover extends OntologyTransformer {

	/**
	 * @param manager
	 * 
	 */
	public EmptyConjunctionRemover(OWLOntologyManager manager) {
		super(manager);
	}

	/**
	 * For the logging
	 */
	private Logger log = LoggerFactory.getLogger(EmptyConjunctionRemover.class);

	
	@Override
	public OWLOntology transform(OWLOntology ontology, IRI outputIRI)
			throws OWLOntologyCreationException {
		log.info("Removing axioms with empty conjunctions ... ");
		
	
		
		log.info("  * Creating a Hermit reasoner and precomputing inferences...");

		/**
		 * Create a Hermit reasoner, load the extended ontology into it,
		 * and precompute the class hierarchy.
		 */
		OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(ontology);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY,
				InferenceType.DISJOINT_CLASSES);

		
		log.info("  * Removing axioms talking about empty conjunctions...");
		Set<OWLAxiom> axioms = new HashSet<>();
		for( OWLAxiom axiom: ontology.getAxioms()) {
			/**
			 * Keep the axiom if it should not be removed
			 */
			if(!isToBeRemovedSubClassOfAxiom(axiom, reasoner)) {
				axioms.add(axiom);
			}
		}
		
		
		/**
		 * Create the output ontology and add the axioms
		 */
		OWLOntology optimizedOntology = ontologyManager.createOntology(axioms, outputIRI);
		log.info("Created an optimized ontology : "
				+ optimizedOntology.getOntologyID().getOntologyIRI() + " with " +
				optimizedOntology.getAxiomCount() + " axioms");

		return optimizedOntology;

	}

	/**
	 * Checks whether axiom can be removed.
	 * 
	 * We consider it can be if the sub class is a conjunction of concepts and it is empty.
	 * Or, if the super class is an existential restriction and it is empty.
	 * 
	 * @param axiom
	 * @param reasoner 
	 * @return
	 */
	private boolean isToBeRemovedSubClassOfAxiom(OWLAxiom axiom, OWLReasoner reasoner) {
		boolean toRemove = false;
		
		if(axiom instanceof OWLSubClassOfAxiom) {
			OWLClassExpression superClass = ((OWLSubClassOfAxiom)axiom).getSuperClass();
			OWLClassExpression subClass = ((OWLSubClassOfAxiom)axiom).getSubClass();
			
			if(subClass instanceof OWLObjectIntersectionOf && !reasoner.isSatisfiable(subClass)) {
				toRemove = true;
			} else if(superClass instanceof OWLObjectSomeValuesFrom && !reasoner.isSatisfiable(superClass)) {
				toRemove = true;
			}
		}
		
		return toRemove;
	}

}
