package inf.unibz.it.dllite.aproximation.semantic;

import java.util.*;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

/******************************************************************************
 * This class implements the step 4 of the Compile and Rewrite
 * procedure.
 * <ul>
 * <li>Step 4 gets as input an owl ontology and returns its DL-LiteR closure,
 * i.e., the set of DL-LiteR axioms entailed by the owl ontology. 
 * </ul>
 * 
 * The constructor for this class requires an OWLOntologyManager to create and
 * manipulate ontologies.
 * 
 * @author Elena Botoeva
 *
 *****************************************************************************/
public class DLLiteRClosure extends OntologyTransformations {

	/**
	 * @param manager
	 * 
	 */
	public DLLiteRClosure(OWLOntologyManager manager) {
		super(manager);
		new_classes = new HashMap<OWLClass, OWLClassExpression>();
	}

	// This set contains the mapping between the new classes that we
	// introduced and its original equivalent descriptions. 
	private HashMap<OWLClass, OWLClassExpression> new_classes;
	// For the logging
	private Logger log = LoggerFactory.getLogger(DLLiteRClosure.class);

	
	/**
	 * Computes the DL-LiteR closure of an input TBox. To do so uses
	 * intermediate fresh names. The output TBox does not use fresh names.
	 * 
	 * @param owl_ont
	 *            an OWL 2 TBox
	 * @param iri_dlliter_ont
	 *            the IRI of the new DL-Lite ontology
	 * 
	 * @return the DL-LiteR closure of owl_ont
	 * 
	 * @throws OWLOntologyCreationException
	 */
	@Override
	public OWLOntology transform(OWLOntology owl_ont, IRI iri_dlliter_ont) 
			throws OWLOntologyCreationException 
	{
		log.info("Computing the DL-LiteR closure of the ontology " + owl_ont.getOntologyID().getOntologyIRI());
				
		// we use this set to keep track of the freshly introduced names
		new_classes = new HashMap<>();
		
		/**
		 * Give names to all basic concepts in order to be able to go through the
		 * class hierarchy
		 */
		OWLOntology extended_owl_ont = giveNamesToBasicConcepts(owl_ont);

		// Create a reasoner factory. In this case, we will use Hermit.
		OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
		// Load the workng ontology into the reasoner.
		OWLReasoner reasoner = reasonerFactory.createReasoner(extended_owl_ont);
		// Asks the reasoner to classify the ontology.
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY,
				InferenceType.OBJECT_PROPERTY_HIERARCHY,
				InferenceType.DATA_PROPERTY_HIERARCHY,
				InferenceType.DISJOINT_CLASSES);

		/**
		 * Collect all entailed DL-LiteR axioms, that is, concept and role
		 * axioms.
		 */
		Set<OWLAxiom> dlliterAxioms = computeEntailedDLLiteRConceptAxioms(reasoner);
		dlliterAxioms.addAll(computeEntailedDLLiteRRoleAxioms(reasoner));
		
		
		/**
		 * Copy over all declaration axioms
		 */
		Set<OWLDeclarationAxiom> declarationAxioms = owl_ont.getAxioms(AxiomType.DECLARATION);
		dlliterAxioms.addAll(declarationAxioms);

		
		/**
		 * Create the dl lite ontology and add the axioms
		 */
		OWLOntology dlliter_ont = ontologyManager.createOntology(dlliterAxioms, iri_dlliter_ont);
		log.info("Created a DL-LiteR ontology : "
				+ dlliter_ont.getOntologyID().getOntologyIRI() + " with " +
				dlliter_ont.getAxiomCount() + " axioms");

		// clear the set of freshly introduced names
		new_classes.clear();

