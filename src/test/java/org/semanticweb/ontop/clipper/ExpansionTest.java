package org.semanticweb.ontop.clipper;

import com.beust.jcommander.internal.Lists;
import org.semanticweb.ontop.exception.InvalidMappingException;
import org.semanticweb.ontop.io.ModelIOManager;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.OBDADataSource;
import org.semanticweb.ontop.model.OBDAMappingAxiom;
import org.semanticweb.ontop.model.OBDAModel;
import org.semanticweb.ontop.model.OBDAQuery;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.ontology.DataPropertyExpression;
import org.semanticweb.ontop.ontology.OClass;
import org.semanticweb.ontop.ontology.ObjectPropertyExpression;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExpansionTest {

    static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    @Test
    public void test() throws IOException, InvalidMappingException {

        String obdaFile =     "src/test/resources/uobm/univ-bench-dl.obda";

        OBDAModel obdaModel = DATA_FACTORY.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        File obdafile = new File(obdaFile);
        ioManager.load(obdafile);

        StringBuilder sb = new StringBuilder();

        OBDADataSource dataSource = obdaModel.getSources().iterator().next();

        ArrayList<OBDAMappingAxiom> mappingAxioms = obdaModel.getMappings().get(dataSource.getSourceID());


        Set<Predicate> predicates = new HashSet<>();

        for (OBDAMappingAxiom mappingAxiom : mappingAxioms) {
            CQIE targetQuery = (CQIE)mappingAxiom.getTargetQuery();

            for (Function function : targetQuery.getBody()) {

                Predicate predicate = function.getFunctionSymbol();

                predicates.add(predicate);
            }
        }

        List<Predicate> orderedPredicates = Lists.newArrayList(predicates);

        Collections.sort(orderedPredicates, new Comparator<Predicate>() {
            @Override
            public int compare(Predicate o1, Predicate o2) {
                if(o1.getArity() != o2.getArity()){
                    return o1.getArity() - o2.getArity();
                } else {
                    return o1.getName().compareTo(o2.getName());
                }

            }
        });


        for (Predicate predicate : orderedPredicates) {
            switch (predicate.getArity()) {
                case 1:
                    sb.append(String.format("edb(v_%s(_)).\n", normalizeName(predicate.getName())));
                    break;
                case 2:
                    sb.append(String.format("edb(v_%s(_,_)).\n", normalizeName(predicate.getName())));
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        for (Predicate predicate : orderedPredicates) {
            switch (predicate.getArity()) {
                case 1:
                    sb.append(String.format("%1$s(X) :- v_%1$s(X).\n", normalizeName(predicate.getName())));
                    break;
                case 2:
                    sb.append(String.format("%1$s(X, Y) :- v_%1$s(X, Y).\n", normalizeName(predicate.getName())));
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        System.out.println(sb);

    }



    public static String normalizeName(String input){

        String alphaAndDigits = input.replaceAll("[^a-zA-Z0-9]","_");
        return alphaAndDigits;
    }


}
