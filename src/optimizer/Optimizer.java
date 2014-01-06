package optimizer;

import java.util.*;
import java.util.Map.Entry;
import parser.*;
import runner.*;

/**
 * Optimize * Prepare to run
 * @author jz33
 */
public class Optimizer {

	private ArrayList<Expression> mySelect; //Select clause
	private Map<String, String> myFrom; //From clause
	private Expression myWhere; //Where clause
	private String myGroup; //Group by clause
	private Map <String, TableData> catalog; //Catalog info
	
	/**
	 * Notice that myGroup is null, then ifGroup can be true, as long as sum/avg in mySelect
	 */
	private boolean ifGrouped = false; 
	
	private final String NOTEXIST = "Some abbr, attr, tablename or type is not in catalog!";
	private String compiler = "g++";
	private String cppDir = "cppDir/";
	
	public Optimizer(
			ArrayList<Expression> mySelect, 
			Map<String, String> myFrom,
			Expression myWhere,
			String myGroyp,
			Map <String, TableData> catalog
			){
		this.mySelect = new ArrayList<Expression>(mySelect);
		this.myFrom   = new HashMap<String,String>(myFrom);
		this.myWhere  = myWhere; //where is initialized as null
		this.myGroup  = myGroyp; //group by is initialized as null
		this.catalog  = new HashMap<String, TableData>(catalog);
	}
	
	/************************The Interface ************************/
	public void execute(){
		//Time start
		System.out.println("Optimizer.execute: Running optimzier...");
	    long startTime = System.currentTimeMillis();  
	    
	    /*********************************Parse myFrom*************************************/
	    //Construct table_list from myFrom
	    ArrayList<Table> table_list = new ArrayList<Table>(myFrom.size());
	    for(String key : myFrom.keySet()){
	    	String tablename = myFrom.get(key);
	    	Table t = new Table(tablename);
	    	t.getAbbrList().add(key);
	    	
	    	//Add attributes into table.attrList, preserving the order attribute sequence order
	    	Map<String, AttInfo> attrs = this.catalog.get(tablename).getAttrs();
	    	for(int i=0;i<attrs.size();i++)
	    		for(Entry<String, AttInfo> e: attrs.entrySet())
	    			if(e.getValue().getAttSequenceNumber()==i){
	    				t.getAttrList().add(new Attribute(e.getValue().getDataType(),e.getKey()));
	    				break;
	    			}//System.out.println("Optimizer.execute.t.attrlist: "+t.getAttrList());
	    	table_list.add(t);
	    }//System.out.println("Optimizer.table_list: "+table_list);
	    
	    /*********************************Parse myWhere*************************************/
	    ArrayList<Condition> cond_list = new ArrayList<Condition>();
	    this.parseWhereRec(cond_list,this.myWhere); //System.out.println("Optimizer.execute.conds: "+conds);
	    
	    /*********************************Parse mySelect*************************************/
	    ArrayList<Attribute> selectedAtts = new ArrayList<Attribute>();// name = att1, type = Str
	    ArrayList<Attribute> requiredAtts  = new ArrayList<Attribute>();// name = o.o_orderdate, type = Str 
	    Map<String,String> exprs     = new HashMap<String,String>();// exprs.put ("att1", "o_orderkey")
	    
	    for(int i=0;i<this.mySelect.size();i++){
	    	selectedAtts.add(new Attribute(
	    			this.getSelectExpTypeRec(mySelect.get(i)),"att"+String.valueOf(i+1) //Not i?
	    			));
	    	this.getRequiredAttsRec(requiredAtts, mySelect.get(i));
	    	exprs.put("att"+String.valueOf(i+1), this.convertExp2StrRec(mySelect.get(i)));
	    }
	    
	    /*********************************Construct Tree*************************************/
	    Node root = new Node(true, false);
	    root.setCond_list(cond_list);//Default
	    root.setSelectedAtts(selectedAtts);//For root only
	    root.setExprs(exprs);//For root only
	    root.setAbbrList(new ArrayList<String>());//For non-leaf nodes

	    //Pull down to right node to construct the tree
	    Node cursor = root;
	    int i=0;
	    //Handle the upper tree when table number > 1
	    for(;i<table_list.size()-2;i++){
	    	cursor.setlNode(new Node(false,true));//left node is set to leaf
	    	cursor.getlNode().setTable(table_list.get(i));
	    	cursor.getlNode().setpNode(cursor);
	    	cursor.setrNode(new Node(false,false));//right node is not leaf node yet
	    	cursor.getrNode().setpNode(cursor);
	    	cursor = cursor.getrNode();//move cursor to right node
	    }
	    //Handle the bottom nodes when table number > 1
	    if(table_list.size()>1){
	    	cursor.setlNode(new Node(false,true));//left node is set to leaf
	    	cursor.getlNode().setTable(table_list.get(i));
	    	cursor.getlNode().setpNode(cursor);
	    	cursor.setrNode(new Node(false,true));//right node is set to leaf too
	    	cursor.getrNode().setTable(table_list.get(i+1));
	    	cursor.getrNode().setpNode(cursor);
	    	cursor = cursor.getrNode();//move cursor to right node
	    }
	    
	    //If only 1 table (2 nodes)
	    if(table_list.size()==1){
	    	root.setIfUnique(true);
	    	root.setlNode(new Node(false,true));//left node is set to leaf
	    	root.getlNode().setTable(table_list.get(0));
	    	root.getlNode().setpNode(root);
	    } 
	    this.aggAbbr2Root(root);
	    
	    /*********************************Optimize tree*************************************/
	    if(root.isIfUnique()) this.pushRequiredAttsDownRec(root,requiredAtts);
	    else{
	    	this.pushCondDownRec(root);
	    	this.pushRequiredAttsDownRec(root,requiredAtts);
	    }
	    this.reorderAbbrListRec(root);
	    //this.debugNodes(root);
	    /*********************************Execute query*************************************/
	    this.executeQueryRec(root);
	    if(this.ifGrouped==true) this.executeGroup(root,selectedAtts);
	    System.out.println("The run took " + (System.currentTimeMillis() - startTime) + " milliseconds");
	}
	
