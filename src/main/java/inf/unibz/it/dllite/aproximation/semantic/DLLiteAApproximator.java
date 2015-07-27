package inf.unibz.it.dllite.aproximation.semantic;

import inf.unibz.it.dllite.aproximation.semantic.exception.FunctionalPropertyParticipatesInQualifiedExistentialException;
import inf.unibz.it.dllite.aproximation.semantic.exception.FunctionalPropertySpecializedException;

import java.io.Serializable;
import java.util.*;

import org.semanticweb.owlapi.model.AddAxiom;
//import org.mindswap.pellet.owlapi.PelletReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/******************************************************************************
 * This class returns the Dl-LiteA ontology obtained by semantically approximating 
 * an original OWL ontology. This semantic approximation is done by the public 
 * method "transform". 
 * In order to do this approximation, we must provide:
 * <ul>
 * <li> The original OWL ontology
 * <li> the IRI of the Dl-Lite approximation.
 * </ul>
 *
 * @author Alejandra Lorenzo, Elena Botoeva
 *
 *****************************************************************************/
public class DLLiteAApproximator extends OntologyTransformations {
	
	/**
	 * @param manager ************************************************************************
	 * 
	 **************************************************************************/
	public DLLiteAApproximator(OWLOntologyManager manager) {
		super(manager);
		new_classes = new HashMap<OWLClass,OWLClassExpression>();
		new_NegClasses = new HashMap<OWLClass,OWLClassExpression>();
		processed_classes = new HashSet<OWLClassExpression> ();
	}


	//These two sets contain the mapping between the new classes that we introduced
	//and its original equivalent descriptions. The only difference is that one is
	//for the new negations introduced, and the other is for the rest of the cases.
	private HashMap <OWLClass,OWLClassExpression> new_classes;
	private HashMap <OWLClass,OWLClassExpression> new_NegClasses;
	//This set will be used in the hierarchy, to keep track of the already 
	//processed classes.
	private Set<OWLClassExpression> processed_classes ;
	//For the logging
	private Logger log = LoggerFactory.getLogger(DLLiteAApproximator.class);
	//This matrix will store in each element [i,j] a "1" when the we try to add a
	// subclass axiom between the classes in position i and j in matrix_classes.
	private BitMatrix2D matrix_Porders; 

		
	


	/**************************************************************************
	 * Return the set of the complements of the named classes in the input ontology.
	 * <p> 
	 * The result will be used to introduce in the working ontology, 
	 * for each element in the set, an equivalent classes axiom between the 
	 * complement and a new named class.
	 *    
	 * @param ont the original ontology, to extract the referenced classes
	 * @param factory the OWLDataFactory, to create the corresponding axioms
	 * 
	 * @return the set of negated atomic concepts in the ontology 
	 *************************************************************************/
	private Set<OWLClassExpression> createAtomicNegations (OWLOntology ont, 
									   OWLDataFactory factory){
		log.info("	* Adding Negations for every named class... ");
		
		//for every class add a new concept corresponding to its negation. 
		//then we will add a new equivalent classes axiom between the negation  
		//of the class, and a new class
		Set<OWLClass> classes =  ont.getClassesInSignature();
		
		Set<OWLClassExpression> atomicNegations=new HashSet<>(classes.size());
		for (OWLClass clazz : classes){
			OWLObjectComplementOf negConcept = factory.getOWLObjectComplementOf(clazz);
			atomicNegations.add(negConcept);
		}
		return atomicNegations;
	}

	/**************************************************************************
	 * Adds and existential restriction for every property in the original 
	 * ontology.
	 * <p>
	 * The filler of the added existential restriction will be Top, and 
	 * if qualified existentials is true, the filler will be every atomic   
	 * concept in the original ontology.
	 * <p>
	 * TODO In this method we just added the first nesting level 
	 * 		-someValuesOf(R,T)-. 
	 * 		It is possible to go on adding further levels. 
	 * 		For example: someValuesOf(R,someValuesOf(R,T))
	 * 		In this case we should add all the possible combinations. 
	 * @param ont the original ontology
	 * @param qualifiedExistentials
	 * @return 
	 *************************************************************************/
	
	
	private Set<OWLClassExpression> constructObjectSomeValuesFrom (OWLOntology ont, 
										   boolean qualifiedExistentials){
		log.info("	* Adding existential restrictions for every object property...");

		OWLClass classThing = ontologyManager.getOWLDataFactory().getOWLClass(OWLRDFVocabulary.OWL_THING.getIRI());
		Set<OWLObjectProperty> properties = ont.getObjectPropertiesInSignature();
		Set<OWLClass> classes = ont.getClassesInSignature();
		
		Set<OWLClassExpression> existentialRestrictions = new HashSet<>();
		for (OWLObjectProperty oprop : properties){
			//add the restriction R some Thing
			OWLObjectSomeValuesFrom res = ontologyManager.getOWLDataFactory().getOWLObjectSomeValuesFrom(oprop, classThing);
			existentialRestrictions.add(res);

			//add the restriction inv(R) some Thing
			res = ontologyManager.getOWLDataFactory().getOWLObjectSomeValuesFrom(
					ontologyManager.getOWLDataFactory().getOWLObjectInverseOf(oprop), classThing);
			existentialRestrictions.add(res);

			if( qualifiedExistentials ) {
				//now add for every atomic concept in the ontology
				for (OWLClass clazz : classes){
					// R some A
					res = ontologyManager.getOWLDataFactory().getOWLObjectSomeValuesFrom(oprop,clazz);
					existentialRestrictions.add(res);

					// inv(R) some A
					res = ontologyManager.getOWLDataFactory().getOWLObjectSomeValuesFrom(
							ontologyManager.getOWLDataFactory().getOWLObjectInverseOf(oprop),clazz);
					existentialRestrictions.add(res);
				}
			}
		}
		return existentialRestrictions;
	}
	
	/**************************************************************************
	 * Adds a data min cardinality restriction for every data property in 
	 * the original ontology.
	 * <p>
	 * The form is: min 1 DataProperty
	 * <p>
	 * TODO: add also axioms of the form: min 1 DataProperty DataRange,
	 * where DataRange are the Datatypes in the ontology. 
	 * @param ont the original ontology
	 * @return 
	 *************************************************************************/
	private Set<OWLClassExpression> constructDataMinCardinalityRestrictions (OWLOntology ont){
		log.info("	* Adding min cardinality restrictions for every " +
				"data property ...");
		
		Set<OWLClassExpression> dataMinCardinalityRestrictions = new HashSet<>();
		
		Set<OWLDataProperty> dproperties = ont.getDataPropertiesInSignature();
		for (OWLDataProperty dprop :dproperties){
			//add the restriction without range
			OWLDataMinCardinality res = ontologyManager.getOWLDataFactory().getOWLDataMinCardinality(1, dprop);
			dataMinCardinalityRestrictions.add(res);
			//TODO 
			//now add for every data type, or data range valid in DL LITE
		}
		return dataMinCardinalityRestrictions;
	}
	

	/**************************************************************************
	 * Returns the TBox of the original ontology completed with the definitions 
	 * of complex class expressions.
	 * That is, returns a complete ontology corresponding to the input ontology, 
	 *   -- plus a new equivalent class axiom for every not named class expressions; 
	 *   -- plus a new equivalent class axiom for the some restriction for 
	 *   	every object property in the ontology (and for every named class 
	 *   	in the original ontology);
	 *   -- plus a data min cardinality restriction for every data property in 
	 * 		the original ontology;
	 *   -- plus a new equivalent class axiom for the negation of each named class 
	 *   	in the original ontology.  
	 * <p> 
	 * All these new axioms added will help in the classification, to obtain
	 * some further inferences.
	 * <p> 
	 * This method does not add the individual axioms or "abox assertions" from
	 * the owl ontology, so that we don't classify them later.
	 * 
	 * @param owl_ont the input ontolgy 
	 * @return OWLOntology the same input ontology, completed with all 
	 * the equivalent classes axioms 
	 **************************************************************************/
	private OWLOntology completeOwlOnt(OWLOntology owl_ont) 
	{

		log.info("Building the conservative extension... ");
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();

		/**
		 * Collect all complex class expressions to be named
		 */
		//the ones that appear in the ontology
		Set<OWLClassExpression> complexExpressionsToBeNamed = extractComplexExpressions(owl_ont);
		//construct all possible concepts of the form `ER.A, ER.T`
		complexExpressionsToBeNamed.addAll(constructObjectSomeValuesFrom(owl_ont, true));
		//construct all possible concepts of the form `min 1 P`
		complexExpressionsToBeNamed.addAll(constructDataMinCardinalityRestrictions (owl_ont));
		//construct all possible atomic negations of the form `\NOT A`
		Set<OWLClassExpression> negatedConcepts = createAtomicNegations(owl_ont, factory);

		
		
		log.info("	* Adding Named classes for every complex class expression...");
		
		/**
		 * Now create all the corresponding definitions for the complex class expressions
		 * and for the atomic negations.
		 * 
		 * We need two methods because the names of the new classes 
		 * are different for the two cases 
		 */
		Set<OWLAxiom> axioms = createDefinitionsForComplexExpressions(complexExpressionsToBeNamed, owl_ont);
		createDefinitionsForAtomicNegations(negatedConcepts, owl_ont);
  		
		ontologyManager.addAxioms(owl_ont, axioms);
		
		/**
		 * Remove the individual axioms to optimize classification later
		 * 
		 * should be done after #extractComplexExpressions as it removes ClassAssetionAxioms
		 */
		removeIndividualAxioms(owl_ont);
		
		return owl_ont;
	}//completeOwlOnt 


