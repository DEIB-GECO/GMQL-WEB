//package controllers;
//
//
//import play.data.Form;
//import play.mvc.Controller;
//import play.mvc.Result;
//import play.mvc.Security;
//
//import static play.libs.Json.toJson;
//
//@Security.Authenticated(Secured.class)
//public class TodoController extends Controller {
//
//
//    public Result getAllTodos() {
//        System.out.println("asd");
//        return ok(toJson(models.Todo.findByUser(SecurityController.getUser())));
//    }
//
//    public Result createTodo() {
//        Form<models.Todo> form = Form.form(models.Todo.class).bindFromRequest();
//        if (form.hasErrors()) {
//            return badRequest(form.errorsAsJson());
//        }
//        else {
//            models.Todo todo = form.get();
//            todo.user = SecurityController.getUser();
//            todo.save();
//            return ok(toJson(todo));
//        }
//    }
//
//}
