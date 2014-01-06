package interpreter;

import java.io.*;
import java.util.*;
import org.antlr.runtime.*;
import parser.*;
import optimizer.*;

/**
 * The main program
 * @author cmj4, jz33
 */
class Interpreter {
  
  public static void main (String [] args) throws Exception {
    
    try {
      CatalogReader foo = new CatalogReader ("Catalog.xml");
      Map <String, TableData> catalog = foo.getCatalog ();
      //System.out.println (CatalogReader.printCatalog (res));
      
      InputStreamReader converter = new InputStreamReader(System.in);
      BufferedReader in = new BufferedReader(converter);
      
      System.out.format ("\nSQL>");
      String soFar = in.readLine () + " ";
      
      // loop forever, or until someone asks to quit
      while (true) {
        
        // keep on reading from standard in until we hit a ";"
        while (soFar.indexOf (';') == -1) soFar += (in.readLine () + " ");
        
        // split the string
        String toParse = soFar.substring (0, soFar.indexOf (';') + 1);
        soFar = soFar.substring (soFar.indexOf (';') + 1, soFar.length ());
        toParse = toParse.toLowerCase ();
        
        // parse it
        ANTLRStringStream parserIn = new ANTLRStringStream (toParse);
        SQLLexer lexer = new SQLLexer (parserIn);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SQLParser parser = new SQLParser (tokens);
        
        // if we got a quit
        if (parser.parse () == false) break; 
        
        // print the results
        System.out.println ("RESULT OF PARSING");
        System.out.println ("Expressions in SELECT:");
        ArrayList <Expression> mySelect = parser.getSELECT ();
        for (Expression e : mySelect) System.out.println ("\t" + e);
        
        System.out.println ("Tables in FROM:");
        Map <String, String> myFrom = parser.getFROM ();
        System.out.println ("\t" + myFrom);
        
        System.out.println ("WHERE clause:");
        Expression myWhere = parser.getWHERE ();
        if (myWhere != null) System.out.println ("\t" + myWhere);
        
        System.out.println ("GROUPING att:");
        String myGroup = parser.getGROUPBY ();
        if (myGroup != null) System.out.println ("\t" + myGroup);
              
        SemanticChecker sc = new SemanticChecker(mySelect, myFrom, myWhere, myGroup, catalog);
        if(sc.execute()==true) new Optimizer(mySelect, myFrom, myWhere, myGroup, catalog).execute();
        else System.err.println("Semantic checking failed");
        
        System.out.format ("\nSQL>");
      } 
    } catch (Exception e) {
    	System.err.println("Interpreter.main.73: error!");
    } 
  }
}