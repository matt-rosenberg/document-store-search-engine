package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.stage1.Document;

import java.net.URI;
import java.util.HashMap;
import java.util.Arrays;

public class DocumentImpl implements Document {
    private URI myUri;
    private String myTxt;
    private byte[] myBytes;
    private HashMap<String, String> myMetaData;

    public DocumentImpl(URI uri, String txt){
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri is null or blank");
        }
        if (txt == null || txt.isBlank()){
            throw new IllegalArgumentException("there are no contents here");
        }
        this.myUri = uri;
        this.myTxt = txt;
        this.myMetaData = new HashMap<>();
    }

    public DocumentImpl(URI uri, byte[] binaryData){
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("uri is null or blank");
        }
        if (binaryData == null || binaryData.length == 0){
            throw new IllegalArgumentException("there's nothing here");
        }
        this.myUri = uri;
        this.myBytes = binaryData;
        this.myMetaData = new HashMap<>();
    }
    /**
     * @param key   key of document metadata to store a value for
     * @param value value to store
     * @return old value, or null if there was no old value
     * @throws IllegalArgumentException if the key is null or blank
     */
    public String setMetadataValue(String key, String value){
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
    public String getMetadataValue(String key){
        if (key == null || key.isBlank()){
            throw new IllegalArgumentException("You didn't send a key");
        }
        return myMetaData.get(key);
    }

    /**
     * @return a COPY of the metadata saved in this document
     */
    public HashMap<String, String> getMetadata(){
        return new HashMap<>(myMetaData);
    }

    /**
     * @return content of text document
     */
    public String getDocumentTxt(){
        return this.myTxt;
    }

    /**
     * @return content of binary data document
     */
    public byte[] getDocumentBinaryData(){
        return this.myBytes;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    public URI getKey(){
        return this.myUri;
    }

    public int hashCode(){
        int result = this.myUri.hashCode();
        result = 31 * result + (this.myTxt != null ? myTxt.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(myBytes);
        return Math.abs(result);
    }

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