package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage4.Document;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DocumentImpl implements Document{
    private URI myUri;
    private String myTxt;
    private byte[] myBytes;
    private HashTableImpl<String, String> myMetaData;
    private HashMap<String, Integer> allWords;

    /**
     * @param uri the uri which represents this document
     * @param txt the string contents of this document
     * @throws IllegalArgumentException if either argument is null or blank
     */
    public DocumentImpl(URI uri, String txt){
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri is null or blank");
        }
        if (txt == null || txt.isBlank()){
            throw new IllegalArgumentException("there are no contents here");
        }
        this.myUri = uri;
        this.myTxt = txt;
        this.myMetaData = new HashTableImpl<>();
        this.allWords = new HashMap<>();
        this.collectWords();
    }

    /**
     * @param uri the uri which represents this document
     * @param binaryData the contents of this document as raw bytes
     * @throws IllegalArgumentException if either argument is null or blank
     */
    public DocumentImpl(URI uri, byte[] binaryData){
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri is null or blank");
        }
        if (binaryData == null || binaryData.length == 0){
            throw new IllegalArgumentException("there's nothing here");
        }
        this.myUri = uri;
        this.myBytes = binaryData;
        this.myMetaData = new HashTableImpl<>();
    }

    /**
     * @param key key of document metadata to store a value for
     * @param value value to store
     * @return old value, or null if there was no old value
     * @throws IllegalArgumentException if the key is null or blank
     */
    @Override
    public String setMetadataValue(String key, String value) {
        if (key == null || key.isBlank()){
            throw new IllegalArgumentException("You didn't send a key");
        }
        return myMetaData.put(key, value);
    }

    /**
     * @param key metadata key whose value we want to retrieve
     * @return corresponding value, or null if there is no such key
     * @throws IllegalArgumentException if the key is null or blank
     */
    @Override
    public String getMetadataValue(String key) {
        if (key == null || key.isBlank()){
            throw new IllegalArgumentException("You didn't send a key");
        }
        return myMetaData.get(key);
    }

    /**
     * @return a COPY of the metadata saved in this document
     */
    @Override
    public HashTable<String, String> getMetadata() {
        HashTable<String, String> copy = new HashTableImpl<>();
        for (String key : myMetaData.keySet()){
            copy.put(key, myMetaData.get(key));
        }
        return copy;
    }

    /**
     * @return content of text document
     */
    @Override
    public String getDocumentTxt() {
        return this.myTxt;
    }

    /**
     * @return content of binary data document
     */
    @Override
    public byte[] getDocumentBinaryData() {
        return this.myBytes;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    @Override
    public URI getKey() {
        return this.myUri;
    }

    /**
     * reads through the text of this document and puts all the words and their word counts into the allWords hashmap
     */
    private void collectWords(){
        String s = this.getDocumentTxt();
        String word = "";
        for (int i = 0; i < s.length(); i++){
            char c = s.charAt(i);
            if (isValidChar(c)){
                word += c;
            } else if (c == ' '){
                if (!word.isEmpty()){
                    allWords.put(word, allWords.getOrDefault(word, 0) + 1);
                    word = "";
                }
            }
        }
        if (!word.isEmpty()){
            allWords.put(word, allWords.getOrDefault(word, 0) + 1);
        }
    }

    /** checks if the given character is a valid alpha-numeric character
     * @param c
     * @return true or false if its valid
     */
    private boolean isValidChar(char c){
        if (c >= 48 && c <= 57){
            return true;
        }
        if (c >= 65 && c <= 90){
            return true;
        }
        if (c >= 97 && c <= 122){
            return true;
        }
        return false;
    }

    /**
     * how many times does the given word appear in the document?
     * @param word
     * @return the number of times the given words appears in the document. If it's a binary document, return 0.
     */
    public int wordCount(String word){
        if (this.allWords == null){
            return 0;
        }
        return this.allWords.getOrDefault(word, 0);
    }

    /**
     * @return all the words that appear in the document
     */
    public Set<String> getWords(){
        if (this.allWords == null){
            return new HashSet<String>();
        }
        return this.allWords.keySet();
    }


    /**
     * @return the hashcode of the document, based on uri and contents
     */
    public int hashCode(){
        int result = this.myUri.hashCode();
        result = 31 * result + (this.myTxt != null ? myTxt.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(myBytes);
        return Math.abs(result);
    }

    /**
     * compares this document to another one
     * @param o   the reference object with which to compare.
     * @return true if they are both documents, have the same URI, and the same contents
     * @return false if this is not a document, or they have different URIs, or they have different contents
     */
    public boolean equals(Object o){
        if (this == o){
            return true;
        }
        if (o == null){
            return false;
        }
        if (!(o instanceof DocumentImpl)){
            return false;
        }
        DocumentImpl d = (DocumentImpl) o;
        if (!(this.myUri.equals(d.myUri))){
            return false;
        }
        if (this.myTxt == null && d.myTxt == null){ //they both have bytes
            return Arrays.equals(this.myBytes, d.myBytes);
        } else if (this.myBytes == null && d.myBytes == null){ //they both have text
            return this.myTxt.equals(d.myTxt);
        } else { //one has bytes and one has text
            return false;
        }
    }
}
