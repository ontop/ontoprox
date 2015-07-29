package org.semanticweb.ontop.beyondql.datalogexpansion;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.Function;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;


import java.util.List;

/**
 * An internal SWI-Prolog representation of Datalog rule.
 *
 * It can be converted to Ontop Datalog rule using {@code toCQIE()} method.
 */
public class DatalogRule {

    static OBDADataFactory DATA_FACOTRY = OBDADataFactoryImpl.getInstance();

    private final Term[] body;
    private final Term head;

//    public DatalogRule(List<Term> terms){
//        this.head = terms.get(0);
//        this.body = terms.subList(1,terms.size());
//    }

    public DatalogRule(Term ruleTerm) {

        if (!(ruleTerm instanceof Compound))
            throw new IllegalArgumentException("is not a pair");

        Compound compound = (Compound) ruleTerm;

        this.head = compound.arg(1);
        this.body = compound.arg(2).toTermArray();
    }

    public CQIE toCQIE(){

        Function head = translate((Compound) this.head);

        List<Function> bodyAtoms = Lists.newArrayList();

        for (Term bodyAtom : body) {
            try {
                bodyAtoms.add(translate((Compound)bodyAtom.arg(1)));
            } catch (Exception e){
                e.printStackTrace();
            }

        }

        CQIE cqie = DATA_FACOTRY.getCQIE(head, bodyAtoms);
        //DATA_FACOTRY.getFunction(((Compound)head).name())


        return cqie;
    }

    private Function translate(Compound compound) {
        String name = compound.name();
        int arity = compound.arity();

        // TODO: switch (arity)
        Predicate predicate = DATA_FACOTRY.getPredicate(name, arity);

        List<it.unibz.krdb.obda.model.Term> args = Lists.newArrayList(arity);

        for (Term term : compound.args()) {
            if(term.isVariable()){
                args.add(translate((Variable)term));
            } else if(term.isAtom()){
                args.add(translate((Atom)term));
            } else {
                throw new IllegalArgumentException("");
            }
        }

        return DATA_FACOTRY.getFunction(predicate, args);
    }


    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(head);
        if(body.length > 0) {
            sb.append(" :- ");
            Joiner.on(", ").appendTo(sb, body);
        }
        sb.append(".");
        return sb.toString();
    }


    public static it.unibz.krdb.obda.model.Variable translate(Variable v){
        return DATA_FACOTRY.getVariable(v.name());
    }

    public static it.unibz.krdb.obda.model.ValueConstant translate(Atom v){
        // TODO check the types of atom v
        return DATA_FACOTRY.getConstantLiteral(v.name());
    }



}
