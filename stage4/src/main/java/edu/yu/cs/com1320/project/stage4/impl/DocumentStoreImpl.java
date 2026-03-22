package edu.yu.cs.com1320.project.stage4.impl;

import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, DocumentImpl> myDocs;
    private StackImpl<Undoable> allCommands;
    private TrieImpl<Document> docSearch;
    private TrieImpl<Document> metadataDocSearch;

    public DocumentStoreImpl(){
        this.myDocs = new HashTableImpl<>();
        this.allCommands = new StackImpl<>();
        this.docSearch = new TrieImpl<>();
        this.metadataDocSearch = new TrieImpl<>();
    }

    @Override
    /**
     * set the given key-value metadata pair for the document at the given uri
     * @param uri
     * @param key
     * @param value
     * @return the old value, or null if there was no previous value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */
    public String setMetadata(URI uri, String key, String value) {
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("There's no uri");
        }
        if (myDocs.get(uri) == null){
            throw new IllegalArgumentException("There's no such document here");
        }
        String s = myDocs.get(uri).setMetadataValue(key, value); //put the new value in the hashtable
        if (s != null){ //if there was a previous value, delete it from the trie
            metadataDocSearch.delete(key + s, myDocs.get(uri));
        }
        if (value != null){ //if I'm putting a new value, add it to the trie. If I was sent null, don't add it to the trie, consistent with it not being in the hashtable
            metadataDocSearch.put(key + value, myDocs.get(uri));
        }
        GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
            DocumentImpl doc = myDocs.get(u);
            doc.setMetadataValue(key, s); //put the old value back in the table
            if (s != null){ //put the old value back in the trie
                metadataDocSearch.put(key + s, doc);
            }
            if (value != null){ //delete the new value from the trie
                metadataDocSearch.delete(key + value, doc);
            }
        });
        allCommands.push(com);
        return s;
    }

    @Override
    /**
     * get the value corresponding to the given metadata key for the document at the given uri
     * @param uri
     * @param key
     * @return the value, or null if there was no value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */
    public String getMetadata(URI uri, String key) {
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("There's no such uri");
        }
        if (myDocs.get(uri) == null){
            throw new IllegalArgumentException("There's no Document here");
        }
        return myDocs.get(uri).getMetadataValue(key);
    }

    @Override
    /**
     * @param input the document being put
     * @param url unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if url is null or empty, or format is null
     */
    public int put(InputStream input, URI url, DocumentFormat format) throws IOException {
        this.illegalPuts(url, format);
        DocumentImpl previousDoc = myDocs.get(url);
        int previous = 0;
        if (previousDoc != null){
            previous = previousDoc.hashCode();
        }
        if (input == null){
            delete(url);
            return previous;
        }
        removePreviousDoc(previousDoc);
        DocumentImpl newDoc;
        newDoc = this.createNewDoc(input, url, format);
        putNewDoc(url, newDoc);
        GenericCommand<URI> com = new GenericCommand<>(url, u -> {
            undoPut(previousDoc, newDoc);
        });
        allCommands.push(com);
        return previous;
    }

    /**
     * deals with the illegal inputs to the public put method
     * @param url the url sent to put
     * @param format the DocumentFormat sent to put
     * @throws IllegalArgumentException if the url is null or blank, if the format is null
     */
    private void illegalPuts(URI url, DocumentFormat format){
        if (url == null || url.toString().isBlank()){
            throw new IllegalArgumentException("There's no uri.");
        }
        if (format == null){
            throw new IllegalArgumentException("What type of document is this?");
        }
    }

    /**
     * Removes the document we are overwriting from the trie for its words and its metadata
     * @param previous
     */
    private void removePreviousDoc(DocumentImpl previous){
        if (previous != null){
            for (String word : previous.getWords()){
                this.docSearch.delete(word, previous);
            }
            HashTableImpl<String, String> metadata = (HashTableImpl) previous.getMetadata();
            for (String key : metadata.keySet()){
                String pair = key + metadata.get(key);
                this.metadataDocSearch.delete(pair, previous);
            }
        }
    }

    /**
     * takes the arguments from the put method and creates the DocumentImpl with the correct constructor
     * @param input
     * @param url
     * @param format
     * @return the DocumentImpl
     * @throws IOException
     */
    private DocumentImpl createNewDoc(InputStream input, URI url, DocumentFormat format) throws IOException {
        byte[] contents = input.readAllBytes();
        DocumentImpl doc;
        if (format == DocumentFormat.TXT) {
            String words = new String(contents);
            doc = new DocumentImpl(url, words);
        } else {
            doc = new DocumentImpl(url, contents);
        }
        return doc;
    }

    /**
     * puts the new document into the hashtable
     * puts the new document into the trie for all of its words
     * @param url
     * @param doc
     */
    private void putNewDoc(URI url, DocumentImpl doc){
        this.myDocs.put(url, doc);
        for (String word : doc.getWords()){
            this.docSearch.put(word, doc);
        }
    }

    /**
     * Implements the undo logic to go within the GenericCommand for this put
     * if there was a document which was overwritten, puts it back into the hashtable and the trie
     * takes the document we just put in out of the trie and hashtable
     * @param prevDoc
     * @param newDoc
     */
    private void undoPut(DocumentImpl prevDoc, DocumentImpl newDoc){
        URI url = newDoc.getKey();
        if (prevDoc != null){
            for (String word : prevDoc.getWords()){
                this.docSearch.put(word, prevDoc);
            }
            HashTableImpl<String, String> metadata = (HashTableImpl) prevDoc.getMetadata();
            for (String key : metadata.keySet()){
                String pair = key + metadata.get(key);
                this.metadataDocSearch.put(pair, prevDoc);
            }
        }
        this.myDocs.put(url, prevDoc);
        for (String word : newDoc.getWords()){
            this.docSearch.delete(word, newDoc);
        }
        HashTableImpl<String, String> metadata = (HashTableImpl) newDoc.getMetadata();
        for (String key : metadata.keySet()){
            String pair = key + metadata.get(key);
            this.metadataDocSearch.delete(pair, newDoc);
        }
    }

    @Override
    /**
     * @param url the unique identifier of the document to get
     * @return the given document
     */
    public Document get(URI url) {
        return myDocs.get(url);
    }

    @Override
    /**
     * @param url the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    public boolean delete(URI url) {
        if (url == null || url.toString().isBlank()){
            return false;
        }
        Document doc = this.get(url);
        if (doc != null){
            for (String word : doc.getWords()){
                this.docSearch.delete(word, doc);
            }
            HashTableImpl<String, String> metadata = (HashTableImpl) doc.getMetadata();
            for (String key : metadata.keySet()){
                String pair = key + metadata.get(key);
                this.metadataDocSearch.delete(pair, doc);
            }
            GenericCommand<URI> com = new GenericCommand<>(url, u -> {
                undoDelete(doc);
            });
            allCommands.push(com);
        }
        return (myDocs.put(url, null) != null);
    }

    /**
     * the undo logic to create the GenericCommand for the delete method
     * puts the document back in the hashtbale and the trie
     * @param doc the document we want to put back
     */
    private void undoDelete(Document doc){
        URI url = doc.getKey();
        this.myDocs.put(url, (DocumentImpl) doc);
        for (String word : doc.getWords()){
            this.docSearch.put(word, doc);
        }
        HashTableImpl<String, String> metadata = (HashTableImpl) doc.getMetadata();
        for (String key : metadata.keySet()){
            String pair = key + metadata.get(key);
            this.metadataDocSearch.put(pair, doc);
        }
    }

    @Override
    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    public void undo() throws IllegalStateException {
        if (allCommands.size() == 0){
            throw new IllegalStateException("there are no commands to undo");
        }
        allCommands.pop().undo();
    }

    @Override
    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param url
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    public void undo(URI url) throws IllegalStateException {
        if (allCommands.size() == 0){
            throw new IllegalStateException("there are no commands to undo");
        }
        StackImpl<Undoable> temp = new StackImpl<>();
        boolean notFound = true;
        while (allCommands.size() > 0 && notFound){
            Undoable com = allCommands.pop();
            notFound = undoCommandWithUrl(url, com, temp);
        }
        while (temp.size() > 0){
            allCommands.push(temp.pop());
        }
        if (notFound){
            throw new IllegalStateException("there are no commands to undo for this uri");
        }
    }

    /**
     * checks if the given Undoable has the given url. If it does, undoes the command. otherwise puts the command onto the temp stack to be pushed back later.
     * @param url
     * @param u
     * @param temp
     * @return true if the Undoable was undone, false if it wasn't.
     */
    private boolean undoCommandWithUrl(URI url, Undoable u, StackImpl<Undoable> temp){
        boolean notFound = true;
        if (u instanceof GenericCommand<?>){
            GenericCommand<URI> c = (GenericCommand<URI>) u;
            if (c.getTarget().equals(url)){
                u.undo();
                notFound = false;
            } else {
                temp.push(u);
            }
        } else { //u is a commandSet
            CommandSet<URI> coms = (CommandSet<URI>) u;
            if (coms.undo(url)){
                notFound = false;
                if(coms.size() != 0){
                    allCommands.push(u);
                }
            } else {
                temp.push(u);
            }
        }
        return notFound;
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
     * removes any non-alpha-numeric characters from the keyword
     * @param rawKeyword
     * @return the keyword with just its alpha-numerics
     */
    private String stripIllegalCharacters(String rawKeyword){
        String key = "";
        for (int i = 0; i < rawKeyword.length(); i++){
            if (isValidChar(rawKeyword.charAt(i))){
                key += rawKeyword.charAt(i);
            }
        }
        return key;
    }
    @Override
    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> search(String keyword) {
        String key = stripIllegalCharacters(keyword);
        Comparator<Document> comparator = new Comparator<Document>() {
            @Override
            public int compare(Document doc1, Document doc2) {
                return doc2.wordCount(key) - doc1.wordCount(key);
            }
        };
        return docSearch.getSorted(keyword, comparator);
    }

    @Override
    /**
     * Retrieve all documents that contain text which starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> searchByPrefix(String keywordPrefix) {
        String prefix = stripIllegalCharacters(keywordPrefix);
        Comparator<Document> comparator = new Comparator<Document>() {
            @Override
            public int compare(Document doc1, Document doc2) {
                int firstCount = prefixCount(doc1, prefix);
                int secondCount = prefixCount(doc2, prefix);
                return secondCount - firstCount;
            }
        };
        return docSearch.getAllWithPrefixSorted(prefix, comparator);
    }


    /**
     * goes through all the words in the document and counts how many words start with the given prefix
     * @param doc
     * @param prefix
     * @return the amount of times the prefix is in the document
     */
    private int prefixCount(Document doc, String prefix){
        int count = 0;
        for (String word : doc.getWords()){
            if (word.startsWith(prefix)){
                count += doc.wordCount(word);
            }
        }
        return count;
    }

    @Override
    /**
     * Completely remove any trace of any document which contains the given keyword
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAll(String keyword) {
        Set<Document> docsWithKeyword = this.docSearch.deleteAll(keyword);
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        for (Document doc : docsWithKeyword){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                undoDelete(doc);
            });
            commands.addCommand(com);
        }
        if (!commands.isEmpty()){
            this.allCommands.push(commands);
        }
        return deletedURIs;
    }

    /**
     * removes the given document from the hashtable and the tries
     * @param uri
     * @param doc
     */
    private void deleteDoc(URI uri, Document doc){
        for (String word : doc.getWords()){
            this.docSearch.delete(word, doc);
        }
        HashTableImpl<String, String> metadata = (HashTableImpl) doc.getMetadata();
        for (String key : metadata.keySet()){
            String pair = key + metadata.get(key);
            this.metadataDocSearch.delete(pair, doc);
        }
        this.myDocs.put(uri, null);
    }

    @Override
    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<Document> docsWithPrefix = docSearch.deleteAllWithPrefix(keywordPrefix);
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        for (Document doc : docsWithPrefix){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                undoDelete(doc);
            });
            commands.addCommand(com);
        }
        if (!commands.isEmpty()){
            allCommands.push(commands);
        }
        return deletedURIs;
    }

    @Override
    /**
     * @param keysValues metadata key-value pairs to search for
     * @return a List of all documents whose metadata contains ALL OF the given values for the given keys. If no documents contain all the given key-value pairs, return an empty list.
     */
    public List<Document> searchByMetadata(Map<String, String> keysValues) {
        List<Set<Document>> sets = new ArrayList<>();
        for (String key : keysValues.keySet()){
            String keyValuePair = key + keysValues.get(key);
            Set<Document> docsForThisPair = metadataDocSearch.get(keyValuePair);
            if (docsForThisPair.isEmpty()){
                return new ArrayList<Document>();
            }
            sets.add(docsForThisPair);
        }
        sets.sort(Comparator.comparingInt(Set::size));
        Set<Document> smallestSet = sets.get(0);
        for (Set<Document> s : sets){
            if (s == sets.get(0)){continue;}
            smallestSet.retainAll(s);
            if (smallestSet.isEmpty()){break;}
        }
        return new ArrayList<Document>(smallestSet);
    }

    @Override
    /**
     * Retrieve all documents whose text contains the given keyword AND which has the given key-value pairs in its metadata
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE SENSITIVE.
     * @param keyword
     * @param keysValues
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        List<Document> docsByKeyword = this.search(keyword);
        List<Document> docsByMetadata = this.searchByMetadata(keysValues);
        Set<Document> docsByMetadataSet = new HashSet<>(docsByMetadata);

        List<Document> docsByKeywordAndMetadata = new ArrayList<>();
        for (Document doc : docsByKeyword){
            if (docsByMetadataSet.contains(doc)){
                docsByKeywordAndMetadata.add(doc);
            }
        }
        return docsByKeywordAndMetadata;
    }

    @Override
    /**
     * Retrieve all documents that contain text which starts with the given prefix AND which has the given key-value pairs in its metadata
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        List<Document> docsByPrefix = this.searchByPrefix(keywordPrefix);
        List<Document> docsByMetadata = this.searchByMetadata(keysValues);
        Set<Document> docsByMetadataSet = new HashSet<>(docsByMetadata);

        List<Document> docsByPrefixAndMetadata = new ArrayList<>();
        for (Document doc : docsByPrefix){
            if (docsByMetadataSet.contains(doc)){
                docsByPrefixAndMetadata.add(doc);
            }
        }
        return docsByPrefixAndMetadata;
    }

    @Override
    /**
     * Completely remove any trace of any document which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithMetadata(Map<String, String> keysValues) {
        Set<Document> docsWithMetadata = new HashSet<>(this.searchByMetadata(keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        for (Document doc : docsWithMetadata){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                undoDelete(doc);
            });
            commands.addCommand(com);
        }
        if (!commands.isEmpty()){
            allCommands.push(commands);
        }
        return deletedURIs;
    }

    @Override
    /**
     * Completely remove any trace of any document which contains the given keyword AND which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword, Map<String, String> keysValues) {
        Set<Document> docsWithKeywordAndMetadata = new HashSet<>(this.searchByKeywordAndMetadata(keyword, keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        for (Document doc : docsWithKeywordAndMetadata){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                undoDelete(doc);
            });
            commands.addCommand(com);
        }
        if (!commands.isEmpty()){
            allCommands.push(commands);
        }
        return deletedURIs;
    }

    @Override
    /**
     * Completely remove any trace of any document which contains a word that has the given prefix AND which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String, String> keysValues) {
        Set<Document> docsWithPrefixAndMetadata = new HashSet<>(this.searchByPrefixAndMetadata(keywordPrefix, keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        for (Document doc : docsWithPrefixAndMetadata){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                undoDelete(doc);
            });
            commands.addCommand(com);
        }
        if (!commands.isEmpty()){
            allCommands.push(commands);
        }
        return deletedURIs;
    }
}
