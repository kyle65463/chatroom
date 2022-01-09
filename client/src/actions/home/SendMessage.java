package actions.home;

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

public class SendMessage extends VoidAction {
    public void perform(Auth auth, HttpSender sender, HttpReceiver receiver) {
        Map<String, String> params = new HashMap<>();
        System.out.println("Enter message:");
        String content = Scanner.getRequiredData("content");
        params.put("id", "123");
        params.put("type", "text");
        params.put("content", content);
        sender.post("/chat/send", JsonUtils.toJson(params), auth.authToken);

        try {
            HttpMessage message = receiver.readMessage();
            if(message instanceof HttpResponse response) {
                if(response.status == 200) {
                    System.out.println("Sent successfully");
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
