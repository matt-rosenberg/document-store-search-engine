package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.stage6.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentPersistenceManagerTest{

    @Test
    public void testSerializeAndDeserializeTextDocument() throws Exception {
        File baseDir = new File(System.getProperty("user.dir"));
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(baseDir);
        URI uri = URI.create("https://fakeurl1.com");
        Document doc = new DocumentImpl(uri, "hello hello hello, world world, this is text", null);
        dpm.serialize(uri, doc);
        Document result = dpm.deserialize(uri);
        assertEquals(doc, result);
        assertEquals("hello hello hello, world world, this is text", result.getDocumentTxt());
        assertEquals(uri, result.getKey());
    }

    @Test
    public void testSerializeAndDeserializeBinaryDocument() throws Exception {
        File baseDir = new File(System.getProperty("user.dir"));
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("https://fakeurl1.com");
        byte[] data = {1, 2, 3, 4};
        Document doc = new DocumentImpl(uri, data);
        dpm.serialize(uri, doc);
        Document result = dpm.deserialize(uri);
        assertEquals(doc, result);
        assertArrayEquals(data, result.getDocumentBinaryData());
        assertEquals(uri, result.getKey());
    }

    @Test
    public void testDeserializeDeletesFile() throws Exception {
        File baseDir = new File(System.getProperty("user.dir"));
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("https://fakeurl1.com");
        Document doc = new DocumentImpl(uri, "test", null);
        dpm.serialize(uri, doc);
        File file = new File(baseDir, "fakeurl1.com.json");
        assertTrue(file.exists());
        dpm.deserialize(uri);
        assertFalse(file.exists());
    }

    @Test
    public void testDeleteDocument() throws Exception {
        File baseDir = new File(System.getProperty("user.dir"));
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("https://fakeurl1.com");
        Document doc = new DocumentImpl(uri, "test", null);
        dpm.serialize(uri, doc);
        boolean deleted = dpm.delete(uri);
        assertTrue(deleted);
    }

    @Test
    public void testDeserializeNonexistentDocumentReturnsNull() throws Exception {
        File baseDir = new File(System.getProperty("user.dir"));
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("https://fakeurl1.com");
        Document result = dpm.deserialize(uri);
        assertNull(result);
    }

    @Test
    public void testMetadataSerialization() throws Exception {
        File baseDir = new File(System.getProperty("user.dir"));
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("https://fakeurl1.com");
        Document doc = new DocumentImpl(uri, "metadata test", null);
        doc.setMetadataValue("author", "matt");
        doc.setMetadataValue("genre", "test");
        dpm.serialize(uri, doc);
        Document result = dpm.deserialize(uri);
        assertEquals("matt", result.getMetadataValue("author"));
        assertEquals("test", result.getMetadataValue("genre"));
    }

    @Test
    public void testLongURIPath() throws Exception{
        File baseDir = new File(System.getProperty("user.dir"));
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(baseDir);
        URI uri = new URI("https://fakeurl1.com/long/path/here/");
        Document doc = new DocumentImpl(uri, "test", null);
        dpm.serialize(uri, doc);
        File file = new File(baseDir, "fakeurl1.com/long/path/here.json");
        assertTrue(file.exists());
    }

    @TempDir
    File tempDir;
    @Test
    public void testOtherBaseDirectory() throws Exception{
        DocumentPersistenceManager dpm = new DocumentPersistenceManager(tempDir);
        URI uri = new URI("https://fakeurl1.com/long/path/here/");
        Document doc = new DocumentImpl(uri, "test", null);
        dpm.serialize(uri, doc);
        File file = new File(tempDir, "fakeurl1.com/long/path/here.json");
        assertTrue(file.exists());
    }
}