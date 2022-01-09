package actions.chatroomMenu;

import actions.VoidAction;
import http.HttpMessage;
import http.HttpReceiver;
import http.HttpResponse;
import http.HttpSender;
import models.Auth;
import utils.JsonUtils;
import utils.Scanner;

import java.util.HashMap;
import java.util.Map;

public class JoinChatroom extends VoidAction {
    public void perform(Auth auth, HttpSender sender, HttpReceiver receiver) {
        Map<String, String> params = new HashMap<>();
        System.out.println("Enter chat room's id:");
        String id = Scanner.getRequiredData("ID");
        params.put("id", id);
        params.put("username", auth.user.username);
        sender.post("/chatroom/user/add", JsonUtils.toJson(params), auth.authToken);

        try {
            HttpMessage message = receiver.readMessage();
            if(message instanceof HttpResponse response) {
                if(response.status == 200) {
                    System.out.println("Joined successfully");
                    System.out.println("");
                }
                else {
                    // Request failed
                    System.out.println(response.body.get("error"));
                }
            }
        }
        catch (Exception ignored) {
        }
    }
}
