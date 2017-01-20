$(document).ready(function () {
  var points = [];
  var markers = [];
  var polyline = null;
  var map = L.map('map').setView([48.75969691865349, 9.181823730468752], 10);

  // initialize map with mapbox tiles
  L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/streets-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1Ijoic2VlZWJpaWkiLCJhIjoiY2l5NGpxc21wMDAxMTMycWg5ZWNlODg3MCJ9.l-3rv7-j3rxd8iSjIqZfqw', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18
  }).addTo(map);

  // register click and change handler
  map.on('click', function(e) {
    resetMap();

    points.push(e.latlng);
    markers.push(L.marker(e.latlng).addTo(map));

    if (points.length > 1) {
      sendRequest();
    }
  });

  $('#vehicle, #mode').on('change', function () {
    resetPolyline();

    if (points.length > 1) {
      sendRequest();
    }
  });

  // helper functions
  function sendRequest() {
    var startPoint = points[points.length - 2];
    var endPoint = points[points.length - 1];
    var url = getRequestUrl(startPoint, endPoint);
    console.log('Sending request to: ', url);
    $.ajax({
      url: url,
      type: 'GET'
    }).done(function (success) {
      polyline = L.polyline(success.points, { color: 'blue'}).addTo(map);
      map.fitBounds(polyline.getBounds());
    }).fail(function (error) {
      console.log('error', error);
    });
  }

  function getRequestUrl(startPoint, endPoint) {
    var params = '?lat1=' + startPoint.lat + '&lon1=' + startPoint.lng + '&lat2=' + endPoint.lat + '&lon2=' + endPoint.lng;
    var vehicle = $('#vehicle').val();
    params += '&vehicle=' + vehicle;
    var mode = $('#mode').val();
    params += '&mode=' + mode;
    return 'api/route' + params;
  }

  function resetMap() {
    if (points.length > 1) {
      // reset map upfront
      resetPolyline();
      for (var i = 0; i < points.length; i++) {
        map.removeLayer(markers[i]);
      }
      markers = [];
      points = [];
    }
  }

  function resetPolyline() {
    if (polyline != null) {
      map.removeLayer(polyline);
    }
  }
});