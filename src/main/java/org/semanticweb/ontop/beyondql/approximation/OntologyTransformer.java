package org.semanticweb.ontop.beyondql.approximation;

import java.io.StringWriter;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;

import com.google.common.base.Joiner;

public abstract class OntologyTransformer {

	
	protected final OWLOntologyManager ontologyManager;

	/**
	 * @param manager
	 * 
	 **************************************************************************/
	public OntologyTransformer(OWLOntologyManager manager) {
		ontologyManager = manager;
	}
	
	
	public abstract OWLOntology transform(OWLOntology ontology,
			IRI outputIRI) throws OWLOntologyCreationException;
	

}
