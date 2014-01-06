package optimizer;

import java.util.*;
import runner.Attribute;

/**
 * Query Node
 * @author jz33
 *
 */
public class Node {
	
	private Node lNode; // left node
	private Node rNode; // right node
	private Node pNode; // parent node
	
	private boolean ifLeaf = false; // if the node is leaf
    private boolean ifRoot = false; // if the node is root
	private boolean ifJoin = false; // if the node is going to perform join
	private boolean ifUniq = false; // if there is only 1 node (only 1 table in from clause), then 2 nodes
	
	private ArrayList<Condition> cond_list;//Default
	private ArrayList<String> requiredAtts;//Default
	private ArrayList<Attribute> selectedAtts;//For root node only
	private Map<String,String> exprs; //For root node only
	private ArrayList<String> abbrList; //For non-leaf nodes only
	private Table table;//For leaf node only

	public Node(boolean ifRoot, boolean ifLeaf){
		this.ifRoot = ifRoot;
		this.ifLeaf = ifLeaf;
		
		this.cond_list = new ArrayList<Condition>();
		this.requiredAtts = new ArrayList<String>();
		
		if(!this.ifLeaf) this.abbrList = new ArrayList<String>();
	}
	
	public String toString(){
		if(this.isIfLeaf())
			return  "ifLeaf: "+this.ifLeaf+"\n"+
					"cond_list: "+this.cond_list+"\n"+
					"selectedAtts: "+this.selectedAtts+"\n"+
					"requiredAtts: "+this.requiredAtts+"\n"+
					"exprs: "+this.exprs+"\n"+
					"table: "+this.table;
		else
			return  "ifLeaf: "+this.ifLeaf+"\n"+
					"cond_list: "+this.cond_list+"\n"+
					"selectedAtts: "+this.selectedAtts+"\n"+
					"requiredAtts: "+this.requiredAtts+"\n"+
					"exprs: "+this.exprs+"\n"+
					"abbrList:" +this.abbrList;
	}

	public boolean isIfLeaf() {
		return ifLeaf;
	}

	public void setIfLeaf(boolean ifLeaf) {
		this.ifLeaf = ifLeaf;
	}

	public boolean isIfRoot() {
		return ifRoot;
	}

	public void setIfRoot(boolean ifRoot) {
		this.ifRoot = ifRoot;
	}

	public boolean isIfJoin() {
		return ifJoin;
	}

	public void setIfJoin(boolean ifJoin) {
		this.ifJoin = ifJoin;
	}
	
	public boolean isIfUnique() {
		return ifUniq;
	}

	public void setIfUnique(boolean ifUniq) {
		this.ifUniq = ifUniq;
	}

	public Node getlNode() {
		return lNode;
	}

	public void setlNode(Node lNode) {
		this.lNode = lNode;
	}

	public Node getrNode() {
		return rNode;
	}

	public void setrNode(Node rNode) {
		this.rNode = rNode;
	}

	public Node getpNode() {
		return pNode;
	}

	public void setpNode(Node pNode) {
		this.pNode = pNode;
	}

	public ArrayList<Condition> getCond_list() {
		return cond_list;
	}

	public void setCond_list(ArrayList<Condition> cond_list) {
		this.cond_list = cond_list;
	}

	public ArrayList<Attribute> getSelectedAtts() {
		return selectedAtts;
	}

	public void setSelectedAtts(ArrayList<Attribute> outAtts) {
		this.selectedAtts = outAtts;
	}

	public Map<String,String> getExprs() {
		return exprs;
	}

	public void setExprs(Map<String,String> expr_dict) {
		this.exprs = expr_dict;
	}

	public ArrayList<String> getAbbrList() {
		return abbrList;
	}

	public void setAbbrList(ArrayList<String> abbrList) {
		this.abbrList = abbrList;
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public ArrayList<String> getRequiredAtts() {
		return requiredAtts;
	}

	public void setRequiredAtts(ArrayList<String> requiredAtts) {
		this.requiredAtts = requiredAtts;
	}
}