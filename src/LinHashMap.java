
/************************************************************************************
 * @file LinHashMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import static java.lang.System.out;
import java.util.*;

/************************************************************************************
 * This class provides hash maps that use the Linear Hashing algorithm.
 * A hash table is created that is an array of buckets.
 */
public class LinHashMap <K, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, Map <K, V>
{
    /** The number of slots (for key-value pairs) per bucket.
     */
    private static final int SLOTS = 4;

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines buckets that are stored in the hash table.
     */
    private class Bucket
    {
        int    nKeys;
        K []   key;
        V []   value;
        Bucket next;

        @SuppressWarnings("unchecked")
        Bucket (Bucket n)
        {
            nKeys = 0;
            key   = (K []) Array.newInstance (classK, SLOTS);
            value = (V []) Array.newInstance (classV, SLOTS);
            next  = n;
        } // constructor
    } // Bucket inner class

    /** The list of buckets making up the hash table.
     */
    private final List <Bucket> hTable;

    /** The modulus for low resolution hashing
     */
    private int mod1;

    /** The modulus for high resolution hashing
     */
    private int mod2;

    /** Counter for the number buckets accessed (for performance testing).
     */
    private int count = 0;

    /** The index of the next bucket to split.
     */
    private int split = 0;

    /********************************************************************************
     * Construct a hash table that uses Linear Hashing.
     * @param classK    the class for keys (K)
     * @param classV    the class for keys (V)
     * @param initSize  the initial number of home buckets (a power of 2, e.g., 4)
     */
    public LinHashMap (Class <K> _classK, Class <V> _classV , int initSize)
    {
        classK = _classK;
        classV = _classV;
        hTable = new ArrayList <> ();
        mod1   = initSize;
        mod2   = 2 * mod1;

        for(int x=0;x<initSize;x++) hTable.add(new Bucket(null));
    } // constructor

    /********************************************************************************
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        Set <Map.Entry <K, V>> enSet = new HashSet <> ();
        for (int i = 0; i < hTable.size(); i++) {
            for (Bucket b = hTable.get(i); b != null; b = b.next) {
                for(int k = 0; k< b.nKeys; k++){
                     enSet.add (new AbstractMap.SimpleEntry <K, V> (b.key[k], b.value[k]));
                }
            } // for
        } // for

        return enSet;
    } // entrySet

    /********************************************************************************
     * Given the key, look up the value in the hash table.
     * @param key  the key used for look up
     * @return  the value associated with the key
     */
    public V get (Object key)
    {
        int i = h (key);
        if(i<split){
          i = h2(key);
        }
        //  T O   B E   I M P L E M E N T E D

        for (Bucket b = hTable.get(i); b != null; b = b.next) {
            count++;
            for(int k = 0; k< b.nKeys; k++){
                if (b.key[k].equals(key)) return b.value[k];
            } //for k
        } // for

        return null;
    } // get

