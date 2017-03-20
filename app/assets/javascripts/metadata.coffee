MAX_SAMPLES = 20
red = 0



$ ->
  $("#metadata-add-button").click metadataAddButtonClick
  $('#metadata-test-button').click testSelect


  #  editor creation
  editor = ace.edit("metadata-query-editor");
  editorOption(editor, getQuery(), "gmql")
  editor.getSession().setUseWrapMode(true)

  generateQuery()


metadataAddButtonClick = ->
  if window.lastSelectedDataSet?
    metadataSearchDiv = $("#metadata-search-div")
    metadataSearchDiv.find('.selectpicker').attr('disabled', true).selectpicker('refresh')
    metadataSearchDiv.find('.del-button').attr('disabled', true)
    row = rowDiv()
    metadataSearchDiv.append row
    row.find(".selectpicker").selectpicker('show')
    # move window till end of metedata division
    windowBottom = $( window ).scrollTop() + $( window ).height()
    metadataSearchDivBottom = $( "#metadata-search-div" ).offset().top +  $( "#metadata-search-div" ).height()
#    $("html, body").animate({ scrollTop: (metadataSearchDivBottom - $( window ).height()) }) if(metadataSearchDivBottom>windowBottom)
    $( window ).scrollTop(metadataSearchDivBottom - $( window ).height()) if(metadataSearchDivBottom>windowBottom)
  else
    alert('Please select a data set')


rowDiv = () ->
  delButton = deleteButton()
  div = $('<div/>')
  .addClass "metadata-row-div"
  .append firstDropDown
  .append secondDropDown
  .append delButton
  delButton.click ->
    metadataSearchDiv = div.prev()
    metadataSearchDiv.find('.selectpicker').attr('disabled', false).selectpicker('refresh')
    metadataSearchDiv.find('.del-button').attr('disabled', false)
    div.remove()
    generateQuery()
  delButton.tooltip()
  div


firstDropDown = (dataSet)->
  select = $('<select/>')
  .attr("id", "first-drop-down")
  .addClass("selectpicker")
  .addClass("first-drop-down")
  .attr("data-style", "btn-primary")
  .attr("data-live-search", "true")
  .attr("title", "Choose attribute")
#  call = jsRoutes.controllers.gmql.MetadataBrowser.getKeys window.lastSelectedDataSet
  call = jsRoutes.controllers.gmql.MetadataBrowser.getFilteredKeys window.lastSelectedDataSet
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'application/json'
    dataType: 'json'
    data: getMetadataQuery()
    success: (result, textStatus, jqXHR) ->
      window.lastResult = result
      list = result.attributes
      for x in list
        newOption = $("<option/>").text("#{x.key} (#{x.valueCount} - #{x.sampleCount})").val(x.key)
        select.append newOption
      $('.selectpicker').selectpicker('refresh')
  select.change firstDropDownOnChange
  select


secondDropDown = ->
  select = $('<select multiple/>')
  .attr("id", "second-drop-down")
  .addClass("selectpicker")
  .addClass("second-drop-down")
  .attr("data-style", "btn-info")
  .attr("data-live-search", "true")
  .attr("data-selected-text-format", "count > 3")
  .attr("title", "First choose attribute")
  select.change secondDropDownOnChange
  select

deleteButton = ->
  button = $('<button/>')
  .attr("id", "delete-button")
  .addClass("del-button btn btn-danger")
  .attr("data-toggle", "tooltip")
  .attr("data-placement", "right")
  .attr("title", "Delete row")
  .html '<span class="glyphicon glyphicon-minus"></span>'

firstDropDownOnChange = ->
  selected = $(this).find("option:selected").val()
  select = $(this).parent().next().find("select")
  select.empty()
  call = jsRoutes.controllers.gmql.MetadataBrowser.getFilteredValues window.lastSelectedDataSet, selected
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'application/json'
    dataType: 'json'
    data: getMetadataQuery()
    success: (result, textStatus, jqXHR) ->
      window.lastResult = result
      list = result.values
      for x in list
        newOption = $("<option/>").text("#{x.text} (#{x.count})").val(x.text)
        select.append newOption
      select.selectpicker({title: "Choose value"}).selectpicker('refresh')
  generateQuery()

secondDropDownOnChange = -> generateQuery()

# ########QUERY

@clearMetadataDiv = -> allMetadataDiv().remove()

allMetadataDiv = () -> $(".metadata-row-div", "#metadata-search-div")
allMetadataDivSelected = () -> div  for div in allMetadataDiv() when $(div).find(".second-drop-down").find("option:selected").length > 0


getQuery = ->
  console.log("getQuery-> dataSetMame: #{window.lastSelectedDataSet}")
  metadataDivs = allMetadataDivSelected()
  outer = for div in metadataDivs
    firstSelectedText = $(div).find(".first-drop-down").find("option:selected").val()
    secondSelected = $(div).find(".second-drop-down").find("option:selected")

    inner = for second in secondSelected
      "#{firstSelectedText} == '#{second.value}'"
    reduced = if inner.length then inner.reduce (t, s) -> "#{t} OR #{s}" else ""
    if inner.length > 1  then "(#{reduced})" else reduced
  reduced = if outer?.length then outer.reduce (t, s) -> t + (if t.length && s.length then " AND " else "") + s else ""

  #  console.log(reduced)
  #  console.log(outer?.filter((x) -> x.length > 0).length == 1)
  #  console.log(outer?.filter((x) -> x.length > 0).length)
  #  console.log(/OR/.test reduced)
  reduced = if outer?.filter((x) -> x.length > 0).length == 1 and /OR/.test reduced then reduced.replace /^\(|\)$/g, "" else reduced
  @red = reduced.length
  console.log "last reduced: '" + reduced + "'"
  console.log "last reduced.length: " + reduced.length
  reduced = (reduced if reduced.length) or ""
  if window.lastSelectedDataSet?
    "DATA_SET_VAR = SELECT(#{reduced}) #{window.lastSelectedDataSet.replace /^public\./, ""};"
  else
    "Please select data set to generate query"