	/**
	 * Gives a name to each atomic negation by creating a new predicate and 
	 * adding an equivalent class axiom
	 * 
	 * @param negatedConcepts
	 * @param owl_ont
	 * @return
	 */
	private void createDefinitionsForAtomicNegations(Set<OWLClassExpression> negatedConcepts,
			OWLOntology owl_ont) {
		
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();
		
		for (OWLClassExpression negClass: negatedConcepts){
			//create a new class with a name `Not_A`
			OWLClass new_class = factory.getOWLClass(
					IRI.create(owl_ont.getOntologyID().getOntologyIRI() +"#Not_" + 
					String.valueOf(((OWLObjectComplementOf)negClass).getOperand())));
			new_NegClasses.put(new_class, negClass);
			
			//create a new equivalent class axiom defining the new class
			OWLEquivalentClassesAxiom new_ax = factory.getOWLEquivalentClassesAxiom(new_class,negClass);

			//add the axiom to the ontology
			AddAxiom addAxiom = new AddAxiom (owl_ont, new_ax);
			ontologyManager.applyChange(addAxiom);
		}
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
		
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();
		
		Set<OWLAxiom> axioms = new HashSet<>();
		
		int n=0;
		for (OWLClassExpression expression: complexExpressionsToBeNamed){
  			//create a new class with a name `Fresh#`
			OWLClass new_class = factory.getOWLClass(IRI.create(owl_ont.getOntologyID().getOntologyIRI() + "#Fresh_" + n));
			new_classes.put(new_class, expression);
			
			//create a new equivalent class axiom defining the new class
			OWLEquivalentClassesAxiom new_ax = factory.getOWLEquivalentClassesAxiom(new_class,expression);
			axioms.add(new_ax);

			n++;			
		}
		return axioms;
	}


	/**
	 * Extracts all complex class expressions from the owl ontology and 
	 * returns them in a set. This set is supposed to be used later to introduce 
	 * a new named class for each class expression. 
	 *  
	 * @param owl_ont
	 * @return
	 */
	private Set<OWLClassExpression> extractComplexExpressions(OWLOntology owl_ont) {
		Set<OWLClassExpression> complexExpressions = new HashSet<>();
		
		for (OWLLogicalAxiom l_ax : owl_ont.getLogicalAxioms()){
			//class axiom
			if(l_ax instanceof OWLClassAxiom){
				complexExpressions.addAll(extractComplexExpressionsFromClassAxiom((OWLClassAxiom)l_ax));
			}
			//object property axiom
			else if(l_ax instanceof OWLObjectPropertyAxiom){
				complexExpressions.addAll(extractComplexExpressionsFromObjectPropertyAxiom((OWLObjectPropertyAxiom)l_ax));
			}
			//data property axiom
			else if (l_ax instanceof OWLDataPropertyAxiom){
				complexExpressions.addAll(extractComplexExpressionsFromDataPropertyAxiom((OWLDataPropertyAxiom)l_ax));
			}
			//an ABox assertion with a complex class expression 
			else if (l_ax instanceof OWLClassAssertionAxiom){
				if (!(((OWLClassAssertionAxiom)l_ax).getClassExpression() instanceof OWLClass)){
					complexExpressions.add(((OWLClassAssertionAxiom)l_ax).getClassExpression());
				}
			}
		}
		
		return complexExpressions;
	}


	private Set<OWLClassExpression> extractComplexExpressionsFromDataPropertyAxiom(
			OWLDataPropertyAxiom l_ax) {
		Set<OWLClassExpression> complexExpressions = new HashSet<>();
		if(l_ax instanceof OWLDataPropertyDomainAxiom){
			if (!(((OWLDataPropertyDomainAxiom)l_ax).getDomain() instanceof OWLClass)){
				complexExpressions.add(((OWLDataPropertyDomainAxiom)l_ax).getDomain());
			}
		}
		// none of the following axioms makes any difference
		else if(l_ax instanceof OWLDataPropertyRangeAxiom){
		}
		else if (l_ax instanceof OWLSubDataPropertyOfAxiom){
		}
		else if (l_ax instanceof OWLEquivalentDataPropertiesAxiom){
		}
		else if (l_ax instanceof OWLDisjointDataPropertiesAxiom){
		}
		else if (l_ax instanceof OWLFunctionalDataPropertyAxiom){
		}
		return complexExpressions;
	}


	private Set<OWLClassExpression> extractComplexExpressionsFromObjectPropertyAxiom(
			OWLObjectPropertyAxiom l_ax) {
		Set<OWLClassExpression> complexExpressions = new HashSet<>();
		
		//ObjectPropertyDomainAxiom
		if (l_ax instanceof OWLObjectPropertyDomainAxiom){
			if (!(((OWLObjectPropertyDomainAxiom)l_ax).getDomain() instanceof 
					OWLClass)){
				complexExpressions.add(((OWLObjectPropertyDomainAxiom)l_ax).getDomain());
			}
		}
		//ObjectPropertyRangeAxiom
		else if (l_ax instanceof OWLObjectPropertyRangeAxiom){
			if (!(((OWLObjectPropertyRangeAxiom)l_ax).getRange() instanceof 
					OWLClass)){
				complexExpressions.add(((OWLObjectPropertyRangeAxiom)l_ax).getRange());
			}
		}
		else if (l_ax instanceof OWLSubObjectPropertyOfAxiom){
		}
		else if (l_ax instanceof OWLSubPropertyChainOfAxiom){
		}
		else if (l_ax instanceof OWLEquivalentObjectPropertiesAxiom){
		}
		else if (l_ax instanceof OWLDisjointObjectPropertiesAxiom){
		}
		else if (l_ax instanceof OWLInverseObjectPropertiesAxiom){
		}
		else if (l_ax instanceof OWLFunctionalObjectPropertyAxiom){
		}
		else if (l_ax instanceof OWLInverseFunctionalObjectPropertyAxiom){
		}
		else if (l_ax instanceof OWLReflexiveObjectPropertyAxiom){
		}
		else if (l_ax instanceof OWLIrreflexiveObjectPropertyAxiom){
		}
		else if (l_ax instanceof OWLSymmetricObjectPropertyAxiom){
		}
		else if (l_ax instanceof OWLAsymmetricObjectPropertyAxiom){
		}
		else if (l_ax instanceof OWLTransitiveObjectPropertyAxiom){
		}
		return complexExpressions;
	}


	private Set<OWLClassExpression> extractComplexExpressionsFromClassAxiom(
			OWLClassAxiom l_ax) {
		Set<OWLClassExpression> complexExpressions = new HashSet<>();
		
		//subclass axiom
		if (l_ax instanceof OWLSubClassOfAxiom){
			
			//if the subclass is not a named class, not an existential quantification
			//and not a union of them then we give a name to it
			//in general we may need to name not only one class but several 
			//of them due to union
			if (!DLLiteAGrammarChecker.isDlLiteSubClassExpression(((OWLSubClassOfAxiom) l_ax).getSubClass())){
				Set<OWLClassExpression> classesToSave = DLLiteAGrammarChecker.getNotDlLiteSubClassExpression(((OWLSubClassOfAxiom) l_ax).getSubClass());
				return classesToSave;
			}
			
			//if the superclass is not a named class, nor a qualified chain,
			//nor a conjunction of them we give a name to it
			//in the case of a chain qualified by a complex class, we give a name
			//only to this class
			//in general we may need to name not only one class but several 
			//of them due to intersection
			if (!DLLiteAGrammarChecker.isDlLiteSuperClassExpression(((OWLSubClassOfAxiom) l_ax).getSuperClass())){
				Set<OWLClassExpression> classesToSave = DLLiteAGrammarChecker.getNotDlLiteSuperClassExpression(((OWLSubClassOfAxiom) l_ax).getSuperClass());
				return classesToSave;
			}
		}
		//equivalent classes axiom
		else if (l_ax instanceof OWLEquivalentClassesAxiom){
			Set<OWLClassExpression> des = ((OWLEquivalentClassesAxiom)l_ax).getClassExpressions();
			for(OWLClassExpression d: des){
				if (!(d instanceof OWLClass)){
					complexExpressions.add(d);
				}
			}
		}
		//disjoint classes axiom
		else if (l_ax instanceof OWLDisjointClassesAxiom){
			Set<OWLClassExpression> des = ((OWLDisjointClassesAxiom)l_ax).getClassExpressions();
			for(OWLClassExpression d: des){
				if (!(d instanceof OWLClass)){
					complexExpressions.add(d);						
				}
			}
		}
		else if (l_ax instanceof OWLDisjointUnionAxiom){
			// TODO
		}
		return complexExpressions;
	}


	private void removeIndividualAxioms(OWLOntology owl_ont) {
		List<OWLOntologyChange> changes = new  ArrayList<OWLOntologyChange>();

		Set<OWLLogicalAxiom> l_axioms = owl_ont.getLogicalAxioms();
		for (OWLLogicalAxiom l_ax : l_axioms){
			if (l_ax instanceof OWLIndividualAxiom){
				//add the individual axioms (or abox assertions), to a 
				//changes list, that will contain all the axioms to remove
				changes.add(new RemoveAxiom(owl_ont, l_ax));
			}
		}
		
		//apply the changes that remove the individual axioms from the ontology
		ontologyManager.applyChanges(changes);
	}


	/**************************************************************************
	 * Initializes the DL lite ontology. In this phase, only copy all the axioms
	 * that are not logical axioms. Logical axioms will be added in the DL lite
	 * ontology after the classification, except the individual axioms, which
	 * will be copied now, and then removed from the owl ontology. 
	 * This last step involving the individual axioms, is done in order to run
	 * run a faster classification (in case the ontology contains too many 
	 * individuals). 
	 * @param owl_ont the OWL original ontology, to copy all non logical
	 * axioms
	 * @param dllite_ont the Dl Lite ontology
	 * @throws OWLOntologyStorageException thrown by the addAxiom method 
	 * @throws OWLOntologyChangeException thrown by the addAxiom method
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager) 
	 *************************************************************************/
	private void initializeDlLiteOnt(OWLOntology owl_ont, OWLOntology dllite_ont) 
	throws OWLOntologyChangeException, OWLOntologyStorageException
	{
		log.info("Initializing the Dl Lite ontology...");
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();
		OWLObjectDuplicator ob = new OWLObjectDuplicator(factory);

		//For every non logical axiom in the owl ontology that is valid in 
		//Dl Lite, copy it in the Dl Lite ontology. The logical axioms will 
		//be infered
		Set<OWLAxiom> axioms = owl_ont.getAxioms();
		for (OWLAxiom ax : axioms){
			if (!(ax instanceof OWLLogicalAxiom)){
				OWLAxiom new_ax = ob.duplicateObject(ax);
				//call a method that check if the axiom is already in the 
				//dl-lite ontology and if not, adds the axiom
				addAxiomToDLLite(new_ax.getNNF(), dllite_ont, ontologyManager);
			}
			//With the individual axioms, or abox assertions, we copy the
			//valid ones in the Dl Lite ontology, and then we will delete
			//them from the working ontology
			else if (ax instanceof OWLIndividualAxiom){
				OWLAxiom new_ax = ob.duplicateObject(ax);
				//call a method that check if the axiom is already in the 
				//dl-lite ontology and if not, adds the axiom
				addAxiomToDLLite(new_ax.getNNF(), dllite_ont, ontologyManager);
			}
		}
	}

	
	/**************************************************************************
	 * Adds the input axiom in the Dl Lite ontology, only if it is valid in Dl
	 * Lite, and if it was not previously added.
	 * 
	 * @param new_axiom the new axiom to added in the Dl Lite ontology
	 * @param dllite_ont the Dl Lite ontology
	 * @param man the ontology Manager
	 **************************************************************************/
	private static void addAxiomToDLLite(OWLAxiom new_axiom, OWLOntology dllite_ont, OWLOntologyManager man) 
	{
		boolean isDlLite = DLLiteAGrammarChecker.isdlLiteAxiom(new_axiom.getNNF());
		boolean NoExists = (!dllite_ont.containsAxiom(new_axiom.getNNF())); 
		if (NoExists && isDlLite){
				//log.debug("Adding the axiom: " + new_axiom );
				AddAxiom addAxiom = new AddAxiom(dllite_ont, new_axiom.getNNF());
				// We now use the manager to apply the change
				man.applyChange(addAxiom);
				
				// commented save
				//man.saveOntology(dlLiteOnt,URI_dl_ont);
		}
		else{
			String reason= ""; //for the log only
			if (!isDlLite){
				reason = "Is not valid in Dl-Lite. ";
			}
			if (!NoExists){
				reason = reason + "Already Exists in the Dl Lite ontology.";
			}
			//log.debug("Didn't add Axiom: "+ new_axiom.getNNF());
			//log.debug( " Reason: " + reason);
		}
	}
	