	/************************The 4 Executions************************/
	/**
	 * Run selection
	 * new Selection(inAtts, outAtts, selection, exprs, inFile, outFile, compiler, cppDir);
	 * @param cond_list
	 * @param requiredAtts
	 * @param t
	 * @param expr_dict
	 * @return
	 */
	private final Table executeSelect(
			ArrayList<Condition> cond_list,//Default
			ArrayList<String> requiredAtts,//Default
			Table t,//Leaf only
			Map<String,String> expr_dict //Root only
			){
		//Set inAtts
		ArrayList<Attribute> inAtts = t.getAttrList(); //"Int", "o_orderkey"

		//Set selection
		String selection = this.ConvertConds2Cond(cond_list).getCond();//"o_orderdate > Str (\"1996-12-19\") && o_custkey < Int (100)"
		for (String abbr : t.getAbbrList()) {
			String regex = abbr+"\\.";
			String replacement = "";
			selection = selection.replaceAll(regex, replacement);
			for(int i=0;i<requiredAtts.size();i++)
				requiredAtts.set(i, requiredAtts.get(i).replaceAll(regex, replacement));
		}
				
		//Set exprs
		Map<String,String> exprs;//"att1", "o_orderkey"
		if(expr_dict != null) exprs = expr_dict;
		else exprs = new HashMap<String, String>();
		
		//Set exprs, outAtts, nextAtts
		ArrayList<Attribute> outAtts = new ArrayList<Attribute>();//"Int", "att1"
		ArrayList<Attribute> nextAtts = new ArrayList<Attribute>();
		for(int i=0, j=0;i<inAtts.size();i++){
			Attribute attr = inAtts.get(i);
			if(requiredAtts.contains(attr.getName())){
				nextAtts.add(attr);
				outAtts.add(new Attribute(attr.getType(),"att"+String.valueOf(j+1))); //Not j?
				if(expr_dict==null) exprs.put("att"+String.valueOf(j+1), attr.getName());
				j++;
			}
		}
		
		//Modify exprs and selection, delete all alias
		for (Entry<String,String> entry : exprs.entrySet()){
			for (String abbr : t.getAbbrList()) {
				String regex = abbr+"\\.";
				String replacement = "";
				entry.setValue(entry.getValue().replaceAll(regex, replacement));
			}
		}

		//Set inFile, outFile
		String inFile = t.getTableName()+".tbl";
		String outFile;
		if (expr_dict != null && this.ifGrouped==false) outFile = "_select_out";
		else outFile = t.creatOutFileName();
		
		try{
			new Selection(inAtts, outAtts, selection, exprs, inFile, outFile+".tbl", this.compiler, this.cppDir);
		}catch(Exception e){
		}finally{
			/*
			System.out.println("Optimizer.executeSelect.inAtts: " + inAtts);
			System.out.println("Optimizer.executeSelect.outAtts: "+outAtts);
			System.out.println("Optimizer.executeSelect.selection: " + selection);
			System.out.println("Optimizer.executeSelect.exprs: " + exprs);
			System.out.println("Optimizer.executeSelect.inFile: " + inFile);
			System.out.println("Optimizer.executeSelect.outFile: " + outFile+"\n");
			*/
		}
		
		//Set output table && Free memory
	    Table nextTable = new Table(outFile);
	    for (String abbr : t.getAbbrList()) nextTable.getAbbrList().add(abbr);
	    nextTable.setAttrList(nextAtts);
		System.out.println("Optimizer.executeSelect success on table "+ t.getTableName() +", output file is "+outFile);
	    t.free();
	    return nextTable;
	}

