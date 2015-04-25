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
import org.semanticweb.ontop.io.PrefixManager;
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
import org.semanticweb.ontop.renderer.SourceQueryRenderer;
import org.semanticweb.ontop.renderer.TargetQueryRenderer;
import org.semanticweb.ontop.sql.DBMetadata;
import org.semanticweb.ontop.sql.JDBCConnectionManager;
import org.semanticweb.ontop.utils.DatalogDependencyGraphGenerator;
import org.semanticweb.ontop.utils.Mapping2DatalogConverter;
import org.semanticweb.ontop.utils.QueryUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.ontop.owlrefplatform.core.unfolding.DatalogUnfolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Ontology2MappingCompilation {

    static OBDADataFactory fac = OBDADataFactoryImpl.getInstance();

    private static Logger log = LoggerFactory.getLogger(Ontology2MappingCompilation.class);

    public static void compileHSHIQtoMappings(String ontologyFile, String obdaFile) throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException {

        /**
         * load ontology using OWL-API
         */
        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(ontologyFile));

        /**
         * load the mappings using Ontop API
         */
        OBDAModel obdaModel = fac.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        File obdafile = new File(obdaFile);
        ioManager.load(obdafile);

        compileHSHIQOntologyToMappings(ontology, obdaModel);
    }

    private static OBDAModel compileHSHIQOntologyToMappings(OWLOntology ontology, OBDAModel obdaModel) throws SQLException, OBDAException {

        /** create a Clipper Reasoner instance */
        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();

        /** feed the ontology to Clipper reasoner */
        qaHornSHIQ.setOntologies(ImmutableSet.of(ontology));

        /** rewrite the ontology to a datalog program represented in Clipper Native API */
        List<CQ> program = qaHornSHIQ.rewriteOntology();

        /** convert the datalog program to Ontop Native API */
        DatalogProgram ontopProgram = ClipperRuleToOntopRuleTranslator.translate(program);

        log.debug("translate program from Clipper {}", ontopProgram);

        /**
         * construct a dependency graph
         */
        DatalogDependencyGraphGenerator dg = new DatalogDependencyGraphGenerator(ontopProgram);

        /**
         * When the program is non-recursive, a topological order exists
         */
        List<Predicate> predicatesInBottomUp = dg.getPredicatesInBottomUp();

        /**
         * We assume we are in Virtual mode and therefore we only have one data source.
         */
        OBDADataSource obdaDataSource = obdaModel.getSources().iterator().next();
        ArrayList<OBDAMappingAxiom> mappingAxioms = obdaModel.getMappings(obdaDataSource.getSourceID());

        DBMetadata dbMetadata = JDBCConnectionManager.getJDBCConnectionManager().getMetaData(obdaDataSource);
        Mapping2DatalogConverter mapping2DatalogConverter = new Mapping2DatalogConverter(dbMetadata);

        /**
         * convert the mappings into a set of rules
         */
        List<CQIE> mappingProgram = mapping2DatalogConverter.constructDatalogProgram(mappingAxioms);

        //Joiner.on("\n").appendTo(System.out, mappingProgram);


        List<Predicate> predicatesDefinedByMapping = collectHeadPredicates(mappingProgram);

        List<Predicate> predicatesToDefine =  Lists.newArrayList(predicatesInBottomUp);

        /*
         * order matters
         */
        //predicatesToDefine.removeAll(predicatesDefinedByMapping);


        List<CQIE> newMappings = Lists.newArrayList();

        //npdv:Wellbore

        //predicatesToDefine = ImmutableList.of(fac.getClassPredicate("http://sws.ifi.uio.no/vocab/npd-v2#Wellbore"));


        List<OBDAMappingAxiom> newObdaMappingAxioms = Lists.newArrayList();

        /*
         * Construct new mapping following the bottom-up order
         */
        for(Predicate predicate : predicatesToDefine) {

//            if(predicate.getName().equals("http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation")){
//                System.out.println("catch it!");
//            }

            List<OBDAMappingAxiom> newObdaMappingAxiomsForAPredicate = Lists.newArrayList();

            Collection<CQIE> cqies = dg.getRuleIndex().get(predicate);


            for(CQIE cqie: cqies) {

                DatalogProgram queryProgram = fac.getDatalogProgram(cqie);

                DatalogProgram queryAndMappingProgram = fac.getDatalogProgram();
                //queryAndMappingProgram.appendRule(queryProgram.getRules());
                queryAndMappingProgram.appendRule(mappingProgram);

                DatalogUnfolder unfolder = new DatalogUnfolder(fac.getDatalogProgram(mappingProgram));

                Multimap<Predicate, Integer> multiTypedFunctionSymbolIndex = ArrayListMultimap.create();

                /**
                 * Unfold
                 */
                DatalogProgram unfolding = unfolder.unfold(queryProgram, predicate.getName(), QuestConstants.BUP, true, multiTypedFunctionSymbolIndex);

                DatalogToMappingAxiomTranslater datalogToMappingAxiomTranslater = new DatalogToMappingAxiomTranslater(dbMetadata);
                List<OBDAMappingAxiom> obdaMappingAxioms = datalogToMappingAxiomTranslater.translate(unfolding.getRules());

                newMappings = unfolding.getRules();
                mappingProgram.addAll(newMappings);

                System.out.println();
                System.out.println(predicate);
                System.out.println(unfolding);

                newObdaMappingAxiomsForAPredicate.addAll(obdaMappingAxioms);
            }

            for(OBDAMappingAxiom m : newObdaMappingAxiomsForAPredicate){
                printOBDAMapping(m, obdaModel.getPrefixManager());
                System.out.println();
            }

        }


        return null;
    }

    public static List<Predicate> collectHeadPredicates(List<CQIE> rules) {
        List<Predicate> headPredicates = Lists.newArrayList();
        for(CQIE rule : rules) {
            headPredicates.add(rule.getHead().getFunctionSymbol());
        }

        return headPredicates;
    }

    public static void printOBDAMapping(OBDAMappingAxiom mappingAxiom, PrefixManager prefixManager){
        // ModelIOManager modelIOManager = new ModelIOManager();
        System.out.println("mappingId\t" + mappingAxiom.getId() );
        System.out.println("target\t\t" + TargetQueryRenderer.encode(mappingAxiom.getTargetQuery(), prefixManager));
        System.out.println("source\t\t" + SourceQueryRenderer.encode(mappingAxiom.getSourceQuery()).replace("\n", " "));
    }

}
