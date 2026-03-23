package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.impl.HashTableImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashSet;

public class DocumentImplTest {

    private DocumentImpl doc;
    private URI baseUri, uri1, uri2, uri3;

    @BeforeEach
    //sets up a couple URIs and a basic Document object
    public void beforeEach(){
        uri1 = URI.create("https://fakeurl1.com");
        uri2 = URI.create("https://fakeurl2.com");
        uri3 = URI.create("https://fakeurl3.com");
        baseUri = URI.create("https://fakeurl0.com");
        doc = new DocumentImpl(baseUri, "base");
    }

    @Test
    //Test that all the invalid arguments throw IAE for the text constructor
    public void testTxtConstructorFailures(){
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(null, "hey");
        });
        URI emptyUri = URI.create("");
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(emptyUri, "hey");
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(URI.create("   "), "hey");
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(uri1, (String) null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(uri2, "   ");
        });
    }

    @Test
    //Tests a regular valid case
    public void testTxtConstructorNormal(){
        String str = "This should work";
        doc = new DocumentImpl(uri1, str);
        Assertions.assertEquals(uri1, doc.getKey());
        Assertions.assertEquals(str, doc.getDocumentTxt());
        Assertions.assertNull(doc.getDocumentBinaryData());
        Assertions.assertTrue(doc.getMetadata() instanceof HashTableImpl && doc.getMetadata().size() == 0);
    }

    @Test
    //Tests the invalid inputs for the binary constructor
    public void testBinaryConstructorFailures(){
        byte[] testBytes = new byte[] {5,6,7};
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(null, testBytes);
        });
        URI emptyUri = URI.create("");
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(emptyUri, testBytes);
        });

        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(URI.create("   "), testBytes);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(uri1, (byte[]) null);
        });
        byte[] blankBytes = new byte[0];
        Assertions.assertThrows(IllegalArgumentException.class, ()->{
            doc = new DocumentImpl(uri3, blankBytes);
        });
    }

    @Test
    //A valid case of the binary constructor
    public void testBinaryConstructorNormal(){
        byte[] binary = new byte[] {1,2,3,4,5};
        doc = new DocumentImpl(uri2, binary);
        Assertions.assertEquals(uri2, doc.getKey());
        Assertions.assertEquals(binary, doc.getDocumentBinaryData());
        Assertions.assertNull(doc.getDocumentTxt());
        Assertions.assertTrue(doc.getMetadata() instanceof HashTableImpl && doc.getMetadata().size() == 0);
    }

    @Test
    //tests that setMetaData throws IAE on invalid inputs, stores correctly in a valid case
    public void testSetMetaDataValue(){
        doc = new DocumentImpl(uri1, "Hello World");
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            doc.setMetadataValue(null, "value");
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            doc.setMetadataValue("  ", "value");
        });
        String key = "Document Author";
        String value = "Matt";
        String old = doc.setMetadataValue(key, value);
        Assertions.assertEquals(value, doc.getMetadataValue(key));
        Assertions.assertNull(old);
        String nextOld = doc.setMetadataValue(key, "Zach");
        Assertions.assertEquals(value, nextOld);
    }

    @Test
    //Tests that getMetaData throws IAEs, runs correctly in valid case
    public void testGetMetaDataValue(){
        doc = new DocumentImpl(uri1, "Hello World");
        String key = "length";
        String value = "six-seven";
        doc.setMetadataValue(key, value);
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            doc.getMetadataValue(null);
        });
        Assertions.assertThrows(IllegalArgumentException.class, ()-> {
            doc.getMetadataValue("  ");
        });
        Assertions.assertNull(doc.getMetadataValue("nonexistent"));
        Assertions.assertEquals(value, doc.getMetadataValue(key));
    }

    @Test
    //tests that getMetaData returns the expected table, and that it only returns a copy
    public void testGetMetaData(){
        doc = new DocumentImpl(uri1, "hello");
        doc.setMetadataValue("author", "Me");
        doc.setMetadataValue("genre", "cs");
        HashTableImpl<String, String> copy = (HashTableImpl) doc.getMetadata();
        //check that the size and key-value pairs of copy are good
        Assertions.assertEquals("Me", copy.get("author"));
        Assertions.assertEquals("cs", copy.get("genre"));
        Assertions.assertEquals(2, copy.size());
        copy.put("type", "text");
        //it only made a copy so this key is not in doc's table
        Assertions.assertNull(doc.getMetadataValue("type"));
    }

    @Test
    //checks that the method returns the expected text
    public void testGetDocumentTxtNormal(){
        doc = new DocumentImpl(uri1, "tester text");
        String text = doc.getDocumentTxt();
        Assertions.assertEquals("tester text", text);
    }

    @Test
    //tests that the method returns null when there is no txt
    public void testGetDocumentTxtBad(){
        byte[] notTxt = new byte[] {10,20,30};
        doc = new DocumentImpl(uri2, notTxt);
        Assertions.assertNull(doc.getDocumentTxt());
    }

    @Test
    //tests that it returns the expected in a normal case
    public void testGetDocumentBinaryDataNormal(){
        byte[] goodBytes = new byte[] {45,10,16,86,43};
        doc = new DocumentImpl(uri3, goodBytes);
        Assertions.assertEquals(goodBytes, doc.getDocumentBinaryData());
    }

    @Test
    //tests that it returns null when there are no bytes
    public void testGetDocumentBinaryDataBad(){
        doc = new DocumentImpl(uri1, "notBytes");
        Assertions.assertNull(doc.getDocumentBinaryData());
    }

    @Test
    //tests the getKey method for both types of documents
    public void testGetKey(){
        doc = new DocumentImpl(uri1, "text");
        Assertions.assertEquals(uri1, doc.getKey());
        byte[] b = new byte[] {1,2,3,4,5,6,7};
        doc = new DocumentImpl(uri2, b);
        Assertions.assertEquals(uri2, doc.getKey());
    }

    @Test
    //checks that the hashcode method fills its contract
    public void testDocumentImplHashCode(){
        //equal objects have the same hashcode
        doc = new DocumentImpl(uri1, "hashCode");
        DocumentImpl doc2 = new DocumentImpl(uri1, "hashCode");
        Assertions.assertEquals(doc, doc2);
        Assertions.assertEquals(doc.hashCode(), doc2.hashCode());
        //hashcode stays consistent
        int one = doc.hashCode();
        int two = doc.hashCode();
        Assertions.assertEquals(one, two);
        //different document objects usually have different hashcodes
        doc2 = new DocumentImpl(uri3, "not the same hashcode");
        Assertions.assertNotEquals(doc.hashCode(), doc2.hashCode());
        doc2 = new DocumentImpl(uri1, "hashCode".getBytes());
        Assertions.assertNotEquals(doc.hashCode(), doc2.hashCode());
    }

    @Test
    //tests that equals acts appropriately when they should be equal
    public void testDocumentImplEqualsGood(){
        //text documents
        doc = new DocumentImpl(uri1, "equal");
        DocumentImpl doc2 = new DocumentImpl(uri1, "equal");
        DocumentImpl doc3 = doc;
        Assertions.assertTrue(doc.equals(doc2));
        Assertions.assertTrue(doc.equals(doc3));
        //binary data documents
        byte[] first = new byte[] {1,3,5,7};
        byte[] second = new byte[] {1,3,5,7};
        doc = new DocumentImpl(uri2, first);
        doc2 = new DocumentImpl(uri2, second);
        DocumentImpl doc4 = doc;
        Assertions.assertTrue(doc.equals(doc2));
        Assertions.assertTrue(doc.equals(doc4));
    }

    @Test
    //tests that equals works when they aren't equal
    public void testDocumentImplEqualsBad(){
        //compare to null
        doc = new DocumentImpl(uri1, "txt");
        DocumentImpl doc2 = null;
        Assertions.assertFalse(doc.equals(doc2));
        //compare to a non-document
        String s = "not a document";
        Assertions.assertFalse(doc.equals(s));
        //compare to a text doc with a different uri
        doc2 = new DocumentImpl(uri2, "txt");
        Assertions.assertFalse(doc.equals(doc2));
        //compare to doc with same url, different text
        doc2 = new DocumentImpl(uri1, "differenttxt");
        Assertions.assertFalse(doc.equals(doc2));
        //compare to a binary doc with a different uri
        byte[] b = new byte[] {1,2,3};
        doc = new DocumentImpl(uri1, b);
        doc2 = new DocumentImpl(uri2, b);
        Assertions.assertFalse(doc.equals(doc2));
        //compare to doc with different bytes
        byte[] otherbytes = new byte[] {4,5,6};
        doc2 = new DocumentImpl(uri1, otherbytes);
        Assertions.assertFalse(doc.equals(doc2));
        //same contents, but one as doc and one byte[]
        doc2 = new DocumentImpl(uri1, new String(b));
        Assertions.assertFalse(doc.equals(doc2));
    }

    @Test
    //tests that the wordCount method returns 0 when the doc is a binary doc
    public void testWordCountBinaryDoc(){
        byte[] bytes = new byte[] {7,8,9};
        DocumentImpl byteDoc = new DocumentImpl(uri1, bytes);
        Assertions.assertEquals(0, byteDoc.wordCount("word"));
    }

    @Test
    //wordCount returns 0 when there are no words or this word isn't there
    public void testWordCountNotThere(){
        doc = new DocumentImpl(uri1, "...");
        Assertions.assertEquals(0, doc.wordCount("word"));
        doc = new DocumentImpl(uri1, "these are real words");
        Assertions.assertEquals(0, doc.wordCount("notthere"));
    }

    @Test
    //wordCount returns the right count when word is there
    public void testWordCountThere(){
        doc = new DocumentImpl(uri1, "word word word once twice twice");
        Assertions.assertEquals(1, doc.wordCount("once"));
        Assertions.assertEquals(2, doc.wordCount("twice"));
        Assertions.assertEquals(3, doc.wordCount("word"));
    }

    @Test
    //tests that getWords returns an empty set when the doc was a binary doc
    public void testGetWordsBinaryDoc(){
        byte[] bytes = new byte[] {7,8,9};
        DocumentImpl byteDoc = new DocumentImpl(uri1, bytes);
        Assertions.assertEquals(new HashSet<String>(), byteDoc.getWords());
    }

    @Test
    //tests that getWords returns an empty set when the text doc has no words
    public void testGetWordsNoWords(){
        doc = new DocumentImpl(uri1, "..'!");
        Assertions.assertEquals(new HashSet<String>(), doc.getWords());
    }

    @Test
    //tests that getWords returns the right set of words
    public void testGetWordsNormal(){
        doc = new DocumentImpl(uri1, "one, two, three; words 'are' in the @set. one one ");
        HashSet<String> words = new HashSet<>();
        words.add("one");
        words.add("two");
        words.add("three");
        words.add("words");
        words.add("are");
        words.add("in");
        words.add("the");
        words.add("set");
        Assertions.assertEquals(words, doc.getWords());
    }
}