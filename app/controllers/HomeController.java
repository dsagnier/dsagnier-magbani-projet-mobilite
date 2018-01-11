package controllers;

import javax.inject.Inject;

import play.mvc.*;
import play.libs.ws.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

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
    private String instagramAuthorizationCode = "https://api.instagram.com/oauth/authorize/?client_id=" + insta_clientId + "&redirect_uri=" + redirect_uri + "&response_type=code";
    private String instagramAuthorizationToken = "https://api.instagram.com/oauth/access_token";
    private String instagramSelfUser = "https://api.instagram.com/v1/users/self/?access_token=";
    private String insta_accessToken = null;
    private String insta_value = null;

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
            return ok("accessToken valid");
        }
        return ok(views.html.index.render(instagramAuthorizationCode));
    }

    public CompletionStage<Result> testAction() {
        String url = this.instagramSelfUser + this.insta_accessToken.replaceAll("\"","");

        System.out.println(url);
        CompletionStage<WSResponse> response = ws.url(url).get();

        return response.thenApply((WSResponse resp) -> {
            this.insta_value = resp.asJson().toString();

            return ok(this.insta_value);
        });

        //return CompletableFuture.completedFuture(ok(this.insta_value));
    }

    public CompletionStage<Result> getInstagramToken() {
        String code = request().getQueryString("code");
        if(code != null) {

            // String body
            String body = "client_id="+insta_clientId+"&client_secret="+insta_clientSecret+"&grant_type=authorization_code&redirect_uri="+redirect_uri+"&code="+code;

            CompletionStage<WSResponse> response = ws.url(instagramAuthorizationToken).setContentType("application/x-www-form-urlencoded")
                    .post(body);

            return response.thenApply((WSResponse resp) -> {
                this.insta_accessToken = resp.asJson().get("access_token").toString();
                return redirect(controllers.routes.HomeController.testAction());
            });
        }
        return null;
    }

}
