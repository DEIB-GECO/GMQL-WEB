$ ->
  $('#jobs-button').click showLogModal


@showLogModal = ->
  console.log "showLogModal"
  BootstrapDialog.show
    id: "showJobs"
    draggable: true
    closeByBackdrop: false
#    closeByKeyboard: false
    size: BootstrapDialog.SIZE_WIDE
    title: 'User jobs '
    message: (dialog) ->
      $message = insertTable(dialog)
      $message
#    data:
#      title: 'User jobs '

#    buttons: [
#      {
#        label: 'Reload'
#        icon: 'glyphicon glyphicon-refresh'
#        action: (dialog) ->
#          dialog.setMessage insertTable(dialog)
#          return
#
#      }
#    ]

insertTable = (dialog) ->
  div = $("<div id='log-status-div'  class='table-responsive'>")
  div.append table = $("
            <table id='log-status-table' class='table table-striped'>
                <thead>
                    <tr>
                        <th>Job ID</th>
                        <th>Status</th>
                        <th>Message</th>
                        <th>Data set names</th>
                        <th>Elapsed time</th>
                    </tr>
                </thead>
            </table>
       ")
  table.append tbody = $("<tbody>")
  setTimeout (-> loadTable(dialog,tbody)), 0
  div




loadTable = (dialog,tbody) ->
  call = jsRoutes.controllers.gmql.QueryMan.getJobsV2()
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-Auth-Token': window.authToken}
    contentType: 'application/json'
    success: (result, textStatus, jqXHR) ->
      console.log "showLogModal-success"
      window.lastResult = result
      console.log JSON.stringify(result, null, "\t")
      #      list = result.valueList.value
      #      for x in list
      #        newOption = $("<option/>").text(x.name)
      #        select.append newOption
      #      select.selectpicker({title: "Choose value"}).selectpicker('refresh')

      jobs = result.jobList.jobs
      length = jobs.length

      if length
        for jobId in jobs
          a = $("<a href='#' data-job-id='#{jobId}'>#{jobId}</a>")
          a.click ->
            jobLog($(this).data('jobId'))

          cell1 = $("<td ></td>")
          cell1.append a
          cell2 = $("<td ></td>")#.html jobId + " status"
          cell3 = $("<td ></td>")#.html jobId + " Message"
          cell4 = $("<td ></td>")#.html jobId + " ds"
          cell5 = $("<td ></td>")#.html jobId + " ET"
          newRow = $("<tr></tr>").append cell1, cell2, cell3, cell4, cell5
          tbody.append newRow
          fillTable(jobId,cell1, cell2, cell3, cell4, cell5, true)
      else
        dialog.setMessage "No job"
    error: (jqXHR, textStatus, errorThrown) ->
      console.log "showLogModal-error"
      console.log "Error: #{jqXHR.status} & #{jqXHR} & #{textStatus} & #{errorThrown}"
#      TODO if there are no jobs then return empty list
      dialog.setMessage "No job"



fillTable = (jobId,cell1, cell2, cell3, cell4, cell5, firstTime) ->
  call = jsRoutes.controllers.gmql.QueryMan.traceJobV2 jobId
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-Auth-Token': window.authToken}
    contentType: 'application/json'
    success: (result, textStatus, jqXHR) ->
      jobStatus = result.gmqlJobStatusXML
#      if /compile/i.test(jobStatus.status)
#        cell1.html jobId
      cell2.html jobStatus.status
      cell3.html jobStatus.message

      cell4.html ""
      for dsName in jobStatus.datasetNames.split ","
        a = $("<a href='#' data-ds-name='#{dsName}'>#{dsName}</a>")
        a.click -> selectNode($(this).data('dsName'))
        a.attr("data-toggle", "tooltip")
        .attr("data-placement", "bottom")
        .attr("title", "Click to select on tree")
        a.tooltip()
        cell4.append a
        cell4.append "<br>"

      execTime = jobStatus.execTime.replace /Execution Time: /, ""
      if execTime == "Execution Under Progress"
        execTime = ""
      cell5.html execTime
#      console.log "cell1"
#      console.log cell1
      window.cell1 = cell1
      switch jobStatus.status
        when "PENDING", "RUNNING", "EXEC_SUCCESS", "COMPILING", "DS_CREATION_RUNNING", "DS_CREATION_SUCCESS"
          console.log "len " + $("[data-job-id='#{jobId}']").length
          setTimeout (-> fillTable(jobId, cell1, cell2, cell3, cell4, cell5)), 5000 if(firstTime || $("[data-job-id='#{jobId}']").length > 0)
        when  "SUCCESS", "COMPILE_SUCCESS"
        else

selectNode = (title) ->
  node  = $("#tree").fancytree("getTree").findFirst(title)
  if(node?.title == title )
    BootstrapDialog.getDialog("showJobs")?.close()
    node.setActive()