@gmql = "gmql values"
MAX_INT = 2147483647
$ ->
#  $(window).resize ->
#    $("#tree-panel").height $(window).height()

$ ->
  $('[data-toggle="tooltip"]').tooltip()
  $('#button-expand-all').click ->
    $(this).focusout()
    $('#tree').fancytree('getTree').visit (node) ->
      node.setExpanded true
  $('#button-collapse-all').click ->
    $(this).focusout()
    $('#tree').fancytree('getTree').visit (node) ->
      node.setExpanded false
  initTree()
  $("#refresh-tree").click ->
    $(this).focusout()
    initTree()
    console.log("refresh tree")


  $('#button-delete-data-set').click ->
    deleteSelectedNodes()
  $('#button-download-data-set').click ->
    downloadDataset()
  $('#button-ucsc-data-set').click ->
    loadUcsc()

  $('.tree-full-screen').click changeFullScreen
  loadContext()
  loadUsage()

@loadUsage = (formFunction) ->
  memUsage = $('#memory-usage')
  memUsageTooltip = $('#memory-usage-tooltip')
  usageBar = $('.usage-bar')
  call = jsRoutes.controllers.gmql.DSManager.getMemoryUsage()
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'json'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      used_percantage = (item.value  for item in result.infoList when item.key == 'used_percentage')[0]
      quotaExceeded = (item.value  for item in result.infoList when item.key == 'quota_exceeded')[0]
      occupied = (item.value  for item in result.infoList when item.key == 'occupied')[0] / 1000
      total = (item.value  for item in result.infoList when item.key == 'total')[0] / 1000


      if used_percantage > 100
        memUsage.width(100 + "%")
      else
        memUsage.width(used_percantage + "%")

      memUsageTooltip.attr("title", "#{occupied} MB / #{total} MB").tooltip('fixTitle')

      memUsage.text(used_percantage + "%")

      memUsage.removeClass("progress-bar-success")
      memUsage.removeClass("progress-bar-info")
      memUsage.removeClass("progress-bar-warning")
      memUsage.removeClass("progress-bar-danger")
      if quotaExceeded is "true"
        memUsage.addClass("progress-bar-danger")
        if formFunction

          BootstrapDialog.alert "User quota is not enough for the operation"

      else
        if used_percantage > 75
          memUsage.addClass("progress-bar-warning")
        else
        if used_percantage > 50
          memUsage.addClass("progress-bar-info")
        else
          memUsage.addClass("progress-bar-success")
        if formFunction
          formFunction()
      used_percantage
      usageBar.show()
    error: ->
      memUsage.addClass("progress-bar-danger")
      memUsage.width(100 + "%")
      memUsage.text("Data usage NA")
      usageBar.show()

@resetPrivate = ->
  $("#tree").fancytree("getRootNode").children[0].resetLazy()
  expandPrivate()
  loadUsage()

expandPrivate = ->
  $("#tree").fancytree("getRootNode").children[0]?.setExpanded(true)

glyph_opts = map:
  doc: 'glyphicon glyphicon-folder-close'
  docOpen: 'glyphicon glyphicon-folder-open'
  checkbox: 'glyphicon glyphicon-unchecked'
  checkboxSelected: 'glyphicon glyphicon-check'
  checkboxUnknown: 'glyphicon glyphicon-share'
  dragHelper: 'glyphicon glyphicon-play'
  dropMarker: 'glyphicon glyphicon-arrow-right'
  error: 'glyphicon glyphicon-warning-sign'
  expanderClosed: 'glyphicon glyphicon-menu-right'
  expanderLazy: 'glyphicon glyphicon-menu-right'
  expanderOpen: 'glyphicon glyphicon-menu-down'
  folder: 'glyphicon glyphicon-plus'
  folderOpen: 'glyphicon glyphicon-minus'
  loading: 'glyphicon glyphicon-refresh glyphicon-spin'