	/**
	 * Run Join
	 * new Join (inAttsLeft, inAttsRight, outAtts, leftHash, rightHash, selection, exprs, 
	 *  inFileLeft, inFileRight, outFile, compiler, cppDir);                    
	 * @param cond_list
	 * @param requiredAtts
	 * @param lt
	 * @param rt
	 * @param expr_dict
	 * @param selectedAtts
	 * @return
	 */
	private final Table executeJoin(
			ArrayList<Condition> cond_list,//Default
			ArrayList<String> requiredAtts,//Default
			Table lt,//Leaf only, left table
			Table rt,//Leaf only, left table
			Map<String,String> expr_dict, //Root only
			ArrayList<Attribute> selectedAtts //Root only
			){
		
		//Modify requiredAtts from o.o_custkey to o_custkey
		for(int i=0;i<requiredAtts.size();i++){
			for(String abbr : lt.getAbbrList())
				requiredAtts.set(i, requiredAtts.get(i).replaceAll(abbr+"\\.", ""));
			for(String abbr : rt.getAbbrList())
				requiredAtts.set(i, requiredAtts.get(i).replaceAll(abbr+"\\.", ""));
		}
		
		//Set outAtts
		ArrayList<Attribute> outAtts = new ArrayList<Attribute>();
		if(selectedAtts != null) outAtts = selectedAtts;
		
		//Set exprs
		Map<String,String> exprs;//"att1", "o_orderkey"
		if(expr_dict != null) exprs = expr_dict;
		else exprs = new HashMap<String, String>();
				
		//Set inAttsLeft, inAttsRight, outAtts, nextAtts, exprs
		ArrayList<Attribute> inAttsLeft  = lt.getAttrList();
		ArrayList<Attribute> inAttsRight = rt.getAttrList();
		ArrayList<Attribute> nextAtts = new ArrayList<Attribute>();

		ArrayList<String> lattrs1 = new ArrayList<String>();
		ArrayList<String> rattrs1 = new ArrayList<String>();
		ArrayList<String> lattrsChanged = new ArrayList<String>();
		ArrayList<String> rattrsChanged = new ArrayList<String>();
		for(int i=0,j=0;i<inAttsLeft.size();i++){
			Attribute attr = inAttsLeft.get(i);
			if(requiredAtts.contains(attr.getName())){
				if(lattrs1.contains(attr.getName())){
					lattrsChanged.add(attr.getName());
					attr.setName(attr.getName()+"1");
				}
				nextAtts.add(attr);
				if(selectedAtts == null) outAtts.add(new Attribute(attr.getType(),"att"+String.valueOf(j+1)));
				if(expr_dict == null) exprs.put("att"+String.valueOf(j+1), "left."+attr.getName());
				j++;
				lattrs1.add(attr.getName());
			}
		}
		for(int i=0,j=0;i<inAttsRight.size();i++){
			Attribute attr = inAttsRight.get(i);
			if(requiredAtts.contains(attr.getName())){
				if(rattrs1.contains(attr.getName())){
					rattrsChanged.add(attr.getName());
					attr.setName(attr.getName()+"1");
				}
				nextAtts.add(attr);
				if(selectedAtts == null) outAtts.add(new Attribute(attr.getType(),"att"+String.valueOf(j+1)));
				if(expr_dict == null) exprs.put("att"+String.valueOf(j+1), "right."+attr.getName());
				j++;
				rattrs1.add(attr.getName());
			}
		}
		
		//Set leftHash, rightHash
		ArrayList<String> leftHash = new ArrayList<String>();
		ArrayList<String> rightHash = new ArrayList<String>();
		for(Condition cond : cond_list)
			for(int i = 0; i < cond.getAbbrList().size(); i++)
				if(lt.getAbbrList().contains(cond.getAbbrList().get(i))){
					if(leftHash.contains(cond.getAttrList().get(i))==false){
					
						if (lattrsChanged.contains(cond.getAttrList().get(i)))
							leftHash.add(cond.getAttrList().get(i)+"1");
						else leftHash.add(cond.getAttrList().get(i));
					}
				}
				else{
					if(rightHash.contains(cond.getAttrList().get(i))==false){
						
						if (rattrsChanged.contains(cond.getAttrList().get(i)))
							rightHash.add(cond.getAttrList().get(i)+"1");
						else rightHash.add(cond.getAttrList().get(i));
					}
				}
		
		//Modify expr_dict from o.o_comment to right.o_comment
		if(expr_dict != null)
			for(Entry<String,String> entry : exprs.entrySet()){
				for(String abbr: lt.getAbbrList())
					entry.setValue(entry.getValue().replaceAll(abbr+"\\.", "left."));
				for(String abbr: rt.getAbbrList())
					entry.setValue(entry.getValue().replaceAll(abbr+"\\.", "right."));
			}
		
		//Set selection
		String selection = this.ConvertConds2Cond(cond_list).getCond();
		
		/**
		 * Replace part of the input string with abbrList
		 * Notice the ps, s trick, e.g.:
		 * @param str = s.s_suppkey == ps.ps_suppkey && s.s_nationkey == n.n_nationkey
		 * @param replacement = "left"
		 * @param abbrList = [s]
		 * @return
		 */
		selection = " "+selection;//Add whitespace
		String replacement = " left.";//Add whitespace
		for(String abbr:lt.getAbbrList()){
			String regex = "\\s"+abbr+"\\.";
			selection = selection.replaceAll(regex, replacement);
		}
		replacement = " right.";//Add whitespace
		for(String abbr:rt.getAbbrList()){
			String regex = "\\s"+abbr+"\\.";
			selection = selection.replaceAll(regex, replacement);
		}
		selection = selection.substring(1); 
		
		//Set inFileLeft, inFileRight, outFile
		String inFileLeft = lt.getTableName()+".tbl";
		String inFileRight= rt.getTableName()+".tbl";
		
		String outFile;
		if(expr_dict != null && this.ifGrouped==false) outFile = "_join_out";
		else outFile = lt.creatOutFileName()+rt.creatOutFileName();
		
		try{
			new Join (inAttsLeft, inAttsRight, outAtts, leftHash, rightHash, selection, exprs, 
					 inFileLeft, inFileRight, outFile+".tbl", this.compiler, this.cppDir);
		}catch(Exception e){
		}finally{
			/*
			System.out.println("Optimizer.executeJoin.inAttsLeft: " + inAttsLeft);
			System.out.println("Optimizer.executeJoin.inAttsRight: " + inAttsRight);
			System.out.println("Optimizer.executeJoin.outAtts: "+outAtts);
			System.out.println("Optimizer.executeJoin.leftHash: "+leftHash);
			System.out.println("Optimizer.executeJoin.rightHash: "+rightHash);
			System.out.println("Optimizer.executeJoin.selection: " + selection);
			System.out.println("Optimizer.executeJoin.exprs: " + exprs);
			System.out.println("Optimizer.executeJoin.inFileLeft: " + inFileLeft);
			System.out.println("Optimizer.executeJoin.inFileRight: " + inFileRight);
			System.out.println("Optimizer.executeJoin.outFile: " + outFile+".tbl\n");
			*/
		}
		
		//Set output table && Free memory
	    Table output = new Table(outFile);
	    for(String str : lt.getAbbrList()) output.getAbbrList().add(str);
	    for(String str : rt.getAbbrList()) output.getAbbrList().add(str);
	    output.setAttrList(nextAtts);
	    System.out.println("Successfully performed join on table " + lt.getTableName()+ " , "+ rt.getTableName() +"!");
	    lt.free();
	    rt.free();
		return output;
	}
	
