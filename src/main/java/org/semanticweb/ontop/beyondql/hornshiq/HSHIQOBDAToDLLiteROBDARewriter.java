package org.semanticweb.ontop.beyondql.hornshiq;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import it.unibz.krdb.obda.exception.DuplicateMappingException;
import it.unibz.krdb.obda.exception.InvalidMappingException;
import it.unibz.krdb.obda.io.ModelIOManager;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.OBDAException;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Variable;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.owlrefplatform.core.unfolding.DatalogUnfolder;
import it.unibz.krdb.obda.utils.Mapping2DatalogConverter;
import it.unibz.krdb.sql.DBMetadata;
import it.unibz.krdb.sql.JDBCConnectionManager;

import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.ontop.beyondql.approximation.ConjunctionNormalizer;
import org.semanticweb.ontop.beyondql.approximation.DLLiteRClosureBuilder;
import org.semanticweb.ontop.beyondql.approximation.EmptyConjunctionRemover;
import org.semanticweb.ontop.beyondql.approximation.IRIUtils;
import org.semanticweb.ontop.beyondql.approximation.OntologyTransformer;
import org.semanticweb.ontop.beyondql.approximation.QualifiedExistentialNormalizer;
import org.semanticweb.ontop.beyondql.datalogexpansion.DatalogExpansion;
import org.semanticweb.ontop.beyondql.mapgen.DatalogToMappingAxiomTranslater;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * This is the main class for rewriting/approximating a OBDA setting (T,M) where T is in Horn-SHIQ to another (hopefully
 * equivalent) OBDA setting (T', M')
 *
 */
public class HSHIQOBDAToDLLiteROBDARewriter {

    private static OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    private static Logger log = LoggerFactory.getLogger(HSHIQOBDAToDLLiteROBDARewriter.class);

    private static Variable X = DATA_FACTORY.getVariable("X");

    private final String tempPrologFile;

    private final int depth;

    private OWLOntologyManager manager;

    private OWLOntology ontology;
    private final OBDAModel obdaModel;

    private OWLOntology rewrittenOntology;

    private OBDAModel rewrittenOBDAModel;

    private Multimap<OWLClass, OWLClass> newConceptsForConjunctions = ArrayListMultimap.create();

    public OBDAModel getRewrittenOBDAModel() {
        return rewrittenOBDAModel;
    }

    public OWLOntology getRewrittenOntology() {
        return rewrittenOntology;
    }