@initTree = ->
# TODO change with http://wwwendt.de/tech/fancytree/demo/#sample-webservice.html
# Initialize Fancytree
  $('#tree').fancytree
    extensions: [
      'glyph'
    ]
    checkbox: true

    glyph: glyph_opts
    selectMode: 3
    source: [
      {
        title: "Private"
        "folder": true
        "lazy": true
        hideCheckbox: false
        unselectable: false
        type: "main"
        value: "private-data-set"
      }
      {
        title: "Public"
        "folder": true
        "lazy": true
        hideCheckbox: true
        unselectable: true
        type: "main"
        value: "public-data-set"
      }
    ]

    toggleEffect:
      effect: 'drop'
      options:
        direction: 'left'
      duration: 400
    wide:
      Width: '1em'
    iconSpacing: '0.5em'
    levelOfs: '1.5em'
#    I cannot use this icon!!!
#    icon: (event, data) ->
#      console.log("ASD")
#      'glyphicon glyphicon-book'


    lazyLoad: (event, data) ->
      window.lastNode = data.node
      console.log(data)

      dfd = $.Deferred();
      data.result = dfd.promise();
      if /main/.test data.node.data.type
        call = jsRoutes.controllers.gmql.DSManager.getDatasets()
      else
        call = jsRoutes.controllers.gmql.DSManager.getSamples(data.node.data.value)
      console.log {'X-AUTH-TOKEN': window.authToken}
      $.ajax
        url: call.url
        type: call.type
        method: call.method
        headers: {'X-AUTH-TOKEN': window.authToken}
        contentType: 'json'
        dataType: 'json'
        success: (result, textStatus, jqXHR) ->
          if "main" == data.node.data.type
            newType = "data-set"
            lazy = true
            datasets = result.datasets
            if "public-data-set" == data.node.data.value
              console.log "public"
              list = datasets.filter (x) -> x.owner == "public"
            else
              console.log "private"
              list = datasets.filter (x) -> x.owner != "public"
          else
            newType = "sample"
            lazy = false
            window.list = list
            icon = "glyphicon glyphicon-file"
            list = result.samples

          res = for att in list
            hideCheckBox = ("public-data-set" == data.node.data.value) || ("public-data-set" == data.node.parent?.data.value)
            result = (item.value  for item in att?.info?.infoList when item.key == 'Number of samples')
            result = if(result?.length) then result = " (#{result})" else ""
            #TODO title should not be used anymore from the other functions, only key should be used
            result = ""

            temp = {
              key: if newType == "data-set" then att.name.replace /^public\./, "" else att.name.split("/").pop()
              title: if newType == "data-set" then (att.name.replace /^public\./, "") + result else att.name.split("/").pop()
              folder: false
              lazy: lazy
              hideCheckbox: hideCheckBox
              unselectable: hideCheckBox
              type: newType
              value: (if newType == "data-set" && att.owner == "public" then att.owner + "." else "") + att.name
              selected: data.node.selected
            }
            temp.icon = icon if icon
            temp.id = att.id if att.id
            temp
          dfd.resolve(res)
      window.lastNode.setActive()

    select: (event, data) ->
      logEvent event, data, 'current state=' + data.node.isSelected()
      s = data.tree.getSelectedNodes().join(', ')
      $('#echoSelected').text s
      console.log 'select'
      data.node.setActive()

    activate: (event, data) ->
      logEvent event, data
      node = data.node
      window.activeNode = node
      $('#echoActive').text node.title + " val " + data.node.data.value
      setDataSetSchema(node)
      setSampleMetadata(node)
      console.log 'activate'

    beforeActivate: (event, data) ->
      window.beforeActivate = data.node
      console.log 'beforeActivate'
      logEvent event, data
      if generateQuery()
        BootstrapDialog.confirm 'Are sure to change the dataset, query in the metadata browser will be lost?', (result) ->
          if result
#            alert 'Yup.'
            $(".del-button").click()

            data.node.setActive()
          else
#            alert 'Nope.'
        return false


setSampleMetadata = (node) ->
  dataSet = node.parent.data.value
  id = node.data.id
  sampleName = node.data.value
  metadataDiv = $("#metadata-div")
  metadataDiv.empty()

  if(node.data.type == "sample")
#    metadataDiv.append "<h2>Sample metadata #{if node.title?.length then "of " + node.title else "" }</h2>"
    insertMetadataTable(metadataDiv, dataSet, id, sampleName)