	/**
	 * new Grouping (inAtts, outAtts, groupingAtts, myAggs, inFile, outFile, compiler, cppDir); 
	 * @param n
	 * @param outAtts
	 */
	private final void executeGroup(Node n, ArrayList<Attribute> outAtts){
		Table t = n.getTable();
		
		//inAtts, outAtts, groupingAtts
		ArrayList<Attribute> inAtts = t.getAttrList();
		ArrayList <String> groupingAtts = new ArrayList <String>();
		if(this.myGroup != null) groupingAtts.add(this.myGroup.split("\\.")[1]);
		
		//myAggs
		Map<String, AggFunc> myAggs = new HashMap<String, AggFunc>();
		String type;
		String selection;
		for(int i=0;i<this.mySelect.size();i++){
			Expression expr = this.mySelect.get(i);
			type = this.getGroupExpTypeRec(expr);
			selection = this.convertExp2StrRec(expr);
			for(String abbr : t.getAbbrList()) selection = selection.replaceAll(abbr+"\\.","");
			if(!type.equals("sum") && !type.equals("avg")) type = "none";
			myAggs.put("att"+String.valueOf(i+1),new AggFunc (type,selection));
		}
		
		//inFile, outFile
		String inFile =t.getTableName()+".tbl";
		String outFile = "_group_out.tbl";
		
		try{
			new Grouping(inAtts,outAtts,groupingAtts,myAggs,inFile,outFile,this.compiler,this.cppDir);
		}catch(Exception e){
		}finally{
			/*
			System.out.println("Optimizer.executeGroup.inAtts: " + inAtts);
			System.out.println("Optimizer.executeGroup.outAtts: "+outAtts);
			System.out.println("Optimizer.executeGroup.groupingAtts: " + groupingAtts);
			System.out.println("Optimizer.executeGroup.myAggs: " + myAggs);
			System.out.println("Optimizer.executeGroup.inFile: " + inFile);
			System.out.println("Optimizer.executeGroup.outFile: " + outFile+"\n");
			*/
		}
		System.out.println("Optimizer.executeGroup success on table "+ t.getTableName() +", output file is "+outFile);
	}
	
