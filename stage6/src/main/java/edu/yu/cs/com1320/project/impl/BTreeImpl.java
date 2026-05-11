package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;
import edu.yu.cs.com1320.project.stage6.impl.DocumentPersistenceManager;

import java.io.IOException;
import java.util.Arrays;

public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree<Key, Value> {
    private static final int MAX = 6;
    private Node root;
    private int height;
    private PersistenceManager<Key, Value> DPM;

    public BTreeImpl(){
        this.root = new Node(0);
    }

    private static final class Node {
        private int entryCount; // number of entries
        private Entry[] entries = new Entry[BTreeImpl.MAX]; // the array of children

        private Node(int k) {
            this.entryCount = k;
        }
    }

    private static class Entry {
        private Comparable key;
        private Object val;
        private Node child;
        private boolean onDisk = false;

        public Entry(Comparable key, Object val, Node child) {
            this.key = key;
            this.val = val;
            this.child = child;
        }

        public void setDiskStatus(boolean onDisk){
            this.onDisk = onDisk;
        }
        public boolean isOnDisk(){
            return this.onDisk;
        }
    }

    private static class Holder<V>{
        V value;
    }

    /**
     * compares two keys to see if this one precedes the other
     * @param key
     * @param otherKey
     * @return true if this key is less than otherKey. False otherwise
     */
    private static boolean less(Comparable key, Comparable otherKey) {
        return key.compareTo(otherKey) < 0;
    }

    /**
     * compare the two keys to see if they are equal
     * @param key
     * @param otherKey
     * @return true if this key equals other key. False otherwise
     */
    private static boolean isEqual(Comparable key, Comparable otherKey) {
        return key.compareTo(otherKey) == 0;
    }

    /**
     * get the value from the tree associated with this key
     * if the value was on disk, it is brought back into memory and removed from disk
     * @param k
     * @return The value for this key. null if there is no value
     */
    @Override
    public Value get(Key k) {
        Entry entry = this.get(this.root, k, this.height);
        if (entry != null){
            if (entry.val != null){ //the doc is in memory so the value is here
                return (Value) entry.val;
            } else {
                if (entry.isOnDisk()){ //the doc is on disk so we have to get it
                    try {
                        Value v = DPM.deserialize(k);
                        entry.val = v;
                        entry.setDiskStatus(false);
                        return v;
                    } catch (IOException e){
                        throw new RuntimeException(e);
                    }
                } //if it's not on disk, it's been deleted so we return null
            }

        }
        return null;
    }

    /**
     * recursively traverses the tree to get the Entry for this key
     * @param currentNode
     * @param key
     * @param height
     * @return the Entry associated with this key. If it isn't in the tree, return null
     */
    private Entry get(Node currentNode, Key key, int height){
        Entry[] entries = currentNode.entries;
        if (height == 0){
            for (int j = 0; j < currentNode.entryCount; j++) {
                if(isEqual(key, entries[j].key)) {
                    return entries[j];
                }
            }
            return null;
        } else {
            for (int j = 0; j < currentNode.entryCount; j++){
                if (j + 1 == currentNode.entryCount || less(key, entries[j + 1].key)){
                    return this.get(entries[j].child, key, height - 1);
                }
            }
            return null;
        }
    }

    /**
     * Puts the key-value pair into the tree
     * if the value is null, this is a call to delete the value from the tree
     * creates a new root node if the root was split
     * @param k
     * @param v
     * @return the previous value for this key. null if there was no previous value
     */
    @Override
    public Value put(Key k, Value v) {
        Holder<Value> previous = new Holder<>();
        Node newNode = this.put(this.root, k, v, this.height, previous);
        if (newNode == null){
            //we don't need to split the root, return the previous value
            return previous.value;
        }
        Node newRoot = new Node(2);
        newRoot.entries[0] = new Entry(this.root.entries[0].key, null, this.root);
        newRoot.entries[1] = new Entry(newNode.entries[0].key, null, newNode);
        this.root = newRoot;
        this.height++;
        return previous.value;
    }

    /**
     * recursively traverses the tree to put the key-value pair
     * if the key had a value on disk, that value is deleted from disk
     * splits nodes to maintain the structure of the tree if necessary
     * @param currentNode
     * @param key
     * @param val
     * @param height
     * @param previous the previous value associated with this key
     * @return a Node which has just been split and needs a parent node. null if there was no split
     */
    private Node put(Node currentNode, Key key, Value val, int height, Holder<Value> previous){
        int j;
        Entry newEntry = new Entry(key, val, null);
        if (height == 0){
            j = findIndexForPut(currentNode, key);
        } else {
            for (j = 0; j < currentNode.entryCount; j++){
                if (j + 1 == currentNode.entryCount || less(key, currentNode.entries[j+1].key)){
                    Node newNode = this.put(currentNode.entries[j++].child, key, val, height - 1, previous);
                    if (newNode == null){
                        return null;
                    }
                    newEntry.key = newNode.entries[0].key;
                    newEntry.val = null;
                    newEntry.child = newNode;
                    break;
                }
            }
        }
        if (j < currentNode.entryCount && isEqual(key, currentNode.entries[j].key)){
            this.putInExistingKey(currentNode, j, key, newEntry, previous);
            return null;
        }
        if (val == null){ //this was a delete call, but if we made it this far the key must not be in the tree
            return null;
        }
        return this.putNewEntry(currentNode, j, newEntry);
    }

    /**
     * finds the index in this node where we need to put the new key-value pair
     * @param currentNode guaranteed to be a leaf node
     * @param key
     * @return the index
     */
    private int findIndexForPut(Node currentNode, Key key){
        int j;
        for (j = 0; j < currentNode.entryCount; j++){
            if (isEqual(key, currentNode.entries[j].key) || less(key, currentNode.entries[j].key)){
                break;
            }
        }
        return j;
    }

    /**
     * puts in the new Entry when this key was already in the tree
     * prepares previous to be returned by the public put
     * if this key had a value on disk, it is deleted from disk
     * @param currentNode guaranteed to be a leaf node
     * @param j the index where we want to put
     * @param key
     * @param newEntry created in private put
     * @param previous
     */
    private void putInExistingKey(Node currentNode, int j, Key key, Entry newEntry, Holder<Value> previous){
        Entry oldEntry = currentNode.entries[j];
        if (oldEntry.val != null){
            previous.value = (Value) oldEntry.val;
        } else if (oldEntry.isOnDisk()){
            try {
                previous.value = DPM.deserialize(key);
            } catch (IOException e){
                throw new RuntimeException(e);
            }
        } //otherwise the Entry is not even on disk so previous stays null
        currentNode.entries[j] = newEntry;
        currentNode.entries[j].setDiskStatus(false);
    }

    /**
     * puts the new Entry into the tree
     * this could be a leaf node where we are putting a key-value pair, or an internal node in which we are placing an Entry as a parent for a new node.
     * splits the node if needed
     * @param currentNode the node where we are placing this key. not necessarily a leaf node
     * @param j the index where we need to place
     * @param newEntry created in private put
     * @return A new node if we needed to split currentNode. null if there was no split
     */
    private Node putNewEntry(Node currentNode, int j, Entry newEntry){
        for (int i = currentNode.entryCount; i > j; i--) {
            currentNode.entries[i] = currentNode.entries[i - 1];
        }
        currentNode.entries[j] = newEntry;
        currentNode.entryCount++;
        if (currentNode.entryCount < BTreeImpl.MAX){
            return null;
        } else {
            return this.split(currentNode);
        }
    }

    /**
     * splits a node into two nodes if there were too many entries in it
     * copies the back half of the first node into the second one
     * deletes the copied entries from the first node
     * @param currentNode the node which needs to be split
     * @return the new node we just created
     */
    private Node split(Node currentNode){
        Node newNode = new Node(BTreeImpl.MAX / 2);
        currentNode.entryCount = BTreeImpl.MAX / 2;
        for (int j = 0; j < BTreeImpl.MAX / 2; j++) {
            newNode.entries[j] = currentNode.entries[BTreeImpl.MAX / 2 + j];
            currentNode.entries[BTreeImpl.MAX/2 + j] = null;
        }
        return newNode;
    }

    /**
     * moves the document associated with this key to disk, using the DocumentPersistanceManager
     * marks the Entry as being on disk
     * @param k
     * @throws IOException
     */
    @Override
    public void moveToDisk(Key k) throws IOException {
        Entry e = this.get(this.root, k, this.height);
        DPM.serialize(k, (Value) e.val);
        e.val = null;
        e.setDiskStatus(true);
    }

    /**
     * sets the Persistance Manager to deal with disk I/O for this tree
     * @param pm
     */
    @Override
    public void setPersistenceManager(PersistenceManager<Key, Value> pm) {
        this.DPM = pm;
    }
}
