package org.semanticweb.ontop.beyondql.datalogexpansion;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;
import org.semanticweb.ontop.beyondql.hornshiq.OntopRuleToSWIPrologTranslator;


public class DatalogExpansion {
    static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    public static void main(String argv[]) throws IOException {

        init();

//        new DatalogExpansion();

//        int depth = 5;
//        testExpand("reach", 2, depth);
//        testExpand("start", 1, depth);
//        testExpand("end", 1, depth);
//        testExpand("p", 1, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_Person", 1, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_Student", 1, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf", 2, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_Employee", 1, depth);
//        testExpand("'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'", 1, depth);

    }



    private  void testExpand(String predicate, int arity, int depth) throws IOException {
        List<CQIE> expansions;

        System.out.println("------------------------------------------");

        System.out.format("Expansions of %s/%d:\n", predicate, arity);
        expansions = expand(predicate, arity, depth);

        Joiner.on("\n").appendTo(System.out, expansions);

        System.out.println();

        System.out.println("------------------------------------------");



    }

    public List<CQIE> expand(String predicate, int arity, int depth) {
        String termTemplate;
        if(arity == 1){
            termTemplate = String.format("%s(_)", predicate);
        } else if(arity == 2){
            termTemplate = String.format("%s(_,_)", predicate);
        } else {
            throw new IllegalArgumentException("airty must be 1 or 2");
        }

        //--------------------------------------------------

        String t2 = String.format("datalog_expansions_opt(%s, %d, Expansions)", termTemplate, depth);
        Query q2 = new Query(t2);

        //--------------------------------------------------

        //List<List<Term>> datalogExpansions = Lists.newArrayList();

        List<DatalogRule> rules = Lists.newArrayList();

        if (q2.hasMoreSolutions()) {
            Map<String, Term> s4 = q2.nextSolution();

            Term expansions1 = s4.get("Expansions");

            if(expansions1.isCompound()){
                Compound expansions = (Compound) expansions1;

                Term[] expansion_list = expansions.toTermArray();

                for (Term t : expansion_list) {
                    if (t instanceof Compound) {
                        DatalogRule cq = new DatalogRule(t);
                        rules.add(cq);
                    } else {
                        System.out.println("catch it");
                        throw new IllegalArgumentException();
                    }
                }
            }
        }

        List<CQIE> cqies = Lists.newArrayList();

        for(DatalogRule datalogRule : rules){
            cqies.add(datalogRule.toCQIE());
            System.out.println(datalogRule.toCQIE());
        }



        return cqies;
    }

    public static void init() {
        String t1 = "consult('src/main/prolog/test.pl')";
        Query q1 = new Query(t1);

        System.out.println(t1 + " " + (q1.hasSolution() ? "succeeded" : "failed"));


//        String t1 = "consult('src/main/prolog/expand.pl')";
//        Query q1 = new Query(t1);
//
//        System.out.println(t1 + " " + (q1.hasSolution() ? "succeeded" : "failed"));
//
//        String t2 = "consult('src/main/prolog/univ_bench_dl.pl')";
//        Query q2 = new Query(t2);
//
//        System.out.println(t2 + " " + (q2.hasSolution() ? "succeeded" : "failed"));
    }


    public DatalogExpansion(DatalogProgram ontopProgram, OBDAModel obdaModel, String prologFile) throws IOException {
        FileWriter writer = null;
        writer = new FileWriter(prologFile);
        InputStream stream = DatalogExpansion.class.getResourceAsStream("/expand.pl");

        String expansionRules = CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));

        writer.append(expansionRules);

        writer.append("\n");

        writer.append(getVocabularyRules(ontopProgram, obdaModel));

        writer.append("\n");

        OntopRuleToSWIPrologTranslator ontopRuleToSWIPrologTranslator = new OntopRuleToSWIPrologTranslator();

        String prologRules = OntopRuleToSWIPrologTranslator.translate(ontopProgram.getRules());

        writer.append(prologRules);

        writer.close();


        String t1 = String.format("consult('%s')", prologFile);

        Query q1 = new Query(t1);

        System.out.println(t1 + " " + (q1.hasSolution() ? "succeeded" : "failed"));
    }




    public String getVocabularyRules(DatalogProgram ontopProgram, OBDAModel obdaModel) {

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

        Collections.sort(orderedPredicates, new Comparator<Predicate>() {
            @Override
            public int compare(Predicate o1, Predicate o2) {
                if (o1.getArity() != o2.getArity()) {
                    return o1.getArity() - o2.getArity();
                } else {
                    return o1.getName().compareTo(o2.getName());
                }

            }
        });

        //:- discontiguous 'http://uob.iodt.ibm.com/univ-bench-dl.owl#isTaughtBy'/2.
        for (Predicate predicate : orderedPredicates) {
            sb.append(String.format(":- discontiguous '%s'/%d.\n", predicate.getName(), predicate.getArity()));
        }

        sb.append("\n");

        sb.append("edb(view(_)).\n");

        for (Predicate predicate : orderedPredicates) {
            switch (predicate.getArity()) {
                case 1:
                    sb.append(String.format("idb('%s'(_)).\n", (predicate.getName())));
                    break;
                case 2:
                    sb.append(String.format("idb('%s'(_,_)).\n", (predicate.getName())));
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        sb.append("\n");

        for (Predicate predicate : orderedPredicates) {
            switch (predicate.getArity()) {
                case 1:
                    sb.append(String.format("'%1$s'(X) :- view('%1$s'(X)).\n", (predicate.getName())));
                    break;
                case 2:
                    sb.append(String.format("'%1$s'(X, Y) :- view('%1$s'(X, Y)).\n", (predicate.getName())));
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        Set<String> freshPredicates = new LinkedHashSet<>();

        for (CQIE cqie : ontopProgram.getRules()) {
            String predicateName = cqie.getHead().getFunctionSymbol().getName();
            if (predicateName.startsWith("http://www.example.org/fresh")) {
                freshPredicates.add(predicateName);
            }
        }

        for (String predicate : freshPredicates) {
            sb.append(String.format("fresh('%s'(_)).\n", predicate));
        }

        sb.append("\n");

        return sb.toString();

    }
}