		return dlliter_ont;
	}

	/**************************************************************************
	 * Returns the TBox of the original ontology completed with the definitions
	 * for existential restrictions. That is, returns a complete ontology
	 * corresponding to the input ontology, -- plus a new equivalent class axiom
	 * for the domain and range of every object property in the ontology; --
	 * plus a new equivalent class axiom for the domain of every data property
	 * in the ontology.
	 * <p>
	 * All these new axioms added will help in the classification, to obtain
	 * some further inferences.
	 * <p>
	 * This method does not add the individual axioms or "abox assertions" from
	 * the owl ontology, so that we don't classify them later.
	 * 
	 * @param owl_ont
	 *            the input ontolgy
	 * @return OWLOntology the same input ontology, completed with all the
	 *         equivalent classes axioms
	 **************************************************************************/
	private OWLOntology giveNamesToBasicConcepts(OWLOntology owl_ont) {
		log.info("Building the conservative extension for the basic concepts... ");

		/**
		 * Collect all existential restrictions to be named
		 */
		// construct all domains and ranges of object properties (concepts of
		// the form `ER.T`)
		Set<OWLClassExpression> existentialRestrictionsToBeNamed = constructObjectSomeValuesFrom(
				owl_ont, false);
		// construct all domains of data properties (concepts of the form `min 1
		// P`)
		existentialRestrictionsToBeNamed
				.addAll(constructDataMinCardinalityRestrictions(owl_ont));

		/**
		 * Now create all the corresponding definitions for the existential
		 * restrictions
		 */
		Set<OWLAxiom> axioms = createDefinitionsForComplexExpressions(
				existentialRestrictionsToBeNamed, owl_ont);
		ontologyManager.addAxioms(owl_ont, axioms);

		return owl_ont;
	}

	/**************************************************************************
	 * Adds and existential restriction for every property in the original
	 * ontology.
	 * <p>
	 * The filler of the added existential restriction will be Top, and if
	 * qualified existentials is true, the filler will be every atomic concept
	 * in the original ontology.
	 * <p>
	 * TODO In this method we just added the first nesting level
	 * -someValuesOf(R,T)-. It is possible to go on adding further levels. For
	 * example: someValuesOf(R,someValuesOf(R,T)) In this case we should add all
	 * the possible combinations.
	 * 
	 * @param ont
	 *            the original ontology
	 * @param qualifiedExistentials
	 * @return
	 *************************************************************************/
	private Set<OWLClassExpression> constructObjectSomeValuesFrom(
			OWLOntology ont, boolean qualifiedExistentials) {
		log.info("	* Adding existential restrictions for every object property...");

		OWLClass classThing = ontologyManager.getOWLDataFactory().getOWLClass(
				OWLRDFVocabulary.OWL_THING.getIRI());
		Set<OWLObjectProperty> properties = ont
				.getObjectPropertiesInSignature();
		Set<OWLClass> classes = ont.getClassesInSignature();

		Set<OWLClassExpression> existentialRestrictions = new HashSet<>();
		for (OWLObjectProperty oprop : properties) {
			// add the restriction R some Thing
			OWLObjectSomeValuesFrom res = ontologyManager.getOWLDataFactory()
					.getOWLObjectSomeValuesFrom(oprop, classThing);
			existentialRestrictions.add(res);

			// add the restriction inv(R) some Thing
			res = ontologyManager.getOWLDataFactory()
					.getOWLObjectSomeValuesFrom(
							ontologyManager.getOWLDataFactory()
									.getOWLObjectInverseOf(oprop), classThing);
			existentialRestrictions.add(res);

			if (qualifiedExistentials) {
				// now add for every atomic concept in the ontology
				for (OWLClass clazz : classes) {
					// R some A
					res = ontologyManager.getOWLDataFactory()
							.getOWLObjectSomeValuesFrom(oprop, clazz);
					existentialRestrictions.add(res);

					// inv(R) some A
					res = ontologyManager.getOWLDataFactory()
							.getOWLObjectSomeValuesFrom(
									ontologyManager.getOWLDataFactory()
											.getOWLObjectInverseOf(oprop),
									clazz);
					existentialRestrictions.add(res);
				}
			}
		}
		return existentialRestrictions;
	}

	/**************************************************************************
	 * Adds a data min cardinality restriction for every data property in the
	 * original ontology.
	 * <p>
	 * The form is: min 1 DataProperty
	 * <p>
	 * TODO: add also axioms of the form: min 1 DataProperty DataRange, where
	 * DataRange are the Datatypes in the ontology.
	 * 
	 * @param ont
	 *            the original ontology
	 * @return
	 *************************************************************************/
	private Set<OWLClassExpression> constructDataMinCardinalityRestrictions(
			OWLOntology ont) {
		log.info("	* Adding min cardinality restrictions for every "
				+ "data property ...");

		Set<OWLClassExpression> dataMinCardinalityRestrictions = new HashSet<>();

		Set<OWLDataProperty> dproperties = ont.getDataPropertiesInSignature();
		for (OWLDataProperty dprop : dproperties) {
			// add the restriction without range
			OWLDataMinCardinality res = ontologyManager.getOWLDataFactory()
					.getOWLDataMinCardinality(1, dprop);
			dataMinCardinalityRestrictions.add(res);
			// TODO
			// now add for every data type, or data range valid in DL LITE
		}
		return dataMinCardinalityRestrictions;
	}

	/**
	 * Gives a name to each complex expression by creating a new predicate and
	 * adding an equivalent class axiom
	 * 
	 * @param complexExpressionsToBeNamed
	 * @param owl_ont
	 * @return the new axioms to be added
	 */
	private Set<OWLAxiom> createDefinitionsForComplexExpressions(
			Set<OWLClassExpression> complexExpressionsToBeNamed,
			OWLOntology owl_ont) {

		log.info("	* Adding Named classes for every complex class expression...");

		OWLDataFactory factory = ontologyManager.getOWLDataFactory();

		Set<OWLAxiom> axioms = new HashSet<>();

		int n = 0;
		for (OWLClassExpression expression : complexExpressionsToBeNamed) {
			// create a new class with a name `Fresh#`
			OWLClass new_class = factory.getOWLClass(IRI.create(owl_ont
					.getOntologyID().getOntologyIRI() + "#Fresh_" + n));
			new_classes.put(new_class, expression);

			// create a new equivalent class axiom defining the new class
			OWLEquivalentClassesAxiom new_ax = factory
					.getOWLEquivalentClassesAxiom(new_class, expression);
			axioms.add(new_ax);

			n++;
		}
		return axioms;
	}

	/**
	 * Returns the set of all entailed DL-LiteR concept axioms, that is, three
	 * types of axioms: -- equivalent basic classes axioms, -- basic concept
	 * inclusions, and -- disjointess axioms for basic concepts.
	 * 
	 * We employ the top-down approach: start from Top=Thing and traverse the
	 * concept hierarchy downwards. So that we need only a linear scan as
	 * opposed to the quadratic number of brute-force checks.
	 * 
	 * We exploit the fact that all existential restrictions are given names, as
	 * the reasoner returns as subclasses and equivalent classes only named
	 * classes.
	 * 
	 * @param reasoner
	 *            The reasoner created for the owl ontology extended with
	 *            definitions for existential restrictions
	 * 
	 * @return the set of entailed DL-LiteR concept axioms.
	 */
	private Set<OWLAxiom> computeEntailedDLLiteRConceptAxioms(
			OWLReasoner reasoner) {

		log.info("Computing the basic concept axioms... ");

		/**
		 * we adopt a top-down approach, so we start from TOP = thing and then
		 * recursively handle all classes
		 * 
		 */
		OWLClass classThing = ontologyManager.getOWLDataFactory().getOWLThing();

		/**
		 * For the set of equivalent classes to Thing we call the recursive
		 * method to compute all the interesting axioms
		 */
		Node<OWLClass> equiv_classes = reasoner
				.getEquivalentClasses(classThing);
		Set<OWLAxiom> axioms = computeEntailedDLLiteRConceptAxioms(
				equiv_classes, classThing, reasoner);

		return axioms;
	}

	/**
	 * Returns the set of all entailed DL-LiteR role axioms, that is, three
	 * types of axioms: 
	 *   -- equivalent basic role axioms, 
	 *   -- basic role inclusions, and 
	 *   -- disjointess axioms for basic concepts.
	 * 
	 * We employ the top-down approach: start from top role and traverse the
	 * role hierarchy downwards. So that we need only a linear scan as opposed
	 * to the quadratic number of brute-force checks.
	 * 
	 * @param reasoner
	 *            The reasoner created for the owl ontology extended with
	 *            definitions for existential restrictions
	 * 
	 * @return the set of entailed DL-LiteR role axioms.
	 */
	private Set<OWLAxiom> computeEntailedDLLiteRRoleAxioms(OWLReasoner reasoner) {
		log.info("Computing the basic role axioms... ");

		/**
		 * we adopt a top-down approach, so we start from the top role and then
		 * recursively handle all roles
		 * 
		 */
		OWLObjectProperty topRole = ontologyManager.getOWLDataFactory()
				.getOWLTopObjectProperty();

		/**
		 * For the set of equivalent roles to the topRole we call the recursive
		 * method to compute all the interesting axioms
		 */
		Node<OWLObjectPropertyExpression> equiv_roles = reasoner
				.getEquivalentObjectProperties(topRole);
		Set<OWLAxiom> axioms = computeEntailedDLLiteRObjectPropertyAxioms(
				equiv_roles, topRole, reasoner);

		/**
		 * The same for data properties
		 * 
		 */
		OWLDataProperty topDataProperty = ontologyManager.getOWLDataFactory()
				.getOWLTopDataProperty();
		Node<OWLDataProperty> equiv_data_properties = reasoner
				.getEquivalentDataProperties(topDataProperty);
		axioms.addAll(computeEntailedDLLiteRDataPropertyAxioms(
				equiv_data_properties, topDataProperty, reasoner));

		return axioms;
	}

	/**
	 * For a given set of equivalent classes and their super class, does 4 main
	 * steps: 
	 *   (1) adds equivalent classes axioms 
	 *   (2) for one representative class adds the subclass axiom between it and 
	 *       the super class 
	 *   (3) adds the disjointess classes axioms for the representative class and 
	 *   	 appropriate classes 
	 *   (4) calls itself recursively for each subclass of the representative class
	 * 
	 * We assume that equiv_classes is non-empty.
	 * 
	 * @param equiv_classes
	 * @param superClass
	 * @param reasoner
	 *            The reasoner created for the owl ontology extended with
	 *            definitions for existential restrictions
	 * @return
	 */
	private Set<OWLAxiom> computeEntailedDLLiteRConceptAxioms(
			Node<OWLClass> equiv_classes, OWLClassExpression superClass,
			OWLReasoner reasoner) {

		// The set of DL-Lite axioms to be returned
		Set<OWLAxiom> axioms = new HashSet<>();

		/**
		 * Select one representative class among the equivalent classes. Should be
		 * a class expression in the original signature
		 */
		OWLClassExpression representativeClass = selectRepresentativeClass(equiv_classes);
		OWLClass namedClass = selectNamedClass(equiv_classes);

		/**
		 * Create the equivalent classes axioms for all class expressions in
		 * equiv_classes
		 * 
		 * At the moment we add equivalent classes axioms only for satisfiable classes
		 */
		if (reasoner.isSatisfiable(namedClass)) {
			axioms.addAll(constructDLLiteEquivalentClassesAxioms(equiv_classes));
		}

		/**
		 * Create the subclass axiom between the representative class and the
		 * super class
		 * 
		 * We do not create trivial axioms of the form "\bot \ISA A" or "A \ISA
		 * \top"
		 */
		if (!superClass.isOWLThing() && !representativeClass.isOWLNothing()) {
			axioms.add(ontologyManager.getOWLDataFactory().getOWLSubClassOfAxiom(representativeClass, superClass));
		}

		/**
		 * Create the disjoint class axioms
		 * 
		 * We use namedClass to get the disjoint classes for
		 * performance reasons
		 */
//		NodeSet<OWLClass> disjointClasses = reasoner.getDisjointClasses(namedClass);
//		axioms.addAll(constructDLLiteDisjointClassesAxioms(representativeClass, disjointClasses));

		/**
		 * The recursive part of the method.
		 * 
		 * For each equivalence class of subclasses call the method recursively.
		 * The representative class will be the superClass for each set of
		 * subclasses.
		 * 
		 *	We use namedClass to get the disjoint classes for
		 * performance reasons
		 */
		NodeSet<OWLClass> sub_classes = reasoner.getSubClasses(namedClass, true); // only direct sublasses
		for (Node<OWLClass> equiv_sub_classes : sub_classes) {
			Set<OWLAxiom> naxioms = computeEntailedDLLiteRConceptAxioms(equiv_sub_classes, representativeClass, reasoner);
			axioms.addAll(naxioms);
		}

		return axioms;
	}

	/**
	 * For a given set of equivalent roles and their super role, does 4 main
	 * steps: (1) adds equivalent object properties axioms (2) for one
	 * representative role adds the sub object property axiom between it and the
	 * super role (3) adds the disjoint object properties axioms for the
	 * representative role and appropriate roles (4) calls itself recursively
	 * for each subroles of the representative role
	 * 
	 * We assume that equiv_roles is non-empty.
	 * 
	 * @param equiv_roles
	 * @param superRole
	 * @param reasoner
	 *            The reasoner created for the owl ontology extended with
	 *            definitions for existential restrictions
	 * @return
	 * 
	 * @see #computeEntailedDLLiteRConceptAxioms(Node, OWLClassExpression,
	 *      OWLReasoner), #computeEntailedDLLiteRDataPropertyAxioms(Node,
	 *      OWLDataProperty, OWLReasoner)
	 */
	private Set<OWLAxiom> computeEntailedDLLiteRObjectPropertyAxioms(
			Node<OWLObjectPropertyExpression> equiv_roles,
			OWLObjectPropertyExpression superRole, OWLReasoner reasoner) {
		// The set of DL-Lite axioms to be returned
		Set<OWLAxiom> axioms = new HashSet<>();

		/**
		 * Create the equivalent obj properties axioms for all object properties
		 * in equiv_roles
		 */
		axioms.add(ontologyManager.getOWLDataFactory()
				.getOWLEquivalentObjectPropertiesAxiom(
						equiv_roles.getEntities()));

		/**
		 * Select one representative role among the equivent roles.
		 */
		OWLObjectPropertyExpression representativeRole = selectRepresentativeObjectProperty(equiv_roles);

		/**
		 * Create the subproperty axiom between the representative role and the
		 * super role
		 * 
		 * We do not create trivial axioms of the form "\bot \ISA R" or "R \ISA
		 * \top"
		 */
		if (!superRole.isOWLTopObjectProperty()
				&& !representativeRole.isOWLBottomObjectProperty()) {
			axioms.add(ontologyManager.getOWLDataFactory()
					.getOWLSubObjectPropertyOfAxiom(representativeRole,
							superRole));
		}

		/**
		 * Create the disjoint properties axioms
		 */
		//NodeSet<OWLObjectPropertyExpression> disjointProperties = reasoner.getDisjointObjectProperties(representativeRole);
		//axioms.addAll(constructDLLiteDisjointPropertiesAxioms(representativeRole, disjointProperties));

		/**
		 * The recursive part of the method.
		 * 
		 * For each equivalence class of subroles call the method recursively.
		 * The representative class will be the superRole for each set of
		 * subroles.
		 */
		NodeSet<OWLObjectPropertyExpression> sub_roles = reasoner
				.getSubObjectProperties(representativeRole, true); // only
																	// direct
																	// sublasses
		for (Node<OWLObjectPropertyExpression> equiv_sub_roles : sub_roles) {
			Set<OWLAxiom> naxioms = computeEntailedDLLiteRObjectPropertyAxioms(
					equiv_sub_roles, representativeRole, reasoner);
			axioms.addAll(naxioms);
		}

		return axioms;
	}

	/**
	 * 
	 * @param equiv_roles
	 * @param superRole
	 * @param reasoner
	 * @return
	 * @see #computeEntailedDLLiteRObjectPropertyAxioms(Node,
	 *      OWLObjectPropertyExpression, OWLReasoner)
	 */
	private Set<OWLAxiom> computeEntailedDLLiteRDataPropertyAxioms(
			Node<OWLDataProperty> equiv_roles, OWLDataProperty superRole,
			OWLReasoner reasoner) {
		// The set of DL-Lite axioms to be returned
		Set<OWLAxiom> axioms = new HashSet<>();

		/**
		 * Create the equivalent obj properties axioms for all object properties
		 * in equiv_roles
		 */
		axioms.add(ontologyManager.getOWLDataFactory()
				.getOWLEquivalentDataPropertiesAxiom(equiv_roles.getEntities()));

		/**
		 * Select one representative role among the equivent roles.
		 */
		OWLDataProperty representativeRole = selectRepresentativeDataProperty(equiv_roles);

		/**
		 * Create the subproperty axiom between the representative role and the
		 * super role
		 * 
		 * We do not create trivial axioms of the form "\bot \ISA R" or "R \ISA
		 * \top"
		 */
		if (!superRole.isOWLTopDataProperty()
				&& !representativeRole.isOWLBottomDataProperty()) {
			axioms.add(ontologyManager
					.getOWLDataFactory()
					.getOWLSubDataPropertyOfAxiom(representativeRole, superRole));
		}

		/**
		 * Create the disjoint properties axioms
		 */
		NodeSet<OWLDataProperty> disjointProperties = reasoner
				.getDisjointDataProperties(representativeRole);
		axioms.addAll(constructDLLiteDisjointDataPropertiesAxioms(
				representativeRole, disjointProperties));

		/**
		 * The recursive part of the method.
		 * 
		 * For each equivalence class of subroles call the method recursively.
		 * The representative class will be the superRole for each set of
		 * subroles.
		 */
		NodeSet<OWLDataProperty> sub_roles = reasoner.getSubDataProperties(
				representativeRole, true); // only direct sublasses
		for (Node<OWLDataProperty> equiv_sub_roles : sub_roles) {
			Set<OWLAxiom> naxioms = computeEntailedDLLiteRDataPropertyAxioms(
					equiv_sub_roles, representativeRole, reasoner);
			axioms.addAll(naxioms);
		}

		return axioms;
	}

	/**
	 * Constructs the set of equivalent classes axioms from the set of concept
	 * names.
	 * 
	 * We need a separate method in order to get rid of the fresh concept names,
	 * and use instead the original existential restrictions.
	 * 
	 * @param equiv_classes
	 * @return
	 */
	private Set<OWLAxiom> constructDLLiteEquivalentClassesAxioms(
			Node<OWLClass> equiv_classes) {

		Set<OWLClassExpression> equiv_class_in_orig_signature = new HashSet<>(
				equiv_classes.getSize());

		// First, substitute fresh names by their definitions
		for (OWLClass clazz : equiv_classes) {
			if (new_classes.containsKey(clazz)) {
				OWLClassExpression existentialRestriction = new_classes
						.get(clazz);
				equiv_class_in_orig_signature.add(existentialRestriction);
			} else {
				equiv_class_in_orig_signature.add(clazz);
			}
		}

		// Second, create the equivalent classes axiom
		Set<OWLAxiom> axioms = new HashSet<>();
		if (equiv_class_in_orig_signature.size() > 1) {
			OWLEquivalentClassesAxiom axiom = ontologyManager
					.getOWLDataFactory().getOWLEquivalentClassesAxiom(
							equiv_class_in_orig_signature);
			axioms.add(axiom);
		}

		return axioms;
	}

	/**
	 * From the set of equivalent classes consisting of original concept names
	 * plus the fresh concept names defining existential restrictions, selects
	 * one class. If possible selects an original concept name. Otherwise
	 * returns the definition of some fresh concept name.
	 * 
	 * @param equiv_classes
	 * @return a basic concept in the original signature
	 */
	private OWLClassExpression selectRepresentativeClass(
			Node<OWLClass> equiv_classes) {
		OWLClassExpression selected_class = null;

		// First try to select an original named class
		for (OWLClass clazz : equiv_classes) {
			if (!new_classes.containsKey(clazz)) {
				selected_class = clazz;
				break;
			}
		}

		// Otherwise get the definition of the first class
		if (selected_class == null) {
			for (OWLClass clazz : equiv_classes) {
				if (new_classes.containsKey(clazz)) {
					selected_class = new_classes.get(clazz);
					break;
				}
			}
		}

		return selected_class;
	}

	private OWLObjectPropertyExpression selectRepresentativeObjectProperty(
			Node<OWLObjectPropertyExpression> equiv_properties) {
		OWLObjectPropertyExpression selected_property = null;

		// First try to select a direct property
		for (OWLObjectPropertyExpression prop : equiv_properties) {
			if (!prop.isAnonymous()) {
				selected_property = prop;
				break;
			}
		}

		// if all properties are non-direct, then we choose the first inverse
		// property
		if (selected_property == null) {
			for (OWLObjectPropertyExpression prop : equiv_properties) {
				selected_property = prop;
				break;
			}
		}

		return selected_property;
	}

	private OWLDataProperty selectRepresentativeDataProperty(
			Node<OWLDataProperty> equiv_properties) {
		OWLDataProperty selected_property = null;

		// Select a first property
		for (OWLDataProperty prop : equiv_properties) {
			selected_property = prop;
			break;
		}

		return selected_property;
	}

	
	/**
	 * Selects a named class. Preference is given to original
	 * named classes.
	 * 
	 * @param equiv_classes
	 * @return
	 */
	private OWLClass selectNamedClass(Node<OWLClass> equiv_classes) {
		OWLClass named_class = null;

		// First try to select an original named class
		for (OWLClass clazz : equiv_classes) {
			if (!new_classes.containsKey(clazz)) {
				named_class = clazz;
				break;
			}
		}
		
		if(named_class == null) {
			for (OWLClass clazz : equiv_classes) {
				named_class = clazz;
				break;
			}	
		}
		
		return named_class;
	}

	/**
	 * Constructs the set of disjointess axioms for a given main enitity and the
	 * set of disjoint entities
	 * 
	 * @param mainClass
	 * @param disjointClasses
	 * @return
	 */
	private Set<OWLAxiom> constructDLLiteDisjointClassesAxioms(
			OWLClassExpression mainClass, NodeSet<OWLClass> disjointClasses) {

		Set<OWLAxiom> axioms = new HashSet<>();

		if (mainClass.isOWLNothing())
			return axioms;

		// For each equivalence class choose a representative and
		// if the representative is not OWL Nothing, add a disjoint classes
		// axioms between
		// the main class and the representative
		for (Node<OWLClass> equiv_classes : disjointClasses) {
			OWLClassExpression representative_class = selectRepresentativeClass(equiv_classes);
			if (!representative_class.isOWLNothing()) {
				axioms.add(ontologyManager.getOWLDataFactory()
						.getOWLDisjointClassesAxiom(mainClass,
								representative_class));
			}
		}

		return axioms;
	}

	private Set<OWLAxiom> constructDLLiteDisjointPropertiesAxioms(
			OWLObjectPropertyExpression mainRole,
			NodeSet<OWLObjectPropertyExpression> disjointProperties) {
		Set<OWLAxiom> axioms = new HashSet<>();

		if (mainRole.isOWLBottomObjectProperty())
			return axioms;

		// For each equivalence class choose a representative and
		// if the representative is not a bottom role, add a disjoint object
		// properties axioms between
		// the main role and the representative
		for (Node<OWLObjectPropertyExpression> equiv_roles : disjointProperties) {
			OWLObjectPropertyExpression representative_role = selectRepresentativeObjectProperty(equiv_roles);
			if (!representative_role.isBottomEntity()) {
				axioms.add(ontologyManager.getOWLDataFactory()
						.getOWLDisjointObjectPropertiesAxiom(mainRole,
								representative_role));
			}
		}

		return axioms;
	}

	private Set<OWLAxiom> constructDLLiteDisjointDataPropertiesAxioms(
			OWLDataProperty mainRole,
			NodeSet<OWLDataProperty> disjointProperties) {
		Set<OWLAxiom> axioms = new HashSet<>();

		if (mainRole.isOWLBottomDataProperty())
			return axioms;

		// For each equivalence class choose a representative and
		// if the representative is not a bottom role, add a disjoint data
		// properties axioms between
		// the main role and the representative
		for (Node<OWLDataProperty> equiv_roles : disjointProperties) {
			OWLDataProperty representative_role = selectRepresentativeDataProperty(equiv_roles);
			if (!representative_role.isBottomEntity()) {
				axioms.add(ontologyManager.getOWLDataFactory()
						.getOWLDisjointDataPropertiesAxiom(mainRole,
								representative_role));
			}
		}

		return axioms;
	}


}
