package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Semantic Checker
 * Please refer to A4-1.1.pdf for requirements
 * @author jz33
 *
 */
public class SemanticChecker {

	private ArrayList<Expression> mySelect; //Select clause
	private Map<String, String> myFrom; //From clause
	private Expression where = null; //Where clause
	private String att = ""; //Group by clause
	private Map <String, TableData> catalog; //Catalog info
	
	private boolean ifGrouped = false; // Indicate if group by clause exists
	private final String NOTEXIST = "Some abbr, attr, tablename or type is not in catalog!";
	
	public SemanticChecker(
			ArrayList<Expression> mySelect, 
			Map<String, String> myFrom,
			Expression where,
			String att,
			Map <String, TableData> catalog
			){
		this.mySelect = new ArrayList<Expression>(mySelect);
		this.myFrom   = new HashMap<String,String>(myFrom);
		this.where    = where; //where is initialized as null
		this.att      = att; //group by is initialized as null
		this.catalog  = new HashMap<String, TableData>(catalog);
	}
	
	/************************The Interface ************************/
	public boolean execute(){
		System.out.println ("SemanticChecker.execute: Semantic Checking...");
		if(!this.checkFrom()) return false;
		if(!this.checkGroup()) return false;
		if(!this.checkSelect()) return false;
		if(!this.checkWhere()) return false;
		System.out.println ("SemanticChecker.execute: Semantic Checking Passed");
		return true;
	}
	
	/************************ The Four Fundamental Clause Checks **************/
	private boolean checkSelect(){
		for(Expression expr:this.mySelect){
			if(this.getExpressionType(expr) == NOTEXIST){
			    System.err.println("SemanticChecker.checkSelect: type of where is not right!");
			    return false;
			}
			
			if (this.ifGrouped)
				if ( (!expr.getType().equals("sum")) && (!expr.getType().equals("avg")) )//System.out.println("Type:"+expr.getType());
					if (this.getAggregateType(expr) == this.NOTEXIST) {
						System.err.println("SemanticChecker.checkSelect: select expression: "+expr.getValue()+" is in valid when group is activated! ");
						return false;
					}
		}
		
		System.out.println("SemanticChecker.checkSelect: PASS");
		return true;
	}
	
	private boolean checkFrom(){
		if(this.myFrom == null){
			System.err.println("SemanticChecker.checkFrom: no From clause...");
			return true;
		}
		
		//Check if abbr exits
		for(String abbr:this.myFrom.keySet()){
			String tablename = this.convertAbbr2Table(abbr);
			if(tablename == this.NOTEXIST){
				System.err.println("SemanticChecker.checkFrom: abbr: "+ abbr+ " not in catalog");
				return false;
			}
		}
		
		//Check if table exits
		for(String table:this.myFrom.values()){
			if(!this.ifTableInCat(table)){
				System.err.println("SemanticChecker.checkFrom: tablename: "+ table+ " not in catalog");
				return false;
			}
		}
		
		System.out.println("SemanticChecker.checkFrom: PASS");
		return true;
	}

	private boolean checkWhere(){
		if(this.where == null){
			System.out.println("SemanticChecker.checkWhere: no Group By clause...");
			return true;
		}
		
		if(this.getExpressionType(this.where) == this.NOTEXIST){
			System.err.println("SementicChecker.checkWhere: type of where is not right!");
			return false;
		}
		
		System.out.println("SemanticChecker.checkWhere: PASS");
		return true;
	}
	
	private boolean checkGroup(){
		if(this.att == null || this.att.equals("")){
			this.ifGrouped = false;
			System.out.println("SemanticChecker.checkGroup: no Group By clause...");
			return true;
		}

		this.ifGrouped = true;
		
		if(this.getIdentifierType(this.att) == this.NOTEXIST){
			System.err.println("SemanticChecker.checkGroup: type of att is not right!");
			return false;
		}
		
		System.out.println("SemanticChecker.checkGroup: PASS");
		return true;
	}
	
	/***********************If In Methods*************************/
	
