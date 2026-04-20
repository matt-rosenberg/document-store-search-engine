package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
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
    private int docCountLimit = Integer.MAX_VALUE;
    private int docCount = 0;
    private int docByteLimit = Integer.MAX_VALUE;
    private int docBytes = 0;
    private MinHeapImpl<Document> heap;


    public DocumentStoreImpl(){
        this.myDocs = new HashTableImpl<>();
        this.allCommands = new StackImpl<>();
        this.docSearch = new TrieImpl<>();
        this.metadataDocSearch = new TrieImpl<>();
        this.heap = new MinHeapImpl<>();
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
        this.updateHeap(myDocs.get(uri)); //update the heap
        if (s != null){ //if there was a previous value, delete it from the trie
            metadataDocSearch.delete(key + s, myDocs.get(uri));
        }
        if (value != null){ //if I'm putting a new value, add it to the trie. If I was sent null, don't add it to the trie, consistent with it not being in the hashtable
            metadataDocSearch.put(key + value, myDocs.get(uri));
        }
        GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
            DocumentImpl doc = myDocs.get(u);
            this.updateHeap(doc);
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
        this.updateHeap(myDocs.get(uri));
        return myDocs.get(uri).getMetadataValue(key);
    }

    @Override
    /**
     * @param input the document being put
     * @param url unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if url is null or empty, or format is null, OR IF THE MEMORY FOOTPRINT OF THE DOCUMENT IS > MAX DOCUMENT BYTES
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
        DocumentImpl newDoc;
        newDoc = this.createNewDoc(input, url, format);
        removePreviousDoc(previousDoc);
        putDocInHeap(newDoc);
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
     * removes the document we are overwriting from the memory counts
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
            decreaseDocFromMemoryCounts(previous);
            removeDocFromHeap(previous);
        }
    }

    /**
     * takes the arguments from the put method and creates the DocumentImpl with the correct constructor
     * @param input
     * @param url
     * @param format
     * @return the DocumentImpl
     * @throws IOException
     * @throws IllegalArgumentException IF THE MEMORY FOOTPRINT OF THE DOCUMENT IS > MAX DOCUMENT BYTES
     */
    private DocumentImpl createNewDoc(InputStream input, URI url, DocumentFormat format) throws IOException {
        byte[] contents = input.readAllBytes();
        if (contents.length > docByteLimit){
            throw new IllegalArgumentException("you can't put a document which is bigger than the memory limit");
        }
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
     * clears space in the store to put the new document
     * puts the new doc into the heap
     * updates the memory counts
     * @param doc the document to insert
     */
    private void putDocInHeap(Document doc){
        while ((docCount + 1) > docCountLimit){
            remove();
        }
        int bytesNeeded = getDocBytes(doc);
        while ((docBytes + bytesNeeded) > docByteLimit){
            remove();
        }
        doc.setLastUseTime(System.nanoTime());
        this.heap.insert(doc);
        this.docCount++;
        this.docBytes += bytesNeeded;
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
     * takes the document we just put in out of the trie and hashtable and heap
     * puts the overwritten doc back into the heap
     * @param prevDoc
     * @param newDoc
     */
    private void undoPut(DocumentImpl prevDoc, DocumentImpl newDoc){
        for (String word : newDoc.getWords()){
            this.docSearch.delete(word, newDoc);
        }
        HashTableImpl<String, String> newMetadata = (HashTableImpl) newDoc.getMetadata();
        for (String key : newMetadata.keySet()){
            String pair = key + newMetadata.get(key);
            this.metadataDocSearch.delete(pair, newDoc);
        }
        decreaseDocFromMemoryCounts(newDoc);
        removeDocFromHeap(newDoc);
        if (prevDoc != null){
            for (String word : prevDoc.getWords()){
                this.docSearch.put(word, prevDoc);
            }
            HashTableImpl<String, String> prevMetadata = (HashTableImpl) prevDoc.getMetadata();
            for (String key : prevMetadata.keySet()){
                String pair = key + prevMetadata.get(key);
                this.metadataDocSearch.put(pair, prevDoc);
            }
            putDocInHeap(prevDoc);
        }
        URI url = newDoc.getKey();
        this.myDocs.put(url, prevDoc);
    }

    @Override
    /**
     * @param url the unique identifier of the document to get
     * @return the given document
     */
    public Document get(URI url) {
        Document doc = myDocs.get(url);
        if (doc != null){
            updateHeap(doc);
        }
        return doc;
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
            decreaseDocFromMemoryCounts(doc);
            removeDocFromHeap(doc);
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
     * puts the document back into the heap
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
        putDocInHeap(doc);
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
        List<Document> docs = searchWithoutUpdatingTime(keyword);
        long time = System.nanoTime();
        for (Document doc : docs){
            doc.setLastUseTime(time);
            heap.reHeapify(doc);
        }
        return docs;
    }

    /**
     * does the search by keyword logic without updating the time of these documents so that other methods can utilize this logic
     * @param keyword
     * @return the documents with this keyword in sorted order
     */
    private List<Document> searchWithoutUpdatingTime(String keyword){
        String key = stripIllegalCharacters(keyword);
        Comparator<Document> comparator = new Comparator<Document>() {
            @Override
            public int compare(Document doc1, Document doc2) {
                return doc2.wordCount(key) - doc1.wordCount(key);
            }
        };
        return docSearch.getSorted(key, comparator);
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
        List<Document> docs = searchByPrefixWithoutUpdatingTime(keywordPrefix);
        long time = System.nanoTime();
        for (Document doc : docs){
            doc.setLastUseTime(time);
            heap.reHeapify(doc);
        }
        return docs;
    }

    /**
     * same as the searchByPrefix method but without updating time so that other methods can use this logic
     * @param keywordPrefix
     * @return the docs with this prefix in sorted order
     */
    private List<Document> searchByPrefixWithoutUpdatingTime(String keywordPrefix){
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
        String key = stripIllegalCharacters(keyword);
        Set<Document> docsWithKeyword = this.docSearch.deleteAll(key);
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithKeyword){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            removeDocFromHeap(doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                if (time[0] == 0){
                    time[0] = System.nanoTime();
                }
                undoDeleteWithSetTime(doc, time[0]);
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
     * takes the memory for this doc out of the memory counts
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
        decreaseDocFromMemoryCounts(doc);
    }

    /**
     * it is the same logic as undoDelete, with the added property that you can send the time
     * the purpose is so an undo of a method that deletes several docs gives them all the same time
     * @param doc
     * @param time
     */
    private void undoDeleteWithSetTime(Document doc, long time){
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
        putDocInHeapWithSetTime(doc, time);
    }

    /**
     * the same thing as putDocInHeap but with a specific time
     * @param doc
     * @param time
     */
    private void putDocInHeapWithSetTime(Document doc, long time){
        while ((docCount + 1) > docCountLimit){
            remove();
        }
        int bytesNeeded = getDocBytes(doc);
        while ((docBytes + bytesNeeded) > docByteLimit){
            remove();
        }
        doc.setLastUseTime(time);
        this.heap.insert(doc);
        this.docCount++;
        this.docBytes += bytesNeeded;
    }

    @Override
    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        String keypre = stripIllegalCharacters(keywordPrefix);
        Set<Document> docsWithPrefix = docSearch.deleteAllWithPrefix(keypre);
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithPrefix){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            removeDocFromHeap(doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                if (time[0] == 0){
                    time[0] = System.nanoTime();
                }
                undoDeleteWithSetTime(doc, time[0]);
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
        List<Document> docs = searchByMetadataWithoutUpdatingTime(keysValues);
        long time = System.nanoTime();
        for (Document doc : docs){
            doc.setLastUseTime(time);
            heap.reHeapify(doc);
        }
        return docs;
    }

    /**
     * same as searchByMetadata but doesn't change the time so other methods can use this logic
     * @param keysValues
     * @return
     */
    private List<Document> searchByMetadataWithoutUpdatingTime(Map<String, String> keysValues){
        if (keysValues == null || keysValues.isEmpty()){
            return new ArrayList<Document>();
        }
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
        String key = stripIllegalCharacters(keyword);
        List<Document> docsByKeyword = this.searchWithoutUpdatingTime(key);
        List<Document> docsByMetadata = this.searchByMetadataWithoutUpdatingTime(keysValues);
        Set<Document> docsByMetadataSet = new HashSet<>(docsByMetadata);

        List<Document> docsByKeywordAndMetadata = new ArrayList<>();
        for (Document doc : docsByKeyword){
            if (docsByMetadataSet.contains(doc)){
                docsByKeywordAndMetadata.add(doc);
            }
        }
        long time = System.nanoTime();
        for (Document doc : docsByKeywordAndMetadata){
            doc.setLastUseTime(time);
            heap.reHeapify(doc);
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
        String keypre = stripIllegalCharacters(keywordPrefix);
        List<Document> docsByPrefix = this.searchByPrefixWithoutUpdatingTime(keypre);
        List<Document> docsByMetadata = this.searchByMetadataWithoutUpdatingTime(keysValues);
        Set<Document> docsByMetadataSet = new HashSet<>(docsByMetadata);

        List<Document> docsByPrefixAndMetadata = new ArrayList<>();
        for (Document doc : docsByPrefix){
            if (docsByMetadataSet.contains(doc)){
                docsByPrefixAndMetadata.add(doc);
            }
        }
        long time = System.nanoTime();
        for (Document doc : docsByPrefixAndMetadata){
            doc.setLastUseTime(time);
            heap.reHeapify(doc);
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
        Set<Document> docsWithMetadata = new HashSet<>(this.searchByMetadataWithoutUpdatingTime(keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithMetadata){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            removeDocFromHeap(doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                if (time[0] == 0){
                    time[0] = System.nanoTime();
                }
                undoDeleteWithSetTime(doc, time[0]);
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
        String key = stripIllegalCharacters(keyword);
        Set<Document> docsWithKeywordAndMetadata = new HashSet<>(this.searchByKeywordAndMetadata(key, keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithKeywordAndMetadata){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            removeDocFromHeap(doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                if (time[0] == 0){
                    time[0] = System.nanoTime();
                }
                undoDeleteWithSetTime(doc, time[0]);
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
        String keypre = stripIllegalCharacters(keywordPrefix);
        Set<Document> docsWithPrefixAndMetadata = new HashSet<>(this.searchByPrefixAndMetadata(keypre, keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithPrefixAndMetadata){
            URI uri = doc.getKey();
            deleteDoc(uri, doc);
            removeDocFromHeap(doc);
            deletedURIs.add(uri);
            GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
                if (time[0] == 0){
                    time[0] = System.nanoTime();
                }
                undoDeleteWithSetTime(doc, time[0]);
            });
            commands.addCommand(com);
        }
        if (!commands.isEmpty()){
            allCommands.push(commands);
        }
        return deletedURIs;
    }

    //**********STAGE 5 ADDITIONS

    /**
     * set maximum number of documents that may be stored
     * @param limit
     * @throws IllegalArgumentException if limit < 1
     */
    public void setMaxDocumentCount(int limit){
        if (limit < 1){
            throw new IllegalArgumentException("you can't set a limit of 0 documents");
        }
        this.docCountLimit = limit;
        while (docCount > docCountLimit){
            this.remove();
        }
    }

    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     * @param limit
     * @throws IllegalArgumentException if limit < 1
     */
    public void setMaxDocumentBytes(int limit){
        if (limit < 1){
            throw new IllegalArgumentException("you can't set a limit of 0 bytes");
        }
        this.docByteLimit = limit;
        while (docBytes > docByteLimit){
            remove();
        }
    }

    /**
     * returns the size of the document in bytes
     * @return the size of the byte[], or of the byte[] returned by getBytes()
     */
    private int getDocBytes(Document doc){
        if (doc.getDocumentTxt() != null){
            return doc.getDocumentTxt().getBytes().length;
        }
        return doc.getDocumentBinaryData().length;
    }

    private void updateHeap(Document doc){
        doc.setLastUseTime(System.nanoTime());
        heap.reHeapify(doc);
    }

    /**
     * removes the least recently used document from all references in memory
     */
    private void remove(){
        Document removed = this.heap.remove();
        deleteDoc(removed.getKey(), removed);
        removeUndoCommands(removed);
    }

    /**
     * removes the given document from the heap by moving it to the top
     * @param doc
     */
    private void removeDocFromHeap(Document doc){
        doc.setLastUseTime(Long.MIN_VALUE);
        heap.reHeapify(doc);
        heap.remove();
    }

    /**
     * decrements the doc from the count and decreases the docBytes
     * @param doc
     */
    private void decreaseDocFromMemoryCounts(Document doc){
        docCount--;
        docBytes -= getDocBytes(doc);
    }

    /**
     * remove all undo commands for the document
     * @param doc the document we just removed from the heap and are erasing from memory
     */
    private void removeUndoCommands(Document doc){
        StackImpl<Undoable> temp = new StackImpl<>();
        while (allCommands.size() > 0) {
            Undoable command = allCommands.pop();
            if (command instanceof GenericCommand<?>){
                GenericCommand<URI> c = (GenericCommand<URI>) command;
                if (!c.getTarget().equals(doc.getKey())){
                    temp.push(c);
                }
            } else {
                CommandSet<URI> set = (CommandSet<URI>) command;
                Iterator<GenericCommand<URI>> it = set.iterator();
                while (it.hasNext()){
                    GenericCommand<URI> c = it.next();
                    if (c.getTarget().equals(doc.getKey())){
                        it.remove();
                    }
                }
                if (set.size() != 0){
                    temp.push(set);
                }
            }
        }
        while (temp.size() > 0){
            allCommands.push(temp.pop());
        }
    }
}