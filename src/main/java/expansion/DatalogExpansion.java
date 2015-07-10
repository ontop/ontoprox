package expansion;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import org.jpl7.Compound;
import org.jpl7.Query;
import org.jpl7.Term;
import org.jpl7.Variable;
import org.semanticweb.ontop.model.CQIE;


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

    private static void testExpand(String predicate, int arity, int k) throws IOException {
        List<DatalogRule> expansions;

        System.out.println("------------------------------------------");

        System.out.format("Expansions of %s/%d:\n", predicate, arity);
        expansions = expand(predicate, arity, k);

        Joiner.on("\n").appendTo(System.out, expansions);

        System.out.println();

        System.out.println("------------------------------------------");


        for(DatalogRule datalogRule : expansions){
            System.out.println(datalogRule.toCQIE());
        }
    }

    private static List<DatalogRule> expand(String predicate, int arity, int depth) {
        String termTemplate;
        if(arity == 1){
            termTemplate = String.format("%s(_)", predicate);
        } else if(arity == 2){
            termTemplate = String.format("%s(_,_)", predicate);
        } else {
            throw new IllegalArgumentException("airty must be 1 or 2");
        }

        //--------------------------------------------------

        String t2 = String.format("datalog_expansions(%s, %d, Expansions)", termTemplate, depth);
        Query q2 = new Query(t2);

        //--------------------------------------------------

        //List<List<Term>> datalogExpansions = Lists.newArrayList();

        List<DatalogRule> rules = Lists.newArrayList();

        if (q2.hasMoreSolutions()) {
            Map<String, Term> s4 = q2.nextSolution();
            Compound expansions = (Compound) s4.get("Expansions");

            //List<Term> expansion_list = flatten(expansions);
            //List<Term> expansion_list = flattenList(expansions);

            Term[] expansion_list = expansions.toTermArray();


            for (Term t : expansion_list) {

                if (t instanceof Compound) {
                    DatalogRule cq = new DatalogRule(t);
                    rules.add(cq);
                } else {
                    System.out.println("catch it");
                }


                //datalogExpansions.add(expansion);
                //System.out.println(expansion);
            }
        }

        return rules;
    }

    private static void init() {
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

    public static List<Term> flatten(Compound compound){
        List<Term> list = Lists.newArrayList();

        while(compound.arity() == 2){
            // 1 based
            Term arg1 = compound.arg(1);
            list.add(arg1);


            if(compound.arg(2) instanceof Compound){
                compound = (Compound) compound.arg(2);
            } else {
                //list.add(compound.arg(2));
                break;
            }

//            try {
//                compound = (Compound) compound.arg(2);
//            }catch (Exception e){
//                System.out.println("catch it!");
//            }
        }

        return list;
    }


}


