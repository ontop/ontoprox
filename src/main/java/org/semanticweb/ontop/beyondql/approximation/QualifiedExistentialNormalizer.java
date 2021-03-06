package org.semanticweb.ontop.beyondql.approximation;

import java.util.*;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/******************************************************************************
 * This transformer class implements the step 2 of the Compile and Rewrite
 * procedure. Step 2 is the additional normalization step that takes as input a
 * normalized Horn-ALCHIQ TBox and substitutes qualified existentials on the
 * right-hand side of concept inclusions with appropriate Dl-LiteR axioms. 
 * Namely, for each occurrence of qualified existential restriction
 * on the right-hand side of concept inclusions
 *  
 * <pre> 
 * 		C \ISA ∃R.(A1 \AND ... \AND An)
 * </pre>
 * 
 * We introduce a fresh role name P and substitute the original axioms with
 * the axioms:
 * 
 * <pre>
 * 		C \ISA ∃P.Top 
 * 		P \ISA R 
 * 		∃P^- \ISA A1 
 * 			... 
 * 		∃P^- \ISA An
 * </pre>
 * 
 * 
 * The constructor for this class requires an OWLOntologyManager to create and
 * manipulate ontologies.
 *
 *****************************************************************************/
public class QualifiedExistentialNormalizer extends OntologyTransformer {

	/**
	 * 
	 * @param manager
	 */
	public QualifiedExistentialNormalizer(OWLOntologyManager manager) {
		super(manager);
	}

	/**
	 * For the logging
	 */
	private Logger log = LoggerFactory.getLogger(QualifiedExistentialNormalizer.class);
	
	
	/** 
	 * The string used when creating fresh role names
	 */
	private static final String roleFillerSeparator = "__";


	/**
	 * The normalization that gets rid of qualifiend existential restrictions on
	 * the right-hand side of concept inclusions by introducing fresh role names
	 * for each concept of the from ER.(A1 \AND ... \AND An).
	 * 
	 * We assume that ontology is in Horn-ALCHIG normal form. Therefore, it is
	 * enough to consider only SubClassOf axioms (no EquivalentClasses, no
	 * Domain, nor Range axioms).
	 * 
	 * @param ontology
	 *            in Horn-ALCHIQ normal form
	 * @param outputIRI
	 *            the IRI of the new ontology
	 * 
	 * @return a version of ontology without qualified existentials on the RHS
	 * 
	 * @throws OWLOntologyCreationException
	 */
	public OWLOntology transform(
			OWLOntology ontology, IRI outputIRI)
			throws OWLOntologyCreationException {
		log.info("Normalizing qualified existential restrictions on the RHS of concept inclusions");


		/**
		 * Go through the set of axioms in ontology, and either substitute the
		 * axiom with the new axioms without qualified existential on the RHS,
		 * or keep the original axiom.
		 */
		Set<OWLAxiom> axioms = new HashSet<>();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			if (requiresSubstitutionOfQualifiedExistentialRestrictionOnRHS(axiom)) {
				axioms
						.addAll(substituteQualifiedExistentialRestrictionOnRHS((OWLSubClassOfAxiom) axiom));
			} else {
				axioms.add(axiom);
			}
		}

		/**
		 * Create the output ontology from the set of axioms
		 */
		OWLOntology output_ont = ontologyManager.createOntology(axioms, outputIRI);
		log.info("Created output ontology : "
				+ output_ont.getOntologyID().getOntologyIRI() + " with " +
				output_ont.getAxiomCount() + " axioms");

