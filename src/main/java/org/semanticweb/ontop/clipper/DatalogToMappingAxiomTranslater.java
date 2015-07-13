package org.semanticweb.ontop.clipper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Term;
import it.unibz.krdb.obda.model.Variable;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.RDBMSourceParameterConstants;
import it.unibz.krdb.obda.model.impl.TermUtils;
import it.unibz.krdb.obda.owlrefplatform.core.queryevaluation.SQLAdapterFactory;
import it.unibz.krdb.obda.owlrefplatform.core.queryevaluation.SQLDialectAdapter;
import it.unibz.krdb.sql.DBMetadata;
import it.unibz.krdb.obda.utils.QueryUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class DatalogToMappingAxiomTranslater {

    static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    static final Function TARGET_QUERY_HEAD = DATA_FACTORY.getFunction(DATA_FACTORY.getPredicate("q", 0));

    static Predicate ANS = DATA_FACTORY.getPredicate("ans", 0);

    private final DBMetadata dbMetadata;
    private final OBDADataSource obdaDataSource;

    public DatalogToMappingAxiomTranslater(DBMetadata dbMetadata, OBDADataSource obdaDataSource){
        this.dbMetadata = dbMetadata;
        this.obdaDataSource = obdaDataSource;
    }

    public OBDAMappingAxiom translate(CQIE cqie) throws OBDAException {

        DatalogProgram programForSourceQuery = DATA_FACTORY.getDatalogProgram(Lists.newArrayList(removeFunctionsInHead(cqie)));

        String parameter = obdaDataSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);

        SQLDialectAdapter sqladapter = SQLAdapterFactory.getSQLDialectAdapter(parameter);

        SQLSourceQueryGenerator sqlGenerator = new SQLSourceQueryGenerator(dbMetadata, sqladapter, false);

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
        // LinkedHashSet preserves order
        Set<Variable> headVariables = new LinkedHashSet<>();
        TermUtils.addReferencedVariablesTo(headVariables, rule.getHead());

        //List<Variable> headVariables = QueryUtils.getVariablesInAtom(rule.getHead());
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
