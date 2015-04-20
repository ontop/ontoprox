package org.semanticweb.ontop.clipper;


import com.beust.jcommander.internal.Lists;


import org.semanticweb.clipper.hornshiq.rule.Atom;
import org.semanticweb.clipper.hornshiq.rule.CQ;

import org.semanticweb.clipper.hornshiq.rule.DLPredicate;
import org.semanticweb.clipper.hornshiq.rule.Predicate;
import org.semanticweb.clipper.hornshiq.rule.Term;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.DatalogProgram;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.ValueConstant;
import org.semanticweb.ontop.model.Variable;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;
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
        org.semanticweb.ontop.model.Predicate ontopPredicate = translate(predicate);
        List<org.semanticweb.ontop.model.Term> ontopTerms = new ArrayList<>();

        for (Term term : atom.getTerms()) {
            ontopTerms.add(translate(term));
        }

        return dataFactory.getFunction(ontopPredicate, ontopTerms);

    }

    private static org.semanticweb.ontop.model.Term translate(Term term) {
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

    private static org.semanticweb.ontop.model.Predicate translate(Predicate predicate) {
        String name = predicate.getName();

        if(predicate.isDLPredicate()){
            OWLEntity owlEntity = ((DLPredicate) predicate).getOwlEntity();
            name = owlEntity.getIRI().toString();
        }

        org.semanticweb.ontop.model.Predicate ontopPredicate = null;
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
