package org.semanticweb.ontop.clipper;


import org.semanticweb.ontop.exception.DuplicateMappingException;
import org.semanticweb.ontop.exception.InvalidMappingException;
import org.semanticweb.ontop.io.ModelIOManager;
import org.semanticweb.ontop.model.OBDAException;
import org.semanticweb.ontop.model.OBDAModel;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.sql.SQLException;

import static org.semanticweb.ontop.clipper.Ontology2MappingCompilation.compileHSHIQtoMappings;


public class NPDTest {
    public static void main(String[] args) throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException, DuplicateMappingException {

        String ontologyFile =  "src/test/resources/npd-v2.owl";
        String obdaFile = "src/test/resources/npd-v2-ql_a_postgres.obda";
        String extendedObdaFile = "src/test/resources/extended-npd-v2-ql_a_postgres.obda";

        OBDAModel newModel = compileHSHIQtoMappings(ontologyFile, obdaFile);
        ModelIOManager modelIOManager = new ModelIOManager(newModel);
        modelIOManager.save(extendedObdaFile);
    }
}
