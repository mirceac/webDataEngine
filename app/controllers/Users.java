package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.User;
import models.domain.ModelUser;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http.*;
import play.mvc.Result;
import service.UserService;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

public class Users extends Controller {
    @Inject
    UserService userService;

    public Result list() {
        List<? extends User> users = userService.getUsers();
        JsonNode node = Json.toJson(users);
        return ok(node.toString());
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result add() {
        RequestBody body = request().body();
        JsonNode node = body.asJson();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            User user = objectMapper.readValue(node.toString(), ModelUser.class);
            userService.createUser(user.getUserName(), user.getPassword());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ok("created");
    }

}