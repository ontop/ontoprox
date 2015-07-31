package org.semanticweb.ontop.beyondql.cli;

import java.io.File;
import java.io.FileOutputStream;

import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.ontop.beyondql.approximation.ConjunctionNormalizer;
import org.semanticweb.ontop.beyondql.approximation.DLLiteRClosureBuilder;
import org.semanticweb.ontop.beyondql.approximation.EmptyConjunctionRemover;
import org.semanticweb.ontop.beyondql.approximation.IRIUtils;
import org.semanticweb.ontop.beyondql.approximation.OntologyTransformer;
import org.semanticweb.ontop.beyondql.approximation.QualifiedExistentialNormalizer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChangeException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import com.google.common.collect.ImmutableSet;

import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

public class OntopOntologyApproximationCmd {

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
		
	
				// iris for the intermediate ontologies
				IRI iri_dllite_ont1_opt = IRIUtils.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step1_opt");
				IRI iri_dllite_ont2 = IRIUtils.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step2");
				IRI iri_dllite_ont3 = IRIUtils.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step3");
				IRI iri_dllite_ont4 = IRIUtils.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step4");
				IRI file_iri_dllite_ont1 = IRIUtils.createIRIWithSuffix(file_iri_owl_ont, "step1");
				IRI file_iri_dllite_ont1_opt = IRIUtils.createIRIWithSuffix(file_iri_owl_ont, "step1_opt");
				IRI file_iri_dllite_ont2 = IRIUtils.createIRIWithSuffix(file_iri_owl_ont, "step2");
				IRI file_iri_dllite_ont3 = IRIUtils.createIRIWithSuffix(file_iri_owl_ont, "step3");
				IRI file_iri_dllite_ont4 = IRIUtils.createIRIWithSuffix(file_iri_owl_ont, "step4");

				
				// step 1
		        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();
		        /** feed the ontology to Clipper reasoner */
		        qaHornSHIQ.setOntologies(ImmutableSet.of(ont));
		        /** rewrite the ontology to a datalog program represented in Clipper Native API */
		        qaHornSHIQ.rewriteOntology();

		        OWLOntology ont1 = qaHornSHIQ.exportNormalizedAxiomsAndSaturatedEnforceRelations(ont.getOntologyID().getOntologyIRI().toString() + "_step1");
				manager.saveOntology(ont1, new RDFXMLOntologyFormat(), new FileOutputStream(file_iri_dllite_ont1.toString()));


				// intermediate optimization step
				EmptyConjunctionRemover emptyRemover = new EmptyConjunctionRemover(manager);
				OWLOntology ont1opt = emptyRemover.transform(ont1, iri_dllite_ont1_opt);
				manager.saveOntology(ont1opt, new FileOutputStream(file_iri_dllite_ont1_opt.toString()));

				// step 2
				QualifiedExistentialNormalizer dlliterNormalizer = new QualifiedExistentialNormalizer(manager);
				OWLOntology ont2 = dlliterNormalizer.transform(ont1opt, iri_dllite_ont2);
				manager.saveOntology(ont2, new FileOutputStream(file_iri_dllite_ont2.toString()));
				
				// step 3
				ConjunctionNormalizer conjNormalizer = new ConjunctionNormalizer(manager);
				OWLOntology ont3 = conjNormalizer.transform(ont2, iri_dllite_ont3);
				manager.saveOntology(ont3, new FileOutputStream(file_iri_dllite_ont3.toString()));
				
				// step 4
				DLLiteRClosureBuilder dlliterClosure = new DLLiteRClosureBuilder(manager);
				OWLOntology ont4 = dlliterClosure.transform(ont3, iri_dllite_ont4);
				manager.saveOntology(ont4, new FileOutputStream(file_iri_dllite_ont4.toString()));
			
				DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();
				System.out.println(renderer.render(ont4));
			       
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
