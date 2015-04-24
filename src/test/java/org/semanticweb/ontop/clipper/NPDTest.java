package org.semanticweb.ontop.clipper;


import org.semanticweb.ontop.exception.InvalidMappingException;
import org.semanticweb.ontop.model.OBDAException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;
import java.sql.SQLException;


public class NPDTest {
    public static void main(String[] args) throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException {

        String ontologyFile =  "src/test/resources/npd-v2.owl";
        String obdaFile = "src/test/resources/npd-v2-ql_a_postgres.obda";

        Ontology2MappingCompilation.compileHSHIQtoMappings(ontologyFile, obdaFile);
    }



}