setDataSetSchema = (node) ->
  data = node.data
  type = data.type
  value =
    switch type
      when "sample"   then node.parent.data.value
      when "data-set"  then data.value


  if value?
    if window.lastSelectedDataSet != value
      call = jsRoutes.controllers.gmql.DSManager.dataSetSchema(value)
      $.ajax
        url: call.url
        type: call.type
        method: call.method
        headers: {'X-AUTH-TOKEN': window.authToken}
        contentType: 'json'
        dataType: 'json'
        success: (result, textStatus, jqXHR) ->
          $('#echoActive').text node.title + " val " + JSON.stringify(result.schemas, null, "\t")
          window.result = result
          window.lastSelectedDataSet = value
          schemaTables(result)
          clearMetadataDiv()
          if generateQuery?
            generateQuery()
          else
            alert 'generateQuery is not defined'

  else
    clearMetadataDiv()
    generateQuery()
    $('#echoActive').text node.title + " val " + data.value
    #        $("#schema-div").hide()
    $("#schema-div").empty()
    window.lastSelectedDataSet = null


schemaTables = (result)->
  schema = result
  schemaDiv = $("#schema-div")
  #  schemaDiv.show()
  schemaDiv.empty()
  #  schemaDiv.append "<h2>DataSet schemas #{if result.gmqlSchemaCollection.name?.length then "of " + result.gmqlSchemaCollection.name else "" }</h2>"
  schemaDiv.append schemaTable(schema)

