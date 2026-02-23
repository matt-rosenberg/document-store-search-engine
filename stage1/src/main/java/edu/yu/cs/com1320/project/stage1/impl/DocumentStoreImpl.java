package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;

public class DocumentStoreImpl implements DocumentStore {
    private HashMap<URI, DocumentImpl> myDocs;

    public DocumentStoreImpl(){
        this.myDocs = new HashMap<>();
    }

    /**
     * set the given key-value metadata pair for the document at the given uri
     *
     * @param uri
     * @param key
     * @param value
     * @return the old value, or null if there was no previous value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */
    public String setMetadata(URI uri, String key, String value){
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("There's no uri");
        }
        if (myDocs.get(uri) == null){
            throw new IllegalArgumentException("There's no such document here");
        }
        return myDocs.get(uri).setMetadataValue(key, value);
    }

    /**
     * get the value corresponding to the given metadata key for the document at the given uri
     *
     * @param uri
     * @param key
     * @return the value, or null if there was no value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */
    public String getMetadata(URI uri, String key){
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("There's no such uri");
        }
        if (myDocs.get(uri) == null){
            throw new IllegalArgumentException("There's no Document here");
        }
        return myDocs.get(uri).getMetadataValue(key);
    }

    /**
     * @param input  the document being put
     * @param uri    unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException              if there is an issue reading input
     * @throws IllegalArgumentException if uri is null or empty, or format is null
     */
    public int put(InputStream input, URI uri, DocumentFormat format) throws IOException{
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("There's no uri.");
        }
        if (format == null){
            throw new IllegalArgumentException("What type of document is this?");
        }
        int previous = 0;
        if (myDocs.get(uri) != null){
            previous = myDocs.get(uri).hashCode();
        }
        if (input == null){
            myDocs.put(uri, null);
        } else {
            byte[] contents = input.readAllBytes();
            if (format == DocumentFormat.TXT) {
                String words = new String(contents);
                DocumentImpl wordDoc = new DocumentImpl(uri, words);
                myDocs.put(uri, wordDoc);
            } else {
                DocumentImpl byteDoc = new DocumentImpl(uri, contents);
                myDocs.put(uri, byteDoc);
            }
        }
        return previous;
    }

    /**
     * @param url the unique identifier of the document to get
     * @return the given document
     */
    public Document get(URI url){
        return myDocs.get(url);
    }

    /**
     * @param url the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    public boolean delete(URI url){
        if (url == null || url.toString().isBlank()){
            return false;
        }
        return (myDocs.put(url, null) != null);
    }
}
