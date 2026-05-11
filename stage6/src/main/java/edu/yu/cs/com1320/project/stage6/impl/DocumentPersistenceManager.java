package edu.yu.cs.com1320.project.stage6.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import edu.yu.cs.com1320.project.stage6.Document;
import edu.yu.cs.com1320.project.stage6.PersistenceManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import jakarta.xml.bind.DatatypeConverter;

public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File directory;
    private Gson gson;

    public DocumentPersistenceManager(File basedir){
        if (basedir == null){
            directory = new File(System.getProperty("user.dir"));
        } else {
            this.directory = basedir;
        }
        if (!this.directory.exists()){
            this.directory.mkdirs();
        }
        GsonBuilder builder = new GsonBuilder();
        this.createSerializer(builder);
        this.createDeserializer(builder);
        this.gson = builder.create();
    }

    private void createSerializer(GsonBuilder builder){
        builder.registerTypeAdapter(Document.class, new JsonSerializer<Document>() {
            @Override
            public JsonElement serialize(Document doc, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject json = new JsonObject();
                if (doc.getDocumentTxt() != null){
                    json.addProperty("text", doc.getDocumentTxt());
                    json.add("wordmap", context.serialize(doc.getWordMap()));
                } else {
                    String bytes = DatatypeConverter.printBase64Binary(doc.getDocumentBinaryData());
                    json.addProperty("bytes", bytes);
                }
                json.add("metadata", context.serialize(doc.getMetadata()));
                json.addProperty("uri", doc.getKey().toString());
                return json;
            }
        });
    }

    private void createDeserializer(GsonBuilder builder){
        builder.registerTypeAdapter(Document.class, new JsonDeserializer<Document>() {
            @Override
            public Document deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                JsonObject obj = json.getAsJsonObject();

                Document doc;
                String uriString = obj.get("uri").getAsString();
                URI uri = URI.create(uriString);
                //if the doc has text and a wordmap
                if (obj.has("text")){
                    String text = obj.get("text").getAsString();
                    Type mapType = new TypeToken<Map<String, Integer>>(){}.getType();
                    Map<String, Integer> wordCountMap = context.deserialize(obj.get("wordmap"), mapType);
                    doc = new DocumentImpl(uri, text, new HashMap<>(wordCountMap));
                } else { //the doc has bytes
                    String bytesString = obj.get("bytes").getAsString();
                    byte[] bytes = DatatypeConverter.parseBase64Binary(bytesString);
                    doc = new DocumentImpl(uri, bytes);
                }
                //add the metadata
                Type metadataType = new TypeToken<HashMap<String,String>>(){}.getType();
                HashMap<String, String> metadata= context.deserialize(obj.get("metadata"), metadataType);
                doc.setMetadata(metadata);
                return doc;
            }
        });
    }

    private File getFileFromUri(URI uri){
        String host = uri.getHost();
        String path = uri.getRawPath();
        if (path == null){
            path = "";
        }
        if (path.startsWith("/")){
            path = path.substring(1);
        }
        if (path.endsWith("/")){
            path = path.substring(0, path.length()-1);
        }
        if (path.isEmpty()){
            return new File(directory, host + ".json");
        }
        File fileWithoutPath = new File(directory, host);
        return new File(fileWithoutPath, path + ".json");
    }

    @Override
    public void serialize(URI key, Document val) throws IOException{
        File file = this.getFileFromUri(key);
        File parent = file.getParentFile();
        if (parent != null){
            parent.mkdirs();
        }
        if (!file.exists()){
            file.createNewFile();
        }
        try (FileWriter fw = new FileWriter(file)){
            gson.toJson(val, Document.class, fw);
        }
    }

    @Override
    public Document deserialize(URI key) throws IOException{
        File file = this.getFileFromUri(key);
        if (!file.exists()){
            return null;
        }
        Document doc;
        try (FileReader fr = new FileReader(file)){
            doc = gson.fromJson(fr, Document.class);
        }
        file.delete();
        return doc;
    }

    /**
     * delete the file stored on disk that corresponds to the given key
     * @param key
     * @return true or false to indicate if deletion occured or not
     * @throws IOException
     */
    @Override
    public boolean delete(URI key) throws IOException{
        File file = getFileFromUri(key);
        return file.delete();
    }
}