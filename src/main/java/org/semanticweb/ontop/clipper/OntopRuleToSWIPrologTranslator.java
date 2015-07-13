package org.semanticweb.ontop.clipper;


import com.google.common.base.Joiner;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.Predicate;


import java.util.List;

public class OntopRuleToSWIPrologTranslator {

    public static String translate(List<CQIE> cqs) {

        StringBuilder programBuilder = new StringBuilder();

        for (CQIE cq : cqs){



            programBuilder.append(translate(cq));

        }


        return programBuilder.toString();
    }

    private static String translate(CQIE cq) {

        StringBuilder sb = new StringBuilder();
        sb.append(translate(cq.getHead()));
        if(cq.getBody().size() > 0) {
            sb.append(" :- ");

            boolean first = true;

            for(Function bodyAtom : cq.getBody()){
                if(!first){
                    sb.append(",");
                }
                sb.append(translate(bodyAtom));
                first = false;
            }


        }
        sb.append(".");
        return sb.toString();


    }

    public static String translate(Function f){
        return translate(f.getFunctionSymbol()) + "(" + Joiner.on(",").join(f.getTerms()) + ")";
    }



    public static String translate(Predicate predicate){
        String predicateName = predicate.getName();
        return String.format("'%s'", predicateName);
    }


}
