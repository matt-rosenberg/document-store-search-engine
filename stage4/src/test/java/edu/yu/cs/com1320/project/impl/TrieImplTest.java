package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

public class TrieImplTest {
    private TrieImpl<Integer> trie;
    Comparator<Integer> comp;
    Set<Integer> values;

    @BeforeEach
    public void beforeEach(){
        this.trie = new TrieImpl<>();
        this.comp = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1 - o2;
            }
        };
        values = new HashSet<>();
    }

    @Test
    //put throws IAE when sent a null key or value
    public void testPutThrows(){
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            trie.put(null, 1);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            trie.put("key", null);
        });
    }

    @Test
    //tests that put works for simple cases
    public void testPutSimple(){
        trie.put("key1", 1);
        values.add(1);
        Assertions.assertEquals(values, trie.get("key1"));

        trie.put("key1", 2);
        values.add(2);
        Assertions.assertEquals(values, trie.get("key1"));

        trie.put("key12", 7);
        Set<Integer> v = new HashSet<>();
        v.add(7);
        Assertions.assertEquals(v, trie.get("key12"));

        Set<Integer> empty = new HashSet<>();
        Assertions.assertEquals(empty, trie.get("Key1"));
        Assertions.assertEquals(empty, trie.get("key"));

        trie.put("thisisareallylongkeytotestthetrie", 7);
        Assertions.assertEquals(v, trie.get("thisisareallylongkeytotestthetrie"));

        trie.put(".o't,h-e[r", 7);
        Assertions.assertEquals(v, trie.get(".o't,h-e[r"));
    }

    public void setUpTrie(){
        trie.put("key", 1);
        trie.put("key", 5);
        trie.put("key", 10);
        trie.put("key", 4);
        trie.put("key", 43);
        trie.put("key", 9);
        trie.put("key", 18);
        trie.put("key", 72);
        trie.put("key", 14);
        values.add(1);
        values.add(5);
        values.add(10);
        values.add(4);
        values.add(43);
        values.add(9);
        values.add(18);
        values.add(72);
        values.add(14);
        trie.put("word", 22);
        trie.put("words", 11);
        trie.put("words", 12);
        trie.put("keypre", 0);
        trie.put("keyprefix", 100);
    }

    @Test
    //tests that getSorted throws when it should
    public void testGetSortedThrows(){
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
           trie.getSorted(null, comp);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            trie.getSorted("key", null);
        });
    }

    @Test
    //getSorted returns an empty list when there was nothing there
    public void testGetSortedNone(){
        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(empty, trie.getSorted("key", comp));
        this.setUpTrie();
        Assertions.assertEquals(empty, trie.getSorted("nonexistentkey", comp));
    }

    @Test
    //getSorted returns the right value when there is only one value
    public void testGetSortedOne(){
        List<Integer> oneInt = new ArrayList<>();
        oneInt.add(0);
        this.setUpTrie();
        Assertions.assertEquals(oneInt, trie.getSorted("keypre", comp));
    }

    @Test
    //getSorted returns the list in sorted order
    public void testGetSortedSorts(){
        setUpTrie();
        List<Integer> ints = new ArrayList<>();
        ints.add(1);
        ints.add(4);
        ints.add(5);
        ints.add(9);
        ints.add(10);
        ints.add(14);
        ints.add(18);
        ints.add(43);
        ints.add(72);
        Assertions.assertEquals(ints, trie.getSorted("key", comp));
    }

    @Test
    //tests that get throws if the key is null
    public void testGetThrows(){
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
           trie.get(null);
        });
    }

    @Test
    //get returns an empty set if there is no value
    public void testGetEmpty(){
        Assertions.assertEquals(values, trie.get("no"));
        Assertions.assertEquals(values, trie.get("ke"));
    }

    @Test
    //get returns the values at this key
    public void testGetNormal(){
        setUpTrie();
        Assertions.assertEquals(values, trie.get("key"));
    }

    @Test
    //tests that getAllWithPrefix throws when given invalid arguments
    public void testGetAllWithPrefixThrows(){
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            trie.getAllWithPrefixSorted(null, comp);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            trie.getAllWithPrefixSorted("key", null);
        });
    }

    @Test
    //tests that getAllWithPrefix returns an empty set if the prefix isn't there
    public void testGetAllWithPrefixEmpty(){
        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(empty, trie.getAllWithPrefixSorted("no", comp));
    }

    @Test
    //getAllWithPrefix returns the right values in sorted order
    public void testGetAllWithPrefixSortedSort(){
        setUpTrie();
        List<Integer> ints = new ArrayList<>();
        ints.add(0);
        ints.add(1);
        ints.add(4);
        ints.add(5);
        ints.add(9);
        ints.add(10);
        ints.add(14);
        ints.add(18);
        ints.add(43);
        ints.add(72);
        ints.add(100);
        Assertions.assertEquals(ints, trie.getAllWithPrefixSorted("key", comp));
        List<Integer> words = new ArrayList<>();
        words.add(11);
        words.add(12);
        words.add(22);
        Assertions.assertEquals(words, trie.getAllWithPrefixSorted("wor", comp));
    }

    @Test
    //tests that all the delete methods throw if given invalid args
    public void testDeletesThrows(){
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            trie.deleteAll(null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            trie.deleteAllWithPrefix(null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            trie.delete(null, 1);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            trie.delete("key", null);
        });
    }

    @Test
    //deleteAllWithPrefix returns the correct set of values
    public void testDeleteAllWithPrefixReturns(){
        setUpTrie();
        values.add(0);
        values.add(100);
        Assertions.assertEquals(values, trie.deleteAllWithPrefix("ke"));
    }

    @Test
    //deleteAllWithPrefix deletes everything under the prefix
    public void testDeleteAllWithPrefixDeletes(){
        setUpTrie();
        trie.deleteAllWithPrefix("key");
        Set<Integer> empty = new HashSet<>();
        Assertions.assertEquals(empty, trie.get("key"));
        Assertions.assertEquals(empty, trie.get("keyprefix"));
    }

    @Test
    //deleteAll returns the set of deleted values
    public void testDeleteAllReturns(){
        setUpTrie();
        Assertions.assertEquals(values, trie.deleteAll("key"));
    }

    @Test
    //deleteAll deletes all the values at this node, but everything under it remains
    public void testDeleteAllDeletes(){
        setUpTrie();
        trie.deleteAll("key");
        Set<Integer> empty = new HashSet<>();
        Assertions.assertEquals(empty, trie.get("key"));
        Set<Integer> notEmpty = new HashSet<>();
        notEmpty.add(100);
        Assertions.assertEquals(notEmpty, trie.get("keyprefix"));
    }

    @Test
    //delete returns null when there is no such key-value pair to delete. returns the correct value when it's deleted
    public void testDeleteReturns(){
        setUpTrie();
        Assertions.assertNull(trie.delete("apple", 1));
        Assertions.assertEquals(18, trie.delete("key", 18));
    }

    @Test
    //delete will delete this node if there are no more values in it
    public void testDeleteDeletesNode(){
        setUpTrie();
        trie.delete("word", 22);
        Assertions.assertEquals(new HashSet<Integer>(), trie.get("word"));
    }

    @Test
    //delete doesn't delete other values at this key
    public void testDeleteMoreValuesAtNode(){
        setUpTrie();
        trie.delete("key", 14);
        values.remove(14);
        Assertions.assertEquals(values, trie.get("key"));
    }
}