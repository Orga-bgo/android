package de.babixgo.monopolygo.database;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Firebase Realtime Database Manager
 * Ersetzt SupabaseManager.java
 * 
 * WICHTIG: Singleton Pattern beibehalten für Konsistenz
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    
    private final FirebaseDatabase database;
    private boolean configured = false;
    
    private FirebaseManager() {
        database = FirebaseDatabase.getInstance();
        
        // Offline Persistence aktivieren (funktioniert auch ohne Internet!)
        try {
            database.setPersistenceEnabled(true);
            configured = true;
            Log.d(TAG, "Firebase initialized with offline persistence");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }
    }
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    public boolean isConfigured() {
        return configured;
    }
    
    /**
     * Get reference to a collection
     * Beispiel: getReference("accounts")
     */
    public DatabaseReference getReference(String path) {
        return database.getReference(path);
    }
    
    /**
     * Get all items from collection
     * KOMPATIBEL mit alten Repository-Methoden
     */
    public <T> CompletableFuture<List<T>> getAll(String collection, Class<T> clazz) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        
        getReference(collection).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<T> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    T item = child.getValue(clazz);
                    if (item != null) {
                        items.add(item);
                    }
                }
                future.complete(items);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(
                    new RuntimeException("Firebase read failed: " + error.getMessage())
                );
            }
        });
        
        return future;
    }
    
    /**
     * Get items with query/filter
     * Supports ordering and filtering
     */
    public <T> CompletableFuture<List<T>> query(String collection, Class<T> clazz, QueryBuilder queryBuilder) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        
        Query query = queryBuilder.build(getReference(collection));
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<T> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    T item = child.getValue(clazz);
                    if (item != null) {
                        items.add(item);
                    }
                }
                future.complete(items);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(
                    new RuntimeException("Firebase query failed: " + error.getMessage())
                );
            }
        });
        
        return future;
    }
    
    /**
     * Get single item by ID
     */
    public <T> CompletableFuture<T> getById(String collection, String id, Class<T> clazz) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        getReference(collection).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                T item = snapshot.getValue(clazz);
                future.complete(item);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                future.completeExceptionally(
                    new RuntimeException("Firebase read failed: " + error.getMessage())
                );
            }
        });
        
        return future;
    }
    
    /**
     * Get single item by field value
     */
    public <T> CompletableFuture<T> getByField(String collection, String fieldName, Object fieldValue, Class<T> clazz) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        getReference(collection)
            .orderByChild(fieldName)
            .equalTo(fieldValue.toString())
            .limitToFirst(1)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    T item = null;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        item = child.getValue(clazz);
                        break; // Get first item only
                    }
                    future.complete(item);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(
                        new RuntimeException("Firebase read failed: " + error.getMessage())
                    );
                }
            });
        
        return future;
    }
    
    /**
     * Create or update item
     * Auto-generiert ID wenn nicht vorhanden
     */
    public <T> CompletableFuture<T> save(String collection, T item, String id) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        String itemId = (id != null) ? id : getReference(collection).push().getKey();
        
        getReference(collection).child(itemId).setValue(item)
            .addOnSuccessListener(aVoid -> future.complete(item))
            .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Update specific fields
     */
    public CompletableFuture<Void> updateFields(String collection, String id, java.util.Map<String, Object> updates) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        getReference(collection).child(id).updateChildren(updates)
            .addOnSuccessListener(aVoid -> future.complete(null))
            .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Delete item
     */
    public CompletableFuture<Void> delete(String collection, String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        getReference(collection).child(id).removeValue()
            .addOnSuccessListener(aVoid -> future.complete(null))
            .addOnFailureListener(future::completeExceptionally);
        
        return future;
    }
    
    /**
     * Query Builder for constructing Firebase queries
     */
    public static class QueryBuilder {
        private String orderByField;
        private Object equalToValue;
        private Object startAtValue;
        private Object endAtValue;
        private Integer limitFirst;
        private Integer limitLast;
        
        public QueryBuilder orderByChild(String field) {
            this.orderByField = field;
            return this;
        }
        
        public QueryBuilder equalTo(Object value) {
            this.equalToValue = value;
            return this;
        }
        
        public QueryBuilder startAt(Object value) {
            this.startAtValue = value;
            return this;
        }
        
        public QueryBuilder endAt(Object value) {
            this.endAtValue = value;
            return this;
        }
        
        public QueryBuilder limitToFirst(int limit) {
            this.limitFirst = limit;
            return this;
        }
        
        public QueryBuilder limitToLast(int limit) {
            this.limitLast = limit;
            return this;
        }
        
        public Query build(DatabaseReference ref) {
            Query query = ref;
            
            if (orderByField != null) {
                query = query.orderByChild(orderByField);
            }
            
            if (equalToValue != null) {
                if (equalToValue instanceof String) {
                    query = query.equalTo((String) equalToValue);
                } else if (equalToValue instanceof Number) {
                    query = query.equalTo(((Number) equalToValue).doubleValue());
                } else if (equalToValue instanceof Boolean) {
                    query = query.equalTo((Boolean) equalToValue);
                }
            }
            
            if (startAtValue != null) {
                if (startAtValue instanceof String) {
                    query = query.startAt((String) startAtValue);
                } else if (startAtValue instanceof Number) {
                    query = query.startAt(((Number) startAtValue).doubleValue());
                }
            }
            
            if (endAtValue != null) {
                if (endAtValue instanceof String) {
                    query = query.endAt((String) endAtValue);
                } else if (endAtValue instanceof Number) {
                    query = query.endAt(((Number) endAtValue).doubleValue());
                }
            }
            
            if (limitFirst != null) {
                query = query.limitToFirst(limitFirst);
            }
            
            if (limitLast != null) {
                query = query.limitToLast(limitLast);
            }
            
            return query;
        }
    }
}
