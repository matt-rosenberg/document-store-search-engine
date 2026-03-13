package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

import java.util.*;

public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {

    class Entry<Key, Value>{
        Key key;
        Value value;
        Entry next;
        Entry(Key k, Value v){
            this.key = k;
            this.value = v;
            this.next = null;
        }
    }

    private Entry<?,?>[] table;
    private int size;
    public HashTableImpl(){
        this.table = new Entry[5];
        this.size = 0;
    }

    /**
     * @param k the key whose value should be returned
     * @return the value that is stored in the HashTable for k, or null if there is no such key in the table
     */
    @Override
    public Value get(Key k) {
        Entry e = getEntry(k);
        if (e == null){
            return null;
        }
        return (Value) e.value;
    }

    /**
     * @param k the key at which to store the value
     * @param v the value to store
     *          To delete an entry, put a null value.
     * Doubles the size of the array and re-hashes when M / 4 >= N
     * @return if the key was already present in the HashTable, return the previous value stored for the key. If the key was not already present, return null.
     */
    @Override
    public Value put(Key k, Value v) {
        int index = hashFunction(k);
        int alreadyHere = findKey(k);
        if (v == null){
            if (alreadyHere == -1){
                return null;
            }
            this.size--;
            return delete(k);
        }
        if (alreadyHere == -1){
            Entry newEntry = new Entry(k, v);
            if (table[index] != null){
                newEntry.next = table[index];
            }
            table[index] = newEntry;
            this.size++;
            if (this.size / 4 >= table.length){
                resize();
            }
            return null;
        } else {
            Entry current = getEntry(k);
            Value val = (Value) current.value;
            current.value = v;
            return val;
        }
    }

    /**
     * @param key the key whose hashCode we need
     * @return the hashCode of key without the sign at the front
     */
    private int hashFunction(Key key){
        return (key.hashCode() & 0x7fffffff) % table.length;
    }

    /**
     * finds the index of this key within the linked list
     * @param k the key we are looking for
     * @return if the key is present, its index in the linked list. If not, -1
     */
    private int findKey(Key k){
        int index = hashFunction(k);
        if (table[index] == null){
            return -1;
        }
        Entry current = table[index];
        int spot = 0;
        while (true){
            if (current.key.equals(k)){
                return spot;
            } else {
                if (current.next == null){
                    return -1;
                } else {
                    current = current.next;
                    spot++;
                }
            }
        }
    }

    /**
     * retrieves the Entry object for this key
     * @param k the key whose Entry object we want
     * @return the Entry object for this key, provided it is in the table. If it's not in the table, return null.
     */
    private Entry getEntry(Key k){
        int spot = findKey(k);
        if (spot == -1){
            return null;
        }
        int index = hashFunction(k);
        Entry current = table[index];
        for (int i = 0; i < spot; i++){
            current = current.next;
        }
        return current;
    }

    /**
     * Deletes a key from the table
     * @param k the key to delete
     * @return the value that was associated with this key. Null if there was no value.
     */
    private Value delete(Key k){
        int index = hashFunction(k);
        int spot = findKey(k);
        Value v;
        if (spot == 0){
            v = (Value) table[index].value;
            table[index] = table[index].next;
            return v;
        }
        Entry current = table[index];
        for (int i = 0; i < spot; i++){
            if (current.next.key.equals(k)){
                v = (Value) current.next.value;
                current.next = current.next.next;
                return v;
            }
        }
        return null;
    }

    /**
     * doubles the size of the HashTable, rehashes and places the entries
     */
    private void resize(){
        Entry<?,?>[] newTable = new Entry[table.length * 2];
        for (int i = 0; i < table.length; i++){
            Entry current = table[i];
            while (current != null){
                Entry newEntry = new Entry(current.key, current.value);
                int index = (current.key.hashCode() & 0x7fffffff) % newTable.length;
                if (newTable[index] != null){
                    newEntry.next = newTable[index];
                }
                newTable[index] = newEntry;
                current = current.next;
            }
        }
        table = newTable;
    }

    /**
     * @param key the key whose presence in the hashtabe we are inquiring about
     * @return true if the given key is present in the hashtable as a key, false if not
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public boolean containsKey(Key key) {
        if (key == null){
            throw new NullPointerException("I can't find a key that doesn't exist");
        }
        return findKey(key) != -1;
    }

    /**
     * @return an unmodifiable set of all the keys in this HashTable
     * @see java.util.Collections#unmodifiableSet(Set)
     */
    @Override
    public Set<Key> keySet() {
        Set<Key> allKeys = new HashSet<>();
        for (int i = 0; i < table.length; i++){
            Entry current = table[i];
            while (current != null){
                allKeys.add((Key) current.key);
                current = current.next;
            }
        }
        return Collections.unmodifiableSet(allKeys);
    }

    /**
     * @return an unmodifiable set of all the keys in this HashTable
     * @see java.util.Collections#unmodifiableSet(Set)
     */
    @Override
    public Collection<Value> values() {
        Collection<Value> allValues = new ArrayList<>();
        for (int i = 0; i < table.length; i++){
            Entry current = table[i];
            while (current != null){
                allValues.add((Value) current.value);
                current = current.next;
            }
        }
        return Collections.unmodifiableCollection(allValues);
    }

    /**
     * @return how entries there currently are in the HashTable
     */
    @Override
    public int size() {
        return this.size;
    }
}
