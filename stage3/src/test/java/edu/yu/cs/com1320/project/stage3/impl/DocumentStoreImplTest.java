package edu.yu.cs.com1320.project.stage3.impl;


import edu.yu.cs.com1320.project.stage3.DocumentStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DocumentStoreImplTest {

    private DocumentStoreImpl store;
    private URI uri1, uri2, uri3, uri4;
    private InputStream input;
    private byte[] basicBytes = new byte[] {1,2,3,4,5,6,7};

    @BeforeEach
    public void beforeEach(){
        uri1 = URI.create("https://fakeurl1.com");
        uri2 = URI.create("https://fakeurl2.com");
        uri3 = URI.create("https://fakeurl3.com");
        uri4 = URI.create("https://fakeurl4.com");
        store = new DocumentStoreImpl();
        input = new ByteArrayInputStream(basicBytes);
    }

    @Test
    //tests that the stores setMetaData throws IAEs
    public void testDocumentStoreSetMetaDataFailures() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        //null uri
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.setMetadata(null, "key", "value");
        });
        //empty uri
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.setMetadata(URI.create("   "), "key", "value");
        });
        //null key
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.setMetadata(uri1, null, "value");
        });
        //empty key
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.setMetadata(uri1, "   ", "value");
        });
        //no document here
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.setMetadata(uri2, "key", "value");
        });
    }

    @Test
    //tests setMetaData when it runs properly
    public void testSetMetaDataNormal() throws IOException{
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        String noOld = store.setMetadata(uri1, "key", "value");
        Assertions.assertNull(noOld);
        String expected = "value";
        Assertions.assertEquals(expected, store.get(uri1).getMetadataValue("key"));
        String old = store.setMetadata(uri1, "key", "new value");
        Assertions.assertEquals("value", old);
    }

    @Test
    //tests getMetaData when it is supposed to throw
    public void testGetMetaDataFailures() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        store.setMetadata(uri1, "key", "value");
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.getMetadata(null, "key");
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.getMetadata(URI.create("   "), "key");
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.getMetadata(uri2, "key");
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.getMetadata(uri1, null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.getMetadata(uri1, "   ");
        });
    }

    @Test
    //tests getMetaData when nothing is wrong
    public void testGetMetaDataNormal() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        store.setMetadata(uri1, "key", "value");
        Assertions.assertEquals("value", store.getMetadata(uri1, "key"));
        Assertions.assertNull(store.getMetadata(uri1, "not a key"));
    }

    @Test
    //tests that the put method will throw the IAEs
    public void testPutIllegalArgs(){
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.put(input, null, DocumentStore.DocumentFormat.BINARY);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.put(input, URI.create("    "), DocumentStore.DocumentFormat.BINARY);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            store.put(input, uri1, null);
        });
    }

    @Test
    //tests that the put method throws the IOException
    public void testPutBadInput() throws IOException {
        InputStream badInput = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException();
            }
        };
        Assertions.assertThrows(IOException.class, ()->{
            store.put(badInput, uri1, DocumentStore.DocumentFormat.BINARY);
        });
    }

    @Test
    //tests that put will make the document at a key null
    public void testPutNullInput() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        store.put(null, uri1, DocumentStore.DocumentFormat.BINARY);
        Assertions.assertNull(store.get(uri1));
    }

    @Test
    //tests that the document is put, and the method returns the right value, by a binary doc
    public void testPutNormalBinaryDoc() throws IOException {
        //if there's no previous, returns 0
        int noPrevious = store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        Assertions.assertEquals(0, noPrevious);
        //if there is a previous, returns its hashcode
        int previousHashCode = store.get(uri1).hashCode();
        byte[] newBytes = new byte[] {50,75,34,26};
        InputStream newInput = new ByteArrayInputStream(newBytes);
        int previous = store.put(newInput, uri1, DocumentStore.DocumentFormat.BINARY);
        Assertions.assertEquals(previousHashCode, previous);
        //puts the right document in the map
        DocumentImpl thisDoc = new DocumentImpl(uri1, newBytes);
        Assertions.assertEquals(thisDoc, store.get(uri1));
    }

    @Test
    //tests that put acts normally when sent text
    public void testPutNormalTxtDoc() throws IOException {
        String s = new String(basicBytes); //the same content that input contains
        store.put(input, uri1, DocumentStore.DocumentFormat.TXT);
        DocumentImpl txtDoc = new DocumentImpl(uri1, s);
        Assertions.assertEquals(txtDoc, store.get(uri1));
        int txthash = store.put(null, uri1, DocumentStore.DocumentFormat.TXT);
        Assertions.assertEquals(txtDoc.hashCode(), txthash);
    }

    @Test
    //tests the stores get method
    public void testGet() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        DocumentImpl thedoc = new DocumentImpl(uri1, basicBytes);
        Assertions.assertEquals(thedoc, store.get(uri1));
        Assertions.assertNull(store.get(uri2));
    }

    @Test
    //tests the delete method
    public void testDelete() throws IOException {
        //it does the delete, it returns correct thing
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        boolean shouldBeTrue = store.delete(uri1);
        Assertions.assertNull(store.get(uri1));
        Assertions.assertTrue(shouldBeTrue);
        Assertions.assertFalse(store.delete(uri2));
        Assertions.assertFalse(store.delete(null));
    }

    //UNDO LOGIC ADDED IN STAGE 3

    @Test
    //tests that undo throws an ISE when the stack is empty, undo(uri) throws when there are no commands for this uri
    public void testUndoThrows() throws IOException {
        Assertions.assertThrows(IllegalStateException.class, ()->{
            store.undo();
        });
        store.put(input, uri2, DocumentStore.DocumentFormat.BINARY);
        Assertions.assertThrows(IllegalStateException.class, ()->{
            store.undo(uri1);
        });
        store.undo();
        Assertions.assertThrows(IllegalStateException.class, ()->{
            store.undo(uri2);
        });
    }

    @Test
    //tests that a call to setMetaData can be undone properly
    public void testUndoSetMetaData() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        store.setMetadata(uri1, "key", "value");
        store.setMetadata(uri1, "key", "new value");
        store.undo();
        //when there was a previous value
        Assertions.assertEquals("value", store.getMetadata(uri1, "key"));
        store.undo();
        //when the value was null
        Assertions.assertNull(store.getMetadata(uri1, "key"));
    }

    @Test
    //tests that put properly implements the undo logic
    public void testUndoPut() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        byte[] newBytes = new byte[] {5,35,39,66};
        InputStream newInput = new ByteArrayInputStream(newBytes);
        store.put(newInput, uri1, DocumentStore.DocumentFormat.TXT);
        //when there was a previous document
        store.undo();
        DocumentImpl first = new DocumentImpl(uri1, basicBytes);
        Assertions.assertEquals(first, store.get(uri1));
        //when the previous was null
        store.undo();
        Assertions.assertNull(store.get(uri1));
    }

    @Test
    //tests that a delete command can be undone
    public void testUndoDelete() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        store.delete(uri1);
        store.undo();
        //undoes a delete
        DocumentImpl doc = new DocumentImpl(uri1, basicBytes);
        Assertions.assertEquals(doc, store.get(uri1));
    }
    @Test
    //tests that undo(uri) performs the correct undo, leaves everything else unchanged
    public void testUndoUri() throws IOException{
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        DocumentImpl doc = (DocumentImpl) store.get(uri1);
        input.reset();
        store.put(input, uri2, DocumentStore.DocumentFormat.BINARY);
        DocumentImpl secondDoc = (DocumentImpl) store.get(uri2);
        input.reset();
        store.put(input, uri3, DocumentStore.DocumentFormat.BINARY);
        DocumentImpl thirdDoc = (DocumentImpl) store.get(uri3);
        byte[] newBytes = new byte[] {5,35,39,66};
        InputStream newInput = new ByteArrayInputStream(newBytes);
        store.put(newInput, uri1, DocumentStore.DocumentFormat.TXT);
        //when the command for this uri is on top
        store.undo(uri1);
        Assertions.assertEquals(doc, store.get(uri1));
        //when it's in middle/on bottom
        store.undo(uri1);
        Assertions.assertNull(store.get(uri1));
        //other commands are unchanged
        Assertions.assertEquals(thirdDoc, store.get(uri3));
        store.undo();
        Assertions.assertNull(store.get(uri3));
        Assertions.assertEquals(secondDoc, store.get(uri2));
        store.undo();
        Assertions.assertNull(store.get(uri2));
    }
}
