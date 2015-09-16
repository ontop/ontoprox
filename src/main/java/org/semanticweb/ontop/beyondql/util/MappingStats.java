package org.semanticweb.ontop.beyondql.util;

import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.utils.MappingSplitter;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class MappingStats {

    private static OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();
    public static void main(String[] args) {
        OBDAModel obdaModel = DATA_FACTORY.getOBDAModel();

        ModelIOManager ioManager = new ModelIOManager(obdaModel);

        File obdafile = new File("src/test/resources/uobm/univ-bench-dl.obda");
        try {
            ioManager.load(obdafile);
        } catch (IOException | InvalidMappingException e) {
            e.printStackTrace();
        }

        URI sourceURI = obdaModel.getSources().iterator().next().getSourceID();

        int size = obdaModel.getMappings(sourceURI).size();

        System.out.println("Nr. of mapping declarations in the obda file: " + size);


        MappingSplitter.splitMappings(obdaModel, sourceURI);

        size = obdaModel.getMappings(sourceURI).size();


        System.out.println("Nr. of split mappings: " + size);
    }
}