    /********************************************************************************
     * Put the key-value pair in the hash table.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null (not the previous value)
     */
    public V put (K key, V value)
    {
    		int i = h (key);
        if(i<split){
          i = h2(key);
        }
        out.println ("LinearHashMap.put: key = " + key + ", h() = " + i + ", value = " + value);

        Bucket temp = hTable.get(i);

        if(temp.nKeys<SLOTS){
          temp.key[temp.nKeys]=key;
          temp.value[temp.nKeys]=value;
          temp.nKeys++;
        }else{
          //out.println("Then Split!");
          hTable.add(new Bucket(null));
          while(temp.next != null){
            temp = temp.next;
          }
          if(temp.nKeys < SLOTS){
                temp.key[temp.nKeys] = key;
                temp.value[temp.nKeys] = value;
                temp.nKeys++;
            }else{
                temp.next = new Bucket(null);
                temp = temp.next;
                temp.key[temp.nKeys]=key;
                temp.value[temp.nKeys]=value;
                temp.nKeys++;
            }
            int numKeys = 0;
            for(int y =0; y<hTable.size();y++){
                Bucket bkt = hTable.get(y);
                do{
                    numKeys = numKeys + bkt.nKeys;
                    bkt=bkt.next;
                }while(bkt !=null);
            }
            double alpha = ((double)numKeys)/(SLOTS * mod1);
            if(alpha >= 1){//load factor
                Bucket temp2 = new Bucket(null);//replace the split
                Bucket temp3 = new Bucket(null);//the new bucket
                temp = hTable.get(split);//the bucket to split
                for(Bucket b = temp; b != null; b = b.next){
                  for(int p=0; p<b.nKeys; p++){
                      int z = h2(b.key[p]);
                      if(z == split){
                          if(temp2.next ==null ||temp2.nKeys==SLOTS){
                              temp2.next = new Bucket(null);
                              temp2 = temp2.next;
                          }
                          temp2.key[temp2.nKeys] = b.key[p];
                          temp2.value[temp2.nKeys] = b.value[p];
                          //out.println("What is thisL "+temp2.nKeys + " On P= " + p);
                          temp2.nKeys++;
                          temp2.next = new Bucket(null);
                          //out.println("\tSplit:\t\tKey: " + b.key[p] + " And the value of :" + b.value[p] + " nKeys: "+temp2.nKeys);

                          hTable.set(split,temp2);
                      }else{
                          if(temp3.next == null|| temp3.nKeys==SLOTS){
                              temp3.next = new Bucket(null);
                              temp3 = temp3.next;
                          }
                          temp3.key[temp3.nKeys] = b.key[p];
                          temp3.value[temp3.nKeys] = b.value[p];
                          //out.println("What is thisL "+temp3.nKeys);
                          temp3.nKeys++;
                          temp3.next = new Bucket(null);
                          out.println("Z: " + z +" Key: "+ b.key[p] + " table size: " + hTable.size());
                          if(z<hTable.size()){
                        	  hTable.set(z,temp3);
                          }else{
                        	  hTable.set(z-hTable.size(),temp3);
                          }
                          //out.println("\t\t\tKey: " + b.key[p] + " And the value of :" + b.value[p]+ " nKeys: "+temp3.nKeys);
                      }

                  }//for
                }// Goes through the overflow if there is one.
                //hTable.set(split,temp2);
                /*
                K[] what = temp2.key;
                K[] ok = temp3.key;
                out.println("Temp2.nKeys: "+ temp2.nKeys);
                for(K t:what){out.print(t+"\t");}
                out.println();
                out.println("Temp3.nKeys: "+ temp3.nKeys);
                for(K t:ok){out.print(t+"\t");}
                out.println();
                */
                if(split == mod1 -1 ){
                    mod1= mod1*2;
                    mod2= mod1*2;
                    split=0;
                }else{
                    split++;
                }
              }//else
        }//split else
        return null;
        /*
        if(key == null || value == null)
        		return null;
        while(hTable.size()< i){
        		add4buckets();
        }//while
        if(hTable.get(i).nKeys == 4){
	        	hTable.add(i,new Bucket(hTable.remove(i)));
	        	hTable.get(i).key[hTable.get(i).nKeys]= key;
	        	hTable.get(i).value[hTable.get(i).nKeys]=value;
	        	hTable.get(i).nKeys++;
        }else{
	        	hTable.get(i).key[hTable.get(i).nKeys] = key;
	        	hTable.get(i).value[hTable.get(i).nKeys]= value;
	        	hTable.get(i).nKeys++;
        	}
        return null;
        */
      }

    /********************************************************************************
     * Return the size (SLOTS * number of home buckets) of the hash table.
     * @return  the size of the hash table
     */
    public int size ()
    {
        return SLOTS * (mod1 + split);
    } // size

    /********************************************************************************
     * Print the hash table.
     */
    private void print ()
    {

        out.println ("Hash Table (Linear Hashing)" + "Split at-----> " + split + " Mod1: " + mod1 + " Mod2: " + mod2);
        out.println ("-------------------------------------------");

        for (int i = 0; i < hTable.size(); i++) {
            out.print (i + ":\t");
            boolean notFirst = false;

            for (Bucket b = hTable.get(i); b != null; b = b.next) {
                if (notFirst) out.print ("--> ");
                for(int k = 0; k< b.nKeys; k++){
                    out.print ("[ " + b.key[k] + ":" + b.value[k] + " ] ");
                    notFirst = true;
                }
            } // for

            out.println ();
        	}//for
        out.println ("-------------------------------------------");

    } // print

    /********************************************************************************
     * Hash the key using the low resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h (Object key)
    {
      int k = key.hashCode () % mod1;
      if(k<0){
        return k+mod1;
      }else return k;
    } // h

    /********************************************************************************
     * Hash the key using the high resolution hash function.
     * @param key  the key to hash
     * @return  the location of the bucket chain containing the key-value pair
     */
    private int h2 (Object key)
    {
      int k = key.hashCode () % mod2;
      if(k<0){
        return k+mod2;
      }else return k;
    } // h2

   private void add4buckets()
   {
	   for(int k = 0; k< 4; k++)
   		hTable.add(new Bucket(null));
   }
    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args [0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {

        int totalKeys    = 10000;
        boolean RANDOMLY = true;

        LinHashMap <Integer, Integer> ht = new LinHashMap <> (Integer.class, Integer.class, 4);
        if (args.length == 1) totalKeys = Integer.valueOf (args [0]);
        int[] random = new int[totalKeys];
        int ran;
        if (RANDOMLY) {
            Random rng = new Random ();
            for (int i = 1; i <= totalKeys; i += 2){
              ran = rng.nextInt (2 * totalKeys);
              ht.put (ran, i * i);
              random[i-1] = ran;
              //ht.print ();
            }
        } else {
            for (int i = 1; i <= totalKeys; i += 1) {
              ht.put (i, i * i);
              //ht.print ();
            }
        } // if

        ht.print ();
        
        if(RANDOMLY){
          for(int x:random)out.println ("key = " + x + " value = " + ht.get (x));
        }else{
          for (int i = 0; i <= totalKeys; i++) {
              out.println ("key = " + i + " value = " + ht.get (i));
          } // for
        }
        out.println ("-------------------------------------------");
        out.println ("Average number of buckets accessed = " + ht.count / (double) totalKeys);
    } // main

} // LinHashMap class
