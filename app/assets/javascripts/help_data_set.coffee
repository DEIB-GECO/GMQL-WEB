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
  call = jsRoutes.controllers.gmql.DSManager.getDatasets()
  ajaxCall(call, "data-set-request", "data-set-response", "data-set-result")

$("#data-set-sample").click ->
  console.log("data-set-sample.clicked")
  call = jsRoutes.controllers.gmql.DSManager.getSamples(dataSetName)
  ajaxCall(call, "data-set-sample-request", "data-set-sample-response", "data-set-sample-result")


$("#data-set-upload-url").click ->
  console.log("data-set-download-file.clicked")
  console.log inputFileListJson
  call = jsRoutes.controllers.gmql.DSManager.uploadSamplesFromUrls(newDataSetName)
  ajaxCall(call, "data-set-upload-url-request", "data-set-upload-url-response", "data-set-upload-url-result", inputFileListJson, "application/json")


$("#data-set-delete").click ->
  console.log("data-set-sample.clicked")
  call = jsRoutes.controllers.gmql.DSManager.deleteDataset(newDataSetName)
  ajaxCall(call, "data-set-delete-request", "data-set-delete-response", "data-set-delete-result")


$("#data-set-zip-file").click ->
  console.log("data-set-zip-file.clicked")
  call = jsRoutes.controllers.gmql.DSManager.zipFilePreparation(newDataSetName, true)
  ajaxCall(call, "data-set-zip-file-request", "data-set-zip-file-response", "data-set-zip-file-result")


$("#data-set-download-file").click ->
  console.log("data-set-download-file.clicked")
  call = jsRoutes.controllers.gmql.DSManager.downloadFileZip(newDataSetName)
  ajaxCall(call, "data-set-download-file-request", "data-set-download-file-response", "data-set-download-file-result", null, null, true)

