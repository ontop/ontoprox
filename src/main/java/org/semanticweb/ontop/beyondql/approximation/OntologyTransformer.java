package org.semanticweb.ontop.beyondql.approximation;


import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;


/**
 * An abstract ontology transformer class that provides 
 * a field ontologyManager and a constructor for its initialization.
 * 
 * It subclasses should implement the method transform that 
 * takes as input an ontology and an outputIRI, and returns a transformed
 * ontology whose IRI is the outputIRI.  
 * 
 */
public abstract class OntologyTransformer {

	
	protected final OWLOntologyManager ontologyManager;

	/**
	 * @param manager
	 * 
	 **************************************************************************/
	public OntologyTransformer(OWLOntologyManager manager) {
		ontologyManager = manager;
	}
	
	
	/**
	 * The main transformer method.
	 * 
	 * Transforms the given ontology with respect to its logic
	 * and returns a new ontology whose IRI is the outputIRI.
	 * 
	 * @param ontology
	 * @param outputIRI
	 * @return transformed ontology
	 * @throws OWLOntologyCreationException
	 */
	public abstract OWLOntology transform(OWLOntology ontology,
			IRI outputIRI) throws OWLOntologyCreationException;
	

}