	/**************************************************************************
	 * Adds in the Dl lite ontology, the inconsistent class axioms inferred by  
	 * the reasoner after the classification.
	 * <p>
	 * In order to do this, we create an inconsistent class, 
	 * <p>				INC subClassOf ComplementOf(INC),
	 * <p>
	 * then we get all inconsistent classes in the owl ontology, and we make 
	 * each inconsistent class equivalent to the new inconsistent class. 
	 * @param dl_ont the Dl Lite ontology
	 * @param owl_ont the OWL complete ontology, were we will 
	 * infere the inconsistent classes.
	 * @param reasoner the OWLReasoner used to get the inconsistent classes
	 * @param ob the OWLObjectDuplicator, to duplicate the classes to add as 
	 * inconsitent classes in the Dl Lite ontology 
	 * @throws OWLOntologyStorageException thrown by the addAxiom method
	 * @throws OWLOntologyChangeException thrown by the addAxiom method
	 * @throws  when inferring the inconsistent classes 
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 *************************************************************************/
	private void addDlLiteInconsistentClassesAxioms (OWLOntology dl_ont, 
													 OWLOntology owl_ont, 
													 OWLReasoner reasoner,
													 OWLObjectDuplicator ob ) 
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException		   
	{
		OWLDataFactory factory = ontologyManager.getOWLDataFactory();
		
		//create an inconsistent class INC which is subclass of NOT INC
		OWLClass incClass =	factory.getOWLClass
						(IRI.create(String.valueOf(owl_ont.getOntologyID().getOntologyIRI()) +"#INC"));
		
		//a set of inconsistent classes to create the equivalent classes axiom with INC
		//it is initialized with the inc class which will be equivalent to the 
		//inconsistent classes.
		HashSet<OWLClassExpression> set_inc = new HashSet<OWLClassExpression> ();
		set_inc.add(incClass);
		
		//var to know whether or not we added some axiom with the new INC class.
		boolean addedINC = false; 
		//get all inconsistent classes and add an EquivalentClasses axiom to this new class
		Node<OWLClass> incs =reasoner.getUnsatisfiableClasses();
		//log.debug("inconsitent classes: "+ incs);
		OWLClassExpression aux_desc = null;
		for (OWLClass in: incs){
			//if it is a new class, then work with the equivalent description
			if (new_classes.containsKey(in)){
				aux_desc = new_classes.get(in);
			} 
			else if (new_NegClasses.containsKey(in)){
				aux_desc = new_NegClasses.get(in);
			}else {
				aux_desc = in;
			}
			//create the EquivalentClasses axiom for the description aux_desc and INC
			if (DLLiteAGrammarChecker.isDlLiteSubClassExpression(aux_desc)){
				OWLClassExpression  new_in = ob.duplicateObject(aux_desc);
				// instead of creating the axiom we add the inconsistent class in a set 
				//to create the equivalent classes axiom with INC
				
				set_inc.add(new_in);
				
				addedINC = true;
			}
		}
		if (addedINC){
			//create the axiom that will make INC inconsistent
			OWLClassExpression compInc = factory.getOWLObjectComplementOf(incClass);
			OWLSubClassOfAxiom sub_inc = factory.getOWLSubClassOfAxiom(incClass, compInc);
			DLLiteAApproximator.addAxiomToDLLite(sub_inc,dl_ont, ontologyManager);
			
			//create the equivalent classes axiom between INC and every inconsistent class
			OWLEquivalentClassesAxiom axiom = factory.getOWLEquivalentClassesAxiom(set_inc);
			//add the axiom in the dl lite ontology
			DLLiteAApproximator.addAxiomToDLLite(axiom, dl_ont, ontologyManager);

			
		}
	}
	
	/**************************************************************************
	 * Given an input class and a list of negated ancestors, it creates a 
	 * Disjoint classes axiom between the class and each ancestor.
	 * In order to do this, it check if the input class is a new Negated class,
	 * <ul> 
	 * <li> if it is then it adds it in the list of ancestors to create disjoints.
	 * <li> if it is not, then if it is a valid sub class expression for dl-lite, 
	 *	and if the list of disjoint ancestors contains some ancestor, it  
	 *	creates the disjoint, and delete the ancestors.
	 *	</ul>
	 * @param clazz of type OWLClassExpression, the input description
	 * @param negAncestors_set a set of OWLClassExpression, containing the 
	 * negated ancestors
	 * @param dllite_ont the Dl Lite ontology
	 * @param duplicator an instance of OWLObjectDuplicator 
	 * @return Set of OWLDescriptions, containing the updated negated 
	 * ancestors 
	 * @throws OWLOntologyStorageException thrown by the addAxiom method 
	 * @throws OWLOntologyChangeException thrown by the addAxiom method
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 *************************************************************************/
	private Set<OWLClassExpression> addDlLiteDisjoints (OWLClassExpression clazz, 
										Set<OWLClassExpression> negAncestors_set, 
										OWLOntology dllite_ont, 
										OWLObjectDuplicator duplicator) 
	throws OWLOntologyChangeException, OWLOntologyStorageException 
	{
		//If it is a new negated class, add it in the set of ancestors to 
		//create disjoints.
		OWLClassExpression new_clazz = null;
		if (new_NegClasses.containsKey(clazz)){
			OWLClassExpression negclazz = new_NegClasses.get(clazz);
			new_clazz = 
			(OWLClassExpression)duplicator.duplicateObject
							  (((OWLObjectComplementOf)negclazz).getOperand());
			negAncestors_set.clear(); //see given it is a list of ancestors which will be used as disjoints, only use the last one... to test
			negAncestors_set.add(new_clazz);
		}
		else {
			//create the disjoint classes axiom
			//if negAncestors_set is empty, then does nothing
			for (OWLClassExpression neg_anc : negAncestors_set){
				new_clazz = duplicator.duplicateObject(clazz);
				OWLDisjointClassesAxiom axiom = ontologyManager.getOWLDataFactory().getOWLDisjointClassesAxiom(new_clazz,neg_anc);
				DLLiteAApproximator.addAxiomToDLLite(axiom, dllite_ont, ontologyManager);
			}
			//empty the set of ancestors for negated classes
			negAncestors_set.clear();
		}
		return negAncestors_set;
	}
	
