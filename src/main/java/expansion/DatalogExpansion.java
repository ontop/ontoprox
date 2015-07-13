package expansion;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import it.unibz.krdb.obda.model.CQIE;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;
import org.jpl7.Variable;


public class DatalogExpansion {

    public static void main(String argv[]) throws IOException {

        init();

        int depth = 5;
//        testExpand("reach", 2, depth);
//        testExpand("start", 1, depth);
//        testExpand("end", 1, depth);
//        testExpand("p", 1, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_Person", 1, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_Student", 1, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_subOrganizationOf", 2, depth);
//        testExpand("http___uob_iodt_ibm_com_univ_bench_dl_owl_Employee", 1, depth);
        testExpand("'http://uob.iodt.ibm.com/univ-bench-dl.owl#Chair'", 1, depth);

    }

    private static void testExpand(String predicate, int arity, int depth) throws IOException {
        List<CQIE> expansions;

        System.out.println("------------------------------------------");

        System.out.format("Expansions of %s/%d:\n", predicate, arity);
        expansions = expand(predicate, arity, depth);

        Joiner.on("\n").appendTo(System.out, expansions);

        System.out.println();

        System.out.println("------------------------------------------");



    }

    public static List<CQIE> expand(String predicate, int arity, int depth) {
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



}


