package expansion;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import org.jpl7.Atom;
import org.jpl7.Compound;
import org.jpl7.Term;
import org.jpl7.Variable;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.OBDADataFactory;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.impl.OBDADataFactoryImpl;

import java.util.List;

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
            // TOOD: view(...)
            try{
                bodyAtoms.add(translate((Compound)bodyAtom.arg(1)));
            }catch (Exception e){
                System.out.println();
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

        List<org.semanticweb.ontop.model.Term> args = Lists.newArrayList(arity);

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


    public static org.semanticweb.ontop.model.Variable translate(Variable v){
        return DATA_FACOTRY.getVariable(v.name());
    }

    public static org.semanticweb.ontop.model.ValueConstant translate(Atom v){
        // TODO check the types of atom v
        return DATA_FACOTRY.getConstantLiteral(v.name());
    }



}