	/**
	 * Execute query tree
	 * @param n
	 */
	private final void executeQueryRec(Node n){
		if(n.isIfLeaf()){
			n.setTable(this.executeSelect(n.getCond_list(),n.getRequiredAtts(),n.getTable(),null));
			n.setIfJoin(true);
			return;
		}
		
		if(n.isIfUnique()){
			if(this.ifGrouped)
			    n.setTable(this.executeSelect(n.getCond_list(),n.getRequiredAtts(),n.getlNode().getTable(),null));
			else
				n.setTable(this.executeSelect(n.getCond_list(),n.getRequiredAtts(),n.getlNode().getTable(),n.getExprs()));
			return;
		}
		
		//Wait until left and right nodes are ready to join
		if(n.getlNode().isIfJoin()==false) this.executeQueryRec(n.getlNode());
		if(n.getrNode().isIfJoin()==false) this.executeQueryRec(n.getrNode());
		
		n.setIfJoin(true);
		if(this.ifGrouped)
		    n.setTable(this.executeJoin(n.getCond_list(),n.getRequiredAtts(),n.getlNode().getTable(),n.getrNode().getTable(),
		    		null,null));
		else
		    n.setTable(this.executeJoin(n.getCond_list(),n.getRequiredAtts(),n.getlNode().getTable(),n.getrNode().getTable(),
		    		n.getExprs(),n.getSelectedAtts()));
	}
	
	/************************The Auxiliary Methods ************************/
    
	/*********************************Helper Function************************************/
	/**
     * Parse where clause, break into conditions (sub clause separated by "and")
     * @param conds
     * @param expr 
     */
    private final void parseWhereRec(ArrayList<Condition> conds, Expression expr) {
    	String etype = expr.getType();
    	//If expression contains "and", break into sub conditions
		if(etype.equals("and")){
			this.parseWhereRec(conds,expr.getSubexpression("left"));
			this.parseWhereRec(conds,expr.getSubexpression("right"));
		}
		
		//Otherwise, recursively parse where clause then add to conds
		else{
			Condition cond = new Condition(this.convertExp2StrRec(expr),etype);
			this.convertExp2CondRec(cond,expr);
			conds.add(cond);
		}
	}

    /**
	 * Push down conditions from root to sub nodes as far as possible
	 * @param n
	 */
	private final void pushCondDownRec(Node n){
		if(n.isIfLeaf()) return;
		
		ArrayList<Condition> dupConds = new ArrayList<Condition>();
		for(Condition cond:n.getCond_list()){
			if(n.getlNode() != null){
				if(n.getlNode().isIfLeaf()){
					if(n.getlNode().getTable().getAbbrList().containsAll(cond.getAbbrList())){
						dupConds.add(cond);
						n.getlNode().getCond_list().add(cond);
						continue;//Push only once!
					}
				}
				else{
					if(n.getlNode().getAbbrList().containsAll(cond.getAbbrList())){
						dupConds.add(cond);
						n.getlNode().getCond_list().add(cond);
						continue;
					}
				}
			}
			
			if(n.getrNode() != null){
				if(n.getrNode().isIfLeaf()){
					if(n.getrNode().getTable().getAbbrList().containsAll(cond.getAbbrList())){
						dupConds.add(cond);
						n.getrNode().getCond_list().add(cond);
						continue;//Push only once!
					}
				}
				else{
					if(n.getrNode().getAbbrList().containsAll(cond.getAbbrList())){
						dupConds.add(cond);
						n.getrNode().getCond_list().add(cond);
						continue;
					}
				}
			}	
		}
		n.getCond_list().removeAll(dupConds);
		if(n.getlNode()!=null) this.pushCondDownRec(n.getlNode());
		if(n.getrNode()!=null) this.pushCondDownRec(n.getrNode());	
	}
	
