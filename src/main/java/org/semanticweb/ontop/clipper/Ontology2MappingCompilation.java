package org.semanticweb.ontop.clipper;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.Invokable;
import expansion.DatalogExpansion;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.ontop.exception.DuplicateMappingException;
import org.semanticweb.ontop.exception.InvalidMappingException;
import org.semanticweb.ontop.io.ModelIOManager;
import org.semanticweb.ontop.io.PrefixManager;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.DatalogProgram;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.OBDADataSource;
import org.semanticweb.ontop.model.OBDAException;
import org.semanticweb.ontop.model.OBDAMappingAxiom;
import org.semanticweb.ontop.model.OBDAModel;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.owlrefplatform.core.basicoperations.QueryAnonymizer;
import org.semanticweb.ontop.renderer.SourceQueryRenderer;
import org.semanticweb.ontop.renderer.TargetQueryRenderer;
import org.semanticweb.ontop.sql.DBMetadata;
import org.semanticweb.ontop.sql.JDBCConnectionManager;
import org.semanticweb.ontop.utils.DatalogDependencyGraphGenerator;
import org.semanticweb.ontop.utils.Mapping2DatalogConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.ontop.owlrefplatform.core.unfolding.DatalogUnfolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Ontology2MappingCompilation {

    private static OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    private static Logger log = LoggerFactory.getLogger(Ontology2MappingCompilation.class);

    private static final String QUERY_HEAD_PREDICATE_NAME = "q";

    private static final Predicate CLASS_QUERY_HEAD_PREDICATE = DATA_FACTORY.getClassPredicate(QUERY_HEAD_PREDICATE_NAME);

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static OBDAModel compileHSHIQtoMappings(String ontologyFile, String obdaFile) throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException, OBDAException, DuplicateMappingException {

        /**
         * load ontology using OWL-API
         */
        OWLOntology ontology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(new File(ontologyFile));

        /**
         * load the mappings using Ontop API
         */
        OBDAModel obdaModel = DATA_FACTORY.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        File obdafile = new File(obdaFile);
        ioManager.load(obdafile);

        OBDAModel newOBDAModel = compileHSHIQOntologyToMappings(ontology, obdaModel);

        return newOBDAModel;
    }

    private static OBDAModel compileHSHIQOntologyToMappings(OWLOntology ontology, OBDAModel obdaModel) throws SQLException, OBDAException, DuplicateMappingException {

        long t1 = System.currentTimeMillis();

        /** create a Clipper Reasoner instance */
        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();

        /** feed the ontology to Clipper reasoner */
        qaHornSHIQ.setOntologies(ImmutableSet.of(ontology));

        /** rewrite the ontology to a datalog program represented in Clipper Native API */
        List<CQ> program = qaHornSHIQ.rewriteOntology();

        /** convert the datalog program to Ontop Native API */
        DatalogProgram ontopProgram = ClipperRuleToOntopRuleTranslator.translate(program);

        long t2 = System.currentTimeMillis();

        System.err.println("Datalog generation time: " + (t2 - t1) + "ms");

        //log.debug("translate program from Clipper {}", ontopProgram);

        t1 = System.currentTimeMillis();

        /**
         * construct a dependency graph
         */
        DatalogDependencyGraphGenerator dg = new DatalogDependencyGraphGenerator(ontopProgram);

        /**
         * When the program is non-recursive, a topological order exists
         */
        List<Predicate> predicates = dg.getPredicatesInBottomUp();

        Set<OWLClass> classesInSignature = ontology.getClassesInSignature();

        for (OWLClass owlClass : classesInSignature) {
            predicates.add(DATA_FACTORY.getClassPredicate(owlClass.getIRI().toString()));
        }

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

        List<Predicate> predicatesToDefine = Lists.newArrayList(predicates);

        List<CQIE> newMappingRules = Lists.newArrayList();


        // --------------------------------------------


        Map<Predicate, List<Integer>> pkeys = DBMetadata.extractPKs(dbMetadata, mappingProgram);


        int i = 0;


        DatalogExpansion.init();

        for (Predicate predicate : predicatesToDefine) {

//            if(predicate.getName().equals("http://sws.ifi.uio.no/vocab/npd-v2#ProductionLicenceWorkObligation")){
//                System.out.println("catch it!");
//            }
            log.debug("compute mapping for {} ({}/{})", new Object[]{predicate, i, predicatesToDefine.size()});
            i++;

            List<OBDAMappingAxiom> newObdaMappingAxiomsForAPredicate = Lists.newArrayList();

            // Collection<CQIE> cqies = dg.getRuleIndex().get(predicate);


            // FIXME: DatalogExpansion only works for UOBM now!!

            List<CQIE> cqies = DatalogExpansion.expand("'" + predicate.getName() + "'", 1, 5);

            for (CQIE cqie : cqies) {


                CQIE query = generateTargetQuery(cqie);

                DatalogProgram queryProgram = DATA_FACTORY.getDatalogProgram(query);

                DatalogProgram queryAndMappingProgram = DATA_FACTORY.getDatalogProgram();
                //queryAndMappingProgram.appendRule(queryProgram.getRules());
                queryAndMappingProgram.appendRule(mappingProgram);


                DatalogUnfolder unfolder = new DatalogUnfolder(DATA_FACTORY.getDatalogProgram(mappingProgram), pkeys);


                /**
                 * Unfold the rules for the predicate w.r.t. the input mappings
                 */
                //DatalogProgram unfoldedQuery = unfolder.unfold(queryProgram, predicate.getName(), QuestConstants.BUP, true, multiTypedFunctionSymbolIndex);
                DatalogProgram unfoldedQuery = unfoldQueryWRTMappings(unfolder, queryProgram);


                List<CQIE> newMappings = unfoldedQuery.getRules();
                //mappingProgram.addAll(newMappings);

                newMappingRules.addAll(newMappings);
                // System.out.println(predicate);
                // System.out.println(unfolding);

//                newObdaMappingAxiomsForAPredicate.addAll(obdaMappingAxioms);
            }


            //newObdaMappingAxioms.addAll(newObdaMappingAxiomsForAPredicate);

        }

        /**
         * Unfolded query can already be translated tso OBDA Mapping
         */
        DatalogToMappingAxiomTranslater datalogToMappingAxiomTranslater = new DatalogToMappingAxiomTranslater(dbMetadata, obdaDataSource);
        List<OBDAMappingAxiom> newObdaMappingAxioms = datalogToMappingAxiomTranslater.translate(newMappingRules);


        //printOBDAMappingAxioms(newObdaMappingAxioms, obdaModel.getPrefixManager());


        OBDAModel extendedObdaModel = DATA_FACTORY.getOBDAModel();
        extendedObdaModel.addSource(obdaDataSource);

        // WE DO NOT NEED THE OLD MAPPINGS ANY MORE!!
        // extendedObdaModel.addMappings(obdaDataSource.getSourceID(), obdaModel.getMappings(obdaDataSource.getSourceID()));


        extendedObdaModel.addMappings(obdaDataSource.getSourceID(), newObdaMappingAxioms);
        extendedObdaModel.setPrefixManager(obdaModel.getPrefixManager());

        t2 = System.currentTimeMillis();

        System.err.println("#  new mappings " + newMappingRules.size());
        System.err.println("Mapping generation time: " + (t2 - t1) + "ms");


        return extendedObdaModel;
    }


    /**
     *
     * unfold the query w.r.t. to the mappings
     *
     * The logic is basically the same with unfold in DatalogUnfolder, but we do not need the step of
     * EQNormalizer.enforceEqualities. Therefore, we call  `computePartialEvaluationWRTMappings` directly using reflection
     *
     * @param unfolder
     * @param queryProgram
     * @return
     */
    private static DatalogProgram unfoldQueryWRTMappings(DatalogUnfolder unfolder, DatalogProgram queryProgram) {

        Multimap<Predicate, Integer> multiTypedFunctionSymbolIndex = ArrayListMultimap.create();

        List<CQIE> workingSet = new LinkedList<>();
        for (CQIE query : queryProgram.getRules())
            workingSet.add(QueryAnonymizer.deAnonymize(query));


        try {
            Method getMethod = DatalogUnfolder.class.getDeclaredMethod("computePartialEvaluationWRTMappings", List.class, Multimap.class);
            getMethod.setAccessible(true);
            Invokable invokable = Invokable.from(getMethod);
            invokable.invoke(unfolder, workingSet, multiTypedFunctionSymbolIndex);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        //unfolder.computePartialEvaluationWRTMappings(workingSet, multiTypedFunctionSymbolIndex);


        DatalogProgram unfoldedQuery = DATA_FACTORY.getDatalogProgram(workingSet);

        return unfoldedQuery;
    }

    public static CQIE generateTargetQuery(CQIE rule) {


        Function head = DATA_FACTORY.getFunction(CLASS_QUERY_HEAD_PREDICATE, rule.getHead().getTerms());

        return DATA_FACTORY.getCQIE(head, rule.getBody());

        //Function head = rule.getHead();
        // URITemplates.getUriTemplateString((Function) head.getTerm(0));
    }


    private static void printOBDAMappingAxioms(List<OBDAMappingAxiom> newObdaMappingAxiomsForAPredicate, PrefixManager prefixManager) {
        for (OBDAMappingAxiom m : newObdaMappingAxiomsForAPredicate) {
            printOBDAMapping(m, prefixManager);
            System.out.println();
        }
    }

    public static List<Predicate> collectHeadPredicates(List<CQIE> rules) {
        List<Predicate> headPredicates = Lists.newArrayList();
        for (CQIE rule : rules) {
            headPredicates.add(rule.getHead().getFunctionSymbol());
        }

        return headPredicates;
    }

    public static void printOBDAMapping(OBDAMappingAxiom mappingAxiom, PrefixManager prefixManager) {
        // ModelIOManager modelIOManager = new ModelIOManager();
        System.out.println("mappingId\t" + mappingAxiom.getId());
        System.out.println("target\t\t" + TargetQueryRenderer.encode(mappingAxiom.getTargetQuery(), prefixManager));
        System.out.println("source\t\t" + SourceQueryRenderer.encode(mappingAxiom.getSourceQuery()).replace("\n", " "));
    }

}
