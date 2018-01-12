package controllers;

import javax.inject.Inject;

import play.mvc.*;
import play.libs.ws.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;

import play.libs.Json;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * This controller contains an action to handle HTTP requests
 * to the application's home page.
 */
public class HomeController extends Controller implements WSBodyReadables, WSBodyWritables {

    private final WSClient ws;

    private static final String insta_clientId = "971d4f12ad2942348aee13a4b5376bf1";
    private static final String insta_clientSecret = "b4116970352442f2834a7e5251e25060";
    private static final String redirect_uri = "http://localhost:9000/importinstagram";
    private String instagramAuthorizationCode = "https://api.instagram.com/oauth/authorize/?client_id=" + insta_clientId + "&redirect_uri=" + redirect_uri + "&response_type=code&scope=follower_list";
    private String instagramAuthorizationToken = "https://api.instagram.com/oauth/access_token";
    private String instagramSelfUser = "https://api.instagram.com/v1/users/self/?access_token=";
    private String instagramFollow = "https://api.instagram.com/v1/users/self/follows?access_token=";
    private String insta_accessToken = null;
    private String insta_value = null;

    private static final String es_url = "http://localhost:9200";

    @Inject
    public HomeController(WSClient ws) {
        this.ws = ws;
    }

    /**
     * An action that renders an HTML page with a welcome message.
     * The configuration in the <code>routes</code> file means that
     * this method will be called when the application receives a
     * <code>GET</code> request with a path of <code>/</code>.
     */
    public Result index() {
        if(this.insta_accessToken != null) {
            JsonNode resultUser = this.getSelfUser();
            this.saveFollows();
            JsonNode resultFollow = this.getFollows().get("_source");

            System.out.println(resultFollow);

            return ok(views.html.index.render(instagramAuthorizationCode, resultFollow));
        }
        return ok(views.html.index.render(instagramAuthorizationCode, null));
    }

    public JsonNode getFollows() {
        String url = this.es_url + "/instagram/follows/" + this.insta_accessToken;
        CompletionStage<WSResponse> response = ws.url(url).get();

        // Apply Ws Request
        CompletionStage<JsonNode> jsonPromise = response.thenApply(WSResponse::asJson);

        // Return a json when completionStage is finish
        try {
            return jsonPromise.toCompletableFuture().get();
        } catch(Exception e) {
            return null;
        }
    }

    public JsonNode saveFollows() {
        System.out.println(this.insta_accessToken);
        String url = this.es_url + "/instagram/follows/"+this.insta_accessToken;

        JsonNode data = this.getApiInstaFollows();

        CompletionStage<WSResponse> response = ws.url(url).setContentType("application/json").put(data);
        CompletionStage<JsonNode> jsonPromise = response.thenApply(WSResponse::asJson);

        try {
            return jsonPromise.toCompletableFuture().get();
        } catch(Exception e) {
            return null;
        }
    }

    // Get the information of user follows
    public JsonNode getApiInstaFollows() {
        // Create Ws Request asynchronous
        String url = this.instagramFollow + this.insta_accessToken;
        CompletionStage<WSResponse> response = ws.url(url).get();

        // Apply Ws Request
        CompletionStage<JsonNode> jsonPromise = response.thenApply(WSResponse::asJson);

        // Return a json when completionStage is finish
        try {
            return jsonPromise.toCompletableFuture().get();
        } catch(Exception e) {
            return null;
        }
    }

    // Get the information of admin profile
    public JsonNode getSelfUser() {
        // Create Ws Request asynchronous
        String url = this.instagramSelfUser + this.insta_accessToken;
        CompletionStage<WSResponse> response = ws.url(url).get();

        // Apply Ws Request
        CompletionStage<JsonNode> jsonPromise = response.thenApply(WSResponse::asJson);

        // Return a json when completionStage is finish
        try {
            return jsonPromise.toCompletableFuture().get();
        } catch(Exception e) {
            return null;
        }
    }

    // Get the instagram access_token with the code
    public CompletionStage<Result> getInstagramToken() {
        String code = request().getQueryString("code");
        if(code != null) {

            // Create Ws Request asynchronous
            String body = "client_id="+insta_clientId+"&client_secret="+insta_clientSecret+"&grant_type=authorization_code&redirect_uri="+redirect_uri+"&code="+code;
            CompletionStage<WSResponse> response = ws.url(instagramAuthorizationToken).setContentType("application/x-www-form-urlencoded")
                    .post(body);

            // Apply and return the ws request
            return response.thenApply((WSResponse resp) -> {
                this.insta_accessToken = resp.asJson().get("access_token").toString().replaceAll("\"","");
                return redirect(controllers.routes.HomeController.index());
            });
        }
        return null;
    }

}
