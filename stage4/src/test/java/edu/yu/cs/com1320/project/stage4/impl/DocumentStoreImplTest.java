package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    //tests the trie implementation in setMetaData
    public void testSetMetaDataTrie() throws IOException {
        store.put(input, uri1, DocumentStore.DocumentFormat.BINARY);
        store.setMetadata(uri1, "Author", "Matt");
        Document doc = store.get(uri1);
        Map<String, String> map = new HashMap<>();
        map.put("Author", "Matt");
        List<Document> docs = new ArrayList<>();
        docs.add(doc);
        Assertions.assertEquals(docs, store.searchByMetadata(map)); //this doc is in the trie at the right key
        store.setMetadata(uri1, "Genre", "Boring");
        map.put("Genre", "Boring");
        Assertions.assertEquals(docs, store.searchByMetadata(map)); //the doc has both keys
        store.setMetadata(uri1, "Author", "NotMatt");
        Assertions.assertEquals(new ArrayList<Document>(), store.searchByMetadata(map)); //the doc doesn't match both keys
        map.put("Author", "NotMatt");
        Assertions.assertEquals(docs, store.searchByMetadata(map)); //the doc matches the new key
        store.setMetadata(uri1, "Author", null);
        Assertions.assertEquals(new ArrayList<Document>(), store.searchByMetadata(map)); //setting to null takes it out
        store.undo(uri1);
        Assertions.assertEquals(docs, store.searchByMetadata(map)); //goes back to what it was
        store.undo(uri1);
        map.put("Author", "Matt");
        Assertions.assertEquals(docs, store.searchByMetadata(map)); //goes back to previous value
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
    //tests that the put method properly deals with the trie
    public void testPutTrie() throws IOException {
        String text = "these are the words in the document. Lets write the words again so that their word count is higher";
        InputStream in = new ByteArrayInputStream((text.getBytes()));
        store.put(in, uri1, DocumentStore.DocumentFormat.TXT);
        DocumentImpl doc = new DocumentImpl(uri1, text);
        List<Document> docs = new ArrayList<>();
        docs.add(doc);
        Assertions.assertEquals(docs, store.search("words")); //this document is now in the trie
        String newText = "This is another example of document content. The above keyword should no longer have this document as a value";
        InputStream newIn = new ByteArrayInputStream(newText.getBytes());
        store.put(newIn, uri1, DocumentStore.DocumentFormat.TXT);
        DocumentImpl newDoc = new DocumentImpl(uri1, newText);
        docs.remove(doc);
        docs.add(newDoc);
        Assertions.assertEquals(new ArrayList<Document>(), store.search("words")); //this keyword no longer contains the doc
        Assertions.assertEquals(docs, store.search("content")); //the new doc's key is there
        store.put(null, uri1, DocumentStore.DocumentFormat.TXT);
        Assertions.assertEquals(new ArrayList<Document>(), store.search("content")); //put(null) means the doc is no longer there
        store.undo(uri1);
        Assertions.assertEquals(docs, store.search("content")); //undo puts it back
        store.undo(uri1);
        docs.remove(newDoc);
        docs.add(doc);
        Assertions.assertEquals(docs, store.search("words")); //undo again puts the previous one back
        store.undo();
        Assertions.assertEquals(new ArrayList<Document>(), store.search("words")); //undo again makes it null again
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

    @Test
    //tests that the delete properly deals with the trie
    public void testDeleteTrie() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        store.delete(uri1);
        Assertions.assertEquals(new ArrayList<Document>(), store.search("wood"));
        store.undo(uri1);
        List<Document> docs = new ArrayList<>();
        docs.add(doc);
        Assertions.assertEquals(docs, store.search("wood"));
    }

    public DocumentImpl setUpTrieForSearch() throws IOException {
        String text = "How much wood could a woodchuck chuck if a woodchuck could chuck wood. This String has the words wood, woodchuck, and could three times. Once I write word word word word, it also has the word word six times.";
        InputStream in = new ByteArrayInputStream(text.getBytes());
        store.put(in, uri1, DocumentStore.DocumentFormat.TXT);
        return new DocumentImpl(uri1, text);
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

    //Searching and deleting added in Stage 4

    @Test
    //tests that search removes the Illegal characters, returns in the right order
    public void testSearch() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        List<Document> docs = new ArrayList<>();
        docs.add(doc);
        Assertions.assertEquals(docs, store.search(".w'o(o#d"));
        String wood = "wood";
        InputStream in = new ByteArrayInputStream(wood.getBytes());
        store.put(in, uri2, DocumentStore.DocumentFormat.TXT);
        docs.add(new DocumentImpl(uri2, wood));
        Assertions.assertEquals(docs, store.search("wood"));
    }

    @Test
    //tests the searchByPrefix method
    public void testSearchByPrefix() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        List<Document> docs = new ArrayList<>();
        docs.add(doc);
        Assertions.assertEquals(docs, store.searchByPrefix("wo"));
    }

    @Test
    //tests the deleteAll method
    public void testDeleteAll() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        store.deleteAll("wood");
        Assertions.assertNull(store.get(uri1));
    }

    @Test
    //tests the deleteAllWithPrefix method
    public void testDeleteAllWithPrefix() throws IOException{
        DocumentImpl doc = setUpTrieForSearch();
        store.deleteAllWithPrefix("wo");
        Assertions.assertNull(store.get(uri1));
    }

    @Test
    //tests the searchByKeywordAndMetadata method
    public void testSearchByKeywordAndMetadata() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        store.setMetadata(uri1, "Author", "Matt");
        Map<String, String> map = new HashMap<>();
        map.put("Author", "Matt");
        List<Document> docs = new ArrayList<>();
        docs.add(doc);
        Assertions.assertEquals(docs, store.searchByKeywordAndMetadata("wood", map));
    }

    @Test
    //tests the searchByPrefixAndMetadata method
    public void testSearchByPrefixAndMetadata() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        store.setMetadata(uri1, "Author", "Matt");
        Map<String, String> map = new HashMap<>();
        map.put("Author", "Matt");
        List<Document> docs = new ArrayList<>();
        docs.add(doc);
        Assertions.assertEquals(docs, store.searchByPrefixAndMetadata("wo", map));
    }

    @Test
    //tests deleteAllWithMetadata
    public void testDeleteAllWithMetadata() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        store.setMetadata(uri1, "Author", "Matt");
        Map<String, String> map = new HashMap<>();
        map.put("Author", "Matt");
        store.deleteAllWithMetadata(map);
        Assertions.assertNull(store.get(uri1));
    }

    @Test
    //tests deleteAllWithKeywordAndMetadata
    public void testDeleteAllWithKeywordAndMetadata() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        store.setMetadata(uri1, "Author", "Matt");
        Map<String, String> map = new HashMap<>();
        map.put("Author", "Matt");
        store.deleteAllWithKeywordAndMetadata("wood", map);
        Assertions.assertNull(store.get(uri1));
    }

    @Test
    //tests deleteAllWithPrefixAndMetadata
    public void testDeleteAllWithPrefixAndMetadata() throws IOException {
        DocumentImpl doc = setUpTrieForSearch();
        store.setMetadata(uri1, "Author", "Matt");
        Map<String, String> map = new HashMap<>();
        map.put("Author", "Matt");
        store.deleteAllWithPrefixAndMetadata("wo", map);
        Assertions.assertNull(store.get(uri1));
    }
}