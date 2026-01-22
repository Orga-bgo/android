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
import java.lang.reflect.Method;
import java.util.Map;

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
     * Helper method to set ID on an object from Firebase key
     * Uses reflection to call setId(long id) method
     * 
     * @param item Object to set ID on
     * @param key Firebase key to use as ID
     * @param <T> Object type
     */
    private <T> void setIdFromKey(T item, String key) {
        if (item == null || key == null) {
            return;
        }
        
        try {
            Method setIdMethod = item.getClass().getMethod("setId", long.class);
            setIdMethod.invoke(item, Long.parseLong(key));
        } catch (NoSuchMethodException e) {
            Log.w(TAG, "Object does not have setId(long) method: " + item.getClass().getName());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Firebase key is not a valid number: " + key);
        } catch (Exception e) {
            Log.w(TAG, "Failed to set ID on object", e);
        }
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
                        setIdFromKey(item, child.getKey());
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
                        setIdFromKey(item, child.getKey());
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
     * Get single object by ID
     * 
     * @param collection Collection path
     * @param id Object ID
     * @param clazz Class type for deserialization
     * @return CompletableFuture with object or null if not found
     */
    public <T> CompletableFuture<T> getById(String collection, String id, Class<T> clazz) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        if (!configured) {
            future.completeExceptionally(
                new RuntimeException("Firebase ist nicht konfiguriert")
            );
            return future;
        }
        
        getReference(collection).child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                T item = snapshot.getValue(clazz);
                if (item != null) {
                    setIdFromKey(item, snapshot.getKey());
                    Log.d(TAG, "Found object in " + collection + "/" + id);
                } else {
                    Log.d(TAG, "No object found in " + collection + "/" + id);
                }
                future.complete(item);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "getById failed: " + error.getMessage());
                future.completeExceptionally(
                    new RuntimeException("Firebase read failed: " + error.getMessage())
                );
            }
        });
        
        return future;
    }
    
    /**
     * Get single object by field value
     * Returns first matching object
     * 
     * @param collection Collection path
     * @param field Field name to query
     * @param value Value to match
     * @param clazz Class type for deserialization
     * @return CompletableFuture with object or null if not found
     */
    public <T> CompletableFuture<T> getByField(String collection, String field, Object value, Class<T> clazz) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        if (!configured) {
            future.completeExceptionally(
                new RuntimeException("Firebase ist nicht konfiguriert")
            );
            return future;
        }
        
        Query query = getReference(collection).orderByChild(field);
        
        // Handle different value types for Firebase equalTo
        if (value instanceof String) {
            query = query.equalTo((String) value);
        } else if (value instanceof Number) {
            query = query.equalTo(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            query = query.equalTo((Boolean) value);
        } else {
            // Fallback to string representation
            query = query.equalTo(value.toString());
        }
        
        query.limitToFirst(1)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        T item = child.getValue(clazz);
                        if (item != null) {
                            setIdFromKey(item, child.getKey());
                            Log.d(TAG, "Found object by " + field + "=" + value);
                        }
                        future.complete(item);
                        return;
                    }
                    Log.d(TAG, "No object found by " + field + "=" + value);
                    future.complete(null);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "getByField failed: " + error.getMessage());
                    future.completeExceptionally(
                        new RuntimeException("Firebase query failed: " + error.getMessage())
                    );
                }
            });
        
        return future;
    }
    
    /**
     * Save or update object in Firebase
     * Automatically handles ID generation for new objects
     * 
     * @param collection Collection path (e.g. "accounts")
     * @param object Object to save
     * @param id Optional ID (null for auto-generate)
     * @return CompletableFuture with saved object (including generated ID)
     */
    public <T> CompletableFuture<T> save(String collection, T object, String id) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        if (!configured) {
            future.completeExceptionally(
                new RuntimeException("Firebase ist nicht konfiguriert")
            );
            return future;
        }
        
        DatabaseReference ref = getReference(collection);
        DatabaseReference itemRef = (id != null) ? ref.child(id) : ref.push();
        
        itemRef.setValue(object)
            .addOnSuccessListener(aVoid -> {
                // Set ID on object if it has setId method
                try {
                    String generatedId = itemRef.getKey();
                    Method setIdMethod = object.getClass().getMethod("setId", long.class);
                    setIdMethod.invoke(object, Long.parseLong(generatedId));
                    Log.d(TAG, "Saved object to " + collection + "/" + generatedId);
                } catch (Exception e) {
                    Log.w(TAG, "Could not set ID on object", e);
                }
                future.complete(object);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Save failed: " + e.getMessage());
                future.completeExceptionally(
                    new RuntimeException("Firebase save failed: " + e.getMessage())
                );
            });
        
        return future;
    }
    
    /**
     * Update specific fields of an object
     * Does NOT overwrite the entire object
     * 
     * @param collection Collection path
     * @param id Object ID
     * @param updates Map of field names to new values
     * @return CompletableFuture that completes when update is done
     */
    public CompletableFuture<Void> updateFields(String collection, String id, Map<String, Object> updates) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        if (!configured) {
            future.completeExceptionally(
                new RuntimeException("Firebase ist nicht konfiguriert")
            );
            return future;
        }
        
        getReference(collection).child(id).updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Updated " + updates.size() + " fields in " + collection + "/" + id);
                future.complete(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "updateFields failed: " + e.getMessage());
                future.completeExceptionally(
                    new RuntimeException("Firebase update failed: " + e.getMessage())
                );
            });
        
        return future;
    }
    
    /**
     * Delete object by ID
     * 
     * @param collection Collection path
     * @param id Object ID
     * @return CompletableFuture that completes when deletion is done
     */
    public CompletableFuture<Void> delete(String collection, String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        if (!configured) {
            future.completeExceptionally(
                new RuntimeException("Firebase ist nicht konfiguriert")
            );
            return future;
        }
        
        getReference(collection).child(id).removeValue()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Deleted object from " + collection + "/" + id);
                future.complete(null);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "delete failed: " + e.getMessage());
                future.completeExceptionally(
                    new RuntimeException("Firebase delete failed: " + e.getMessage())
                );
            });
        
        return future;
    }
    
    /**
     * Query with simple orderBy (basic example)
     * Für komplexere Queries: QueryBuilder verwenden
     */
    public <T> CompletableFuture<List<T>> query(String collection, String orderBy, Class<T> clazz) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        
        getReference(collection)
            .orderByChild(orderBy)
            .addListenerForSingleValueEvent(new ValueEventListener() {
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
     * Realtime Listener (NEU - nicht in Supabase!)
     * Updates werden automatisch gepusht
     * 
     * WICHTIG: Listener bleibt aktiv bis removeEventListener() aufgerufen wird
     * 
     * Beispiel:
     * firebaseManager.addRealtimeListener("accounts", Account.class, new RealtimeListener<Account>() {
     *     @Override
     *     public void onDataChanged(List<Account> items) {
     *         // UI automatisch aktualisieren
     *         adapter.setAccounts(items);
     *     }
     *     
     *     @Override
     *     public void onError(Exception e) {
     *         Log.e(TAG, "Realtime update failed", e);
     *     }
     * });
     */
    public <T> void addRealtimeListener(String collection, Class<T> clazz, 
                                        RealtimeListener<T> listener) {
        getReference(collection).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<T> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    T item = child.getValue(clazz);
                    if (item != null) {
                        items.add(item);
                    }
                }
                listener.onDataChanged(items);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }
    
    /**
     * Interface for realtime data updates
     */
    public interface RealtimeListener<T> {
        void onDataChanged(List<T> items);
        void onError(Exception e);
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
