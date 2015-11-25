package org.semanticweb.ontop.beyondql.hornshiq;

import com.beust.jcommander.internal.Lists;
import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ExpansionTest {

    static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    @Test
    public void test() throws IOException, InvalidMappingException {

        String obdaFile = "src/test/resources/uobm/univ-bench-dl.obda";

        OBDAModel obdaModel = DATA_FACTORY.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        File obdafile = new File(obdaFile);
        ioManager.load(obdafile);

        StringBuilder sb = new StringBuilder();

        OBDADataSource dataSource = obdaModel.getSources().iterator().next();

        ArrayList<OBDAMappingAxiom> mappingAxioms = obdaModel.getMappings().get(dataSource.getSourceID());


        Set<Predicate> predicates = new HashSet<>();

        for (OBDAMappingAxiom mappingAxiom : mappingAxioms) {
            List<Function> targetQuery = mappingAxiom.getTargetQuery();

            for (Function function : targetQuery) {

                Predicate predicate = function.getFunctionSymbol();

                predicates.add(predicate);
            }
        }

        List<Predicate> orderedPredicates = Lists.newArrayList(predicates);

        Collections.sort(orderedPredicates, (o1, o2) -> {
            if (o1.getArity() != o2.getArity()) {
                return o1.getArity() - o2.getArity();
            } else {
                return o1.getName().compareTo(o2.getName());
            }

        });


        sb.append("edb(view(_)).\n");

       for (Predicate predicate : orderedPredicates) {
            switch (predicate.getArity()) {
                case 1:
                    sb.append(String.format("idb(%s(_)).\n", normalizeName(predicate.getName())));
                    break;
                case 2:
                    sb.append(String.format("idb(%s(_,_)).\n", normalizeName(predicate.getName())));
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        for (Predicate predicate : orderedPredicates) {
            switch (predicate.getArity()) {
                case 1:
                    sb.append(String.format("%1$s(X) :- view(%1$s(X)).\n", normalizeName(predicate.getName())));
                    break;
                case 2:
                    sb.append(String.format("%1$s(X, Y) :- view(%1$s(X, Y)).\n", normalizeName(predicate.getName())));
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        System.out.println(sb);

    }


    public static String normalizeName(String input) {

//        if (Pattern.matches(".*[^a-zA-Z0-9].*", input)) {
//            return "'" + input + "'";
//        } else {
//            return input;
//        }

        String alphaAndDigits = input.replaceAll("[^a-zA-Z0-9]","_");
        return alphaAndDigits;
    }


}
