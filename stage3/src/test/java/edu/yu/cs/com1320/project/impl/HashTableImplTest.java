package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

public class HashTableImplTest {
    private HashTableImpl<String, String> table;

    @BeforeEach
    public void beforeEach(){
        table = new HashTableImpl<>();
    }

    @Test
    //tests the get method, when they key does or does not exist
    public void testGet(){
        table.put("key", "value");
        Assertions.assertEquals("value", table.get("key"));
        Assertions.assertNull(table.get("not key"));
    }
    @Test
    //tests the put method when a value is actually sent, that it properly puts, increments size, returns previous
    public void testPutNormal(){
        String noPrevious = table.put("key", "value");
        Assertions.assertEquals("value", table.get("key"));
        Assertions.assertEquals(1, table.size());
        Assertions.assertNull(noPrevious);
        String previous = table.put("key", "new value");
        Assertions.assertEquals("new value", table.get("key"));
        Assertions.assertEquals(1, table.size());
        Assertions.assertEquals("value", previous);
    }

    @Test
    //tests the put method when a delete is intended
    public void testPutDelete(){
        table.put("key", "value");
        table.put("other key", "other value");
        String previous = table.put("other key", null);
        Assertions.assertEquals("other value", previous);
        Assertions.assertFalse(table.containsKey("other key"));
        Assertions.assertEquals(1, table.size());
        String nextPrevious = table.put("other key", null);
        Assertions.assertNull(nextPrevious);
        Assertions.assertEquals(1, table.size());
    }

    @Test
    //tests that the table is resized and the elements are still accesible
    public void testPutResize(){
        HashTableImpl<Integer, Integer> nums = new HashTableImpl<>();
        for (int i = 0; i < 50; i++){
            nums.put(i, i*2);
        }
        for (int i = 0; i < 50; i++){
            Assertions.assertEquals(i*2, nums.get(i));
        }
    }

    @Test
    //tests the contains key method for all scenarios
    public void testContainsKey(){
        table.put("k", "v");
        Assertions.assertTrue(table.containsKey("k"));
        Assertions.assertFalse(table.containsKey("key"));
        Assertions.assertThrows(NullPointerException.class, ()->{
            table.containsKey(null);
        });
    }

    @Test
    //tests the keySet method, that it changes correctly, returns an unmodifiable set
    public void testKeySet(){
        Set<String> keys = new HashSet<>();
        Assertions.assertEquals(keys, table.keySet());
        table.put("a", "v");
        table.put("b", "va");
        table.put("c", "val");
        table.put("f", "valu"); //test the separate chaining
        table.put("k", "value");
        keys.add("a");
        keys.add("b");
        keys.add("c");
        keys.add("f");
        keys.add("k");
        Assertions.assertEquals(keys, table.keySet());
        table.put("b", null);
        keys.remove("b");
        Assertions.assertEquals(keys, table.keySet());
        Set<String> unmodifiable = table.keySet();
        Assertions.assertThrows(UnsupportedOperationException.class, ()->{
            unmodifiable.add("key");
        });
    }

    @Test
    //tests the values method, that it changes correctly, returns an unmodifiable collection
    public void testValues(){
        List<String> values = new ArrayList<>();
        Assertions.assertEquals(values, new ArrayList<>(table.values()));
        table.put("a", "v");
        table.put("b", "va");
        table.put("c", "val");
        table.put("f", "valu"); //test the separate chaining
        table.put("k", "value");
        //the order is due to how the hashcodes come out
        values.add("value");
        values.add("valu");
        values.add("v");
        values.add("va");
        values.add("val");
        Assertions.assertEquals(values, new ArrayList<>(table.values()));
        table.put("b", null);
        values.remove("va");
        Assertions.assertEquals(values, new ArrayList<>(table.values()));
        Collection<String> unmodifiable = table.values();
        Assertions.assertThrows(UnsupportedOperationException.class, ()->{
            unmodifiable.add("some value");
        });
    }

    @Test
    //tests the size method in various cases
    public void testSize(){
        Assertions.assertEquals(0, table.size());
        table.put("key", "value");
        table.put("keys", "values");
        table.put("a", "b");
        Assertions.assertEquals(3, table.size());
        table.put("key", null);
        Assertions.assertEquals(2, table.size());
    }
}