schemaTable = (schema) ->
  div = $("<div id='schema-table-div-#{schema.name}'>")
  #  div.append "<h4><b>Schema name:</b> #{schema.name}</h4>" if schema.name?.length
  div.append "<h4><b>Schema type:</b> #{schema.type}</h4>" if schema.type?.length
  div.append "<b>Coordinate system:</b> #{schema.coordinate_system}" if schema.coordinate_system?.length and schema.coordinate_system != 'default'
  div.append table = $("
            <table id='schema-table-#{schema.name}' class='table table-striped table-bordered'>
                <thead>
                    <tr>
                        <th>Field name</th>
                        <th>Field type</th>
                        <th>Heat map</th>
                    </tr>
                </thead>
            </table>")

  table.append tbody = $("<tbody>")
  for x in schema.fields
    newRow = $("<tr>").append($("<td>").text(x.name)).append($("<td>").text(x.type))
    if x.name not in ["seqname", "feature", "start", "end"] and x.type not in ["STRING",
      "CHAR"] and not /^public\./.test window.lastSelectedDataSet
      link = jsRoutes.controllers.gmql.DSManager.parseFiles(window.lastSelectedDataSet, x.name).url
      button = $("<a target='_blank' href='#{link}' class='btn btn-default btn-xs'>View</a>")
      newRow.append($("<td>").append(button))
    else
      newRow.append($("<td>").text(""))
    tbody.append newRow
  div


logEvent = (event, data, msg) ->
#        var args = $.isArray(args) ? args.join(", ") :
  msg = if msg then ': ' + msg else ''
  $.ui.fancytree.info "Event('" + event.type + '\', node=' + data.node + ')' + msg
  console.log "Event('" + event.type + '\', node=' + data.node + ')' + msg


deleteSelectedNodes = ->
  selectedNodes = findSelectedNode()
  window.selectedNode = selectedNodes
  console.log "selectedNode: "
  console.log selectedNodes
  dataSetsDiv = (selectedNodes.data_sets.map (node) -> "<div>#{node.title}</div>").join('')

  samplesDiv = (selectedNodes.samples.map (node) -> "<div>#{node.parent.title} -> #{node.title} </div>").join('')
  console.log dataSetsDiv

  if dataSetsDiv.length + samplesDiv.length > 0
    BootstrapDialog.show
      message: '<div>Are you sure to delete? </div>' +
        (("<b>Datasets:</b>#{dataSetsDiv}" if dataSetsDiv.length) or '') +
        (("<b>Samples:</b>#{samplesDiv}" if samplesDiv.length) or '') +
        (("<div>Sample deletion is not implemented, yet</div>" if samplesDiv.length) or '')
      buttons: [
        {
          label: 'Delete'
          cssClass: 'btn-danger'
          icon: 'glyphicon glyphicon-trash'
          action: (dialogItself) ->
            startDelete selectedNodes
            dialogItself.close()
        }
        {
          label: 'Cancel'
          action: (dialogItself) ->
            dialogItself.close()

        }
      ]
  else
    BootstrapDialog.alert "No dataset or sample selected!"

findSelectedNode = ->
  nodes = $('#tree').fancytree('getRootNode').tree.getSelectedNodes()
  uniqueDataSetNodes = []
  uniqueSampleNodes = []
  for node in nodes
    if node.data.type == "data-set"
      console.log "data-set" + node.data.type
      console.log node
      uniqueDataSetNodes.push node
    else if node.data.type == "sample"
      if not (node.parent in nodes)
        uniqueSampleNodes.push node
        console.log "samples" + node.data.type
        console.log node
  {data_sets: uniqueDataSetNodes, samples: uniqueSampleNodes}


startDelete = (selectedNodes) ->
  for node in selectedNodes.data_sets
    deleteDataset(node)


deleteDataset = (node) ->
  console.log("data-set-sample.clicked")
  call = jsRoutes.controllers.gmql.DSManager.deleteDataset(node.title)
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    success: (result, textStatus, jqXHR) ->
      displayInfo(result)
      #      BootstrapDialog.show
      #        message: "Dataset(#{node.title}) is deleted"
      $.notify({
        title: '<strong>Deleted</strong>',
        message: "Dataset(#{node.title}) is deleted"
      }, {
        type: 'success'
        placement: {
          align: "left"
          from: "top"
        }
        delay: 1000
      });

      resetPrivate()
    error: (jqXHR, textStatus, errorThrown) ->
      console.log("error" + textStatus)
      BootstrapDialog.alert "#{node.title} cannot be deleted"
      resetPrivate()

downloadDataset = () ->
  console.log(".clicked")
  if window.lastSelectedDataSet.startsWith("public.")
    BootstrapDialog.alert "Public dataset download is not available from this interface. For downloading them, please visit the <a href='http://www.bioinformatics.deib.polimi.it/GMQLsystem/datasets/' target='_blank'>link</a>. "
  else
#    dialog = BootstrapDialog.alert "Download file is preparing, please wait"
#    window.lastDownloadDataSet = window.lastSelectedDataSet
    call = jsRoutes.controllers.gmql.DSManager.zip window.lastSelectedDataSet
    window.location = call.url
#    $.ajax
#      url: call.url
#      type: call.type
#      method: call.method
#      dataType: 'binary'
#      headers: {'X-AUTH-TOKEN': window.authToken}
#      success: (result, textStatus, jqXHR) ->
#        if(result != "inProgress")
#          callUrl = jsRoutes.controllers.gmql.DSManager.downloadFileZip window.lastDownloadDataSet
#          window.location = callUrl.url
#          dialog.close()
#        else
#          BootstrapDialog.alert "Download preparation is still in preparation"
#          dialog.close()
#      error: (jqXHR, textStatus, errorThrown) ->
#        console.log("error222" + textStatus)


#    call = jsRoutes.controllers.gmql.DSManager.downloadFileZip window.lastSelectedDataSet
#    # always open windows for downloading
#    if false #window.URL
#      $.ajax
#        url: call.url
#        type: call.type
#        method: call.method
#        dataType: 'binary'
#        headers: {'X-AUTH-TOKEN': window.authToken}
#        success: (result, textStatus, jqXHR) ->
#          downloadResult(result, jqXHR)
#        error: (jqXHR, textStatus, errorThrown) ->
#          console.log("error222" + textStatus)
#  #      BootstrapDialog.alert "Error"
#    else


# expand the tree
$ ->
  setTimeout expandPublic, 2000
  setTimeout selectFirstPublic, 4000

expandPublic = ->
  $("#tree").fancytree("getRootNode").children[1].setExpanded(true)

selectFirstPublic = ->
  firstDS = $("#tree").fancytree("getRootNode").children[1].children[0]
  firstDS.setActive(true)

loadUcsc = ->
  dataSet = window.lastSelectedDataSet
  if dataSet.startsWith("public.")
    BootstrapDialog.alert "Public dataset UCSC browsing is not available from this interface."
  else
    console.log(".clicked")

    # get first sample
    call = jsRoutes.controllers.gmql.DSManager.getSamples dataSet
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {'X-AUTH-TOKEN': window.authToken}
      contentType: 'json'
      dataType: 'json'
      success: (result, textStatus, jqXHR) ->
        first_sample_name = result.samples[0].name
        console.log "first_sample_name: #{first_sample_name}"
        call = jsRoutes.controllers.gmql.DSManager.getRegionStream(dataSet, first_sample_name)
        # get first sample first line
        call.url = call.url + "?top=1&bed6=true"
        $.ajax
          url: call.url
          type: call.type
          method: call.method
          headers: {'X-AUTH-TOKEN': window.authToken}
          contentType: 'text'
          dataType: 'text'
          success: (result, textStatus, jqXHR) ->
            splitted = result.split "\t"
            chr = splitted[0]
            start = splitted[1]
            end = splitted[2]
            if start > end
              start = end
            length = end - start + 1
            start = start - length
            if start < 1
              start = 1
            end = start + 3 * length - 1
            if end > MAX_INT/2
              end = MAX_INT/2

            showUcscDialog(dataSet, chr, start, end)

          error: (jqXHR, textStatus, errorThrown) ->
            BootstrapDialog.alert
              message: "Cannot prepare UCSC links for dataset #{dataSet}"
              type: BootstrapDialog.TYPE_WARNING

      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert
          message: "Cannot prepare UCSC links for dataset #{dataSet}"
          type: BootstrapDialog.TYPE_WARNING


showUcscDialog = (dataSet, chr, start, end)->
  position = "#{chr}:#{start}-#{end}"
  buttons = []
  buttons.push
    label: 'Open'
    action: (dialogItself) ->
      newWin = window.open('', '_blank');
      newWin.blur();
      call = jsRoutes.controllers.gmql.DSManager.getUcscLink dataSet, $('#position').val()
      $.ajax
        url: call.url
        type: call.type
        method: call.method
        headers: {'X-AUTH-TOKEN': window.authToken}
        success: (result, textStatus, jqXHR) ->
          console.log result
          #      window.open (ucscBaseLink + result)
          ucscBaseLink = "http://genome.ucsc.edu/cgi-bin/hgTracks?db=#{$('#assembly').val()}&hgt.customText="
          newWin.location = ucscBaseLink + result
        error: (jqXHR, textStatus, errorThrown) ->
          BootstrapDialog.alert
            message: "Cannot prepare UCSC links for dataset #{dataSet}"
            type: BootstrapDialog.TYPE_WARNING
          newWin.close()
      dialogItself.close()
  buttons.push
    label: 'Close'
    action: (dialogItself) ->
      dialogItself.close()

  BootstrapDialog.show
    closeByBackdrop: false
    closeByKeyboard: true
    title: "UCSC of #{dataSet}"
#    size: BootstrapDialog.SIZE_WIDE
    message: '<form>
                <div class="form-group">
                  <label for="assembly">Assembly</label>
                  <select class="form-control" id="assembly">
                    <option value="hg19">Feb. 2009 GRCh37/hg19</option>
                    <option value="hg38">Dec. 2013 GRCh38/hg38</option>
                  </select>
                </div>
                <div class="form-group">
                  <label for="position">Browser position</label>
                  <input type="text" class="form-control" id="position" value="' + position + '">
                </div>
              </form>'
    buttons: buttons
    onhide: ->
    onshown: (dialogRef) ->


changeFullScreen = ->
  $('#col-tree').toggleClass('col-md-4 col-md-12');
  $('#col-query').toggleClass('col-md-8 col-md-0');
  #  $('#col-schema').toggleClass('col-md-4 col-md-0');
  $('.tree-full-screen span').toggleClass('glyphicon-resize-full glyphicon-resize-small');
  ace.edit("main-query-editor").resize()


showInfo = (node) ->
  data = node.data
  type = data.type
  datasetName =
    switch type
      when "sample"   then node.parent.data.value
      when "data-set"  then data.value

  sampleName =
    switch type
      when "sample"   then data.value

  call =
    switch type
      when "sample"   then jsRoutes.controllers.gmql.DSManager.getSampleInfo(datasetName, sampleName)
      when "data-set"  then jsRoutes.controllers.gmql.DSManager.getDatasetInfo(datasetName)


  if datasetName?
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {'X-AUTH-TOKEN': window.authToken}
      contentType: 'json'
      dataType: 'json'
      success: (result, textStatus, jqXHR) ->
        showInfoBootstrapDialog("Info of #{datasetName}", result)

      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert "Error"

showInfoBootstrapDialog = (title, result) ->
  buttons = []
  BootstrapDialog.show
    closeByBackdrop: false
    closeByKeyboard: true
    title: title
    size: BootstrapDialog.SIZE_WIDE
    message: '<div id="infoTableDiv"><table id="displayInfoTable" ></table></div>'
    buttons: [
      {
        label: 'Close'
        action: (dialogItself) ->
          dialogItself.close()
      }
    ]
    onhide: ->
      $('#displayTable').DataTable().destroy(true)
      window.tableResult = null
    onshown: (dialogRef) ->
# to define which direction will be the result
      window.tableResult = result
      showInfoTable(result)

showInfoTable = (result) ->
  $('#infoTableDiv').empty()
  $('#infoTableDiv').append '<table id="displayInfoTable" ><thead><tr></tr></thead><tfoot><tr></tr></tfoot></table>'

  thead = $('#displayInfoTable thead tr')
  tfoot = $('#displayInfoTable tfoot tr')

  thead.append $("<th>Attribute</th>")
  tfoot.append $("<th>Attribute</th>")
  thead.append $("<th>Value</th>")
  tfoot.append $("<th>Value</th>")


  data = result.infoList.map (info) -> [info.key, info.value]


  table = $('#displayInfoTable').DataTable
    data: data

# start showQuery: shows the query of the dataset.
showQuery = (node) ->
  data = node.data
  type = data.type
  datasetName =
    switch type
      when "sample"   then node.parent.data.value
      when "data-set"  then data.value

  if datasetName?
    call = jsRoutes.controllers.gmql.DSManager.getQueryStream(datasetName)
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {'X-AUTH-TOKEN': window.authToken}
      contentType: 'text'
      dataType: 'text'
      success: (result, textStatus, jqXHR) ->
        showQueryBootstrapDialog("Query of #{datasetName}", result, call)

      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert "There is no query for this dataset."
# end showQuery: shows the query of the dataset.

showVocabulary = (node) ->
  data = node.data
  type = data.type
  datasetName =
    switch type
      when "sample"   then node.parent.data.value
      when "data-set"  then data.value

  if datasetName?
    call = jsRoutes.controllers.gmql.DSManager.getVocabularyStream(datasetName)
    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {'X-AUTH-TOKEN': window.authToken}
      contentType: 'text'
      dataType: 'text'
      success: (result, textStatus, jqXHR) ->
        showQueryBootstrapDialog("Vocabulary of #{datasetName}", result, call, 'vocabulary')

      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert "There is no vocabulary for this dataset."


@showQueryBootstrapDialog = (title, query, call, type) ->
  buttons = []
  if(call?)
    buttons.push
      label: 'Download'
      action: (dialogItself) ->
        window.location = call.url
  buttons.push
    label: 'Copy to clipboard'
    action: (dialogItself) ->
      editor = ace.edit("tree-query-editor")
      editor.selectAll()
      editor.focus()
      document.execCommand('copy')
      editor.clearSelection()
  if(type != 'vocabulary')
    buttons.push
      label: 'Copy to query editor'
      action: (dialogItself) ->
        if(ace.edit("main-query-editor").getValue().length)
          BootstrapDialog.confirm 'Are you sure to overwrite to query editor?', (yesNo) ->
            if(yesNo)
              ace.edit("main-query-editor").setValue(query)
              $.each BootstrapDialog.dialogs, (id, dialog) -> dialog.close() # close all dialogs
        else
          ace.edit("main-query-editor").setValue(query)
          $.each BootstrapDialog.dialogs, (id, dialog) -> dialog.close() # close all dialogs
  buttons.push
    label: 'Close'
    action: (dialogItself) ->
      dialogItself.close()


  BootstrapDialog.show
    title: title
    size: BootstrapDialog.SIZE_WIDE
    message: "<div id='tree-query-editor' style='height: 100px;'></div>"
    buttons: buttons
    onshown: ->
      editor = ace.edit("tree-query-editor")
      editorOption(editor, query, "gmql")
      editor.getSession().setUseWrapMode(true)

renameDataset = (node) ->
  data = node.data
  type = data.type
  datasetName =
    switch type
      when "sample"   then node.parent.data.value
      when "data-set"  then data.value

  if datasetName?
    BootstrapDialog.show
      title: "Change dataset name of #{datasetName}"
      message: "<div style='height: 100px;'><label>New name:</label> <input id='dataset-new-name'></input></div>"
      buttons: [
        {
          label: 'Rename'
          action: (dialogItself) ->
            rename(datasetName, $('#dataset-new-name').val())
        }
        {
          label: 'Close'
          action: (dialogItself) ->
            dialogItself.close()
        }
      ]


rename = (datasetName, datasetNewName) ->
  call = jsRoutes.controllers.gmql.DSManager.renameDataset(datasetName, datasetNewName)
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'json'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      BootstrapDialog.show
        message: 'Done!'
        buttons: [
          {
            label: 'Close'
            action: (dialogItself) ->
              $.each BootstrapDialog.dialogs, (id, dialog) -> dialog.close() # close all dialogs
          }
        ]
      resetPrivate()
    error: (jqXHR, textStatus, errorThrown) ->
      BootstrapDialog.alert jqXHR.responseJSON.error
      resetPrivate()


# start showRegion: shows the first lines of the regions of sample
showMetaRegion = (node, isMeta) ->
  data = node.data
  type = data.type
  sampleName = data.value
  datasetName = node.parent.data.value

  if type == "sample"
    call = jsRoutes.controllers.gmql.DSManager.getRegionStream(datasetName, sampleName)
    top = 20
    if(isMeta)
      call = jsRoutes.controllers.gmql.DSManager.getMetadataStream(datasetName, sampleName)
      top = 0


    callUrlOriginal = call.url # for download button

    call.url = call.url + "?header=true"
    call.url = call.url + "&top=#{top}" if top # if it is meta show all

    $.ajax
      url: call.url
      type: call.type
      method: call.method
      headers: {'X-AUTH-TOKEN': window.authToken}
      contentType: 'text'
      dataType: 'text'
      success: (result, textStatus, jqXHR) ->
        window.result = result
        BootstrapDialog.show
          cssClass: 'modal-wide'
          title: (if isMeta then "Metadata " else "Region data ") + "of #{datasetName}->#{sampleName}"
          message: "<div id='tree-query-editor' style='height: 100px;'></div>"
          buttons: [
            {
              label: 'Download'
              action: (dialogItself) ->
                window.location = callUrlOriginal
            }
            {
              label: 'Copy to clipboard'
              action: (dialogItself) ->
                editor = ace.edit("tree-query-editor")
                editor.selectAll()
                editor.focus()
                document.execCommand('copy')
                editor.clearSelection()
            }
            {
              label: 'Close'
              action: (dialogItself) ->
                dialogItself.close()
            }
          ]
          onshown: ->
            editor = ace.edit("tree-query-editor")
            editorOption(editor, result, "")
            editor.getSession().setUseWrapMode(true)

      error: (jqXHR, textStatus, errorThrown) ->
        BootstrapDialog.alert "The sample is not available"
# end showRegion: shows the first lines of the regions of sample


loadContext = -> $('#tree').contextmenu
  delegate: 'span.fancytree-title'
  autoFocus: true
  menu: [
    {
      title: 'Show info'
      cmd: 'showInfo'
      uiIcon: 'ui-icon-info'
    }
    {title: "----"}
    {
      title: 'Show query'
      cmd: 'showQuery'
      uiIcon: 'ui-icon-note'
    }
#    {
#      title: 'Show vocabulary'
#      cmd: 'showVocabulary'
#      uiIcon: 'ui-icon-note'
#    }
    {
      title: 'Rename'
      cmd: 'renameDataset'
      uiIcon: 'ui-icon-pencil'
    }
    {title: "----"}
    {
      title: 'Show region data'
      cmd: 'showRegion'
      uiIcon: 'ui-icon-circle-zoomin'
    }
    {
      title: 'Show metadata'
      cmd: 'showMeta'
      uiIcon: 'ui-icon-circle-zoomin'
    }
#    {
#      title: 'Cut'
#      cmd: 'cut'
#      uiIcon: 'ui-icon-scissors'
#    }
#    {
#      title: 'Copy'
#      cmd: 'copy'
#      uiIcon: 'ui-icon-copy'
#    }
#    {
#      title: 'Paste'
#      cmd: 'paste'
#      uiIcon: 'ui-icon-clipboard'
#      disabled: false
#    }
#    { title: '----' }
#    {
#      title: 'Edit'
#      cmd: 'edit'
#      uiIcon: 'ui-icon-pencil'
#      disabled: true
#    }
#    {
#      title: 'Delete'
#      cmd: 'delete'
#      uiIcon: 'ui-icon-trash'
#      disabled: true
#    }
#    {
#      title: 'More'
#      children: [
#        {
#          title: 'Sub 1'
#          cmd: 'sub1'
#        }
#        {
#          title: 'Sub 2'
#          cmd: 'sub1'
#        }
#      ]
#    }
  ]
  beforeOpen: (event, ui) ->
    node = $.ui.fancytree.getNode(ui.target)
    # Modify menu entries depending on node status
    #    $('#tree').contextmenu 'enableEntry', 'paste', node.isFolder()
    isSample = node.data.type == 'sample'
    isPrivateDs = node.parent.data.value == 'private-data-set'
    $('#tree').contextmenu 'enableEntry', 'showRegion', isSample
    $('#tree').contextmenu 'enableEntry', 'showMeta', isSample
    $('#tree').contextmenu 'enableEntry', 'showQuery', isPrivateDs
    $('#tree').contextmenu 'enableEntry', 'showVocabulary', isPrivateDs
    $('#tree').contextmenu 'enableEntry', 'renameDataset', isPrivateDs

    # Show/hide single entries
    #            $("#tree").contextmenu("showEntry", "cut", false);
    # Activate node on right-click
    node.setActive()
    # Disable tree keyboard handling
    ui.menu.prevKeyboard = node.tree.options.keyboard
    node.tree.options.keyboard = false
    if node.data.type == 'main'
      return false
    else
      return true
#    else
#      tempNode = tempNode.parent until tempNode.data.type == 'main'
#      return tempNode.data.value == 'private-data-set'
  close: (event, ui) ->
# Restore tree keyboard handling
# console.log("close", event, ui, this)
# Note: ui is passed since v1.15.0
    node = $.ui.fancytree.getNode(ui.target)
    node.tree.options.keyboard = ui.menu.prevKeyboard
    node.setFocus()
    return
  select: (event, ui) ->
    node = $.ui.fancytree.getNode(ui.target)
    console.log 'select ' + ui.cmd + ' on ' + node
    switch ui.cmd
      when 'showInfo' then showInfo(node)
      when 'showQuery' then showQuery(node)
      when 'showVocabulary' then showVocabulary(node)
      when 'renameDataset' then renameDataset(node)
      when 'showRegion' then showMetaRegion(node, false)
      when 'showMeta' then showMetaRegion(node, true)


$ ->
  $('#popover-anchor-tree').popover html: true
  popover = $('#popover-anchor-tree').popover()
  popover.on 'inserted.bs.popover', ->
    instance = $(this).data('bs.popover')
    # Replace the popover's content element with the 'content' element
    slider = new Slider('#tree-size', {})
    editor = ace.edit("main-query-editor")
    fontSize = $('ul.fancytree-container').css('font-size').replace /px/, ""
    min = ($('ul.fancytree-container').css('min-height').replace /px/, "") / fontSize
    max = ($('ul.fancytree-container').css('max-height').replace /px/, "") / fontSize
    slider.setValue([min, max])
    #    slider.setValues(editor.getOption("minLines"),editor.getOption("maxLines"))
    slider.on 'change', (event) ->
      editor = ace.edit("main-query-editor")
      n = event.newValue
      o = event.oldValue
      #      console.log event
      $('ul.fancytree-container').css('min-height', "#{n[0]}em") if o[0] != n[0]
      $('ul.fancytree-container').css('max-height', "#{n[1]}em") if o[1] != n[1]


#      ul.fancytree-container {
#          min-height: 160px;
#  width: 100%;
#  max-height: 320px;
#}