	/**
     * Push requiredAtts down
     * @param n
     * @param requireddAtts
     * @return
     */
    private final void pushRequiredAttsDownRec(Node n, ArrayList<Attribute> requireddAtts){
    	if(n.getpNode()==null){
    		if(requireddAtts !=null)
    		    for(Attribute att:requireddAtts)
    		    	n.getRequiredAtts().add(att.getName());
    	}
    	else{
    		this.copyArrayList(n.getRequiredAtts(), n.getpNode().getRequiredAtts());
    		
    		//Also add attributes in parent's cond_list to n's selectedAtts
    		for(Condition cond:n.getpNode().getCond_list())
    			this.copyArrayList(n.getRequiredAtts(), cond.getAttrList());
    	}
    	
    	if(n.getlNode()!=null) this.pushRequiredAttsDownRec(n.getlNode(), null);
    	if(n.getrNode()!=null) this.pushRequiredAttsDownRec(n.getrNode(), null);
    }
    
    /**
     * Get the C++ type from mySelect, not considering aggregate (sum/avg)
     * Notice that this one is not same with SemanticChecker.getExpressionType
     * @param expr
     * @return
     */
    private final String getSelectExpTypeRec(Expression expr){
    	String etype = expr.getType();
    	
    	//3 base types
    	if(etype.equals("literal string")) return "Str";
    	if(etype.equals("literal float"))  return "Float";
    	if(etype.equals("literal int"))    return "Int";
    	
    	//Identifier
    	if (etype.equals("identifier")){
    		String strs[] = expr.getValue().split("\\.");
    		String abbr = strs[0];
    		String attr = strs[1];
    		return this.catalog.get(this.myFrom.get(abbr)).getAttInfo(attr).getDataType();
    	}
    	
    	//Numeric Binary, + - * /
    	if (etype.equals("plus") || etype.equals("minus") || etype.equals("times") || etype.equals("divided by"))
    		return this.getSelectExpTypeRec(expr.getSubexpression("left"));
    	
    	//Unary, sum, avg, unary minus
    	if (etype.equals("unary minus") || etype.equals("sum") || etype.equals("avg")){
    		if(!etype.equals("unary minus")) this.ifGrouped = true;
    		return this.getSelectExpTypeRec(expr.getSubexpression());
    	}
    		
		System.err.println("Optimizer.getSelectExpTypeRec: input expression: "+expr+" is of invalid type! ");
    	return NOTEXIST;
    }
    
    /**
     * Get the C++ type from mySelect, considering aggregate (sum/avg)
     * @param expr
     * @return
     */
    private final String getGroupExpTypeRec(Expression expr){
        String etype = expr.getType();
    	
    	//3 base types
    	if(etype.equals("literal string")) return "Str";
    	if(etype.equals("literal float"))  return "Float";
    	if(etype.equals("literal int"))    return "Int";
    	
    	//Identifier
    	if (etype.equals("identifier")){
    		String strs[] = expr.getValue().split("\\.");
    		String abbr = strs[0];
    		String attr = strs[1];
    		return this.catalog.get(this.myFrom.get(abbr)).getAttInfo(attr).getDataType();
    	}
    	
    	//Numeric Binary, + - * /
    	if (etype.equals("plus") || etype.equals("minus") || etype.equals("times") || etype.equals("divided by"))
    		return this.getSelectExpTypeRec(expr.getSubexpression("left"));
    	
    	//Unary, unary minus
    	if (etype.equals("unary minus")) return this.getSelectExpTypeRec(expr.getSubexpression());
    			
    	//Aggregate sum, avg
    	if(etype.equals("sum")) return "sum";
    	if(etype.equals("avg")) return "avg";
    		
		System.err.println("Optimizer.getGroupExpTypeRec: input expression: "+expr+" is of invalid type! ");
    	return NOTEXIST;
    }
         
