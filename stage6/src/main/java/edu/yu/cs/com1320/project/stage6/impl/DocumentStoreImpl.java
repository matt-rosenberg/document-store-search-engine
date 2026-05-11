package edu.yu.cs.com1320.project.stage6.impl;

import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.DocumentStore;
import edu.yu.cs.com1320.project.undo.CommandSet;
import edu.yu.cs.com1320.project.undo.GenericCommand;
import edu.yu.cs.com1320.project.undo.Undoable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

public class DocumentStoreImpl implements DocumentStore {
    private BTreeImpl<URI, Document> myDocs;
    private StackImpl<Undoable> allCommands;
    private TrieImpl<URI> docSearch;
    private TrieImpl<URI> metadataDocSearch;
    private int docCountLimit = Integer.MAX_VALUE;
    private int docCount = 0;
    private int docByteLimit = Integer.MAX_VALUE;
    private int docBytes = 0;
    private MinHeapImpl<HeapEntry> heap;
    private HashMap<URI, HeapEntry> docsInHeap;

    private static final class HeapEntry implements Comparable<HeapEntry>{
        private URI uri;
        private BTreeImpl<URI, Document> tree;
        public HeapEntry(URI myUri, BTreeImpl myTree){
            this.uri = myUri;
            this.tree = myTree;
        }
        public int compareTo(HeapEntry other){
            return tree.get(this.uri).compareTo(tree.get(other.uri));
        }
    }

    public DocumentStoreImpl(File basedir){
        this.myDocs = new BTreeImpl<>();
        myDocs.setPersistenceManager(new DocumentPersistenceManager(basedir));
        this.allCommands = new StackImpl<>();
        this.docSearch = new TrieImpl<>();
        this.metadataDocSearch = new TrieImpl<>();
        this.heap = new MinHeapImpl<>();
        this.docsInHeap = new HashMap<>();
    }

    public DocumentStoreImpl(){
        this.myDocs = new BTreeImpl<>();
        myDocs.setPersistenceManager(new DocumentPersistenceManager(null));
        this.allCommands = new StackImpl<>();
        this.docSearch = new TrieImpl<>();
        this.metadataDocSearch = new TrieImpl<>();
        this.heap = new MinHeapImpl<>();
        this.docsInHeap = new HashMap<>();
    }

