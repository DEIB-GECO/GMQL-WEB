inputFileList =
  schema_file: "http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/HG19_ANN.schema"
  data_files: [
    "http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed",
    "http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/RefSeqGenesExons_hg19.bed.meta",
    "http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed",
    "http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/TSS_hg19.bed.meta",
    "http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed",
    "http://www.bioinformatics.deib.polimi.it/canakoglu/guest_data/VistaEnhancers_hg19.bed.meta"
  ]

inputFileListJson = JSON.stringify(inputFileList, null, "\t")


dataSetName = $("#data-set-name").val()
newDataSetName = $("#new-data-set-name").val()


$ -> initDataSet()

initDataSet = ->
  console.log("init-data-set")
  editorOption(ace.edit("data-set-upload-url-input"), inputFileListJson, "json")


$("#data-set").click ->
  console.log("data-set.clicked")
  call = jsRoutes.controllers.gmql.DSManager.dataSetAll()
  ajaxCall(call, "data-set-request", "data-set-response", "data-set-result")

$("#data-set-sample").click ->
  console.log("data-set-sample.clicked")
  call = jsRoutes.controllers.gmql.DSManager.dataSetSamples(dataSetName)
  ajaxCall(call, "data-set-sample-request", "data-set-sample-response", "data-set-sample-result")


$("#data-set-upload-url").click ->
  console.log("data-set-download-file.clicked")
  console.log inputFileListJson
  call = jsRoutes.controllers.gmql.DSManager.uploadSamplesFromUrls(newDataSetName)
  ajaxCall(call, "data-set-upload-url-request", "data-set-upload-url-response", "data-set-upload-url-result", inputFileListJson, "application/json")


$("#data-set-delete").click ->
  console.log("data-set-sample.clicked")
  call = jsRoutes.controllers.gmql.DSManager.dataSetDeletion(newDataSetName)
  ajaxCall(call, "data-set-delete-request", "data-set-delete-response", "data-set-delete-result")


$("#data-set-zip-file").click ->
  console.log("data-set-zip-file.clicked")
  call = jsRoutes.controllers.gmql.DSManager.zipFilePreparation(newDataSetName, true)
  ajaxCall(call, "data-set-zip-file-request", "data-set-zip-file-response", "data-set-zip-file-result")


$("#data-set-download-file").click ->
  console.log("data-set-download-file.clicked")
  call = jsRoutes.controllers.gmql.DSManager.downloadFileZip(newDataSetName)
  ajaxCall(call, "data-set-download-file-request", "data-set-download-file-response", "data-set-download-file-result", null, null, true)


#  addNewDiv(
#    "data-set",
#    jsRoutes.controllers.gmql.DSManager.dataSetAll()
#  ).appendTo $("#data-set-management")

#addNewDiv = ( groupId, call, input, contentType, isBinary, onComplete) ->
#  divDescription = $('<div/>', {'id': groupId + "-call-description"})
#  divDescription.append "URL: "
#
#
#  button = $('<button/>', {'id': groupId + "-button"})
#  button.html  call.url
#  button.click ->
#    console.log(groupId + ".clicked")
#    ajaxCall(call, groupId + "-request", groupId + "-response", input, contentType, isBinary, onComplete)
#  divInput = $('<div/>', {'id': groupId + "-input"})
#  divRequest = $('<div/>', {'id': groupId + "-request"})
#  divResponse = $('<div/>', {'id': groupId + "-response"})
#
#  div = $('<div/>', {'id': groupId + "-div"})
#  div.append divDescription, button, divInput, divRequest, divResponse
#  div
#
#
#getTable = (groupId) ->
#  tbody = $('<table/>', {'id': groupId + "-table"})
#  trUrl = $('<tr/>', {'id': groupId + "-table-tr-url"})
#  trUrl.append
#  tbody.append trUrl


