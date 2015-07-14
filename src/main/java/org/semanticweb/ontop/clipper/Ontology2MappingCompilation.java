package org.semanticweb.ontop.clipper;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.reflect.Invokable;
import expansion.DatalogExpansion;
import it.unibz.krdb.obda.model.Function;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import it.unibz.krdb.obda.exception.DuplicateMappingException;
import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.io.PrefixManager;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.RDBMSourceParameterConstants;
import it.unibz.krdb.obda.owlrefplatform.core.QuestConstants;
import it.unibz.krdb.obda.renderer.SourceQueryRenderer;
import it.unibz.krdb.obda.renderer.TargetQueryRenderer;
import it.unibz.krdb.sql.DBMetadata;
import it.unibz.krdb.sql.JDBCConnectionManager;
import it.unibz.krdb.obda.utils.Mapping2DatalogConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import it.unibz.krdb.obda.owlrefplatform.core.unfolding.DatalogUnfolder;
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
         *
         */
        List<Predicate> predicatesToDefine = getDeclaredPredicates(ontology);

        /**
         * We assume we are in Virtual mode and therefore we only have one data source.
         */
        OBDADataSource obdaDataSource = obdaModel.getSources().iterator().next();
        ArrayList<OBDAMappingAxiom> mappingAxioms = obdaModel.getMappings(obdaDataSource.getSourceID());

        DBMetadata dbMetadata = JDBCConnectionManager.getJDBCConnectionManager().getMetaData(obdaDataSource);
        Mapping2DatalogConverter mapping2DatalogConverter = new Mapping2DatalogConverter();

        /**
         * convert the mappings into a set of rules
         */
        List<CQIE> mappingProgram = mapping2DatalogConverter.constructDatalogProgram(mappingAxioms, dbMetadata);


        List<CQIE> newMappingRules = Lists.newArrayList();

        Map<Predicate, List<Integer>> pkeys = DBMetadata.extractPKs(dbMetadata, mappingProgram);

        int i = 0;

        DatalogExpansion.init();

        for (Predicate predicate : predicatesToDefine) {

            log.debug("compute mapping for {} ({}/{})", new Object[]{predicate, i, predicatesToDefine.size()});
            i++;

            // FIXME: DatalogExpansion only works for UOBM now!!

            int depth = 5;

            List<CQIE> cqies = DatalogExpansion.expand("'" + predicate.getName() + "'", predicate.getArity(), depth);

            for (CQIE cqie : cqies) {

                DatalogProgram queryProgram = DATA_FACTORY.getDatalogProgram(Lists.newArrayList(cqie));

                DatalogProgram queryAndMappingProgram = DATA_FACTORY.getDatalogProgram();
                queryAndMappingProgram.appendRule(mappingProgram);

                DatalogUnfolder unfolder = new DatalogUnfolder(mappingProgram, pkeys);

                /**
                 * Unfold the rules for the predicate w.r.t. the input mappings
                 */
                DatalogProgram unfoldedQuery = unfolder.unfold(queryProgram, null);

                List<CQIE> newMappings = unfoldedQuery.getRules();

                newMappingRules.addAll(newMappings);
            }

        }

        /**
         * Unfolded query can already be translated tso OBDA Mapping
         */
        DatalogToMappingAxiomTranslater datalogToMappingAxiomTranslater = new DatalogToMappingAxiomTranslater(dbMetadata, obdaDataSource);
        List<OBDAMappingAxiom> newObdaMappingAxioms = datalogToMappingAxiomTranslater.translate(newMappingRules);


        OBDAModel extendedObdaModel = DATA_FACTORY.getOBDAModel();
        extendedObdaModel.addSource(obdaDataSource);

        extendedObdaModel.addMappings(obdaDataSource.getSourceID(), newObdaMappingAxioms);
        extendedObdaModel.setPrefixManager(obdaModel.getPrefixManager());

        t2 = System.currentTimeMillis();

        System.err.println("#  new mappings " + newMappingRules.size());
        System.err.println("Mapping generation time: " + (t2 - t1) + "ms");


        return extendedObdaModel;
    }

    private static List<Predicate> getDeclaredPredicates(OWLOntology ontology) {
        List<Predicate> predicatesToDefine = Lists.newArrayList();

        for (OWLClass owlClass : ontology.getClassesInSignature()) {
            predicatesToDefine.add(DATA_FACTORY.getClassPredicate(owlClass.getIRI().toString()));
        }

        for (OWLObjectProperty owlObjectProperty : ontology.getObjectPropertiesInSignature()) {
            predicatesToDefine.add(DATA_FACTORY.getObjectPropertyPredicate(owlObjectProperty.getIRI().toString()));
        }

        for (OWLObjectProperty owlDatatypeProperty : ontology.getObjectPropertiesInSignature()) {
            predicatesToDefine.add(DATA_FACTORY.getDataPropertyPredicate(owlDatatypeProperty.getIRI().toString()));
        }
        return predicatesToDefine;
    }


}
