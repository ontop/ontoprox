package inf.unibz.it.dllite.aproximation.semantic;
import inf.unibz.it.dllite.aproximation.semantic.exception.FunctionalPropertyParticipatesInQualifiedExistentialException;
import inf.unibz.it.dllite.aproximation.semantic.exception.FunctionalPropertySpecializedException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;


/******************************************************************************
 * This class provides a way of checking if a given axiom is syntactically
 * valid in Dl Lite.
 * <p>
 * Notes: 
 * <ul>
 * <li> Based in the OWL 2 QL profile syntax plus functionality.
 *    DL-Lite_A is more expressive than OWL 2 QL, since it has 
 *    functionality, which OWL 2 QL does not have. 
 * <li>Every method assumes a valid OWL Ontology.
 * </ul>
 *  @author Alejandra Lorenzo
 *****************************************************************************/

public class DLLiteGrammarChecker {

	
/****************************   PRIVATE METHODS    ***************************/
	
	/**************************************************************************
	 * Returns true when the input Object Expression is valid in Dl Lite.
	 *<p> 
	 * ObjectPropertyExpression:= 
	 * 			ObjectProperty |  InverseOf (ObjectProperty), the same as OWL 2
	 *<p> 
	 * However, the OWL API method getInverse, applied to a 
	 * OWLObjectPropertyInverse returns an OWLObjectPropertyExpression object
	 * so, we added a restriction here to allow only an OWLObjectProperty 
	 * object for the ObjectPropertyInverse case
	 * @param obj the input Object Expression
	 * @return True when the the input OWLObjectPropertyExpression is valid 
	 * syntactically in Dl Lite and False in the opposite case.
	 * ***********************************************************************/
	private static boolean isDlLiteObjectPropertyExpression 
											 (OWLObjectPropertyExpression obj){
		boolean isDlLite = false;
		if (obj instanceof OWLObjectProperty || 
		   (obj instanceof OWLObjectInverseOf && 
		   ((OWLObjectInverseOf)obj).getInverse() instanceof 
		   													OWLObjectProperty))
		{ 
			isDlLite =true;
		}
		return isDlLite;
	}
	
	
	/************************************************************************** 
	 * This method accepts as input an OWLObjectPropertyDomainAxiom and returns  
	 * true if it is a valid OWLObjectPropertyDomainAxiom for Dl-Lite.
	 * <p>
	 * ObjectPropertyDomain:= 
	 * 		PropertyDomain (axiomAnnotations 
	 * 						ObjectPropertyExpression superClassExpression)
	 * 
	 * @param ax the input OWLObjectPropertyDomainAxiom 
	 * @return True when the the input OWLObjectPropertyDomainAxiom is valid 
	 * syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteObjectPropertyDomainAxiom 
											 (OWLObjectPropertyDomainAxiom ax){
		boolean isDlLite = false;
		OWLClassExpression dom = ((OWLObjectPropertyDomainAxiom)ax).getDomain();
		OWLObjectPropertyExpression prop = 
							  ((OWLObjectPropertyDomainAxiom)ax).getProperty();
		if(isDlLiteSuperClassExpression(dom) &&
		   isDlLiteObjectPropertyExpression(prop)){
			isDlLite = true;
		}
		return isDlLite;
	}
	
	
	/**************************************************************************
	 * This method accepts as input an OWLObjectPropertyRangeAxiom and returns  
	 * true if it is a valid OWLObjectPropertyRangeAxiom for Dl-Lite.
	 * <p>
	 * ObjectPropertyDomain:= 
	 * 			PropertyDomain (axiomAnnotations 
	 * 							ObjectPropertyExpression superClassExpression)
	 * 
	 * @param ax the input OWLObjectPropertyRangeAxiom 
	 * @return True when the the input OWLObjectPropertyRangeAxiom is valid 
	 * syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteObjectPropertyRangeAxiom 
											  (OWLObjectPropertyRangeAxiom ax){
		boolean isDlLite = false;
		OWLClassExpression dom = ((OWLObjectPropertyRangeAxiom)ax).getRange();
		OWLObjectPropertyExpression prop = 
							   ((OWLObjectPropertyRangeAxiom)ax).getProperty();
		if(isDlLiteSuperClassExpression(dom) &&
		   isDlLiteObjectPropertyExpression(prop)){
				isDlLite = true;
		}
		return isDlLite;
	}
	

	/**************************************************************************
	 * This method accepts as input an OWLSymmetricObjectPropertyAxiom and  
	 * returns true if it's a valid OWLSymmetricObjectPropertyAxiom for Dl-Lite
	 * <p>
	 * SymmetricObjectProperty := SymmetricProperty (axiomAnnotations 
	 * 												 ObjectPropertyExpression)
	 * @param ax the input OWLSymmetricObjectPropertyAxiom 
	 * @return True when the the input OWLSymmetricObjectPropertyAxiom is valid 
	 * syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteSymmetricObjectPropertyAxiom 
										  (OWLSymmetricObjectPropertyAxiom ax){
		boolean isDlLite = false;
		OWLObjectPropertyExpression prop = 
							((OWLSymmetricObjectPropertyAxiom)ax).getProperty();
		if( isDlLiteObjectPropertyExpression(prop)){
			isDlLite = true;
		}
		return isDlLite;
	}

	/**************************************************************************
	 * This method accepts a OWLEquivalentObjectPropertiesAxiom and returns 
	 * true if it is a valid OWLEquivalentObjectPropertiesAxiom in Dl-Lite
	 * <p>
	 * NOTE: this axiom has the same estructure in Dl-Lite and in OWL2, but we  
	 * implemented this method in order to check the objectPropertyExpression.
	 * <p>
	 * EquivalentObjectProperties := EquivalentProperties (axiomAnnotations 
	 * 												ObjectPropertyExpression 
	 * 												ObjectPropertyExpression 
	 * 												{ObjectPropertyExpression})
	 * @param ax the input OWLEquivalentObjectPropertiesAxiom
	 * @return True when the the input OWLEquivalentObjectPropertiesAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteEquivalentObjectPropertiesAxiom 
									   (OWLEquivalentObjectPropertiesAxiom ax){
		boolean isDlLite = true;
		Set<OWLObjectPropertyExpression> props = ax.getProperties();
		for (OWLObjectPropertyExpression prop:props){
			if (!isDlLiteObjectPropertyExpression(prop)){
				isDlLite = false;
				break;
			}
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts a OWLDisjointObjectPropertiesAxiom and returns true 
	 * if it is a valid OWLDisjointObjectPropertiesAxiom in Dl-Lite
	 * <p>
	 * DisjointObjectProperties := DisjointProperties (axiomAnnotations 
	 * 												ObjectPropertyExpression 
	 * 												ObjectPropertyExpression 
	 * 												{ObjectPropertyExpression})
	 * <p>
	 * NOTE: this axiom has the same estructure in Dl-Lite and in OWL2, but we
	 * 	implemented this method in order to check the objectPropertyExpression.
	 * @param ax the input OWLDisjointObjectPropertiesAxiom
	 * @return True when the the input OWLDisjointObjectPropertiesAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 **************************************************************************/
	private static boolean isDlLiteDisjointObjectPropertiesAxiom 
										(OWLDisjointObjectPropertiesAxiom ax){
		boolean isDlLite = true;
		Set<OWLObjectPropertyExpression> props = ax.getProperties();
		for (OWLObjectPropertyExpression prop:props){
			if (!isDlLiteObjectPropertyExpression(prop)){
				isDlLite = false;
				break;
			}
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts a OWLInverseObjectPropertiesAxiom and returns true 
	 * if it is a valid OWLInverseObjectPropertiesAxiom in Dl-Lite
	 * <p>
	 * NOTE: this axiom has the same estructure in Dl-Lite and in OWL2, but we  
	 * implemented this method in order to check the objectPropertyExpression.
	 * <p>
	 * InverseObjectProperties := InverseProperties (axiomAnnotations 
	 * 												 ObjectPropertyExpression 
	 * 												 ObjectPropertyExpression)
 	 * @param  ax the input OWLInverseObjectPropertiesAxiom
	 * @return True when the the input OWLInverseObjectPropertiesAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteInverseObjectPropertiesAxiom 
										  (OWLInverseObjectPropertiesAxiom ax){
		boolean isDlLite = false;
		OWLObjectPropertyExpression prop1 = ax.getFirstProperty();
		OWLObjectPropertyExpression prop2 = ax.getSecondProperty();
		if (isDlLiteObjectPropertyExpression(prop1) &&
			isDlLiteObjectPropertyExpression(prop2)	){
				isDlLite = true;
		}
		return isDlLite;
	}

	/**************************************************************************
	 * This method accepts an OWLDataSubPropertyAxiom and returns true if it is 
	 * a valid OWLDataSubPropertyAxiom in Dl-Lite
	 * <p>
	 * SubDataPropertyOf := SubPropertyOf ( axiomAnnotations 
	 * 							DataPropertyExpression DataPropertyExpression )
	 * @param ax the input OWLDataSubPropertyAxiom 
	 * @return True when the the input OWLDataSubPropertyAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteDataSubPropertyAxiom (OWLSubDataPropertyOfAxiom ax){
		boolean isDlLite = false;
		if ((ax.getSubProperty() instanceof OWLDataProperty) &&
			(ax.getSuperProperty() instanceof OWLDataProperty)){
			isDlLite = true;
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts an OWLEquivalentDataPropertiesAxiom and returns true 
	 *  if it is a valid OWLEquivalentDataPropertiesAxiom in Dl-Lite
	 * <p>
	 * EquivalentDataProperties := EquivalentProperties (axiomAnnotations 
	 * 													DataPropertyExpression 
	 * 													DataPropertyExpression 
	 * 												  {DataPropertyExpression})
	 * @param ax the input OWLEquivalentDataPropertiesAxiom 
	 * @return True when the the input OWLEquivalentDataPropertiesAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteEquivalentDataPropertiesAxiom 
										(OWLEquivalentDataPropertiesAxiom ax){
		boolean isDlLite = true;
		Set<OWLDataPropertyExpression> props = ax.getProperties();
		for (OWLDataPropertyExpression prop : props){
			if (!(prop instanceof OWLDataProperty)){
				isDlLite = false;
			}
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts an OWLDisjointDataPropertiesAxiom and returns true  
	 * if it is a valid OWLDisjointDataPropertiesAxiom in Dl-Lite
	 * <p>
	 * DisjointDataProperties := DisjointProperties (axiomAnnotations 
	 * 												 DataPropertyExpression 
	 * 												 DataPropertyExpression 
	 * 												 {DataPropertyExpression }) 
	 * @param ax the input OWLDisjointDataPropertiesAxiom 
	 * @return True when the the input OWLDisjointDataPropertiesAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteDisjointDataPropertiesAxiom 
										  (OWLDisjointDataPropertiesAxiom ax){
		boolean isDlLite = true;
		Set<OWLDataPropertyExpression> props = ax.getProperties();
		for (OWLDataPropertyExpression prop : props){
			if (!(prop instanceof OWLDataProperty)){
				isDlLite = false;
			}
		}
		return isDlLite;
	}

	
	/**************************************************************************
	 * This method accepts as input an OWLDataPropertyDomainAxiom and returns  
	 * true if it is a valid OWLDataPropertyDomainAxiom for Dl-Lite.
	 * <p>
	 * DataPropertyDomain := PropertyDomain (axiomAnnotations 
	 * 							   DataPropertyExpression superClassExpression)
	 * <p>
	 * DataPropertyExpression := DataProperty
	 * @param ax the input OWLDataPropertyDomainAxiom 
	 * @return True when the the input OWLDataPropertyDomainAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteDataPropertyDomainAxiom 
											   (OWLDataPropertyDomainAxiom ax){
		boolean isDlLite = false;
		OWLClassExpression dom = ((OWLDataPropertyDomainAxiom)ax).getDomain();
		OWLDataPropertyExpression prop = 
								((OWLDataPropertyDomainAxiom)ax).getProperty();
		if(isDlLiteSuperClassExpression(dom) &&
		   prop instanceof OWLDataProperty){
			isDlLite = true;
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts as input an OWLDataPropertyRangeAxiom and returns  
	 * true if it is a valid OWLDataPropertyRangeAxiom for Dl-Lite.
	 * <p>
	 * DataPropertyRange := PropertyRange (axiomAnnotations 
	 * 									   DataPropertyExpression DataRange)
	 * <p>
	 * DataPropertyExpression := DataProperty
	 * @param ax the input OWLDataPropertyRangeAxiom 
	 * @return True when the the input OWLDataPropertyRangeAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteDataPropertyRangeAxiom
												(OWLDataPropertyRangeAxiom ax){
		boolean isDlLite = false;
		OWLDataRange range = ((OWLDataPropertyRangeAxiom)ax).getRange();
		OWLDataPropertyExpression prop = 
								 ((OWLDataPropertyRangeAxiom)ax).getProperty();
		if(isDlLiteDataRange(range) &&
		   prop instanceof OWLDataProperty){
			isDlLite = true;
		}
		return isDlLite;
	}

	/**************************************************************************
	 * This method accepts as input an OWLClassAssertionAxiom and returns true  
	 * if it is a valid OWLClassAssertionAxiom for Dl-Lite.
	 * <p>
	 * ClassAssertion := ClassAssertion (axiomAnnotations Class Individual)
	 * @param ax the input OWLClassAssertionAxiom 
	 * @return True when the the input OWLClassAssertionAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteClassAssertionAxiom (OWLClassAssertionAxiom ax){
		boolean isDlLite = false;
		OWLClassExpression clazz = ((OWLClassAssertionAxiom)ax).getClassExpression();
		//OWLIndividual ind = ((OWLClassAssertionAxiom)ax).getIndividual(); 
		//same as OWL2
		if (clazz instanceof OWLClass ){
			isDlLite = true;
		}
		return isDlLite;
	}
	
	
	/**************************************************************************
	 * This method accepts an OWLObjectPropertyAssertionAxiom and returns true  
	 * if it is a valid OWLObjectPropertyAssertionAxiom in Dl-Lite
	 * <p>
	 * ObjectPropertyAssertion :=PropertyAssertion (axiomAnnotations 
	 * 				ObjectPropertyExpression sourceIndividual targetIndividual)
	 * @param ax the input OWLObjectPropertyAssertionAxiom 
	 * @return True when the the input OWLObjectPropertyAssertionAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteObjectPropertyAssertionAxiom 
										  (OWLObjectPropertyAssertionAxiom ax){
		boolean isDlLite =false; 
		//ax.getObject() and ax.getSubject() return OWLIndividual 
		//same as dl-lite, don't control.
		if (isDlLiteObjectPropertyExpression(ax.getProperty())){
			isDlLite = true;
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts a OWLDataPropertyAssertionAxiom and returns true if 
	 * it is a valid OWLDataPropertyAssertionAxiom for Dl-Lite
	 * <p>
	 * DataPropertyAssertion :=PropertyAssertion (axiomAnnotations 
	 * 					   DataPropertyExpression sourceIndividual targetValue)
	 * @param ax the input OWLDataPropertyAssertionAxiom 
	 * @return True when the the input OWLDataPropertyAssertionAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteDataPropertyAssertionAxiom 
											(OWLDataPropertyAssertionAxiom ax){
		boolean isDlLite =false; 
		//ax.getObject() and ax.getSubject() return OWLIndividual, 
		//same as dl-lite, don't control.
		if ((ax.getProperty() instanceof OWLDataProperty) &&
			(ax.getSubject() instanceof OWLIndividual) &&
			(ax.getObject() instanceof OWLLiteral)){
				isDlLite = true;
		}
		return isDlLite;
	}

	
	/**************************************************************************
	 * This method accepts an OWLObjectSomeValuesFrom and returns true if it  
	 * is a valid OWLObjectSomeValuesFrom for a Dl-Lite SubClassExpression 
	 * <p>
	 * subObjectSomeValuesFrom:= SomeValuesFrom 
	 * 									   (ObjectPropertyExpression owl:Thing)
	 * <p>
	 * NOTE: The method isOWLThing() does not determine if the description is 
	 * 		 equivalent to Thing, only if it is Thing.
	 * @param obj the input OWLObjectSomeValuesFrom 
	 * @return True when the the input OWLObjectSomeValuesFrom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteSubObjectSomeValuesFrom
												(OWLObjectSomeValuesFrom obj){
		boolean isDlLite = false;
		OWLClassExpression filler = ((OWLObjectSomeValuesFrom)obj).getFiller();
		OWLObjectPropertyExpression prop = 
								((OWLObjectSomeValuesFrom)obj).getProperty();
		if (filler.isOWLThing()
			&& isDlLiteObjectPropertyExpression (prop)){
			isDlLite = true;
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts an OWLObjectUnionOf object and returns true  
	 * if it is a valid DL-Lite OWLObjectUnionOf object.
	 * <p>
	 * subObjectUnionOf := UnionOf (subClassExpression 
	 * 							   subClassExpression {subClassExpression})
	 * @param obj the input OWLObjectUnionOf 
	 * @return True when the the input OWLObjectUnionOf is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteObjectUnionOf (OWLObjectUnionOf obj){
		boolean isDlLite = true;
		Set<OWLClassExpression> operands = 
								  ((OWLObjectUnionOf)obj).getOperands();
		for (OWLClassExpression op : operands){
			if (!isDlLiteSubClassExpression(op)){
				isDlLite=false;
				break;
			}
		}
		return isDlLite;
	}
	
	
	/**************************************************************************
	 * This method accepts an OWLObjectIntersectionOf object and returns true  
	 * if it is a valid OWLObjectIntersectionOf object.
	 * <p>
	 * superObjectIntersectionOf := IntersectionOf (superClassExpression 
	 * 							   superClassExpression {superClassExpression})
	 * @param obj the input OWLObjectIntersectionOf 
	 * @return True when the the input OWLObjectIntersectionOf is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteObjectIntersectionOf (OWLObjectIntersectionOf obj){
		boolean isDlLite = true;
		Set<OWLClassExpression> operands = 
								  ((OWLObjectIntersectionOf)obj).getOperands();
		for (OWLClassExpression op : operands){
			if (!isDlLiteSuperClassExpression(op)){
				isDlLite=false;
				break;
			}
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts an OWLObjectComplementOf and returns true if it is a 
	 * valid OWLObjectComplementOf for a SuperClass Expression in Dl-Lite
	 * <p>
	 * superObjectComplementOf:=Complement Of (subClassExpression)
	 * @param obj the input OWLObjectComplementOf 
	 * @return True when the the input OWLObjectComplementOf is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteSuperObjectComplementOf(OWLObjectComplementOf obj){
		boolean isDlLite = false;
		OWLClassExpression op = ((OWLObjectComplementOf)obj).getOperand();
		if (isDlLiteSubClassExpression(op)){
			isDlLite=true;
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts an OWLObjectSomeValuesFrom and returns true if it a 
	 * valid OWLObjectSomeValuesFrom for a SuperClass Expression in Dl-Lite
	 * <p>
	 * superObjectSomeValuesFrom:= SomeValuesFrom
	 * 										   (ObjectPropertyExpression Class) 
	 * @param obj the input OWLObjectSomeValuesFrom 
	 * @return True when the the input OWLObjectSomeValuesFrom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteSuperObjectSomeValuesFrom 
												(OWLObjectSomeValuesFrom obj){
		boolean isDlLite = false;
		OWLObjectPropertyExpression propExp = 
								 ((OWLObjectSomeValuesFrom)obj).getProperty();
		if ((((OWLObjectSomeValuesFrom)obj).getFiller() instanceof OWLClass) &&
								isDlLiteObjectPropertyExpression(propExp)){
			isDlLite = true;
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts an OWLDataSomeValuesFrom and returns true in case it  
	 * is a valid OWLDataSomeValuesFrom for a Super Class Expression in Dl-Lite
	 * <p>
	 * DataSomeValuesFrom:= SomeValuesFrom (DataPropertyExpression DataRange)
	 * @param obj the input OWLDataSomeValuesFrom 
	 * @return True when the the input OWLDataSomeValuesFrom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteDataSomeValuesFrom (OWLDataSomeValuesFrom obj){
		boolean isDlLite = false;
		OWLDataPropertyExpression prop = 
								((OWLDataSomeValuesFrom)obj).getProperty();
		OWLDataRange range = ((OWLDataSomeValuesFrom)obj).getFiller();
		if( prop instanceof OWLDataProperty &&
			isDlLiteDataRange(range)){
					isDlLite=true;
			}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts a OWLDataMinCardinality and returns true 
	 * if it is valid in Dl Lite.
	 * Now the only min cardinality restrictions accepted are those that have
	 * min cardinality of 1 and that are not qualified. A data restriction is 
	 * unqualified if it has a filler which is the top data type (rdfs:Literal).
	 * TODO But the qualification could be extended to any DataType.
	 * <p>
	 * This restriction is not valid in OWL QL, but it is valid in Dl Lite A. 
	 * @param obj the input OWLDataMinCardinality 
	 * @return True when the the input OWLDataMinCardinality is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 **************************************************************************/
	private static boolean isDataMinCardinalityRestriction 
										(OWLDataMinCardinality obj){
		boolean isDlLite = false;
		if ((obj.getCardinality() == 1)&&
			(!obj.isQualified())){
			isDlLite = true;
		}
		return isDlLite;
	}

/*********************   PUBLIC METHODS    ***********************************/

	/**************************************************************************
	 * This method accepts a OWLDataRange object, and return true if it is a 
	 * valid OWLDataRange for Dl-Lite
	 * <p>
	 * DataRange:= Datatype || DataIntersectionOf
	 * <p>
	 * DataIntersectionOf:= IntersectionOf (DataRange DataRange {DataRange})
	 * <p>
	 * TODO DataIntersectionOf I don't know the corresponding object in OWL Api
	 * @param obj the input OWLDataRange 
	 * @return True when the the input OWLDataRange is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteDataRange(OWLDataRange obj){
		boolean isDlLite = false;
		if (((OWLDataRange)obj).isDatatype()){
				isDlLite = true;
		} 
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method check, for an input OWLFunctionalObjectPropertyAxiom, if it 
	 * is valid in dl-lite.
	 * <p>
	 * In order to do this check, we must also check that the prop. expression  
	 * is not being used in the right side of any inclusion, and also that the 
	 * property is not included in a some restriction. 
	 * @param ax the input OWLFunctionalObjectPropertyAxiom 
	 * @param ont the input ontology, to check for conflicting axioms
	 * @return True when the the input OWLFunctionalObjectPropertyAxiom is  
	 * valid syntactically in the input Dl Lite ontology and False in the 
	 * opposite case.
	 * @throws FunctionalPropertySpecializedException, when the functionality
	 * conflicts with a SubObjectProperty axiom 
	 * @throws FunctionalPropertyParticipatesInQualifiedExistentialException,
	 * when the functional property participates in a qualified existential 
	 * restriction, and the functionality conflicts with this axiom.
	 **************************************************************************/
	public static boolean isDlLiteFunctionalObjectPropertyAxiom 
						(OWLFunctionalObjectPropertyAxiom ax, OWLOntology ont) 
	throws FunctionalPropertySpecializedException,
		   FunctionalPropertyParticipatesInQualifiedExistentialException
	{
		boolean isDlLite = false;
		//if the prop.expression is valid in dl-lite
		OWLObjectPropertyExpression prop = ax.getProperty();
		if (isDlLiteObjectPropertyExpression(prop)){
			//get the subproperty axioms where the functional property appears 
			//in the right side
			Set<OWLSubObjectPropertyOfAxiom> axsRHS = 
									ont.getObjectSubPropertyAxiomsForSuperProperty(prop);
			//get the equivalent properties axioms, as is propA equiv. propB, 
			//then propA is subproperty of propB and 
			//	   propB is subproperty of propA.
			Set<OWLEquivalentObjectPropertiesAxiom> axsEq = 
								ont.getEquivalentObjectPropertiesAxioms(prop);

			//Check that the property does not appear in a some restriction 
			//with a filler different than Top
			//Will extend the OWLOntologyWalkerVisitor because it provides a convenience method to
			//get the current axiom being visited as we go.
            // Create the walker.  Pass in the pizza ontology - we need to put it into a set
            // though, so we just create a singleton set in this case.
            OWLOntologyWalker walker = new OWLOntologyWalker(Collections.singleton(ont));
            RestrictionWalkerVisitor visitor = new RestrictionWalkerVisitor(walker);
            // Now ask the walker to walk over the ontology structure using our visitor instance.
            walker.walkStructure(visitor);
            //get the object properties involved in a some restriction
            Set<OWLObjectPropertyExpression> props = visitor.getO_props();
            //see if the input property is involved in a some restriction.
            
			//if any of these sets is not empty, or if the prop is in a some  
			//restriction description, the functional property axiom is not 
            //valid in dl-lite,
            if ((!axsRHS.isEmpty()) || (!axsEq.isEmpty())){
				isDlLite = false;
				throw new FunctionalPropertySpecializedException();
            }
            else if (props.contains(prop)){
            	isDlLite= false;
            	throw new FunctionalPropertyParticipatesInQualifiedExistentialException();
            }
            else if (axsRHS.isEmpty() && axsEq.isEmpty() && (!props.contains(prop))){
            	isDlLite = true;
            }
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method check, for an input OWLFunctionalDataPropertyAxiom, if it is
	 * valid in dl-lite.
	 * <p>
	 * In order to do this check, we must also check that the prop. expression  
	 * is not being used in the right side of any inclusion.
	 * @param ax the input OWLFunctionalDataPropertyAxiom 
	 * @param ont the input ontology to check for conflicting axioms
	 * @return True when the the input OWLFunctionalDataPropertyAxiom is  
	 * valid syntactically in the input Dl Lite ontology and False in the 
	 * opposite case.
	 *************************************************************************/
	public static boolean isDlLiteFunctionalDataPropertyAxiom 
						  (OWLFunctionalDataPropertyAxiom ax, OWLOntology ont){
		boolean isDlLite = false;
		//if the prop.expression is valid in dl-lite
		OWLDataPropertyExpression prop = ax.getProperty();
		if (prop instanceof OWLDataProperty){
			//get the subproperty axioms where the functional property appears 
			//in the right side
			Set<OWLSubDataPropertyOfAxiom> axsRHS = 
									ont.getDataSubPropertyAxiomsForSuperProperty(prop);
			
			//get the equivalent properties axioms, as is propA equiv. propB, 
			//then propA is subproperty of propB and 
			//	   propB is subproperty of propA.
			Set<OWLEquivalentDataPropertiesAxiom> axsEq = 
				   ont.getEquivalentDataPropertiesAxioms((OWLDataProperty)prop);
			//if any of these sets is not empty, the functional property axiom 
			//is not valid in dl-lite,
			if ((!axsRHS.isEmpty()) && 
				(!axsEq.isEmpty())) {
				isDlLite = true;
			}
		}
		return isDlLite;
	}

		
	/**************************************************************************
	 * This method accepts as input an OWLClassExpression object and returns true  
	 * in case it is a valid sub class expression in DL-Lite
	 * <p>
	 * subClassExpression:=Class | subObjectSomeValuesFrom | DataSomeValuesFrom
	 * | dataMinCardinalityRestriction
	 * <p>
	 * NOTE: dataMinCardinalityRestriction is not present in OWL QL
 	 * @param subClass the input OWLClassExpression 
	 * @return True when the the input OWLClassExpression is valid   
	 * a valid SubClassExpression in Dl Lite and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteSubClassExpression(OWLClassExpression subClass){
		boolean isDlLite = false;
		//Control over the NNF form
		OWLClassExpression subClassNNF = subClass.getNNF();
		//Class
		if (subClassNNF instanceof OWLClass){
			isDlLite = true;
		}
		//subObjectUnionOf
		else if ((subClassNNF instanceof OWLObjectUnionOf) &&
				isDlLiteObjectUnionOf
				  					((OWLObjectUnionOf)subClassNNF)){
			isDlLite = true;
		}
		//subObjectSomeValuesFrom
		else if ((subClassNNF instanceof OWLObjectSomeValuesFrom) &&
				  isDlLiteSubObjectSomeValuesFrom
				  					((OWLObjectSomeValuesFrom)subClassNNF)){
			isDlLite = true;
		}
		//DataSomeValuesFrom
		else if ((subClassNNF instanceof OWLDataSomeValuesFrom) &&
				isDlLiteDataSomeValuesFrom ((OWLDataSomeValuesFrom)subClassNNF)){
			isDlLite=true;
		}
		//dataMinCardinalityRestriction
		else if((subClassNNF instanceof OWLDataMinCardinality) &&
				isDataMinCardinalityRestriction 
				((OWLDataMinCardinality)subClassNNF)){
			isDlLite = true;
		}
		return isDlLite;
	}
	

	/**************************************************************************
	 * This method accepts as input an OWLClassExpression object  and returns true  
	 * in case it is a valid super class expression in DL-Lite
	 * <p>
	 * superClassExpression:= Class | superObjectIntersectionOf | 
	 * superObjectComplementOf | superObjectSomeValuesFrom | DataSomeValuesFrom
	 * | dataMinCardinalityRestriction
	 * <p>
	 * NOTE: dataMinCardinalityRestriction is not present in OWL QL
	 * @param superClass the input OWLClassExpression 
	 * @return True when the the input OWLClassExpression is a valid
	 * SuperClassExpression in Dl Lite and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteSuperClassExpression(OWLClassExpression superClass){
		//
		boolean isDlLite = false;
		OWLClassExpression superClassNNF = superClass.getNNF();
		//Class
		if (superClassNNF instanceof OWLClass){
			isDlLite = true;
		}
		//superObjectIntersectionOf  
		else if(superClassNNF instanceof OWLObjectIntersectionOf &&
				isDlLiteObjectIntersectionOf
									((OWLObjectIntersectionOf)superClassNNF)){
			isDlLite = true;
		}
		//superObjectComplementOf
		else if (superClassNNF instanceof OWLObjectComplementOf &&
				isDlLiteSuperObjectComplementOf 
									   ((OWLObjectComplementOf)superClassNNF)){
			isDlLite = true;
		}
		//superObjectSomeValuesFrom
		else if((superClassNNF instanceof OWLObjectSomeValuesFrom) &&
				isDlLiteSuperObjectSomeValuesFrom 
									((OWLObjectSomeValuesFrom)superClassNNF)){
			isDlLite = true;
		}
		//DataSomeValuesFrom
		else if ((superClassNNF instanceof OWLDataSomeValuesFrom) &&
				isDlLiteDataSomeValuesFrom 
				  					  ((OWLDataSomeValuesFrom)superClassNNF)){
			isDlLite = true;
		}
		//dataMinCardinalityRestriction
		else if((superClassNNF instanceof OWLDataMinCardinality)&&
				isDataMinCardinalityRestriction 
							((OWLDataMinCardinality)superClassNNF)){
			isDlLite = true;
		}
		
		return isDlLite;
	}
	

	/**************************************************************************
	 * This method accepts as input an OWLClassExpression object  and returns the  
	 * set of outer most classes that are not a valid DL-Lite super classes
	 * @param subClass the input OWLClassExpression 
	 * @return empty set when the the input OWLClassExpression is a valid
	 * super class in Dl Lite and set of classes in the opposite case.
	 *************************************************************************/	
	public static Set<OWLClassExpression> getNotDlLiteSubClassExpression(OWLClassExpression subClass)
	{
		OWLClassExpression subClassNNF = subClass.getNNF();
		Set<OWLClassExpression> notDlLiteSubClasses = new HashSet<OWLClassExpression>();

		if (subClassNNF instanceof OWLObjectUnionOf) {
			Set<OWLClassExpression> operands = ((OWLObjectUnionOf)subClassNNF).getOperands();
			// for each class participating in union we recursively calculate
			// classes needed to be named
			for (OWLClassExpression op : operands){
				notDlLiteSubClasses.addAll(getNotDlLiteSubClassExpression(op));
			}
		}
		else if (subClassNNF instanceof OWLObjectAllValuesFrom) {
			// TODO do we really need it for sub classes?
			notDlLiteSubClasses.add(subClassNNF);
//			OWLClassExpression filler = ((OWLObjectAllValuesFrom)subClassNNF).getFiller();
//			if(!isDlLiteSubClassExpression(filler)){
//				notDlLiteSubClasses.addAll(getNotDlLiteSubClassExpression(filler));
//			}
		}
		else if (!isDlLiteSubClassExpression(subClassNNF))
		{
			// anything else is that is not a DL Lite sub class is a complex class 
			// => we give a name to it
			notDlLiteSubClasses.add(subClassNNF);
		}
		return notDlLiteSubClasses;
	}
	
	/**************************************************************************
	 * This method accepts as input an OWLClassExpression object  and returns the  
	 * set of outer most classes that are not a valid DL-Lite super classes
	 * @param superClass the input OWLClassExpression 
	 * @return empty set when the the input OWLClassExpression is a valid
	 * super class in Dl Lite and set of classes in the opposite case.
	 *************************************************************************/	
	public static Set<OWLClassExpression> getNotDlLiteSuperClassExpression(OWLClassExpression superClass)
	{
		OWLClassExpression superClassNNF = superClass.getNNF();
		Set<OWLClassExpression> notDlLiteSuperClasses = new HashSet<OWLClassExpression>();

		if(superClassNNF instanceof OWLClass) {
		}
		else if(superClassNNF instanceof OWLObjectSomeValuesFrom) {
			OWLClassExpression filler = ((OWLObjectSomeValuesFrom)superClassNNF).getFiller();
			// if filler is some restriction or a named class we continue recursively
			if (filler instanceof OWLObjectSomeValuesFrom || filler instanceof OWLClass) {
				notDlLiteSuperClasses = getNotDlLiteSuperClassExpression(((OWLObjectSomeValuesFrom)superClassNNF).getFiller());
			}
			// if filler is anything else then we give a name to it
			else {
				notDlLiteSuperClasses.add(filler);
			}
		}
		else if (superClassNNF instanceof OWLObjectIntersectionOf) {
			Set<OWLClassExpression> operands = ((OWLObjectIntersectionOf)superClassNNF).getOperands();
			// for each class participating in intersection we recursively calculate
			// classes needed to be named
			for (OWLClassExpression op : operands){
				notDlLiteSuperClasses.addAll(getNotDlLiteSuperClassExpression(op));
			}
		}
		else if (superClassNNF instanceof OWLObjectComplementOf) {
			// if the negated class is a complex class, then we give a name to it
			if(!isDlLiteSuperObjectComplementOf((OWLObjectComplementOf)superClassNNF))
				notDlLiteSuperClasses.add(superClassNNF);
		}
		else if (superClassNNF instanceof OWLObjectAllValuesFrom) {
			// we give a name to the for all restriction 
			// and recursively to its filler, because of the example:
			// A \ISA ER
			// A \ISA AR.(B \OR C)
			// B \ISA ER1.D
			// C \ISA ER1.D
			notDlLiteSuperClasses.add(superClassNNF);
			OWLClassExpression filler = ((OWLObjectAllValuesFrom)superClassNNF).getFiller();
			if(!isDlLiteSuperClassExpression(filler)) {
				notDlLiteSuperClasses.addAll(getNotDlLiteSuperClassExpression(filler));
			}
		}
		else {
			// anything else is a complex class => we give a name to it
			notDlLiteSuperClasses.add(superClassNNF);
		}
		return notDlLiteSuperClasses;
	}
	
	
	/**************************************************************************
	 * This method accepts as input an OWLSubClassAxiom an returns true if it 
	 * is a valid OWLSubClassAxiom in Dl-Lite
	 * <p>
	 * SubclassOf:= (axiomAnnotations subClassExpression superClassExpression)
	 * @param ax the input OWLSubClassAxiom 
	 * @return True when the the input OWLSubClassAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteSubClassAxiom (OWLSubClassOfAxiom ax){
		boolean isDlLite = false;
		OWLClassExpression subClass =((OWLSubClassOfAxiom)ax).getSubClass().getNNF();
		OWLClassExpression superClass = 
							   ((OWLSubClassOfAxiom)ax).getSuperClass().getNNF();
		if (isDlLiteSubClassExpression(subClass) && 
			isDlLiteSuperClassExpression(superClass)){
			isDlLite = true;	
		}
		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts a OWLEquivalentClassesAxiom and returns true if it  
	 * is a valid OWLEquivalentClassesAxiom in Dl-Lite
	 * <p>
	 * EquivalentClasses := EquivalentClasses (axiomAnnotations 
	 * 				subClassExpression subClassExpression {subClassExpression})
	 * @param ax the input OWLEquivalentClassesAxiom  
	 * @return True when the the input OWLEquivalentClassesAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteEquivalentClassAxiom(OWLEquivalentClassesAxiom ax){
		boolean isDlLite = true;
		Set<OWLClassExpression> descriptions = 
							((OWLEquivalentClassesAxiom) ax).getClassExpressions();
		for (OWLClassExpression desc: descriptions){
				if (!isDlLiteSubClassExpression(desc.getNNF())){
					isDlLite = false;
					break;
				} 
			}//for
		return isDlLite;
	}

	/**************************************************************************
	 * This method accepts a OWLDisjointClassesAxiom and returns true if it is 
	 * a valid OWLDisjointClassesAxiom for Dl-Lite
	 * <p>
	 * DisjointClasses := DisjointClasses (axiomAnnotations 
	 * 				subClassExpression subClassExpression {subClassExpression})
	 * @param ax the input OWLDisjointClassesAxiom 
	 * @return True when the the input OWLDisjointClassesAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	private static boolean isDlLiteDisjointClassesAxiom (OWLDisjointClassesAxiom ax){
		boolean isDlLite = true;
		Set<OWLClassExpression> descriptions = 
							((OWLDisjointClassesAxiom) ax).getClassExpressions();
		for (OWLClassExpression desc: descriptions){
			if (!isDlLiteSubClassExpression(desc.getNNF())){
				isDlLite = false;
				break;
			} 
		}//for
		return isDlLite;
	}
	
	
	/**************************************************************************
	 * This method accepts as input an OWLClassAxiom and returns true in case 
	 * it is a OWLClassAxiom valid for DL Lite
	 * <p>
	 * ClassAxiom: SubClassOf|EquivalentClasses | DisjointClasses
	 * @param ax the input OWLClassAxiom 
	 * @return True when the the input OWLClassAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteClassAxiom (OWLClassAxiom ax){
		boolean isDlLite = false;
		//sub class axiom
		if ((ax instanceof OWLSubClassOfAxiom) &&
			isDlLiteSubClassAxiom ((OWLSubClassOfAxiom)ax)){
			isDlLite = true;
		}
		//equivalent classes axiom
		else if ((ax instanceof OWLEquivalentClassesAxiom)&&
			isDlLiteEquivalentClassAxiom ((OWLEquivalentClassesAxiom)ax)){
					isDlLite = true;
		}
		//disjoint classes axiom
		else if ((ax instanceof OWLDisjointClassesAxiom) &&
			isDlLiteDisjointClassesAxiom ((OWLDisjointClassesAxiom)ax) ){
					isDlLite = true;
		}
		
		//DisjointUnion not valid in DLLite, so not covered here, will be 
		//assigned false
		return isDlLite;
	} 
	
	
	/**************************************************************************
	 * This method accepts as input an OWLObjectSubPropertyAxiom and returns  
	 * true in case it is a OWLObjectSubPropertyAxiom valid in DL Lite
	 * <p>
	 * subObjectPropertyOf:= SubPropertyOf (axiomAnnotations 
	 * 						ObjectPropertyExpression ObjectPropertyExpression)
	 * <p>
	 * NOTE: In OWL2 grammar, a SubObjectPropertyAxiom accepts as subproperty  
	 * expression, a propertyChain, but in OWL Api, this is implemented as a 
	 * different SubObjectPropertyAxiom: OWLObjectPropertyChainSubPropertyAxiom, 
	 * so here we don't treat this axiom and so it won't recognize it.
	 * @param ax the input OWLObjectSubPropertyAxiom 
	 * @return True when the the input OWLObjectSubPropertyAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteSubObjectPropertyAxiom(OWLSubObjectPropertyOfAxiom ax)
	{
		boolean isDlLite = false;
		OWLObjectPropertyExpression subProp = 
							((OWLSubObjectPropertyOfAxiom)ax).getSubProperty();
		OWLObjectPropertyExpression superProp = 
							((OWLSubObjectPropertyOfAxiom)ax).getSuperProperty();
		if ((isDlLiteObjectPropertyExpression (subProp))&&
			(isDlLiteObjectPropertyExpression (superProp))){
			isDlLite = true;
		}
		return isDlLite;
	}


	/**************************************************************************
	 * This method accepts as input an OWLObjectPropertyAxiom and returns true  
	 * in case it is an Object Property Axiom valid in DL Lite
	 * <p>
	 * ObjectPropertyAxiom:= SubObjectPropertyOf | EquivalentObjectProperties | 
	 * 						 DisjointObjectProperties |InverseObjectProperties 
	 * 						 | ObjectPropertyDomain | ObjectPropertyRange | 
	 * 						 SymmetricObjectProperty | 
	 * 						 Functional property (DL-Lite, but not OWL-QL)
	 * @param ax the input OWLObjectPropertyAxiom 
	 * @return True when the the input OWLObjectPropertyAxiom is  
	 * valid syntactically in Dl Lite, for the input Dl Lite ontology, 
	 * and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteObjectPropertyAxiom (OWLObjectPropertyAxiom ax){
		boolean isDlLite = false;
		//SubObjectPropertyOf 
		if ((ax instanceof OWLSubObjectPropertyOfAxiom) && 
			isDlLiteSubObjectPropertyAxiom((OWLSubObjectPropertyOfAxiom)ax)){
			isDlLite = true;
		}
		//EquivalentObjectProperties
		else if ((ax instanceof OWLEquivalentObjectPropertiesAxiom)	&& 
				 isDlLiteEquivalentObjectPropertiesAxiom 
				 				((OWLEquivalentObjectPropertiesAxiom)ax)) {
			isDlLite = true;
		}
		//DisjointObjectProperties
		else if ((ax instanceof OWLDisjointObjectPropertiesAxiom) && 
				 isDlLiteDisjointObjectPropertiesAxiom
				 					((OWLDisjointObjectPropertiesAxiom)ax)) {
			isDlLite = true;
		}
		//InverseObjectProperties
		else if ((ax instanceof OWLInverseObjectPropertiesAxiom) && 
				isDlLiteInverseObjectPropertiesAxiom
										((OWLInverseObjectPropertiesAxiom)ax)){
			isDlLite = true;
		}
		//ObjectPropertyDomain
		else if ((ax instanceof OWLObjectPropertyDomainAxiom)&& 
				isDlLiteObjectPropertyDomainAxiom 
										((OWLObjectPropertyDomainAxiom)ax)) {
			isDlLite = true;
		}
		//ObjectPropertyRange
		else if ((ax instanceof OWLObjectPropertyRangeAxiom) && 
				isDlLiteObjectPropertyRangeAxiom
											((OWLObjectPropertyRangeAxiom)ax)){
			isDlLite = true;
		}
		//SymmetricObjectProperty
		else if ((ax instanceof OWLSymmetricObjectPropertyAxiom)&& 
				isDlLiteSymmetricObjectPropertyAxiom
										((OWLSymmetricObjectPropertyAxiom)ax)){
			isDlLite = true;
		}
		//Functional property axiom
		//not in DL-LiteR so we don't care
//		else if (ax instanceof OWLFunctionalObjectPropertyAxiom) {
//			try {
//				if(isDlLiteFunctionalObjectPropertyAxiom
//						  ((OWLFunctionalObjectPropertyAxiom)ax, ont)){
//					isDlLite = true;
//				}
//			} catch (FunctionalPropertySpecializedException e) {
//				// TODO Auto-generated catch block
//				//e.printStackTrace();
//				//don't do anything, just to deal with the exception
//			} catch (FunctionalPropertyParticipatesInQualifiedExistentialException e2) {
//				// TODO Auto-generated catch block
//				//e2.printStackTrace();
//				//don't do anything, just to deal with the exception
//			} 
//		}
				

		return isDlLite;
	}
	
	/**************************************************************************
	 * This method accepts as input an OWLDataPropertyAxiom and returns true in 
	 *  case it is a valid OWLDataPropertyAxiom in DL Lite
	 *  <p>
	 * DataPropertyAxiom := SubDataPropertyOf | EquivalentDataProperties | 
	    				    DisjointDataProperties | DataPropertyDomain | 
	    				    DataPropertyRange | 
	    				    Functional property (DL-Lite, but not OWL-QL)
	 * @param ax the input OWLDataPropertyAxiom 
	 * @return True when the the input OWLDataPropertyAxiom is  
	 * valid syntactically in Dl Lite for the input Dl Lite ontology, 
	 * and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteDataPropertyAxiom (OWLDataPropertyAxiom ax){
		boolean isDlLite = false;
		//SubDataPropertyOf 
		if ((ax instanceof OWLSubDataPropertyOfAxiom) && 
			isDlLiteDataSubPropertyAxiom((OWLSubDataPropertyOfAxiom)ax)){
			isDlLite =true;
		}
		//EquivalentDataProperties
		else if((ax instanceof OWLEquivalentDataPropertiesAxiom) && 
			isDlLiteEquivalentDataPropertiesAxiom
									((OWLEquivalentDataPropertiesAxiom)ax)){
			isDlLite = true;
		}
		//DisjointDataProperties 
		else if ((ax instanceof OWLDisjointDataPropertiesAxiom)&& 
			isDlLiteDisjointDataPropertiesAxiom
										((OWLDisjointDataPropertiesAxiom)ax)){
			isDlLite = true;
		}
		//DataPropertyDomain
		else if ((ax instanceof OWLDataPropertyDomainAxiom) &&
			isDlLiteDataPropertyDomainAxiom
											((OWLDataPropertyDomainAxiom)ax)){
				isDlLite=true;
		}
		//DataPropertyRange 
		else if ((ax instanceof OWLDataPropertyRangeAxiom)&&
			isDlLiteDataPropertyRangeAxiom
											((OWLDataPropertyRangeAxiom)ax)){
				isDlLite = true;
		}
		//Functional property axiom
		// not DL-LiteR axiom, so we don't care
//		else if ((ax instanceof OWLFunctionalDataPropertyAxiom) &&
//				isDlLiteFunctionalDataPropertyAxiom
//									((OWLFunctionalDataPropertyAxiom)ax, ont)){
//			isDlLite = true;
//		}
		return isDlLite;
	} 


	/**************************************************************************
	 * This method accepts as input an OWLIndividualAxiom and returns true in 
	 * case it is an valid OWLIndividualAxiom in DL Lite
	 * <p>
	 * Assertion/IndividualAxiom := DifferentIndividuals | ClassAssertion | 
	 * 						ObjectPropertyAssertion | DataPropertyAssertion
	 * @param ax the input OWLIndividualAxiom 
	 * @return True when the the input OWLIndividualAxiom is  
	 * valid syntactically in Dl Lite and False in the opposite case.
	 *************************************************************************/
	public static boolean isDlLiteIndividualAxiom (OWLIndividualAxiom ax){
		boolean isDlLite = false;
		//DifferentIndividuals := DifferentIndividuals (axiomAnnotations 
		//									Individual Individual {Individual })
		//same as OWL2
		if (ax instanceof OWLDifferentIndividualsAxiom){
			isDlLite = true;
		}
		//ClassAssertion := ClassAssertion (axiomAnnotations Class Individual)
		else if ((ax instanceof OWLClassAssertionAxiom)&& 
			isDlLiteClassAssertionAxiom((OWLClassAssertionAxiom)ax)){
				isDlLite =true;
		}
		//ObjectPropertyAssertion
		else if ((ax instanceof OWLObjectPropertyAssertionAxiom) &&
			isDlLiteObjectPropertyAssertionAxiom 
										((OWLObjectPropertyAssertionAxiom)ax)){
			isDlLite = true;
		}
		//DataPropertyAssertion 
		//same as OWL2
		else if((ax instanceof OWLDataPropertyAssertionAxiom)&&
			isDlLiteDataPropertyAssertionAxiom 
										  ((OWLDataPropertyAssertionAxiom)ax)){
			isDlLite =true;
		}
		return isDlLite;
	} 

	/*************************************************************************
	 * this method accepts as input an OWLAxiom and returns true in case it is
	 *  an axiom valid for DL Lite.
	 *  <p>
	 * dlLiteAxiom = Declaration | ClassAxiom | ObjectPropertyAxiom | 
	 * 				 DataPropertyAxiom|Assertion |	AnnotatedAxiom
	 * <p>
	 * NOTE: we added the ontology, for the case of functional property axiom
	 * @param ax the input axiom
	 * @return True when the the input OWLAxiom is valid syntactically 
	 * in Dl Lite for the input ontology, and False in the opposite case.
	 *************************************************************************/
	//TODO grammar: see if axiomAnnotions is the same as in OWL 2, so we don't
	// need to control it here.
	public static boolean isdlLiteAxiom(OWLAxiom ax){
		boolean isDlLite = false;
		//Declaration. TODO see if it is ok, see if there are more cases to consider 
		if (ax instanceof OWLDeclarationAxiom){
			isDlLite = true;
		} 
		//Logical axioms:  
		//		ClassAxiom|ObjectPropertyAxiom|DataPropertyAxiom|Assertion
		else if (ax instanceof OWLLogicalAxiom){
				//ClassAxiom
				if ((ax instanceof OWLClassAxiom) && 
					 isDlLiteClassAxiom((OWLClassAxiom)ax)){
					isDlLite = true;
				}
				//ObjectPropertyAxiom
				else if ((ax instanceof OWLObjectPropertyAxiom) && 
						isDlLiteObjectPropertyAxiom((OWLObjectPropertyAxiom)ax)){
					isDlLite = true;
				}
				//DataPropertyAxiom
				else if ((ax instanceof OWLDataPropertyAxiom) && 
						isDlLiteDataPropertyAxiom ((OWLDataPropertyAxiom)ax)){
					isDlLite = true;
				}
				//Assertion
				else if ((ax instanceof OWLIndividualAxiom) && 
						isDlLiteIndividualAxiom ((OWLIndividualAxiom)ax)){
					isDlLite = true;
				}
		} 
		//annotation axiom
		if (ax instanceof OWLAnnotationAxiom){
			//TODO seems to be the same as in OWL 2, so, if it is an annotion 
			//axiom, it is DLlite axiom
			isDlLite = true;
		}
		return isDlLite;
	}//dlLiteAxiom
	
/******************************************************************************
 * This class "walks" the input ontology searching for descriptions of the form 
 *  some values from (existential restriction), and returns a set containg all
 *  the properties involved in those descriptions. 
 *  Will be used to check if a given property functionality is valid in Dl-Lite
 * @author Alejandra Lorenzo
 *****************************************************************************/
	private static  class RestrictionWalkerVisitor extends OWLOntologyWalkerVisitor<Object> {
		private Set<OWLObjectPropertyExpression> o_props;	
		public Set<OWLObjectPropertyExpression> getO_props() {
			return o_props;
		}
		public Set<OWLDataPropertyExpression> getD_props() {
			return d_props;
		}
		private Set<OWLDataPropertyExpression> d_props;
		public RestrictionWalkerVisitor (OWLOntologyWalker walker){
			 	super(walker);
				o_props = new HashSet<OWLObjectPropertyExpression>();
				d_props = new HashSet<OWLDataPropertyExpression>();
		}
		public Object visit(OWLObjectSomeValuesFrom desc) {
	        if (!desc.getFiller().isOWLThing()){
	        	o_props.add(desc.getProperty());	
	        }
			// We don't need to return anything here.
	        return null;
	    }
     };
	
}//class
