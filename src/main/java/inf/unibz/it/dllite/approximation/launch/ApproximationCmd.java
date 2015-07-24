package inf.unibz.it.dllite.approximation.launch;

import inf.unibz.it.dllite.aproximation.semantic.DLLiteApproximator;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

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
				OWLOntology owl_ont = manager.loadOntologyFromOntologyDocument(new File(args[0]));
		
	
				// uris for DL-Lite ontology
				IRI file_iri_dllite_ont = DLLiteApproximator.createIRIWithSuffix(file_iri_owl_ont, "approx");
				IRI iri_dllite_ont = DLLiteApproximator.createIRIWithSuffix(owl_ont.getOntologyID().getOntologyIRI(), "approx");
				
				
				// Approximate owl_ont
				DLLiteApproximator dlliteApprox = new DLLiteApproximator(manager);
	//			OWLOntology dl_ont = dlliteApprox.approximate(owl_ont, iri_dllite_ont);
				
				
				IRI iri_dllite_ont2 = DLLiteApproximator.createIRIWithSuffix(owl_ont.getOntologyID().getOntologyIRI(), "approx2");
				OWLOntology dl_ont2 = dlliteApprox.computeDLLiteRClosure(owl_ont, iri_dllite_ont2);
				
				// Save the approximated ontology
				manager.saveOntology(dl_ont2, new FileOutputStream(file_iri_dllite_ont.toString()));
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
					"Usage DLLiteApproximator " +
						"<URI of the OWL ontology> ");
		}
		//URI uri_owl_ont = URI.create("file:///home/alejandra/EclipseWorkspace/ProjectBolzano/inf.unibz.it.dllite.aproximation.semantic/resources/examples/ontologies/proofInc.owl");
	   	//URI uri_owl_ont = URI.create("file:///home/alejandra/EclipseWorkspace/ProjectBolzano/inf.unibz.it.dllite.aproximation.semantic/resources/examples/ontologies/pizzaWorking.owl");
	   	//URI uri_dl_ont = URI.create("file:///home/alejandra/EclipseWorkspace/ProjectBolzano/inf.unibz.it.dllite.aproximation.semantic/resources/examples/ontologies/proof-project-DLLIte.owl");
		//URI uri_working_ont = URI.create("file:///tmp/workingont.owl");

	}
}
