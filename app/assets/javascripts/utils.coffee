unless String::trim then String::trim = -> @replace /^\s+|\s+$/g, ""

separator = "---------------------------------------------------------------<br>"


@displayInfo = (info) ->
  $("#info").prepend $("<p>").text(info)
  console.log(info)


@displayError = (error) ->
  $("#info").prepend $("<p>").text(error).css("color", "red")
  console.error(error)

@htmlSpecialChars = (unsafe) ->
  unsafe
  .replace(/&/g, "&amp;")
  .replace(/</g, "&lt;")
  .replace(/>/g, "&gt;")
  .replace(/"/g, "&quot;")


@ajaxCall = (call, requestDivId, responseDivId, resultDivId, input, contentType, isBinary, onComplete) ->
  printRequest(call, input, contentType, requestDivId)
  dataType = 'binary' if isBinary
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: contentType
    data: input
    dataType: dataType
    success: (result, textStatus, jqXHR) ->
      printResponse(textStatus, jqXHR, responseDivId, isBinary)
      if isBinary
        downloadResult(result, jqXHR)
      else
        printResult(result, resultDivId, jqXHR.getResponseHeader("content-type"))
    error: (jqXHR, textStatus, errorThrown) ->
      $("##{responseDivId}").append "Error: #{jqXHR.status} & #{jqXHR} & #{textStatus} & #{errorThrown} <br>"
    complete: (jqXHR, textStatus) ->
#      requestDiv.append separator
      onComplete?(jqXHR.responseText)

urlBeaker = (input) ->
  input.replace /\//g, "/<wbr>"

  
printRequest = (call, input, contentType, requestDivId) ->
  requestDiv = $("##{requestDivId}")
  requestDiv.empty()
  #  requestDiv.append separator
  requestDiv.append "<b>Request url:</b> #{urlBeaker(call.url)} <br>"
  requestDiv.append "<b>Request type:</b> #{call.type} <br>"
  requestDiv.append "<b>Request method:</b> #{call.method} <br>"
  requestDiv.append "<b>Headers:</b> {'X-AUTH-TOKEN': #{window.authToken}} <br>"
  requestDiv.append "<b>Content type:</b> #{contentType} <br>" if contentType?
#  requestDiv.append "<b>Request data:</b> #{input} <br>" if input?
#  requestDiv.append separator

printResponse = (textStatus, jqXHR, responseDivId, isBinary) ->
  responseDiv = $("##{responseDivId}")
  responseDiv.append "<b>Response Status:</b> #{textStatus} <br>"
  responseDiv.append "<b>HTTP Status:</b> #{jqXHR.statusText} (#{jqXHR.status}) <br>"
#  responseDiv.append "<b>Response text:</b> #{htmlSpecialChars(jqXHR.responseText)} <br>" if not isBinary


printResult = (result, responseDivId, contentType) ->
  if result?
    if /xml/i.test contentType
      editorOption(ace.edit(responseDivId), new XMLSerializer().serializeToString(result.documentElement), "xml")
    else if /json/i.test contentType
      editorOption(ace.edit(responseDivId), JSON.stringify(result, null, "\t"), "json")
    else
      editorOption(ace.edit(responseDivId), result)


@downloadResult = (result, jqXHR) ->
  if window.URL
    contentDisposition = jqXHR.getResponseHeader('Content-Disposition')
    filename = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition)[1]
    url = window.URL.createObjectURL(result)
    $('<a/>', {'href': url, 'download': filename, 'text': "click"}).hide().appendTo("body")[0].click()
    window.URL.revokeObjectURL(url)
#  else
#    window.open("http://www.w3schools.com")


# returns splitted clickable datasets list. When it is clicked, it selected the related DS from tree
@getDatasetList = (datasets) ->
  result = $("<div>")
  if datasets
    for dataset in datasets
      a = $("<a href='#' data-ds-name='#{dataset.name}'>#{dataset.name}</a>")
      a.click -> selectNode($(this).data('dsName'))
      a.attr("data-toggle", "tooltip")
      .attr("data-placement", "bottom")
      .attr("title", "Click to select on dataset tree")
      a.tooltip()
      result.append a
      result.append "<br>"
  result


selectNode = (title) ->
  node  = $("#tree").fancytree("getTree").findFirst(title)
  if(node?.title == title )
    BootstrapDialog.getDialog("showJobs")?.close()
    node.setActive()
  else
    BootstrapDialog.alert("There is no dataset available in the tree")

@msToTime = (s) ->
  ms = s % 1000
  # Pad to 2 or 3 digits, default is 2

  pad = (n, z) ->
    z = z or 2
    ('00' + n).slice -z

  s = (s - ms) / 1000
  secs = s % 60
  s = (s - secs) / 60
  mins = s % 60
  hrs = (s - mins) / 60
  pad(hrs) + ':' + pad(mins) + ':' + pad(secs) + '.' + pad(ms, 3)
