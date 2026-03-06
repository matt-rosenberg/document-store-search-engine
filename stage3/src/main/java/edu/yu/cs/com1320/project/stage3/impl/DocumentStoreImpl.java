package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import edu.yu.cs.com1320.project.undo.Command;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, DocumentImpl> myDocs;
    private StackImpl<Command> allCommands;

    public DocumentStoreImpl(){
        this.myDocs = new HashTableImpl<>();
        this.allCommands = new StackImpl<>();
    }

    /**
     * set the given key-value metadata pair for the document at the given uri
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
        String s = myDocs.get(uri).setMetadataValue(key, value);
        Command com = new Command(uri, u -> {
            DocumentImpl doc = myDocs.get(u);
            doc.setMetadataValue(key, s);
        });
        allCommands.push(com);
        return s;
    }

    /**
     * get the value corresponding to the given metadata key for the document at the given uri
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
     * @param input the document being put
     * @param url unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if url is null or empty, or format is null
     */
    public int put(InputStream input, URI url, DocumentStore.DocumentFormat format) throws IOException{
        if (url == null || url.toString().isBlank()){
            throw new IllegalArgumentException("There's no uri.");
        }
        if (format == null){
            throw new IllegalArgumentException("What type of document is this?");
        }
        int previous = putPrep(url);
        putContents(input, url, format);
        return previous;
    }

    /**
     * prepares the previous hashcode, and creates the undo commands
     * @param url the identifier for the document we are replacing
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc.
     */
    private int putPrep(URI url){
        int previous = 0;
        if (myDocs.get(url) != null){
            previous = myDocs.get(url).hashCode();
            DocumentImpl doc = myDocs.get(url);
            Command com = new Command(url, u -> myDocs.put(u, doc));
            allCommands.push(com);
        } else {
            Command com = new Command(url, u -> myDocs.put(u, null));
            allCommands.push(com);
        }
        return previous;
    }
    /**
     * private method to do the actual placing of contents into the hashtable
     * @param input Document to be put
     * @param url identifier for the document
     * @param format which type of doc is being passed
     * @throws IOException if there's an issue reading input
     */
    private void putContents(InputStream input, URI url, DocumentStore.DocumentFormat format) throws IOException {
        if (input == null){
            myDocs.put(url, null);
        } else {
            byte[] contents = input.readAllBytes();
            if (format == DocumentFormat.TXT) {
                String words = new String(contents);
                DocumentImpl wordDoc = new DocumentImpl(url, words);
                myDocs.put(url, wordDoc);
            } else {
                DocumentImpl byteDoc = new DocumentImpl(url, contents);
                myDocs.put(url, byteDoc);
            }
        }
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

    //**********STAGE 3 ADDITIONS

    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    public void undo() throws IllegalStateException{
        if (allCommands.size() == 0){
            throw new IllegalStateException("there are no commands to undo");
        }
        allCommands.pop().undo();
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param url
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    public void undo(URI url) throws IllegalStateException{
        if (allCommands.size() == 0){
            throw new IllegalStateException("there are no commands to undo");
        }
        StackImpl<Command> temp = new StackImpl<>();
        boolean notFound = true;
        while (allCommands.size() > 0 && notFound){
            Command com = allCommands.pop();
            if (com.getUri().equals(url)){
                com.undo();
                notFound = false;
            } else {
                temp.push(com);
            }
        }
        while (temp.size() > 0){
            allCommands.push(temp.pop());
        }
        if (notFound){
            throw new IllegalStateException("there are no commands to undo for this uri");
        }
    }
}