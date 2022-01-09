package database;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import models.Chatroom;
import models.Friend;
import models.User;
import models.chat.ChatHistory;
import models.chat.ChatMessage;
import models.chat.ChatMessageFactory;
import utils.JsonUtils;
import utils.TimeUtils;

import java.io.IOException;
import java.util.*;

public class Firestore extends Database {
    public static void init() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);
        db = FirestoreClient.getFirestore();
    }

    public User createUser(String displayName, String username, String password) throws Exception {
        // Check if user exists
        Query query = db.collection("users").whereEqualTo("username", username);
        if (query.get().get().getDocuments().size() > 0) throw new Exception("Username exists.");

        // Create the user
        try {
            Map<String, Object> userData = new HashMap<>();
            userData.put("displayName", displayName);
            userData.put("username", username);
            userData.put("password", password);
            userData.put("friendIds", new ArrayList<>());
            userData.put("chatroomIds", new ArrayList<>());
            ApiFuture<WriteResult> future = db.collection("users").document(username).set(userData);
            future.get();
            return new User(username, displayName, password, new ArrayList<>(), new ArrayList<>());
        } catch (Exception e) {
            throw new Exception("Create user error.");
        }
    }

    // For login
    public User getUser(String username, String password) throws Exception {
        try {
            Query query = db.collection("users")
                    .whereEqualTo("username", username)
                    .whereEqualTo("password", password);
            QuerySnapshot querySnapshot = query.get().get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            if (documents.size() > 0) {
                QueryDocumentSnapshot doc = documents.get(0);
                String displayName = doc.getString("displayName");
                List<String> friendIds = (List<String>) doc.get("friendIds");
                List<String> chatroomIds = (List<String>) doc.get("chatroomIds");
                return new User(username, displayName, password, friendIds, chatroomIds);
            }
        } catch (Exception e) {
            throw new Exception("Login error.");
        }

        // User not found
        throw new Exception("Username or password incorrect.");
    }

    public User getUser(String username) throws Exception {
        try {
            Query query = db.collection("users").whereEqualTo("username", username);
            QuerySnapshot querySnapshot = query.get().get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            if (documents.size() > 0) {
                QueryDocumentSnapshot doc = documents.get(0);
                String password = doc.getString("password");
                String displayName = doc.getString("displayName");
                List<String> friendIds = (List<String>) doc.get("friendIds");
                List<String> chatroomIds = (List<String>) doc.get("chatroomIds");
                return new User(username, displayName, password, friendIds, chatroomIds);
            }
        } catch (Exception e) {
            throw new Exception("Get user error.");
        }

        // User not found
        throw new Exception("User not found.");
    }

    public List<Friend> getFriends(List<String> ids) throws Exception {
        try {
            List<Friend> friends = new ArrayList<>();
            if (ids.size() <= 0) return friends;
            Query query = db.collection("users").whereIn("username", ids);
            QuerySnapshot querySnapshot = query.get().get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            for (QueryDocumentSnapshot doc : documents) {
                String displayName = doc.getString("displayName");
                String username = doc.getString("username");
                friends.add(new Friend(username, displayName));
            }
            return friends;
        } catch (Exception e) {
            throw new Exception("Get friend list error.");
        }
    }

    public void updateUser(User user) throws Exception {
        ApiFuture<WriteResult> result = db.collection("users").document(user.username).set(user);
        result.get();
    }

    public Chatroom createChatroom(String chatroomName, String username) throws Exception {
        try {
            Map<String, Object> chatroomData = new HashMap<>();
            List<String> usernames = Collections.singletonList(username);
            String id = UUID.randomUUID().toString().substring(0, 4) + UUID.randomUUID().toString().substring(9, 12);
            chatroomData.put("id", id);
            chatroomData.put("name", chatroomName);
            chatroomData.put("usernames", usernames);
            ApiFuture<WriteResult> future = db.collection("chatrooms").document(id).set(chatroomData);
            future.get();
            return new Chatroom(id, chatroomName, usernames);
        } catch (Exception e) {
            throw new Exception("Create chatroom error.");
        }
    }

    public Chatroom getChatroom(String id) throws Exception {
        try {
            Query query = db.collection("chatrooms").whereEqualTo("id", id);
            QuerySnapshot querySnapshot = query.get().get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            if (documents.size() > 0) {
                QueryDocumentSnapshot doc = documents.get(0);
                String name = doc.getString("name");
                List<String> usernames = (List<String>) doc.get("usernames");
                return new Chatroom(id, name, usernames);
            }
        } catch (Exception e) {
            throw new Exception("Get chat room error.");
        }

        // User not found
        throw new Exception("Chat room not found.");
    }

    public void updateChatroom(Chatroom chatroom) throws Exception {
        ApiFuture<WriteResult> result = db.collection("chatrooms").document(chatroom.id).set(chatroom);
        result.get();
    }

    public List<Chatroom> getChatrooms(List<String> ids) throws Exception {
        try {
            List<Chatroom> chatrooms = new ArrayList<>();
            if (ids.size() <= 0) return chatrooms;
            Query query = db.collection("chatrooms").whereIn("id", ids);
            QuerySnapshot querySnapshot = query.get().get();
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            for (QueryDocumentSnapshot doc : documents) {
                String id = doc.getString("id");
                String name = doc.getString("name");
                List<String> usernames = (List<String>) doc.get("usernames");
                chatrooms.add(new Chatroom(id, name, usernames));
            }
            return chatrooms;
        } catch (Exception e) {
            throw new Exception("Get chatroom list error.");
        }
    }

    public void appendChatMessage(String chatroomId, ChatMessage message) throws Exception {
        try{
            Query query = db.collection("chatHistories")
                    .whereEqualTo("chatroomId", chatroomId)
                    .limit(1).orderBy("timestamp", Query.Direction.DESCENDING);
            QuerySnapshot querySnapshot = query.get().get();
            List<QueryDocumentSnapshot> docs = querySnapshot.getDocuments();
            if(docs.size() > 0) {
                QueryDocumentSnapshot doc = docs.get(0);
                String id = doc.getString("id");
                List<Map<String, Object>> messages = (List<Map<String, Object>>) doc.get("messages");
                if(messages.size() > 25) {
                    // History is too long, create a new one
                    createChatHistory(chatroomId, Collections.singletonList(message));
                }
                else {
                    // Append it to the last entry
                    messages.add(JsonUtils.objToMap(message));
                    ApiFuture<WriteResult> result = db.collection("chatHistories").document(id).set(
                            Collections.singletonMap("messages", messages), SetOptions.merge()
                    );
                    result.get();
                }
                return;
            }
        }
        catch (Exception e) {
            throw new Exception("Append chat message error.");
        }
        throw new Exception("Append chat message error.");
    }

    public ChatHistory getChatHistories(String chatroomId, int limit) throws Exception {
        try {
            Query query = db.collection("chatHistories")
                    .whereEqualTo("chatroomId", chatroomId)
                    .limit(limit).orderBy("timestamp", Query.Direction.DESCENDING);
            QuerySnapshot querySnapshot = query.get().get();
            List<QueryDocumentSnapshot> docs = querySnapshot.getDocuments();

            boolean isLast = false;
            List<ChatMessage> messages = new ArrayList<>();
            for (QueryDocumentSnapshot doc : docs) {
                List<Map<String, Object>> rawMessages = (List<Map<String, Object>>) doc.get("messages");
                assert rawMessages != null;
                for(Map<String, Object> message : rawMessages) {
                    String messageId = (String) message.get("id");
                    String type = (String) message.get("type");
                    String content = (String) message.get("content");
                    messages.add(ChatMessageFactory.parse(messageId, type, content));
                }

                boolean historyIsLast = doc.getBoolean("isLast");
                if(historyIsLast) {
                    isLast = true;
                }
            }

            if(messages.size() == 0) {
                isLast = true;
            }
            return new ChatHistory(messages, isLast);
        } catch (Exception e) {
            throw new Exception("Get chat history error.");
        }
    }

    private void createChatHistory(String chatroomId, List<ChatMessage> messages) throws Exception {
        try {
            Map<String, Object> chatHistoryData = new HashMap<>();
            String id = UUID.randomUUID().toString();
            chatHistoryData.put("id", id);
            chatHistoryData.put("isLast", true);
            chatHistoryData.put("timestamp", TimeUtils.getCurrentTimeString());
            chatHistoryData.put("chatroomId", chatroomId);
            chatHistoryData.put("messages", messages.stream().map(JsonUtils::objToMap));
            ApiFuture<WriteResult> future = db.collection("chatrooms").document(id).set(chatHistoryData);
            future.get();
        } catch (Exception e) {
            throw new Exception("Create chatroom error.");
        }
    }

    private static com.google.cloud.firestore.Firestore db;
}
