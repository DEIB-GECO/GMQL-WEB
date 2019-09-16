#document.cookie = 'authToken=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
$ -> init()

getQueryVariable = (variable) ->
  query = window.location.search.substring(1)
  vars = query.split("&")
  i = 0

  while i < vars.length
    pair = vars[i].split("=")
    return pair[1]  if pair[0] is variable
    i++
  false

init = () ->
  console.log "document.cookie: " + document.cookie
  window.authToken = Cookies.get('authToken')
  if (window.authToken?)
    call = jsRoutes.controllers.SecurityController.getUser()
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      dataType: 'json'
      headers: {"X-AUTH-TOKEN": window.authToken}
      success: (result, textStatus, jqXHR) ->
        window.user = result
        displayLoggedIn(result.fullName)
        updateUploadView()

        showExample = getQueryVariable("showExample")
        if(showExample)
          ajaxUrl = jsRoutes.controllers.gmql.GecoQueries.gecoQueriesJson("federated")
          $.ajax
            url: ajaxUrl.url
            type: ajaxUrl.type
            method: ajaxUrl.method
            headers: {'X-AUTH-TOKEN': window.authToken}
            success: (result, textStatus, jqXHR) ->
              console.log("EEEEEXAMPLEEEE"+result)
              query = result.data.filter((x)-> x.name==showExample )[0].query
              console.log("EEEEEXAMPLEEEE"+query)
              ace.edit("main-query-editor").setValue(query)





          ace.edit("main-query-editor").setValue(showExample)
#        $( "input[name='X-AUTH-TOKEN']" ).val(window.authToken)
      error: (jqXHR, textStatus, errorThrown) ->
        displayGuestButton()
        Cookies.remove('authToken')

#        document.cookie = "auth-token" + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT"
  else
    displayGuestButton()
  $("#login-button").click ->
    loginButtonClick()
  $("#register-button").click ->
    registerButtonClick()
  $("#forgot-password").click ->
    forgotPassword()

  info_call = jsRoutes.controllers.gmql.AdminManager.getInfo()
  $.ajax
    url: info_call.url
    type: info_call.type
    method: info_call.method
    dataType: 'json'
    headers: {"X-AUTH-TOKEN": window.authToken}
    success: (result, textStatus, jqXHR) ->
      if ("adminEmail" of result)
        console.log(result)
        $("#menu-items").append("<li id='contact-admin'><a href='mailto:#{result.adminEmail}'>Contact Admin</a></li>")
    error: (jqXHR, textStatus, errorThrown) ->
      console.log("error")


