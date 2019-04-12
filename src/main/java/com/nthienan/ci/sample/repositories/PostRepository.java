package com.nthienan.ci.sample.repositories;

import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.nthienan.ci.sample.MongoConfiguration;
import com.nthienan.ci.sample.model.Post;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Repository used to manage tags for a blog post.
 *
 */
@Repository
public class PostRepository implements Closeable {
    /**
     * Document field which holds the blog tags.
     */
    private static final String TAGS_FIELD = "tags";

    /**
     * The post collection.
     */
    private MongoCollection<Document> collection;

    /**
     * The MongoDB client, stored in a field in order to close it later.
     */
    private MongoClient mongoClient;

    @Autowired
    private MongoConfiguration config;

    /**
     * Instantiates a new PostRepository by opening the DB connection.
     */
    public PostRepository(MongoConfiguration config) {
        MongoClientURI mongoClientURI = new MongoClientURI(config.connectURI());
        mongoClient = new MongoClient(mongoClientURI);
        MongoDatabase database = mongoClient.getDatabase(config.getDatabase());
        collection = database.getCollection("post");
    }

    /**
     * Returns a list of posts which contains one or more of the tags passed as
     * argument.
     *
     * @param tags
     *            a list of tags
     * @return a list of posts which contains at least one of the tags passed as
     *         argument
     */
    public List<Post> getPostsWithAtLeastOneTag(String... tags) {
        FindIterable<Document> results = collection.find(Filters.in(TAGS_FIELD, tags));
        return StreamSupport.stream(results.spliterator(), false).map(PostRepository::documentToPost)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of posts which contains all the tags passed as argument.
     *
     * @param tags
     *            a list of tags
     * @return a list of posts which contains all the tags passed as argument
     */
    public List<Post> getPostsWithAllTags(String... tags) {
        FindIterable<Document> results = collection.find(Filters.all(TAGS_FIELD, tags));
        return StreamSupport.stream(results.spliterator(), false).map(PostRepository::documentToPost)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of posts which contains none of the tags passed as
     * argument.
     *
     * @param tags
     *            a list of tags
     * @return a list of posts which contains none of the tags passed as
     *         argument
     */
    public List<Post> getPostsWithoutTags(String... tags) {
        FindIterable<Document> results = collection.find(Filters.nin(TAGS_FIELD, tags));
        return StreamSupport.stream(results.spliterator(), false).map(PostRepository::documentToPost)
                .collect(Collectors.toList());
    }

    public List<Post> getAll() {
        List<Post> result = new ArrayList<>();
        MongoCursor<Document> cursor = collection.find().iterator();
        while (cursor.hasNext()) {
            result.add(documentToPost(cursor.next()));
        }
        return result;
    }

    /**
     * Adds a list of tags to the blog post with the given title.
     *
     * @param title
     *            the title of the blog post
     * @param tags
     *            a list of tags to add
     * @return the outcome of the operation
     */
    public boolean addTags(String title, List<String> tags) {
        UpdateResult result = collection.updateOne(new BasicDBObject(DBCollection.ID_FIELD_NAME, title),
                Updates.addEachToSet(TAGS_FIELD, tags));
        return result.getModifiedCount() == 1;
    }

    /**
     * Removes a list of tags to the blog post with the given title.
     *
     * @param title
     *            the title of the blog post
     * @param tags
     *            a list of tags to remove
     * @return the outcome of the operation
     */
    public boolean removeTags(String title, List<String> tags) {
        UpdateResult result = collection.updateOne(new BasicDBObject(DBCollection.ID_FIELD_NAME, title),
                Updates.pullAll(TAGS_FIELD, tags));
        return result.getModifiedCount() == 1;
    }

    /**
     * Utility method used to map a MongoDB document into a {@link Post}.
     *
     * @param document
     *            the document to map
     * @return a {@link Post} object equivalent to the document passed as
     *         argument
     */
    @SuppressWarnings("unchecked")
    private static Post documentToPost(Document document) {
        Post post = new Post();
        post.setTitle(document.getString("title"));
        post.setAuthor(document.getString("author"));
        post.setTags((List<String>) document.get(TAGS_FIELD));
        return post;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException {
        mongoClient.close();
    }
}
