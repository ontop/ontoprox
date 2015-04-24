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
import org.semanticweb.ontop.model.OBDAMappingAxiom;
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

            DatalogToMappingAxiomTranslater datalogToMappingAxiomTranslater = new DatalogToMappingAxiomTranslater(dbMetadata);
            OBDAMappingAxiom mappingAxiom = datalogToMappingAxiomTranslater.translate(unfolding.getRules().get(0));

            System.out.println("---------------------------------------------------------------------");

            System.out.println(mappingAxiom);

            System.out.println("---------------------------------------------------------------------");

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


}
