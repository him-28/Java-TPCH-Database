package optimizer;

import java.util.ArrayList;
import runner.Attribute;;

/**
 * A modified table class holding the catalog table infos
 * Table at from clause is named like: partsupp as ps
 * @author jz33
 */
public class Table {
	
	/**
	 * Table's name, e.g., partsupp, without ".tbl"
	 */
	private String tablename = "";
	private ArrayList<String> abbrList; // Abbreviate list
	private ArrayList<Attribute> attrList; // Attribute list
	
	public Table (String tablename) {
		this.tablename = tablename;
		this.abbrList = new ArrayList<String>();
		this.attrList = new ArrayList<Attribute>();
	}
	
	public void free(){
		this.tablename = "";
		this.abbrList.clear();
		this.attrList.clear();
	}
	
	public String toString(){
		return "(tablename = " + tablename + 
				", abbrList = "+ abbrList+ 
				//", attrList = "+ attrList+
				" )";
	}
	
	public String creatOutFileName(){
		return "_T_"+this.abbrList.toString();
	}
	
	public String getTableName(){return this.tablename;}
	public ArrayList<String> getAbbrList(){return this.abbrList;}
	public ArrayList<Attribute> getAttrList(){return this.attrList;}
	
	public void setAbbrList(ArrayList<String> abbrList){this.abbrList=abbrList;}
	public void setAttrList(ArrayList<Attribute> attrList){this.attrList=attrList;}
}
