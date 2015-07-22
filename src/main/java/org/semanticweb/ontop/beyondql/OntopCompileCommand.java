package org.semanticweb.ontop.beyondql;


import it.unibz.krdb.obda.exception.DuplicateMappingException;
import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAModel;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.sql.SQLException;

import static org.semanticweb.ontop.beyondql.hornshiq.Ontology2MappingCompilation.compileHSHIQtoMappings;

public class OntopCompileCommand {

    public static void main(String[] args) throws SQLException, IOException, InvalidMappingException, DuplicateMappingException, OBDAException, OWLOntologyCreationException {

        if(args.length != 3){
            System.err.println("Usage: OntopCompileCommand ontology.owl mapping.obda newMapping.obda");
            System.exit(-1);
        }


        String ontologyFile =  args[0];
        String obdaFile = args[1];
        String extendedObdaFile = args[2];

        OBDAModel newModel = compileHSHIQtoMappings(ontologyFile, obdaFile);
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(extendedObdaFile);
    }
}
