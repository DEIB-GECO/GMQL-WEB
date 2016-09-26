/**
 * Created by canakoglu on 6/2/16.
 */
function editorOption(editor, val, mode) {
    editor.$blockScrolling = Infinity;
    // editor.setTheme("ace/theme/xcode");
    if (mode == "xml")
        editor.getSession().setMode("ace/mode/xml");
    else if (mode == "json")
        editor.getSession().setMode("ace/mode/json");
    else if (mode == "gmql")
        editor.getSession().setMode("ace/mode/gmql");
    editor.setReadOnly(true);
    // editor.setValue(val, 1);
    editor.session.setUseWorker(false);
    editor.renderer.setShowGutter(false);
    editor.setShowPrintMargin(false);
    editor.setOptions({maxLines: Infinity});
    editor.setOptions({
        readOnly: true,
        highlightActiveLine: false,
        highlightGutterLine: false
    });
    editor.renderer.$cursorLayer.element.style.opacity = 0;
    // editor.setOptions({hScrollBarAlwaysVisible: true, vScrollBarAlwaysVisible: false});
    //editor.getSession().setUseWrapMode(true);
    // editor.resize();
    editorSetValue(editor, val)
}

function editorSetValue(editor, val) {
    editor.setValue(val, 1);
    // editor.resize();
    // setTimeout(
    //     function () {
    //         editor.setValue(val, 1);
    //         editor.resize();
    //     }, 500);
}