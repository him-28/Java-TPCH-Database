package optimizer;

import java.util.ArrayList;

/**
 * Condition (sub clause separated by "and" in where clause) holder
 * @author jz33
 *
 */
public class Condition {
	
	/**
	 * Sub clause separated by and in where clause
	 */
	private String cond = "";
	
	/**
	 * The condition's type. Notice that the type cannot be "and"
	 */
	private String type = "";
	private ArrayList<String> abbrList; //The abbrs in the condition
	private ArrayList<String> attrList; //The attrs in the condition
	private Condition lCond; //Left sub cond
	private Condition rCond; //Right sub cond
	private boolean isNot = false; //If the condition has !

	public Condition(String cond){
		this.cond = cond;
		this.type = "";
		this.abbrList = new ArrayList<String>();
		this.attrList = new ArrayList<String>();
	}
	
	public Condition (String cond, String type) {
		this.cond = cond;
		this.type = type;
		this.abbrList = new ArrayList<String>();
		this.attrList = new ArrayList<String>();
	}
	
	public String toString(){
		return  "(cond: " + cond +
				", type: " + type +
				", abbrlist: "+ abbrList+
				", attrlist: "+ attrList+
				")";
	}
	
	public String getCond(){return this.cond;}
	public String getType(){return this.type;}
	public ArrayList<String> getAbbrList(){return this.abbrList;}
	public ArrayList<String> getAttrList(){return this.attrList;}
	public Condition getLCond(){return this.lCond;}
    public Condition getRCond(){return this.rCond;}
    public boolean getIsNot(){return this.isNot;}

    public void setCond(String cond) {this.cond=cond;}
	public void setType(String type) {this.type=type;}
    public void setLCond(Condition lCond){this.lCond = lCond;}
    public void setRCond(Condition rCond){this.rCond = rCond;}
    public void setIsNot(boolean isNot){this.isNot = isNot;}
}