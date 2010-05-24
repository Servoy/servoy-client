/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/
package com.servoy.j2db.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class IntHashMap<V> implements Cloneable, Serializable
{
	transient volatile Set<Integer>        keySet = null;
	transient volatile Collection<V> values = null;
    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     **/
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    transient Entry<V>[] table;

    /**
     * The number of key-value mappings contained in this identity hash map.
     */
    transient int size;
  
    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    int threshold;
  
    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient volatile int modCount;

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity The initial capacity.
     * @param  loadFactor      The load factor.
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive.
     */
    public IntHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " + //$NON-NLS-1$
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " + //$NON-NLS-1$
                                               loadFactor);

        // Find a power of 2 >= initialCapacity
        int capacity = 1;
        while (capacity < initialCapacity) 
            capacity <<= 1;
    
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
        table = new Entry[capacity];
        init();
    }
  
    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public IntHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    public IntHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = (int)(DEFAULT_INITIAL_CAPACITY * DEFAULT_LOAD_FACTOR);
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
        init();
    }



    // internal utilities

    /**
     * Initialization hook for subclasses. This method is called
     * in all constructors and pseudo-constructors (clone, readObject)
     * after HashMap has been initialized but before any entries have
     * been inserted.  (In the absence of this method, readObject would
     * require explicit knowledge of subclasses.)
     */
    void init() {
    }

    /**
     * Value representing null keys inside tables.
     */
    static final Object NULL_KEY = new Object();

    /**
     * Returns internal representation for key. Use NULL_KEY if key is null.
     */
    static Object maskNull(Object key) {
        return (key == null ? NULL_KEY : key);
    }

    /**
     * Returns key represented by specified internal representation.
     */
    static Object unmaskNull(Object key) {
        return (key == NULL_KEY ? null : key);
    }

    /**
     * Returns a hash value for the specified object.  In addition to 
     * the object's own hashCode, this method applies a "supplemental
     * hash function," which defends against poor quality hash functions.
     * This is critical because HashMap uses power-of two length 
     * hash tables.<p>
     *
     * The shift distances in this function were chosen as the result
     * of an automated search over the entire four-dimensional search space.
     */
    static int hash(int  h) {

        h += ~(h << 9);
        h ^=  (h >>> 14);
        h +=  (h << 4);
        h ^=  (h >>> 10);
        return h;
    }

    /** 
     * Check for equality of non-null reference x and possibly-null y. 
     */
    static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    /**
     * Returns index for hash code h. 
     */
    static int indexFor(int h, int length) {
        return h & (length-1);
    }
 
    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map.
     */
    public int size() {
        return size;
    }
  
    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the value to which the specified key is mapped in this identity
     * hash map, or <tt>null</tt> if the map contains no mapping for this key.
     * A return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contains no mapping for the key; it is also possible that
     * the map explicitly maps the key to <tt>null</tt>. The
     * <tt>containsKey</tt> method may be used to distinguish these two cases.
     *
     * @param   key the key whose associated value is to be returned.
     * @return  the value to which this map maps the specified key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(Object, Object)
     */
    public V get(int key) {
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        Entry<V> e = table[i]; 
        while (true) {
            if (e == null)
                return null;
            if (e.hash == hash && key == e.key)
                return e.value;
            e = e.next;
        }
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    public boolean containsKey(int key) {
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        Entry<V> e = table[i]; 
        while (e != null) {
            if (e.hash == hash && key == e.key) 
                return true;
            e = e.next;
        }
        return false;
    }

    /**
     * Returns the entry associated with the specified key in the
     * HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry<V> getEntry(int key) {
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        Entry<V> e = table[i]; 
        while (e != null && !(e.hash == hash && key == e.key))
            e = e.next;
        return e;
    }
  
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the HashMap previously associated
     *	       <tt>null</tt> with the specified key.
     */
    public V put(int key, V value) {
        int hash = hash(key);
        int i = indexFor(hash, table.length);

        for (Entry<V> e = table[i]; e != null; e = e.next) {
            if (e.hash == hash && key == e.key) {
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }

        modCount++;
        addEntry(hash, key, value, i);
        return null;
    }

    /**
     * This method is used instead of put by constructors and
     * pseudoconstructors (clone, readObject).  It does not resize the table,
     * check for comodification, etc.  It calls createEntry rather than
     * addEntry.
     */
    private void putForCreate(int key, V value) {
        int hash = hash(key);
        int i = indexFor(hash, table.length);

        /**
         * Look for preexisting entry for key.  This will never happen for
         * clone or deserialize.  It will only happen for construction if the
         * input Map is a sorted map whose ordering is inconsistent w/ equals.
         */
        for (Entry<V> e = table[i]; e != null; e = e.next) {
            if (e.hash == hash && key == e.key) {
                e.value = value;
                return;
            }
        }

        createEntry(hash, key, value, i);
    }

    void putAllForCreate(IntHashMap<V> m) {
        for (Iterator<Entry<V>> i = m.entrySet().iterator(); i.hasNext(); ) {
            Entry<V> e = i.next();
            putForCreate(e.getKey(), e.getValue());
        }
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    void resize(int newCapacity) {
        Entry<V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry<V>[] newTable = new Entry[newCapacity];
        transfer(newTable);
        table = newTable;
        threshold = (int)(newCapacity * loadFactor);
    }

    /** 
     * Transfer all entries from current table to newTable.
     */
    void transfer(Entry<V>[] newTable) {
        Entry<V>[] src = table;
        int newCapacity = newTable.length;
        for (int j = 0; j < src.length; j++) {
            Entry<V> e = src[j];
            if (e != null) {
                src[j] = null;
                do {
                    Entry<V> next = e.next;
                    int i = indexFor(e.hash, newCapacity);  
                    e.next = newTable[i];
                    newTable[i] = e;
                    e = next;
                } while (e != null);
            }
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * These mappings will replace any mappings that
     * this map had for any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map.
     * @throws NullPointerException if the specified map is null.
     */
    public void putAll(IntHashMap<V> m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0)
            return;

        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        for (Iterator<Entry<V>> i = m.entrySet().iterator(); i.hasNext(); ) {
            Entry<V> e = i.next();
            put(e.getKey(), e.getValue());
        }
    }
  
    /**
     * Removes the mapping for this key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the map previously associated <tt>null</tt>
     *	       with the specified key.
     */
    public V remove(int key) {
        Entry<V> e = removeEntryForKey(key);
        return (e == null ? null : e.value);
    }

    /**
     * Removes and returns the entry associated with the specified key
     * in the HashMap.  Returns null if the HashMap contains no mapping
     * for this key.
     */
    Entry<V> removeEntryForKey(int key) {
        int hash = hash(key);
        int i = indexFor(hash, table.length);
        Entry<V> prev = table[i];
        Entry<V> e = prev;

        while (e != null) {
            Entry<V> next = e.next;
            if (e.hash == hash && key == e.key) {
                modCount++;
                size--;
                if (prev == e) 
                    table[i] = next;
                else
                    prev.next = next;
                return e;
            }
            prev = e;
            e = next;
        }
   
        return e;
    }

    /**
     * Special version of remove for EntrySet.
     */
    Entry<V> removeMapping(Object o) {
        if (!(o instanceof Entry))
            return null;

        Entry<V> entry = (Entry<V>)o;
        int hash = hash(entry.getKey());
        int i = indexFor(hash, table.length);
        Entry<V> prev = table[i];
        Entry<V> e = prev;

        while (e != null) {
            Entry<V> next = e.next;
            if (e.hash == hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e) 
                    table[i] = next;
                else
                    prev.next = next;
                return e;
            }
            prev = e;
            e = next;
        }
   
        return e;
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear() {
        modCount++;
        Entry<V> tab[] = table;
        for (int i = 0; i < tab.length; i++) 
            tab[i] = null;
        size = 0;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    public boolean containsValue(Object value) {
	if (value == null) 
            return containsNullValue();

	Entry<V> tab[] = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry<V> e = tab[i] ; e != null ; e = e.next)
                if (value.equals(e.value))
                    return true;
	return false;
    }

    /**
     * Special-case code for containsValue with null argument
     **/
    private boolean containsNullValue() {
	Entry<V> tab[] = table;
        for (int i = 0; i < tab.length ; i++)
            for (Entry<V> e = tab[i] ; e != null ; e = e.next)
                if (e.value == null)
                    return true;
	return false;
    }

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map.
     */
    public Object clone() {
        IntHashMap<V> result = null;
	try { 
	    result = (IntHashMap<V>)super.clone();
	} catch (CloneNotSupportedException e) { 
	    // assert false;
	}
        result.table = new Entry[table.length];
        result.entrySet = null;
        result.modCount = 0;
        result.size = 0;
        result.init();
        result.putAllForCreate(this);

        return result;
    }

    public static class Entry<V> {
        final int key;
        V value;
        final int hash;
        Entry<V> next;

        /**
         * Create new entry.
         */
        Entry(int h, int k, V v, Entry<V> n) { 
            value = v; 
            next = n;
            key = k;
            hash = h;
        }

        public int getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    
        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
    
        public boolean equals(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<V> e = (Entry<V>)o;
            int k1 = getKey();
            int k2 = e.getKey();
            if (k1 == k2) {
                V v1 = getValue();
                V v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2))) 
                    return true;
            }
            return false;
        }
    
        public int hashCode() {
            return key ^ (value==null   ? 0 : value.hashCode());
        }
    
        public String toString() {
            return getKey() + "=" + getValue(); //$NON-NLS-1$
        }




    }

    /**
     * Add a new entry with the specified key, value and hash code to
     * the specified bucket.  It is the responsibility of this 
     * method to resize the table if appropriate.
     *
     * Subclass overrides this to alter the behavior of put method.
     */
    void addEntry(int hash, int key, V value, int bucketIndex) {
        table[bucketIndex] = new Entry<V>(hash, key, value, table[bucketIndex]);
        if (size++ >= threshold) 
            resize(2 * table.length);
    }

    /**
     * Like addEntry except that this version is used when creating entries
     * as part of Map construction or "pseudo-construction" (cloning,
     * deserialization).  This version needn't worry about resizing the table.
     *
     * Subclass overrides this to alter the behavior of HashMap(Map),
     * clone, and readObject.
     */
    void createEntry(int hash, int key, V value, int bucketIndex) {
        table[bucketIndex] = new Entry<V>(hash, key, value, table[bucketIndex]);
        size++;
    }

    private abstract class HashIterator {
        Entry<V> next;                  // next entry to return
        int expectedModCount;        // For fast-fail 
        int index;                   // current slot 
        Entry<V> current;               // current entry

        HashIterator() {
            expectedModCount = modCount;
            Entry<V>[] t = table;
            int i = t.length;
            Entry<V> n = null;
            if (size != 0) { // advance to first entry
                while (i > 0 && (n = t[--i]) == null)
                    ;
            }
            next = n;
            index = i;
        }

        public boolean hasNext() {
            return next != null;
        }

        Entry<V> nextEntry() { 
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            Entry<V> e = next;
            if (e == null) 
                throw new NoSuchElementException();
                
            Entry<V> n = e.next;
            Entry<V>[] t = table;
            int i = index;
            while (n == null && i > 0)
                n = t[--i];
            index = i;
            next = n;
            return current = e;
        }

        public void remove() {
            if (current == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            int k = current.key;
            current = null;
            IntHashMap.this.removeEntryForKey(k);
            expectedModCount = modCount;
        }

    }

    private class ValueIterator extends HashIterator  implements Iterator<V> {
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator implements Iterator<Integer> {
        public Integer next() {
            return new Integer(nextEntry().getKey());
        }
    }

    private class EntryIterator extends HashIterator implements Iterator<Entry<V>>{
        public Entry<V> next() {
            return nextEntry();
        }
    }

    // Subclass overrides these to alter behavior of views' iterator() method
    Iterator<Integer> newKeyIterator()   {
        return new KeyIterator();
    }
    Iterator<V> newValueIterator()   {
        return new ValueIterator();
    }
    Iterator<Entry<V>> newEntryIterator()   {
        return new EntryIterator();
    }


    // Views

    private transient Set<Entry<V>> entrySet = null;

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  The set supports element removal, which removes the
     * corresponding mapping from this map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and
     * <tt>clear</tt> operations.  It does not support the <tt>add</tt> or
     * <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    public Set<Integer> keySet() {
        Set<Integer> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    private class KeySet extends AbstractSet<Integer> {
        public Iterator<Integer> iterator() {
            return newKeyIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
        		if(o instanceof Number)
        		{
        			return containsKey(((Number)o).intValue());
        		}
        		return false;
        }
        public boolean remove(Object o) {
        		if(o instanceof Number)
        		{
        			return IntHashMap.this.removeEntryForKey(((Number)o).intValue()) != null;
        		}
        		return false;
        }
        public void clear() {
            IntHashMap.this.clear();
        }
    }

    /**
     * Returns a collection view of the values contained in this map.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from this map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null ? vs : (values = new Values()));
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return newValueIterator();
        }
        public int size() {
            return size;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public void clear() {
            IntHashMap.this.clear();
        }
    }

    /**
     * Returns a collection view of the mappings contained in this map.  Each
     * element in the returned collection is a <tt>Map.Entry</tt>.  The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa.  The collection supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the mappings contained in this map.
     * @see Map.Entry
     */
    public Set<Entry<V>> entrySet() {
        Set<Entry<V>> es = entrySet;
        return (es != null ? es : (entrySet = new EntrySet()));
    }

    private class EntrySet extends AbstractSet<Entry<V>> {
        public Iterator<Entry<V>> iterator() {
            return newEntryIterator();
        }
        public boolean contains(Object o) {
            if (!(o instanceof Entry))
                return false;
            Entry<V> e = (Entry<V>)o;
            Entry<V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }
        public boolean remove(Object o) {
            return removeMapping(o) != null;
        }
        public int size() {
            return size;
        }
        public void clear() {
            IntHashMap.this.clear();
        }
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *		   bucket array) is emitted (int), followed  by the
     *		   <i>size</i> of the HashMap (the number of key-value
     *		   mappings), followed by the key (Object) and value (Object)
     *		   for each key-value mapping represented by the HashMap
     *             The key-value mappings are emitted in the order that they
     *             are returned by <tt>entrySet().iterator()</tt>.
     * 
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException
    {
	// Write out the threshold, loadfactor, and any hidden stuff
	s.defaultWriteObject();

	// Write out number of buckets
	s.writeInt(table.length);

	// Write out size (number of Mappings)
	s.writeInt(size);

        // Write out keys and values (alternating)
        for (Iterator<Entry<V>> i = entrySet().iterator(); i.hasNext(); ) {
            Entry<V> e = i.next();
            s.writeInt(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    private static final long serialVersionUID = 362498820763181265L;

    /**
     * Reconstitute the <tt>HashMap</tt> instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
	// Read in the threshold, loadfactor, and any hidden stuff
	s.defaultReadObject();

	// Read in number of buckets and allocate the bucket array;
	int numBuckets = s.readInt();
	table = new Entry[numBuckets];

        init();  // Give subclass a chance to do its thing.

	// Read in size (number of Mappings)
	int size = s.readInt();

	// Read the keys and values, and put the mappings in the HashMap
	for (int i=0; i<size; i++) {
	    int key = s.readInt();
	    V value = (V)s.readObject();
	    putForCreate(key, value);
	}
    }

    // These methods are used when serializing HashSets
    int   capacity()     { return table.length; }
    float loadFactor()   { return loadFactor;   }
}