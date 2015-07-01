package expansion;


import com.google.common.base.Joiner;
import jpl.Term;

import java.util.List;

public class DatalogRule {

    private final List<Term> body;
    private final Term head;

    public DatalogRule(List<Term> terms){
        this.head = terms.get(0);
        this.body = terms.subList(1,terms.size());
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(head);
        if(body.size() > 0) {
            sb.append(" :- ");
            Joiner.on(", ").appendTo(sb, body);
        }
        sb.append(".");
        return sb.toString();
    }
}