	/**************************************************************************
	 * Returns true if the input class "clazz" is Nothing or equivalent to Nothing. 
	 * Used in the method  addDlLiteSubClasses, to know whether or not to create the 
	 * subclass axioms (we won't create subclass axioms for classes equivalent
	 * to nothing).
	 * @param clazz the evaluated input class
	 * @param reasoner an instance of OWLReasoner, used to get the equivalent 
	 * classes of the input class
	 * @return boolean. It returns true if the class is equivalent (or equals)
	 * to nothing, and false in the oppossite case.
	 * @throws , when interacting with the reasoner. 
	 *************************************************************************/
	private static boolean isNothingEquivalentClass (OWLClassExpression clazz, OWLReasoner reasoner) 
	{
		boolean isNothingEq = false;
		if (clazz.isOWLNothing()){
			isNothingEq = true;
		}
		else {
			Node<OWLClass> eqs = reasoner.getEquivalentClasses(clazz);
			for (OWLClass eq : eqs){
				if (eq.isOWLNothing()){
					isNothingEq = true;
				}
			}
		}
		return isNothingEq;
	}
	
	
	/**************************************************************************
	 * Receives as input a class ("clazz") and a set of ancestors, and for each
	 * ancestor, it adds to the bit matrix (containing the class hierarchy): 
	 * <ul><li>"true" in the position corresponding to the pair 
	 * (clazz,ancestor), only if clazz is a valid sublcass expression
	 * and the ancestor a valid superclass expression for DL Lite.</ul> 
	 * After adding the value in the matrix, the ancestor is deleted from the list 
	 * <p>
	 * Also, if clazz is a valid superclass expression, it is added
	 * to the ancestors set, to be used later to add in the matrix the subclass 
	 * between clazz and its subclasses.
	 * <p>
	 * Returns the updated set of ancestors. 
	 * 
	 * @param clazz the class being visited it is an instance of OWLClassExpression 
	 * @param ancestors_set the set of ancesotors of
	 * the current clazz
	 * @param reasoner an instance of OWLReasoner, used to get the equivalent  
	 * classes of the current clazz when calling to the method 
	 * isNothingEquivalentClass
	 * 
	 * @return Set<OWLClassExpression>. The set of ancestors, updated.
	 *
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 *************************************************************************/
	private Set<OWLClassExpression> addDlLiteSubClasses (OWLClassExpression clazz, 
											Set<OWLClassExpression> ancestors_set, 
											OWLReasoner reasoner) 
	{
		//don't add subclass axioms for Nothing or for classes equivalent to nothing
		if (!DLLiteAApproximator.isNothingEquivalentClass(clazz, reasoner)){
			//if the  description received is a valid sub-class expression 
			//in dl-lite, then we add a sub class axiom for every ancestor
			if(DLLiteAGrammarChecker.isDlLiteSubClassExpression(clazz) && !ancestors_set.isEmpty()){
				for (OWLClassExpression sup: ancestors_set){
					if (!sup.equals(clazz)){
						//Instead of adding the axiom, we will add a "true" in the bit matrix
						//matrix_classes[i,j] where i is the position of the subclass, and 
						//j is the position of the superclass in the matrix_classes hashMap
						matrix_Porders.setD(clazz,sup,true);
					}
				}
				ancestors_set.clear();
			}
			//if it is a valid super-class expression in dl-lite,
			//add it to the ancestors list, so it will be added later on in 
			//a subclass axiom,but as father 
			if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(clazz)){
				ancestors_set.add(clazz);
			}
			
		} else {
			ancestors_set.clear();
		}
		return ancestors_set;
	}

	/**************************************************************************
	 * Receives a set of equivalent descriptions, and creates equivalent classes 
	 * axioms when the descriptions are valid subclass expressions for Dl Lite,
	 * or subclass axioms for those descriptions that are not valid subclass 
	 * expressions but that are valid superclass expressions in Dl Lite.
	 * <p>  
	 * Returns a class selected from the input set of equivalent classes. This
	 * class will be used to go on working with the hierarchy. In order to 
	 * select the class, first picks the first one, and then preferes the 
	 * most simple class, i.e. if there is a named one, it chooses it.
	 * 
	 * @param equivClasses the set of equivalent classes
	 * @param clazz the father class, just to compare that any 
	 * of the the classes is equal to the father class. 
	 * @param dllite_ont the Dl Lite ontology
	 * @param duplicator an instance of OWLObjectDuplicator 
	 * 
	 * @return OWLClassExpression, the class that was selected from the set of
	 * equivalent classes
	 * 
	 * @throws OWLOntologyStorageException thrown by the addAxiom method
	 * @throws OWLOntologyChangeException thrown by the addAxiom method
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 *************************************************************************/
	private OWLClassExpression addDlLiteEquivalentClassesAxioms 
													(boolean firstime,
													 Set<OWLClass> equivClasses,
													 OWLClassExpression clazz,
													 OWLOntology dllite_ont, 
													 OWLObjectDuplicator duplicator) 
	throws OWLOntologyChangeException, OWLOntologyStorageException
	{
		//just in case we are calling with a set of classe equivalent to
		//thing, we add thing in the set of eq. classes...
		if (firstime){
			equivClasses.add((OWLClass)clazz);
		}
		
		Set<OWLClassExpression> sub_classes= new HashSet<OWLClassExpression> ();
		Set<OWLClassExpression> super_classes = new HashSet<OWLClassExpression> ();
		OWLClassExpression new_eq = null;
		OWLClassExpression selected_class = null;
		
		for (OWLClass sub_class : equivClasses){
			
			if (!sub_class.equals(clazz) || firstime){
				
				//select initially the first class in the set
				if (selected_class == null) {
					//if the selected class is a new class, the select instead its 
					//equivalent description
					if (new_classes.containsKey(sub_class)){
						selected_class = new_classes.get(sub_class);
					}
					//if it is a new_negated class, the same
					else if (new_NegClasses.containsKey(sub_class)) {
						selected_class = new_NegClasses.get(sub_class);
					}
					else { //it is a named class
						selected_class = sub_class;
					}
				}
				
				/**
				 * 
				 * If the class in the set is a valid subclass expression in 
				 * DL Lite, then add to the set of subclasses.
				 * 
				 * Else, if it is a valid super class expression in Dl Lite, 
				 * then add the class to the set of superclasses.
				 * 
				 * In order to do it, first ask if it is a new class, and, in 
				 * case it is, get the equivalent description.
				 * 
				 */
				if (new_classes.containsKey(sub_class)){
					// if it is a new (auxiliary) class (not a negated one), then 
					//add its equivalent description in the corresponding set
					if (DLLiteAGrammarChecker.isDlLiteSubClassExpression
												 (new_classes.get(sub_class))){
					  new_eq = duplicator.duplicateObject(new_classes.get(sub_class));
					  sub_classes.add(new_eq);
					  //if the chosen class so far, is not a valid subclass expression
					  //in dl-lite, replace it with the subclass expression
					  if (!DLLiteAGrammarChecker.isDlLiteSubClassExpression
							  								(selected_class)){
							selected_class = new_classes.get(sub_class);
					  }
						
					}// if it is not a subclass expression, check for 
					//superclass expression
					else if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression
												 (new_classes.get(sub_class))){
						new_eq = duplicator.duplicateObject(new_classes.get(sub_class));
						super_classes.add(new_eq);
					}
				}
				//if it is not neither a new class nor a new negated class, 
				//it is a named class,add it to the set of subclasses and  
				//select it as the selected class to go on working
				else if ((!new_classes.containsKey(sub_class))&& 
						 (!new_NegClasses.containsKey(sub_class))){
					sub_classes.add(sub_class);
					selected_class = sub_class;
				}
			}
		}
		//create the equivalent classes axioms with all subclass expressions
		if (sub_classes.size()>1){
			OWLEquivalentClassesAxiom axiom = ontologyManager.getOWLDataFactory().getOWLEquivalentClassesAxiom(sub_classes);
			DLLiteAApproximator.addAxiomToDLLite(axiom, dllite_ont, ontologyManager);
		}
		sub_classes.clear();

		//create the subclass axioms for the superclass expression 
		//use the selected class to work as subclass,
		//if in the set of equivalent classes there were no subclass expression
		//then won't create any subclass axiom
		if ((!(selected_class == null)) &&
			(!new_NegClasses.containsKey(selected_class))){
			OWLClassExpression new_sub = duplicator.duplicateObject(selected_class);
			for (OWLClassExpression sup : super_classes){
				if (!sup.equals(selected_class)){
					OWLSubClassOfAxiom axiom = ontologyManager.getOWLDataFactory().getOWLSubClassOfAxiom(new_sub, sup);
					addAxiomToDLLite(axiom, dllite_ont, ontologyManager);
				}
			}
		}
		return selected_class;
	}
	
	
	/**************************************************************************
	 * A recursive function that goes through the owl ontology hierarchy 
	 * inferred by the reasoner, and adds the subclass, equivalent classes and 
	 * disjoint classes axioms to the Dl Lite ontology.
	 * <p>
	 * When retreiving the sublcass of the clazz, it returns a set of sets,
	 * where each subset contains equivalent classes. So, we use only one class
	 * in each set to go on with the hierarchy. The method that adds the 
	 * equivalent classes axioms in the Dl Lite ontology, is the one that 
	 * selects the class. 
	 * 
	 * @param clazz the class being visited
	 * @param ancestors_set the set of ancestors of the
	 * current clazz with which we will create subclass axioms.
	 * @param negAncestors_set the set of ancestors of the
	 * current clazz with which we will create disjoint classes axioms.
	 * @param dllite_ont the Dl Lite ontology
	 * @param reasoner the reasoner, an instance of OWLReasoner used to get 
	 * the subclasses of the current class, and go on with the hierarchy
	 * @param duplicator the object duplicator
	 * 
	 * @throws  thrown by the invoqued method 
	 * addDlLiteSubClasses
	 * @throws OWLOntologyStorageException  thrown by the invoqued methods 
	 * addDlLiteDisjoints,addDlLiteSubClasses,addDlLiteEquivalentClassesAxioms
	 * @throws OWLOntologyChangeException  thrown by the invoqued methods
	 * addDlLiteDisjoints,addDlLiteSubClasses,addDlLiteEquivalentClassesAxioms
	 *************************************************************************/
	private void addDlLiteHierarchy(OWLClassExpression clazz, 
									Set<OWLClassExpression> ancestors_set,
									Set<OWLClassExpression> negAncestors_set,
									OWLOntology dllite_ont,   
									OWLReasoner reasoner,
									OWLObjectDuplicator duplicator) 
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException		    
	{
		//Add equivalent class axioms for Thing
		if (clazz.isOWLThing()){
			Node<OWLClass> sete = reasoner.getEquivalentClasses(clazz);
			this.addDlLiteEquivalentClassesAxioms
											(true, sete.getEntities(), clazz, 
											dllite_ont, duplicator);
		}
		

		//creates the disjoints, and get the ancestors for disjoints.
		if (!DLLiteAApproximator.isNothingEquivalentClass(clazz, reasoner)){
			negAncestors_set = this.addDlLiteDisjoints(clazz, negAncestors_set, 
					   dllite_ont, duplicator);
		}
		
		//Create the subclass axioms, and get the ancestors set.
		if (!new_NegClasses.containsKey(clazz)){ //To Test: add subclasses axioms only when the class is not a New Negated Class
												// see is this doesn't work change the conversion from neg-class to its equivalent class upwards
			ancestors_set = this.addDlLiteSubClasses(clazz, ancestors_set, reasoner);
		}
		else {
			//if the clazz is an new negated class introduced in the owl ontology, 
			//work with its corresponding description.
			clazz = new_NegClasses.get(clazz);
		}

		
		/**
		 * The recursive part: for each subclass of clazz we build the hierarchy
		 * 
		 * We do it only the clazz has not been yet processed
		 */
		if (!processed_classes.contains(clazz)){
			processed_classes.add(clazz);

			//In any case, get the subclasses and call recursively
			NodeSet<OWLClass> sub_classess = reasoner.getSubClasses(clazz, true);
			//log.debug("sublcases " + sub_classess);
			for (Node<OWLClass> set_classes: sub_classess){
				//Create the equivalentClasses axioms, and select one of the
				//classes to go on looking in the hierarchy. For getting 
				//equivalent classes, use the result of the getSubClasses method
				//log.debug("llama equ. con " + set_classes + "  y " + clazz);
				OWLClassExpression selected_class = this.addDlLiteEquivalentClassesAxioms
															(false, set_classes.getEntities(), clazz, 
															 dllite_ont, duplicator);
				// Call recursively
				if (selected_class != null){
					Set<OWLClassExpression> new_ancestors = new HashSet<>(ancestors_set);
					Set<OWLClassExpression> new_negAncestors = new HashSet<>(negAncestors_set);
					this.addDlLiteHierarchy(selected_class, new_ancestors,
												new_negAncestors, dllite_ont, 
												reasoner, duplicator);
				}
				else {
					log.debug("There is no selected_class to get hierachy");
				}
			}
		}
	}

	/**************************************************************************
	 * This method adds the subproperties axioms and also the equivalent 
	 * properties axioms.
	 * <p>
	 * The reasoner getSubObjectProperties method, returns a set of sets,
	 * where the elements of each set are equivalent properties. So we
	 * take advantage of this to create the equivalent properties axioms. 
	 * @param prop the property used to get the subproperties
	 * @param new_prop the duplicated property.
	 * @param reasoner an instance of OWLReasoner
	 * @param dl_ont the Dl Lite ontology
	 * @param duplicator the object duplicator
	 * @throws 
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method 
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	private void addSubObjectPropertyAxioms(OWLObjectProperty prop, 
											OWLObjectProperty new_prop,
											OWLReasoner reasoner,
											OWLOntology dl_ont, 
											OWLObjectDuplicator duplicator) 
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException
	{
		// We get all subproperties, not only the direct ones
		NodeSet<OWLObjectPropertyExpression> sub_props = reasoner.getSubObjectProperties(prop, true);//SubProperties(prop);
		for (Node<OWLObjectPropertyExpression> sub_prop_set : sub_props){
			//each element in the set is an equivalent property, so
			//will use this to add equivalent property axioms
			//create equivalent properties axioms with the properties 
			OWLEquivalentObjectPropertiesAxiom eq_axiom = ontologyManager.getOWLDataFactory().getOWLEquivalentObjectPropertiesAxiom(sub_prop_set.getEntities());
			addAxiomToDLLite(eq_axiom, dl_ont, ontologyManager);
			OWLObjectProperty new_sub= null;
			for (OWLObjectPropertyExpression sub_prop : sub_prop_set){
				//pick one property from the set
				new_sub= duplicator.duplicateObject(sub_prop);
				break;
			}
			if (!new_sub.equals(null)){
				//create subproperties axiom
				OWLSubObjectPropertyOfAxiom sub_axiom = ontologyManager.getOWLDataFactory().getOWLSubObjectPropertyOfAxiom(new_sub, new_prop);
				addAxiomToDLLite(sub_axiom, dl_ont, ontologyManager);
			}

		}
	}
	
	/**************************************************************************
	 * This method accepts an object property, and adds in the Dl Lite ontology
	 * all the ObjectPropertyDomain Axioms retrieved by the reasoner for the 
	 * input property. Given that the reasoner method for getting the domains
	 * returns a set of sets of descriptions, where the elements in each subset
	 * are equivalent descriptions, we just take one description per set, 
	 * taking care that the selected description is a valid superclass 
	 * expression in Dl Lite.
	 * @param prop the input property
	 * @param new_prop the double of the input property, to create the axiom
	 * @param dl_ont the Dl Lite ontology
	 * @param owl_ont the OWL ontology
	 * @param reasoner the OWLReasoner
	 * @param ob the object duplicator
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @throws , when interacting with the reasoner
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 *************************************************************************/
	private void addObjectPropertyDomainAxiom (OWLObjectProperty prop,
											   OWLObjectProperty new_prop,
											   OWLOntology dl_ont, 
											   OWLOntology owl_ont,  
											   OWLReasoner reasoner,
											   OWLObjectDuplicator ob ) 
	throws OWLOntologyChangeException, 
	OWLOntologyStorageException	 
	{
		//TODO see, just get one domain from the set of equivalent domains.
		OWLClassExpression selected_dom = null;
		OWLClassExpression aux_dom = null; 
		NodeSet<OWLClass> doms = reasoner.getObjectPropertyDomains(prop, false);//getDomains(prop);
		for (Node<OWLClass> set_dom : doms){
			for (OWLClass dom: set_dom){
				//select a dom from the se of equivalent doms obtained.	
				//select initially the first class in the set and then
				//if there is a valid superclass expression assign it
				if (selected_dom == null) {
						selected_dom = dom;
				}
				if (new_classes.containsKey(dom)){
					if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(new_classes.get(dom))){
						selected_dom = new_classes.get(dom);
					}
				}
				else if (new_NegClasses.containsKey(dom)){
					if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(new_NegClasses.get(dom))){
						selected_dom = new_NegClasses.get(dom);
					}
				}
				else if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(dom)){
					selected_dom = dom;
				}
			}
			if (new_classes.containsKey(selected_dom)){
				aux_dom = ob.duplicateObject(new_classes.get(selected_dom));
			}
			else if (new_NegClasses.containsKey(selected_dom)){
				aux_dom = ob.duplicateObject(new_NegClasses.get(selected_dom));
			}
			else {
				aux_dom = ob.duplicateObject(selected_dom);
			}
			
			if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(selected_dom)){
				OWLObjectPropertyDomainAxiom axiom = ontologyManager.getOWLDataFactory().getOWLObjectPropertyDomainAxiom(new_prop, 
															aux_dom);
				addAxiomToDLLite(axiom,dl_ont, ontologyManager);
			}
			selected_dom = null;
		}
		
	}
	
	/**************************************************************************
	 * This method accepts a property and asks the reasoner whether it is a 
	 * functional property. If it is a functional property, then we add the
	 * corresponding axiom only when the functionality is valid in the 
	 * Dl Lite ontology. Otherwise, we show a warning message explaining 
	 * why the functionality is not valid, so that the user can choose 
	 * to include in the Dl Lite ontology either the functionality or
	 * the conflicting axiom.
	 * @param prop the input property
	 * @param new_prop the double of the input property
	 * @param dl_ont the Dl Lite ontology
	 * @param reasoner the OWLReasoner
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @throws , when interacting with the reasoner
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	private void addFunctionalObjectPropertyAxioms(OWLObjectProperty prop,
												   OWLObjectProperty new_prop,
												   OWLOntology dl_ont, 
												   OWLReasoner reasoner ) 
	throws OWLOntologyChangeException, 
	OWLOntologyStorageException	 
	{
		if (prop.isFunctional(reasoner.getRootOntology())){ //reasoner.isFunctional(prop)){
			//TODO see if it is ok
			//create the axiom
			OWLFunctionalObjectPropertyAxiom axiom = ontologyManager.getOWLDataFactory().getOWLFunctionalObjectPropertyAxiom(new_prop);
			//We will check if property fuarg0nctionality is valid in Dl Lite
			//and if it is not, just add a log saying for which properties 
			//either the functionality or some rule can't be added.
			try {
				if(DLLiteAGrammarChecker.isDlLiteFunctionalObjectPropertyAxiom(axiom,
																   dl_ont)){
					//TODO won't add the axiom in case it is not dl lite... see, 
					//in case it is necesary to add anyway the axiom, do it without
					//calling this method...
					addAxiomToDLLite(axiom,dl_ont, ontologyManager);
				}
			} catch (FunctionalPropertySpecializedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.warn("The property " + String.valueOf(new_prop)+ " is " +
						"Functional but, its functionality won't be added " +
						"because it is already specialized in the Dl Lite " +
						"ontology.");
			} catch (FunctionalPropertyParticipatesInQualifiedExistentialException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				log.warn("The property " + String.valueOf(new_prop)+ " is Functional " +
						"but, its functionality won't be added because it already " +
						"participates in a qualified existential in the Dl Lite " +
						"ontology.");
			}
		}
	}
	
	/**************************************************************************
	 * This method adds the inverse properties axioms inferred by the reasoner
	 * for the input property, into the Dl Lite ontology
	 * @param prop the input property
	 * @param new_prop
	 * @param dl_ont the Dl Lite ontology
	 * @param reasoner the OWLReasoner
	 * @param duplicator the object duplicator
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @throws , when interacting with the reasoner
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	private void addInverseObjectPropertiesAxioms (OWLObjectProperty prop,
												   OWLObjectProperty new_prop,
												   OWLOntology dl_ont, 
												   OWLReasoner reasoner,
												   OWLObjectDuplicator duplicator)
	throws OWLOntologyChangeException, 
	OWLOntologyStorageException	 
	{
		Node<OWLObjectPropertyExpression> inv_props = reasoner.getInverseObjectProperties(prop);
		for (OWLObjectPropertyExpression inv_prop : inv_props){
			//for (OWLObjectProperty inv_prop: set_props){
				OWLObjectPropertyExpression new_inv = duplicator.duplicateObject(inv_prop);
				OWLInverseObjectPropertiesAxiom axiom = ontologyManager.getOWLDataFactory().getOWLInverseObjectPropertiesAxiom(new_prop, new_inv);
				addAxiomToDLLite(axiom, dl_ont, ontologyManager);
			//}
		}
	}
	
	/**************************************************************************
	 * This method adds into de Dl Lite ontology, the inferred object property  
	 * range axioms for the input property, only if they are valid in Dl Lite.  
	 * @param prop the input property
	 * @param new_prop the double of the input property
	 * @param dl_ont the Dl Lite ontology
	 * @param reasoner the OWLReasoner
	 * @param duplicator the object duplicator
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @throws , when interacting with the reasoner
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	private void addObjectPropertyRangeAxioms(OWLObjectProperty prop,
											  OWLObjectProperty new_prop,
											  OWLOntology dl_ont, 
											  OWLReasoner reasoner,
											  OWLObjectDuplicator duplicator)
	throws OWLOntologyChangeException, 
	OWLOntologyStorageException	 
	{
		OWLClassExpression aux_range = null;
		NodeSet<OWLClass> ranges = reasoner.getObjectPropertyRanges(prop, false);
		for (Node<OWLClass> range : ranges){
			if (new_classes.containsKey(range)){
				aux_range = duplicator.duplicateObject(new_classes.get(range));
			} else if (new_NegClasses.containsKey(range)){
				aux_range = duplicator.duplicateObject(new_NegClasses.get(range));
			} else {
				aux_range = duplicator.duplicateObject((OWLObject) range.getRepresentativeElement().asOWLClass());
			}
			if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(aux_range)){
				OWLObjectPropertyRangeAxiom axiom = ontologyManager.getOWLDataFactory().getOWLObjectPropertyRangeAxiom(new_prop, 
															   aux_range);
				addAxiomToDLLite(axiom,dl_ont, ontologyManager);
			}
		}

	}
	
	/**************************************************************************
	 * Adds in the Dl Lite ontology the Object Property hierarchy, and other 
	 * inferences made by the reasoner, related with object property objects, 
	 * i.e. inferred: 
	 * domains, ranges, functional object properties, symmetric object	
	 * properties, subproperty axioms, equivalent properties axioms and   
	 * inversed properties axioms.
	 * <p>
	 * For the functional property axioms, it only adds them if they don't 
	 * conflict with other axioms already existing in the Dl lite ontology.
	 * I.e. if in the Dl Lite ontology does not exist any subpropery axiom
	 * where the functional property is on the right side, or if the property
	 * is not included in a some restriction.
	 * @param dl_ont the Dl Lite ontology
	 * @param owl_ont the OWL ontology
	 * @param reasoner the OWLReasoner
	 * @param ob the object duplicator
	 * @throws   
	 * @throws OWLOntologyStorageException thrown by the invoqued method addAxiom
	 * @throws OWLOntologyChangeException by the invoqued method addAxiom
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 *************************************************************************/
	private void addDlLiteObjectProperties (OWLOntology dl_ont, 
											OWLOntology owl_ont,  
											OWLReasoner reasoner,
											OWLObjectDuplicator ob ) 
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException 
	{ 
		//get all the object properties
		Set<OWLObjectProperty>ob_props=
									owl_ont.getObjectPropertiesInSignature();
		//for each one, look its hierarchy... 
		for (OWLObjectProperty prop : ob_props ){
			//not necessary:getDescendantProperties,getAncestorProperties, 
			//getSuperProperties; 
			OWLObjectProperty new_prop = ob.duplicateObject(prop);
			//add hierachy
			addSubObjectPropertyAxioms(prop,new_prop,reasoner, 
									   dl_ont,ob);
			//get domains
			addObjectPropertyDomainAxiom(prop,new_prop,dl_ont,owl_ont,
										 reasoner,ob);
			
			//get functional object properties infered by the reasoner
			addFunctionalObjectPropertyAxioms(prop,new_prop,dl_ont,reasoner);
			//get symmetric  object properties infered by the reasoner
			// TODO check that the reasoner implements isSymmetric
			//reasoner.isDefined(OWLSymmetricObjectPropertyAxiom);
			//try{
				if (prop.isSymmetric(reasoner.getRootOntology())){
					OWLSymmetricObjectPropertyAxiom axiom = 
							ontologyManager.getOWLDataFactory().getOWLSymmetricObjectPropertyAxiom(new_prop);
					addAxiomToDLLite(axiom,dl_ont,ontologyManager);
				}
			//}catch( e1)
			//{
			//	log.debug(e1.getMessage(), e1);
			//}
			//get inverse props
			addInverseObjectPropertiesAxioms(prop,new_prop,dl_ont,reasoner, ob);
			//get ranges
			addObjectPropertyRangeAxioms(prop,new_prop,dl_ont,reasoner,ob);
		}
	}
	

	/**************************************************************************
	 * This method adds the subproperties axioms and also the equivalent 
	 * properties axioms.
	 * <p>
	 * The reasoner getSubDataProperties method, returns a set of sets,
	 * where the elements of each set are equivalent properties. So we
	 * take advantage of this to create the equivalent properties axioms. 
	 * @param prop the property used to get the subproperties
	 * @param new_prop the duplicated property.
	 * @param reasoner the OWLReasoner
	 * @param dl_ont the Dl Lite ontology
	 * @param ob the object duplicator
	 * @throws 
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method 
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	 
	private void addSubDataPropertyAxioms (OWLDataProperty prop,
										   OWLDataProperty new_prop,
										   OWLReasoner reasoner,
										   OWLOntology dl_ont,
										   OWLObjectDuplicator ob)
	throws OWLOntologyChangeException, 
	   	OWLOntologyStorageException 
	{
		NodeSet<OWLDataProperty> sub_props = reasoner.getSubDataProperties(prop, false);
		for (Node<OWLDataProperty> sub_prop_set : sub_props){
			//the elements in the sets are equivalent properties, 
			//add the equivalent properties axioms here
			OWLEquivalentDataPropertiesAxiom eq_axiom = ontologyManager.getOWLDataFactory().getOWLEquivalentDataPropertiesAxiom(sub_prop_set.getEntities());
	  		addAxiomToDLLite(eq_axiom,dl_ont, ontologyManager);
	  		//now get one property in each set to create the subprop axiom
	  		OWLDataProperty new_sub = null;
			for (OWLDataProperty sub_prop : sub_prop_set){
				new_sub = ob.duplicateObject(sub_prop);
				break;
			}
			OWLSubDataPropertyOfAxiom axiom = ontologyManager.getOWLDataFactory().getOWLSubDataPropertyOfAxiom(new_sub,new_prop);
			addAxiomToDLLite(axiom,dl_ont, ontologyManager);
		}
	}

	/**************************************************************************
	 * Accepts an OWLDataProperty, and adds in the Dl Lite ontology
	 * all the data Property Domain Axioms retrieved by the reasoner for the 
	 * input object. Given that the reasoner method for getting the domains
	 * returns a set of sets of descriptions, where the elements in each subset
	 * are equivalent descriptions, we just take one description per set, 
	 * taking care that the selected description is a valid superclass 
	 * expression in Dl Lite.
	 * @param prop the input property
	 * @param new_prop the double of the input property, to create the axiom
	 * @param reasoner the OWLReasoner
	 * @param dl_ont the Dl Lite ontology
	 * @param ob the object duplicator
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @throws , when interacting with the reasoner
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	private void addDataPropertyDomainAxioms(OWLDataProperty prop,
											 OWLDataProperty new_prop,
											 OWLReasoner reasoner,
											 OWLOntology dl_ont,
											 OWLObjectDuplicator ob)
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException 
	{
		//TODO see, just get one domain from the set of equivalent domains.
		NodeSet<OWLClass> doms  = reasoner.getDataPropertyDomains(prop, true);
		OWLClassExpression aux_dom = null;
		OWLClassExpression selected_dom = null;
		for (Node<OWLClass> set_dom : doms){
			for (OWLClassExpression dom: set_dom){
				//select a dom from the se of equivalent doms obtained.	
				//select initially the first class in the set and then
				//if there is a valid superclass expression assign it
				if (selected_dom == null) {
						selected_dom = dom;
				}
				if (new_classes.containsKey(dom)){
					if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(new_classes.get(dom))){
						selected_dom = new_classes.get(dom);
					}
				}
				else if (new_NegClasses.containsKey(dom)){
					if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(new_NegClasses.get(dom))){
						selected_dom = new_NegClasses.get(dom);
					}
				}
				else if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(dom)){
					selected_dom = dom;
				}
			}
			if (new_classes.containsKey(selected_dom)){
				aux_dom = ob.duplicateObject(new_classes.get(selected_dom));
			}
			else if (new_NegClasses.containsKey(selected_dom)){
				aux_dom = ob.duplicateObject(new_NegClasses.get(selected_dom));
			}
			else {
				aux_dom = ob.duplicateObject(selected_dom);
			}
			if (DLLiteAGrammarChecker.isDlLiteSuperClassExpression(aux_dom)){
				OWLDataPropertyDomainAxiom axiom = ontologyManager.getOWLDataFactory().getOWLDataPropertyDomainAxiom(new_prop, 
														  aux_dom);
				addAxiomToDLLite(axiom,dl_ont, ontologyManager);
			}
			selected_dom = null;
		}

	}
	

	/**************************************************************************
	 * This method accepts a property and asks the reasoner whether it is a 
	 * functional property. If it is a functional property, then we add the
	 * corresponding axiom only when the functionality is valid in the 
	 * Dl Lite ontology. Otherwise, we show a warning message explaining 
	 * why the functionality is not valid, so that the user can choose 
	 * to include in the Dl Lite ontology either the functionality or
	 * the conflicting axiom.
	 * @param prop the input property
	 * @param new_prop the double of the input property
	 * @param reasoner the OWLReasoner
	 * @param dl_ont the Dl Lite ontology
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @throws , when interacting with the reasoner
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	private void addFunctionalDataPropertyAxioms (OWLDataProperty prop,
												  OWLDataProperty new_prop,
												  OWLReasoner reasoner,
												  OWLOntology dl_ont)
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException 
	{
		if (prop.isFunctional(reasoner.getRootOntology())){
			//TODO see if it is ok
			//create the axiom
			OWLFunctionalDataPropertyAxiom axiom = ontologyManager.getOWLDataFactory().getOWLFunctionalDataPropertyAxiom(new_prop);
			//We will check if property fuarg0nctionality is valid in Dl Lite
			//and if it is not, just add a log saying for which properties 
			//either the functionality or some rule can't be added.
			if (!DLLiteAGrammarChecker.isDlLiteFunctionalDataPropertyAxiom(axiom, 
																 dl_ont)){
				log.warn("The property " + String.valueOf(new_prop)+ " is " +
						"Functional but, its functionality won't be added " +
						"because it is already specialized in the Dl Lite " +
						"ontology."); 
			}
			//TODO won't add the axiom in case it is not dl lite... see, 
			//in case it is necesary to add anyway the axiom, do it without
			//calling this method...

			addAxiomToDLLite(axiom,dl_ont, ontologyManager);
		}

	}


	/**************************************************************************
	 * This method adds the data property range axioms, inferred by the 
	 * reasoner for the input property, into de Dl Lite ontology if they
	 * are valid in Dl Lite.  
	 * @param prop the input property
	 * @param new_prop
	 * @param reasoner the OWLReasoner
	 * @param dl_ont the Dl Lite ontology
	 * @param ob the object duplicator
	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
	 * @throws , when interacting with the reasoner
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 **************************************************************************/
	private void addDataPropertyRangeAxioms(OWLDataProperty prop,
											OWLDataProperty new_prop,
											OWLReasoner reasoner,
											OWLOntology dl_ont,
											OWLObjectDuplicator ob)
	throws OWLOntologyChangeException, 
		OWLOntologyStorageException 
	{
		Set<OWLDataRange> ranges = prop.getRanges(reasoner.getRootOntology());
		for (OWLDataRange range : ranges){
			if (DLLiteAGrammarChecker.isDlLiteDataRange(range)){
				OWLDataRange new_range = ob.duplicateObject(range);
				OWLDataPropertyRangeAxiom axiom = ontologyManager.getOWLDataFactory().getOWLDataPropertyRangeAxiom(new_prop, 
																	 new_range);
				addAxiomToDLLite(axiom,dl_ont, ontologyManager);
			}
		}

	}

	/**************************************************************************
	 * Adds in the Dl Lite ontology the Data Property hierarchy, and other 
	 * inferences made by the reasoner, related with data property objects, 
	 * i.e. inferred: 
	 * domains, ranges, functional object properties, subproperty axioms and
	 * equivalent properties axioms.
	 * <p>
	 * For the functional property axioms, it only adds them if they don't 
	 * conflict with other axioms already existing in the Dl lite ontology.
	 * I.e. if in the Dl Lite ontology does not exist any subpropery axiom
	 * where the functional property is on the right side.
	 * @param dl_ont the Dl Lite ontology
	 * @param owl_ont the OWL ontology
	 * @param reasoner the OWL Reasoner
	 * @param ob the object duplicator
	 * @throws  
	 * @throws OWLOntologyStorageException thrown by the invoqued method addAxiom
	 * @throws OWLOntologyChangeException by the invoqued method addAxiom
	 * @see #addAxiomToDLLite(OWLAxiom,OWLOntology,OWLOntologyManager)
	 *************************************************************************/
	private void addDlLiteDataProperties (OWLOntology dl_ont, 
										  OWLOntology owl_ont,  
										  OWLReasoner reasoner,
										  OWLObjectDuplicator ob ) 
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException
	{
		//get all the object properties
		//Set<OWLDataProperty>d_props = reasoner.getDataProperties(); 
		//not available in OWLReasoner, only in Reasoner
		Set<OWLDataProperty>d_props  = owl_ont.getDataPropertiesInSignature();
		//For each property
		for (OWLDataProperty prop : d_props ){
			OWLDataProperty new_prop = ob.duplicateObject(prop);

			//get the hierarchy
			addSubDataPropertyAxioms (prop,new_prop,reasoner,dl_ont, ob);
			//get domains
			addDataPropertyDomainAxioms(prop,new_prop,reasoner,dl_ont,ob);
			//is functional data property inferred by the reasoner
			addFunctionalDataPropertyAxioms (prop,new_prop,reasoner,dl_ont);
			//get ranges
			addDataPropertyRangeAxioms(prop,new_prop,reasoner,dl_ont,ob);
		}
	}
	/****************************************************************************
	 * This method is to initialize the bit matrix. 
	 * Given that a matrix has a fix number of columns and rows, 
	 * we need to know in advance the size of the matrix. 
	 * So we set it by getting the number of descendants of Thing.
	 * Then we will only use the cells that we need. 
	 * @param clazz this class will be Thing
	 * @param reasoner to get the descendants of Thing
	 * @throws 
	 ***************************************************************************/
	private void initializeBitMatrix(OWLClass clazz,
									 OWLReasoner reasoner) 
	{
		//get the max number of OWLDescriptions that the Dl ontology can contain,
		//in order to create the bitMatrix.
		NodeSet<OWLClass> desc_thing = reasoner.getSubClasses(clazz, false);
		int max_lengh = desc_thing.getFlattened().size(); 
		//now initialize the bit matrix
		matrix_Porders = new BitMatrix2D (max_lengh,max_lengh);
	}
	
	/****************************************************************************
	 * This recursive method will get the descendants of the class corresponding  
	 * to the input index col,  and for each descendant will eliminate it in 
	 * each ancestor.
	 * For example, if we have the following subclass axioms:
	 * (T,A), (T,B), (T,C), (A,C); where T,A,B and C 
	 * are classes, and each pair represents a subclass axiom, then the algorithm 
	 * will eliminate the axiom (T,C) given it is redundant.
	 * @param col the current column of the bit matrix 
	 * @param len the actual size of the bit matrix 
	 * @param ancestors the lis of ancestors that made the recursive call, and 
	 * where I should remove the redundant subclass axioms. 
	 ***************************************************************************/
	private void call_children (int col,
								int len,
								Set<Integer>ancestors ){
		for (int row = 0; row < len; row++){
			if (matrix_Porders.get(row, col)==true){
				for (Integer anc:ancestors){
					matrix_Porders.set(row, anc, false);
					Set<Integer> new_ancestors = 
						(Set<Integer>) ((HashSet<Integer>)ancestors).clone();
					new_ancestors.add(col);
					call_children(row, len, new_ancestors);
					
				}
			}
		}
	}
	
	
	/****************************************************************************
	 * This method minimizes the bit matrix, and inserts the subclass axioms in 
	 * the Dl Lite ontology.
	 * Each element of the matrix M[r,c] will contain true if the class r is 
	 * subclass of the class c. What we want to do is to avoid having redundant axioms
	 * such that: if we have a subclass axiom (A,B), and a subclass axiom (B,C), 
	 * then we don't want to have in the ontology a subclass axiom (A,C).
	 * So, we need to eliminate these redundant axioms. 
	 * The idea of the method is: for each element in the column corresponding a 
	 * Thing, that has the true value (i.e. for each subclass axiom (child,thing)), 
	 * we go through all the descendants of the child by exploring the column 
	 * corresponding to the child. If we find that that a descendant of the child is 
	 * true at Thing, then we make it false (at Thing). We repeat this procedure 
	 * recursively for each descendant until we reach a leaf.
	 * Then, once the matrix is already minimized, we insert in the Dl Lite 
	 * ontology the subclass axioms corresponding to the remaining true values in
	 * the matrix.
	 * @param dlLiteOnt the Dl Lite Ontology
	 * @param factory the factory, to create the axioms
	 * @param ob an instance of OWLObjectDuplicator, used to duplicate the  
	 * objects that will be included in the Dl Lite ontology
	 * @throws OWLOntologyChangeException
	 * @throws OWLOntologyStorageException
	 ***************************************************************************/
	private void addSubClassAxiomsFromMatrix(OWLOntology dlLiteOnt, 
											 OWLObjectDuplicator ob ) 
	throws OWLOntologyChangeException, 
		   OWLOntologyStorageException{
		
		int len = matrix_Porders.get_current_length();
		
		//get the column of Thing
		//we adopt a top-down approach, so we start from TOP = thing
		IRI classURI = OWLRDFVocabulary.OWL_THING.getIRI();
		OWLClass top = ontologyManager.getOWLDataFactory().getOWLClass(classURI);
		int top_col = matrix_Porders.get_class_index(top);
		
		//for each row in the column of thing, we will explore the 
		//children
		for (int top_row = 0; top_row < len; top_row++){
			if (matrix_Porders.get(top_row, top_col)== true){
				Set<Integer> ancestors = new HashSet<Integer> ();
				ancestors.add(top_col);
				call_children(top_row,len, ancestors);
			}
		}
		
		//Finally, create the subclass axiom
		for (int r=0; r<len; r++){
			for (int c=0; c<len; c++){
				if (matrix_Porders.get(r, c)==true){
					OWLClassExpression sub = matrix_Porders.get_class_desc(r);
					OWLClassExpression new_sub= ob.duplicateObject(sub);
					OWLClassExpression sup = matrix_Porders.get_class_desc(c);
		
					OWLClassExpression new_super= ob.duplicateObject(sup);
					OWLSubClassOfAxiom axiom = 
							ontologyManager.getOWLDataFactory().getOWLSubClassOfAxiom(new_sub,new_super);
					addAxiomToDLLite(axiom, dlLiteOnt, ontologyManager);
					
				}
			}
		}
	}

		
   /***************************************************************************
	*  This method returns a DL-Lite ontology, which is an approximation of the
	*  OWL ontology passed by parameters. Other input parameters are an OWL 
	*  ontology manager, an OWL ontology reasoner and a logical IRI of the 
	*  output DL Lite ontology. The manager and the ontology must be already
	*  initialized and the IRI must be mapped to a physical IRI in the manager.
	*  The manager is needed in order to be able to create the new ontology,
	*  the reasoner performs reasoning and is required to provide the basic
	*  functionality (subclasses of a class, subproperties of a property).
	*  <p> 
	*  There are two approximation modes: simple and with full chains. In the
	*  case of simple approximation the output ontology is a DL-Lite_A ontology
	*  that captures the basic concept hierarchy plus concept inclusions with
	*  qualified existentials on the right hand side.
	*  <p> 
	*  Approximation with full chains returns a sound and complete DL-Lite_A 
	*  approximation. More precisely, the output ontology is a DL-Lite_A 
	*  ontology that contains the simple approximation and inclusions 
	*  of the form B \isa ER1 ... ERn . B' with B, B' basic classes. The 
	*  result captures all existential chains implied by the original ontology.
	*  
	*  
	* @param owl_ont the input OWL 2 ontology
	* @param manager the OWL ontology manager 
	* @param uri_dllite_ont the IRI of the approximated DL-Lite ontology
	* @return the Dl Lite ontology, which is the semantic approximation of 
	* an OWL ontology.
	* 
	* @throws 
	* @throws OWLOntologyChangeException
	* @throws OWLOntologyCreationException
	* @see #initializeDlLiteOnt(OWLOntology, OWLOntology, OWLOntologyManager)
	* @see #duplicateOntology(OWLOntology, OWLOntologyManager, OWLDataFactory,
	*  OWLObjectDuplicator)
	* @see #completeOwlOnt(OWLOntology, OWLOntologyManager)
	* @see #addDlLiteHierarchy(OWLClassExpression, Set, Set, OWLOntology, 
	* OWLReasoner, OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator, 
	* DLLiteAGrammarChecker)
	* @see #addDlLiteInconsistentClassesAxioms(OWLOntology, OWLOntology, 
	* OWLReasoner, OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator,
	*  DLLiteAGrammarChecker)
	* @see #addDlLiteObjectProperties(OWLOntology, OWLOntology, OWLReasoner, 
	* OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator, 
	* DLLiteAGrammarChecker)
	* @see #addDlLiteDataProperties(OWLOntology, OWLOntology, OWLReasoner, 
	* OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator, 
	* DLLiteAGrammarChecker)  
	**************************************************************************/
	public OWLOntology transform(OWLOntology owl_ont, 
							IRI uri_dllite_ont) 
	throws OWLOntologyCreationException
	{
		
		/**
		 * Create and Inizialize the dl lite ontology 
		 * with non-logical and individual axioms
		 */
		OWLOntology dllite_ont = ontologyManager.createOntology(uri_dllite_ont);
		log.info("Created dl ontology : " + dllite_ont.getOntologyID().getOntologyIRI());

		try {
		//Initialize the DL-Lite Ontology
			initializeDlLiteOnt(owl_ont, dllite_ont);

		
		/**
		 * Complete the owl ontology with new names
		 * (enumerating the logical axioms is performed)
		 */
		OWLOntology complete_owl_ont = completeOwlOnt(owl_ont);

		



		/**
		 * The actual reasoning
		 */
		
		// Create a reasoner factory.  In this case, we will use Hermit.
		//OWLReasonerFactory reasonerFactory = (OWLReasonerFactory)Class.forName("org.semanticweb.HermiT.Reasoner.ReasonerFactory").newInstance();
		OWLReasonerFactory reasonerFactory = new org.semanticweb.HermiT.Reasoner.ReasonerFactory();
		
		// Load the workng ontology into the reasoner.  
		OWLReasoner reasoner = reasonerFactory.createReasoner(complete_owl_ont);
		// Asks the reasoner to classify the ontology.  
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY, InferenceType.OBJECT_PROPERTY_HIERARCHY, InferenceType.DATA_PROPERTY_HIERARCHY);
		

		OWLDataFactory owlDataFactory = ontologyManager.getOWLDataFactory();
		OWLObjectDuplicator duplicator = new OWLObjectDuplicator(owlDataFactory);
		
		/**
		 * we adopt a top-down approach, so we start from TOP = thing
		 * and initialize the bit matrix		 * 
		 */
		IRI classThingIRI = OWLRDFVocabulary.OWL_THING.getIRI();
		OWLClass classThing = owlDataFactory.getOWLClass(classThingIRI);
		this.initializeBitMatrix(classThing, reasoner);
		
		
		log.info("Adding Inferred axioms in the Dl Lite ontology...");

		log.info("Adding sub classes, equivalent classes, and disjoint classes" +
				 " axioms...");

		Set<OWLClassExpression> ancestors_set = new HashSet<OWLClassExpression>();
		Set<OWLClassExpression> negAncestors_set = new HashSet<OWLClassExpression>();
		//add subclass, equivalent classes and disjoint classes axioms
		addDlLiteHierarchy(classThing, ancestors_set,negAncestors_set,
								dllite_ont,  reasoner, duplicator);
		
				
		addSubClassAxiomsFromMatrix(dllite_ont, duplicator);
		
		log.info("Adding Inconsitent classes axioms...");
		//add Inconsistent classes
		addDlLiteInconsistentClassesAxioms(dllite_ont, complete_owl_ont, reasoner, 
										   duplicator);
		
		log.info("Adding properties axioms...");
		//add properties axioms
		addDlLiteObjectProperties(dllite_ont, complete_owl_ont, reasoner, 
								  duplicator);
		addDlLiteDataProperties(dllite_ont, complete_owl_ont, reasoner, 
								duplicator);
		
		
		//manager.saveOntology(complete_owl_ont, phys_uri_working_ont);
		log.info("Logical Axioms " + dllite_ont.getLogicalAxiomCount());
		log.info("Axioms count " + dllite_ont.getAxiomCount());
		log.info("Classes count "+dllite_ont.getClassesInSignature().size());
		log.info("Object properties count "+dllite_ont.getObjectPropertiesInSignature().size());

		} catch (OWLOntologyChangeException | OWLOntologyStorageException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dllite_ont;
	}
	


	/***************************************************************************
	 * The class BitMatrix2D will represent a two dimensions bit matrix, which
	 * will be used to represent the class hierarchy of the Dl Lite ontology.
	 * The idea is to have a representation which allows us to efficiently
	 * minimize the subclasses axioms to be added in the Dl LIte ontology.
	 ***************************************************************************/
	public class BitMatrix2D implements Serializable {
		private static final long 
		serialVersionUID = 29187494607829404L;

		//The fixed number of rows
		private final int rows;
		//The fixed number of columns
		private final int columns;
		//The actual data stored
		private int[] data;
		//This map will contain pairs <pos,clas>, where pos will be the position of the
		//the class in the bit matrix which we will use to minimize the subclasses axioms.
		private HashMap<OWLClassExpression,Integer> matrix_classes;
		//The map inverse to matrix_classes, used to get the class in a given position efficiently.
		private HashMap<Integer,OWLClassExpression> matrix_classes_inv;

	 
		/**
		 * Class constructor. We must indicate the fixed (maximum) number of rows 
		 * and columns that the matrix will contain.
		 * @param rows the fixed number of rows that the matrix will contain
		 * @param columns the fixed number of columns that the matrix will contain
		 */
		public BitMatrix2D(int rows, int columns) {
			matrix_classes= new HashMap<OWLClassExpression,Integer>();
			matrix_classes_inv = new HashMap<Integer,OWLClassExpression>();
			this.rows = rows;
			this.columns = columns;
			/* x >> 5 = x / 32, only faster */
			int lengh = Math.max(1,(rows * columns + 1)>> 5);
			this.data = new int[lengh];
			for (int i = 0; i < lengh; i++){
				data[i]=0;
			}

		}
		/**
		 * Returns the fixed (maximum) number of rows in the matrix. 
		 * This does not represent the acutal size of the matrix, but
		 * the value set when it was created.
		 * @return the value of rows private variable
		 */
		public int rows() {
			return rows;
		}
		/**
		 * Returns the fixed (maximum) number of columns in the matrix. 
		 * This does not represent the acutal size of the matrix, but
		 * the value set when it was created.
		 * @return the value of rows private variable
		 */
		public int columns() {
			return columns;
		}
	 
		/**
		 * Given the input indexes row and col, this method returns the 
		 * value stored in the [row,col] position of the matrix.
		 * @param row the index corresponding to the row
		 * @param col the index corresponding to the column
		 * @return the value of matrix[row,col]
		 */
		public boolean get(int row, int col) {
			if (row < 0 || row >= rows)
				throw 
					new IndexOutOfBoundsException(
					"Row index out of bounds:" + row);
			else if (col < 0 || col >= columns)
				throw 
					new IndexOutOfBoundsException(
					"Column index out of bounds:" + col);
	 
			int i = row * columns + col;
			return ( (data[i >> 5] >> (i % 32)) & 1 ) != 0;
		}
		
		/**
		 * Given the input indexes row and col, and the boolean value v,
		 * this method set the value in the [row,col] position of matrix to v
		 * @param row the index corresponding to the row
		 * @param col the index corresponding to the column
		 * @param v the boolean value to store in in matrix[row,col]
		 */
		public void set(int row, int col, boolean v) {
			if (row < 0 || row >= rows)
				throw 
					new IndexOutOfBoundsException(
					"Row index out of bounds:" + row);
			else if (col < 0 || col >= columns)
				throw 
					new IndexOutOfBoundsException(
					"Column index out of bounds:" + col);
	 
			//given the matrix is represented as a vector, it will be
			//store a secuence of columns, let's say, rows*columns columns
			//then to access to the element corresponding to [row,col]
			//we must do row * columns + col
			//an example: supose we have 3 rows and 3 columns, this is 
			//store in the matrix like: 1 2 3 4 5 6 7 8 9 (cells, where
			//cells 1,2,3 correspond to row 0, 4,5,6 row1, and 7,8,9 row2)
			//then if we want to access element [2,1] then it will be 7 
			int i = row * columns + col;
			//given that the array representing the matrix is array of ints
			//and each int has 32 bits, idiv32 will contain the chunk of bits
			//containing the cell to be modified
			int idiv32 = i >> 5;
			//modBit is a mask that will contain all 0 and only one 1 in the 
			//possition corresponding to [row,col]
			//the way to do this is shifting with 0's, for example
			//1 << 3 = 100 
			int modBit = 1 << (i % 32);
			//data[idiv32] will be calculated as:
			//if v is true, set it by calculating an "or" between what is already
			//and the mask
			//if ve is false, it makes an "and" between what is already and the 
			//negation of the mask
			data[idiv32] = v ? data[idiv32] |modBit :   
							   data[idiv32] & ~modBit; 
		}

		/**
		 * Returns the current matrix lenght. This value does not 
		 * correspond to the actual matrix lenght, but to actual busy space of
		 * the matrix
		 * @return the size corresponding to the list of classes in the matrix.
		 */
		public int get_current_length(){
			return matrix_classes.size();
		}
		
		/**
		 * Returns the index of the clazz class in the matrix 
		 * @param clazz the input class
		 * @return the index corresponding to the input class
		 */
		public int get_class_index(OWLClassExpression clazz){
			return matrix_classes.get(clazz);
		}

		/**
		 * Returns the class represented by the pos index in the matrix
		 * @param pos the input index
		 * @return the class corresponding to the input index
		 */
		public OWLClassExpression  get_class_desc(int pos){
			return matrix_classes_inv.get(pos);
		}

		/**
		 *Gets the indexes row and col corresponding to 
		 *the input classes sub and sup respectively, and then
		 *retrieves the boolean value corresponding to the [row,col]  
		 *position in the matrix.
		 * @param sub the input class to look in the rows of the matrix
		 * @param sup the input class to look in the columns of the matrix
		 * @return the value stored in matrix[row,col], where row is
		 * the index of sub, and col is the index of col
		 */
		public boolean getD(OWLClassExpression sub, OWLClassExpression sup){
			//get the index for subnull
			int row = matrix_classes.get(sub);
			//get the index for sup
			int col = matrix_classes.get(sup);
			return get(row,col);
		}

		/**
		 *Gets the indexes row and col corresponding to 
		 *the input classes sub and sup respectively, and then
		 *sets the value of the [row,col] position in matrix to v.
		 *When trying to get the indexes of the input classes, if any of
		 *the classes doesn't have an index, the method adds it.
		 * @param sub the input class to look in the rows of the matrix
		 * @param sup the input class to look in the columns of the matrix
		 * @param v the boolean value to set
		 */
		public void setD(OWLClassExpression sub, 
						 OWLClassExpression sup,
						 boolean v){
			int row,col;
			
			//get the index for the row
			if (matrix_classes.containsKey(sub)){
				row = matrix_classes.get(sub);
			}
			else {//else create the index for the matrix
				row = matrix_classes.size() ;
				matrix_classes.put(sub, row);
				matrix_classes_inv.put(row, sub);
			}
			
			//get the index for the column
			if (matrix_classes.containsKey(sup)){
				col = matrix_classes.get(sup);
			}
			else {//else create the index for the matrix
				col = matrix_classes.size() ;
				matrix_classes.put(sup, col);
				matrix_classes_inv.put(col, sup);
			}
			
			//set the value
			set(row, col, v);
		}
		
	}

}