    public HSHIQOBDAToDLLiteROBDARewriter(String ontologyFile, String obdaFile, int depth){
        int i = ontologyFile.lastIndexOf(".");

        this.tempPrologFile = ontologyFile.substring(0, i) + ".pl";
        this.depth = depth;

        try {
            this.manager = OWLManager.createOWLOntologyManager();
            this.ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFile));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }

        /**
         * load the mappings using Ontop API
         */
        this.obdaModel = DATA_FACTORY.getOBDAModel();
        ModelIOManager ioManager = new ModelIOManager(obdaModel);
        File obdafile = new File(obdaFile);
        try {
            ioManager.load(obdafile);
        } catch (IOException | InvalidMappingException e) {
            e.printStackTrace();
        }

    }

    public void rewrite() throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException,
            OBDAException, DuplicateMappingException, OWLOntologyStorageException {
        rewrite(ontology, obdaModel, tempPrologFile, depth);

    }

    private OBDAModel rewrite(OWLOntology ontology, OBDAModel obdaModel, String tempPrologFile, int depth)
            throws SQLException, OBDAException, DuplicateMappingException, OWLOntologyCreationException, IOException, OWLOntologyStorageException {

        long t1 = System.currentTimeMillis();

        /** create a Clipper Reasoner instance */
        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();

        /** feed the ontology to Clipper reasoner */
        qaHornSHIQ.setOntologies(ImmutableSet.of(ontology));

        /** rewrite the ontology to a datalog program represented in Clipper Native API */
        List<CQ> program = qaHornSHIQ.rewriteOntology();

        String originalIRI = ontology.getOntologyID().getOntologyIRI().toString();

        OWLOntology owlOntology_step1 = qaHornSHIQ.exportNormalizedAxiomsAndSaturatedEnforceRelations(originalIRI + "_step1");

        this.rewrittenOntology = rewriteOntology(owlOntology_step1);

        /** convert the datalog program to Ontop representation using Ontop API*/
        DatalogProgram ontopProgram = ClipperRuleToOntopRuleTranslator.translate(program);

        List<CQIE> rulesForNewConceptsForConjunctions = getRulesForNewConceptsForConjunctions(newConceptsForConjunctions);

        ontopProgram.appendRule(rulesForNewConceptsForConjunctions);

        long t2 = System.currentTimeMillis();

        System.err.println("Datalog generation time: " + (t2 - t1) + "ms");

        //log.debug("translate program from Clipper {}", ontopProgram);

        OBDAModel extendedObdaModel = rewriteMappings(ontology, obdaModel, tempPrologFile, newConceptsForConjunctions, ontopProgram, depth);

        this.rewrittenOBDAModel = extendedObdaModel;

        return extendedObdaModel;
    }

    private static OBDAModel rewriteMappings(OWLOntology ontology, OBDAModel obdaModel, String tempPrologFile,
                                             Multimap<OWLClass, OWLClass> newConceptsForConjunctions, DatalogProgram ontopProgram, int depth)
            throws SQLException, OBDAException, DuplicateMappingException, IOException {
        long t3 = System.currentTimeMillis();

        /**
         *
         */
        List<Predicate> predicatesToDefine = getDeclaredPredicates(ontology);
        predicatesToDefine.addAll(getDeclaredPredicates(newConceptsForConjunctions));

        /**
         * We assume we are in Virtual mode and therefore we only have one data source.
         */
        OBDADataSource obdaDataSource = obdaModel.getSources().iterator().next();
        ArrayList<OBDAMappingAxiom> mappingAxioms = obdaModel.getMappings(obdaDataSource.getSourceID());

        DBMetadata dbMetadata = JDBCConnectionManager.getJDBCConnectionManager().getMetaData(obdaDataSource);

        //Mapping2DatalogConverter mapping2DatalogConverter = new Mapping2DatalogConverter();

        /**
         * convert the mappings into a set of rules
         */
        List<CQIE> mappingProgram = Mapping2DatalogConverter.constructDatalogProgram(mappingAxioms, dbMetadata);


        List<CQIE> newMappingRules = Lists.newArrayList();

        Map<Predicate, List<Integer>> pkeys = DBMetadata.extractPKs(dbMetadata, mappingProgram);

        int i = 0;

        DatalogExpansion datalogExpansion = new DatalogExpansion(ontopProgram, obdaModel, tempPrologFile);

        for (Predicate predicate : predicatesToDefine) {

            log.debug("compute mapping for {} ({}/{})", new Object[]{predicate, i, predicatesToDefine.size()});
            i++;

            List<CQIE> cqies = datalogExpansion.expand(String.format("'%s'", predicate.getName()), predicate.getArity(), depth);

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
         * Unfolded query can already be translated to OBDA Mapping
         */
        DatalogToMappingAxiomTranslater datalogToMappingAxiomTranslater = new DatalogToMappingAxiomTranslater(dbMetadata, obdaDataSource);
        List<OBDAMappingAxiom> newObdaMappingAxioms = datalogToMappingAxiomTranslater.translate(newMappingRules);


        OBDAModel extendedObdaModel = DATA_FACTORY.getOBDAModel();
        extendedObdaModel.addSource(obdaDataSource);

        extendedObdaModel.addMappings(obdaDataSource.getSourceID(), newObdaMappingAxioms);
        extendedObdaModel.setPrefixManager(obdaModel.getPrefixManager());

        long t4 = System.currentTimeMillis();

        System.err.println("# new mappings " + newMappingRules.size());
        System.err.println("Mapping generation time: " + (t4 - t3) + "ms");
        return extendedObdaModel;
    }

    private static List<CQIE> getRulesForNewConceptsForConjunctions(Multimap<OWLClass, OWLClass> newConceptsForConjunctions) {

        List<CQIE> rules = Lists.newArrayList();

        for (OWLClass entry : newConceptsForConjunctions.keys()) {

            Predicate predicate = DATA_FACTORY.getClassPredicate(entry.getIRI().toString());

            Collection<OWLClass> owlClasses = newConceptsForConjunctions.get(entry);

            Function head = DATA_FACTORY.getFunction(predicate, X);

            List<Function> body = Lists.newArrayList();

            for (OWLClass owlClass : owlClasses) {
                Predicate bodyPredicate = DATA_FACTORY.getClassPredicate(owlClass.getIRI().toString());
                body.add(DATA_FACTORY.getFunction(bodyPredicate, X));
            }

            rules.add(DATA_FACTORY.getCQIE(head, body));
        }

        return rules;
    }

    private static List<Predicate> getDeclaredPredicates(OWLOntology ontology) {
        List<Predicate> predicatesToDefine = Lists.newArrayList();

        for (OWLClass owlClass : ontology.getClassesInSignature(true)) {
            predicatesToDefine.add(DATA_FACTORY.getClassPredicate(owlClass.getIRI().toString()));
        }

        for (OWLObjectProperty owlObjectProperty : ontology.getObjectPropertiesInSignature(true)) {
            predicatesToDefine.add(DATA_FACTORY.getObjectPropertyPredicate(owlObjectProperty.getIRI().toString()));
        }

        for (OWLDataProperty owlDatatypeProperty : ontology.getDataPropertiesInSignature(true)) {
            predicatesToDefine.add(DATA_FACTORY.getDataPropertyPredicate(owlDatatypeProperty.getIRI().toString()));
        }
        return predicatesToDefine;
    }

    private static List<Predicate> getDeclaredPredicates(Multimap<OWLClass, OWLClass> newConceptsForConjunctions) {

        List<Predicate> predicates = Lists.newArrayList();

        for (OWLClass entry : newConceptsForConjunctions.keys()) {
            Predicate predicate = DATA_FACTORY.getClassPredicate(entry.getIRI().toString());
            predicates.add(predicate);
        }

        return predicates;
    }

    private OWLOntology rewriteOntology(OWLOntology ont) throws OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {

        IRI iri_dllite_ont1_opt = IRIUtils.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step1_opt");
        IRI iri_dllite_ont2 = IRIUtils.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step2");
        IRI iri_dllite_ont3 = IRIUtils.createIRIWithSuffix(ont.getOntologyID().getOntologyIRI(), "step3");
        IRI iri_dllite_ont4 = IRIUtils.createIRIWithSuffix(this.ontology.getOntologyID().getOntologyIRI(), "step4");

		// intermediate optimization step
		EmptyConjunctionRemover emptyRemover = new EmptyConjunctionRemover(manager);
		OWLOntology ont1opt = emptyRemover.transform(ont, iri_dllite_ont1_opt);


        // step 2
        QualifiedExistentialNormalizer dlliterNormalizer = new QualifiedExistentialNormalizer(manager);
        OWLOntology ont2 = dlliterNormalizer.transform(ont1opt, iri_dllite_ont2);
        //manager.saveOntology(ont2, new FileOutputStream(file_iri_dllite_ont2.toString()));

        // step 3
        ConjunctionNormalizer conjNormalizer = new ConjunctionNormalizer(manager);
        OWLOntology ont3 = conjNormalizer.transform(ont2, iri_dllite_ont3);

        this.newConceptsForConjunctions = conjNormalizer.getNewConceptsForConjunctions();

        // step 4
        DLLiteRClosureBuilder dlliterClosure = new DLLiteRClosureBuilder(manager);
        OWLOntology ont4 = dlliterClosure.transform(ont3, iri_dllite_ont4);
        return ont4;
    }


}
