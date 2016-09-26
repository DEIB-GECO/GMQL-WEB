//package controllers
//
//import models.User
//import play.mvc.Result
//
///**
//  * Created by canakoglu on 6/14/16.
//  */
//class SecuredScala extends controller {
//  override def getUsername(ctx: Http.Context): String = {
//    val authTokenHeaderValues: Array[String] = ctx.request.headers.get(SecurityControllerScala.AUTH_TOKEN_HEADER)
//    if ((authTokenHeaderValues != null) && (authTokenHeaderValues.length == 1) && (authTokenHeaderValues(0) != null)) {
//      val user: User = models.User.findByAuthToken(authTokenHeaderValues(0))
//      if (user != null) {
//        ctx.args.put("user", user)
//        return user.getEmailAddress
//      }
//    }
//    return null
//  }
//
//  override def onUnauthorized(ctx: Http.Context): Result = {
//    return unauthorized
//  }
//}
