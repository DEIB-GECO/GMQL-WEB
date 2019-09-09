fillTable = (q_type) ->
  ajaxUrl = jsRoutes.controllers.gmql.GecoQueries.gecoQueriesJson(q_type).url
  table = $('#geco_queries_table').DataTable(
    "bSort": false
    'crossDomain': true,
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
      {
        'className': 'details-control'
        'orderable': false
        'data': null
        'defaultContent': ''
      }
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


  #  last column click
  $('#geco_queries_table').on 'click', 'td.details-control', (e) ->
    name = table.row($(this).parents('tr')).data()['name']
    query = table.row($(this).parents('tr')).data()['query']
    showQueryBootstrapDialog("Example query: #{name}", query)


showGecoQueriesTable = (q_type) ->
  call = jsRoutes.controllers.gmql.GecoQueries.gecoQueries()
  BootstrapDialog.show
    title: "Example Queries"
    size: BootstrapDialog.SIZE_WIDE
    message: $('<div></div>').load(call.url)
    onshown: ->
      fillTable(q_type)


$ ->
  $('#main-query-example').on 'click', (e) ->
    showGecoQueriesTable("default")
$ ->
  $('#federated-query-example').on 'click', (e) ->
    showGecoQueriesTable("federated")