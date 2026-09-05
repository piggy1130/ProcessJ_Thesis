package ast;

import utilities.Visitor;
import utilities.Error;

public class AltCase extends AST {
    /*AltCases known which number they are in the list as they will
      be assigned one in the case statement they will eventually go into.*/
    private int caseNumber = -1;
    //TODO I actually don't know if we need this...
    /*If this is a Timeout Stat this string will hold the name of what the
      the temp Time should be called.*/
    private String tempTimerName = null;
    public boolean isAltStat = false;
    
    /* Precondition and priority can be null. */
    public AltCase(Expression expr, Guard guard, Statement stat) {
        this(expr, guard, stat, null);
    }

    public AltCase(
            Expression expr,
            Guard guard,
            Statement stat,
            Expression priority) {
        super(guard);
        nchildren = 4;
        children = new AST[] { expr, guard, stat, priority };
    }

    /* An alt case can be another alt. */
    public AltCase(AltStat stat) {
        super(stat);
        nchildren = 4;
        isAltStat = true;
        children = new AST[] { null, null, stat, null };
    }
    
    public Expression precondition() {
        return (Expression) children[0];
    }

    public Guard guard() {
        return (Guard) children[1];
    }

    public Statement stat() {
        return (Statement) children[2];
    }

    public Expression priority() {
        return (Expression) children[3];
    }


    public boolean isAltStat() {
	return isAltStat;
    }

    
    public <S extends Object> S visit(Visitor<S> v) {
        return v.visitAltCase(this);
    }

    public int getCaseNumber() {
        if (caseNumber == -1)
            Error.error("AltCase Error: The caseNumber for this AltCase was never set!");

        return caseNumber;
    }

    public void setCaseNumber(int n) {
        caseNumber = n;

        return;
    }

    public String getTempTimerName() {
        if (tempTimerName == null)
            Error.error("AltCase Error: The tempTimerName for this AltCase was never set!");

        return tempTimerName;
    }

    public void setTempTimerName(String str) {
        tempTimerName = str;

        return;
    }

}
