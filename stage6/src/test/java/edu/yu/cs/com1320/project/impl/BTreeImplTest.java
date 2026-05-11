package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;
import edu.yu.cs.com1320.project.stage6.impl.DocumentImpl;
import edu.yu.cs.com1320.project.stage6.impl.DocumentPersistenceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

public class BTreeImplTest {

    private BTreeImpl<URI, Document> tree;
    private PersistenceManager<URI, Document> dpm;
    URI uri1 = URI.create("https://fakeurl1.com");
    URI uri2 = URI.create("https://fakeurl2.com");
    URI uri3 = URI.create("https://fakeurl3.com");

    @BeforeEach
    public void beforeEach() {
        tree = new BTreeImpl<>();
        dpm = new DocumentPersistenceManager(null);
        tree.setPersistenceManager(dpm);
    }

    private Document makeDoc(String text) {
        return new DocumentImpl(uri1, text, null);
    }

    @Test
    public void testPutAndGet(){
        Document doc = makeDoc("text");
        tree.put(uri1, doc);
        Assertions.assertEquals(doc, tree.get(uri1));
    }

    @Test
    public void testOverwriteDocument(){
        Document doc = makeDoc("text");
        Document doc2 = makeDoc("other");
        tree.put(uri1, doc);
        Document previous = tree.put(uri1, doc2);
        Assertions.assertEquals(doc, previous);
        Assertions.assertEquals(doc2, tree.get(uri1));
    }

    @Test
    public void testGetNotThere(){
        Assertions.assertNull(tree.get(uri1));
    }

    @Test
    public void testDelete(){
        Document doc = makeDoc("text");
        tree.put(uri1, doc);
        Document previous = tree.put(uri1, null);
        Assertions.assertEquals(doc, previous);
        Assertions.assertNull(tree.get(uri1));
    }

    @Test
    public void testMoveToDisk() throws IOException {
        Document doc = makeDoc("text");
        tree.put(uri1, doc);
        tree.moveToDisk(uri1);
        Assertions.assertEquals(doc, tree.get(uri1));
    }

    @Test
    public void testMultipleDocs(){
        Document doc = makeDoc("one");
        Document doc2 = new DocumentImpl(uri2, "two", null);
        Document doc3 = new DocumentImpl(uri3, "three", null);
        tree.put(uri1, doc);
        tree.put(uri2, doc2);
        tree.put(uri3, doc3);
        Assertions.assertEquals(doc, tree.get(uri1));
        Assertions.assertEquals(doc2, tree.get(uri2));
        Assertions.assertEquals(doc3, tree.get(uri3));
    }

    @Test
    public void testOverwriteFromDisk() throws IOException {
        Document doc = makeDoc("text");
        Document doc2 = makeDoc("other");
        tree.put(uri1, doc);
        tree.moveToDisk(uri1);
        Document previous = tree.put(uri1, doc2);
        Assertions.assertEquals(doc, previous);
        Assertions.assertEquals(doc2, tree.get(uri1));
        tree.moveToDisk(uri1);
        Document second = tree.put(uri1, null);
        Assertions.assertEquals(doc2, second);
        Assertions.assertNull(tree.get(uri1));
    }

    @Test
    public void testGetTakesOffDisk() throws IOException{
        Document doc = makeDoc("text");
        tree.put(uri1, doc);
        tree.moveToDisk(uri1);
        tree.get(uri1);
        Assertions.assertNull(dpm.deserialize(uri1));
    }
}
