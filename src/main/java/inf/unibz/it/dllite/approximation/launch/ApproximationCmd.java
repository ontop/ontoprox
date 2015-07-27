package inf.unibz.it.dllite.approximation.launch;

import inf.unibz.it.dllite.aproximation.semantic.DLLiteApproximator;
import inf.unibz.it.dllite.aproximation.semantic.DLLiteRNormalizer;
import inf.unibz.it.dllite.aproximation.semantic.OntologyTransformations;
import inf.unibz.it.dllite.aproximation.semantic.Rewriter;

import java.io.File;
import java.io.FileOutputStream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class ApproximationCmd {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// args contains the input parameters, they should be
		// - The URI of the OWL ontology, the first parameter
		if (args.length == 1){ 
			IRI file_iri_owl_ont = IRI.create(args[0]);
			
			try {
				// Create our ontology manager in the usual way.	
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				// Load a copy of the ontology passed by parameters.  
				OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(args[0]));
		
	
				// uris for DL-Lite ontology
				IRI file_iri_dllite_ont2 = OntologyTransformations.createIRIWithSuffix(file_iri_owl_ont, "step2");
				IRI iri_dllite_ont2 = OntologyTransformations.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step2");
				IRI file_iri_dllite_ont4 = OntologyTransformations.createIRIWithSuffix(file_iri_owl_ont, "step4");
				IRI iri_dllite_ont4 = OntologyTransformations.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step4");
				
				
				// Approximate owl_ont
				Rewriter dlliterRewriter = new Rewriter(manager);
			//	OWLOntology ont2 = dlliterRewriter.computeDLLiteRClosure(ont, iri_dllite_ont2);
			
				DLLiteRNormalizer dlliterNormalizer = new DLLiteRNormalizer(manager);
				OWLOntology ont4 = dlliterNormalizer.normalizeQualifiedExistentialRestrictions(ont, iri_dllite_ont4);
				
				// Save the approximated ontology
			//	manager.saveOntology(ont2, new FileOutputStream(file_iri_dllite_ont2.toString()));
				manager.saveOntology(ont4, new FileOutputStream(file_iri_dllite_ont4.toString()));
			}
			catch (OWLOntologyCreationException e1) {
				System.out.println("Could not load the ontology: " +
									e1.getMessage());
			}
			catch (OWLOntologyChangeException e2){
				System.out.println("Could not apply changes to the ontology: " + 
									e2.getMessage());
			}
			catch (OWLOntologyStorageException e3){
				System.out.println("Could not save ontology: " +
									e3.getMessage());
			}
			catch (Exception e) {
			  e.printStackTrace();
			}
						

		}else{
			System.err.println("Invalid input parameters. \n" +
					"Usage ApproximationCmd " +
						"<URI of the OWL ontology> ");
		}

	}
}
