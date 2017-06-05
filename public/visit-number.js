var httpRequest;
function makeRequest(url) {
    if (window.XMLHttpRequest) {
        httpRequest = new XMLHttpRequest();
    } else if (window.ActiveXObject) {
        try {
            httpRequest = new ActiveXObject("Msxml2.XMLHTTP");
        }
        catch (e) {
            try {
                httpRequest = new ActiveXObject("Microsoft.XMLHTTP");
            }
            catch (e) {}
        }
    }
    if (!httpRequest) {
        alert('Giving up :( Cannot create an XMLHTTP instance');
        return false;
    }
    httpRequest.onreadystatechange = processResponse;
    httpRequest.open('GET', url);
    httpRequest.send();
}
function processResponse() {
    if (httpRequest.readyState === 4) {
        if (httpRequest.status === 200) {
            console.info('[Analytics] Response: ' + httpRequest.responseText);

            var response = JSON.parse(httpRequest.responseText);
            if (response) {
                var numberOfVisits = response["gmql-rest"].visits[0];
                console.info('[Analytics] Total Visits: ', numberOfVisits);
                var canvas = document.getElementById('visit_count');
                var nVisitors = document.createElement('span');
                nVisitors.innerHTML = 'Number of visits: ' + numberOfVisits;
                canvas.appendChild(nVisitors);
            } else {
                console.warn('[Analytics] No results found');
            }
        } else {
            console.error('There was a problem with the request.');
        }
    }
}

