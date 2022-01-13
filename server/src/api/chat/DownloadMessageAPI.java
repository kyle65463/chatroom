package api.chat;

import api.API;
import database.Database;
import http.HttpRequest;
import http.HttpSender;
import utils.JsonUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DownloadMessageAPI extends API {
    @Override
    public String getPath() {
        return "/chat/download";
    }

    // Should only be called by other threads
    @Override
    public void handle(HttpRequest request, HttpSender sender, Database database) {
        // Parse request
        Map<String, Object> body = request.body;
        String chatroomId = (String) body.get("id");
        String messageId = (String) body.get("messageId");
        String type = (String) body.get("type");
        if (chatroomId == null || messageId == null || type == null) {
            sender.response(400, "Incorrect request format.");
            return;
        }

        try {
            byte[] file = database.downloadFile(type, messageId);
            String fileStr = Base64.getEncoder().encodeToString(file);
            Map<String, Object> output = new HashMap<>();
            output.put("file", fileStr);
            sender.response(200, JsonUtils.toJson(output));
        } catch (Exception e) {
            Map<String, String> output = new HashMap<>();
            output.put("error", e.getMessage());
            sender.response(400, JsonUtils.toJson(output));
        }
    }
}