###
Generate query into ACE editor
###
@generateQuery = ->
  console.log "generateQuery: " +  @red

  editor = ace.edit("metadata-query-editor");
  query = getQuery()
  console.log "generateQuery: " +  query
  editorSetValue(editor, query)
  console.log "generateQuery: " +  @red
  @red > 0

getMetadataQuery = ->
  metadataDivs = allMetadataDivSelected()
  outer =
    attributes: for div in metadataDivs
      firstSelectedText = $(div).find(".first-drop-down").find("option:selected").val()
      secondSelecteds = (text: $(second).val() for second in $(div).find(".second-drop-down").find("option:selected"))
      key: firstSelectedText
      values: secondSelecteds
  input = JSON.stringify(outer, null, "\t")
  console.log input
  input

testSelect = ->
  console.log "testSelect"
  console.log("getQuery-> dataSetMame: #{window.lastSelectedDataSet}")
  
  call = jsRoutes.controllers.gmql.MetadataBrowser.getFilteredDataset window.lastSelectedDataSet
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'application/json'
    dataType: 'json'
    data: getMetadataQuery()
    success: (result, textStatus, jqXHR) ->
      window.lastResult = result
      console.log JSON.stringify(result, null, "\t")
      #      list = result.valueList.value
      #      for x in list
      #        newOption = $("<option/>").text(x.name)
      #        select.append newOption
      #      select.selectpicker({title: "Choose value"}).selectpicker('refresh')

      samples = result.samples
      count = samples.length

      if count
        list = samples.slice(0, MAX_SAMPLES)
        idList = (second.id for second in list).join "_"
        bsd = BootstrapDialog.show
#          size: BootstrapDialog.SIZE_WIDE
          title: (dialog) ->
            dialog.getData('title')
          message: (dialog) ->
            $message = $('<div>ERROR</div>')
            $message.load dialog.getData('pageToLoad')
            $message
          onshown: ->
            getSamples(window.lastSelectedDataSet)
            #          $('.nav-pills, .nav-tabs').tabdrop()

            changeTab()
          data:
            pageToLoad: jsRoutes.controllers.Application.sampleMetadata(count, idList).url
            title: 'Search result for ' + window.lastSelectedDataSet
        bsd.getModalDialog().addClass("modal-xl")
      else
        BootstrapDialog.alert "No result"

changeTab = ->
  $('.nav-tabs a').click ->
    $(this).tab 'show'

  $('.nav-tabs a').on 'shown.bs.tab', (event) ->
    x = $(event.target)
    console.log x
    window.activeTab = x
    href = x.attr("href")
    id = href.split "-"
    id = id[1]
    tab = $(href)
    sampleName = $("#a-sample-#{id}").text()
    #    tab.text("Test: " + id[1])
    if not tab.attr "loaded"
      tab.attr "loaded", true
      insertMetadataTable(tab, window.lastSelectedDataSet, id, sampleName)
# active tab
#    y = $(event.relatedTarget)
#     previous tab
#    $('.act span').text x 
#    $('.prev span').text y


getSamples = (dataSetName) ->
  call = jsRoutes.controllers.gmql.DSManager.getSamples(dataSetName)
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'application/json'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      window.sslastResult = result
      console.log JSON.stringify(result, null, "\t")
      for sample in result.samples
        id = sample.id
        name = sample.name #.split("/").pop()
        console.log 'id: ' + id
        console.log 'name: ' + name
        $("#a-sample-#{id}").text name
      $('.nav-pills, .nav-tabs').tabdrop()
      $($('.nav-tabs').children()[1]).find("a").tab('show')


###
        Metadata Table
###

@insertMetadataTable = (div, dataSet, id,sampleName) ->
  call = jsRoutes.controllers.gmql.MetadataBrowser.getSampleMetadata(dataSet, sampleName)
  $.ajax
    url: call.url
    type: call.type
    method: call.method
    headers: {'X-AUTH-TOKEN': window.authToken}
    contentType: 'json'
    dataType: 'json'
    success: (result, textStatus, jqXHR) ->
      window.result = result
      div.append metadataTable(dataSet, id, result.attributes)


metadataTable = (dataSet, id, metadata) ->
  div = $("<div id='metadata-table-div-#{dataSet}'  class='table-responsive'>")
  div.append table = $("
            <table id='metadata-table-#{dataSet}' class='table table-striped'>
                <thead>
                    <tr>
                        <th>Attribute</th>
                        <th>Value</th>
                    </tr>
                </thead>
            </table>
       ")
  table.prepend
  table.append tbody = $("<tbody>")
  for x in metadata
#    x.attribute = "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/brca/gsc/genome.wustl.edu/illuminaga_dnaseq_curated/mutations/genome.wustl.edu_BRCA.IlluminaGA_DNASeq_curated.Level_2.1.1.0/genome.wustl.edu_brca.illuminaga_dnaseq.level_2.1.1.0.curated.somatic.maf"
    cell1 = $("<td></td>").html x.key.replace(/[\/-]/g, "$&<wbr>")
    cell2 = $("<td></td>").html x.value.text.replace(/[\/-]/g, "$&<wbr>")
    newRow = $("<tr></tr>").append cell1, cell2
    tbody.append newRow
  div


