package runner;

/**
 * Attribute holder
 * @author cmj4, jz33
 */
public class Attribute {
	
	private String type;
    private String name;
    
    public Attribute (String type, String name) {
	    this.type = type;
	    this.name = name;
    }
    
    public String getName(){return this.name;}
    public String getType(){return this.type;}
    
    public void setName(String name){this.name=name;}
    
    public String toString(){
  	  return "(type: "+this.type+" name: "+this.name+")";
    }  
}