displayGuestButton = () ->
  $("#login").empty()
  $("#login").append $("<button class='btn btn-default navbar-btn'>").text("Guest Login").click(doGuestLogin)
  $("#signInDropdown").show()
  $('.registered-user').remove()
  $('#login-problem').append('
      <div>
        <h4>Please login before using the GMQL.</h4>
        <p>This system is under active development, please forgive us for possible errors and send us your comments, criticisms and congratulations, if any.</p>
        <div id="visit_count"></div>
        <div>GMQL core version: <span id="GMQL-version"></span></div>
        <div>GMQL WEB version: <span id="GMQL-WEB-version"></span></div>
      </div>
  ')
  $.ajax
    url: "assets/GMQL-version.txt"
    success: (result, textStatus, jqXHR) ->
      $("#GMQL-version").append(result)
  $.ajax
    url: "assets/GMQL-WEB-version.txt"
    success: (result, textStatus, jqXHR) ->
      $("#GMQL-WEB-version").append(result)
  makeRequest('/AnalyticsQueryWrapper/AnalyticsServlet')



displayLoggedIn = (fullName) ->
  $("#login").empty()
  displayInfo?("Logged in")
  $("#login").append $("<span>").text("Hello " + fullName + " ") if fullName?
  $("#login").append $("<button class='btn btn-default navbar-btn'>").text("Logout").click(doLogout)
  $("#signInDropdown").hide()
  if window.user.userType == "ADMIN"
    $("#menu-items").append("<li id='admin-page-nav'><a href='#{jsRoutes.controllers.Application.adminPage().url}'>Admin Page</a></li>")
  swaggerUserName? fullName


@doGuestLogin = (event) ->
  call = jsRoutes.controllers.SecurityController.loginGuest()
  #  $.get call.url, {asd}, (data) -> doLogin(data), 'json'

  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {"X-AUTH-TOKEN": window.authToken}
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      doLogin(result)
    error: ->
      BootstrapDialog.alert "Guest users cannot be created on this instance."





doLogin = (data) ->
  token = data.authToken
  window.user = data
  window.authToken = token # global state holder for the auth token
  #  document.cookie = "auth-token" + "=" + token + "; expires=Tue, 19 Jan 2038 03:14:07 UTC"
  #  $.cookie('authToken', token , { expires: 2147483647 });
  Cookies.set('authToken', token, { expires: 365 })
  displayInfo?("LOGIN OK")
  displayLoggedIn(data.fullName)
  $(".login-event").click()
  location.reload()

  updateUploadView()


doLogout = (event) ->
  call = jsRoutes.controllers.SecurityController.logout()
  $.ajax
    url: call.url
    type: call.method
    headers: {"X-AUTH-TOKEN": window.authToken}
    success: (data, textStatus, jqXHR) ->
      displayInfo("logout success")
      #      displayGuestButton()
      $(".login-event").click()
    error: (jqXHR, textStatus, errorThrown) ->
      console.log "document.cookie: " + document.cookie
      displayError("logout failed:" + jqXHR + "&" + textStatus + "&" + errorThrown)
    displayGuestButton()
    Cookies.remove('authToken')
    $("#admin-page-nav").remove()
    dispatchEvent(new Event('logout'))
    window.authToken = undefined
#    document.cookie = "auth-token" + "=;expires=Thu, 01 Jan 1970 00:00:00 GMT"
    console.log "document.cookie: " + document.cookie



loginButtonClick = () ->
  console.log("loginButton.click")
  call = jsRoutes.controllers.SecurityController.login()
  input = JSON.stringify(
    username: $("#usernameInput").val()
    password: $("#passwordInput").val(), null, "\t"
  )
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {"X-AUTH-TOKEN": window.authToken}
    data: input
    dataType: 'json'
    contentType: 'application/json'
    success: (result, textStatus, jqXHR) ->
      doLogin(result)
    error: (jqXHR, textStatus, errorThrown) ->
      BootstrapDialog.alert JSON.parse(jqXHR.responseText).errorString

registerButtonClick = () ->
  console.log("loginButton.click")
  call = jsRoutes.controllers.SecurityController.registerUser()
  if($("#register-password").val() != $("#register-password-confirmation").val())
    BootstrapDialog.alert("Passwords are not same")
  else
    input = JSON.stringify(
      firstName: $("#register-first-name").val()
      lastName: $("#register-last-name").val()
      username: $("#register-username").val()
      email: $("#register-email").val()
      password: $("#register-password").val()
    , null, "\t"
    )
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {"X-AUTH-TOKEN": window.authToken}
      data: input
      dataType: 'json'
      contentType: 'application/json'
      success: (result, textStatus, jqXHR) ->
        doLogin(result)
        $('#register-modal').modal('hide');
      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert jqXHR.responseText


forgotPassword = () ->
  username = $("#usernameInput").val()
  if not username? or username is ''
    BootstrapDialog.alert "Please fill the username field in the login form"
  else
    call = jsRoutes.controllers.SecurityController.passwordRecoveryEmail(username)

    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {"X-AUTH-TOKEN": window.authToken}
      success: (result, textStatus, jqXHR) ->
        BootstrapDialog.alert "Please check your mailbox for the recovery link of the user " + username
      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert "Something wrong with your request\n" + jqXHR.responseText


