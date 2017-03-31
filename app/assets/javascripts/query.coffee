$ ->
  $("#query-execute-button").click ->
    console.log '$("#query-execute-button").click'
    saveQuery("execute")
  $("#query-compile-button").click ->
    console.log '$("#query-compile-button").click'
    saveQuery("compile")
  $('#query-status').click ->
    lastJobLog()
  $("#query-status").hide()

  $('#query-stop').click ->
    lastJobStop()
  $("#query-stop").hide()
  $("#query-stop").tooltip()

#  $('#main-query-select').change selectQuery()
  $('#main-query-select').change selectQuery

  loadQueries()
  $('#main-query-full-screen').click changeFullScreen


saveQuery = (type) ->
  editor = ace.edit("main-query-editor");
  code = editor.getValue();

  fileName = $('#file-name').val()
  if(fileName == "")
    BootstrapDialog.alert "Filename cannot be empty"
  else
    call = jsRoutes.controllers.gmql.QueryBrowser.saveQuery("#{fileName}")
    $("#result_pane").append "\n" + call.url + "\n"
    console.log(code)
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      contentType: "text/plain"
      headers: {"X-AUTH-TOKEN": window.authToken}
      data: code
      success: (result, textStatus, jqXHR) ->
        console.log(" saveQuery result:" + result)
        $("#result_pane").append "\n" + code + "\n"
        $("#result_pane").append "\n" + result + "\n"
        setTimeout loadQueries, 0
        runQuery(result, type)
      error: (jqXHR, textStatus, errorThrown) ->
        console.log("error:" + "saveQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)
        displayError("saveQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)
        BootstrapDialog.alert jqXHR.responseText

runQuery = (fileKey, type) ->
  outputType = $('input:radio[name=optionsRadios]:checked').val()
#   gtfOutput = outputType == "gtf"
  editor = ace.edit("main-query-editor");
  code = editor.getValue();

  fileName = $('#file-name').val()
  if(fileName == "")
    BootstrapDialog.alert "Filename cannot be empty"
  else
    if type == "execute"
      call = jsRoutes.controllers.gmql.QueryMan.runQuery(fileName, outputType)
    else
      call = jsRoutes.controllers.gmql.QueryMan.compileQuery()
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      dataType: "json"
      contentType: "text/plain"
      headers: {"X-AUTH-TOKEN": window.authToken}
      data: code
      success: (result, textStatus, jqXHR) ->
        jobId = result.job.id
        $("#query-status").show()
        console.log("runQuery jobId:" + jobId)
        #      $("#result_pane").append "\n" + inputExampleScript + "\n"
        $("#result_pane").append "\n" + jobId + "\n"
        window.lastJobId = jobId
        checkLastJob()
        BootstrapDialog.alert("Execution started with ID: #{jobId}") if type == "execute"
      error: (jqXHR, textStatus, errorThrown) ->
        console.log("error:" + "runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)
        displayError("runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)
    #TODO reload only if it is not in the list





@checkLastJob = () ->
  jobId = window.lastJobId
  call = jsRoutes.controllers.gmql.QueryMan.traceJob(jobId)
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'json'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      console.log result
      window.result = result
      status = result.status
      $("#query-status").val status
      switch status
        when "PENDING", "RUNNING", "EXEC_SUCCESS", "COMPILING", "DS_CREATION_RUNNING", "DS_CREATION_SUCCESS"
          setTimeout checkLastJob, 5000
          $("#query-stop").show()
        when  "SUCCESS"
          setTimeout reloadComponents, 1000
#          alert('job finished');
          $("#query-stop").hide()
        when  "COMPILE_SUCCESS"
          $("#query-stop").hide()
        else
          #alert('job error status(TODO SHOW IN A BETTER FORMAT): ' + JSON.stringify(result, null, "\t") )
          jobLog(jobId)
          $("#query-stop").hide()
#          setTimeout(checkLastJob, 1000)
    error: (jqXHR, textStatus, errorThrown)->
      console.log("error checkLastJob:" + "runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)


reloadComponents = ->
  resetPrivate()
  loadQueries()


lastJobLog = () ->
  jobLog(window.lastJobId)

@jobLogOld = (jobId) ->
  console.log jobId
  call = jsRoutes.controllers.gmql.QueryMan.getLog(jobId)
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    contentType: "text/plain"
    headers: {"X-AUTH-TOKEN": window.authToken}
    accept: "json"
    success: (result, textStatus, jqXHR) ->
      console.log result
      window.result = result
      BootstrapDialog.show
        size: BootstrapDialog.SIZE_WIDE
        title: (dialog) ->
          dialog.getData('title')
        message:
          result.jobList.jobs.join('<br>')
        onshown: ->

        data:
          result : result
          title: 'Query log' + jobId

    error: (jqXHR, textStatus, errorThrown)->
      console.log("error checkLastJob:" + "runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)

@jobLog = (jobId) ->
  console.log jobId
  call = jsRoutes.controllers.gmql.QueryMan.traceJob jobId
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'json'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      jobStatus = result
      console.log jobStatus
      BootstrapDialog.show
        size: BootstrapDialog.SIZE_WIDE
        title: (dialog) ->
          dialog.getData('title')
        message:  (dialog) ->
          divs = $("<div class='form-horizontal'>
            <div class='form-group'>
                <label class='col-xs-2 control-label'>Status</label>
                <div class='col-xs-10'>
                  <p class='form-control-static'>#{jobStatus.status}</p>
                </div>
            </div>
            <div class='form-group'>
                <label class='col-xs-2 control-label'>Message</label>
                <div class='col-xs-10'>
                  <p class='form-control-static'>#{jobStatus.message}</p>
                </div>
            </div>
            <div class='form-group'>
                <label class='col-xs-2 control-label'>Dataset names</label>
                <div class='col-xs-10'>
                  <p id ='datasetNames' class='form-control-static'></p>
                </div>
            </div>
            <div class='form-group'>
                <label class='col-xs-2 control-label'>Execution time</label>
                <div class='col-xs-10'>
                  <p class='form-control-static'>#{jobStatus.executionTime.replace /Execution Time: /, ""}</p>
                </div>
            </div>
          </div>")
          divs.find("#datasetNames").html getDatasetList(jobStatus.datasets)
          divs

        onshow: (dialog) ->
          call = jsRoutes.controllers.gmql.QueryMan.getLog(jobId)
          window.test = dialog.getMessage()
          $.ajax
            url: call.url
            type: call.type
            method: call.method
            contentType: "text/plain"
            headers: {"X-AUTH-TOKEN": window.authToken}
            dataType: 'json'
            success: (result, textStatus, jqXHR) ->
              console.log result
              window.result = result
              # there are two consecutive paranthesis after get message, because get message returns a function
              dialog.setMessage(dialog.getMessage()().append(result.log.join('<br>')))
        onshown: (dialog) ->
        onshown: (dialog) ->

        data:
          result : result
          title: 'Query log' + jobId
        data:
          result : result
          title: 'Query log' + jobId





#      jobStatus = result.gmqlJobStatusXML
#      if /compile/i.test(jobStatus.status)
#        cell1.html jobId
#      cell2.html jobStatus.status
#      cell3.html jobStatus.message
#      cell4.html jobStatus.datasetNames.replace /,/, "<br>"
#      execTime = jobStatus.execTime.replace /Execution Time: /, ""
#      if execTime == "Execution Under Progress"
#        execTime = ""
#      cell5.html execTime




loadQueries =  ->
  call = jsRoutes.controllers.gmql.QueryBrowser.getQueries()
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    contentType: "text/plain"
    headers: {"X-AUTH-TOKEN": window.authToken}
    accept: "json"
    success: (result, textStatus, jqXHR) ->
      console.log result
      window.result = result
      select = $('#main-query-select')
      select.empty()
      list = result.queries
      for x in list
        newOption = $("<option/>").text(x.name).val(x.name)
        select.append newOption
      select.selectpicker('refresh')
    error: (jqXHR, textStatus, errorThrown)->
      console.log("error checkLastJob:" + "runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)

selectQuery = () ->
  selected = $(this).val()
  console.log "Query selected value: " + selected

  call = jsRoutes.controllers.gmql.QueryBrowser.getQuery(selected)
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {"X-AUTH-TOKEN": window.authToken}
    contentType: 'json'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      console.log result
      window.result =
      $("#file-name").val result.name
      ace.edit("main-query-editor").session.setValue(result.text);
    error: (jqXHR, textStatus, errorThrown)->
      console.log("error checkLastJob:" + "runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)



changeFullScreen = ->
  $('#col-tree').toggleClass('col-md-4 col-md-0');
  $('#col-query').toggleClass('col-md-8 col-md-12');
#  $('#col-schema').toggleClass('col-md-4 col-md-0');
  $('#main-query-full-screen span').toggleClass('glyphicon-resize-full glyphicon-resize-small');
  ace.edit("main-query-editor").resize()


lastJobStop = ->
  jobId = window.lastJobId
  stopJob(jobId)