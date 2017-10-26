
/****************************************************************************************
 * @file  Table.java
 *
 * @author   John Miller
 *
 * @author Matt Zhou, Richard Deng, Hoonjae Won, Daniel Snyder, James Burgess
 * boop
 */

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.lang.Boolean.*;
import static java.lang.System.out;

/****************************************************************************************
 * This class implements relational database tables (including attribute names, domains
 * and a list of tuples.  Five basic relational algebra operators are provided: project,
 * select, union, minus and join.  The insert data manipulation operator is also provided.
 * Missing are update and delete data manipulation operators.
 */
public class Table
       implements Serializable
{
    /** Relative path for storage directory
     */
    private static final String DIR = "store" + File.separator;

    /** Filename extension for database files
     */
    private static final String EXT = ".dbf";

    /** Counter for naming temporary tables.
     */
    private static int count = 0;

    /** Table name.
     */
    private final String name;

    /** Array of attribute names.
     */
    private final String [] attribute;

    /** Array of attribute domains: a domain may be
     *  integer types: Long, Integer, Short, Byte
     *  real types: Double, Float
     *  String types: Character, String
     */
    private final Class [] domain;

    /** Collection of tuples (data storage).
     */
    private final List <Comparable []> tuples;

    /** Primary key.
     */
    private final String [] key;

    /** Index into tuples (maps key to tuple number).
     */
    private final Map <KeyType, Comparable []> index;

    /** The supported map types.
     */
    private enum MapType { NO_MAP, TREE_MAP, LINHASH_MAP, BPTREE_MAP }

    /** The map type to be used for indices.  Change as needed.
     */
    //private static final MapType mType = MapType.TREE_MAP;
    private static final MapType mType = MapType.LINHASH_MAP;
    //private static final MapType mType = MapType.BPTREE_MAP;

    /************************************************************************************
     * Make a map (index) given the MapType.
     */
    private static Map <KeyType, Comparable []> makeMap ()
    {
        switch (mType) {
        //case TREE_MAP:    return new TreeMap <> ();
        case LINHASH_MAP: return new LinHashMap <> (KeyType.class, Comparable [].class, 5);
        case TREE_MAP:    return new TreeMap <> ();
        //case LINHASH_MAP: return new LinHashMap <> (KeyType.class, Comparable [].class);
        case BPTREE_MAP:  return new BpTreeMap <> (KeyType.class, Comparable [].class);
        default:          return null;
        } // switch
    } // makeMap

    //-----------------------------------------------------------------------------------
    // Constructors
    //-----------------------------------------------------------------------------------

    /************************************************************************************
     * Construct an empty table from the meta-data specifications.
     *
     * @param _name       the name of the relation
     * @param _attribute  the String containing attributes names
     * @param _domain     the String containing attribute domains (data types)
     * @param _key        the primary key
     */
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = new ArrayList <> (); //or FileList
        index     = makeMap ();

    } // primary constructor

    /************************************************************************************
     * Construct a table from the meta-data specifications and data in _tuples list.
     *
     * @param _name       the name of the relation
     * @param _attribute  the String containing attributes names
     * @param _domain     the String containing attribute domains (data types)
     * @param _key        the primary key
     * @param _tuples     the list of tuples containing the data
     */
    public Table (String _name, String [] _attribute, Class [] _domain, String [] _key,
                  List <Comparable []> _tuples)
    {
        name      = _name;
        attribute = _attribute;
        domain    = _domain;
        key       = _key;
        tuples    = _tuples;
        index     = makeMap ();
    } // constructor

    /************************************************************************************
     * Construct an empty table from the raw String specifications.
     *
     * @param _name       the name of the relation
     * @param attributes  the String containing attributes names
     * @param domains     the String containing attribute domains (data types)
     * @param _key        the primary key
     */
    public Table (String _name, String attributes, String domains, String _key)
    {
        this (_name, attributes.split (" "), findClass (domains.split (" ")), _key.split(" "));

        out.println ("DDL> create table " + name + " (" + attributes + ")");
    } // constructor

    //----------------------------------------------------------------------------------
    // Public Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Project the tuples onto a lower dimension by keeping only the given attributes.
     * Check whether the original key is included in the projection.
     *
     * #usage movie.project ("title year studioNo")
     *
     * @param attributes  the attributes to project onto
     * @return  a table of projected tuples
     */
    public Table project (String attributes)
    {
        out.println ("RA> " + name + ".project (" + attributes + ")");
        String [] attrs     = attributes.split (" ");
        Class []  colDomain = extractDom (match (attrs), domain);
        String [] newKey    = (Arrays.asList (attrs).containsAll (Arrays.asList (key))) ? key : attrs;

        List <Comparable []> rows = new ArrayList <> ();

        /*
        for each element in this.tuple
    		check the attribute of the tuple
    		see if the tuple is in the column wanted
    		throw out the rest of the row
        */
        for(Comparable[] tuple1 : this.tuples){
            rows.add(this.extract(tuple1, attrs));
        }

        return new Table (name + count++, attrs, colDomain, newKey, rows);
    } // project

    /************************************************************************************
     * Select the tuples satisfying the given predicate (Boolean function).
     *
     * #usage movie.select (t -> t[movie.col("year")].equals (1977))
     *
     * @param predicate  the check condition for tuples
     * @return  a table with tuples satisfying the predicate
     */
    public Table select (Predicate <Comparable []> predicate)
    {
        out.println ("RA> " + name + ".select (" + predicate + ")");

        return new Table (name + count++, attribute, domain, key,
                   tuples.stream ().filter (t -> predicate.test (t))
                                   .collect (Collectors.toList ()));
    } // select

    /************************************************************************************
     * Select the tuples satisfying the given key predicate (key = value).  Use an index
     * (Map) to retrieve the tuple with the given key value.
     *
     * Find the tuple that corresponds to the key keyVal and return a table with all
     * of the tuples in the current Table that match the keyVal tuple.
     *
     * @param keyVal  the given key value
     * @return  a table with the tuple satisfying the key predicate
     */
    public Table select (KeyType keyVal)
    {
        out.println ("RA> " + name + ".select (" + keyVal + ")");

        List <Comparable []> rows = new ArrayList <> ();
        Comparable[] target = index.get(keyVal);
        for( Comparable[] tuple1: this.tuples){
        	if(compareTuples(target,tuple1))
        		rows.add(tuple1);
        }
        return new Table (name + count++, attribute, domain, key, rows);
    } // select

    /**
     * Select the tuples satisfying the given key predicate (keyval1 <= value < keyval2).
     * Use an B+ Tree index (SortedMap) to retrieve the tuples with keys in the given range.
     *
     * @param keyVal1  the given lower bound for the range (inclusive)
     * @param keyVal2  the given upper bound for the range (exclusive)
     * @return  a table with the tuples satisfying the key predicate
     */
    public Table select (KeyType keyVal1, KeyType keyVal2)
    {
        out.println ("RA> " + name + ".select between (" + keyVal1 + ") and " + keyVal2);

        List <Comparable []> rows = new ArrayList <> ();
        Set <Map.Entry<KeyType, Comparable []>> what = index.entrySet().stream()
        .filter(p -> keyVal1.compareTo(p.getKey()) <=0)
        .filter(p -> keyVal2.compareTo(p.getKey()) >0)
        .collect(Collectors.toSet());
        for (Map.Entry<KeyType, Comparable []> entry : what) {
          rows.add(index.get(entry.getKey()));
        }
        return new Table (name + count++, attribute, domain, key, rows);
    } // range_select

    /************************************************************************************
     * Union this table and table2.  Check that the two tables are compatible.
     *
     * #usage movie.union (show)
     *
     * Made sure that Table1 and Table2 were compatible. Afterwards I created
     * a for each loop to place all the tuples in Table1 into the new table.
     * Then for the second table, I looped through all the tuples in Table2 and
     * a nested loop for the tuples in the new table and compared using the helper
     * method we created. This is to ensure there are no duplicates.
     *
     * @param table2  the rhs table in the union operation
     * @return  a table representing the union
     */
    public Table union (Table table2)
    {
        out.println ("RA> " + name + ".union (" + table2.name + ")");
        if (! compatible (table2)) return null;
        List <Comparable []> rows = new ArrayList <> ();
        boolean notDuplicate = true;
        for(Comparable []r : this.tuples){ //add first table to rows
        		rows.add(r);
        }//r for
        for(Comparable []t : table2.tuples){
        		notDuplicate = true;
        			for(int i =0; i<rows.size();i++){ //for looping through rows
        				if(compareTuples(rows.get(i),t)){ //if equal break loop
        					notDuplicate = false;
        					break;
        				}
        			}
        			if(notDuplicate){
        				rows.add(t); //if notDuplicate add tuples
        			}

        }//t-tuple for loop

        return new Table (name + count++, attribute, domain, key, rows);
    } // union

    /************************************************************************************
     * Take the difference of this table and table2.  Check that the two tables are
     * compatible.
     *
     * #usage movie.minus (show)
     *
     * Use a for each loop to compare each tuple in current table to all tuples in table 2.
     * If any tuple matches are found it is flagged as part of the intersection of the
     * two tuples. The final rows list will have all the tuples in the current table that aren't in
     * the intersection.
     *
     * @param table2  The rhs table in the minus operation
     * @return  a table representing the difference
     */
    public Table minus (Table table2)
    {
        out.println ("RA> " + name + ".minus (" + table2.name + ")");
        if (! compatible (table2)) {
        	return null;
        }
        List <Comparable []> rows = new ArrayList <> ();
        boolean intersected;
        for( Comparable[] tuple1: this.tuples){
            intersected = false;
            for( Comparable[] tuple2: table2.tuples){
                if(compareTuples(tuple1, tuple2))
                    intersected = true;
            }
            if(!intersected)
                rows.add(tuple1);

        }
        return new Table (name + count++, attribute, domain, key, rows);
    } // minus

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Tuples from both tables
     * are compared requiring attributes1 to equal attributes2.  Disambiguate attribute
     * names by append "2" to the end of any duplicate attribute name.  Implement using
     * a Nested Loop Join algorithm.
     *
     *I created sevveral for loops to loop through the two String[] attribute1 and
     *attribute2 and checked through the tuples on the two tables with the use of col()
     *because of how col works, I can compared certain attributes on the tables, which
     *allows me to create the equi-join result into rows.
     *
     * #usage movie.join ("studioNo", "name", studio)
     *
     * @param attribute1  the attributes of this table to be compared (Foreign Key)
     * @param attribute2  the attributes of table2 to be compared (Primary Key)
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (String attributes1, String attributes2, Table table2)
    {
        out.println ("RA> " + name + ".join (" + attributes1 + ", " + attributes2 + ", "
                                               + table2.name + ")");

        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");

        List <Comparable []> rows = new ArrayList <> ();
//        rows = tuples.stream().filter(a -> ) tuples.collect(Collectors.toList());

        for(String t:t_attrs){
          for(String u:u_attrs){
            for(Comparable[] ttup:this.tuples){
              for(Comparable[] utup:table2.tuples){
                if(ttup[this.col(t)]==utup[table2.col(u)]){
                  rows.add(ArrayUtil.concat(ttup,utup));
                }
              }
            }
          }
        }

        return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
        									 ArrayUtil.concat (domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Same as above, but implemented
     * using an Index Join algorithm.
     *
     * @param attribute1  the attributes of this table to be compared (Foreign Key)
     * @param attribute2  the attributes of table2 to be compared (Primary Key)
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table i_join (String attributes1, String attributes2, Table table2)
    {
    	out.println ("RA> " + name + ".i_join (" + attributes1 + ", " + attributes2 + ", "
                + table2.name + ")");

    	String [] u_attrs = attributes1.split(" ");
    	String [] t_attrs = attributes2.split(" ");

    	List <Comparable []> rows = new ArrayList<>();

    	//just check to make sure
    	if(u_attrs.length != t_attrs.length){
    		out.println("Please use attributes that are equivalent");
    		return null;
    	}

    	for(int i = 0; i < u_attrs.length; i++){
    		Comparable[] ttup = tuples.get(i);

        out.println("ttup");
        for(Comparable t:ttup){
          out.println(t);
        }

        KeyType keyVal = new KeyType(extract(ttup,u_attrs)); //this will be our foreign key to compare
    		Comparable[] utup = table2.index.get(keyVal);

        out.println("utup:");
        for(Comparable u:utup){
          out.println(u);
        }

        if(ttup==utup){
          out.println(ttup + " is the same as " + utup + "?");
          rows.add(ArrayUtil.concat(ttup,utup));
        }
        out.println(ttup + " is the same as " + utup + "?");
      }


        return new Table((name + count++), ArrayUtil.concat(attribute, table2.attribute),
        								ArrayUtil.concat(domain, table2.domain), key, rows);
    } // i_join

    /************************************************************************************
     * Join this table and table2 by performing an "equi-join".  Same as above, but implemented
     * using a Hash Join algorithm.
     *
     * @param attribute1  the attributes of this table to be compared (Foreign Key)
     * @param attribute2  the attributes of table2 to be compared (Primary Key)
     * @param table2      the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table h_join (String attributes1, String attributes2, Table table2)
    {
    	out.println ("RA> " + name + ".h_join (" + attributes1 + ", " + attributes2 + ", "
                                               + table2.name + ")");

        String [] t_attrs = attributes1.split (" ");
        String [] u_attrs = attributes2.split (" ");

        List <Comparable []> rows = new ArrayList <> ();
        LinHashMap <Comparable[], Comparable[]> hmap = new LinHashMap(Comparable[].class, Comparable[].class,4);
        if(t_attrs.length != u_attrs.length){
    		out.println("Please use attributes that are equivalent");
    		return null;
    	}

	    if(this.tuples.size() >= table2.tuples.size()) {
	         for( Comparable[] tuple1: this.tuples){
	         	hmap.put(extract(tuple1,t_attrs),tuple1) ;

	         }
	         for(Comparable[] tuple2: table2.tuples){
	        	Comparable[] utup = table2.extract(tuple2,u_attrs);
	         	if(hmap.get(utup)!= null)
	         	{
	         		rows.add(ArrayUtil.concat(hmap.get(utup),tuple2));

	         	}
	         }
	    }
	    else
	    {
		   for( Comparable[] tuple1: table2.tuples){
	        	hmap.put(extract(tuple1,t_attrs),tuple1) ;

	        }
	        for(Comparable[] tuple2: this.tuples){
	       	Comparable[] utup = table2.extract(tuple2,u_attrs);
	        	if(hmap.get(utup)!= null)
	        	{
	        		rows.add(ArrayUtil.concat(hmap.get(utup),tuple2));

	        	}
	        }
		}
        return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
        									 ArrayUtil.concat (domain, table2.domain), key, rows);
    } // h_join

    /************************************************************************************
     * Join this table and table2 by performing an "natural join".  Tuples from both tables
     * are compared requiring common attributes to be equal.  The duplicate column is also
     * eliminated.
     *
     * #usage movieStar.join (starsIn)
     *
     *For natural join, disregarding duplicates for now, thos method loops through
     *the tuples from each table and compares each attribute from one tuple in this.table
     *with another attribute in another tuple in table2. This way, the result of this
     *would ensure that the resulting table will have a new table of tuples that have
     *the sum of the attributes from both tables with only the common tuples.
     *
     * @param table2  the rhs table in the join operation
     * @return  a table with tuples satisfying the equality predicate
     */
    public Table join (Table table2)
    {
    		boolean cont = true;
    		Comparable a1, a2;
    		int index1 = 0;
    	  	int index2 = 0;
        out.println ("RA> " + name + ".join (" + table2.name + ")");

//        this.print();
//        table2.print();

        List <Comparable []> rows = new ArrayList <> ();

	    for(Comparable[] ttup:this.tuples){
	      for(Comparable[] utup:table2.tuples){
	    	  	index1 = 0;
	    	  	index2 = 0;
	    	  	cont = true;
	        while(cont){
	        	  a1 = ttup[index1];
	        	  a2 = utup[index2];
	        	  if(a1 == a2) {
	        		  rows.add(ArrayUtil.concat(ttup, utup));
	        		  cont = false;
	        	  }else {
	        		  if(index2 >= utup.length-1) {
	        			  if(index1 < ttup.length-1) {
		        			  index1++;
		        			  index2 = 0;
		        		  }else {
		        			  cont = false;
		        		  }
	        		  }else if(index2 < utup.length-1){
	        			  index2++;
	        		  }else {
	        			  cont = false;
	        		  }
	        	  }
	        }
	      }
	    }
        return new Table (name + count++, ArrayUtil.concat (attribute, table2.attribute),
                                          ArrayUtil.concat (domain, table2.domain), key, rows);
    } // join

    /************************************************************************************
     * Return the column position for the given attribute name.
     *
     * @param attr  the given attribute name
     * @return  a column position
     */
    public int col (String attr)
    {
        for (int i = 0; i < attribute.length; i++) {
           if (attr.equals (attribute [i])) return i;
        } // for

        return -1;  // not found
    } // col

    /************************************************************************************
     * Insert a tuple to the table.
     *
     * #usage movie.insert ("'Star_Wars'", 1977, 124, "T", "Fox", 12345)
     *
     * @param tup the array of attribute values forming the tuple
     * @return  whether insertion was successful
     */
    public boolean insert (Comparable [] tup)
    {
        out.println ("DML> insert into " + name + " values ( " + Arrays.toString (tup) + " )");

        if (typeCheck (tup)) {
            tuples.add (tup);
            Comparable [] keyVal = new Comparable [key.length];
            int []        cols   = match (key);
            for (int j = 0; j < keyVal.length; j++) keyVal [j] = tup [cols [j]];
            if (mType != MapType.NO_MAP) index.put (new KeyType (keyVal), tup);
            return true;
        } else {
            return false;
        } // if
    } // insert

    /************************************************************************************
     * Get the name of the table.
     *
     * @return  the table's name
     */
    public String getName ()
    {
        return name;
    } // getName

    /************************************************************************************
     * Print this table.
     */
    public void print ()
    {
        out.println ("\n Table " + name);
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        out.print ("| ");
        for (String a : attribute) out.printf ("%15s", a);
        out.println (" |");
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
        for (Comparable [] tup : tuples) {
            out.print ("| ");
            for (Comparable attr : tup) out.printf ("%15s", attr);
            out.println (" |");
        } // for
        out.print ("|-");
        for (int i = 0; i < attribute.length; i++) out.print ("---------------");
        out.println ("-|");
    } // print

    /************************************************************************************
     * Print this table's index (Map).
     */
    public void printIndex ()
    {
        out.println ("\n Index for " + name);
        out.println ("-------------------");
        if (mType != MapType.NO_MAP) {
            for (Map.Entry <KeyType, Comparable []> e : index.entrySet ()) {
                out.println (e.getKey () + " -> " + Arrays.toString (e.getValue ()));
            } // for
        } // if
        out.println ("-------------------");
    } // printIndex

    /************************************************************************************
     * Load the table with the given name into memory.
     *
     * @param name  the name of the table to load
     */
    public static Table load (String name)
    {
        Table tab = null;
        try {
            ObjectInputStream ois = new ObjectInputStream (new FileInputStream (DIR + name + EXT));
            tab = (Table) ois.readObject ();
            ois.close ();
        } catch (IOException ex) {
            out.println ("load: IO Exception");
            ex.printStackTrace ();
        } catch (ClassNotFoundException ex) {
            out.println ("load: Class Not Found Exception");
            ex.printStackTrace ();
        } // try
        return tab;
    } // load

    /************************************************************************************
     * Save this table in a file.
     */
    public void save ()
    {
        try {
            ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream (DIR + name + EXT));
            oos.writeObject (this);
            oos.close ();
        } catch (IOException ex) {
            out.println ("save: IO Exception");
            ex.printStackTrace ();
        } // try
    } // save

    /************************************************************************************
     * pack this this table in a file.
     */
    public byte [] pack(Comparable [] t)
    {
        byte [] rec = new byte [tupleSize()];
        byte [] b = null;
        int s = 0;
        int x = 0;

        for(int i = 0; i < domain.length; i++)
        {
            switch(domain[i].getName())
            {
                case "java.lang.Byte":
                    b = new byte[] {(byte) t[i]};
                    s = 1;
                    break;
                case "java.lang.Short":
                    b = Conversions.short2ByteArray((Short) t[i]);
                    s = 2;
                    break;
                case "java.lang.Integer":
                    b = Conversions.int2ByteArray((Integer) t[i]);
                    s = 4;
                    break;
                case "java.lang.Long":
                    b = Conversions.long2ByteArray((Long) t[i]);
                    s = 8;
                    break;
                case "java.lang.Float":
                    b = Conversions.float2ByteArray((Float) t[i]);
                    s = 4;
                    break;
                case "java.lang.Double":
                    b = Conversions.double2ByteArray((Double) t[i]);
                    s = 8;
                    break;
                case "java.lang.Character":
                    String strTemp = Character.toString((char) t[i]);
                    byte [] byteTemp = strTemp.getBytes();
                    b = new byte[2];
                    b[0] = byteTemp[0];
                    b[1] = 0;
                    s = 2;
                    break;
                case "java.lang.String":
                    byte [] temp = new byte[64];
                    byte [] temp1 = ((String) t[i]).getBytes();
                    for(int j = 0; j < temp1.length; j++)
                    {
                        temp[j] = temp1[j];
                    }
                    for(int j = temp1.length; j < 64; j++)
                    {
                        temp[j] = 0;
                    }
                    b = temp;
                    s = 64;
                    break;
            }// switch
            if(b == null)
            {
                out.println("Table.pack: byte array is null");
                return null;
            }
            for(int j = 0; j < s; j++)
            {
                rec[i++] = b[j];
            }
        }// for
        return rec;
    }// pack

    public Comparable [] unpack (byte [] t)
    {
        Comparable [] ret = new Comparable[domain.length];

        ByteBuffer buff = ByteBuffer.wrap(t);
        buff.order(ByteOrder.BIG_ENDIAN);

        for(int i = 0; i < domain.length; i++)
        {
            switch(domain[i].getName())
            {
                case "java.lang.Byte":
                    ret[i] = buff.get();
                    break;
                case "java.lang.Short":
                    ret[i] = buff.getShort();
                    break;
                case "java.lang.Integer":
                    ret[i] = buff.getInt();
                    break;
                case "java.lang.Long":
                    ret[i] = buff.getLong();
                    break;
                case "java.lang.Float":
                    ret[i] = buff.getFloat();
                    break;
                case "java.lang.Double":
                    ret[i] = buff.getDouble();
                    break;
                case "java.lang.Character":
                    ret[i] = (char)buff.get();
                    buff.get();
                    break;
                case "java.lang.String":
                     byte [] b = new byte[64];
                     for(int j = 0; j < 64; j++)
                     {
                        b[j] = buff.get();
                     }
                     String temp = new String(b).trim();
                     ret[i] = temp;
                     break;
            }// switch
        }// for
        return ret;
    }

    private int tupleSize ()
    {
        int ret = 0;

        for(int i = 0; i < domain.length; i++)
        {
            ret += addDomainLength(domain[i].getName());
        }

        return ret;
    }

    private int addDomainLength(String str)
    {
        switch (str){
            case "java.lang.Byte":
                return 1;
            case "java.lang.Short":
                return 2;
            case "java.lang.Integer":
                return 4;
            case "java.lang.Long":
                return 8;
            case "java.lang.Float":
                return 4;
            case "java.lang.Double":
                return 8;
            case "java.lang.Charachter":
                return 2;
            case "java.lang.String":
                return 64;
            default:
                return 0;
        }
    }
    //----------------------------------------------------------------------------------
    // Private Methods
    //----------------------------------------------------------------------------------

    /************************************************************************************
     * Determine whether the two tables (this and table2) are compatible, i.e., have
     * the same number of attributes each with the same corresponding domain.
     *
     * @param table2  the rhs table
     * @return  whether the two tables are compatible
     */
    private boolean compatible (Table table2)
    {
        if (domain.length != table2.domain.length) {
            out.println ("compatible ERROR: table have different arity");
            return false;
        } // if
        for (int j = 0; j < domain.length; j++) {
            if (domain [j] != table2.domain [j]) {
                out.println ("compatible ERROR: tables disagree on domain " + j);
                return false;
            } // if
        } // for
        return true;
    } // compatible

    /************************************************************************************
     * Match the column and attribute names to determine the domains.
     *
     * @param column  the array of column names
     * @return  an array of column index positions
     */
    private int [] match (String [] column)
    {
        int [] colPos = new int [column.length];

        for (int j = 0; j < column.length; j++) {
            boolean matched = false;
            for (int k = 0; k < attribute.length; k++) {
                if (column [j].equals (attribute [k])) {
                    matched = true;
                    colPos [j] = k;
                } // for
            } // for
            if ( ! matched) {
                out.println ("match: domain not found for " + column [j]);
            } // if
        } // for

        return colPos;
    } // match

    /************************************************************************************
     * Extract the attributes specified by the column array from tuple t.
     *
     * @param t       the tuple to extract from
     * @param column  the array of column names
     * @return  a smaller tuple extracted from tuple t
     */
    private Comparable [] extract (Comparable [] t, String [] column)
    {
        Comparable [] tup = new Comparable [column.length];
        int [] colPos = match (column);
        for (int j = 0; j < column.length; j++) tup [j] = t [colPos [j]];
        return tup;
    } // extract

    /************************************************************************************
     * Check the size of the tuple (number of elements in list) as well as the type of
     * each value to ensure it is from the right domain.
     *
     * @param t  the tuple as a list of attribute values
     * @return  whether the tuple has the right size and values that comply
     *          with the given domains
     */
    private boolean typeCheck (Comparable [] t)
    {
		Comparable dbl = (double)0.0;
		Comparable flt = (float)0.0;
    	if(t.length > domain.length)
		{
			return false;
		}
		Class [] tClasses = extractDom(match(this.attribute), this.domain);
		for(int i = 0; i < t.length; i++)
		{
			if(t[i].getClass() != tClasses[i]) {
				if(!((t[i].getClass() == flt.getClass()
						&& tClasses[i] == dbl.getClass())
						|| (t[i].getClass() == dbl.getClass()
						&& tClasses[i] == flt.getClass())))
				{
    				out.println("Cannot insert, " + t[i] + "!");
    				out.println("Inserted Class type is (" + t[i].getClass() + "),");
    				out.println("Expected Class type is (" + tClasses[i] + ").");
    				return false;
				}
			}
    	}
        return true;
    } // typeCheck

    /************************************************************************************
     * Find the classes in the "java.lang" package with given names.
     *
     * @param className  the array of class name (e.g., {"Integer", "String"})
     * @return  an array of Java classes
     */
    private static Class [] findClass (String [] className)
    {
        Class [] classArray = new Class [className.length];

        for (int i = 0; i < className.length; i++) {
            try {
                classArray [i] = Class.forName ("java.lang." + className [i]);
            } catch (ClassNotFoundException ex) {
                out.println ("findClass: " + ex);
            } // try
        } // for

        return classArray;
    } // findClass

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param colPos the column positions to extract.
     * @param group  where to extract from
     * @return  the extracted domains
     */
    private Class [] extractDom (int [] colPos, Class [] group)
    {
        Class [] obj = new Class [colPos.length];

        for (int j = 0; j < colPos.length; j++) {
            obj [j] = group [colPos [j]];
        } // for

        return obj;
    } // extractDom

    /************************************************************************************
     * Extract the corresponding domains.
     *
     * @param tuple1 the first tuple to compare
     * @param tuple2 the second tuple to compare
     * @return boolean if the two tuples are identical
     */
    private boolean compareTuples (Comparable[] tuple1 , Comparable[] tuple2)
    {
        if(tuple1.length != tuple2.length)
            return false; // length of tuples dont match so they cant be the same
        for(int i = 0; i< tuple1.length; i ++ )
        {
            if(tuple1[i] != tuple2[i])
                return false;  //an element is not the same
        }

        return true; //tuples are identical!
    } //compareTuples
} // Table class
