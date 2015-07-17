package inf.unibz.it.dllite.approximation.launch;

import inf.unibz.it.dllite.aproximation.semantic.DLLiteApproximator;

import java.io.File;
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
			URI file_uri_owl_ont = URI.create(args[0]);
			
			try {
				// Create our ontology manager in the usual way.	
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				// Load a copy of the ontology passed by parameters.  
				OWLOntology owl_ont = manager.loadOntologyFromOntologyDocument(new File(file_uri_owl_ont));
		
	
				// uris for DL-Lite ontology
				IRI file_uri_dllite_ont = DLLiteApproximator.createIRIWithSuffix(IRI.create(file_uri_owl_ont), "approx");
				IRI uri_dllite_ont = DLLiteApproximator.createIRIWithSuffix(owl_ont.getOntologyID().getOntologyIRI(), "approx");
				// We need this mapping so that manager can create the DL-Lite ontology
				manager.addIRIMapper(new SimpleIRIMapper(uri_dllite_ont, file_uri_dllite_ont));

				
				// Approximate owl_ont
				DLLiteApproximator dlliteApprox = new DLLiteApproximator();
				OWLOntology dl_ont = dlliteApprox.approximate(owl_ont, manager, uri_dllite_ont);
				
				
				// Save the approximated ontology
				manager.saveOntology(dl_ont);
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
