package org.semanticweb.ontop.beyondql.mapgen;

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
import it.unibz.krdb.obda.model.ValueConstant;
import it.unibz.krdb.obda.model.Variable;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.RDBMSourceParameterConstants;
import it.unibz.krdb.obda.owlrefplatform.core.queryevaluation.SQLAdapterFactory;
import it.unibz.krdb.obda.owlrefplatform.core.queryevaluation.SQLDialectAdapter;
import it.unibz.krdb.sql.DBMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class DatalogToMappingAxiomTranslater {

    static final OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    static final Function TARGET_QUERY_HEAD = DATA_FACTORY.getFunction(DATA_FACTORY.getPredicate("q", 0));

    static Predicate ANS = DATA_FACTORY.getPredicate("ans", 0);

    private final DBMetadata dbMetadata;
    private final OBDADataSource obdaDataSource;

    public DatalogToMappingAxiomTranslater(DBMetadata dbMetadata, OBDADataSource obdaDataSource) {
        this.dbMetadata = dbMetadata;
        this.obdaDataSource = obdaDataSource;
    }

    public OBDAMappingAxiom translate(CQIE cqie) throws OBDAException {

        CQIE cqieClone = cqie.clone();

        Map<Variable, ValueConstant> newVariableMap = new HashMap<>();

        List<Term> newHeadTerms = new ArrayList<>();

        DatalogProgram programForSourceQuery = DATA_FACTORY.getDatalogProgram(Lists.newArrayList(removeFunctionsInHead(cqie, /* out */ newVariableMap, /* out */ newHeadTerms)));

        String jdbcDriverClassName = obdaDataSource.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER);

        SQLDialectAdapter sqladapter = SQLAdapterFactory.getSQLDialectAdapter(jdbcDriverClassName);

        SQLSourceQueryGenerator sqlGenerator = new SQLSourceQueryGenerator(dbMetadata, sqladapter, false);

        List<Term> headTerms = programForSourceQuery.getRules().get(0).getHead().getTerms();

        List<String> signature = Lists.newArrayList();

        for (Term headTerm : headTerms) {
            if (headTerm instanceof Variable) {
                signature.add(((Variable) headTerm).getName());
            } else {
                throw new IllegalStateException();
            }
        }

        String sourceQuery = sqlGenerator.generateSourceQuery(programForSourceQuery, signature, newVariableMap);

        CQIE targetQuery = generateTargetQuery(cqieClone, newHeadTerms);

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


//    /*
//     * http://it.unibz.krdb/obda/test/simple#A(URI("http://it.unibz.krdb/obda/test/simple#{}",t1_1))
//     *      :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
//     * ->
//     * ans(t1_1) :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
//     */
//    public static DatalogProgram removeFunctionsInHead(DatalogProgram p){
//        List<CQIE> newRules = Lists.newArrayList();
//
//        Map<Variable, ValueConstant> map = new HashMap<>();
//
//        for(CQIE rule : p.getRules()){
//            CQIE cqie = removeFunctionsInHead(rule, map);
//            newRules.add(cqie);
//        }
//
//        return DATA_FACTORY.getDatalogProgram(newRules);
//    }


    /**
     * q(URI("http://www.Department{}.University{}.edu/{}{}",t2_14,t3_14,"Lecturer",t1_14))
     * ->
     * ans(t2_14,t3_14,tnew,t1_14),
     * {v_Lecture -> "Lecturer"}
     */
    private static CQIE removeFunctionsInHead(CQIE rule, Map<Variable, ValueConstant> newVariableMap, List<Term> newHeadTerms) {
        // LinkedHashSet preserves order
        Set<Variable> headVariables = new LinkedHashSet<>();
        //TermUtils.addReferencedVariablesTo(headVariables, rule.getHead());

//        List<Term> newHeadTerms = new ArrayList<>();

        for (Term term : rule.getHead().getTerms()) {

            if (term instanceof Function) {
                List<Term> args = ((Function) term).getTerms();

                List<Term> newArgs = Lists.newArrayList();
                newArgs.add(args.get(0));

                for (int i = 1; i < args.size(); i++) {
                    Term arg_i = args.get(i);
                    if (arg_i instanceof Variable) {
                        headVariables.add((Variable) arg_i);
                        newArgs.add(arg_i);
                    } else if (arg_i instanceof ValueConstant) {
                        // If we encounter a constant, then we introduce a new variable for it
                        Variable newVar = DATA_FACTORY.getVariable("v_" + ((ValueConstant) arg_i).getValue());
                        newVariableMap.put(newVar, (ValueConstant) arg_i);
                        headVariables.add(newVar);
                        newArgs.add(newVar);
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }

                newHeadTerms.add(DATA_FACTORY.getFunction(((Function) term).getFunctionSymbol(), newArgs));
            } else if (term instanceof Variable) {
                newHeadTerms.add(term);
                headVariables.add((Variable) term);
            } else if (term instanceof ValueConstant) {
                Variable newVar = DATA_FACTORY.getVariable("v_" + ((ValueConstant) term).getValue());
                newVariableMap.put(newVar, (ValueConstant) term);
                headVariables.add(newVar);
            } else {
                throw new UnsupportedOperationException();
            }
        }


        //newHeadContainer[0] = DATA_FACTORY.getFunction(rule.getHead().getFunctionSymbol(), newHeadTerms);

        //List<Variable> headVariables = QueryUtils.getVariablesInAtom(rule.getHead());
        return DATA_FACTORY.getCQIE(DATA_FACTORY.getFunction(ANS, Lists.<Term>newArrayList(headVariables)), rule.getBody());
    }


    /**
     * http://it.unibz.krdb/obda/test/simple#A(URI("http://it.unibz.krdb/obda/test/simple#{}",t1_1))
     * :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
     * ->
     * q() :- http://it.unibz.krdb/obda/test/simple#A(URI("http://it.unibz.krdb/obda/test/simple#{t1_1}"))
     */
    public CQIE generateTargetQuery(CQIE rule, List<Term> newHeadTerms) {
        return DATA_FACTORY.getCQIE(TARGET_QUERY_HEAD, DATA_FACTORY.getFunction(rule.getHead().getFunctionSymbol(), newHeadTerms));

        //Function head = rule.getHead();
        // URITemplates.getUriTemplateString((Function) head.getTerm(0));
    }

}
