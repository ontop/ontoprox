package expansion;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import jpl.Compound;
import jpl.Query;
import jpl.Term;

public class DatalogExpansion {

    public static void main(String argv[]) throws IOException {

        init();

        int k = 4;
        testExpand("reach", 2, k);
        testExpand("start", 1, k);
        testExpand("end", 1, k);
    }

    private static void testExpand(String predicate, int arity, int k) throws IOException {
        List<DatalogRule> expansions;

        System.out.println("------------------------------------------");

        System.out.format("Expansions of %s/%d:\n", predicate, arity);
        expansions = expand(predicate, arity, k);

        Joiner.on("\n").appendTo(System.out, expansions);

        System.out.println();
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
            Hashtable s4 = q2.nextSolution();
            Compound expansions = (Compound) s4.get("Expansions");

            List<Term> expansion_list = flatten(expansions);


            for (Term t : expansion_list) {

                List<Term> expansion = flatten((Compound) t);
                //datalogExpansions.add(expansion);
                DatalogRule datalogRule = new DatalogRule(expansion);
                rules.add(datalogRule);
                //System.out.println(expansion);
            }
        }

        return rules;
    }

    private static void init() {
        String t1 = "consult('src/main/prolog/expand.pl')";
        Query q1 = new Query(t1);

        System.out.println(t1 + " " + (q1.hasSolution() ? "succeeded" : "failed"));
    }

    public static List<Term> flatten(Compound compound){
        List<Term> list = Lists.newArrayList();

        while(compound.arity() == 2){
            // 1 based
            Term arg1 = compound.arg(1);
            list.add(arg1);
            compound = (Compound) compound.arg(2);
        }

        return list;
    }


}


