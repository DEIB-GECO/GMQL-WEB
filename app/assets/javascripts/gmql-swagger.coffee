#$( "input[name='X-AUTH-TOKEN']" ).val(window.authToken)
$ ->
  $('#swagger-token').val  window.authToken
  console.log window.authToken
  if(window.authToken?)
    $("#swagger-guest-button").attr('disabled', true)
#    swaggerUserName(window.fullName)

  $("#swagger-guest-button").click ->
    swaggerGuestLogin()
  $("#swagger-copy-button").click ->
    swaggerSetAllTokens()



swaggerGuestLogin =  ->
  doGuestLogin()
  $('#swagger-token').val = window.authToken

swaggerSetAllTokens =  ->
  $( "input[name='X-AUTH-TOKEN']").val($('#swagger-token').val())

@swaggerUserName = (fullName) ->
  $('#swagger-user').val fullName
