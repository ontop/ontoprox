package org.semanticweb.ontop.clipper;


import com.google.common.collect.ImmutableSet;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.semanticweb.clipper.hornshiq.queryanswering.QAHornSHIQ;
import org.semanticweb.clipper.hornshiq.rule.CQ;
import org.semanticweb.ontop.model.DatalogProgram;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.utils.DatalogDependencyGraphGenerator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.util.List;


public class NPDTest {
    public static void main(String[] args) throws OWLOntologyCreationException {

        QAHornSHIQ qaHornSHIQ = new QAHornSHIQ();

        String ontologyFile =  "/Users/xiao/npd-v2.owl";

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

    }


}
