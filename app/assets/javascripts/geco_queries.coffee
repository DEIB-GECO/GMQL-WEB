helloWorld = () ->
  ajaxUrl = jsRoutes.controllers.gmql.GecoQueries.gecoQueriesJson().url
  table = $('#geco_queries_table').DataTable(
    'crossDomain' : true,
    'ajax': ajaxUrl
    'columns': [
      {'data': 'name'}
      {
        'data': 'query'
        'visible': false
      }
      {
        'data': 'group'
        'visible': false
      }
      {'data': 'description'}
    ]
    'order': [[2, 'asc']]
    'displayLength': 25
    'drawCallback': (settings) ->
      api = @api()
      rows = api.rows(page: 'current').nodes()
      last = null
      api.column(2, page: 'current').data().each (group, i) ->
        if last != group
          $(rows).eq(i).before '<tr class="group"><td colspan="5">' + group + '</td></tr>'
          last = group
        return
      return
  )
  # Order by the grouping
  $('#geco_queries_table tbody').on 'click', 'tr.group', ->
    currentOrder = table.order()[0]
    if currentOrder[0] == 2 and currentOrder[1] == 'asc'
      table.order([2, 'desc']).draw()
    else
      table.order([2, 'asc']).draw()


  $('#geco_queries_table').on 'dblclick', 'td', (e) ->
    name = table.row($(this).parents('tr')).data()['name']
    query = table.row($(this).parents('tr')).data()['query']
    showQueryBootstrapDialog("Example query: #{name}", query)


showGecoQueriesTable = ->
  call = jsRoutes.controllers.gmql.GecoQueries.gecoQueries()
  BootstrapDialog.show
    title: "Example Queries"
    size: BootstrapDialog.SIZE_WIDE
    message:       $('<div></div>').load(call.url)
    onshown: ->
      helloWorld()


$ ->
  $('#main-query-example').on 'click', (e) ->
    showGecoQueriesTable()