    /**
     * set the given key-value metadata pair for the document at the given uri
     * @param uri
     * @param key
     * @param value
     * @return the old value, or null if there was no previous value
     * @throws IllegalArgumentException if the uri is null or blank, if there is no document stored at that uri, or if the key is null or blank
     */
    public String setMetadata(URI uri, String key, String value) throws IOException{
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("There's no uri");
        }
        Document doc = this.getDocFromTree(uri); //this deals with memory and heap
        if (doc == null){
            throw new IllegalArgumentException("There's no such document here");
        }
        String s = doc.setMetadataValue(key, value); //put the new value in, store the old value
        if (s != null){ //if there was a previous value, delete it from the trie
            metadataDocSearch.delete(key + s, uri);
        }
        if (value != null){ //if I'm putting a new value, add it to the trie
            metadataDocSearch.put(key + value, uri);
        }
        GenericCommand<URI> com = new GenericCommand<>(uri, u -> {
            Document d = getDocFromTree(u); //this updates heap and memory
            d.setMetadataValue(key, s); //put the old value back in the table
            if (s != null){ //put the old value back in the trie
                metadataDocSearch.put(key + s, u);
            }
            if (value != null){ //delete the new value from the trie
                metadataDocSearch.delete(key + value, u);
            }
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
    public String getMetadata(URI uri, String key) throws IOException{
        if (uri == null || uri.toString().isBlank()){
            throw new IllegalArgumentException("There's no such uri");
        }
        Document doc = getDocFromTree(uri);
        if (doc == null){
            throw new IllegalArgumentException("There's no Document here");
        }
        return doc.getMetadataValue(key);
    }
    /**
     * @param input the document being put
     * @param url unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if uri is null or empty, or format is null
     */
    public int put(InputStream input, URI url, DocumentStore.DocumentFormat format) throws IOException{
        this.illegalPuts(url, format);
        Document doc = this.createNewDoc(input, url, format);
        Document previousDoc = myDocs.get(url);
        int previous = 0;
        if (previousDoc != null){
            previous = previousDoc.hashCode();
        }
        if (input == null){
            delete(url);
            return previous;
        }
        this.removeDocFromStore(previousDoc);
        this.putNewDoc(url, doc);
        this.putDocInHeap(doc);
        GenericCommand<URI> com = new GenericCommand<>(url, u -> {
            undoPut(previousDoc, doc);
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
     * takes the arguments from the put method and creates the DocumentImpl with the correct constructor
     * @param input
     * @param url
     * @param format
     * @return the DocumentImpl
     * @throws IOException
     */
    private Document createNewDoc(InputStream input, URI url, DocumentFormat format) throws IOException {
        if (input == null){
            return null;
        }
        byte[] contents = input.readAllBytes();
        if (contents.length > docByteLimit){
            throw new IllegalArgumentException("you can't create a doc that is bigger than the limit");
        }
        Document doc;
        if (format == DocumentFormat.TXT) {
            String words = new String(contents);
            doc = new DocumentImpl(url, words, null);
        } else {
            doc = new DocumentImpl(url, contents);
        }
        return doc;
    }

    /**
     * Removes the document from the trie for its words and its metadata
     * removes the document from the memory counts and the heap if neccesary
     * @param previous
     */
    private void removeDocFromStore(Document previous){
        if (previous == null){
            return;
        }
        URI uri = previous.getKey();
        for (String word : previous.getWords()){
            this.docSearch.delete(word, uri);
        }
        HashMap<String, String> metadata = previous.getMetadata();
        for (String key : metadata.keySet()){
            String pair = key + metadata.get(key);
            this.metadataDocSearch.delete(pair, uri);
        }
        if (docsInHeap.get(uri) != null){
            decreaseDocFromMemoryCounts(previous);
            removeDocFromHeap(previous);
        }
    }

    /**
     * clears space in the store to put the new document
     * puts the new doc into the heap
     * updates the memory counts
     * @param doc the document to insert
     */
    private void putDocInHeap(Document doc){
        int bytesNeeded = getDocBytes(doc);
        if (bytesNeeded > docByteLimit){ //if this doc is bigger than the limit, we put every doc on disk including this one
            while (docCount > 0){
                this.moveToDisk();
            }
            try {
                myDocs.moveToDisk(doc.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        while ((docCount + 1) > docCountLimit){
            this.moveToDisk();
        }
        while ((docBytes + bytesNeeded) > docByteLimit){
            this.moveToDisk();
        }
        doc.setLastUseTime(System.nanoTime());
        HeapEntry entry = new HeapEntry(doc.getKey(), myDocs);
        this.heap.insert(entry);
        this.docsInHeap.put(entry.uri, entry);
        this.docCount++;
        this.docBytes += bytesNeeded;
    }

    /**
     * puts the new document into the tree
     * puts the new document into the trie for all of its words
     * @param url
     * @param doc
     */
    private void putNewDoc(URI url, Document doc){
        this.myDocs.put(url, doc);
        for (String word : doc.getWords()){
            this.docSearch.put(word, url);
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
    private void undoPut(Document prevDoc, Document newDoc){
        removeDocFromStore(newDoc);
        URI url = newDoc.getKey();
        undoDelete(url, prevDoc);
    }

    /**
     * @param url the unique identifier of the document to get
     * @return the given document
     */
    public Document get(URI url) throws IOException{
        return getDocFromTree(url);
    }

    /**
     * gets the document associated with this uri
     * updates the lastUseTime and calls reHeapify
     * if the doc was on disk, makes space for it and creates a heap entry
     * @param uri
     * @return the document. null if it is not in the tree
     */
    private Document getDocFromTree(URI uri){
        Document doc = myDocs.get(uri);
        if (doc == null){
            return null;
        }
        HeapEntry entry = docsInHeap.get(uri);
        if (entry != null){ //the doc was in memory
            doc.setLastUseTime(System.nanoTime());
            heap.reHeapify(entry);
        } else { //the doc was on disk
            putDocInHeap(doc);
        }
        return doc;
    }

    /**
     * @param url the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    public boolean delete(URI url){
        if (url == null || url.toString().isBlank()){
            return false;
        }
        Document doc = myDocs.get(url);
        if (doc != null){
            removeDocFromStore(doc);
            GenericCommand<URI> com = new GenericCommand<>(url, u -> {
                undoDelete(u, doc);
            });
            allCommands.push(com);
        }
        return (myDocs.put(url, null) != null);
    }

    /**
     * the undo logic to create the GenericCommand for the delete method
     * puts the document back in the tree and the trie
     * puts the document back into the heap
     * @param doc the document we want to put back
     */
    private void undoDelete(URI url, Document doc){
        this.myDocs.put(url, doc);
        if (doc != null){
            for (String word : doc.getWords()){
                this.docSearch.put(word, url);
            }
            HashMap<String, String> metadata = doc.getMetadata();
            for (String key : metadata.keySet()){
                String pair = key + metadata.get(key);
                this.metadataDocSearch.put(pair, url);
            }
            putDocInHeap(doc);
        }

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

    //**********STAGE 4 ADDITIONS

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

    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> search(String keyword) throws IOException{
        List<Document> docs = searchWithoutUpdatingTime(keyword);
        return updateDocumentsToSetTime(docs);
    }

    /**
     * does the search by keyword logic without updating the time of these documents so that other methods can utilize this logic
     * @param keyword
     * @return the documents with this keyword in sorted order
     */
    private List<Document> searchWithoutUpdatingTime(String keyword){
        String key = stripIllegalCharacters(keyword);
        Comparator<URI> comparator = new Comparator<URI>() {
            @Override
            public int compare(URI uri1, URI uri2) {
                return myDocs.get(uri2).wordCount(key) - myDocs.get(uri1).wordCount(key);
            }
        };
        List<URI> uris = docSearch.getSorted(key, comparator);
        List<Document> docs = new ArrayList<>();
        for (URI uri : uris){
            docs.add(myDocs.get(uri));
        }
        return docs;
    }

    /**
     * takes the list of documents which has been retrieved by a search method and updates the memory and heap.
     * if a doc was already in memory, just updates doc to have the set time.
     * if it was on disk, creates a heap entry, makes space for it in memory, and gives it the set time.
     * @param docs
     * @return the updated documents
     */
    private List<Document> updateDocumentsToSetTime(List<Document> docs){
        long time = System.nanoTime();
        for (Document doc : docs){
            HeapEntry entry = docsInHeap.get(doc.getKey());
            if (entry != null){
                doc.setLastUseTime(time);
                heap.reHeapify(entry);
            } else {
                putDocInHeapWithSetTime(doc, time);
            }
        }
        return docs;
    }

    /**
     * clears space for the doc and puts it in the heap
     * updates the memory count
     * @param doc assumed to not be in the heap yet
     * @param time the set time we are giving this doc, so it can be the same as other docs
     */
    private void putDocInHeapWithSetTime(Document doc, long time){
        int bytesNeeded = getDocBytes(doc);
        if (bytesNeeded > docByteLimit){ //if this doc is bigger than the limit, we put every doc on disk including this one
            while (docCount > 0){
                this.moveToDisk();
            }
            try {
                myDocs.moveToDisk(doc.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        while ((docCount + 1) > docCountLimit){
            this.moveToDisk();
        }
        while ((docBytes + bytesNeeded) > docByteLimit){
            this.moveToDisk();
        }
        doc.setLastUseTime(time);
        HeapEntry entry = new HeapEntry(doc.getKey(), myDocs);
        this.heap.insert(entry);
        this.docsInHeap.put(entry.uri, entry);
        this.docCount++;
        this.docBytes += bytesNeeded;
    }

    /**
     * Retrieve all documents containing a word that starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> searchByPrefix(String keywordPrefix) throws IOException{
        List<Document> docs = searchByPrefixWithoutUpdatingTime(keywordPrefix);
        return updateDocumentsToSetTime(docs);
    }

    /**
     * same as the searchByPrefix method but without updating time so that other methods can use this logic
     * @param keywordPrefix
     * @return the docs with this prefix in sorted order
     */
    private List<Document> searchByPrefixWithoutUpdatingTime(String keywordPrefix){
        String prefix = stripIllegalCharacters(keywordPrefix);
        Comparator<URI> comparator = new Comparator<URI>() {
            @Override
            public int compare(URI uri1, URI uri2) {
                int firstCount = prefixCount(myDocs.get(uri1), prefix);
                int secondCount = prefixCount(myDocs.get(uri2), prefix);
                return secondCount - firstCount;
            }
        };
        List<URI> uris = docSearch.getAllWithPrefixSorted(prefix, comparator);
        List<Document> docs = new ArrayList<>();
        for (URI uri : uris){
            docs.add(myDocs.get(uri));
        }
        return docs;
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

    /**
     * Completely remove any trace of any document which contains the given keyword
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAll(String keyword){
        String key = stripIllegalCharacters(keyword);
        Set<URI> URIsWithKeyword = docSearch.deleteAll(key);
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (URI uri : URIsWithKeyword){
            Document doc = myDocs.get(uri);
            removeDocFromStore(doc);
            myDocs.put(uri, null);
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
        return URIsWithKeyword;
    }

    /**
     * The same logic as undoDelete, but you can send a set time
     * so an undo of a method that deletes several docs gives them all the same time
     * @param doc
     * @param time
     */
    private void undoDeleteWithSetTime(Document doc, long time){
        URI uri = doc.getKey();
        for (String word : doc.getWords()){
            this.docSearch.put(word, uri);
        }
        HashMap<String, String> metadata = doc.getMetadata();
        for (String key : metadata.keySet()){
            String pair = key + metadata.get(key);
            this.metadataDocSearch.put(pair, uri);
        }
        myDocs.put(uri, doc);
        putDocInHeapWithSetTime(doc, time);
    }

    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefix(String keywordPrefix){
        String keypre = stripIllegalCharacters(keywordPrefix);
        Set<URI> URIsWithPrefix = docSearch.deleteAllWithPrefix(keypre);
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (URI uri : URIsWithPrefix){
            Document doc = myDocs.get(uri);
            removeDocFromStore(doc);
            myDocs.put(uri, null);
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
        return URIsWithPrefix;
    }

    /**
     * @param keysValues metadata key-value pairs to search for
     * @return a List of all documents whose metadata contains ALL OF the given values for the given keys. If no documents contain all the given key-value pairs, return an empty list.
     */
    public List<Document> searchByMetadata(Map<String,String> keysValues) throws IOException{
        List<Document> docs = this.searchByMetadataWithoutUpdatingTime(keysValues);
        return updateDocumentsToSetTime(docs);
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
        List<Set<URI>> sets = new ArrayList<>();
        for (String key : keysValues.keySet()){
            String keyValuePair = key + keysValues.get(key);
            Set<URI> docsForThisPair = metadataDocSearch.get(keyValuePair);
            if (docsForThisPair.isEmpty()){
                return new ArrayList<Document>();
            }
            sets.add(docsForThisPair);
        }
        sets.sort(Comparator.comparingInt(Set::size));
        Set<URI> smallestSet = sets.get(0);
        for (Set<URI> s : sets){
            if (s == sets.get(0)){continue;}
            smallestSet.retainAll(s);
            if (smallestSet.isEmpty()){break;}
        }
        List<Document> docs = new ArrayList<>();
        for (URI uri : smallestSet){
            docs.add(myDocs.get(uri));
        }
        return docs;
    }

    /**
     * Retrieve all documents whose text contains the given keyword AND which has the given key-value pairs in its metadata
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE SENSITIVE.
     * @param keyword
     * @param keysValues
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> searchByKeywordAndMetadata(String keyword, Map<String,String> keysValues) throws IOException{
        List<Document> docsByKeywordAndMetadata = searchByKeyWordAndMetadataWithoutUpdatingTime(keyword, keysValues);
        return updateDocumentsToSetTime(docsByKeywordAndMetadata);
    }

    /**
     * do the search logic for searchByKeywordAndMetadata
     * but don't put these docs in the heap so the logic can be used by deleteAllWithKeyWordAndMetadata
     * @param keyword
     * @param keysValues
     * @return
     * @throws IOException
     */
    private List<Document> searchByKeyWordAndMetadataWithoutUpdatingTime(String keyword, Map<String, String> keysValues) throws IOException{
        String key = stripIllegalCharacters(keyword);
        List<Document> docsByKeyword = this.searchWithoutUpdatingTime(key);
        List<Document> docsByMetadata = this.searchByMetadataWithoutUpdatingTime(keysValues);
        Set<Document> docsByMetadataSet = new HashSet<>(docsByMetadata);

        List<Document> docsByKeywordAndMetadata = new ArrayList<>();
        for (Document doc : docsByKeyword){
            if (docsByMetadataSet.contains(doc)){
                docsByKeywordAndMetadata.add(doc);
            } else {
                if (docsInHeap.get(doc.getKey()) == null){ //this doc was on disk, but we moved it to memory by calling BTree.get
                    myDocs.moveToDisk(doc.getKey());
                }
            }
        }
        return docsByKeywordAndMetadata;
    }


    /**
     * Retrieve all documents that contain text which starts with the given prefix AND which has the given key-value pairs in its metadata
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    public List<Document> searchByPrefixAndMetadata(String keywordPrefix, Map<String,String> keysValues) throws IOException{
        List<Document> docsByPrefixAndMetadata = searchByPrefixAndMetadataWithoutUpdatingTime(keywordPrefix, keysValues);
        return updateDocumentsToSetTime(docsByPrefixAndMetadata);
    }

    /**
     * do the search logic for searchByPrefixAndMetadata
     * but don't put these docs in the heap so the logic can be used by deleteAllWithPrefixAndMetadata
     * @param keywordPrefix
     * @param keysValues
     * @return
     * @throws IOException
     */
    private List<Document> searchByPrefixAndMetadataWithoutUpdatingTime(String keywordPrefix, Map<String,String> keysValues) throws IOException{
        String keypre = stripIllegalCharacters(keywordPrefix);
        List<Document> docsByPrefix = this.searchByPrefixWithoutUpdatingTime(keypre);
        List<Document> docsByMetadata = this.searchByMetadataWithoutUpdatingTime(keysValues);
        Set<Document> docsByMetadataSet = new HashSet<>(docsByMetadata);

        List<Document> docsByPrefixAndMetadata = new ArrayList<>();
        for (Document doc : docsByPrefix){
            if (docsByMetadataSet.contains(doc)){
                docsByPrefixAndMetadata.add(doc);
            } else {
                if (docsInHeap.get(doc.getKey()) == null){ //this doc was on disk, but we moved it to memory by calling BTree.get
                    myDocs.moveToDisk(doc.getKey());
                }
            }
        }
        return docsByPrefixAndMetadata;
    }

    /**
     * Completely remove any trace of any document which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithMetadata(Map<String,String> keysValues) throws IOException{
        Set<Document> docsWithMetadata= new HashSet<>(searchByMetadataWithoutUpdatingTime(keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithMetadata){
            URI uri = doc.getKey();
            deletedURIs.add(uri);
            removeDocFromStore(doc);
            myDocs.put(uri, null);
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

    /**
     * Completely remove any trace of any document which contains the given keyword AND which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithKeywordAndMetadata(String keyword,Map<String,String> keysValues) throws IOException{
        Set<Document> docsWithKeywordAndMetadata = new HashSet<>(this.searchByKeyWordAndMetadataWithoutUpdatingTime(keyword, keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithKeywordAndMetadata){
            URI uri = doc.getKey();
            deletedURIs.add(uri);
            removeDocFromStore(doc);
            myDocs.put(uri, null);
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
    /**
     * Completely remove any trace of any document which contains a word that has the given prefix AND which has the given key-value pairs in its metadata
     * Search is CASE SENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefixAndMetadata(String keywordPrefix, Map<String,String> keysValues) throws IOException{
        Set<Document> docsWithPrefixAndMetadata = new HashSet<>(searchByPrefixAndMetadataWithoutUpdatingTime(keywordPrefix, keysValues));
        Set<URI> deletedURIs = new HashSet<>();
        CommandSet<URI> commands = new CommandSet<>();
        long[] time = new long[1];
        for (Document doc : docsWithPrefixAndMetadata){
            URI uri = doc.getKey();
            deletedURIs.add(uri);
            removeDocFromStore(doc);
            myDocs.put(uri, null);
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
            moveToDisk();
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
            moveToDisk();
        }
    }

    /**
     * When we need to clear space in memory, removes the top doc from the heap and puts it on disk
     * updates the memory count
     */
    private void moveToDisk(){
        HeapEntry entry = this.heap.remove();
        Document removed = myDocs.get(entry.uri);
        try {
            myDocs.moveToDisk(removed.getKey());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        docsInHeap.remove(entry.uri);
        this.decreaseDocFromMemoryCounts(removed);
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

    /**
     * removes the given document from the heap by moving it to the top
     * assumes the doc is currently in the heap
     * @param doc
     */
    private void removeDocFromHeap(Document doc){
        doc.setLastUseTime(Long.MIN_VALUE);
        HeapEntry entry = docsInHeap.get(doc.getKey());
        heap.reHeapify(entry);
        heap.remove();
        docsInHeap.remove(entry.uri);
    }

    /**
     * decrements the doc from the count and decreases the docBytes
     * @param doc
     */
    private void decreaseDocFromMemoryCounts(Document doc){
        docCount--;
        docBytes -= getDocBytes(doc);
    }
}
