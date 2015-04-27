package org.semanticweb.ontop.clipper;

import com.beust.jcommander.internal.Lists;


import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.ValueConstant;
import it.unibz.krdb.obda.model.Variable;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import org.semanticweb.clipper.hornshiq.rule.Atom;
import org.semanticweb.clipper.hornshiq.rule.CQ;

import org.semanticweb.clipper.hornshiq.rule.DLPredicate;
import org.semanticweb.clipper.hornshiq.rule.Predicate;
import org.semanticweb.clipper.hornshiq.rule.Term;

import org.semanticweb.owlapi.model.OWLEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ClipperRuleToOntopRuleTranslator {

    static OBDADataFactory dataFactory = OBDADataFactoryImpl.getInstance();

    public static DatalogProgram translate(List<CQ> cqs) {
        List<CQIE> cqies = Lists.newArrayList();
        for (CQ cq:cqs){
            cqies.add(translate(cq));
        }
        DatalogProgram program = dataFactory.getDatalogProgram(cqies);
        return program;
    }

    public static CQIE translate(CQ cq) {
        Function ontopHead = translate(cq.getHead());
        List<Function> ontopBody = new ArrayList<>();
        for(Atom b : cq.getBody()){
            ontopBody.add(translate(b));
        }

        return dataFactory.getCQIE(ontopHead, ontopBody);
    }

    private static Function translate(Atom atom) {
        Function function;
        Predicate predicate = atom.getPredicate();
        it.unibz.krdb.obda.model.Predicate ontopPredicate = translate(predicate);
        List<it.unibz.krdb.obda.model.Term> ontopTerms = new ArrayList<>();

        for (Term term : atom.getTerms()) {
            ontopTerms.add(translate(term));
        }

        return dataFactory.getFunction(ontopPredicate, ontopTerms);

    }

    private static it.unibz.krdb.obda.model.Term translate(Term term) {
        if (term.isVariable()) {
            Variable variable = dataFactory.getVariable(term.getName());
            return variable;
        } else if (term.isConstant()) {
            ValueConstant literal = dataFactory.getConstantLiteral(term.getName());
            return literal;
        } else {
            throw new IllegalArgumentException("cannot translate term " + term.toString());
        }
    }

    private static it.unibz.krdb.obda.model.Predicate translate(Predicate predicate) {
        String name = predicate.getName();

        if(predicate.isDLPredicate()){
            OWLEntity owlEntity = ((DLPredicate) predicate).getOwlEntity();
            name = owlEntity.getIRI().toString();
        }

        it.unibz.krdb.obda.model.Predicate ontopPredicate = null;
        int arity = predicate.getArity();
        if (arity == 1) {
            ontopPredicate = dataFactory.getClassPredicate(name);
        } else if(arity == 2) {
            ontopPredicate = dataFactory.getObjectPropertyPredicate(name);
            // TODO: how to deal with data properties ???
        } else {
            throw new IllegalArgumentException(predicate.toString());
        }
        return ontopPredicate;
    }


}
