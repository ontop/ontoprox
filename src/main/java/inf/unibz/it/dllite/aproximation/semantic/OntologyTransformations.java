package inf.unibz.it.dllite.aproximation.semantic;

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

public abstract class OntologyTransformations {

	private static final String conceptSeparator = "_and_";
	protected final OWLOntologyManager ontologyManager;

	/**
	 * @param manager
	 * 
	 **************************************************************************/
	public OntologyTransformations(OWLOntologyManager manager) {
		ontologyManager = manager;
	}
	
	
	public abstract OWLOntology transform(OWLOntology ontology,
			IRI outputIRI) throws OWLOntologyCreationException;
	

	/**************************************************************************
	 * Adds a suffix to the original IRI
	 * 
	 * @param iri
	 *            the original IRI
	 * @param suffix
	 *            the suffix to add
	 *************************************************************************/
	static public IRI createIRIWithSuffix(IRI iri, String suffix) {
		String uriStr = iri.toString();
		if (uriStr.endsWith(".owl"))
			uriStr = uriStr.substring(0, uriStr.length() - ".owl".length())
					+ "_" + suffix + ".owl";
		else
			uriStr = uriStr + "_" + suffix + ".owl";
		IRI new_iri = IRI.create(uriStr);

		return new_iri;
	}

	
	protected static String extractConceptNames(OWLClassExpression clazz) {
		String conceptNames = "";
		if (clazz instanceof OWLClass) {
			conceptNames = extractPredicateName(clazz.asOWLClass());
		} else if (clazz instanceof OWLObjectIntersectionOf ){
			conceptNames = extractConceptNamesFromConjunction((OWLObjectIntersectionOf)clazz);
		}
		return conceptNames;
	}

	/**
	 * We assume that clazz is a conjunction of atomic concepts
	 * 
	 * @param clazz
	 * @return
	 */
	protected static String extractConceptNamesFromConjunction(
			OWLObjectIntersectionOf clazz) {
		
//        StringWriter writer = new StringWriter();
//        ManchesterOWLSyntaxObjectRenderer renderer = new ManchesterOWLSyntaxObjectRenderer(writer, new SimpleShortFormProvider());
//        clazz.accept(renderer);
//
//        String className = writer.toString();
//        String newName = className.replaceAll("\\(","").replaceAll("\\)","").replaceAll("\\s+", "_");
//
//        return newName;

		Set<OWLClassExpression> operands = ((OWLObjectIntersectionOf) clazz)
				.getOperands();

		// we sort the names of the operands so that the string 
		// is the same for equivalent concepts
		SortedSet<String> operandNames = new TreeSet<>();
		for (OWLClassExpression op : operands) {
			operandNames.add(extractPredicateName(op.asOWLClass()));
		}
		String conceptNames = Joiner.on(conceptSeparator).join(operandNames);
		
		return conceptNames;	
	}


	protected static String extractPredicateName(OWLClass clazz) {
		String name = clazz.getIRI().getFragment();
		if (name == null) {
			String iri = clazz.getIRI().toString();
			name = iri.substring(iri.indexOf('#')+1, iri.length());
		}
		return name;
	}

	protected static String extractPrefix(OWLClass clazz) {
		return clazz.getIRI().getNamespace();
	}

	protected static String extractPredicateName(OWLObjectPropertyExpression prop) {
		String predName = "";
		if(prop.isAnonymous()) {
			predName = prop.getNamedProperty().getIRI().getFragment();
		} else {
			predName = prop.asOWLObjectProperty().getIRI().getFragment();
		}
		return predName;
	}

	protected static String extractPrefix(OWLObjectPropertyExpression prop) {
		String prefix = "";
		if(prop.isAnonymous()) {
			prefix = prop.getNamedProperty().getIRI().getNamespace();
		} else {
			prefix = prop.asOWLObjectProperty().getIRI().getNamespace();
		}
		return prefix;
	}


}
