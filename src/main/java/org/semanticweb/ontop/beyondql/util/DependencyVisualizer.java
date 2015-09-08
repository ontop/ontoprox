package org.semanticweb.ontop.beyondql.util;

import com.google.common.collect.ImmutableSet;
import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.organic.JGraphFastOrganicLayout;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.Predicate;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.ontop.beyondql.hornshiq.ClipperRuleToOntopRuleTranslator;
import org.semanticweb.ontop.beyondql.mapgen.DatalogDependencyGraphGenerator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import javax.swing.*;
import java.io.File;
import java.util.List;

public class DependencyVisualizer {

    public static void main(String[] args) throws OWLOntologyCreationException, InterruptedException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File("src/test/resources/npd/npd-v2.owl"));
        visualize(ontology);

    }

    public static void visualize(OWLOntology ontology) throws InterruptedException {

        long t1 = System.currentTimeMillis();

        /** create a Clipper Reasoner instance */
        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();

        /** feed the ontology to Clipper reasoner */
        qaHornSHIQ.setOntologies(ImmutableSet.of(ontology));

        /** rewrite the ontology to a datalog program represented in Clipper Native API */
        List<CQ> program = qaHornSHIQ.rewriteOntology();

        /** convert the datalog program to Ontop representation using Ontop API*/
        DatalogProgram ontopProgram = ClipperRuleToOntopRuleTranslator.translate(program);

        DatalogDependencyGraphGenerator gen = new DatalogDependencyGraphGenerator(ontopProgram);
        DirectedGraph<Predicate, DefaultEdge> g = gen.getPredicateDependencyGraph();

        JFrame frame = new JFrame();

        frame.setSize(400, 400);


        JGraph jgraph = new JGraph(new JGraphModelAdapter<>(g));

        // Let's see if we can lay it out
        JGraphFacade jgf = new JGraphFacade(jgraph);
        JGraphFastOrganicLayout layoutifier = new JGraphFastOrganicLayout();
        layoutifier.run(jgf);

        frame.getContentPane().add(jgraph);
        frame.setVisible(true);
        while (true) Thread.sleep(2000);
    }
}