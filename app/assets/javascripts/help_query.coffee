inputScript =
  "HM = SELECT(assembly == \"hg19\") GUEST_TEST_DS;
  \nMATERIALIZE HM into hm4;"


dataSetName = $("#data-set-name").val()
newDataSetName = $("#new-data-set-name").val()

$ -> initQuery()

initQuery = () ->
  console.log("init-query")
  editorOption(ace.edit("query-save-input"), inputScript, "gmql")


$("#query-save").click ->
  console.log("query-save.clicked")
  call = jsRoutes.controllers.gmql.QueryMan.saveQueryAs("fileName2.gmql", null)
  ajaxCall(call, "query-save-request", "query-save-response", "query-save-result", inputScript, "text/plain", false, runQuery)

  $("#query-save-header").text "Save result"


runQuery = (fileKey) ->
  console.log("runQuery")
  call = jsRoutes.controllers.gmql.QueryMan.runQueryV2File(fileKey, true, "spark")
  ajaxCall(call, "query-run-request", "query-run-response", "query-run-result", null, null, null, ((result) -> window.lastJobId = result))

  $("#query-run-header").text "Run result"


$("#query-status").click ->
  console.log("query-status.clicked")
  call = jsRoutes.controllers.gmql.QueryMan.traceJobV2(window.lastJobId)
  ajaxCall(call, "query-status-request", "query-status-response", "query-status-result")


$("#query-log").click ->
  console.log("query-log.clicked")
  call = jsRoutes.controllers.gmql.QueryMan.getLog(window.lastJobId)
  ajaxCall(call, "query-log-request", "query-log-response", "query-log-result")

