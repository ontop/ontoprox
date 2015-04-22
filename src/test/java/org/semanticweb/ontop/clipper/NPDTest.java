package org.semanticweb.ontop.clipper;


import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;

import com.google.common.collect.Multimap;
import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.ontop.exception.InvalidMappingException;
import org.semanticweb.ontop.io.ModelIOManager;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.DatalogProgram;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.OBDADataSource;
import org.semanticweb.ontop.model.OBDAModel;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
import org.semanticweb.ontop.owlrefplatform.core.QuestConstants;
import org.semanticweb.ontop.owlrefplatform.core.unfolding.DatalogUnfolder;
import org.semanticweb.ontop.sql.DBMetadata;
import org.semanticweb.ontop.sql.JDBCConnectionManager;
import org.semanticweb.ontop.utils.DatalogDependencyGraphGenerator;
import org.semanticweb.ontop.utils.Mapping2DatalogConverter;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;


public class NPDTest {
    public static void main(String[] args) throws OWLOntologyCreationException, IOException, InvalidMappingException, SQLException {

        String ontologyFile =  "src/test/resources/npd-v2.owl";
        String obdaFile = "src/test/resources/npd-v2-ql_a_postgres.obda";

        OntologyMappingCompilation.compileHSHIQtoMappings(ontologyFile, obdaFile);
    }



}
