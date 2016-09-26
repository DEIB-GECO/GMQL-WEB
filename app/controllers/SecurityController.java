/*
package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import gql.services.rest.DataSetsManager;
import models.User;
import orchestrator.services.GQLServiceException;
import play.data.Form;
import play.data.validation.Constraints;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;

import java.security.InvalidKeyException;
import java.util.concurrent.atomic.AtomicInteger;

public class SecurityController extends Controller {


    public final static String AUTH_TOKEN_HEADER = "X-Auth-Token";
    public static final String AUTH_TOKEN = "authToken";


    public static User getUser() {
        return (User) Http.Context.current().args.get("user");
    }

    // returns an authToken
    public Result login() {
        Form<Login> loginForm = Form.form(Login.class).bindFromRequest();

        if (loginForm.hasErrors()) {
            return badRequest(loginForm.errorsAsJson());
        }

        Login login = loginForm.get();

        User user = User.findByUsernameAndPassword(login.username, login.password);

        if (user == null) {
            return unauthorized();
        } else {
            String authToken = user.createToken();
            ObjectNode authTokenJson = Json.newObject();
            authTokenJson.put(AUTH_TOKEN, authToken);
            response().setCookie(AUTH_TOKEN, authToken);
            return ok(authTokenJson);
        }
    }

    //TODO: save guest counter in the database
    private static final AtomicInteger guestCounter = new AtomicInteger();

    public Result loginGuest() throws GQLServiceException, InvalidKeyException {
        String username = "guest" + guestCounter.incrementAndGet();
//        username = "canakoglu";
        while (User.existsByUsername(username)) {
            username = "guest" + guestCounter.incrementAndGet();
        }
        User user = new User(username, username + "@demo.com", "password", "John Doe");
        user.save();

        new DataSetsManager().registerUser(username);

        String authToken = user.createToken();
//        //TODO CORRECT
//        authToken = "test-best-token";
        ObjectNode authTokenJson = Json.newObject();
        authTokenJson.put(AUTH_TOKEN, authToken);
        authTokenJson.put("username", username);
        response().setCookie(AUTH_TOKEN, authToken, Integer.MAX_VALUE);
        return ok(authTokenJson);

    }

    @Security.Authenticated(Secured.class)
    public Result logout() throws GQLServiceException, InvalidKeyException {
//        response().discardCookie(AUTH_TOKEN);
        User user = getUser();
        if (user != null) {
            user.deleteAuthToken();
            if (user.getUsername().startsWith("guest"))
                new DataSetsManager().unRegisterUser(user.getUsername());
            return ok("Logout");
        }else
            return notFound("User not found");
    }

    public static class Login {

        @Constraints.Required
//        @Constraints.Email
        public String username;

        @Constraints.Required
        public String password;

    }

}
*/
