package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {
    private static int alphabetSize = 128;
    private Node<Value> root;

    class Node<Value>{
        Set<Value> values;
        Node[] links = new Node[TrieImpl.alphabetSize];
        /**
         * adds a value to the Collection for this node. Creates the Set if it didn't exist previously.
         * @param v
         */
        private void add(Value v){
            if (values == null){
                this.values = new HashSet<>();
            }
            values.add(v);
        }
    }

    public TrieImpl(){
        this.root = new Node<>();
    }

    /**
     * add the given value at the given key
     * @param key
     * @param val
     */
    public void put(String key, Value val){
        if (key == null || val == null){
            throw new IllegalArgumentException("you can't put null into a trie");
        }
        this.root = this.put(this.root, key, val, 0);
    }

    /**
     * recursively traverses the trie to put val in the Collection for this trie, creating Nodes along the way if necessary
     * @param x the node we are visiting along the path
     * @param key
     * @param val
     * @param depth the character we are up to in the key
     * @return the Node we just updated
     */
    private Node put(Node x, String key, Value val, int depth){
        if (x == null){
            x = new Node();
        }
        if (depth == key.length()){
            x.add(val);
            return x;
        }
        char c = key.charAt(depth);
        x.links[c] = this.put(x.links[c], key, val, depth + 1);
        return x;
    }

    /**
     * Get all exact matches for the given key, sorted in descending order, where "descending" is defined by the comparator.
     * NOTE FOR COM1320 PROJECT: FOR PURPOSES OF A *KEYWORD* SEARCH, THE COMPARATOR SHOULD DEFINE ORDER AS HOW MANY TIMES THE KEYWORD APPEARS IN THE DOCUMENT.
     * Search is CASE SENSITIVE.
     * @param key
     * @param comparator used to sort values
     * @return a List of matching Values. Empty List if no matches.
     */
    public List<Value> getSorted(String key, Comparator<Value> comparator){
        if (key == null){
            throw new IllegalArgumentException("You can't find a null key in the trie");
        }
        Node x = this.get(this.root, key, 0);
        if (x == null || x.values == null){
            return new ArrayList<Value>();
        }
        List<Value> sortedValues = new ArrayList<>(x.values);
        sortedValues.sort(comparator);
        return sortedValues;
    }

    /**
     * get all exact matches for the given key.
     * Search is CASE SENSITIVE.
     * @param key
     * @return a Set of matching Values. Empty set if no matches.
     */
    public Set<Value> get(String key){
        if (key == null){
            throw new IllegalArgumentException("You can't find a null key in the trie");
        }
        Node x = this.get(this.root, key, 0);
        if (x == null || x.values == null){
            return new HashSet<Value>();
        }
        return x.values;
    }

    /**
     * recursively traverse the node to look for the given key
     * @param x the node we are visiting along the path
     * @param key
     * @param depth
     * @return The node that represents this key. null if this key isn't in the trie
     */
    private Node get(Node x, String key, int depth){
        if (x == null){
            return null;
        }
        if (depth == key.length()){
            return x;
        }
        char c = key.charAt(depth);
        return this.get(x.links[c], key, depth + 1);
    }

    /**
     * get all matches which contain a String with the given prefix, sorted in descending order, where "descending" is defined by the comparator.
     * NOTE FOR COM1320 PROJECT: FOR PURPOSES OF A *KEYWORD* SEARCH, THE COMPARATOR SHOULD DEFINE ORDER AS HOW MANY TIMES THE KEYWORD APPEARS IN THE DOCUMENT.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE SENSITIVE.
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order. Empty List if no matches.
     */
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator){
        if (prefix == null){
            throw new IllegalArgumentException("you didn't send a prefix to search");
        }
        Node pre = this.get(this.root, prefix, 0);
        if (pre == null){
            return new ArrayList<Value>();
        }
        Set<Value> vals = new HashSet<>();
        collectValues(pre, vals);
        List<Value> allWithPrefix = new ArrayList<>(vals);
        allWithPrefix.sort(comparator); //sorting documents based on prefix count is done in the comparator we are sent
        return allWithPrefix;
    }

    /**
     * collects all the values in x's subtrie into allValues
     * @param x
     * @param allValues
     */
    private void collectValues(Node x, Set<Value> allValues){
        if (x == null){
            return;
        }
        if (x.values != null){
            allValues.addAll(x.values);
        }
        for (int c = 0; c < 128; c++){
            if (x.links[c] != null){
                collectValues(x.links[c], allValues);
            }
        }
    }

    /**
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE SENSITIVE.
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    public Set<Value> deleteAllWithPrefix(String prefix){
        if (prefix == null){
            throw new IllegalArgumentException("there's no such thing as a null prefix");
        }
        Set<Value> deleted = new HashSet<>();
        this.deleteAllWithPrefix(this.root, prefix, 0, deleted);
        return deleted;
    }

    /**
     * deletes all keys in the subtree rooted by this prefix, and any nodes above it that now represent no keys
     * adds all the values we have deleted into deleted
     * @param x
     * @param prefix
     * @param depth
     * @param deleted
     * @return The node we have visited, if there is still a value below it. null if this node should be deleted
     */
    private Node deleteAllWithPrefix(Node x, String prefix, int depth, Set<Value> deleted){
        if (x == null){
            return null;
        }
        if (depth == prefix.length()){
            this.collectValues(x, deleted);
            x = null;
            return null;
        } else {
            char c = prefix.charAt(depth);
            x.links[c] = deleteAllWithPrefix(x.links[c], prefix, depth + 1, deleted);
        }
        if (x.values != null){
            return x;
        }
        for (int c = 0; c < 128; c++){
            if (x.links[c] != null) {
                return x;
            }
        }
        return null;
    }

    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     * @param key
     * @return a Set of all Values that were deleted.
     */
    public Set<Value> deleteAll(String key){
        if (key == null){
            throw new IllegalArgumentException("you can't delete a null key");
        }
        Set<Value> deleted = new HashSet<>();
        this.deleteAll(this.root, key, 0, deleted);
        return deleted;
    }

    /**
     * delete all the values from the set at this key, without removing any other keys that contain values
     * @param x
     * @param key
     * @param depth
     * @param deleted
     * @return a node which still contains a value. null if the node has no value
     */
    private Node deleteAll(Node x, String key, int depth, Set<Value> deleted){
        if (x == null){
            return null;
        }
        if (depth == key.length()){
            deleted = x.values;
            x.values = null;
        } else {
            char c = key.charAt(depth);
            x.links[c] = deleteAll(x.links[c], key, depth + 1, deleted);
        }
        if (x.values != null){
            return x;
        }
        for (int c = 0; c < 128; c++){
            if (x.links[c] != null){
                return x;
            }
        }
        return null;
    }

    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    public Value delete(String key, Value val){
        boolean[] wasDeleted = new boolean[1];
        this.delete(this.root, key, val, 0, wasDeleted);
        return wasDeleted[0] ? val : null;
    }

    /**
     * delete this one value from the set at this key. If this is the only value for that key, sets the nodes value to be null
     * @param x
     * @param key
     * @param val
     * @param depth
     * @param wasDeleted set to true or false if the value was deleted
     * @return the node within the key string who still has a value below it. null otherwise
     */
    private Node delete(Node x, String key, Value val, int depth, boolean[] wasDeleted){
        if (x == null){
            return null;
        }
        if (depth == key.length()){
            wasDeleted[0] = x.values.remove(val);
            if (x.values.isEmpty()){
                x.values = null;
            }
        } else {
            char c = key.charAt(depth);
            x.links[c] = delete(x.links[c], key, val, depth + 1, wasDeleted);
        }
        if (x.values != null){
            return x;
        }
        for (int c = 0; c < 128; c++){
            if (x.links[c] != null){
                return x;
            }
        }
        return null;
    }
}
