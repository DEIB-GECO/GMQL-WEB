define("ace/mode/gmql_highlight_rules",["require","exports","module","ace/lib/oop","ace/mode/text_highlight_rules"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextHighlightRules = require("./text_highlight_rules").TextHighlightRules;

var GmqlHighlightRules = function() {

    var keywords = (
        "select|cover|materialize|map|project|join"
    );

    var builtinConstants = (
        "true|false|distance|mindistance|dle|dge|md|any|all|start|stop|chr|strand|left|right|up|down|downstream|upstream"
    );

    var builtinFunctions = (
        "semijoin|region|parser|region_modifier|meta_modifier|meta_project|into|groupby|aggregate|joinby|output|meta_top|meta_topg|region_order|region_top|region_topg"
    );

    var dataTypes = (
        "int|numeric|decimal|date|varchar|char|bigint|float|double|bit|binary|text|set|timestamp|" +
        "money|real|number|integer"
    );

    var keywordMapper = this.createKeywordMapper({
        "support.function": builtinFunctions,
        "keyword": keywords,
        "constant.language": builtinConstants,
        "storage.type": dataTypes
    }, "identifier", true);

    this.$rules = {
        "start" : [ {
            token : "comment",
            regex : "#.*$"
        },  {
            token : "string",           // " string
            regex : '".*?"'
        }, {
            token : "string",           // ' string
            regex : "'.*?'"
        }, {
            token : "constant.numeric", // float
            regex : "[+-]?\\d+(?:(?:\\.\\d*)?(?:[eE][+-]?\\d+)?)?\\b"
        }, {
            token : keywordMapper,
            regex : "[a-zA-Z_$][a-zA-Z0-9_$]*\\b"
        }, {
            token : "keyword.operator",
            regex : "\\+|\\-|\\/|\\/\\/|%|<@>|@>|<@|&|\\^|~|<|>|<=|=>|==|!=|<>|="
        }, {
            token : "paren.lparen",
            regex : "[\\(]"
        }, {
            token : "paren.rparen",
            regex : "[\\)]"
        }, {
            token : "text",
            regex : "\\s+"
        } ]
    };
    this.normalizeRules();
};

oop.inherits(GmqlHighlightRules, TextHighlightRules);

exports.GmqlHighlightRules = GmqlHighlightRules;
});

define("ace/mode/gmql",["require","exports","module","ace/lib/oop","ace/mode/text","ace/mode/gmql_highlight_rules","ace/range"], function(require, exports, module) {
"use strict";

var oop = require("../lib/oop");
var TextMode = require("./text").Mode;
var GmqlHighlightRules = require("./gmql_highlight_rules").GmqlHighlightRules;
var Range = require("../range").Range;

var Mode = function() {
    this.HighlightRules = GmqlHighlightRules;
};
oop.inherits(Mode, TextMode);

(function() {

    this.lineCommentStart = "--";

    this.$id = "ace/mode/gmql";
}).call(Mode.prototype);

exports.Mode = Mode;

});
