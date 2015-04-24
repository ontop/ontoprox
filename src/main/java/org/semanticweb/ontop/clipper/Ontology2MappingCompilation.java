package org.semanticweb.ontop.clipper;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.ontop.exception.InvalidMappingException;
import org.semanticweb.ontop.io.ModelIOManager;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.DatalogProgram;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.OBDADataSource;
import org.semanticweb.ontop.model.OBDAException;
import org.semanticweb.ontop.model.OBDAModel;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.Term;
import org.semanticweb.ontop.model.Variable;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.owlrefplatform.core.QuestConstants;
import org.semanticweb.ontop.owlrefplatform.core.queryevaluation.SQL99DialectAdapter;
import org.semanticweb.ontop.owlrefplatform.core.sql.SQLGenerator;
import org.semanticweb.ontop.sql.DBMetadata;
import org.semanticweb.ontop.sql.JDBCConnectionManager;
import org.semanticweb.ontop.utils.DatalogDependencyGraphGenerator;
import org.semanticweb.ontop.utils.Mapping2DatalogConverter;
import org.semanticweb.ontop.utils.QueryUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.ontop.owlrefplatform.core.unfolding.DatalogUnfolder;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;


public class Ontology2MappingCompilation {

    static OBDADataFactory fac = OBDADataFactoryImpl.getInstance();

    static Predicate ANS = fac.getPredicate("ans", 0);

    public static void compileHSHIQtoMappings(String ontologyFile, String obdaFile) throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException {
        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();


        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(ontologyFile));

        qaHornSHIQ.setOntologies(ImmutableSet.of(ontology));

        List<CQ> program = qaHornSHIQ.rewriteOntology();

        DatalogProgram ontopProgram = ClipperRuleToOntopRuleTranslator.translate(program);

        System.out.println(ontopProgram);

        DatalogDependencyGraphGenerator dg = new DatalogDependencyGraphGenerator(ontopProgram);

        List<Predicate> predicatesInBottomUp = dg.getPredicatesInBottomUp();

        for(Predicate p:predicatesInBottomUp){
            System.out.println(p);
        }

        OBDAModel obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);

        File obdafile = new File(obdaFile);
        ioManager.load(obdafile);

        OBDADataSource obdaDataSource = obdaModel.getSources().iterator().next();

        DBMetadata dbMetadata = JDBCConnectionManager.getJDBCConnectionManager().getMetaData(obdaDataSource);
        Mapping2DatalogConverter mapping2DatalogConverter = new Mapping2DatalogConverter(dbMetadata);

        List<CQIE> mappingProgram = mapping2DatalogConverter.constructDatalogProgram(obdaModel.getMappings(obdaDataSource.getSourceID()));

        //Joiner.on("\n").appendTo(System.out, mappingProgram);


        List<Predicate> predicatesDefinedByMapping = collectHeadPredicates(mappingProgram);

        List<Predicate> predicatesToDefine =  Lists.newArrayList(predicatesInBottomUp);


        /*
         * order matters
         */
        //predicatesToDefine.removeAll(predicatesDefinedByMapping);


        System.out.println("Generated mappings");
        System.out.println("------------------");




        List<CQIE> newMappings = Lists.newArrayList();


        //npdv:Wellbore

        //predicatesToDefine = ImmutableList.of(fac.getClassPredicate("http://sws.ifi.uio.no/vocab/npd-v2#Wellbore"));

        for(Predicate predicate : predicatesToDefine) {

            if(predicate.getName().equals("http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation")){
                System.out.println("catch it!");
            }

            Collection<CQIE> cqies = dg.getRuleIndex().get(predicate);

            if(cqies.size() == 0){
                continue;
            }

            DatalogProgram queryProgram = fac.getDatalogProgram(cqies);

            DatalogProgram queryAndMappingProgram = fac.getDatalogProgram();
            queryAndMappingProgram.appendRule(queryProgram.getRules());
            queryAndMappingProgram.appendRule(mappingProgram);

            //queryProgram.appendRule(mappingProgram);


            DatalogUnfolder unfolder = new DatalogUnfolder(fac.getDatalogProgram(mappingProgram));

            Multimap<Predicate, Integer> multiTypedFunctionSymbolIndex = ArrayListMultimap.create();

            //DatalogProgram unfolding = unfolder.unfold(queryProgram, predicate.getName(), QuestConstants.BUP, false, multiTypedFunctionSymbolIndex);


            DatalogProgram unfolding = unfolder.unfold(queryProgram, predicate.getName(), QuestConstants.BUP, true, multiTypedFunctionSymbolIndex);

            DatalogProgram programForSourceQuery = removeFunctionsInHead(unfolding);

            SQLSourceQueryGenerator sqlGenerator = new SQLSourceQueryGenerator(dbMetadata, new SQL99DialectAdapter(), false);

            String sourceQuery = sqlGenerator.generateSourceQuery(programForSourceQuery, ImmutableList.of("x"));

            System.out.println(sourceQuery);


            newMappings = unfolding.getRules();
            mappingProgram.addAll(newMappings);

            System.out.println();
            System.out.println(predicate);
            System.out.println(unfolding);
        }
    }

    public static List<Predicate> collectHeadPredicates(List<CQIE> rules) {
        List<Predicate> headPredicates = Lists.newArrayList();
        for(CQIE rule : rules) {
            headPredicates.add(rule.getHead().getFunctionSymbol());
        }

        return headPredicates;
    }

    /*
     * http://it.unibz.krdb/obda/test/simple#A(URI("http://it.unibz.krdb/obda/test/simple#{}",t1_1)) :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
     * ->
     * ans(t1_1) :- TABLE2(t1_1,t2_1,t3_1), LT(t1_1,5), TABLE2(t1_1,t2_2,t3_2), LT(t1_1,3), TABLE2(t1_1,t2_3,t3_3), GT(t1_1,1)
     */
    public static DatalogProgram removeFunctionsInHead(DatalogProgram p){
        List<CQIE> newRules = Lists.newArrayList();

        for(CQIE rule : p.getRules()){
            List<Variable> headVariables = QueryUtils.getVariablesInAtom(rule.getHead());
            newRules.add( fac.getCQIE(fac.getFunction(ANS, (List<Term>) (List<?>) headVariables), rule.getBody()));
        }

        return fac.getDatalogProgram(newRules);


    }
}
