package org.semanticweb.ontop.clipper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.DatalogProgram;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.OBDAException;
import org.semanticweb.ontop.model.OBDAMappingAxiom;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.Term;
import org.semanticweb.ontop.model.Variable;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.owlrefplatform.core.queryevaluation.SQL99DialectAdapter;
import org.semanticweb.ontop.sql.DBMetadata;
import org.semanticweb.ontop.utils.QueryUtils;

import java.util.ArrayList;
import java.util.List;


public class DatalogToMappingAxiomTranslater {

    static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    static final Function TARGET_QUERY_HEAD = DATA_FACTORY.getFunction(DATA_FACTORY.getPredicate("q", 0));

    static Predicate ANS = DATA_FACTORY.getPredicate("ans", 0);

    private final DBMetadata dbMetadata;

    public DatalogToMappingAxiomTranslater(DBMetadata dbMetadata){
        this.dbMetadata = dbMetadata;
    }

    public OBDAMappingAxiom translate(CQIE cqie) throws OBDAException {
        DatalogProgram programForSourceQuery = DATA_FACTORY.getDatalogProgram( removeFunctionsInHead(cqie));

        SQLSourceQueryGenerator sqlGenerator = new SQLSourceQueryGenerator(dbMetadata, new SQL99DialectAdapter(), false);

        List<Term> headTerms = programForSourceQuery.getRules().get(0).getHead().getTerms();

        List<String> signature = Lists.newArrayList();

        for (Term headTerm: headTerms){
            if (headTerm instanceof  Variable){
                signature.add(((Variable) headTerm).getName());
            }
        }


        String sourceQuery = sqlGenerator.generateSourceQuery(programForSourceQuery, signature);

        CQIE targetQuery = generateTargetQuery(cqie);

        OBDAMappingAxiom mappingAxiom = DATA_FACTORY.getRDBMSMappingAxiom(sourceQuery, targetQuery);
        return mappingAxiom;
    }

    public List<OBDAMappingAxiom> translate(List<CQIE> rules) throws OBDAException {
        List<OBDAMappingAxiom> obdaMappingAxioms = Lists.newArrayList();
        for (CQIE rule : rules) {
            obdaMappingAxioms.add(translate(rule));
        }

        return obdaMappingAxioms;
    }


    /*
     * http://it.unibz.krdb/obda/test/simple#A(URI("http://it.unibz.krdb/obda/test/simple#{}",t1_1))
     *      :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
     * ->
     * ans(t1_1) :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
     */
    public static DatalogProgram removeFunctionsInHead(DatalogProgram p){
        List<CQIE> newRules = Lists.newArrayList();

        for(CQIE rule : p.getRules()){
            CQIE cqie = removeFunctionsInHead(rule);
            newRules.add(cqie);
        }

        return DATA_FACTORY.getDatalogProgram(newRules);
    }

    private static CQIE removeFunctionsInHead(CQIE rule) {
        List<Variable> headVariables = QueryUtils.getVariablesInAtom(rule.getHead());
        return DATA_FACTORY.getCQIE(DATA_FACTORY.getFunction(ANS, (List<Term>) (List<?>) headVariables), rule.getBody());
    }


    /**
     * http://it.unibz.krdb/obda/test/simple#A(URI("http://it.unibz.krdb/obda/test/simple#{}",t1_1))
     *      :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
     * ->
     *  q() :- http://it.unibz.krdb/obda/test/simple#A(URI("http://it.unibz.krdb/obda/test/simple#{t1_1}"))
     *
     */
    public CQIE generateTargetQuery(CQIE rule) {
        return DATA_FACTORY.getCQIE(TARGET_QUERY_HEAD, rule.getHead());

        //Function head = rule.getHead();
        // URITemplates.getUriTemplateString((Function) head.getTerm(0));
    }

}
