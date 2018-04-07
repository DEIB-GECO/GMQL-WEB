updateUserType = (selUsername) ->
  call =  jsRoutes.controllers.gmql.AdminManager.updateType(selUsername)

  newType = $("#usertype_sel_"+selUsername).val()
  console.log "Setting category of user #{selUsername} to #{newType}"

  $.ajax
    url: call.url
    type: call.type
    method: call.method
    contentType: 'text'
    dataType: 'text'
    contentType: 'application/json'
    data: "{\"type\": \"#{newType}\"}"
    success: () ->
      BootstrapDialog.alert("User <i>#{selUsername}</i> is now of type <i>#{newType}</i>.")
    error: (jqXHR) ->
      if jqXHR.status == 403 || jqXHR.status == 401
        window.location = jsRoutes.controllers.Application.gmql().url
      else
        BootstrapDialog.alert("Update failed, please try again."+textStatus)



getActionsHTML = (userTypes, username) ->

  button_id = "usertype_btn_" + username

  "<button id='#{button_id}' class='btn btn-primary #{"disabled" if username==window.user.username}'
           data-username='#{username}' type='button'>Update</button>"

getSelectHTML = (userTypes, username, type) ->

  select_id = "usertype_sel_" + username

  "<select id='#{select_id}' class='form-control'>" +
    (userTypes.map((userType) ->
      "<option #{ 'selected' if userType == type }>#{userType}</option>")
      .reduce (x, y) -> "#{x}\n#{y}") +
  "</select>"


getUsers = (userTypes) ->
  call =  jsRoutes.controllers.gmql.AdminManager.getUsers()

  $.ajax
    url: call.url
    type: call.type
    method: call.method
    contentType: 'text'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->

      registered_users =  result.users.filter( (user) -> user["userType"]!="GUEST")
      guest_count = result.users.length - registered_users.length

      $("#users_description").html("There are currently <b>#{registered_users.length}</b> registered users and <b>#{guest_count}</b> guest users.")

      $('#users_table').DataTable(
        data: registered_users
        columns: [
          {'data': 'username'}
          {'data': 'firstName'}
          {'data': 'lastName'}
          {'data': 'emailAddress'}
          {
            'render' :  ( data, type, full ) -> getSelectHTML(userTypes, full.username, full.userType)
          }
          {
            'render' :  ( data, type, full ) -> getActionsHTML(userTypes, full.username)
          }
        ]

        'displayLength': 25
      )

      # set callback for update buttons
      for selUser in result.users
        if selUser.username!=window.user.username
          $("#usertype_btn_"+selUser.username).click( ()-> updateUserType($(this).attr("data-username")))

    error: (jqXHR) ->
      if jqXHR.status == 403 || jqXHR.status == 401
        window.location = jsRoutes.controllers.Application.gmql().url
      else
        BootstrapDialog.alert "Error loading the list of users."+textStatus



getUserTypes = () ->
  call =  jsRoutes.controllers.gmql.AdminManager.getUserTypes()

  $.ajax
    url: call.url
    type: call.type
    method: call.method
    contentType: 'text'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      getUsers(result.userTypes)

    error: (jqXHR) ->
      if jqXHR.status == 403 || jqXHR.status == 401
        window.location = jsRoutes.controllers.Application.gmql().url
      else
        BootstrapDialog.alert "Error loading the list of users. "+textStatus


$ ->
  addEventListener('logout', () ->  window.location = jsRoutes.controllers.Application.gmql().url)
  getUserTypes()