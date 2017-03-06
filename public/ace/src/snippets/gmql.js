define("ace/snippets/gmql",["require","exports","module"], function(require, exports, module) {
"use strict";

exports.snippetText = "##\n\
## SELECT\n\
snippet SELECT\n\
	SELECT(${1:pm}; region: ${2:pr}; semijoin: ${3:psj(DSext)}) ${4:DSin};\n\
\n\
##\n\
\n\
## MATERIALIZE\n\
snippet MATERIALIZE\n\
	MATERIALIZE ${1:DS} INTO ​${2:fileName};\n\
##\n\
\n\
## PROJECT\n\
snippet PROJECT\n\
	PROJECT(region_update: ${1:new_right} AS ${2:right}) ${3:DS};\n\
##\n\
\n\
## EXTEND\n\
snippet EXTEND\n\
	EXTEND(${1:RegionCount} AS COUNT()) ${2:EXP};\n\
##\n\
\n\
## GROUP\n\
snippet GROUP\n\
	GROUP(${1:Tumor_type}; region_aggregate: ${2:Min AS MIN(score)}) ${3:EXP};\n\
##\n\
\n\
## ORDER\n\
snippet ORDER\n\
	ORDER(Region_count ${1:DESC}; meta_top: ${2:2};) ${3:INPUT_DS};\n\
##\n\
\n\
## MERGE\n\
snippet MERGE\n\
	MERGE(groupby: ${1:antibody_target}) ${2:EXPERIMENT};\n\
##\n\
\n\
## UNION\n\
snippet UNION\n\
	UNION() ${1:BROAD} ${2:NARROW};\n\
##\n\
\n\
## DIFFERENCE\n\
snippet DIFFERENCE\n\
	DIFFERENCE(joinby: ${1:antibody_target}) ${2:EXP1} ${3:EXP2};\n\
##\n\
\n\
## MAP\n\
snippet MAP\n\
	MAP (${1:minScore} AS ${2:MIN(score)}; joinby: ${3:cell_tissue}) ${4:REF} ${5:EXP};\n\
##\n\
\n\
## JOIN\n\
snippet JOIN\n\
	JOIN(${1:genometric_predicate}; output: ${2:CAT}; joinby: ${3:provider}) ${4:DS_ANCHOR} ${5:DS_EXP};\n\
##\n\
\n\
## COVER\n\
snippet COVER\n\
	COVER(${1:minAcc}, ${2:maxAcc};​ groupby: ${3:cell}; aggregate: ${4:min_pValue} AS ${5:MIN(pValue)}) ${6:DSin};\n\
##\n\
\n\
## FLAT\n\
snippet FLAT\n\
	FLAT(${1:minAcc}, ${2:maxAcc}; groupby: ${3:cell}) ${4:EXP};\n\
##\n\
\n\
## SUMMIT\n\
snippet SUMMIT\n\
	SUMMIT(${1:minAcc}, ${2:maxAcc}; groupby: ${3:cell}) ${4:EXP};\n\
##\n\
\n\
## HISTOGRAM \n\
kerufherifuh \n\
snippet HISTOGRAM\n\
	HISTOGRAM(${1:minAcc}, ${2:maxAcc}; groupby: ${3:cell}) ${4:EXP};\n\
##\n\
";
exports.scope = "gmql";

});
