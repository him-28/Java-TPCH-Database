package runner;

/**
 * Note that it is assumed that funcToRun is one of "none", "avg", or "sum"
 * @author cmj4, jz33
 */
public class AggFunc {
 
  private String type;
  private String selection;
  
  public String getFuncToRun () {
    return type; 
  }
  
  public String getExpr () {
    return selection;
  }
  
  public AggFunc (String inFuncToRun, String inExpr) {
    type = inFuncToRun;
    selection = inExpr;
  }
  
  public String toString(){
	  return "(type: "+this.type+" selection: "+this.selection+")";
  }
}