    /**
     * Convert expression to valid C++ string, see Runner
     * Not like the stock Expression.getValue
     * @param expr
     * @return
     */
    private final String convertExp2StrRec(Expression expr){
    	String etype = expr.getType();
    	
    	if(etype.equals("literal string")) return "Str ("  +expr.getValue()+")";
    	if(etype.equals("literal float"))  return "Float ("+expr.getValue()+")";
    	if(etype.equals("literal int"))    return "Int ("  +expr.getValue()+")";
    	
    	if (etype.equals("identifier")) return expr.getValue();//??
    	
    	if (etype.equals("unary minus")) return "-(" + this.convertExp2StrRec(expr.getSubexpression()) +")"; 
    	if (etype.equals("not")) return "!(" + this.convertExp2StrRec(expr.getSubexpression()) +")"; 
    	if (etype.equals("sum") || etype.equals("avg")) return this.convertExp2StrRec(expr.getSubexpression());
    			
    	if(etype.equals("plus")) 
    		return "("+this.convertExp2StrRec(expr.getSubexpression("left"))+ " + "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"))+     ")";
    	
    	if(etype.equals("minus")) 
    		return "("+this.convertExp2StrRec(expr.getSubexpression("left"))+ " - "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"))+     ")";
    	
    	if(etype.equals("times")) 
    		return "("+this.convertExp2StrRec(expr.getSubexpression("left"))+ " * "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"))+     ")";
    	
    	if(etype.equals("divided by")) 
    		return "("+this.convertExp2StrRec(expr.getSubexpression("left"))+ " / "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"))+     ")";
    	
    	if(etype.equals("greater than")) 
    		return "("+this.convertExp2StrRec(expr.getSubexpression("left"))+ " > "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"))+     ")";
    	
    	if(etype.equals("less than")) 
    		return "("+this.convertExp2StrRec(expr.getSubexpression("left"))+ " < "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"))+     ")";
    	
    	if(etype.equals("or")) 
    		return "("+this.convertExp2StrRec(expr.getSubexpression("left"))+ " || "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"))+     ")";
    	
    	if(etype.equals("equals")) 
    		return     this.convertExp2StrRec(expr.getSubexpression("left"))+ " == "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"));
    	
    	if(etype.equals("and")) 
    		return     this.convertExp2StrRec(expr.getSubexpression("left"))+ " && "
    				  +this.convertExp2StrRec(expr.getSubexpression("right"));
    	
    	System.err.println("Optimizer.convertExp2Str: input expression: "+expr+" cannot be converted! ");
    	return NOTEXIST;
    }
 
    /**
     * Prepare all selected attributes in mySelect
     * @param requiredAtts
     * @param expression
     */
    private final void getRequiredAttsRec(ArrayList<Attribute> requiredAtts, Expression expr) {
    	if(expr.getType().equals("identifier"))
    		requiredAtts.add(new Attribute(this.getSelectExpTypeRec(expr), expr.getValue()));
    	
    	if(expr.getSubexpression().getType().equals("error")==false)
    		this.getRequiredAttsRec(requiredAtts, expr.getSubexpression());
    	
    	if(expr.getSubexpression("left").getType().equals("error")==false)
    		this.getRequiredAttsRec(requiredAtts, expr.getSubexpression("left"));
    	
    	if(expr.getSubexpression("right").getType().equals("error")==false)
    		this.getRequiredAttsRec(requiredAtts, expr.getSubexpression("right"));
	}

    /**
     * Convert where clause sub conditions (other than "and")
     * @param cond
     * @param expr
     */
	private final void convertExp2CondRec(Condition cond, Expression expr) {
		String etype = expr.getType();
		
		//3 base types
    	if(etype.equals("literal string")) return;
    	if(etype.equals("literal float"))  return;
    	if(etype.equals("literal int"))    return;
    	
		//Base is identifier
		if(etype.equals("identifier")){
			String strs[] = expr.getValue().split("\\.");
    		String abbr = strs[0];
    		String attr = strs[1];
    		cond.getAbbrList().add(abbr);
    		cond.getAttrList().add(attr);
    		return;
		}
		
		//Binary or, construct 2 child tree nodes
		if(etype.equals("or")){
			cond.setLCond(new Condition(this.convertExp2StrRec(expr.getSubexpression("l"))));
			this.convertExp2CondRec(cond.getLCond(), expr.getSubexpression("l"));
			cond.setRCond(new Condition(this.convertExp2StrRec(expr.getSubexpression("r"))));
			this.convertExp2CondRec(cond.getRCond(), expr.getSubexpression("r"));
			this.copyArrayList(cond.getAbbrList(), cond.getLCond().getAbbrList());
			this.copyArrayList(cond.getAbbrList(), cond.getRCond().getAbbrList());
			this.copyArrayList(cond.getAttrList(), cond.getLCond().getAttrList());
			this.copyArrayList(cond.getAttrList(), cond.getRCond().getAttrList());
			return;
		}
		
		//Unary not. Notice that "not" is treated specially without constructing a child tree node
		if(etype.equals("not")){
			cond.setIsNot(true);
			this.convertExp2CondRec(cond, expr.getSubexpression());
			return;
		}
		
		//Binary ==, >, <. Notice that no child tree nodes is set, just add attrlist, abbrlist to cond
		if (etype.equals("equals")||etype.equals("greater than")||etype.equals("less than")) {
			this.convertExp2CondRec(cond, expr.getSubexpression("l"));
			this.convertExp2CondRec(cond, expr.getSubexpression("r"));
			return;
		}
		
		//Binary +,-,*,/
		if (etype.equals("divided by")||etype.equals("minus")||etype.equals("times")||etype.equals("plus")) {
			this.convertExp2CondRec(cond, expr.getSubexpression("l"));
			this.convertExp2CondRec(cond, expr.getSubexpression("r"));
			return;
		}
		
		System.err.println("Optimizer.covertExp2CondRec: input expression: "+expr+" cannot be converted! ");
    	return;
	}
	
	/**
	 * Concat conditions to 1 condition with "&&"
	 * @param conds
	 * @return
	 */
    private final Condition ConvertConds2Cond(ArrayList<Condition> conds){
    	if(conds.size()==0) return new Condition("true","true");
    	
    	String str = "";
    	Condition res = new Condition(str,"and");//Type is "and"
    	for(Condition cond:conds){
    		if(str.equals("")) str += cond.getCond();
    		else str += " && "+cond.getCond();
    		this.copyArrayList(res.getAbbrList(), cond.getAbbrList());
    		this.copyArrayList(res.getAttrList(), cond.getAttrList());
    	}
    	res.setCond(str);
		return res;	
    }
    
    /**
	 * Aggregate abbr list from sub nodes to root
	 * @param n
	 */
	private final void aggAbbr2Root(Node n){
		if(n.isIfLeaf()) return;
		
		if(n.getlNode() != null && n.getlNode().isIfLeaf()==true)
			this.copyArrayList(n.getAbbrList(), n.getlNode().getTable().getAbbrList());
		else if(n.getlNode() != null && n.getlNode().isIfLeaf()==false){
			this.aggAbbr2Root(n.getlNode());
			this.copyArrayList(n.getAbbrList(),n.getlNode().getAbbrList());
		}

		if(n.getrNode() != null && n.getrNode().isIfLeaf()==true)
			this.copyArrayList(n.getAbbrList(), n.getrNode().getTable().getAbbrList());
		else if(n.getrNode() != null && n.getrNode().isIfLeaf()==false){
			this.aggAbbr2Root(n.getrNode());
			this.copyArrayList(n.getAbbrList(),n.getrNode().getAbbrList());
		}
	}
	
	
	/**
	 * Copy ArrayList from src to tag without duplicate elements
	 * @param tag
	 * @param src
	 */
	private final void copyArrayList(ArrayList<String> tag, ArrayList<String> src){
		if(tag==null) tag = new ArrayList<String>();
		for (String str : src) if (!tag.contains(str)) tag.add(str);
	}
	
	/**
	 * Re-order each node's abbrList to put "ps","p1" in front
	 * @param n
	 */
	private final void reorderAbbrListRec(Node n){
		if(n==null) return;
		if(n.isIfLeaf()){
			ArrayList<String> abbrList = n.getTable().getAbbrList();
			Collections.sort(abbrList,Collections.reverseOrder());
			n.getTable().setAbbrList(abbrList);
		}
		else{
			ArrayList<String> abbrList = n.getAbbrList();
			Collections.sort(abbrList,Collections.reverseOrder());
			n.setAbbrList(abbrList);
		}
		this.reorderAbbrListRec(n.getlNode());
		this.reorderAbbrListRec(n.getrNode());
	}
	
	/************************The Debug Methods ************************/
    
	/*******************Debug Functions******************/
	/**
     * Debug tree, pre-order traverse
     * @param n
     */
	@SuppressWarnings("unused")
	private final void debugNodes(Node n){
		if(n==null) return;
		System.out.println(n+"\n");
		debugNodes(n.getlNode());
		debugNodes(n.getrNode());
	}
}