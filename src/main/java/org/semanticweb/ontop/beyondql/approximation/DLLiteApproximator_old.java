package org.semanticweb.ontop.beyondql.approximation;
//package inf.unibz.it.dllite.aproximation.semantic;
//
//import inf.unibz.it.dllite.aproximation.semantic.exception.FunctionalPropertyParticipatesInQualifiedExistentialException;
//import inf.unibz.it.dllite.aproximation.semantic.exception.FunctionalPropertySpecializedException;
//import inf.unibz.it.dllite.aproximation.semantic.exception.InvalidParametersException;
//
//import java.awt.image.Kernel;
//import java.io.Serializable;
//import java.util.*;
//
//import org.semanticweb.owlapi.model.AddAxiom;
////import org.mindswap.pellet.owlapi.PelletReasonerFactory;
//import org.semanticweb.owlapi.model.IRI;
//import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLAxiom;
//import org.semanticweb.owlapi.model.OWLClass;
//import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
//import org.semanticweb.owlapi.model.OWLClassAxiom;
//import org.semanticweb.owlapi.model.OWLClassExpression;
//import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
//import org.semanticweb.owlapi.model.OWLDataFactory;
//import org.semanticweb.owlapi.model.OWLDataMinCardinality;
//import org.semanticweb.owlapi.model.OWLDataProperty;
//import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
//import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
//import org.semanticweb.owlapi.model.OWLDataRange;
//import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
//import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
//import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
//import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
//import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
//import org.semanticweb.owlapi.model.OWLEntity;
//import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
//import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
//import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
//import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLIndividualAxiom;
//import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
//import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLLogicalAxiom;
//import org.semanticweb.owlapi.model.OWLObject;
//import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
//import org.semanticweb.owlapi.model.OWLObjectComplementOf;
//import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
//import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
//import org.semanticweb.owlapi.model.OWLObjectProperty;
//import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
//import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
//import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
//import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
//import org.semanticweb.owlapi.model.OWLObjectUnionOf;
//import org.semanticweb.owlapi.model.OWLOntology;
//import org.semanticweb.owlapi.model.OWLOntologyChange;
//import org.semanticweb.owlapi.model.OWLOntologyChangeException;
//import org.semanticweb.owlapi.model.OWLOntologyCreationException;
//import org.semanticweb.owlapi.model.OWLOntologyManager;
//import org.semanticweb.owlapi.model.OWLOntologyStorageException;
//import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
//import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
//import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
//import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
//import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
//import org.semanticweb.owlapi.model.RemoveAxiom;
//import org.semanticweb.owlapi.reasoner.Node;
//import org.semanticweb.owlapi.reasoner.NodeSet;
//import org.semanticweb.owlapi.reasoner.OWLReasoner;
//import org.semanticweb.owlapi.util.OWLObjectDuplicator;
//import org.semanticweb.owlapi.util.ProgressMonitor;
//import org.semanticweb.owlapi.util.SimpleIRIMapper;
//import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.Marker;
//
//import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;
//
//
///******************************************************************************
// * This class returns the Dl-Lite ontology obtained by semantically approximating 
// * an original OWL ontology. This semantic approximation is done by the public 
// * method "approximate". 
// * In order to do this approximation, we must indicate three URIs:
// * <ul>
// * <li> The IRI of the original OWL ontology
// * <li> the IRI that we want for the Dl-Lite approximation, and 
// * <li> an extra IRI, to save the "working ontology". The "working
// * ontology" is the original ontology plus some further axioms added in order
// * to get the approximation.
// * </ul>
// * These three URIs must be indicated in the constructor of the class.
// * @author Alejandra Lorenzo, Elena Botoeva
// *
// *****************************************************************************/
//public class DLLiteApproximator_old{
//	
//	/**************************************************************************
//	 * 
//	 **************************************************************************/
//	public DLLiteApproximator_old(boolean fullChains, boolean extendedAB, int maxChainLength) {
//		super();
//   		futureNamedComplexConcepts =  new HashSet<OWLClassExpression>(); 
//		toSaveNeg =  new HashSet<OWLClassExpression>();
//		new_classes = new HashMap <OWLClass,OWLClassExpression>();
//		new_NegClasses = new HashMap <OWLClass,OWLClassExpression>();
//		processed_classes = new HashSet<OWLClassExpression> ();
//		
//		this.fullChains = fullChains;
//		this.withoutNewNames = !extendedAB;
//		this.maximumChainLength = maxChainLength;
//		chainAxioms = new TreeSet<ChainInclusion>();
//		chainInclusions = new HashMap<OWLClassExpression, Set<ChainInclusion>>();
//	}
//
//
//	//This set will contain the Descriptions that are not a named class, 
//	//we will use it to create an equivalent classes axiom for each
//	//of these descriptions.
//	private Set<OWLClassExpression> futureNamedComplexConcepts;
//	//This set will contain the new negations that will we introduce in the 
//	//original ontology for every named class. We will use these negations
//	//later on to get the disjoint classes axioms.
//	private Set<OWLClassExpression> toSaveNeg;
//	//These two sets contain the mapping between the new classes that we introduced
//	//and its original equivalent descriptions. The only difference is that one is
//	//for the new negations introduced, and the other is for the rest of the cases.
//	private HashMap <OWLClass,OWLClassExpression> new_classes;
//	private HashMap <OWLClass,OWLClassExpression> new_NegClasses;
//	//This set will be used in the hierarchy, to keep track of the already 
//	//processed classes.
//	private Set<OWLClassExpression> processed_classes ;
//	//For the logging
//	private Logger log = LoggerFactory.getLogger(DLLiteApproximator_old.class);
//	//This matrix will store in each element [i,j] a "1" when the we try to add a
//	// subclass axiom between the classes in position i and j in matrix_classes.
//	private BitMatrix2D matrix_Porders; 
//
//	
//	// flag indicating that the approximation must be complete w.r.t. existential
//	// chains
//	// TODO move it to the method approximate?
//	private boolean fullChains;
//	private boolean withoutNewNames = false;
//	private int maximumChainLength = 0;
//	private Set<ChainInclusion> chainAxioms = null;
//	// This set will contain all the chain inclusions (also with introduced names)
//	private Map<OWLClassExpression, Set<ChainInclusion>> chainInclusions = null;
//	
//	private IRI ThingURI = OWLRDFVocabulary.OWL_THING.getIRI();
//	private OWLClass owlClassThing = null;
//	private OWLClass owlClassNothing = null;
//	private Set<ProgressMonitor> progressMonitors = new HashSet<ProgressMonitor>();
//	
//	
//	/**************************************************************************
//	 * Adds a suffix to the original IRI 
//	 * @param iri the original IRI
//	 * @param suffix the suffix to add
//	 *************************************************************************/
//	public IRI createIRIWithSuffix(IRI iri, String suffix)
//	{
//		String uriStr =  iri.toString();
//		if(uriStr.endsWith(".owl"))
//			uriStr = uriStr.substring(0, uriStr.length() - ".owl".length()) + "_" + suffix + ".owl";
//		else
//			uriStr = uriStr + "_" + suffix + ".owl";
//		IRI new_iri = IRI.create(uriStr);
//
//		return new_iri;
//	}
//
//	
//	/**************************************************************************
//	 * Adds a progress monitor to the list of progress monitors.
//	 * Can be used for displaying progress bar
//	 * @param progressMonitor the monitor to add
//	 *************************************************************************/
//	public void addProgressMonitor(ProgressMonitor progressMonitor)
//	{
//		progressMonitors.add(progressMonitor);
//	}
//
//
//	//TODO Mariano: Describir los parametros de los metodos. Procurar usar nombres completos para variables
//	// completar tambien @return y @throws explicando que cosa se regrea y cuando se tiran excepciones.
//	// agregar @see tags en los comentarios, para referir a metodos relacionados si el metodo usa otros 
//	// metodos importantes
//	
//	/**************************************************************************
//	 * Creates a new owl ontology and copies all the axioms, 
//	 * so that we can work on it, without modifying the  
//	 * original ontology. Returns the new ontology with the working IRI.
//	 * @param ont the original ontology that we want to duplicate
//	 * @param man the OWLOntologyManager
//	 * @param factory the OWLDataFactory for creating the duplicated ontology
//	 * @param duplicator for duplicating the ontology
//	 * @return OWLOntology the clone of the original ontology
//	 * @throws OWLOntologyStorageException  if there isn't an ontology in this
//	 *  manager which has the specified IRI.
//	 * @throws OWLOntologyChangeException 
//	 * @throws OWLOntologyCreationException 
//	 *************************************************************************/
//	private OWLOntology duplicateOntology (OWLOntology ont,
//										   OWLOntologyManager man,
//										   OWLDataFactory factory, 
//									//	   OWLObjectDuplicator duplicator,
//										   IRI logIRI) 
//	throws  OWLOntologyStorageException, OWLOntologyCreationException, OWLOntologyChangeException{
//			log.info("Duplicating the original ontology...");
//			
//			OWLOntology new_ont = man.createOntology(ont.getAxioms(), logIRI);
//			
//			return new_ont;
//	}
//
//	/**************************************************************************
//	 * For each named class in the input ontology, adds its complement to the
//	 * "toSaveNeg" set.
//	 * <p> 
//	 * This "toSaveNeg" set will be used to introduce in the working ontology, 
//	 * for each element in the set, an equivalent classes axiom between the 
//	 * complement and a new named class.   
//	 * @param ont the original ontology, to extract the referenced classes
//	 * @param factory the OWLDataFactory, to create the corresponding axioms
//	 *************************************************************************/
//	private void addToSaveNegConcepts (OWLOntology ont, 
//									   OWLDataFactory factory){
//		log.info("	* Adding Negations for every named class... ");
//		Set<OWLClass> signature =  ont.getClassesInSignature();
//		//for every class add a new concept corresponding to its negation. 
//		//then we will add a new equivalent classes axiom between the negation  
//		//of the class, and a new class
//		for (OWLClass clazz : signature){
//				OWLObjectComplementOf negConcept = factory.getOWLObjectComplementOf(clazz);
//				if (!(toSaveNeg.contains(negConcept))){  
//					toSaveNeg.add(negConcept);
//				}
//		}
//		
//	}
//
//	/**************************************************************************
//	 * Adds and existential restriction for every property in the original 
//	 * ontology.
//	 * <p>
//	 * The filler of the added existencial restriction will be every atomic   
//	 * concept in the original ontology, including Top.
//	 * <p>
//	 * TODO In this method we just added the first nesting level 
//	 * 		-someValuesOf(R,T)-. 
//	 * 		It is possible to go on adding further levels. 
//	 * 		For example: someValuesOf(R,someValuesOf(R,T))
//	 * 		In this case we should add all the possible combinations. 
//	 * @param ont the original ontology
//	 * @param factory the data factory
//	 *************************************************************************/
//	private void addToSaveSomeRestriction (OWLOntology ont, 
//										   OWLDataFactory factory){
//		log.info("	* Adding existential restrictions for every object property...");
//
//		IRI classIRI = OWLRDFVocabulary.OWL_THING.getIRI();
//		OWLClass classThing = factory.getOWLClass(classIRI);
//		
//		Set<OWLObjectProperty> properties = ont.getObjectPropertiesInSignature();
//		Set<OWLClass> classes = ont.getClassesInSignature();
//		
//		for (OWLObjectProperty oprop : properties){
//			//add the restriction for Top
//			OWLObjectSomeValuesFrom res = factory.getOWLObjectSomeValuesFrom(oprop, classThing);
//			if (!futureNamedComplexConcepts.contains(res)){
//					futureNamedComplexConcepts.add(res);
//			}
//				
//			//now add for every atomic concept in the ontology
//			for (OWLClass clazz : classes){
//				res = factory.getOWLObjectSomeValuesFrom(oprop,clazz);
//				if (!(futureNamedComplexConcepts.contains(res))){
//					futureNamedComplexConcepts.add(res);
//				}
//			}
//		}
//	}
//	
//	/**************************************************************************
//	 * Adds a data min cardinality restriction for every data property in 
//	 * the original ontology.
//	 * <p>
//	 * The form is: min 1 DataProperty
//	 * <p>
//	 * TODO: add also axioms of the form: min 1 DataProperty DataRange,
//	 * where DataRange are the Datatypes in the ontology. 
//	 * @param ont the original ontology
//	 * @param factory the data factory
//	 *************************************************************************/
//	private void addToSaveMinCardinalityRestrictions (OWLOntology ont, 
//										   OWLDataFactory factory){
//		log.info("	* Adding min cardinality restrictions for every " +
//				"data property ...");
//		
//		Set<OWLDataProperty> dproperties = ont.getDataPropertiesInSignature();
//		for (OWLDataProperty dprop :dproperties){
//			//add the restriction without range
//			OWLDataMinCardinality res = factory.getOWLDataMinCardinality(1, dprop);
//			if (!futureNamedComplexConcepts.contains(res)){
//				futureNamedComplexConcepts.add(res);
//			}
//			//TODO 
//			//now add for every data type, or data range valid in DL LITE
//		}
//	}
//	
//	/**************************************************************************
//	 * Given an input subclass axiom, if either the subclass or the superclass 
//	 * is not a named class, it is added to the "toSave" list.   
//	 * @param l_ax the input subclass axiom  
//	 *************************************************************************/
//	private void addToSaveSubClassAxioms (OWLSubClassOfAxiom l_ax, DLLiteGrammarChecker checkDLLite){
//		//if the subclass is not a named class, not an existential quantification
//		//and not a union of them then we give a name to it
//		//in general we may need to name not only one class but several 
//		//of them due to union
//		if (!checkDLLite.isDlLiteSubClassExpression(((OWLSubClassOfAxiom) l_ax).getSubClass())){
//			Set<OWLClassExpression> classesToSave = checkDLLite.getNotDlLiteSubClassExpression(((OWLSubClassOfAxiom) l_ax).getSubClass());
//			futureNamedComplexConcepts.addAll(classesToSave);
//		}
//		//if the superclass is not a named class, nor a qualified chain,
//		//nor a conjunction of them we give a name to it
//		//in the case of a chain qualified by a complex class, we give a name
//		//only to this class
//		//in general we may need to name not only one class but several 
//		//of them due to intersection
//		if (!checkDLLite.isDlLiteSuperClassExpression(((OWLSubClassOfAxiom) l_ax).getSuperClass())){
//			Set<OWLClassExpression> classesToSave = checkDLLite.getNotDlLiteSuperClassExpression(((OWLSubClassOfAxiom) l_ax).getSuperClass());
//			futureNamedComplexConcepts.addAll(classesToSave);
//		}
//	}
//	
//	/**************************************************************************
//	 * Given an input Equivalent classes axiom, if it contains a description which
//	 * is not a named class, then it is added to the "toSave" list.  
//	 * @param l_ax the input equivalent classes axiom
//	 *************************************************************************/
//	private void addToSaveEquivalentClassesAxiom 
//											  (OWLEquivalentClassesAxiom l_ax){
//		//if it does not contain any named class, we add it  
//		//to save set
//		Set<OWLClassExpression> des = 
//					((OWLEquivalentClassesAxiom)l_ax).getClassExpressions();
//		for(OWLClassExpression d: des){
//			if (!(d instanceof OWLClass)){
//				futureNamedComplexConcepts.add(d);
//			}
//		}
//	}
//	
//	/**************************************************************************
//	 * Given an input Disjoint classes axiom, if it contains a description which
//	 * is not a named class, then it is added to the "toSave" list.  
//	 * @param l_ax the input disjoint classes axiom
//	 *************************************************************************/
//	private void addToSaveDisjointClassesAxiom(OWLDisjointClassesAxiom l_ax){
//		Set<OWLClassExpression> des = 
//							((OWLDisjointClassesAxiom)l_ax).getClassExpressions();
//		for(OWLClassExpression d: des){
//			if (!(d instanceof OWLClass)){
//					futureNamedComplexConcepts.add(d);						
//			}
//		}
//	}
//
//	/**************************************************************************
//	 * Receives a OWLClassAxiom, and calls the corresponding method to handle 
//	 * the corresponding type of axiom 
//	 * @param l_ax the input class axiom
//	 **************************************************************************/
//	private void addToSaveClassAxioms (OWLClassAxiom l_ax, DLLiteGrammarChecker checkDLLite){
//			//subclass axiom
//			if (l_ax instanceof OWLSubClassOfAxiom){
//				addToSaveSubClassAxioms((OWLSubClassOfAxiom)l_ax, checkDLLite);
//			}
//			//equivalent classes axiom
//			else if (l_ax instanceof OWLEquivalentClassesAxiom){
//  				addToSaveEquivalentClassesAxiom((OWLEquivalentClassesAxiom)l_ax);
//  			}
//  			//disjoint classes axiom
//  			else if (l_ax instanceof OWLDisjointClassesAxiom){
//				addToSaveDisjointClassesAxiom((OWLDisjointClassesAxiom)l_ax);
//			}
//			else if (l_ax instanceof OWLDisjointUnionAxiom){
//				// TODO
//			}
//
//	}
//
//	/**************************************************************************
//	 *  Receives an Object Property axiom, and if it is an instance of an   
//	 *  axiom type that can contain an OWLClassExpression (i.e. Object Property 
//	 *  Domain or Object Property Range axioms), then if the description is 
//	 *  not a named class, it is added to the "toSave" list.
//	 * @param l_ax the input object property axiom
//	 **************************************************************************/
//	private void addToSaveObjectPropertyAxiom (OWLObjectPropertyAxiom l_ax){
//		//ObjectPropertyDomainAxiom
//		if (l_ax instanceof OWLObjectPropertyDomainAxiom){
//			if (!(((OWLObjectPropertyDomainAxiom)l_ax).getDomain() instanceof 
//					OWLClass)){
//				futureNamedComplexConcepts.add(((OWLObjectPropertyDomainAxiom)l_ax).getDomain());
//			}
//		}
//		else if (l_ax instanceof OWLObjectPropertyRangeAxiom){
//			if (!(((OWLObjectPropertyRangeAxiom)l_ax).getRange() instanceof 
//					OWLClass)){
//				futureNamedComplexConcepts.add(((OWLObjectPropertyRangeAxiom)l_ax).getRange());
//			}
//		}
//		else if (l_ax instanceof OWLSubObjectPropertyOfAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLSubPropertyChainOfAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLEquivalentObjectPropertiesAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLDisjointObjectPropertiesAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLInverseObjectPropertiesAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLFunctionalObjectPropertyAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLInverseFunctionalObjectPropertyAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLReflexiveObjectPropertyAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLIrreflexiveObjectPropertyAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLSymmetricObjectPropertyAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLAsymmetricObjectPropertyAxiom){
//			// TODO
//		}
//		else if (l_ax instanceof OWLTransitiveObjectPropertyAxiom){
//			// TODO
//		}
//	}
//	
//	/**************************************************************************
//	 *  Receives a Data Property axiom, and if it is an instance of an   
//	 *  axiom type that can contain a OWLClassExpression (i.e. Data Property Domain
//	 *  axiom), then if the description is not a named class, it is added to 
//	 *  the "toSave" list.
//	 * @param l_ax the input data property axiom
//	 **************************************************************************/
//	private void addToSaveDataPropertyDomainAxiom
//											 (OWLDataPropertyDomainAxiom l_ax){
//		if (!(((OWLDataPropertyDomainAxiom)l_ax).getDomain() instanceof 
//				OWLClass)){
//				futureNamedComplexConcepts.add(((OWLDataPropertyDomainAxiom)l_ax).getDomain());
//			}
//	}
//		
//	/**************************************************************************
//	 * Receives an Assertion Axiom, and if it is an instance of an axiom type   
//	 *  that can contain a OWLClassExpression (i.e. Class Assertion Axiom), then  
//	 *  if the description is not a named class, it is added to the "toSave" list.
//	 * @param l_ax the input class assertion axiom
//	 *************************************************************************/
//	private void addToSaveClassAssertionAxiom (OWLClassAssertionAxiom l_ax){
//  		if (!(((OWLClassAssertionAxiom)l_ax).getClassExpression() instanceof 
//  				OWLClass)){
//			futureNamedComplexConcepts.add(((OWLClassAssertionAxiom)l_ax).getClassExpression());
//		}
//	}
//
//	/**************************************************************************
//	 * Returns the original ontology completed with the elements in the "toSave" 
//	 * and "toSaveNeg" lists.
//	 * That is, returns a complete ontology corresponding to the input ontology, 
//	 * plus a new equivalent classes axiom for every not named description; plus 
//	 * a new equivalent class axiom for the negation of each named class
//	 * in the original ontology;  plus a new equivalent classes axiom for the 
//	 * some restriction for every object property in the ontology (and for 
//	 * every named class in the original ontology).
//	 * Also adds a data min cardinality restriction for every data property in 
//	 * the original ontology.
//	 * <p> 
//	 * All these new axioms added will help in the classification, to obtain
//	 * some further inferences.
//	 * <p> 
//	 * This method also remove all individual axioms or "abox assertions" from
//	 * the working ontology, so we don't classify them later.
//	 * @param new_ont the input ontolgy 
//	 * @param man the ontology Manager
//	 * @return OWLOntology the same input ontology, completed with all 
//	 * the equivalent classes axioms 
//	 * @throws OWLOntologyChangeException, can happen when applying the changes 
//	 * to the input ontology.
//	 * @throws OWLOntologyStorageException, can happen when saving the ontology. 
//	 **************************************************************************/
//	private OWLOntology completeOwlOnt (OWLOntology new_ont, 
//										OWLOntologyManager man, DLLiteGrammarChecker checkDLLite) 
//	throws OWLOntologyChangeException,  OWLOntologyStorageException
//	{
//		List<OWLOntologyChange> changes = new  ArrayList<OWLOntologyChange>();
//
//		log.info("Building the conservative extension... ");
//		OWLDataFactory factory = man.getOWLDataFactory();
//		log.info("	* Adding named classes for every description...");
//		//Start working now with the copy of the original ontology
//		//get the logical axioms, and for each axiom that contains a description,
//		//save the description in a set that we will use later on the create the
//		//equivalent class axiom.
//		Set<OWLLogicalAxiom> l_axioms = new_ont.getLogicalAxioms();
//		for (OWLLogicalAxiom l_ax : l_axioms){
//			//class axiom
//			if(l_ax instanceof OWLClassAxiom){
//				addToSaveClassAxioms((OWLClassAxiom)l_ax, checkDLLite);
//			}
//			//object property axiom
//			else if(l_ax instanceof OWLObjectPropertyAxiom){
//				addToSaveObjectPropertyAxiom((OWLObjectPropertyAxiom)l_ax);
//			}
//			//data property axiom
//			else if (l_ax instanceof OWLDataPropertyAxiom){
//				// TODO ontology repository/cmt contains such an axiom
//				if(l_ax instanceof OWLDataPropertyDomainAxiom)
//					addToSaveDataPropertyDomainAxiom((OWLDataPropertyDomainAxiom)l_ax);
//				// none of the following axioms makes any difference
//				else if(l_ax instanceof OWLDataPropertyRangeAxiom)
//					;	
//				else if (l_ax instanceof OWLSubDataPropertyOfAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLEquivalentDataPropertiesAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLDisjointDataPropertiesAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLFunctionalDataPropertyAxiom){
//					// TODO
//				}
//			}
//			else if (l_ax instanceof OWLIndividualAxiom){
//				//add the individual axioms (or abox assertions), to a 
//				//changes list, that will contain all the axioms to remove
//				changes.add(new RemoveAxiom(new_ont, l_ax));
//				//class assertion axiom
//				if (l_ax instanceof OWLClassAssertionAxiom){
//					addToSaveClassAssertionAxiom((OWLClassAssertionAxiom)l_ax);
//				}
//	  		}
//
//		}
//		//apply the changes that remove the individual axioms from the ontology
//		man.applyChanges(changes);
//		
//		//Now create:
//		// - the negation of every concept (which will be added in the 
//		//	 toSaveNeg set), and
//		// - the some restriction for every object property (which will be 
//		//	 added in the same set of used for all the other descriptions.
//		this.addToSaveNegConcepts(new_ont, factory);
//		if(!fullChains) {
//			this.addToSaveSomeRestriction(new_ont, factory);
//		}
//		this.addToSaveMinCardinalityRestrictions (new_ont,factory);
//		
//		//Now create all the corresponding equivalent classes axioms. 
//		int n= 0;
//  		for (OWLClassExpression obj: futureNamedComplexConcepts){
//  			//save every description that is not a  named class, in a new 
//  			// equivalent classes axiom with a new class
//  			//TODO change string name for the new classes for global constants...
//				OWLClass new_classFather = 
//				  factory.getOWLClass(IRI.create(new_ont.getOntologyID().getOntologyIRI() +"#new"+ n));
//				new_classes.put(new_classFather, obj);
//				OWLEquivalentClassesAxiom new_ax = 
//					factory.getOWLEquivalentClassesAxiom(new_classFather,obj);
//  				AddAxiom addAxiom = new AddAxiom (new_ont, new_ax);
//  				man.applyChange(addAxiom);
//				n= n+1;
//		}
//  		for (OWLClassExpression obj: toSaveNeg){
//				OWLClass new_classFather = 
//				   factory.getOWLClass(IRI.create(new_ont.getOntologyID().getOntologyIRI() +"#Not_" + 
//				   String.valueOf(((OWLObjectComplementOf)obj).getOperand())));
//				new_NegClasses.put(new_classFather, obj);
//				OWLEquivalentClassesAxiom new_ax =
//					factory.getOWLEquivalentClassesAxiom(new_classFather,obj);
//  				AddAxiom addAxiom = new AddAxiom (new_ont, new_ax);
//  				man.applyChange(addAxiom);
//				n= n+1;
//		}
//  		
//  		// commented save
//		//man.saveOntology(new_ont,URI_working_ont);
//  		//log.info("Saving complete working ontology...");
//  		
//		return new_ont;
//	}//completeOwlOnt 
//
//	
//	/**************************************************************************
//	 * Initializes the DL lite ontology. In this phase, only copy all the axioms
//	 * that are not logical axioms. Logical axioms will be added in the DL lite
//	 * ontology after the classification, excepting the individual axioms, which
//	 * will be copied now, and then removed from the working ontology. 
//	 * This last step involving the individual axioms, is done in order to run
//	 * run a faster classification (in case the ontology contains too many 
//	 * individuals). 
//	 * @param ont the OWL original ontology, to copy all non logical
//	 * axioms
//	 * @param dl_ont the Dl Lite ontology
//	 * @param man the ontology manager
//	 * @throws OWLOntologyStorageException thrown by the addAxiom method 
//	 * @throws OWLOntologyChangeException thrown by the addAxiom method
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager) 
//	 *************************************************************************/
//	private void initializeDlLiteOnt(OWLOntology ont,OWLOntology dl_ont,
//									 OWLOntologyManager man) 
//	throws OWLOntologyChangeException, OWLOntologyStorageException
//	{
//		log.info("Initializing the Dl Lite ontology...");
//		OWLDataFactory factory = man.getOWLDataFactory();
//		Set<OWLAxiom> axioms = ont.getAxioms();
//		OWLObjectDuplicator ob = new OWLObjectDuplicator(factory);
//		//For every non logical axiom in the owl ontology that is valid in 
//		//Dl Lite, copy it in the Dl Lite ontology. The logical axioms will 
//		//be infered
//		for (OWLAxiom ax : axioms){
//			if (!(ax instanceof OWLLogicalAxiom)){
//				OWLAxiom new_ax = ob.duplicateObject(ax);
//				//call a method that check if the axiom is already in the 
//				//dl-lite ontology and if not, adds the axiom
//				addAxiom(new_ax.getNNF(), dl_ont, man);
//			}
//			//With the individual axioms, or abox assertions, we copy the
//			//valid ones in the Dl Lite ontology, and then we will delete
//			//them from the working ontology
//			else if (ax instanceof OWLIndividualAxiom){
//				OWLAxiom new_ax = ob.duplicateObject(ax);
//				//call a method that check if the axiom is already in the 
//				//dl-lite ontology and if not, adds the axiom
//				addAxiom(new_ax.getNNF(), dl_ont, man);
//			}
//		}
//	}
//
//	
//	/**************************************************************************
//	 * Adds the input axiom in the Dl Lite ontology, only if it is valid in Dl
//	 * Lite, and if it was not previously added.
//	 * @param new_axiom the new axiom to added in the Dl Lite ontology
//	 * @param dlLiteOnt the Dl Lite ontology
//	 * @param man the ontology Manager
//	 * @throws OWLOntologyChangeException, when applying changes 
//	 * @throws OWLOntologyStorageException, when saving the ontology
//	 **************************************************************************/
//	private void addAxiom(OWLAxiom new_axiom, OWLOntology dlLiteOnt, 
//						  OWLOntologyManager man) 
//	throws OWLOntologyChangeException, OWLOntologyStorageException 
//	{
//		DLLiteGrammarChecker checkDLLite = new DLLiteGrammarChecker(fullChains);
//		boolean isDlLite = checkDLLite.isdlLiteAxiom(new_axiom.getNNF(),dlLiteOnt);
//		boolean NoExists = (!dlLiteOnt.containsAxiom(new_axiom.getNNF())); 
//		if (NoExists && isDlLite){
//				//log.debug("Adding the axiom: " + new_axiom );
//				AddAxiom addAxiom = new AddAxiom(dlLiteOnt, new_axiom.getNNF());
//				// We now use the manager to apply the change
//				man.applyChange(addAxiom);
//				
//				// commented save
//				//man.saveOntology(dlLiteOnt,URI_dl_ont);
//		}
//		else{
//			String reason= ""; //for the logg only
//			if (!isDlLite){
//				reason = "Is not valid in Dl-Lite. ";
//			}
//			if (!NoExists){
//				reason = reason + "Already Exists in the Dl Lite ontology.";
//			}
//			//log.debug("Didn't add Axiom: "+ new_axiom.getNNF());
//			//log.debug( " Reason: " + reason);
//		}
//	}
//	
//
//	/**************************************************************************
//	 * Adds in the Dl lite ontology, the inconsistent class axioms inferred by  
//	 * the reasoner after the classification.
//	 * <p>
//	 * In order to do this, we create an inconsistent class, 
//	 * <p>				INC subClassOf ComplementOf(INC),
//	 * <p>
//	 * then we get all inconsistent classes in the owl ontology, and we make 
//	 * each inconsistent class equivalent to the new inconsistent class. 
//	 * @param dl_ont the Dl Lite ontology
//	 * @param owl_ont the OWL complete ontology, were we will 
//	 * infere the inconsistent classes.
//	 * @param reasoner the OWLReasoner used to get the inconsistent classes
//	 * @param man the ontology Manager
//	 * @param factory the OWLDataFactory 
//	 * @param ob the OWLObjectDuplicator, to duplicate the classes to add as 
//	 * inconsitent classes in the Dl Lite ontology 
//	 * @param checkDLLite an instance of DLLiteGrammarChecker used to check, 
//	 * in case the inconsitent class is a new added class, that its 
//	 * corresponding description is a valid sub class expression in Dl Lite.
//	 * @throws OWLOntologyStorageException thrown by the addAxiom method
//	 * @throws OWLOntologyChangeException thrown by the addAxiom method
//	 * @throws  when inferring the inconsistent classes 
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 *************************************************************************/
//	private void addDlLiteInconsistentClassesAxioms (OWLOntology dl_ont, 
//													 OWLOntology owl_ont, 
//													 OWLReasoner reasoner,
//													 OWLOntologyManager man,
//													 OWLDataFactory factory,
//													 OWLObjectDuplicator ob ,
//											DLLiteGrammarChecker checkDLLite ) 
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException		   
//	{
//		//create an inconsistent class INC which is subclass of NOT INC
//		OWLClass incClass =	factory.getOWLClass
//						(IRI.create(String.valueOf(owl_ont.getOntologyID().getOntologyIRI()) +"#INC"));
//		
//		//a set of inconsistent classes to create the equivalent classes axiom with INC
//		//it is initialized with the inc class which will be equivalent to the 
//		//inconsistent classes.
//		HashSet<OWLClassExpression> set_inc = new HashSet<OWLClassExpression> ();
//		set_inc.add(incClass);
//		
//		//var to know whether or not we added some axiom with the new INC class.
//		boolean addedINC = false; 
//		//get all inconsistent classes and add an EquivalentClasses axiom to this new class
//		Node<OWLClass> incs =reasoner.getUnsatisfiableClasses();
//		//log.debug("inconsitent classes: "+ incs);
//		OWLClassExpression aux_desc = null;
//		for (OWLClass in: incs){
//			//if it is a new class, then work with the equivalent description
//			if (new_classes.containsKey(in)){
//				aux_desc = new_classes.get(in);
//			} 
//			else if (new_NegClasses.containsKey(in)){
//				aux_desc = new_NegClasses.get(in);
//			}else {
//				aux_desc = in;
//			}
//			//create the EquivalentClasses axiom for the description aux_desc and INC
//			if (checkDLLite.isDlLiteSubClassExpression(aux_desc)){
//				OWLClassExpression  new_in = ob.duplicateObject(aux_desc);
//				// instead of creating the axiom we add the inconsistent class in a set 
//				//to create the equivalent classes axiom with INC
//				
//				set_inc.add(new_in);
//				
//				addedINC = true;
//			}
//		}
//		if (addedINC){
//			//create the axiom that will make INC inconsistent
//			OWLClassExpression compInc = factory.getOWLObjectComplementOf(incClass);
//			OWLSubClassOfAxiom sub_inc = 
//									factory.getOWLSubClassOfAxiom(incClass, compInc);
//			addAxiom(sub_inc,dl_ont,man);
//			
//			//create the equivalent classes axiom between INC and every inconsistent class
//			OWLEquivalentClassesAxiom axiom = 
//							factory.getOWLEquivalentClassesAxiom(set_inc);
//			//add the axiom in the dl lite ontology
//			addAxiom(axiom, dl_ont,man);
//
//			
//		}
//	}
//	
//	/**************************************************************************
//	 * Given an input class and a list of negated ancestors, it creates a 
//	 * Disjoint classes axiom between the class and each ancestor.
//	 * In order to do this, it check if the input class is a new Negated class,
//	 * <ul> 
//	 * <li> if it is then it adds it in the list of ancestors to create disjoints.
//	 * <li> if it is not, then if it is a valid sub class expression for dl-lite, 
//	 *	and if the list of disjoint ancestors contains some ancestor, it  
//	 *	creates the disjoint, and delete the ancestors.
//	 *	</ul>
//	 * @param clazz of type OWLClassExpression, the input description
//	 * @param negAncestors_set a set of OWLClassExpression, containing the 
//	 * negated ancestors
//	 * @param dlLiteOnt the Dl Lite ontology
//	 * @param ob an instance of OWLObjectDuplicator 
//	 * @param factory an instance of OwLDataFactory 
//	 * @param man the ontology Manager
//	 * @return Set of OWLDescriptions, containing the updated negated 
//	 * ancestors 
//	 * @throws OWLOntologyStorageException thrown by the addAxiom method 
//	 * @throws OWLOntologyChangeException thrown by the addAxiom method
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 *************************************************************************/
//	private Set<OWLClassExpression> addDlLiteDisjoints (OWLClassExpression clazz, 
//										Set<OWLClassExpression> negAncestors_set, 
//										OWLOntology dlLiteOnt, 
//										OWLObjectDuplicator ob ,
//										OWLDataFactory factory, 
//										OWLOntologyManager man) 
//	throws OWLOntologyChangeException, OWLOntologyStorageException 
//	{
//		//If it is a new negated class, add it in the set of ancestors to 
//		//create disjoints.
//		OWLClassExpression new_clazz = null;
//		if (new_NegClasses.containsKey(clazz)){
//			OWLClassExpression negclazz = new_NegClasses.get(clazz);
//			new_clazz = 
//			(OWLClassExpression)ob.duplicateObject
//							  (((OWLObjectComplementOf)negclazz).getOperand());
//			negAncestors_set.clear(); //see given it is a list of ancestors which will be used as disjoints, only use the last one... to test
//			negAncestors_set.add(new_clazz);
//		}
//		else if (!negAncestors_set.isEmpty()){
//			//create the disjoint classes axiom
//			for (OWLClassExpression neg_anc : negAncestors_set){
//				new_clazz = ob.duplicateObject(clazz);
//				OWLDisjointClassesAxiom axiom = 
//						factory.getOWLDisjointClassesAxiom(new_clazz,neg_anc);
//				addAxiom(axiom, dlLiteOnt, man);
//			}
//			//empty the set of ancestors for negated classes
//			negAncestors_set.clear();
//		}
//		return negAncestors_set;
//	}
//	
//	/**************************************************************************
//	 * Returns true if the input class "clazz" is Nothing or equivalent to Nothing. 
//	 * Used in the method  addDlLiteSubClasses, to know whether or not to create the 
//	 * subclass axioms (we won't create subclass axioms for classes equivalent
//	 * to nothing).
//	 * @param clazz the evaluated input class
//	 * @param reasoner an instance of OWLReasoner, used to get the equivalent 
//	 * classes of the input class
//	 * @return boolean. It returns true if the class is equivalent (or equals)
//	 * to nothing, and false in the oppossite case.
//	 * @throws , when interacting with the reasoner. 
//	 *************************************************************************/
//	private boolean isNothingEquivalentClass (OWLClassExpression clazz, OWLReasoner reasoner) 
//	{
//		boolean isNothingEq = false;
//		if (clazz.isOWLNothing()){
//			isNothingEq = true;
//		}
//		else {
//			Node<OWLClass> eqs = reasoner.getEquivalentClasses(clazz);
//			for (OWLClass eq : eqs){
//				if (eq.isOWLNothing()){
//					isNothingEq = true;
//				}
//			}
//		}
//		return isNothingEq;
//	}
//	
//	
//	/**************************************************************************
//	 * Recives as input a class ("clazz") and a set of ancestors, and for each
//	 * ancestor, it adds to the bit matrix (containing the class hierarchy): 
//	 * <ul><li>"true" in the position corresponding to the pair 
//	 * (clazz,ancestor), only if clazz is a valid sublcass expression
//	 * and the ancestor a valid superclass expression for DL Lite.</ul> 
//	 * After adding the value in the matrix, the ancestor is deleted from the list 
//	 * <p>
//	 * Also, if clazz is a valid superclass expression, it is added
//	 * to the ancestors set, to be used later to add in the matrix the subclass 
//	 * between clazz and its subclasses.
//	 * <p>
//	 * Returns the updated set of ancestors. 
//	 * @param clazz the class being visited it is an instance of OWLClassExpression 
//	 * @param ancestors_set the set of ancesotors of
//	 * the current clazz
//	 * @param dlLiteOnt the Dl Lite ontology
//	 * @param checkDLLite an instance of DLLiteGrammarChecker, used to 
//	 * check if the class is a valid sub/super class expression in Dl Lite. 
//	 * @param factory an instance of OwLDataFactory
//	 * @param reasoner an instance of OWLReasoner, used to get the equivalent  
//	 * classes of the current clazz when calling to the method 
//	 * isNothingEquivalentClass
//	 * @param man the ontology manager
//	 * @return Set<OWLClassExpression>. The set of ancestors, updated.
//	 * @throws OWLOntologyStorageException thrown by the addAxiom method
//	 * @throws OWLOntologyChangeException thrown by the addAxiom method
//	 * @throws  thrown by the isNothingEquivalentClass 
//	 * method 
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 *************************************************************************/
//	private Set<OWLClassExpression> addDlLiteSubClasses (OWLClassExpression clazz, 
//											Set<OWLClassExpression> ancestors_set, 
//											OWLOntology dlLiteOnt, 
//											DLLiteGrammarChecker checkDLLite,
//											OWLDataFactory factory,
//											OWLReasoner reasoner,
//											OWLOntologyManager man) 
//	throws OWLOntologyChangeException, OWLOntologyStorageException  
//	 {
//		//don't add subclass axioms for Nothing or for classes equivalent to nothing
//		if (!this.isNothingEquivalentClass(clazz, reasoner)){
//			//if the  description received is a valid sub-class expression 
//			//in dl-lite, then we add a sub class axiom for every ancestor
//			if(checkDLLite.isDlLiteSubClassExpression(clazz) &&
//				!ancestors_set.isEmpty()){
//				for (OWLClassExpression sup: ancestors_set){
//					if (!sup.equals(clazz)){
//						//Instead of adding the axiom, we will add a "true" in the bit matrix
//						//matrix_classes[i,j] where i is the position of the subclass, and 
//						//j is the position of the superclass in the matrix_classes hashMap
//						matrix_Porders.setD(clazz,sup,true);
//					}
//				}
//				ancestors_set.clear();
//			}
//			//if it is a valid super-class expression in dl-lite,
//			//add it to the ancestors list, so it will be added later on in 
//			//a subclass axiom,but as father 
//			if (checkDLLite.isDlLiteSuperClassExpression(clazz)){
//				ancestors_set.add(clazz);
//			}
//			
//		}else {
//			ancestors_set.clear();
//		}
//		return ancestors_set;
//	}
//
//	/**************************************************************************
//	 * Receives a set of equivalent descriptions, and creates equivalent classes 
//	 * axioms when the descriptions are valid subclass expressions for Dl Lite,
//	 * or subclass axioms for those descriptions that are not valid subclass 
//	 * expressions but that are valid superclass expressions in Dl Lite.
//	 * <p>  
//	 * Returns a class selected from the input set of equivalent classes. This
//	 * class will be used to go on working with the hierarchy. In order to 
//	 * select the class, first picks the first one, and then preferes the 
//	 * most simple class, i.e. if there is a named one, it chooses it.
//	 * @param sete the set of equivalent classes
//	 * @param clazz the father class, just to compare that any 
//	 * of the the classes is equal to the father class. 
//	 * @param dlLiteOnt the Dl Lite ontology
//	 * @param man the ontology manager
//	 * @param factory an instance of OWLDataFactory  
//	 * @param ob an instance of OWLObjectDuplicator 
//	 * @param checkDLLite an instance of DLLiteGrammarChecker used to check 
//	 * for expressions valid in Dl Lite.
//	 * @return OWLClassExpression, the class that was selected from the set of
//	 * equivalent classes
//	 * @throws OWLOntologyStorageException thrown by the addAxiom method
//	 * @throws OWLOntologyChangeException thrown by the addAxiom method
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 *************************************************************************/
//	private OWLClassExpression addDlLiteEquivalentClassesAxioms 
//													(boolean firstime,
//													 Set<OWLClass> sete,
//													 OWLClassExpression clazz,
//													 OWLOntology dlLiteOnt, 
//													 OWLOntologyManager man,
//													 OWLDataFactory factory,
//													 OWLObjectDuplicator ob,
//													 DLLiteGrammarChecker checkDLLite) 
//	throws OWLOntologyChangeException, OWLOntologyStorageException
//	{
//		//just in case we are calling with a set of classe equivalent to
//		//thing, we add thing in the set of eq. classes...
//		if (firstime){
//			sete.add((OWLClass)clazz);
//		}
//		Set<OWLClassExpression> sub_classes= new HashSet<OWLClassExpression> ();
//		Set<OWLClassExpression> super_classes = new HashSet<OWLClassExpression> ();
//		OWLClassExpression new_eq = null;
//		OWLClassExpression selected_class = null;
//		
//		for (OWLClass sub_class : sete){
//			
//			if (!sub_class.equals(clazz)||firstime){
//				
//				//select initially the first class in the set
//				if (selected_class == null) {
//					//if the selected class is a new class, the select instead its 
//					//equivalent description
//					if (new_classes.containsKey(sub_class)){
//						selected_class = new_classes.get(sub_class);
//					}
//					//if it is a new_negated class, the same
//					else if (new_NegClasses.containsKey(sub_class)) {
//						selected_class = new_NegClasses.get(sub_class);
//					}
//					else { //it is a named class
//						selected_class = sub_class;
//					}
//				}
//				
//				//If the class in the set is a valid subclass expression in 
//				//DL Lite, then add to the set of subclasses.
//				//else, if it is a valid super class expression in Dl Lite,  
//				//then add the class to the set of superclasses.
//				//In order to do it, first ask if it is a new class, and, in  
//				//case it is, get the equivalent description.
//				if (new_classes.containsKey(sub_class)){
//					// if it is a new (auxiliary) class (not a negated one), then 
//					//add its equivalent description in the corresponding set
//					if (checkDLLite.isDlLiteSubClassExpression
//												 (new_classes.get(sub_class))){
//					  new_eq = ob.duplicateObject(new_classes.get(sub_class));
//					  sub_classes.add(new_eq);
//					  //if the chosen class so far, is not a valid subclass expression
//					  //in dl-lite, replace it with the subclass expression
//					  if (!checkDLLite.isDlLiteSubClassExpression
//							  								(selected_class)){
//							selected_class = new_classes.get(sub_class);
//					  }
//						
//					}// if it is not a subclass expression, check for 
//					//superclass expression
//					else if (checkDLLite.isDlLiteSuperClassExpression
//												 (new_classes.get(sub_class))){
//						new_eq = ob.duplicateObject(new_classes.get(sub_class));
//						super_classes.add(new_eq);
//					}
//				}
//				//if it is not neither a new class nor a new negated class, 
//				//it is a named class,add it to the set of subclasses and  
//				//select it as the selected class to go on working
//				else if ((!new_classes.containsKey(sub_class))&& 
//						 (!new_NegClasses.containsKey(sub_class))){
//					sub_classes.add(sub_class);
//					selected_class = sub_class;
//				}
//			}
//		}
//		//create the equivalent classes axioms with all subclass expressions
//		if (sub_classes.size()>1){
//			OWLEquivalentClassesAxiom axiom = 
//							factory.getOWLEquivalentClassesAxiom(sub_classes);
//			this.addAxiom(axiom, dlLiteOnt, man);
//		}
//		sub_classes.clear();
//
//		//create the subclass axioms for the superclass expression 
//		//use the selected class to work as subclass,
//		//if in the set of equivalent classes there were no subclass expression
//		//then won't create any subclass axiom
//		if ((!(selected_class == null)) &&
//			(!new_NegClasses.containsKey(selected_class))){
//			OWLClassExpression new_sub = ob.duplicateObject(selected_class);
//			for (OWLClassExpression sup : super_classes){
//				if (!sup.equals(selected_class)){
//					OWLSubClassOfAxiom axiom = 
//						factory.getOWLSubClassOfAxiom(new_sub, sup);
//					this.addAxiom(axiom, dlLiteOnt, man);
//				}
//			}
//		}
//		return selected_class;
//	}
//	
//	
//	/**************************************************************************
//	 * Goes through the owl ontology hierarchy inferred by the reasoner, and 
//	 * adds the subclass, equivalent classes and disjoint classes axioms, in 
//	 * the Dl Lite ontology.
//	 * <p>
//	 * When retreiving the sublcass of the clazz, it returns a set of sets,
//	 * where each subset contains equivalent classes. So, we use only one class
//	 * in each set to go on with the hierarchy. The method that adds the 
//	 * equivalent classes axioms in the Dl Lite ontology, is the one that 
//	 * selects the class. 
//	 * @param clazz the class being visited
//	 * @param ancestors_set the set of ancestors of the
//	 * current clazz with which we will create subclass axioms.
//	 * @param negAncestors_set the set of ancestors of the
//	 * current clazz with which we will create disjoint classes axioms.
//	 * @param dlLiteOnt the Dl Lite ontology
//	 * @param reasoner the reasoner, an instance of OWLReasoner used to get 
//	 * the subclasses of the current class, and go on with the hierarchy
//	 * @param man the ontology manager
//	 * @param factory the data Factory
//	 * @param ob the object duplicator
//	 * @param checkDLLite an instance of DLLiteGrammarChecker 
//	 * @throws  thrown by the invoqued method 
//	 * addDlLiteSubClasses
//	 * @throws OWLOntologyStorageException  thrown by the invoqued methods 
//	 * addDlLiteDisjoints,addDlLiteSubClasses,addDlLiteEquivalentClassesAxioms
//	 * @throws OWLOntologyChangeException  thrown by the invoqued methods
//	 * addDlLiteDisjoints,addDlLiteSubClasses,addDlLiteEquivalentClassesAxioms
//	 *************************************************************************/
//	private void addDlLiteHierarchy(OWLClassExpression clazz, 
//									Set<OWLClassExpression> ancestors_set,
//									Set<OWLClassExpression> negAncestors_set,
//									OWLOntology dlLiteOnt,   
//									OWLReasoner reasoner,
//									OWLOntologyManager man, 
//									OWLDataFactory factory, 
//									OWLObjectDuplicator ob ,
//									DLLiteGrammarChecker checkDLLite) 
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException		    
//	{
//		//in case there are in the ontology descriptions equivalents to thing
//		if (clazz.isOWLThing()){
//			
//			Node<OWLClass> sete= reasoner.getEquivalentClasses(clazz);
//			OWLClassExpression aux = this.addDlLiteEquivalentClassesAxioms
//											(true,sete.getEntities(),clazz, 
//											dlLiteOnt, man, factory, ob, checkDLLite);
//		}
//		
//
//		OWLClassExpression selected_class = null;
//		//creates the disjoints, and get the ancestors for disjoints.
//		if (!this.isNothingEquivalentClass(clazz, reasoner)){
//			negAncestors_set = this.addDlLiteDisjoints(clazz, negAncestors_set, 
//					   dlLiteOnt, ob, 
//					   factory, man);
//		}
//		//Create the subclass axioms, and get the ancestors set.
//		if (!new_NegClasses.containsKey(clazz)){ //To Test: add subclasses axioms only when the class is not a New Negated Class
//												// see is this doesn't work change the conversion from neg-class to its equivalent class upwards
//			ancestors_set = this.addDlLiteSubClasses(clazz, ancestors_set, 
//					 dlLiteOnt, 
//					 checkDLLite, 
//					 factory, 
//					 reasoner,man);
//		}
//
//		//if the clazz is an new negated class introduced in the owl ontology, 
//		//work with its corresponding description.
//		if (new_NegClasses.containsKey(clazz)){
//			clazz = new_NegClasses.get(clazz);
//		}
//
//		//If clazz is already processed, then don't go on with the hierarchy
//		if (!processed_classes.contains(clazz)){
//		//if (true){
//			processed_classes.add(clazz);
//			//In any case, get the subclasses and call recursively
//			NodeSet<OWLClass> sub_classess = reasoner.getSubClasses(clazz, false);
//			//log.debug("sublcases " + sub_classess);
//			for (Node<OWLClass> set_classes: sub_classess){
//				selected_class = null;
//				//Create the equivalentClasses axioms, and select one of the
//				//classes to go on looking in the hierarchy. For getting 
//				//equivalent classes, use the result of the getSubClasses method
//				//log.debug("llama equ. con " + set_classes + "  y " + clazz);
//				selected_class = this.addDlLiteEquivalentClassesAxioms
//															(false, set_classes.getEntities(),clazz, 
//															 dlLiteOnt, man, factory, ob, 
//															 checkDLLite);
//				// Call recursively
//				if (selected_class != null){
//					Set<OWLClassExpression> new_ancestors = 
//						(Set<OWLClassExpression>)
//							((HashSet<OWLClassExpression>)ancestors_set).clone();
//						Set<OWLClassExpression> new_negAncestors = 
//						(Set<OWLClassExpression>)
//						   ((HashSet<OWLClassExpression>)negAncestors_set).clone();
//						this.addDlLiteHierarchy(selected_class, new_ancestors,
//												new_negAncestors, dlLiteOnt, 
//												reasoner, man, 
//												factory, ob, checkDLLite);
//
//				}
//				else {
//					log.debug("There is no selected_class to get hierachy");
//				}
//			}
//		}
//	}
//
//	/**************************************************************************
//	 * This method adds the subproperties axioms and also the equivalent 
//	 * properties axioms.
//	 * <p>
//	 * The reasoner getSubObjectProperties method, returns a set of sets,
//	 * where the elements of each set are equivalent properties. So we
//	 * take advantage of this to create the equivalent properties axioms. 
//	 * @param prop the property used to get the subproperties
//	 * @param new_prop the duplicated property.
//	 * @param reasoner an instance of OWLReasoner
//	 * @param dl_ont the Dl Lite ontology
//	 * @param man the ontology manager
//	 * @param factory the data factory
//	 * @param duplicator the object duplicator
//	 * @throws 
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method 
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	private void addSubObjectPropertyAxioms(OWLObjectProperty prop, 
//											OWLObjectProperty new_prop,
//											OWLReasoner reasoner,
//											OWLOntology dl_ont, 
//											OWLOntologyManager man,
//											OWLDataFactory factory,
//											OWLObjectDuplicator duplicator) 
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException
//	{
//		// We get all subproperties, not only the direct ones
//		NodeSet<OWLObjectPropertyExpression> sub_props = reasoner.getSubObjectProperties(prop, false);//SubProperties(prop);
//		for (Node<OWLObjectPropertyExpression> sub_prop_set : sub_props){
//			//each element in the set is an equivalent property, so
//			//will use this to add equivalent property axioms
//			//create equivalent properties axioms with the properties 
//			OWLEquivalentObjectPropertiesAxiom eq_axiom = 
//				factory.getOWLEquivalentObjectPropertiesAxiom(sub_prop_set.getEntities());
//			addAxiom(eq_axiom, dl_ont,man);
//			OWLObjectProperty new_sub= null;
//			for (OWLObjectPropertyExpression sub_prop : sub_prop_set){
//				//pick one property from the set
//				new_sub= duplicator.duplicateObject(sub_prop);
//				break;
//			}
//			if (!new_sub.equals(null)){
//				//create subproperties axiom
//				OWLSubObjectPropertyOfAxiom sub_axiom = 
//					factory.getOWLSubObjectPropertyOfAxiom(new_sub, new_prop);
//				addAxiom(sub_axiom, dl_ont,man);
//			}
//
//		}
//	}
//	
//	/**************************************************************************
//	 * This method accepts an object property, and adds in the Dl Lite ontology
//	 * all the ObjectPropertyDomain Axioms retrieved by the reasoner for the 
//	 * input property. Given that the reasoner method for getting the domains
//	 * returns a set of sets of descriptions, where the elements in each subset
//	 * are equivalent descriptions, we just take one description per set, 
//	 * taking care that the selected description is a valid superclass 
//	 * expression in Dl Lite.
//	 * @param prop the input property
//	 * @param new_prop the double of the input property, to create the axiom
//	 * @param dl_ont the Dl Lite ontology
//	 * @param owl_ont the OWL ontology
//	 * @param reasoner the OWLReasoner
//	 * @param man the ontology manager
//	 * @param factory the data factory
//	 * @param ob the object duplicator
//	 * @param checkDLLite an instance of DLLiteGrammarChecker
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @throws , when interacting with the reasoner
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 *************************************************************************/
//	private void addObjectPropertyDomainAxiom (OWLObjectProperty prop,
//											   OWLObjectProperty new_prop,
//											   OWLOntology dl_ont, 
//											   OWLOntology owl_ont,  
//											   OWLReasoner reasoner,
//											   OWLOntologyManager man, 
//											   OWLDataFactory factory, 
//											   OWLObjectDuplicator ob ,
//											   DLLiteGrammarChecker checkDLLite ) 
//	throws OWLOntologyChangeException, 
//	OWLOntologyStorageException	 
//	{
//		//TODO see, just get one domain from the set of equivalent domains.
//		OWLClassExpression selected_dom = null;
//		OWLClassExpression aux_dom = null; 
//		NodeSet<OWLClass> doms = reasoner.getObjectPropertyDomains(prop, false);//getDomains(prop);
//		for (Node<OWLClass> set_dom : doms){
//			for (OWLClass dom: set_dom){
//				//select a dom from the se of equivalent doms obtained.	
//				//select initially the first class in the set and then
//				//if there is a valid superclass expression assign it
//				if (selected_dom == null) {
//						selected_dom = dom;
//				}
//				if (new_classes.containsKey(dom)){
//					if (checkDLLite.isDlLiteSuperClassExpression(new_classes.get(dom))){
//						selected_dom = new_classes.get(dom);
//					}
//				}
//				else if (new_NegClasses.containsKey(dom)){
//					if (checkDLLite.isDlLiteSuperClassExpression(new_NegClasses.get(dom))){
//						selected_dom = new_NegClasses.get(dom);
//					}
//				}
//				else if (checkDLLite.isDlLiteSuperClassExpression(dom)){
//					selected_dom = dom;
//				}
//			}
//			if (new_classes.containsKey(selected_dom)){
//				aux_dom = ob.duplicateObject(new_classes.get(selected_dom));
//			}
//			else if (new_NegClasses.containsKey(selected_dom)){
//				aux_dom = ob.duplicateObject(new_NegClasses.get(selected_dom));
//			}
//			else {
//				aux_dom = ob.duplicateObject(selected_dom);
//			}
//			
//			if (checkDLLite.isDlLiteSuperClassExpression(selected_dom)){
//				OWLObjectPropertyDomainAxiom axiom = 
//					factory.getOWLObjectPropertyDomainAxiom(new_prop, 
//															aux_dom);
//				addAxiom(axiom,dl_ont,man);
//			}
//			selected_dom = null;
//		}
//		
//	}
//	
//	/**************************************************************************
//	 * This method accepts a property and asks the reasoner whether it is a 
//	 * functional property. If it is a functional property, then we add the
//	 * corresponding axiom only when the functionality is valid in the 
//	 * Dl Lite ontology. Otherwise, we show a warning message explaining 
//	 * why the functionality is not valid, so that the user can choose 
//	 * to include in the Dl Lite ontology either the functionality or
//	 * the conflicting axiom.
//	 * @param prop the input property
//	 * @param new_prop the double of the input property
//	 * @param dl_ont the Dl Lite ontology
//	 * @param reasoner the OWLReasoner
//	 * @param man the ontology manager
//	 * @param factory the data factory
//	 * @param checkDLLite an instance of DLLiteGrammarChecker
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @throws , when interacting with the reasoner
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	private void addFunctionalObjectPropertyAxioms(OWLObjectProperty prop,
//												   OWLObjectProperty new_prop,
//												   OWLOntology dl_ont, 
//												   OWLReasoner reasoner,
//												   OWLOntologyManager man, 
//												   OWLDataFactory factory, 
//											DLLiteGrammarChecker checkDLLite ) 
//	throws OWLOntologyChangeException, 
//	OWLOntologyStorageException	 
//	{
//		if (prop.isFunctional(reasoner.getRootOntology())){ //reasoner.isFunctional(prop)){
//			//TODO see if it is ok
//			//create the axiom
//			OWLFunctionalObjectPropertyAxiom axiom = 
//						factory.getOWLFunctionalObjectPropertyAxiom(new_prop);
//			//We will check if property fuarg0nctionality is valid in Dl Lite
//			//and if it is not, just add a log saying for which properties 
//			//either the functionality or some rule can't be added.
//			try {
//				if(checkDLLite.isDlLiteFunctionalObjectPropertyAxiom(axiom,
//																   dl_ont)){
//					//TODO won't add the axiom in case it is not dl lite... see, 
//					//in case it is necesary to add anyway the axiom, do it without
//					//calling this method...
//					addAxiom(axiom,dl_ont,man);
//				}
//			} catch (FunctionalPropertySpecializedException e) {
//				// TODO Auto-generated catch block
//				//e.printStackTrace();
//				log.warn("The property " + String.valueOf(new_prop)+ " is " +
//						"Functional but, its functionality won't be added " +
//						"because it is already specialized in the Dl Lite " +
//						"ontology.");
//			} catch (FunctionalPropertyParticipatesInQualifiedExistentialException e) {
//				// TODO Auto-generated catch block
//				//e.printStackTrace();
//				log.warn("The property " + String.valueOf(new_prop)+ " is Functional " +
//						"but, its functionality won't be added because it already " +
//						"participates in a qualified existential in the Dl Lite " +
//						"ontology.");
//			}
//		}
//	}
//	
//	/**************************************************************************
//	 * This method adds the inverse properties axioms inferred by the reasoner
//	 * for the input property, into the Dl Lite ontology
//	 * @param prop the input property
//	 * @param new_prop
//	 * @param dl_ont the Dl Lite ontology
//	 * @param reasoner the OWLReasoner
//	 * @param man the ontology manager
//	 * @param factory the data factory
//	 * @param duplicator the object duplicator
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @throws , when interacting with the reasoner
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	private void addInverseObjectPropertiesAxioms (OWLObjectProperty prop,
//												   OWLObjectProperty new_prop,
//												   OWLOntology dl_ont, 
//												   OWLReasoner reasoner,
//												   OWLOntologyManager man, 
//												   OWLDataFactory factory,
//												   OWLObjectDuplicator duplicator)
//	throws OWLOntologyChangeException, 
//	OWLOntologyStorageException	 
//	{
//		Node<OWLObjectPropertyExpression> inv_props = reasoner.getInverseObjectProperties(prop);
//		for (OWLObjectPropertyExpression inv_prop : inv_props){
//			//for (OWLObjectProperty inv_prop: set_props){
//				OWLObjectPropertyExpression new_inv = duplicator.duplicateObject(inv_prop);
//				OWLInverseObjectPropertiesAxiom axiom = 
//					factory.getOWLInverseObjectPropertiesAxiom(new_prop, new_inv);
//				addAxiom(axiom, dl_ont, man);
//			//}
//		}
//	}
//	
//	/**************************************************************************
//	 * This method adds into de Dl Lite ontology, the inferred object property  
//	 * range axioms for the input property, only if they are valid in Dl Lite.  
//	 * @param prop the input property
//	 * @param new_prop the double of the input property
//	 * @param dl_ont the Dl Lite ontology
//	 * @param reasoner the OWLReasoner
//	 * @param man the ontology manager
//	 * @param factory the data factory
//	 * @param duplicator the object duplicator
//	 * @param checkDLLite an instance of DLLiteGrammarChecker
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @throws , when interacting with the reasoner
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	private void addObjectPropertyRangeAxioms(OWLObjectProperty prop,
//											  OWLObjectProperty new_prop,
//											  OWLOntology dl_ont, 
//											  OWLReasoner reasoner,
//											  OWLOntologyManager man, 
//											  OWLDataFactory factory, 
//											  OWLObjectDuplicator duplicator,
//											  DLLiteGrammarChecker checkDLLite)
//	throws OWLOntologyChangeException, 
//	OWLOntologyStorageException	 
//	{
//		OWLClassExpression aux_range = null;
//		NodeSet<OWLClass> ranges = reasoner.getObjectPropertyRanges(prop, false);
//		for (Node<OWLClass> range : ranges){
//			if (new_classes.containsKey(range)){
//				aux_range = duplicator.duplicateObject(new_classes.get(range));
//			} else if (new_NegClasses.containsKey(range)){
//				aux_range = duplicator.duplicateObject(new_NegClasses.get(range));
//			} else {
//				aux_range = duplicator.duplicateObject((OWLObject) range);
//			}
//			if (checkDLLite.isDlLiteSuperClassExpression(aux_range)){
//				OWLObjectPropertyRangeAxiom axiom = 
//						factory.getOWLObjectPropertyRangeAxiom(new_prop, 
//															   aux_range);
//				addAxiom(axiom,dl_ont,man);
//			}
//		}
//
//	}
//	
//	/**************************************************************************
//	 * Adds in the Dl Lite ontology the Object Property hierarchy, and other 
//	 * inferences made by the reasoner, related with object property objects, 
//	 * i.e. inferred: 
//	 * domains, ranges, functional object properties, symmetric object	
//	 * properties, subproperty axioms, equivalent properties axioms and   
//	 * inversed properties axioms.
//	 * <p>
//	 * For the functional property axioms, it only adds them if they don't 
//	 * conflict with other axioms already existing in the Dl lite ontology.
//	 * I.e. if in the Dl Lite ontology does not exist any subpropery axiom
//	 * where the functional property is on the right side, or if the property
//	 * is not included in a some restriction.
//	 * @param dl_ont the Dl Lite ontology
//	 * @param owl_ont the OWL ontology
//	 * @param reasoner the OWLReasoner
//	 * @param man the ontology manager
//	 * @param factory the data factory
//	 * @param ob the object duplicator
//	 * @param checkDLLite an instance of DLLiteGrammarChecker
//	 * @throws   
//	 * @throws OWLOntologyStorageException thrown by the invoqued method addAxiom
//	 * @throws OWLOntologyChangeException by the invoqued method addAxiom
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 *************************************************************************/
//	private void addDlLiteObjectProperties (OWLOntology dl_ont, 
//											OWLOntology owl_ont,  
//											OWLReasoner reasoner,
//											OWLOntologyManager man, 
//											OWLDataFactory factory, 
//											OWLObjectDuplicator ob ,
//											DLLiteGrammarChecker checkDLLite ) 
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException 
//	{ 
//		//get all the object properties
//		Set<OWLObjectProperty>ob_props=
//									owl_ont.getObjectPropertiesInSignature();
//		//for each one, look its hierarchy... 
//		for (OWLObjectProperty prop : ob_props ){
//			//not necessary:getDescendantProperties,getAncestorProperties, 
//			//getSuperProperties; 
//			OWLObjectProperty new_prop = ob.duplicateObject(prop);
//			//add hierachy
//			addSubObjectPropertyAxioms(prop,new_prop,reasoner, 
//									   dl_ont,man,factory,ob);
//			//get domains
//			addObjectPropertyDomainAxiom(prop,new_prop,dl_ont,owl_ont,
//										 reasoner,man,factory,ob,
//										 checkDLLite);
//			
//			//get functional object properties infered by the reasoner
//			addFunctionalObjectPropertyAxioms(prop,new_prop,dl_ont,reasoner,
//											  man,factory,checkDLLite);
//			//get symmetric  object properties infered by the reasoner
//			// TODO check that the reasoner implements isSymmetric
//			//reasoner.isDefined(OWLSymmetricObjectPropertyAxiom);
//			//try{
//				if (prop.isSymmetric(reasoner.getRootOntology())){
//					OWLSymmetricObjectPropertyAxiom axiom = 
//							  factory.getOWLSymmetricObjectPropertyAxiom(new_prop);
//					addAxiom(axiom,dl_ont,man);
//				}
//			//}catch( e1)
//			//{
//			//	log.debug(e1.getMessage(), e1);
//			//}
//			//get inverse props
//			addInverseObjectPropertiesAxioms(prop,new_prop,dl_ont,reasoner,
//											 man,factory,ob);
//			//get ranges
//			addObjectPropertyRangeAxioms(prop,new_prop,dl_ont,reasoner,man,
//										 factory,ob,checkDLLite);
//		}
//	}
//	
//
//	/**************************************************************************
//	 * This method adds the subproperties axioms and also the equivalent 
//	 * properties axioms.
//	 * <p>
//	 * The reasoner getSubDataProperties method, returns a set of sets,
//	 * where the elements of each set are equivalent properties. So we
//	 * take advantage of this to create the equivalent properties axioms. 
//	 * @param prop the property used to get the subproperties
//	 * @param new_prop the duplicated property.
//	 * @param reasoner the OWLReasoner
//	 * @param dl_ont the Dl Lite ontology
//	 * @param man the data manager
//	 * @param factory the data factory
//	 * @param ob the object duplicator
//	 * @throws 
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method 
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	 
//	private void addSubDataPropertyAxioms (OWLDataProperty prop,
//										   OWLDataProperty new_prop,
//										   OWLReasoner reasoner,
//										   OWLOntology dl_ont,
//										   OWLOntologyManager man,
//										   OWLDataFactory factory,
//										   OWLObjectDuplicator ob)
//	throws OWLOntologyChangeException, 
//	   	OWLOntologyStorageException 
//	{
//		NodeSet<OWLDataProperty> sub_props = reasoner.getSubDataProperties(prop, false);
//		for (Node<OWLDataProperty> sub_prop_set : sub_props){
//			//the elements in the sets are equivalent properties, 
//			//add the equivalent properties axioms here
//			OWLEquivalentDataPropertiesAxiom eq_axiom = 
//	  		  factory.getOWLEquivalentDataPropertiesAxiom(sub_prop_set.getEntities());
//	  		addAxiom(eq_axiom,dl_ont,man);
//	  		//now get one property in each set to create the subprop axiom
//	  		OWLDataProperty new_sub = null;
//			for (OWLDataProperty sub_prop : sub_prop_set){
//				new_sub = ob.duplicateObject(sub_prop);
//				break;
//			}
//			OWLSubDataPropertyOfAxiom axiom = factory.getOWLSubDataPropertyOfAxiom(new_sub,new_prop);
//			addAxiom(axiom,dl_ont,man);
//		}
//	}
//
//	/**************************************************************************
//	 * Accepts an OWLDataProperty, and adds in the Dl Lite ontology
//	 * all the data Property Domain Axioms retrieved by the reasoner for the 
//	 * input object. Given that the reasoner method for getting the domains
//	 * returns a set of sets of descriptions, where the elements in each subset
//	 * are equivalent descriptions, we just take one description per set, 
//	 * taking care that the selected description is a valid superclass 
//	 * expression in Dl Lite.
//	 * @param prop the input property
//	 * @param new_prop the double of the input property, to create the axiom
//	 * @param reasoner the OWLReasoner
//	 * @param dl_ont the Dl Lite ontology
//	 * @param man the data manager
//	 * @param factory the data factory
//	 * @param ob the object duplicator
//	 * @param checkDLLite an instance of DLLiteGrammarChecker
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @throws , when interacting with the reasoner
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	private void addDataPropertyDomainAxioms(OWLDataProperty prop,
//											 OWLDataProperty new_prop,
//											 OWLReasoner reasoner,
//											 OWLOntology dl_ont,
//											 OWLOntologyManager man,
//											 OWLDataFactory factory,
//											 OWLObjectDuplicator ob,
//											 DLLiteGrammarChecker checkDLLite)
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException 
//	{
//		//TODO see, just get one domain from the set of equivalent domains.
//		NodeSet<OWLClass> doms  = reasoner.getDataPropertyDomains(prop, true);
//		OWLClassExpression aux_dom = null;
//		OWLClassExpression selected_dom = null;
//		for (Node<OWLClass> set_dom : doms){
//			for (OWLClassExpression dom: set_dom){
//				//select a dom from the se of equivalent doms obtained.	
//				//select initially the first class in the set and then
//				//if there is a valid superclass expression assign it
//				if (selected_dom == null) {
//						selected_dom = dom;
//				}
//				if (new_classes.containsKey(dom)){
//					if (checkDLLite.isDlLiteSuperClassExpression(new_classes.get(dom))){
//						selected_dom = new_classes.get(dom);
//					}
//				}
//				else if (new_NegClasses.containsKey(dom)){
//					if (checkDLLite.isDlLiteSuperClassExpression(new_NegClasses.get(dom))){
//						selected_dom = new_NegClasses.get(dom);
//					}
//				}
//				else if (checkDLLite.isDlLiteSuperClassExpression(dom)){
//					selected_dom = dom;
//				}
//			}
//			if (new_classes.containsKey(selected_dom)){
//				aux_dom = ob.duplicateObject(new_classes.get(selected_dom));
//			}
//			else if (new_NegClasses.containsKey(selected_dom)){
//				aux_dom = ob.duplicateObject(new_NegClasses.get(selected_dom));
//			}
//			else {
//				aux_dom = ob.duplicateObject(selected_dom);
//			}
//			if (checkDLLite.isDlLiteSuperClassExpression(aux_dom)){
//				OWLDataPropertyDomainAxiom axiom = 
//					factory.getOWLDataPropertyDomainAxiom(new_prop, 
//														  aux_dom);
//				addAxiom(axiom,dl_ont,man);
//			}
//			selected_dom = null;
//		}
//
//	}
//	
//
//	/**************************************************************************
//	 * This method accepts a property and asks the reasoner whether it is a 
//	 * functional property. If it is a functional property, then we add the
//	 * corresponding axiom only when the functionality is valid in the 
//	 * Dl Lite ontology. Otherwise, we show a warning message explaining 
//	 * why the functionality is not valid, so that the user can choose 
//	 * to include in the Dl Lite ontology either the functionality or
//	 * the conflicting axiom.
//	 * @param prop the input property
//	 * @param new_prop the double of the input property
//	 * @param reasoner the OWLReasoner
//	 * @param dl_ont the Dl Lite ontology
//	 * @param man the data manager
//	 * @param factory the data factory
//	 * @param checkDLLite an instance of DLLiteGrammarChecker
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @throws , when interacting with the reasoner
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	private void addFunctionalDataPropertyAxioms (OWLDataProperty prop,
//												  OWLDataProperty new_prop,
//												  OWLReasoner reasoner,
//												  OWLOntology dl_ont,
//												  OWLOntologyManager man,
//												  OWLDataFactory factory,
//											DLLiteGrammarChecker checkDLLite)
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException 
//	{
//		if (prop.isFunctional(reasoner.getRootOntology())){
//			//TODO see if it is ok
//			//create the axiom
//			OWLFunctionalDataPropertyAxiom axiom = 
//						factory.getOWLFunctionalDataPropertyAxiom(new_prop);
//			//We will check if property fuarg0nctionality is valid in Dl Lite
//			//and if it is not, just add a log saying for which properties 
//			//either the functionality or some rule can't be added.
//			if (!checkDLLite.isDlLiteFunctionalDataPropertyAxiom(axiom, 
//																 dl_ont)){
//				log.warn("The property " + String.valueOf(new_prop)+ " is " +
//						"Functional but, its functionality won't be added " +
//						"because it is already specialized in the Dl Lite " +
//						"ontology."); 
//			}
//			//TODO won't add the axiom in case it is not dl lite... see, 
//			//in case it is necesary to add anyway the axiom, do it without
//			//calling this method...
//
//			addAxiom(axiom,dl_ont,man);
//		}
//
//	}
//
//
//	/**************************************************************************
//	 * This method adds the data property range axioms, inferred by the 
//	 * reasoner for the input property, into de Dl Lite ontology if they
//	 * are valid in Dl Lite.  
//	 * @param prop the input property
//	 * @param new_prop
//	 * @param reasoner the OWLReasoner
//	 * @param dl_ont the Dl Lite ontology
//	 * @param man the data manager
//	 * @param factory the data factory
//	 * @param ob the object duplicator
//	 * @param checkDLLite an instance of DLLiteGrammarChecker
//	 * @throws OWLOntologyChangeException, thrown by the addAxiom method
//	 * @throws OWLOntologyStorageException, thrown by the addAxiom method
//	 * @throws , when interacting with the reasoner
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 **************************************************************************/
//	private void addDataPropertyRangeAxioms(OWLDataProperty prop,
//											OWLDataProperty new_prop,
//											OWLReasoner reasoner,
//											OWLOntology dl_ont,
//											OWLOntologyManager man,
//											OWLDataFactory factory,
//											OWLObjectDuplicator ob,
//											DLLiteGrammarChecker checkDLLite)
//	throws OWLOntologyChangeException, 
//		OWLOntologyStorageException 
//	{
//		Set<OWLDataRange> ranges = prop.getRanges(reasoner.getRootOntology());
//		for (OWLDataRange range : ranges){
//			if (checkDLLite.isDlLiteDataRange(range)){
//				OWLDataRange new_range = ob.duplicateObject(range);
//				OWLDataPropertyRangeAxiom axiom = 
//								factory.getOWLDataPropertyRangeAxiom(new_prop, 
//																	 new_range);
//				addAxiom(axiom,dl_ont,man);
//			}
//		}
//
//	}
//
//	/**************************************************************************
//	 * Adds in the Dl Lite ontology the Data Property hierarchy, and other 
//	 * inferences made by the reasoner, related with data property objects, 
//	 * i.e. inferred: 
//	 * domains, ranges, functional object properties, subproperty axioms and
//	 * equivalent properties axioms.
//	 * <p>
//	 * For the functional property axioms, it only adds them if they don't 
//	 * conflict with other axioms already existing in the Dl lite ontology.
//	 * I.e. if in the Dl Lite ontology does not exist any subpropery axiom
//	 * where the functional property is on the right side.
//	 * @param dl_ont the Dl Lite ontology
//	 * @param owl_ont the OWL ontology
//	 * @param reasoner the OWL Reasoner
//	 * @param man the ontology manager
//	 * @param factory the data factory
//	 * @param ob the object duplicator
//	 * @param checkDLLite an instance of DLLiteGrammarChecker  
//	 * @throws  
//	 * @throws OWLOntologyStorageException thrown by the invoqued method addAxiom
//	 * @throws OWLOntologyChangeException by the invoqued method addAxiom
//	 * @see #addAxiom(OWLAxiom,OWLOntology,OWLOntologyManager)
//	 *************************************************************************/
//	private void addDlLiteDataProperties (OWLOntology dl_ont, 
//										  OWLOntology owl_ont,  
//										  OWLReasoner reasoner,
//										  OWLOntologyManager man, 
//										  OWLDataFactory factory, 
//										  OWLObjectDuplicator ob ,
//										  DLLiteGrammarChecker checkDLLite ) 
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException
//	{
//		//get all the object properties
//		//Set<OWLDataProperty>d_props = reasoner.getDataProperties(); 
//		//not available in OWLReasoner, only in Reasoner
//		Set<OWLDataProperty>d_props  = owl_ont.getDataPropertiesInSignature();
//		//For each property
//		for (OWLDataProperty prop : d_props ){
//			OWLDataProperty new_prop = ob.duplicateObject(prop);
//
//			//get the hierarchy
//			addSubDataPropertyAxioms (prop,new_prop,reasoner,dl_ont,
//									  man,factory,ob);
//			//get domains
//			addDataPropertyDomainAxioms(prop,new_prop,reasoner,dl_ont,man,
//										factory,ob,checkDLLite);
//			//is functional data property inferred by the reasoner
//			addFunctionalDataPropertyAxioms (prop,new_prop,reasoner,dl_ont,
//											 man,factory,checkDLLite);
//			//get ranges
//			addDataPropertyRangeAxioms(prop,new_prop,reasoner,dl_ont,man,
//									   factory,ob,checkDLLite);
//		}
//	}
//	/****************************************************************************
//	 * This method is to initialize the bit matrix. 
//	 * Given that a matrix has a fix number of columns and rows, 
//	 * we need to know in advance the size of the matrix. 
//	 * So we set it by getting the number of descendants of Thing.
//	 * Then we will only use the cells that we need. 
//	 * @param clazz this class will be Thing
//	 * @param reasoner to get the descendants of Thing
//	 * @throws 
//	 ***************************************************************************/
//	private void initializeBitMatrix(OWLClass clazz,
//									 OWLReasoner reasoner) 
//	{
//		//get the max number of OWLDescriptions that the Dl ontology can contain,
//		//in order to create the bitMatrix.
//		NodeSet<OWLClass> desc_thing = reasoner.getSubClasses(clazz, false);
//		int max_lengh = desc_thing.getFlattened().size(); 
//		//now initialize the bit matrix
//		matrix_Porders = new BitMatrix2D (max_lengh,max_lengh);
//	}
//	
//	/****************************************************************************
//	 * This recursive method will get the descendants of the class corresponding  
//	 * to the input index col,  and for each descendant will eliminate it in 
//	 * each ancestor.
//	 * For example, if we have the following subclass axioms:
//	 * (T,A), (T,B), (T,C), (A,C); where T,A,B and C 
//	 * are classes, and each pair represents a subclass axiom, then the algorithm 
//	 * will eliminate the axiom (T,C) given it is redundant.
//	 * @param col the current column of the bit matrix 
//	 * @param len the actual size of the bit matrix 
//	 * @param ancestors the lis of ancestors that made the recursive call, and 
//	 * where I should remove the redundant subclass axioms. 
//	 ***************************************************************************/
//	private void call_children (int col,
//								int len,
//								Set<Integer>ancestors ){
//		for (int row = 0; row < len; row++){
//			if (matrix_Porders.get(row, col)==true){
//				for (Integer anc:ancestors){
//					matrix_Porders.set(row, anc, false);
//					Set<Integer> new_ancestors = 
//						(Set<Integer>)
//							((HashSet<Integer>)ancestors).clone();
//					new_ancestors.add(col);
//					call_children(row, len, new_ancestors);
//					
//				}
//			}
//		}
//	}
//	
//	
//	/****************************************************************************
//	 * This method minimizes the bit matrix, and inserts the subclass axioms in 
//	 * the Dl Lite ontology.
//	 * Each element of the matrix M[r,c] will contain true if the class r is 
//	 * subclass of the class c. What we want to do is to avoid having redundant axioms
//	 * such that: if we have a subclass axiom (A,B), and a subclass axiom (B,C), 
//	 * then we don't want to have in the ontology a subclass axiom (A,C).
//	 * So, we need to eliminate these redundant axioms. 
//	 * The idea of the method is: for each element in the column corresponding a 
//	 * Thing, that has the true value (i.e. for each subclass axiom (child,thing)), 
//	 * we go through all the descendants of the child by exploring the column 
//	 * corresponding to the child. If we find that that a descendant of the child is 
//	 * true at Thing, then we make it false (at Thing). We repeat this procedure 
//	 * recursively for each descendant until we reach a leaf.
//	 * Then, once the matrix is already minimized, we insert in the Dl Lite 
//	 * ontology the subclass axioms corresponding to the remaining true values in
//	 * the matrix.
//	 * @param dlLiteOnt the Dl Lite Ontology
//	 * @param man the ontology manager
//	 * @param factory the factory, to create the axioms
//	 * @param ob an instance of OWLObjectDuplicator, used to duplicate the  
//	 * objects that will be included in the Dl Lite ontology
//	 * @param checkDLLite the grammar checker to verify that the axioms to add are
//	 * valid in Dl Lite
//	 * @throws OWLOntologyChangeException
//	 * @throws OWLOntologyStorageException
//	 ***************************************************************************/
//	private void addSubClassAxiomsFromMatrix(OWLOntology dlLiteOnt, 
//											 OWLOntologyManager man, 
//											 OWLDataFactory factory, 
//											 OWLObjectDuplicator ob ,
//											 DLLiteGrammarChecker checkDLLite ) 
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException{
//		
//		int len = matrix_Porders.get_current_length();
//		
//		//get the column of Thing
//		//we adopt a top-down approach, so we start from TOP = thing
//		IRI classURI = OWLRDFVocabulary.OWL_THING.getIRI();
//		OWLClass top = factory.getOWLClass(classURI);
//		int top_col = matrix_Porders.get_class_index(top);
//		
//		//for each row in the column of thing, we will explore the 
//		//children
//		for (int top_row = 0; top_row < len; top_row++){
//			if (matrix_Porders.get(top_row, top_col)== true){
//				Set<Integer> ancestors = new HashSet<Integer> ();
//				ancestors.add(top_col);
//				call_children(top_row,len, ancestors);
//			}
//		}
//		
//		//Finally, create the subclass axiom
//		for (int r=0; r<len; r++){
//			for (int c=0; c<len; c++){
//				if (matrix_Porders.get(r, c)==true){
//					OWLClassExpression sub = matrix_Porders.get_class_desc(r);
//					OWLClassExpression new_sub= ob.duplicateObject(sub);
//					OWLClassExpression sup = matrix_Porders.get_class_desc(c);
//		
//					OWLClassExpression new_super= ob.duplicateObject(sup);
//					OWLSubClassOfAxiom axiom = 
//								factory.getOWLSubClassOfAxiom(new_sub,new_super);
//					addAxiom(axiom, dlLiteOnt,man);
//					
//				}
//			}
//		}
//	}
//
//	
//	/***************************************************************************
//	 * The class ChainInclusion is a very simple class for storing
//	 * components of chain inclusions, namely the left-hand side named class,
//	 * the sequence of roles participating in the chain and
//	 * the filler class (a named class)
//	 ***************************************************************************/
//	public class ChainInclusion extends Object implements Comparable<ChainInclusion>{
//		OWLClassExpression left, right;
//		LinkedList<OWLObjectPropertyExpression> roles;
//		//int hashCode;
//		
//		ChainInclusion(OWLClassExpression left, LinkedList<OWLObjectPropertyExpression> reversedRoles, OWLClassExpression right)
//		{
//			this.left = left;
//			this.roles = reversedRoles;
//			this.right = right;
//		//	hashCode = this.toString().hashCode();
//		}
//
//		public OWLClassExpression getRight() {
//			return right;
//		}
//		
//		public OWLClassExpression getLeft() {
//			return left;
//		}
//		
//		public LinkedList<OWLObjectPropertyExpression> getRoles() {
//			return roles;
//		}
//
////		public boolean equals(Object obj)
////		{
////			ChainInclusion chain = (ChainInclusion)obj;
////			
////			if(roles.size() != chain.getRoles().size())
////				return false;
////			
////			boolean equalRoles = true;
////			for(int i=0; i<roles.size(); i++)
////			{
////				OWLObjectProperty prop1 = roles.get(i);
////				OWLObjectProperty prop2 = chain.getRoles().get(i);
////				if(!prop1.toString().equals(prop2.toString())) {
////						equalRoles = false;
////						break;
////				}
////			}
////			return left.toString().equals(chain.getLeft().toString()) && right.toString().equals(chain.getRight().toString()) && equalRoles;
////		}
//		
////		public int hashCode() {
////			return this.toString().hashCode();//hashCode;//
////		}
//		
////		public String toString()
////		{
////			return left.toString() + " " + roles.toString() + " " + right.toString();
////		}
//
//		public int compareTo(ChainInclusion chain) {
//			int res = left.toString().compareTo(chain.getLeft().toString());
//			if(res == 0)
//			{
//				res = roles.size() - chain.getRoles().size();
//				if(res == 0)
//				{
//					for(int i=0; i<roles.size(); i++)
//					{
//						OWLObjectPropertyExpression prop1 = roles.get(i);
//						OWLObjectPropertyExpression prop2 = chain.getRoles().get(i);
//						res = prop1.toString().compareTo(prop2.toString());
//						if(res != 0) {
//							return res;
//						}
//					}
//					res = right.toString().compareTo(chain.getRight().toString());
//				}
//			}
//			return res;
//		}
//	}
//		
//	/****************************************************************************
//	 * The method getReversedSequenceOfRoles returns the reversed sequence of roles 
//	 * corresponding to the current count, which can be seen as a decimal 
//	 * representation of the number in the base system. 
//	 * Count is converted to this number, where each "digit" is the index of a role  
//	 * @param count the serial number of the current combination of roles
//	 * @param base total number of roles in propsArray
//	 * @param propsArray the array of roles to combine chains
//	 ***************************************************************************/
//	private LinkedList<OWLObjectPropertyExpression> getReversedSequenceOfRoles(long count, int base, Object[] propsArray)
//	{
//		LinkedList<OWLObjectPropertyExpression> roles = new LinkedList<OWLObjectPropertyExpression>();
//		
//		int lowByte;
//		long highByte;
//		highByte = count;
//		while(highByte > 0)
//		{
//			lowByte = (int)((highByte-1) % base);
//			highByte = (highByte - lowByte) / base;
//			roles.addLast((OWLObjectPropertyExpression)propsArray[lowByte]);
//		}
//		
//		
//		return roles;
//	}
//	
//	/****************************************************************************
//	 * The method getQualifiedChain returns the OWLClassExpression of the chain 
//	 * consisting of the roles in reversedRoles and qualified by the filler class
//	 * @param reversedRoles the reversed sequence of roles in the chain
//	 * @param filler the named class to qualify the chain
//	 * @param factory the factory to create the axioms
//	 ***************************************************************************/
//	private OWLClassExpression getQualifiedChain(LinkedList<OWLObjectPropertyExpression> reversedRoles, 
//											OWLClassExpression filler, 
//											OWLDataFactory factory) {
//		
//		OWLClassExpression qualifiedChain = filler;
//		for(OWLObjectPropertyExpression oprop : reversedRoles)
//		{
//			qualifiedChain = factory.getOWLObjectSomeValuesFrom(oprop, qualifiedChain);
//		}
//		
//		return qualifiedChain;
//	}
//	
//	/****************************************************************************
//	 * The method isOriginalNamedClass checks whether clazz is an original named class or an 
//	 * introduced by the algorithm name
//	 * @param clazz the named class
//	 ***************************************************************************/
//	private boolean isOriginalNamedClass(OWLClassExpression clazz)
//	{
//		return clazz instanceof OWLClass && !clazz.toString().startsWith("Not_") && !clazz.toString().startsWith("new");
//	}
//	
//	/****************************************************************************
//	 * The method isNegatedNameClass checks whether clazz is a new name for a 
//	 * negated named class introduced by the algorithm
//	 * @param clazz the named class
//	 ***************************************************************************/
//	private boolean isNegatedNameClass(OWLClass clazz)
//	{
//		return clazz.toString().startsWith("Not_");
//	}
//	
//	
//	private boolean isSomeRestriction(OWLClassExpression clazz) {
//		// TODO Auto-generated method stub
//		return clazz instanceof OWLDataMinCardinality || clazz instanceof OWLObjectSomeValuesFrom;
//	}
//
//
//	/***************************************************************************
//	 * The method getRightClasses returns a set of classes to qualify chains
//	 * in chain inclusions
//	 * @param owl_ont the OWL ontology
//	 * @param reasoner the ontology reasoner
//	 * @throws  thrown by the invoqued methods of reasoner
//	 ***************************************************************************/
//	private Set<OWLClassExpression> getRightClasses(	OWLOntology owl_ont,  
//											OWLReasoner reasoner,
//											OWLDataFactory factory) 
//	{
//		Set<OWLClassExpression> rightClasses = new HashSet<OWLClassExpression>();
//		
//		
//		//**********all the named classes A, newA, plain, not optimized************
//		Set<OWLClass> classes = owl_ont.getClassesInSignature();
//		// we delete Thing from the set of named classes so we don't check if not needed
//		// unqualified chain inclusions
//		classes.remove(owlClassThing);
//		// and classes equivalent to it
//		Node<OWLClass> equivThingClasses = reasoner.getEquivalentClasses(owlClassThing);
//		classes.removeAll(equivThingClasses.getEntities());
//		
//		// we also delete Not_Thing since we are interested in 
//		// meaningful chain inclusions
//		classes.remove(owlClassNothing);
//		// and classes equivalent to it
//		Node<OWLClass> equivNothingClasses = reasoner.getEquivalentClasses(owlClassNothing);
//		classes.removeAll(equivNothingClasses.getEntities());
//		
//		rightClasses.addAll(classes);
//
//		Set<OWLDataProperty> dprops = owl_ont.getDataPropertiesInSignature();
//		for(OWLDataProperty prop:dprops)
//		{
//			rightClasses.add(factory.getOWLDataMinCardinality(1, prop));
//		}
//
//		return rightClasses;
//	}
//		
//	/***************************************************************************
//	 * The method getLeftClasses returns a set of classes to be on the lhs
//	 * of chain inclusions
//	 * @param owl_ont the OWL ontology
//	 * @param reasoner the ontology reasoner
//	 * @throws  thrown by the invoqued methods of reasoner
//	 ***************************************************************************/
//	private Set<OWLClassExpression> getLeftClasses(OWLOntology owl_ont,  
//										OWLReasoner reasoner,
//										OWLDataFactory factory) 
//	{
//		Set<OWLClassExpression> leftClasses = new HashSet<OWLClassExpression>();
//	
//		
//		//**********all the named classes A, newA, plain, not optimized************
//		Set<OWLClass> classes = owl_ont.getClassesInSignature();
//		// we delete Thing from the set of named classes so we don't check if not needed
//		// unqualified chain inclusions
//		classes.remove(owlClassThing);
//		// and classes equivalent to it
//		Node<OWLClass> equivThingClasses = reasoner.getEquivalentClasses(owlClassThing);
//		classes.removeAll(equivThingClasses.getEntities());
//		
//		// we also delete Nothing since we are interested in 
//		// meaningful chain inclusions
//		classes.remove(owlClassNothing);
//		// and classes equivalent to it
//		Node<OWLClass> equivNothingClasses = reasoner.getEquivalentClasses(owlClassNothing);
//		classes.removeAll(equivNothingClasses.getEntities());
//		
//		leftClasses.addAll(classes);
//		
//		Set<OWLObjectProperty> oprops = owl_ont.getObjectPropertiesInSignature();
//		for(OWLObjectProperty prop:oprops)
//		{
//			leftClasses.add(factory.getOWLObjectSomeValuesFrom(prop, owlClassThing));
//			leftClasses.add(factory.getOWLObjectSomeValuesFrom(prop.getInverseProperty(), owlClassThing));
//		}
//		
//		// TODO check that it is really needed
//		Set<OWLDataProperty> dprops = owl_ont.getDataPropertiesInSignature();
//		for(OWLDataProperty prop:dprops)
//		{
//			leftClasses.add(factory.getOWLDataMinCardinality(1, prop));
//		}
//
//		return leftClasses;
//	}
//
//	private Object[] getBasicRoles(OWLOntology owl_ont) {
//	
//		Set<OWLObjectPropertyExpression> basicRoles = new HashSet<OWLObjectPropertyExpression>();
//		Set<OWLObjectProperty> props = owl_ont.getObjectPropertiesInSignature();
//		for(OWLObjectProperty prop : props)
//		{
//			basicRoles.add(prop);
//			basicRoles.add(prop.getInverseProperty());
//		}
//		return basicRoles.toArray();
//	}
//
//	
//	/****************************************************************************
//	 * The method checkChainInclusionsBF checks for chain inclusions up to the length 
//	 * levelOfRoleNesting that hold in the ontology with all the named classes in 
//	 * the ends, both original and introduced names.
//	 * It enumerates all the possible chains and named classes in the ends,
//	 * if there is a chain inclusion implied by the ontology, the method stores it in 
//	 * chainInclusions, where for each named class chainInclusions has a set of chain 
//	 * inclusions starting from this class (so later it is easier to concatenate chains) 
//	 * @param owl_ont the OWL ontology
//	 * @param reasoner the ontology reasoner
//	 * @param factory the factory to create the OWL descriptions
//	 * @throws  thrown by the invoqued method reasoner.isSubClassOf
//	 ***************************************************************************/
//	private void checkChainInclusionsBF(OWLOntology owl_ont,  
//								OWLReasoner reasoner,
//								OWLDataFactory factory,
//								int levelOfRoleNesting)
//	{
//		// clear stored chain inclusions
//		chainInclusions.clear();
//	
//		// sets of classes that are not equivalent to bottom or to top
//		Set<OWLClassExpression> leftClasses = getLeftClasses(owl_ont, reasoner, factory);
//		Set<OWLClassExpression> rightClasses = getRightClasses(owl_ont, reasoner, factory);
//	
//		
//		Object[] propsArray = getBasicRoles(owl_ont);
//		
//		int numberOfProperties = propsArray.length;
//		long maxNumberOfCombinations = (long)Math.pow(numberOfProperties, levelOfRoleNesting);
//		
//		// checking chain inclusions for every class being on the left-hand side
//		for (OWLClassExpression left : leftClasses){
//			Set<ChainInclusion> chains = new TreeSet<ChainInclusion>();
//
//			// we start from 1 (not from 0), later, when it is needed, 
//			// we substract 1 for getting the correct index
//			long count = 1;
//			
//			// we enumerate all the chains consisting of sub roles
//			while(count <= maxNumberOfCombinations)
//			{
//				LinkedList<OWLObjectPropertyExpression> reversedSequenceOfRoles = 
//					getReversedSequenceOfRoles(count, numberOfProperties, propsArray);
//			
//				boolean atLeastOneQualChain = false;
//				
//				// checking for all possible classes as fillers of the chain
//				for (OWLClassExpression right : rightClasses){
//					OWLClassExpression superChain = getQualifiedChain(reversedSequenceOfRoles, right, factory);
//					if(	reasoner.isEntailed(new OWLSubClassOfAxiomImpl(left, superChain, null)) ){
//						ChainInclusion chainInclusion = new ChainInclusion(left, reversedSequenceOfRoles, right);
//						chains.add(chainInclusion);
//						atLeastOneQualChain = true;
//					}
//				}
//				
//				// only if there was no qualified chain inclusion with the initial reversedRoles
//				// we check for the unqualified one
//				if(!atLeastOneQualChain)
//				{
//					OWLClassExpression superChain = getQualifiedChain(reversedSequenceOfRoles, owlClassThing, factory);
//					if(!left.equals(superChain) && reasoner.isEntailed(new OWLSubClassOfAxiomImpl(left, superChain, null))){
//						ChainInclusion chainInclusion = new ChainInclusion(left, reversedSequenceOfRoles, owlClassThing);
//						chains.add(chainInclusion);
//					}
//				}
//			
//				// enumerating all the different combinations of role chains
//				count++;
//			}
//			
//			// store for each named class the set of derived chains
//			chainInclusions.put(left, chains);
//		}
//		
////		log.info("Detected chains");
////		for(OWLClass key : chainInclusions.keySet())
////		{
////			Set<ChainInclusion> chains = chainInclusions.get(key);
////			for(ChainInclusion chain : chains)
////				log.info(chain.getLeft() + " " + chain.getRoles() + " " + chain.getRight());
////		}
//	}
//
//
//	/****************************************************************************
//	 * The method assembleChains recursively goes throw the chain inclusions starting from 
//	 * original named classes and tries to "close" them by other named classes,
//	 * f.ex., A \ISA \exists R1 . Anew1, Anew1 \ISA \exists R2 . A' =>
//	 * A \ISA \exists R1 \exists R1 . \exists R2 . A'.
//	 * We allow to call the method recursively for the same class, however we check
//	 * that the length of the chain is not bigger than maximumChainLength
//	 * If it is not possible to close a chain inclusion, then it is closed by Thing. 
//	 * @param clazz the current class for which OWL ontology
//	 * @param accumulatedChain the ontology reasoner
//	 * @param left the factory to create the OWL descriptions
//	 ***************************************************************************/
//	private void assembleChains(OWLClassExpression clazz, LinkedList<OWLObjectPropertyExpression> accumulatedChain, OWLClassExpression left)
//	{
//		Set<ChainInclusion> chains = chainInclusions.get(clazz);
//		// if the length of the accumulated chain exceeds the maximum length of queries
//		// (system parameter), then we stop recursion here
//		if(accumulatedChain.size() >= maximumChainLength) {
//			int numberOfExtraRoles = accumulatedChain.size() - maximumChainLength;
//			for(int i=0; i<numberOfExtraRoles; i++)
//				accumulatedChain.poll();
//
//			// clazz is not a named class since it's a recursive call
//			// so we qualify by Thing
//			chainAxioms.add(new ChainInclusion(left, accumulatedChain, owlClassThing));
//			return;
//		}
//		// else if for the current clazz we can find extensions of chains
//		// we add chains to the accumulated chain
//		else if(chains != null && chains.size() > 0) {
//			for(ChainInclusion chain : chains)
//			{
//				// since it is reversed we add accumulated chain to the end of current chain
//				LinkedList<OWLObjectPropertyExpression> prolongatedChain = (LinkedList<OWLObjectPropertyExpression>)chain.getRoles().clone();
//				prolongatedChain.addAll(accumulatedChain);
//				
//				// if it is an original name, we stop recursion
//				if(isOriginalNamedClass(chain.getRight()) || isSomeRestriction(chain.getRight())) {
//					chainAxioms.add(new ChainInclusion(left, prolongatedChain, chain.getRight()));
//				}
//				// else we continue recursively for the filler of the chain
//				else {
//					assembleChains(chain.getRight(), prolongatedChain, left);
//				}
//			}
//		}
//		// else if chains is null then we should stop here.
//		// Checking that it is not the first call of the method
//		else if(accumulatedChain.size() > 0) {
//			chainAxioms.add(new ChainInclusion(left, accumulatedChain, owlClassThing));
//		}
//	}
//
//
//	/****************************************************************************
//	 * The method concatenateChainInclusions concatenates the chain inclusions 
//	 * at the newly introduced named classes, so the output ontology does not 
//	 * contain new names.  
//	 * For every original named class it calls the method assembleChains that
//	 * recursively assebmles all the possible chains of the form
//	 * A \ISA \exists R1 ... \exists Rn . A', with A, A' original named classes. 
//	 * @param owl_ont the OWL ontology
//	 ***************************************************************************/
//	private void concatenateChainInclusions(OWLOntology owl_ont)
//	{
//		// the output ontology should not contain the extended alphabet
//		// so we apply the limit on chain length
//		if(withoutNewNames){
//			// TODO as in functional programing, if we went through some recursive steps already, do not
//			// repeat, but take advantage of the history
//
//			Set<OWLClassExpression> basicClasses = chainInclusions.keySet();
//			for(OWLClassExpression left : basicClasses)
//			{
//				// for every original named class or ER we try to assemble chains
//				if(!(left instanceof OWLClass) || isOriginalNamedClass(left))
//				{
//					assembleChains((OWLClassExpression)left, new LinkedList<OWLObjectPropertyExpression>(), left);
//				}
//				
//			}
//		}
//		// if there is no restriction on extending the alphabet
//		// we just output all detected chain inclusions
//		else {
//			for(OWLClassExpression key : chainInclusions.keySet())
//			{
//				Set<ChainInclusion> chains = chainInclusions.get(key);
//				chainAxioms.addAll(chains);
//			}
//		}
//	}
//	
//	/****************************************************************************
//	 * The method addChainAxioms adds all the chain axioms to the dl_ont 
//	 * @param dl_ont the DL-Lite ontology
//	 * @param owl_ont the OWL ontology
//	 * @param reasoner the ontology reasoner
//	 * @param factory the factory to create the OWL descriptions
//	 * @throws OWLOntologyChangeException thrown by the invoqued method addAxiom
//	 * @throws OWLOntologyStorageException thrown by the invoqued method addAxiom
//	 ***************************************************************************/
//	private void addChainAxioms(OWLOntology dl_ont, 
//			OWLOntology owl_ont,  
//			OWLOntologyManager manager,
//			OWLDataFactory factory)
//	throws OWLOntologyChangeException, 
//	OWLOntologyStorageException
//	{
//
//		for(ChainInclusion chainAxiom : chainAxioms)
//		{
//			OWLClassExpression chain = getQualifiedChain(chainAxiom.getRoles(), chainAxiom.getRight(), factory);
//			OWLSubClassOfAxiom axiom = factory.getOWLSubClassOfAxiom(chainAxiom.getLeft(), chain);
//			addAxiom(axiom,dl_ont,manager);
//		}
//		
//	}
//
//	private int getRoleDepth(OWLClassExpression clazz)
//	{
//		/****************************************************************************
//		 * The method quantNesting recursively calculates the level of role nesting
//		 * in a class. If it is a some or all restriction, it calculates the level of
//		 * nesting for the filler class and returns the value + 1. In the case of 
//		 * intersection or union it returns the maximum value of participating classes.
//		 * In other cases it return 0.
//		 * @param clazz the current class
//		 ***************************************************************************/
//		OWLClassExpression clazzNNF = clazz.getNNF();
//		
//		if(clazzNNF instanceof OWLClass)
//			return 0;
//		else if(clazzNNF instanceof OWLObjectSomeValuesFrom)
//		{
//			return 1 + getRoleDepth(((OWLObjectSomeValuesFrom)clazzNNF).getFiller());
//		}
//		else if(clazzNNF instanceof OWLObjectAllValuesFrom)
//		{
//			return 1 + getRoleDepth(((OWLObjectAllValuesFrom)clazzNNF).getFiller());
//		}
//		// we do not need to check for Max Cardinality Restriction
//		// since it is a negative information
//		else if(clazzNNF instanceof OWLObjectMinCardinality)
//		{
//			return 1 + getRoleDepth(((OWLObjectMinCardinality)clazzNNF).getFiller());
//		}
//		else if(clazzNNF instanceof OWLObjectIntersectionOf)
//		{
//			int level=0;
//			int locLevel;
//			Set<OWLClassExpression> operands = 
//				  ((OWLObjectIntersectionOf)clazzNNF).getOperands();
//			for (OWLClassExpression op : operands){
//				locLevel = getRoleDepth(op);
//				if(level < locLevel)
//					level = locLevel;
//			}
//			return level;
//		}
//		else if(clazzNNF instanceof OWLObjectUnionOf)
//		{
//			int level=0;
//			int locLevel;
//			Set<OWLClassExpression> operands = ((OWLObjectUnionOf)clazzNNF).getOperands();
//			for (OWLClassExpression op : operands){
//				locLevel = getRoleDepth(op);
//				if(level < locLevel)
//					level = locLevel;
//			}
//			return level;
//		}
//		else if(clazzNNF instanceof OWLDataSomeValuesFrom)
//		{
//			return 1;
//		}
//		else if(clazzNNF instanceof OWLDataAllValuesFrom)
//		{
//			return 1;
//		}
//		else if(clazzNNF instanceof OWLDataMinCardinality)
//		{
//			return 1;
//		}
//		return 0;
//	}
//	
//	/****************************************************************************
//	 * The method levelOfQuantificationNesting calculates the parameter maximum
//	 * level of nesting of role quantifications  
//	 * @param ontology the original ontology
//	 ***************************************************************************/
//	private int getRoleDepth(OWLOntology ontology)
//	{
//		int level = 0;
//		int locLevel;
//		
//		// TODO check that all the cases are considered
//		// add number restrictions
//		
//		Set<OWLLogicalAxiom> axioms = ontology.getLogicalAxioms();
//		for(OWLLogicalAxiom l_ax: axioms)
//		{
//			//class axiom
//			if(l_ax instanceof OWLClassAxiom){
//				if (l_ax instanceof OWLSubClassOfAxiom){
//					locLevel = Math.max(
//							getRoleDepth(((OWLSubClassOfAxiom) l_ax).getSubClass()),
//							getRoleDepth(((OWLSubClassOfAxiom) l_ax).getSuperClass()));
//					if(level < locLevel)
//						level = locLevel;
//				}
//				//equivalent classes axiom
//				else if (l_ax instanceof OWLEquivalentClassesAxiom){
//		  			Set<OWLClassExpression> des = ((OWLEquivalentClassesAxiom)l_ax).getClassExpressions();
//					for(OWLClassExpression d: des){
//						locLevel = getRoleDepth(d);
//						if(level < locLevel)
//							level = locLevel;
//					}
//	  			}
//	  			//disjoint classes axiom
//				else if (l_ax instanceof OWLDisjointClassesAxiom){
//					Set<OWLClassExpression> des = ((OWLDisjointClassesAxiom)l_ax).getClassExpressions();
//					for(OWLClassExpression d: des){
//						locLevel = getRoleDepth(d);
//						if(level < locLevel)
//							level = locLevel;
//					}
//				}
//				//disjoint union axiom
//				else if (l_ax instanceof OWLDisjointUnionAxiom){
//					Set<OWLClassExpression> des = ((OWLDisjointUnionAxiom)l_ax).getClassExpressions();
//					for(OWLClassExpression d: des){
//						locLevel = getRoleDepth(d);
//						if(level < locLevel)
//							level = locLevel;
//					}
//				}
//			}
//			//object property axiom
//			else if(l_ax instanceof OWLObjectPropertyAxiom){
//				if (l_ax instanceof OWLObjectPropertyDomainAxiom){
//					locLevel = getRoleDepth(((OWLObjectPropertyDomainAxiom)l_ax).getDomain());
//					if(locLevel < 1)
//						locLevel = 1;
//					if(level < locLevel)
//						level = locLevel;
//				}
//				else if (l_ax instanceof OWLObjectPropertyRangeAxiom){
//					locLevel = getRoleDepth(((OWLObjectPropertyRangeAxiom)l_ax).getRange());
//					if(locLevel < 1)
//						locLevel = 1;
//					if(level < locLevel)
//						level = locLevel;
//				}
//				else if (l_ax instanceof OWLSubObjectPropertyOfAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLSubPropertyChainOfAxiom){
//					level = ((OWLSubPropertyChainOfAxiom)l_ax).getPropertyChain().size();
//				}
//				else if (l_ax instanceof OWLEquivalentObjectPropertiesAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLDisjointObjectPropertiesAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLInverseObjectPropertiesAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLFunctionalObjectPropertyAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLInverseFunctionalObjectPropertyAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLReflexiveObjectPropertyAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLIrreflexiveObjectPropertyAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLSymmetricObjectPropertyAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLAsymmetricObjectPropertyAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLTransitiveObjectPropertyAxiom){
//					// TODO
//					// as a chain role inclusion it makes the role depth to be 2,
//					// but it does not add new information
//				}
//			}
//			//data property axiom
//			else if (l_ax instanceof OWLDataPropertyAxiom){
//				if (l_ax instanceof OWLDataPropertyDomainAxiom){
//					locLevel = getRoleDepth(((OWLDataPropertyDomainAxiom)l_ax).getDomain());
//					if(level < locLevel)
//						level = locLevel;
//				}
//				else if (l_ax instanceof OWLDataPropertyRangeAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLSubDataPropertyOfAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLEquivalentDataPropertiesAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLDisjointDataPropertiesAxiom){
//					// TODO
//				}
//				else if (l_ax instanceof OWLFunctionalDataPropertyAxiom){
//					// TODO
//				}
//			}
//			// membership assertion
//			else if (l_ax instanceof OWLIndividualAxiom){
//				if (l_ax instanceof OWLClassAssertionAxiom){
//					locLevel = getRoleDepth(((OWLClassAssertionAxiom)l_ax).getClassExpression());
//					if(level < locLevel)
//						level = locLevel;
//					}
//	  		}
//		}
//		return level;
//	}
//	
//   /***************************************************************************
//	*  This method returns a DL-Lite ontology, which is an approximation of the
//	*  OWL ontology passed by parameters. Other input parameters are an OWL 
//	*  ontology manager, an OWL ontology reasoner and a logical IRI of the 
//	*  output DL Lite ontology. The manager and the ontology must be already
//	*  initialized and the IRI must be mapped to a physical IRI in the manager.
//	*  The manager is needed in order to be able to create the new ontology,
//	*  the reasoner performs reasoning and is required to provide the basic
//	*  functionality (subclasses of a class, subproperties of a property).
//	*  <p> 
//	*  There are two approximation modes: simple and with full chains. In the
//	*  case of simple approximation the output ontology is a DL-Lite_A ontology
//	*  that captures the basic concept hierarchy plus concept inclusions with
//	*  qualified existentials on the right hand side.
//	*  <p> 
//	*  Approximation with full chains returns a sound and complete DL-Lite_A 
//	*  approximation. More precisely, the output ontology is a DL-Lite_A 
//	*  ontology that contains the simple approximation and inclusions 
//	*  of the form B \isa ER1 ... ERn . B' with B, B' basic classes. The 
//	*  result captures all existential chains implied by the original ontology.
//	*  
//	*  
//	* @param owl_ont the input OWL 2 ontology
//	* @param manager the OWL ontology manager 
//	* @param reasoner the OWL reasoner
//	* @param uri_dl_ont the IRI of the approximated DL-Lite ontology
//	* @return the Dl Lite ontology, which is the semantic approximation of 
//	* an OWL ontology.
//	* 
//	* @throws 
//	* @throws OWLOntologyChangeException
//	* @throws OWLOntologyStorageException
//	* @throws OWLOntologyCreationException
//	* 
//	* @see #initializeDlLiteOnt(OWLOntology, OWLOntology, OWLOntologyManager)
//	* @see #duplicateOntology(OWLOntology, OWLOntologyManager, OWLDataFactory,
//	*  OWLObjectDuplicator)
//	* @see #completeOwlOnt(OWLOntology, OWLOntologyManager)
//	* @see #addDlLiteHierarchy(OWLClassExpression, Set, Set, OWLOntology, 
//	* OWLReasoner, OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator, 
//	* DLLiteGrammarChecker)
//	* @see #addDlLiteInconsistentClassesAxioms(OWLOntology, OWLOntology, 
//	* OWLReasoner, OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator,
//	*  DLLiteGrammarChecker)
//	* @see #addDlLiteObjectProperties(OWLOntology, OWLOntology, OWLReasoner, 
//	* OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator, 
//	* DLLiteGrammarChecker)
//	* @see #addDlLiteDataProperties(OWLOntology, OWLOntology, OWLReasoner, 
//	* OWLOntologyManager, OWLDataFactory, OWLObjectDuplicator, 
//	* DLLiteGrammarChecker)  
//	**************************************************************************/
//	public OWLOntology approximate(OWLOntology owl_ont, 
//							OWLOntologyManager manager, 
//							OWLReasoner reasoner, 
//							IRI uri_dl_ont)
//	throws OWLOntologyChangeException, 
//		   OWLOntologyStorageException, 
//		   OWLOntologyCreationException
//	{
//
//		// set size and start progress monitor
//		long size = 3;
//		if(fullChains) {
//			size += 2;
//		}
//		for(ProgressMonitor monitor : progressMonitors)
//		{
//			monitor.setSize(size);
//			monitor.setStarted();
//		}
//		
//		// Create all the needed objects	
//		OWLDataFactory factory = manager.getOWLDataFactory();
//		OWLObjectDuplicator ob = new OWLObjectDuplicator(factory);
//		DLLiteGrammarChecker checkDLLite = new DLLiteGrammarChecker(fullChains);
//
//		
//		//Create and Inizialize the dl lite ontology
//		
//		OWLOntology dl_ont = manager.createOntology(uri_dl_ont);
//		log.info("Created dl ontology : " + dl_ont.getOntologyID().getOntologyIRI());
//		//Initialize the DL-Lite Ontology
//		initializeDlLiteOnt(owl_ont, dl_ont, manager);
//		
//		
//		// uris for the working ontology
//		IRI phys_uri_working_ont = createIRIWithSuffix(dl_ont.getOntologyID().getOntologyIRI(), "temp");
//		IRI uri_working_ont = createIRIWithSuffix(dl_ont.getOntologyID().getOntologyIRI(), "temp");
//		manager.addIRIMapper(new SimpleIRIMapper(uri_working_ont, phys_uri_working_ont));
//
//
//		//Duplicate the OWL ontology, so we can modify it.
//		OWLOntology new_ont = duplicateOntology(owl_ont, manager, factory, 
//			//	ob, 
//				uri_working_ont);
//		//Complete the duplicated ontology with the new names
//		//Here enumerating the logical axioms is performed 
//		OWLOntology complete_owl_ont = completeOwlOnt(new_ont, manager, checkDLLite);
//
//
//		//Second part
//		
//		// Load the workng ontology into the reasoner.  
//		Set<OWLOntology> importsClosure = manager.getImportsClosure(complete_owl_ont);
//		reasoner.loadOntologies(importsClosure);
//		
//		//Asks the reasoner to classify the ontology.  
//		reasoner.classify();
//		for(ProgressMonitor monitor : progressMonitors)
//		{
//			monitor.setProgress(1);
//			monitor.setMessage("Ontology is classified...");
//		}
//		
//		//we adopt a top-down approach, so we start from TOP = thing
//		IRI classIRI = OWLRDFVocabulary.OWL_THING.getIRI();
//		OWLClass clazz = factory.getOWLClass(classIRI);
//
//		//initialize the bit matrix
//		this.initializeBitMatrix(clazz, reasoner);
//		
//		
//		log.info("Adding Inferred axioms in the Dl Lite ontology...");
//		log.info("Adding sub classes, equivalent classes, and disjoint classes" +
//				 " axioms...");
//
//		Set<OWLClassExpression> ancestors_set = new HashSet<OWLClassExpression>();
//		Set<OWLClassExpression> negAncestors_set = new HashSet<OWLClassExpression>();
//		//add subclass, equivalent classes and disjoint classes axioms
//		this.addDlLiteHierarchy(clazz, ancestors_set,negAncestors_set,
//								dl_ont,  reasoner, manager, 
//								factory,ob, checkDLLite);
//		
//		for(ProgressMonitor monitor : progressMonitors)
//		{
//			monitor.setProgress(2);
//			monitor.setMessage("Class hierarchy is processed...");
//		}
//		
//		this.addSubClassAxiomsFromMatrix(dl_ont, manager, factory, ob, checkDLLite);
//		
//		if(fullChains){
//			log.info("Adding chain axioms...");
//			owlClassThing = factory.getOWLClass(ThingURI);
//			owlClassNothing = factory.getOWLClass(OWLRDFVocabulary.OWL_NOTHING.getIRI());//IRI.create(owl_ont.getOntologyID().getOntologyIRI() +"#Not_" + String.valueOf(owlClassThing)));
//			
//			int roleDepth = getRoleDepth(owl_ont);
//			checkChainInclusionsBF(complete_owl_ont, reasoner, factory, roleDepth);
//			
//			for(ProgressMonitor monitor : progressMonitors)
//			{
//				monitor.setProgress(3);
//			}
//			
//			concatenateChainInclusions(complete_owl_ont);
//			addChainAxioms(dl_ont,complete_owl_ont, manager, factory);
//			for(ProgressMonitor monitor : progressMonitors)
//			{
//				monitor.setProgress(4);
//			}
//		}
//		
//		log.info("Adding Inconsitent classes axioms...");
//		//add Inconsistent classes
//		addDlLiteInconsistentClassesAxioms(dl_ont, complete_owl_ont, reasoner, 
//										   manager, factory, ob, checkDLLite);
//		
//		log.info("Adding properties axioms...");
//		//add properties axioms
//		addDlLiteObjectProperties(dl_ont, complete_owl_ont, reasoner, 
//								  manager, factory, ob, checkDLLite);
//		addDlLiteDataProperties(dl_ont, complete_owl_ont, reasoner, 
//								manager, factory, ob, checkDLLite);
//		
//		
//		for(ProgressMonitor monitor : progressMonitors)
//		{
//			monitor.setFinished();
//		}
//		
//		//manager.saveOntology(complete_owl_ont, phys_uri_working_ont);
//		log.info("Logical Axioms " + dl_ont.getLogicalAxiomCount());
//		log.info("Axioms count " + dl_ont.getAxiomCount());
//		log.info("Classes count "+dl_ont.getClassesInSignature().size());
//		log.info("Object properties count "+dl_ont.getObjectPropertiesInSignature().size());
//
//		return dl_ont;
//		
//}
//	
//	
//
//	/***************************************************************************
//	 * The class BitMatrix2D will represent a two dimensions bit matrix, which
//	 * will be used to represent the class hierarchy of the Dl Lite ontology.
//	 * The idea is to have a representation which allows us to efficiently
//	 * minimize the subclasses axioms to be added in the Dl LIte ontology.
//	 ***************************************************************************/
//	public class BitMatrix2D implements Serializable {
//		private static final long 
//		serialVersionUID = 29187494607829404L;
//
//		//The fixed number of rows
//		private final int rows;
//		//The fixed number of columns
//		private final int columns;
//		//The actual data stored
//		private int[] data;
//		//This map will contain pairs <pos,clas>, where pos will be the position of the
//		//the class in the bit matrix which we will use to minimize the subclasses axioms.
//		private HashMap<OWLClassExpression,Integer> matrix_classes;
//		//The map inverse to matrix_classes, used to get the class in a given position efficiently.
//		private HashMap<Integer,OWLClassExpression> matrix_classes_inv;
//
//	 
//		/**
//		 * Class constructor. We must indicate the fixed (maximum) number of rows 
//		 * and columns that the matrix will contain.
//		 * @param rows the fixed number of rows that the matrix will contain
//		 * @param columns the fixed number of columns that the matrix will contain
//		 */
//		public BitMatrix2D(int rows, int columns) {
//			matrix_classes= new HashMap<OWLClassExpression,Integer>();
//			matrix_classes_inv = new HashMap<Integer,OWLClassExpression>();
//			this.rows = rows;
//			this.columns = columns;
//			/* x >> 5 = x / 32, only faster */
//			int lengh = Math.max(1,(rows * columns + 1)>> 5);
//			this.data = new int[lengh];
//			for (int i = 0; i < lengh; i++){
//				data[i]=0;
//			}
//
//		}
//		/**
//		 * Returns the fixed (maximum) number of rows in the matrix. 
//		 * This does not represent the acutal size of the matrix, but
//		 * the value set when it was created.
//		 * @return the value of rows private variable
//		 */
//		public int rows() {
//			return rows;
//		}
//		/**
//		 * Returns the fixed (maximum) number of columns in the matrix. 
//		 * This does not represent the acutal size of the matrix, but
//		 * the value set when it was created.
//		 * @return the value of rows private variable
//		 */
//		public int columns() {
//			return columns;
//		}
//	 
//		/**
//		 * Given the input indexes row and col, this method returns the 
//		 * value stored in the [row,col] position of the matrix.
//		 * @param row the index corresponding to the row
//		 * @param col the index corresponding to the column
//		 * @return the value of matrix[row,col]
//		 */
//		public boolean get(int row, int col) {
//			if (row < 0 || row >= rows)
//				throw 
//					new IndexOutOfBoundsException(
//					"Row index out of bounds:" + row);
//			else if (col < 0 || col >= columns)
//				throw 
//					new IndexOutOfBoundsException(
//					"Column index out of bounds:" + col);
//	 
//			int i = row * columns + col;
//			return ( (data[i >> 5] >> (i % 32)) & 1 ) != 0;
//		}
//		
//		/**
//		 * Given the input indexes row and col, and the boolean value v,
//		 * this method set the value in the [row,col] position of matrix to v
//		 * @param row the index corresponding to the row
//		 * @param col the index corresponding to the column
//		 * @param v the boolean value to store in in matrix[row,col]
//		 */
//		public void set(int row, int col, boolean v) {
//			if (row < 0 || row >= rows)
//				throw 
//					new IndexOutOfBoundsException(
//					"Row index out of bounds:" + row);
//			else if (col < 0 || col >= columns)
//				throw 
//					new IndexOutOfBoundsException(
//					"Column index out of bounds:" + col);
//	 
//			//given the matrix is represented as a vector, it will be
//			//store a secuence of columns, let's say, rows*columns columns
//			//then to access to the element corresponding to [row,col]
//			//we must do row * columns + col
//			//an example: supose we have 3 rows and 3 columns, this is 
//			//store in the matrix like: 1 2 3 4 5 6 7 8 9 (cells, where
//			//cells 1,2,3 correspond to row 0, 4,5,6 row1, and 7,8,9 row2)
//			//then if we want to access element [2,1] then it will be 7 
//			int i = row * columns + col;
//			//given that the array representing the matrix is array of ints
//			//and each int has 32 bits, idiv32 will contain the chunk of bits
//			//containing the cell to be modified
//			int idiv32 = i >> 5;
//			//modBit is a mask that will contain all 0 and only one 1 in the 
//			//possition corresponding to [row,col]
//			//the way to do this is shifting with 0's, for example
//			//1 << 3 = 100 
//			int modBit = 1 << (i % 32);
//			//data[idiv32] will be calculated as:
//			//if v is true, set it by calculating an "or" between what is already
//			//and the mask
//			//if ve is false, it makes an "and" between what is already and the 
//			//negation of the mask
//			data[idiv32] = v ? data[idiv32] |modBit :   
//							   data[idiv32] & ~modBit; 
//		}
//
//		/**
//		 * Returns the current matrix lenght. This value does not 
//		 * correspond to the actual matrix lenght, but to actual busy space of
//		 * the matrix
//		 * @return the size corresponding to the list of classes in the matrix.
//		 */
//		public int get_current_length(){
//			return matrix_classes.size();
//		}
//		
//		/**
//		 * Returns the index of the clazz class in the matrix 
//		 * @param clazz the input class
//		 * @return the index corresponding to the input class
//		 */
//		public int get_class_index(OWLClassExpression clazz){
//			return matrix_classes.get(clazz);
//		}
//
//		/**
//		 * Returns the class represented by the pos index in the matrix
//		 * @param pos the input index
//		 * @return the class corresponding to the input index
//		 */
//		public OWLClassExpression  get_class_desc(int pos){
//			return matrix_classes_inv.get(pos);
//		}
//
//		/**
//		 *Gets the indexes row and col corresponding to 
//		 *the input classes sub and sup respectively, and then
//		 *retrieves the boolean value corresponding to the [row,col]  
//		 *position in the matrix.
//		 * @param sub the input class to look in the rows of the matrix
//		 * @param sup the input class to look in the columns of the matrix
//		 * @return the value stored in matrix[row,col], where row is
//		 * the index of sub, and col is the index of col
//		 */
//		public boolean getD(OWLClassExpression sub, OWLClassExpression sup){
//			//get the index for subnull
//			int row = matrix_classes.get(sub);
//			//get the index for sup
//			int col = matrix_classes.get(sup);
//			return get(row,col);
//		}
//
//		/**
//		 *Gets the indexes row and col corresponding to 
//		 *the input classes sub and sup respectively, and then
//		 *sets the value of the [row,col] position in matrix to v.
//		 *When trying to get the indexes of the input classes, if any of
//		 *the classes doesn't have an index, the method adds it.
//		 * @param sub the input class to look in the rows of the matrix
//		 * @param sup the input class to look in the columns of the matrix
//		 * @param v the boolean value to set
//		 */
//		public void setD(OWLClassExpression sub, 
//						 OWLClassExpression sup,
//						 boolean v){
//			int row,col;
//			
//			//get the index for the row
//			if (matrix_classes.containsKey(sub)){
//				row = matrix_classes.get(sub);
//			}
//			else {//else create the index for the matrix
//				row = matrix_classes.size() ;
//				matrix_classes.put(sub, row);
//				matrix_classes_inv.put(row, sub);
//			}
//			
//			//get the index for the column
//			if (matrix_classes.containsKey(sup)){
//				col = matrix_classes.get(sup);
//			}
//			else {//else create the index for the matrix
//				col = matrix_classes.size() ;
//				matrix_classes.put(sup, col);
//				matrix_classes_inv.put(col, sup);
//			}
//			
//			//set the value
//			set(row, col, v);
//		}
//		
//	}
//
//}
//
//