		return output_ont;
	}

	/**
	 * We do a substitution only if axiom is an #OWLSubClassOfAxiom and the
	 * super class is a qualified restriction where the property is a direct or
	 * inverse property, an the filler is a conjunction of atomic concepts
	 * 
	 * @param axiom
	 * @return
	 */
	private boolean requiresSubstitutionOfQualifiedExistentialRestrictionOnRHS(
			OWLAxiom axiom) {
		boolean requires = false;

		if (axiom instanceof OWLSubClassOfAxiom) {
			OWLClassExpression superClass = ((OWLSubClassOfAxiom) axiom)
					.getSuperClass();

			if (superClass instanceof OWLObjectSomeValuesFrom) {
				OWLClassExpression filler = ((OWLObjectSomeValuesFrom) superClass)
						.getFiller();
							
				/**
				 * Filler must not be Thing
				 */
				if(filler.isOWLThing()){
					/** does nothing */
				}
				/**
				 * Filler can be a concept name or
				 */
				else if (filler instanceof OWLClass) {
					requires = true;
				}
				/**
				 * a conjunction of concept names
				 */
				else if (filler instanceof OWLObjectIntersectionOf) {
					requires = true;
					for (OWLClassExpression clazz : ((OWLObjectIntersectionOf) filler)
							.getOperands()) {
						if (!(clazz instanceof OWLClass)) {
							requires = false;
						}
					}
				}
			}
		}

		return requires;
	}

	/**
	 * It is assumed that the axiom was checked and it requires substitution.
	 * Therefore, we assume that axiom is of the form
	 * <pre> 
	 * 		C \ISA ∃R.(A1 \AND ... \AND An)
	 * </pre>
	 * We introduce a fresh role name P and return the axioms:
	 * <pre>
	 * 		C \ISA ∃P.Top 
	 * 		P \ISA R 
	 * 		∃P^- \ISA A1 
	 * 			... 
	 * 		∃P^- \ISA An
	 * </pre>
	 * @param axiom
	 * @return
	 */
	private Set<OWLAxiom> substituteQualifiedExistentialRestrictionOnRHS(
			OWLSubClassOfAxiom axiom) {
		Set<OWLAxiom> axioms = new HashSet<>();

		OWLClassExpression superClass = ((OWLSubClassOfAxiom) axiom)
				.getSuperClass();
		OWLClassExpression subClass = ((OWLSubClassOfAxiom) axiom)
				.getSubClass();

		OWLObjectPropertyExpression prop = ((OWLObjectSomeValuesFrom) superClass)
				.getProperty();
		OWLClassExpression filler = ((OWLObjectSomeValuesFrom) superClass)
				.getFiller();

		
		/**
		 * Create a fresh role name
		 */
		IRI new_role_iri = IRI.create(IRIUtils.extractPrefix(prop)  
				+ IRIUtils.extractPredicateName(prop) + roleFillerSeparator 
				+ IRIUtils.extractConceptNames(filler));
		OWLObjectProperty new_role = ontologyManager.getOWLDataFactory()
				.getOWLObjectProperty(new_role_iri);

		/**
		 * Add the axiom C \ISA ∃P.Top
		 */
		axioms.add(ontologyManager.getOWLDataFactory().getOWLSubClassOfAxiom(
				subClass,
				ontologyManager.getOWLDataFactory().getOWLObjectSomeValuesFrom(
						new_role,
						ontologyManager.getOWLDataFactory().getOWLThing())));

		/**
		 * Add the axiom P \ISA R
		 */
		axioms.add(ontologyManager.getOWLDataFactory()
				.getOWLSubObjectPropertyOfAxiom(new_role, prop));

		/**
		 * Add the axioms ∃P^- \ISA Ai
		 */
		if (filler instanceof OWLClass) {
			axioms.add(ontologyManager.getOWLDataFactory()
					.getOWLObjectPropertyRangeAxiom(new_role, filler));
		} else {
			for (OWLClassExpression clazz : ((OWLObjectIntersectionOf) filler)
					.getOperands()) {
				OWLObjectPropertyRangeAxiom rangeAxiom = ontologyManager
						.getOWLDataFactory().getOWLObjectPropertyRangeAxiom(
								new_role, clazz);
				axioms.add(rangeAxiom);
			}
		}

		return axioms;
	}

}
