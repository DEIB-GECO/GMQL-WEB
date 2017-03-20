$ -> init()

init = () ->
  $("#password-change-button").click ->
    updatePassword()


updatePassword = () ->
  console.log("updatePassword.click")
  call = jsRoutes.controllers.SecurityController.updatePassword()
#   console.log $("#recovery-password").val()
#   console.log $("#recovery-password-confirmation").val()
  if($("#recovery-password").val() != $("#recovery-password-confirmation").val())
    BootstrapDialog.alert("Passwords are not same")
  else
    input = JSON.stringify(
      password: $("#recovery-password").val()
    , null, "\t"
    )
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {"X-AUTH-TOKEN": window.authToken}
      data: input
      contentType: 'application/json'
      success: (result, textStatus, jqXHR) ->
#        BootstrapDialog.alert "JSOK " + result
        window.location = jsRoutes.controllers.Application.gmql().url
        Cookies.remove('authToken')
      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert jqXHR.responseText
