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
  $('.main-query-full-screen').click changeFullScreen

  $('#main-query-delete').on 'click', (e) ->
    e.preventDefault()
    deleteQuery $('#main-query-select').val()



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
  else if type == "execute"
    compiled = compileQuery()
    console.log "compiled: " + compiled
    if compiled
      call = jsRoutes.controllers.gmql.QueryMan.runQuery(fileName, outputType)
      $.ajax
        url: call.url
        type: call.type
        method: call.method
        dataType: "json"
        contentType: "text/plain"
        headers: {"X-AUTH-TOKEN": window.authToken}
        data: code
        success: (result, textStatus, jqXHR) ->
          job = result
          jobId = job.id
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
  else
    compileQuery()


compileQuery = () ->
  editor = ace.edit("main-query-editor");
  code = editor.getValue();
  compiled = false
  call = jsRoutes.controllers.gmql.QueryMan.compileQuery()
  $.ajax
    async: false
    url: call.url
    type: call.type
    method: call.method
    dataType: "json"
    contentType: "text/plain"
    headers: {"X-AUTH-TOKEN": window.authToken}
    data: code
    success: (result, textStatus, jqXHR) ->
      job = result
      status = job.status
      $("#query-status").val status
      $("#query-status").show()
      $("#query-stop").hide()
      switch status
        when  "COMPILE_FAILED"
          console.log "COMPILE_FAILED"
          jobLogDialog(job)
        when  "COMPILE_SUCCESS"
          console.log "COMPILE_SUCCESS"
#          jobLogDialog(job)
          compiled = true
        else
          BootstrapDialog.alert "Unknown compile result"
    error: (jqXHR, textStatus, errorThrown) ->
      console.log("error:" + "runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)
      displayError("runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)
  compiled


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
          result: result
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
      jobLogDialog(result)

jobLogDialog = (job) ->
  console.log job

  BootstrapDialog.show
    size: BootstrapDialog.SIZE_WIDE
    title: (dialog) ->
      'Query log' + job.id
    message: (dialog) ->
      divs = $("<div class='form-horizontal'></div>")
      divs.append("<div class='form-group'>
            <label class='col-xs-2 control-label'>Status</label>
            <div class='col-xs-10'>
              <p class='form-control-static'>#{job.status}</p>
            </div>
        </div>")
      divs.append("<div class='form-group'>
            <label class='col-xs-2 control-label'>Message</label>
            <div class='col-xs-10'>
              <p class='form-control-static'>#{job.message}</p>
            </div>
        </div>") unless not job.message # is not null or empty
      divs.append("<div class='form-group'>
            <label class='col-xs-2 control-label'>Dataset names</label>
            <div class='col-xs-10'>
              <p id ='datasetNames' class='form-control-static'></p>
            </div>
        </div>") unless not job.datasets
      divs.find("#datasetNames").html getDatasetList(job.datasets) unless not job.datasets

      divs.append ("<div class='form-group'>
            <label class='col-xs-2 control-label'>Execution time</label>
            <div class='col-xs-10'>
              <p class='form-control-static'>#{msToTime(job.executionTime)}</p>
            </div>
        </div>") unless not job.executionTime
      divs

    onshow: (dialog) ->
      call = jsRoutes.controllers.gmql.QueryMan.getLog(job.id)
      window.test = dialog.getMessage()
      $.ajax
        url: call.url
        type: call.type
        method: call.method
        contentType: "text/plain"
        headers: {"X-AUTH-TOKEN": window.authToken}
        dataType: 'json'
        success: (result, textStatus, jqXHR) ->
          # there are two consecutive paranthesis after get message, because get message returns a function
          dialog.setMessage(dialog.getMessage()().append(result.log.join('<br>')))




loadQueries = ->
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
  $('.main-query-full-screen span').toggleClass('glyphicon-resize-full glyphicon-resize-small');
  ace.edit("main-query-editor").resize()


lastJobStop = ->
  jobId = window.lastJobId
  stopJob(jobId)


deleteQuery = (queryName) ->
  console.log "pre-delete ->" + queryName
  if queryName
    console.log "delete ->" + queryName
    call = jsRoutes.controllers.gmql.QueryBrowser.deleteQuery(queryName)
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {"X-AUTH-TOKEN": window.authToken}
      success: (result, textStatus, jqXHR) ->
        loadQueries()
      error: (jqXHR, textStatus, errorThrown)->
        console.log("error checkLastJob:" + "runQuery: " + jqXHR + "&" + textStatus + "&" + errorThrown)
        loadQueries()
    console.log "after delete ->" + queryName


$ ->
  $('#popover-anchor-query').popover html: true
  popover = $('#popover-anchor-query').popover()
  popover.on 'inserted.bs.popover', ->
    instance = $(this).data('bs.popover')
    # Replace the popover's content element with the 'content' element
    slider = new Slider('#query-size', {})
    editor = ace.edit("main-query-editor")
    slider.setValue([editor.getOption("minLines"),editor.getOption("maxLines")])
    #    slider.setValues(editor.getOption("minLines"),editor.getOption("maxLines"))
    slider.on 'change', (event) ->
      editor = ace.edit("main-query-editor")
      n = event.newValue
      o = event.oldValue
#      console.log event
      editor.setOption("minLines", n[0]) if o[0] != n[0]
      editor.setOption("maxLines", n[1]) if o[1] != n[1]
