HTMLWidgets.widget({

    name: 'd3heatmap',

    type: 'output',

    initialize: function (el, width, height) {

        return {
            lastTheme: null,
            lastValue: null
        };

    },

    renderValue: function (el, x, instance) {
        this.doRenderValue(el, x, instance);
    },

    // Need dedicated helper function that can be called by both renderValue
    // and resize. resize can't call this.renderValue because that will be
    // routed to the Shiny wrapper method from htmlwidgets, which expects the
    // wrapper data object, not x.
    doRenderValue: function (el, x, instance) {
        var self = this;

        instance.lastValue = x;

        if (instance.lastTheme && instance.lastTheme != x.theme) {
            d3.select(document.body).classed("theme-" + instance.lastTheme, false);
        }
        if (x.theme) {
            d3.select(document.body).classed("theme-" + x.theme, true);
        }

        el.innerHTML = "";

        // this.loadImage(x.image, function (imgData, w, h) {

        // if (w !== x.matrix.dim[0] || h !== x.matrix.dim[1]) {
        //     throw new Error("Color dimensions didn't match data dimensions")
        // }
        // console.log(x.matrix.data);

        window.heatmapjson = x.matrix.data;
        var numbers = onlyNumber(x.matrix.data);
        var arrayNumber = convertNumber(numbers);
        var max = arrayMax(arrayNumber);
        var min = arrayMin(arrayNumber);
        // console.log("max: " + max);
        // console.log("min: " + min);
        var merged = [];
        for (var i = 0; i < x.matrix.data.length; i++) {
            // var r = parseInt(imgData[i * 4] / 2);
            // var g = imgData[i * 4 + 1];
            // var b = imgData[i * 4 + 2];
            // var a = imgData[i * 4 + 3];
            // console.log(r+"-"+g+"-"+b+"-"+a);

            merged.push({
                //TODO how to push label
                label: x.matrix.data[i] ,
                color: ConvertToRgb(min, max, x.matrix.data[i])//"rgba(" + [r,g,b,a/255].join(",") + ")"
            })
        }

        x.matrix.merged = merged;
        //console.log(JSON.stringify({merged: x.matrix.merged}, null, "  "));

        var hm = heatmap(el, x, x.options);
        if (window.Shiny) {
            var id = self.getId(el);
            hm.on('hover', function (e) {
                Shiny.onInputChange(id + '_hover', !e.data ? e.data : {
                    label: e.data.label,
                    row: x.matrix.rows[e.data.row],
                    col: x.matrix.cols[e.data.col]
                });
            });
            /* heatmap doesn't currently send click, since it means zoom-out
             hm.on('click', function(e) {
             Shiny.onInputChange(id + '_click', !e.data ? e.data : {
             label: e.data.label,
             row: e.data.row + 1,
             col: e.data.col + 1
             });
             });
             */
        }
        // });
    },

    resize: function (el, width, height, instance) {
        if (instance.lastValue) {
            this.doRenderValue(el, instance.lastValue, instance);
        }
    },

    // loadImage: function (uri, callback) {
    //     var img = new Image();
    //     img.onload = function () {
    //         // Save size
    //         w = img.width;
    //         h = img.height;
    //
    //         // Create a dummy canvas to extract the image data
    //         var imgDataCanvas = document.createElement("canvas");
    //
    //         imgDataCanvas.width = w;
    //         imgDataCanvas.height = h;
    //         imgDataCanvas.style.display = "none";
    //         document.body.appendChild(imgDataCanvas);
    //
    //         var imgDataCtx = imgDataCanvas.getContext("2d");
    //         imgDataCtx.drawImage(img, 0, 0);
    //
    //         // Save the image data.
    //         imgData = imgDataCtx.getImageData(0, 0, w, h).data;
    //
    //         // Done with the canvas, remove it from the page so it can be gc'd.
    //         document.body.removeChild(imgDataCanvas);
    //
    //         callback(imgData, w, h);
    //     };
    //     img.src = uri;
    // },
});

// TODO For the architectural design check if it is better to defined this function in the d3heatmap of not
function ConvertToRgb(min, max, value) {
    // TODO move minimum and maximum converson should be done once in the upper part.
    var minimum = parseFloat(min);
    var maximum = parseFloat(max);
    var value = parseFloat(value);
    var r, g, b;
    var a = 255;
    window.asd =value;
    // console.log (value);
    // console.log(typeof(value) != 'undefined' && value != null && value != NaN );
    // TODO isNaN would be enough, check it
    if (typeof(value) != 'undefined' && value != null && !isNaN(value)) {
        var boundry = (min + max * 3) / 4 ;
        // var halfmax = (minimum + maximum) / 2;
        var percentFade, sRed,sGreen,sBlue,eRed,eGreen,eBlue;
        if (value <= boundry) {
            sRed = 255;
            sGreen = 0;
            sBlue = 0;

            eRed = 255;
            eGreen = 255;
            eBlue = 0;

            percentFade =  (value - minimum) / (boundry - min);//2 * (value - minimum) / (maximum - minimum);
        } else {
            sRed = 255;
            sGreen = 255;
            sBlue = 0;
            eRed = 255;
            eGreen = 255;
            eBlue = 213;


            percentFade =  (value - boundry) / (max - boundry);//2 * (value - halfmax) / (maximum - minimum) ;
        }


        // console.log(sRed);

        var diffRed = eRed - sRed;
        var diffGreen = eGreen - sGreen;
        var diffBlue = eBlue - sBlue;

        r = parseInt((diffRed * percentFade) + sRed);
        g = parseInt((diffGreen * percentFade) + sGreen);
        b = parseInt((diffBlue * percentFade) + sBlue);

        // console.log(value);

        // var ratio = 2 * (value - minimum) / (maximum - minimum);
        // var b = parseInt(Math.max(0, 255 * (1 - ratio)));
        // var r = parseInt(Math.max(0, 255 * (ratio - 1)));
        // var g = 255 - b - r;
    } else {

        r = 0;
        g = 0;
        b = 0;
    }
    // console.log("rgba(" + [r, g, b, a / 255].join(",") + ")")
    return "rgba(" + [r, g, b, a / 255].join(",") + ")";
}

var arrayMax = Function.prototype.apply.bind(Math.max, null);
var arrayMin = Function.prototype.apply.bind(Math.min, null);

function onlyNumber(x) {
    return x.filter(function(x){return !isNaN(x);});
}

function convertNumber(x) {
    return x.map(function (x) {
        return Number(x);
    })
}