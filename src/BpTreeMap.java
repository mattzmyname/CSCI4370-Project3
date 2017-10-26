
/************************************************************************************
 * @file BpTreeMap.java
 *
 * @author  John Miller
 */

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;

import static java.lang.Math.ceil;
import static java.lang.System.out;

/************************************************************************************
 * The BpTreeMap class provides B+Tree maps.  B+Trees are used as multi-level index
 * structures that provide efficient access for both point queries and range queries.
 * All keys will be at the leaf level with leaf nodes linked by references.
 * Internal nodes will contain divider keys such that each divider key corresponds to
 * the largest key in its left subtree (largest left).  Keys in left subtree are "<=",
 * while keys in right subtree are ">".
 */
public class BpTreeMap <K extends Comparable <K>, V>
       extends AbstractMap <K, V>
       implements Serializable, Cloneable, SortedMap <K, V>
{
    /** The debug flag
     */
    private static final boolean DEBUG = true;

    /** The maximum fanout (number of children) for a B+Tree node.
     *  May wish to increase for better performance for Program 3.
     */
    private static final int ORDER = 5;

    /** The maximum fanout (number of children) for a big B+Tree node.
     */
    private static final int BORDER = ORDER + 1;

    /** The ceiling of half the ORDER.
     */
    private static final int MID = (int) ceil (ORDER / 2.0);

    /** The class for type K.
     */
    private final Class <K> classK;

    /** The class for type V.
     */
    private final Class <V> classV;

    /********************************************************************************
     * This inner class defines nodes that are stored in the B+tree map.
     */
    private class Node
    {
        boolean   isLeaf;                             // whether the node is a leaf
        int       nKeys;                              // number of active keys
        K []      key;                                // array of keys
        Object [] ref;                                // array of references/pointers

        /****************************************************************************
         * Construct a node.
         * @param p       the order of the node (max refs)
         * @param isLeaf  whether the node is a leaf
         */
        @SuppressWarnings("unchecked")
        Node (int p, boolean _isLeaf)
        {
            isLeaf = _isLeaf;
            nKeys  = 0;
            key    = (K []) Array.newInstance (classK, p-1);
            if (isLeaf) {
                ref = new Object [p];
            } else {
                ref = (Node []) Array.newInstance (Node.class, p);
            } // if
        } // constructor

        /****************************************************************************
         * Copy keys and ref from node n to this node.
         * @param n     the node to copy from
         * @param from  where in n to start copying from
         * @param num   the number of keys/refs to copy
         */
        void copy (Node n, int from, int num)
        {
            nKeys = num;
            for (int i = 0; i < num; i++) { key[i] = n.key[from+i]; ref[i] = n.ref[from+i]; }
            ref[num] = n.ref[from+num];
        } // copy

        /****************************************************************************
         * Find the "<=" match position in this node.
         * @param k  the key to be matched.
         * @return  the position of match within node, where nKeys indicates no match
         */
        int find (K k)
        {
            for (int i  = 0; i < nKeys; i++) if (k.compareTo (key[i]) <= 0) return i;
            return nKeys;
        } // find

        /****************************************************************************
         * Overriding toString method to print the Node. Prints out the keys.
         */
        @Override
        public String toString ()
        {
            return Arrays.deepToString (key);
        } // toString

    } // Node inner class

    /** The root of the B+Tree
     */
    private Node root;

    /** The first (leftmost) leaf in the B+Tree
     */
    private final Node firstLeaf;

    /** A big node to hold all keys and references/pointers before splitting
     */
    private final Node bn;

    /** Flag indicating whether a split at the level below has occurred that needs to be handled
     */
    private boolean hasSplit = false;

    /** The counter for the number nodes accessed (for performance testing)
     */
    private int count = 0;

    /** The counter for the total number of keys in the B+Tree Map
     */
    private int keyCount = 0;

    /********************************************************************************
     * Construct an empty B+Tree map.
     * @param _classK  the class for keys (K)
     * @param _classV  the class for values (V)
     */
    public BpTreeMap (Class <K> _classK, Class <V> _classV)
    {
        classK    = _classK;
        classV    = _classV;
        root      = new Node (ORDER, true);
        firstLeaf = root;
        bn        = new Node (BORDER, true);
    } // constructor

    /********************************************************************************
     * Return null to use the natural order based on the key type.  This requires the
     * key type to implement Comparable.
     */
    public Comparator <? super K> comparator ()
    {
        return null;
    } // comparator

    /*
     * Return a set containing all the entries as pairs of keys and values.
     * @return  the set view of the map
     */
    public Set <Map.Entry <K, V>> entrySet ()
    {
        return helperSet(root);
    } // entrySet

    /**
     * Recursive helper function for entrySet
     * @param node    the node to start from
     * @return set      the set that was added to recursively
     */
    //helper function because I can't do entrySet(Node n) for some reason...
    public Set <Map.Entry <K, V>> helperSet (Node n){
        Set <Map.Entry <K, V>> helpSet = new HashSet <> ();

         if(n.isLeaf){
             for(int i = 0; i<n.nKeys; i++){
                 helpSet.add(new AbstractMap.SimpleEntry<K, V>(n.key[i], (V)n.ref[i]));
             }//for
             return helpSet;
         }//if

         else{//else not leaf
            //out.println("nKeys: " + n.nKeys);
             for(int j = 0;j<n.ref.length;j++){
                 Node temp = (Node) n.ref[j];
                 if(temp != null)
                     helpSet.addAll(helperSet(temp));

             }//for
             return helpSet;

         }//else

    }//helperSet

    /********************************************************************************
     * Given the key, look up the value in the B+Tree map.
     * @param key  the key used for look up
     * @return  the value associated with the key or null if not found
     */
    @SuppressWarnings("unchecked")
    public V get (Object key)
    {
        return find ((K) key, root);
    } // get

    /********************************************************************************
     * Put the key-value pair in the B+Tree map.
     * @param key    the key to insert
     * @param value  the value to insert
     * @return  null, not the previous value for this key
     */
    public V put (K key, V value)
    {
        //insert (key, value, root);
        insert (key, value, root,null,0);
        return null;
    } // put

    /********************************************************************************
     * Return the first (smallest) key in the B+Tree map.
     * @return  the first key in the B+Tree map.
     */
    public K firstKey ()
    {
        return firstLeaf.key[0];
    } // firstKey

    /********************************************************************************
     * Return the last (largest) key in the B+Tree map.
     * @return  the last key in the B+Tree map.
     */
    public K lastKey ()
    {
        if(this.root.isLeaf){
            return(this.root.key[root.nKeys-1]);
        }else {
            Node n = this.root;
            while (!n.isLeaf) {
                n = (Node) n.ref[n.nKeys];
            }
            return n.key[n.nKeys - 1];
        }
    } // lastKey

    /********************************************************************************
     * Return the portion of the B+Tree map where key < toKey.
     * @return  the submap with keys in the range [firstKey, toKey)
     */
    public SortedMap <K,V> headMap (K toKey)
    {
        return subMap(firstKey(), toKey);
        //return null;
    } // headMap

    /********************************************************************************
     * Return the portion of the B+Tree map where fromKey <= key.
     * @return  the submap with keys in the range [fromKey, lastKey]
     */
    public SortedMap <K,V> tailMap (K fromKey)
    {
        return subMap(fromKey, lastKey());
        //return null;
    } // tailMap

    /********************************************************************************
     * Return the portion of the B+Tree map whose keys are between fromKey and toKey,
     * i.e., fromKey <= key < toKey.
     * @return  the submap with keys in the range [fromKey, toKey)
     */
    public SortedMap <K,V> subMap (K fromKey, K toKey)
    {
        BpTreeMap<K, V> map = new BpTreeMap(classK, classV);
        Set<Map.Entry<K,V>> entry = this.entrySet();
        //out.println("Entry: "+entry);

        //loop through the list of pairs and compare them with fromKey and toKey
        for(Entry<K, V> currPair : entry){
            //checks the "fromKey <= key < toKey" condition. If true then put into map
            //out.println(currPair.getKey());
            if(currPair.getKey().compareTo(fromKey) >= 0 && currPair.getKey().compareTo(toKey) < 0){
                map.put(currPair.getKey(), currPair.getValue());
            }//if

        }//for


        return map;
    } // subMap

    /********************************************************************************
     * Return the size (number of keys) in the B+Tree.
     * @return  the size of the B+Tree
     */
    public int size ()
    {
        return keyCount;
    } // size

    /********************************************************************************
     * Print the B+Tree using a pre-order traversal and indenting each level.
     * @param n      the current node to print
     * @param level  the current level of the B+Tree
     */
    @SuppressWarnings("unchecked")
    private void print (Node n, int level)
    {
        if (n == root) out.println ("BpTreeMap");
        out.println ("-------------------------------------------");

        for (int j = 0; j < level; j++) out.print ("\t");
        out.print ("[ . ");
        for (int i = 0; i < n.nKeys; i++) out.print (n.key[i] + "/" + this.get(n.key[i]) + " . ");
        out.println ("]");
        if ( ! n.isLeaf) {
            for (int i = 0; i <= n.nKeys; i++) print ((Node) n.ref[i], level + 1);
        } // if

        if (n == root) out.println ("-------------------------------------------");
    } // print

    /********************************************************************************
     * Recursive helper function for finding a key in B+trees.
     * @param key  the key to find
     * @param n    the current node
     */
    @SuppressWarnings("unchecked")
    private V find (K key, Node n)
    {
        count++;
        int i = n.find (key);
        if (i < n.nKeys) {
            K k_i = n.key[i];
            if (n.isLeaf) return (key.compareTo (k_i) == 0) ? (V) n.ref[i] : null;
            else          return find (key, (Node) n.ref[i]);
        } else {
            return (n.isLeaf) ? null : find (key, (Node) n.ref[n.nKeys]);
        } // if
    } // find

    /********** **********************************************************************
     * Recursive helper function for inserting a key in B+trees.
     * @param key  the key to insert
     * @param ref  the value/node to insert
     * @param n    the current node
     * @return  the newly allocated right sibling node of n
     */
     /*
    @SuppressWarnings("unchecked")
    private Node insert (K key, V ref, Node n)
    {
        out.println ("=============================================================");
        out.println ("insert: key = " + key + " value = " + ref );
        out.println ("=============================================================");

        Node rt = null;           
        if (n.isLeaf) {                                                      // handle leaf node level

            if (n.nKeys < ORDER - 1) {                                       // current node is not full
                wedge (key, ref, n, n.find (key), true);                     // wedge (key, ref) pair in at position i
            } else {                                                         // current node is full
                rt = split (key, ref, n, true);                              // split current node, return right sibling
                n.ref[n.nKeys] = rt;                                         // link leaf n to leaf rt
                if (n == root && rt != null) {
                    root = makeRoot (n, n.key[n.nKeys-1], rt);               // make a new root
                    return null;
                } else if (rt != null) {
                    hasSplit = true;                                         // indicate an unhandled split

                } // if

            } // if

        } else {                                                             // handle internal node level

            int i = n.find (key);                                            // find "<=" position
            out.println("\tKey: " + key + " Value: " + this.get(key) + " Ref to node: " + n.ref[i]);
            out.println("Ref node to: " + i + " Node: " + rt);
            rt = insert (key, ref, (Node) n.ref[i]);                         // recursive call 

                //K what: n.key <- gives the 'key'
                //this.get(n.key[a]) <- get the actual value of the 'key'

            out.println("\tKey: " + key + " Value: " + this.get(key) + " Ref to node: " + n.ref[i]);
            out.println("Ref node to: " + i + " Node: " + rt);
//            if(hasSplit){

              Node p = (Node) n.ref[i];
              //out.println(p.key[MID]);
              //out.println(this.get(p.key[MID]));
              if (rt!=null &&n.nKeys < ORDER - 1) {                                       // current node is not full
                wedge ( p.key[MID-1], rt, n, i, false);                               // wedge (key, ref) pair in at position i
              } else if(rt!=null){                                                         // current node is full
                  rt = split (p.key[MID-1], rt, n, false);                              // split current node, return right sibling
                  //n.ref[n.nKeys] = rt;                                         // link leaf n to leaf rt
                  if (n == root && rt != null) {
                    Node LC = new Node(ORDER,false);
                    LC.copy(n,0,n.nKeys-1);
                    root = makeRoot (LC, n.key[n.nKeys-1], rt);               // make a new root
                    //return rt;

                  } else {
                    //return rt;
                      //hasSplit = true;                                         // indicate an unhandled split
  //                } // if

              }

            }
            out.println("\tKey: " + key + " Value: " + this.get(key) + " Ref to node: " + n.ref[i]);
            out.println("Ref node to: " + i + " Node: " + rt);
//            if(hasSplit){

              Node p = (Node) n.ref[i];
              //out.println(p.key[MID]);
              //out.println(this.get(p.key[MID]));
              if (rt!=null &&n.nKeys < ORDER - 1) {                                       // current node is not full
                wedge ( p.key[MID-1], rt, n, i, false);                               // wedge (key, ref) pair in at position i
              } else if(rt!=null){                                                         // current node is full
                  rt = split (p.key[MID-1], rt, n, false);                              // split current node, return right sibling
                  //n.ref[n.nKeys] = rt;                                         // link leaf n to leaf rt
                  if (n == root && rt != null) {
                    Node LC = new Node(ORDER,false);
                    LC.copy(n,0,n.nKeys-1);
                    root = makeRoot (LC, n.key[n.nKeys-1], rt);               // make a new root
                    //return rt;

                  } else {
                    //return rt;
                      //hasSplit = true;                                         // indicate an unhandled split
  //                } // if

              }

            }
        } // if
        if (DEBUG) print (root, 0);
        return rt;                                                           // return right node
    } // insert
*/
    /********************************************************************************
     * Make a new root, linking to left and right child node, separated by a divider key.
     * @param ref0  the reference to the left child node
     * @param key0  the divider key - largest left
     * @param ref1  the reference to the right child node
     * @return  the node for the new root
     */
    private Node makeRoot (Node ref0, K key0, Node ref1)
    {
        Node nr   = new Node (ORDER, false);                          // make a node to become the new root
        nr.nKeys  = 1;
        nr.ref[0] = ref0;                                             // reference to left node
        nr.key[0] = key0;                                             // divider key - largest left
        nr.ref[1] = ref1;                                             // reference to right node
        return nr;
    } // makeRoot

    /********************************************************************************
     * Wedge the key-ref pair into node n.  Shift right to make room if needed.
     * @param key   the key to insert
     * @param ref   the value/node to insert
     * @param n     the current node
     * @param i     the insertion position within node n
     * @param left  whether to start from the left side of the key
     * @return  whether wedge succeeded (i.e., no duplicate)
     */
     /*
    private boolean wedge (K key, Object ref, Node n, int i, boolean left)
    {
        if (i < n.nKeys && key.compareTo(n.key[i]) == 0) {
             out.println ("BpTreeMap.insert: attempt to insert duplicate key = " + key);
             return false;
        } // if
        n.ref[n.nKeys + 1] = n.ref[n.nKeys];                          // preserving the last ref
        for (int j = n.nKeys; j > i; j--) {
            n.key[j] = n.key[j-1];                                    // make room: shift keys right
            if (left || j > i + 1) n.ref[j] = n.ref[j-1];             // make room: shift refs right
        } // for
        n.key[i] = key;                                               // place new key
        if (left) n.ref[i] = ref; else n.ref[i+1] = ref;              // place new ref
        n.nKeys++;                                                    // increment number of keys
        return true;
    } // wedge
*/
    /********************************************************************************
     * Split node n and return the newly created right sibling node rt.  The bigger half
     * should go in the current node n, with the remaining going in rt.
     * @param key  the new key to insert
     * @param ref  the new value/node to insert
     * @param n    the current node
     * @return  the right sibling node, if allocated, else null
     */
     /*
    private Node split (K key, Object ref, Node n, boolean left)
    {
        bn.copy (n, 0, ORDER-1);                                          // copy n into big node
        if (wedge (key, ref, bn, bn.find (key), left)) {                  // if wedge (key, ref) into big node was successful
            n.copy (bn, 0, MID);                                          // copy back first half to node n
            Node rt = new Node (ORDER, n.isLeaf);                         // make a right sibling node (rt)
            rt.copy (bn, MID, ORDER-MID);                                 // copy second to node rt
            return rt;                                                    // return right sibling
        } // if
        return null;                                                      // no new node created as key is duplicate
    } // split
*/

//=========================================================================================================================================
private Node insert (K key, V ref, Node n, Node p, int level)
    {
      /*
      out.println ("=============================================================");
      out.println ("insert: key = " + key + " value = " + ref );
      out.println ("=============================================================");
      */
      //if we are in a leaf node
        if(n.isLeaf)
        {
        	//if there is room in the node
        	if(n.nKeys < (ORDER-1))
        	{
        		boolean haveWedged = false;
        		for (int i = 0; (i < n.nKeys && haveWedged==false); i++) {
                    K k_i = n.key [i];
                    if (key.compareTo (k_i) < 0) {
                        wedge (key, ref, n, i);
                        haveWedged = true;
                    } else if (key.equals (k_i)) {
out.println ("DEBUG::BpTree:insert: attempt to insert duplicate key = " + key);
                        haveWedged = true;
                    } // if
                }// for
        		if(!haveWedged)
        		{
        			wedge(key,ref,n,n.nKeys);
        		}// if
        	}else
        	{
        		Node newNode = split(key,ref,n,level);
        		//if we are at the root level
        		if(level==0)
        		{
        			root = newNode;
        			root.isLeaf = false;
        		}else
        		{
                            return newNode;

                        }// if
                }
        return null;
        //if we are in a non-leaf node
        }else
        {
        	//go through all the keys in the node
        	for(int i=0;i<n.nKeys;i++)
        	{

        		//if the key to insert is less than the current key
        		if(key.compareTo(n.key[i])<0)
        		{
        			//insert (recursively) the new/value pair into the appropriate Node
        			Node result = insert(key,ref,(Node)n.ref[i],n,level+1);
                                if(result != null && n.nKeys < ORDER -1){
                                    int position = -1;
                                    for(int j = 0; j < n.nKeys - 1; j++){
                                        if(result.key[0].compareTo(n.key[j]) > 0){
                                            position = j+1;
                                        }
                                    }
                                    if(position == -1){
                                        position = 0;
                                    }
                                    wedge(result.key[0], (V)result, n, position);
                                }
                                else if(result != null){
                                    Node newParent = split(result.key[0], (V)result, n, level-1);
                                    if(level!=0){
                                        return newParent;
                                    }
                                    else{
                                        root = newParent;
                                    }
                                }
        			return null;
        		}
        	}

        	//otherwise, it belongs in the last Node, so insert it accordingly
        	Node result = insert(key,ref,(Node)n.ref[n.nKeys],n,level+1);
                if(result != null && n.nKeys < ORDER -1){
                    int position = -1;
                    for(int j = 0; j < n.nKeys - 1; j++){
                        if(n.key[j].compareTo(result.key[0]) > 0){
                            position = j;
                        }
                    }
                    if(position == -1){
                        position = n.nKeys;
                    }
                    wedge(result.key[0], (V)result, n, position);
                }
                else if(result != null){
                    Node newParent = split(result.key[0], (V)result, n, level-1);
                    if(level!=0){
                        return newParent;
                    }
                    else{
                        root = newParent;
                    }
                }

        	return null;
        }
    } // insert

    private void wedge (K key, V ref, Node n, int i)
    {
        if(n.isLeaf == true){
            for (int j = n.nKeys; j > i; j--) {
                n.key [j] = n.key [j - 1];
                n.ref [j] = n.ref [j - 1];
            } // for
            n.key [i] = key;
            n.ref [i] = ref;
            n.nKeys++;
        }
        else{

            Node secondNode = (Node) ref;
            for(int j = n.nKeys; j > i; j--){
                n.key[j] = n.key[j-1];
                n.ref[j+1] = n.ref[j];
            }

            n.key[i] = secondNode.key[0];
            n.ref[i] = secondNode.ref[0];
            n.ref[i+1] = secondNode.ref[1];
            n.nKeys++;
        }

    } // wedge

     private Node split (K key, V ref, Node n, int level)
    {
           //if we're splitting a leaf node
    	if(n.isLeaf)
    	{
    		//the new node can never be a leaf (by nature of split)
    		Node result = new Node(ORDER,false);
    		//make an array to hold all 5 keys
    		ArrayList<K> karray = new ArrayList<>();
    		//and another for the value
    		ArrayList<V> varray = new ArrayList<>();
    		boolean addedNew = false;
    		//cycle through the node and add the keys in order
    		for (int i = 0; i < n.nKeys; i++) {
    			//if the new key is less than the next key
    			if(key.compareTo(n.key[i])<0 && (!addedNew))
    			{
    				karray.add(key);
    				varray.add(ref);
    				karray.add(n.key[i]);
    				varray.add((V) n.ref[i]);
    				addedNew = true;
    			}else
    			{
    				karray.add(n.key[i]);
    				varray.add((V) n.ref[i]);
    			}// if
    		}// for
    		//if the new key is larger than all the keys already in the node
    		if(key.compareTo(n.key[n.nKeys-1])>0 || key.compareTo(n.key[n.nKeys-1]) == 0)
    		{
    			karray.add(key);
    			varray.add(ref);
    		}// if
    		//the keys should all be in order now in karray
    		//make a new Node to be the leftchild with the same leafality as n
    		Node LC = new Node(ORDER,n.isLeaf);
    		//make a new Node to be the rightchild with the same leafality as n
    		Node RC = new Node(ORDER,n.isLeaf);
    		//put the appropriate values in the appropriate nodes
    		insert((K)karray.get(0),(V)varray.get(0),LC,result,level);
    		insert((K)karray.get(1),(V)varray.get(1),LC,result,level);
    		insert((K)karray.get(2),(V)varray.get(2),LC,result,level);
    		insert((K)karray.get(3),(V)varray.get(3),RC,result,level);
    		insert((K)karray.get(4),(V)varray.get(4),RC,result,level);
    		result.key[0]=(K)karray.get(2);
    		result.nKeys++;
    		result.ref[0] = LC;
    		result.ref[1] = RC;
            return(result);
    	//if we're not splitting a leaf node
    	}else
    	{
            Node toInsert = (Node) ref;
            //the new node can never be a leaf (by nature of split)
            Node result = new Node(ORDER,false);
            //make an array to hold all 5 keys
            ArrayList<K> karray = new ArrayList<>();
            //and another for the value
            ArrayList<V> varray = new ArrayList<>();
            boolean addedNew = false;
            //cycle through the node and add the keys in order
            for (int i = 0; i < n.nKeys; i++) {
                    //if the new key is less than the next key
                    if(toInsert.key[0].compareTo(n.key[i])<0 && (!addedNew))
                    {
                            karray.add(toInsert.key[0]);
                            varray.add((V)toInsert.ref[0]);
                            varray.add((V)toInsert.ref[1]);
                            karray.add(n.key[i]);
                            varray.add((V) n.ref[i]);
                            varray.add((V) n.ref[i+1]);
                            addedNew = true;
                    }
                    //If the new node hasn't been added yet
                    else if(addedNew == false)
                    {
                            karray.add(n.key[i]);
                            varray.add((V) n.ref[i]);
                    }
                    //If the node has been added, we don't want it's right ref overwritten
                    else if(addedNew == true && i < n.nKeys-1){
                            karray.add(n.key[i]);
                            varray.add((V) n.ref[i+1]);
                    }else if(addedNew == true && i == n.nKeys-1){
                            karray.add(n.key[i]);
                    }// if
            }// for
            //if the new key is larger than all the keys already in the node
            if(key.compareTo(n.key[n.nKeys-1])>0 || key.compareTo(n.key[n.nKeys-1]) == 0)
            {
                    karray.add(toInsert.key[0]);
                    varray.add((V)toInsert.ref[0]);
                    varray.add((V)toInsert.ref[1]);
            }// if

            //the keys should all be in order now in karray
            //make a new Node to be the leftchild with the same leafality as n
            Node LC = new Node(ORDER,n.isLeaf);
            //make a new Node to be the rightchild with the same leafality as n
            Node RC = new Node(ORDER,n.isLeaf);
            //put the appropriate values in the appropriate nodes
            for(int i = 0; i < 2; i++){
                LC.key[i] = (K)karray.get(i);
                LC.ref[i] = (V)varray.get(i);
                LC.ref[i+1] = (V)varray.get(i+1);
                LC.nKeys++;
            }
            for(int i = 3; i < 5; i++){
                RC.key[i-3] = (K)karray.get(i);
                RC.ref[i-3] = (V)varray.get(i);
                RC.ref[i-2] = (V)varray.get(i+1);
                RC.nKeys++;
            }

            result.key[0]=(K)karray.get(2);
            result.nKeys++;
            result.ref[0] = LC;
            result.ref[1] = RC;
            return result;
    	}// if
    } // split

    //=========================================================================================================================================


    /********************************************************************************
     * The main method used for testing.
     * @param  the command-line arguments (args[0] gives number of keys to insert)
     */
    public static void main (String [] args)
    {
        int totalKeys    = 20;
        boolean RANDOMLY = false;

        BpTreeMap <Integer, Integer> bpt = new BpTreeMap <> (Integer.class, Integer.class);
        if (args.length == 1) totalKeys = Integer.valueOf (args[0]);
        if (RANDOMLY) {
            Random rng = new Random ();
            for (int i = 1; i <= totalKeys; i += 2) bpt.put (rng.nextInt (2 * totalKeys), i * i);
        } else {
            for (int i = 1; i <= totalKeys; i += 1) bpt.put (i, i); //for (int i = 1; i <= totalKeys; i += 2) bpt.put (i, i * i);
        } // if

        bpt.print (bpt.root, 0);
        for (int i = 0; i <= totalKeys; i++) {
            out.println ("key = " + i /*+ " value = " + bpt.get (i)*/);
        } // for
        out.println ("-------------------------------------------");
        out.println ("Average number of nodes accessed = " + bpt.count / (double) totalKeys);
        out.println ("-------------------------------------------");
        out.println("First Key: " + bpt.firstKey());
        out.println("Last Key Value: " + bpt.get(bpt.firstKey()));
        out.println ("-------------------------------------------");
        out.println("Last Key: " + bpt.lastKey());
        out.println("Last Key Value: " + bpt.get(bpt.lastKey()));
        out.println ("-------------------------------------------");
    } // main

} // BpTreeMap class
