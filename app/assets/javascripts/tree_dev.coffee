$ ->
  setTimeout expand, 1000
  setTimeout selectFirstPublic, 2000
  #  setTimeout runTimeout, 3000
#  setTimeout expandFirstPublic, 3000
#  setTimeout selectFirstFirstPublic, 4000
#  setTimeout expand, 5000

expand = ->
  $("#tree").fancytree("getRootNode").visit((node)-> node.setExpanded(true))



expandFirstPublic = ->
  $("#tree").fancytree("getRootNode").children[1].children[0].setExpanded(true)

selectFirstPublic = ->
  firstDS = $("#tree").fancytree("getRootNode").children[1].children[0]
  firstDS.setActive(true)

selectFirstFirstPublic = ->
  firstDS = $("#tree").fancytree("getRootNode").children[1].children[0].children[0]
  firstDS.setActive(true)
