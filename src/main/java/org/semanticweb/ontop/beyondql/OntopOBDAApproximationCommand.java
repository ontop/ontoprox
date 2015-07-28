package org.semanticweb.ontop.beyondql;


import it.unibz.krdb.obda.exception.DuplicateMappingException;
import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAModel;
import org.semanticweb.ontop.beyondql.hornshiq.HSHIQOBDAToDLLiteROBDARewriter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;


public class OntopOBDAApproximationCommand {

    public static void main(String[] args) throws SQLException, IOException, InvalidMappingException, DuplicateMappingException, OBDAException, OWLOntologyCreationException, OWLOntologyStorageException {

        long t1 = System.currentTimeMillis();

        if(args.length != 4){
            System.err.println("Usage: OntopOBDAApproximationCommand ontology.owl mapping.obda " +
                    "newOntology.owl newMapping.obda");
            System.exit(-1);
        }


        String ontologyFile =  args[0];
        String obdaFile = args[1];
        String rewrittenOntologyFile = args[2];
        String rewrittenOBDAFile = args[3];

        HSHIQOBDAToDLLiteROBDARewriter rewriter = new HSHIQOBDAToDLLiteROBDARewriter(ontologyFile, obdaFile);
        rewriter.rewrite();

        OBDAModel newModel = rewriter.getRewrittenOBDAModel();
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(rewrittenOBDAFile);

        System.err.println("new mappings saved in: " + rewrittenOBDAFile);


        OWLOntology rewrittenOntology = rewriter.getRewrittenOntology();
        OWLManager.createOWLOntologyManager().saveOntology(rewrittenOntology,
                new RDFXMLOntologyFormat(),
                new FileOutputStream(rewrittenOntologyFile));

        System.err.println("new ontology saved in: " + rewrittenOntologyFile);


        long t2 = System.currentTimeMillis();

        System.err.println("total time: " + (t2 - t1) + "ms");
    }
}