	/**
	 * Check if table in catalog
	 * Call by checkFrom
	 * @return
	 */
	private final boolean ifTableInCat(String tablename){
		switch(tablename){
        case("part"): break;
        case("supplier"): break;
        case("partsupp"): break;
        case("customer"): break;
        case("orders"): break;
        case("lineitem"): break;
        case("nation"): break;
        case("region"): break;
        default:
        	System.err.println("SemanticChecker.ifTableInCat: "+tablename+" is not in catalog!");
        	return false;
		}
		return true;
	}
		
	/**
	 * Check if attribute in table
	 * Call by checkFrom
	 * @return
	 */
	private final boolean ifAttrInCat(String abbr, String attr){
		String tablenmae=this.convertAbbr2Table(abbr);
        switch(tablenmae){
        case("part"): {
        	switch(attr){
        	case("p_partkey"): break;
        	case("p_name"): break;
        	case("p_mfgr"): break;
        	case("p_brand"): break;
        	case("p_type"): break;
        	case("p_size"): break;
        	case("p_container"): break;
        	case("p_retailprice"): break;
        	case("p_comment"): break;
        	default:
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        case("supplier"): {
        	switch(attr){
        	case("s_suppkey"): break;
        	case("s_name"): break;
        	case("s_address"): break;
        	case("s_nationkey"): break;
        	case("s_phone"): break;
        	case("s_acctbal"): break;
        	case("s_commnet"): break;
        	default:
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        case("partsupp"): {
        	switch(attr){
        	case("ps_partkey"): break;
        	case("ps_suppkey"): break;
        	case("ps_availqty"): break;
        	case("ps_supplycost"): break;
        	case("ps_comment"): break;
        	default: 
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        case("customer"): {
        	switch(attr){
        	case("c_custkey"): break;
        	case("c_name"): break;
        	case("c_address"): break;
        	case("c_nationkey"): break;
        	case("c_phone"): break;
        	case("c_acctbal"): break;
        	case("c_mktsegment"): break;
        	case("c_comment"): break;
        	default: 
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        case("orders"):{
        	switch(attr){
        	case("o_orderkey"): break;
        	case("o_custkey"): break;
        	case("o_orderstatus"): break;
        	case("o_totalprice"): break;
        	case("o_orderdate"): break;
        	case("o_orderpriority"): break;
        	case("o_clerk"): break;
        	case("o_shippriority"): break;
        	case("o_comment"): break;
        	default: 
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        case("lineitem"):{
        	switch(attr){
        	case("l_orderkey"): break;
        	case("l_partkey"): break;
        	case("l_suppkey"): break;
        	case("l_linenumber"): break;
        	case("l_quantity"): break;
        	case("l_extendedprice"): break;
        	case("l_discount"): break;
        	case("l_tax"): break;
        	case("l_returnflag"): break;
        	case("l_linestatus"): break;
        	case("l_shipdate"): break;
        	case("l_commitdate"): break;
        	case("l_receiptdate"): break;
        	case("l_shipinstruct"): break;
        	case("l_shipmode"): break;
        	case("l_comment"): break;
        	default:
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        case("nation"):{
        	switch(attr){
        	case("n_nationkey"): break;
        	case("n_name"): break;
        	case("n_regionkey"): break;
        	case("n_comment"): break;
        	default:
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        case("region"):{
        	switch(attr){
        	case("r_regionkey"): break;
        	case("r_name"): break;
        	case("r_comment"): break;
        	default:
            	System.err.println("SemanticChecker.ifTableInCat: "+attr+" is not an attribute of "+tablenmae+" !");
            	return false;
        	}
        	break;
        }
        default:
        	System.err.println("SemanticChecker.ifTableInCat: "+abbr+" is not a valid abbreviate !");
        	return false;
        }
		return true;
	}
	
	/**
	 * Check if abbreviate in from clause
	 * Call by checkGroup
	 * @return
	 */
	private final boolean ifAbbrInFrom(String abbr){
		return this.myFrom.containsKey(abbr)? true:false;
	}
	
	
	/***********************Convert Methods*************************/
	/**
     * Convert abbr to tablename
     * Notice that abbr can be p, ps, p1, p2
     * @param abbr
     * @return
     */
    private final String convertAbbr2Table(String abbr){
    	String tablename = new String();

    	if(abbr.length()>3 || abbr.length()==0){
    		System.err.println("SemanticChecker.convertAbbr2table: "+abbr+ " is inappropriate in length!");
    		return this.NOTEXIST;
    	}
    	else if(abbr.length()==3)
    		if(abbr.substring(0,2).equals("ps")) tablename = "partsupp";
    		else{
    			System.err.println("SemanticChecker.convertAbbr2table: "+abbr+ " with length 3 is not ps#!");
        		return this.NOTEXIST;
    		}
    	else if(abbr.length()==2)
    		if(abbr.equals("ps")) tablename = "partsupp";
    		else{
    			String second = abbr.substring(1,2);//System.out.println(second);
    			switch(second){
    	        case("1"): break;
    	        case("2"): break;
    	        case("3"): break;
    	        default:
    	        	System.err.println("SemanticChecker.convertAbbr2table: "+abbr+ " has too many duplicates!");
    	        	return this.NOTEXIST;
    	        }
    			
    			String first = abbr.substring(0,1);
    			switch(first){
    	        case("p"): tablename="part";break;
    	        case("s"): tablename="supplier";break;
    	        case("c"): tablename="customer";break;
    	        case("o"): tablename="orders";break;
    	        case("l"): tablename="lineitem";break;
    	        case("n"): tablename="nation";break;
    	        case("r"): tablename="region";break;
    	        default:
    	        	System.err.println("SemanticChecker.convertAbbr2table: "+abbr+ " is not in catalog!");
    	        	return this.NOTEXIST;
    	        }
    		}
    	else
    		switch(abbr){
            case("p"): tablename="part";break;
            case("s"): tablename="supplier";break;
            case("c"): tablename="customer";break;
            case("o"): tablename="orders";break;
            case("l"): tablename="lineitem";break;
            case("n"): tablename="nation";break;
            case("r"): tablename="region";break;
            default:
            	System.err.println("SemanticChecker.convertAbbr2table: "+abbr+ " is not in catalog!");
            	return this.NOTEXIST;
            }
    	return tablename;
    }
	
    
    /***********************Type Methods*************************/
    /**
     * Get the type of an input expression
     * @param expr
     * @return
     */
    private final String getExpressionType(Expression expr){
    	String etype = expr.getType();
    	String utype; // for unary
    	String ltype; // left type
		String rtype; // right type
    	
    	//3 base types
    	if (etype.equals("literal string") || etype.equals("literal float") || etype.equals("literal int")) return etype;
    	
    	//Identifier
    	if (etype.equals("identifier")) return this.getIdentifierType(expr.getValue());
    	
    	//Binary, - * /
    	if (etype.equals("minus") || etype.equals("times") || etype.equals("divided by")){
    		ltype = this.getExpressionType(expr.getSubexpression("left"));
    		rtype = this.getExpressionType(expr.getSubexpression("right"));
    		if (ltype.equals("literal int") || rtype.equals("literal int")) return "literal int";
			return "literal float";
    	}
    	
    	//Binary, > < = +
    	if (etype.equals("greater than") || etype.equals("less than") || etype.equals("equals") || etype.equals("plus")){
    		ltype = this.getExpressionType(expr.getSubexpression("left"));
    		rtype = this.getExpressionType(expr.getSubexpression("right"));
			if ((ltype.equals("literal string") && rtype.equals("literal string")) || (isNumeric(ltype) && isNumeric(rtype)) ){
				if (etype.equals("plus")) return ltype;
				return etype;
			}
			System.err.println("SemanticChecker.getExpressionType: "+ltype+" and "+rtype+" can not be params for "+etype+": \n Left type and right type should be either str or number!");
			return NOTEXIST;
		}
    	
    	//Unary, sum, avg, unary minus
    	if (etype.equals("unary minus") || etype.equals("sum") || etype.equals("avg")){//System.out.println("utype:"+expr.getType());
        	utype = this.getExpressionType(expr.getSubexpression());
    		if (utype.equals(NOTEXIST)) return NOTEXIST;
    		if (!this.isNumeric(utype)){
    			System.err.println("SemanticChecker.getExpressionType: subtype of sum, avg, unary minus should be numeric!");
    			return NOTEXIST;
    		}
    		return utype;
    	}
		
    	//Unary, not
    	//Only valid in where clause
    	if (etype.equals("not")){
    		utype = this.getExpressionType(expr.getSubexpression());
    		if(!this.isComparison(utype)){
    			System.err.println("SemanticChecker.getExpressionType: subtype of not should be comparison!");
    			return NOTEXIST;
    		}
    		return etype;
    	}
    	
    	//Binary, and, or
    	//Only valid in where clause
    	if (etype.equals("and") || etype.equals("or")){
    		ltype = this.getExpressionType(expr.getSubexpression("left"));
    		rtype = this.getExpressionType(expr.getSubexpression("right"));
			if (!this.isComparison(ltype) || !this.isComparison(rtype)){
				System.err.println("SemanticChecker.getExpressionType: "+ltype+" and "+rtype+" can not be params for "+etype+": \n Left type and right type should be booleans!");
				return NOTEXIST;
			}
			return etype;
		}
    	
		System.err.println("SemanticChecker.getExpressionType: input expression: "+expr+" is of invalid type!");
    	return NOTEXIST;
    }
    
    /**
     * Find attribute type
     * @param identifier
     * @return String type
     */
    private final String getIdentifierType(String identifier){
    	//If too short
    	if(identifier.length()<3){
    		System.err.println("SemanticChecker.getIdentifierType: identifier: "+identifier+" is not in right format!");
    		return this.NOTEXIST;
    	}
    	//Split, group by string is abbr.attr
    	String strs[] = identifier.split("\\.");
		if(strs.length != 2 || strs == null || strs[0].equals("") || strs[1].equals("")){
			System.err.println("SemanticChecker.getIdentifierType: identifier: "+identifier+" is not in right format!");
			return this.NOTEXIST;
		}
		String abbr = strs[0];
		String attr = strs[1];		
    	
		//Check if abbreviate is in from clause
		if(!this.ifAbbrInFrom(abbr)){
			System.err.println("SemanticChecker.getIdentifierType: abbr: "+abbr+" Abbreviate not in from clause!");
			return this.NOTEXIST;
		}		
		
		//Check if attribute is in catalog
		if(!this.ifAttrInCat(abbr, attr)){
			System.err.println("SemanticChecker.getIdentifierType: attr : "+attr+" Attribute not in catalog!");
			return this.NOTEXIST;
		}
		
		String output = this.catalog.get(this.convertAbbr2Table(abbr)).getAttInfo(attr).getDataType();
		switch(output){
		case("Int"):  output = "literal int";break;
		case("Str"):  output = "literal string";break;
		case("Float"):output = "literal float";break;
		}
    	return output;
    }
    
    /**
     * Find subtype of sum, svg
     * @param expr
     * @return
     */
    private final String getAggregateType(Expression expr){
    	String etype = expr.getType();
		if (etype.equals("identifier")) { //System.out.println("etype: "+etype);
			return expr.getValue().equals(this.att)? etype:this.NOTEXIST;
		}
		else if (etype.equals("literal string") || etype.equals("literal float") ||etype.equals("literal int")) {
			return etype;
		}
		else if ( etype.equals("unary minus") || etype.equals("not") ){
			return this.getAggregateType(expr.getSubexpression());
		}
		else if (this.isBinary(etype)){
			return this.getAggregateType(expr.getSubexpression("left"));
		}
		
		System.err.println("SemanticChecker.getAggregateType: expression: "+expr+" not exists!");
		return this.NOTEXIST;
    }
    
   /**
    * Check if a type is numeric
    * @param type
    * @return
    */
	private final boolean isNumeric (String type){
		return type.equals("literal int") || type.equals("literal float");
	}
	
	/**
	 * Check if a type is comparison
	 * @param type
	 * @return
	 */
	private final boolean isComparison (String type){
		return  type.equals("and") || 
				type.equals("or") || 
				type.equals("equals") || 
				type.equals("less than") || 
				type.equals("greater than") || 
				type.equals("not");
	}
	
	/**
	 * Check if an operator type is binary
	 * @param type
	 * @return
	 */
	private final boolean isBinary (String type){
		return  type.equals("and") || 
				type.equals("or") || 
				type.equals("equals") || 
				type.equals("less than") || 
				type.equals("greater than") || 
				type.equals("plus") ||
				type.equals("minus") ||
				type.equals("divided by") ||
				type.equals("times");
	}
}
