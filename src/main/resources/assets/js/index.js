xy = null;
showPoint = null;
showLine = null;

$(document).ready(function () {
  var points = [];
  var markers = [];
  var polyline = null;
  var showPois = true; // default status for navigation
  var map = L.map('map').setView([48.75969691865349, 9.181823730468752], 10);
  var greenIcon = new L.Icon({
    iconUrl: 'https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
  });
  var redIcon = new L.Icon({
    iconUrl: 'https://cdn.rawgit.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
  });

  showPoint = function(lat, lng) {
    L.marker([lat, lng], {icon: redIcon}).addTo(map);
  };

  showLine = function(points) {
    var polyline = L.polyline(points).addTo(map);
    map.fitBounds(polyline.getBounds());
  };

  xy = function(lat, lng, dist) {
    $.ajax({
      url: '/api/route/points?lat=' + (!!lat ? lat : '48.72819563976143') +
      '&lon=' + (!!lng ? lng : '9.124341895803811') + '&dist=' + (!!dist ? dist : 80),
      type: 'GET'
    }).done(function (data) {
      console.log(data.points);
      data.points.forEach(function (value) {
        console.log(value);
        L.marker([value[0], value[1]]).addTo(map);
      });
      map.fitBounds(data.points);
    });
  };

  // initialize map with mapbox tiles
  L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/streets-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1Ijoic2VlZWJpaWkiLCJhIjoiY2l5NGpxc21wMDAxMTMycWg5ZWNlODg3MCJ9.l-3rv7-j3rxd8iSjIqZfqw', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
    maxZoom: 18
  }).addTo(map);

  $('.tabs button').on('click', function () {
    var buttonValue = $(this).val();
    var otherValue = (buttonValue == 'pois') ? 'routing' : 'pois';

    $('.tabs button[value="' + buttonValue + '"]').addClass('active');
    $('.tabs button[value="' + otherValue + '"]').removeClass('active');
    $('#' + buttonValue).show();
    $('#' + otherValue).hide();

    showPois = buttonValue == 'pois';
  });

  // register click and change handler
  map.on('click', function (e) {
    console.log('point: ', e.latlng);
    if ((showPois && points.length >= 1) || (!showPois && points.length >= 2)) {
      resetMap();
    }

    addPointToMap(e.latlng, {icon: greenIcon});

    if (points.length > 1) {
      sendRequest(points[0], points[1]);
    }
  });

  $('#vehicle, #mode').on('change', function () {
    if (showPois) {
      return;
    }

    removePolyline();

    if (points.length > 1) {
      sendRequest(points[0], points[1]);
    }
  });

  $('button[name="search"]').on('click', function () {
    $('#error').hide();
    if (!showPois) {
      return;
    }

    if (points.length == 0) {
      $('#error').html('You have to select a point first.').show();
      return;
    }

    getPois();
  });

  $('button[name="route_clear"]').on('click', function () {
    resetMap();
    $('#error').hide();
    $('#route_from').val('');
    $('#route_to').val('');
  });

  // helper functions
  function getPois() {
    removePolyline();
    removePointsExceptFirst();
    removeMarkersExceptFirst();
    var distance = $('#maxDistance').val();
    sendRequestToPois(points[0], distance);
  }

  function sendRequestToPois(startPoint, distance) {
    $('#error').hide();
    var url = '/api/pois?lat=' + startPoint[0] + '&lon=' + startPoint[1] + '&maxDistance=' + distance;
    if (startPoint.length > 2) {
      url += '&pid=' + startPoint[2];
    }
    url += '&typeKey=' + $('#typeKey').val();
    url += '&typeValue=' + $('#typeValue').val();
    console.log('Sending request to: ', url);
    $.ajax({
      url: url,
      type: 'GET'
    }).done(function (success) {
      if (success && success.poiList.points) {
        if (success.poiList.points.length == 0) {
          $('#error').html('Could not find any POIs nearby. Please increase the distance or choose another POI type.').show();
        } else {
          removePoints();
          removeMarkers();
          addPointToMap(success.startPoint, {icon: greenIcon});
          for (var i = 0; i < success.poiList.points.length; i++) {
            addPointToMap(success.poiList.points[i], {clickHandler: routeToPoi});
          }
          map.fitBounds(points);
        }
      }
    }).fail(function (error) {
      $('#error').html('Something went wrong. Error message: ' + error.responseText).show();
      console.error('Error occurred while retrieving route between two points.', error);
    });
  }

  function routeToPoi(e) {
    // show routing tab
    $('.tabs button[value="routing"]').trigger('click');

    // do actual routing stuff
    var startPoint = points[0];
    var filtered = points.filter(function (value) {
      return value[0] == e.latlng.lat && value[1] == e.latlng.lng;
    });
    var endPoint = filtered[0];
    removePolyline();
    removeMarkersExceptFirst();
    removePointsExceptFirst();
    addPointToMap(endPoint);
    sendRequest(startPoint, endPoint);
  }

  function sendRequest(startPoint, endPoint) {
    $('#error').hide();
    $('#route_from').val(pointToString(startPoint));
    $('#route_to').val(pointToString(endPoint));
    var url = getRequestUrl(startPoint, endPoint);
    console.log('Sending request to: ', url);
    $.ajax({
      url: url,
      type: 'GET'
    }).done(function (success) {
      polyline = L.polyline(success.points, {color: 'blue'}).addTo(map);
      map.fitBounds(polyline.getBounds());

      $('#estimatedDistance').val(success.distance);
    }).fail(function (error) {
      $('#error').html('Something went wrong. Error message: ' + error.responseText).show();
      console.error('Error occurred while retrieving route between two points.', error);
    });
  }

  function getRequestUrl(startPoint, endPoint) {
    var params = '?lat1=' + startPoint[0] + '&lon1=' + startPoint[1] + '&lat2=' + endPoint[0] + '&lon2=' + endPoint[1];
    if (startPoint.length > 2) {
      params += '&pid1=' + startPoint[2];
    }
    if (endPoint.length > 2) {
      params += '&pid2=' + endPoint[2];
    }
    var vehicle = $('#vehicle').val();
    params += '&vehicle=' + vehicle;
    var mode = $('#mode').val();
    params += '&mode=' + mode;
    return '/api/route' + params;
  }

  function resetMap() {
    removePolyline();
    removeMarkers();
    removePoints();
  }

  function removePoints() {
    points = [];
  }

  function removePointsExceptFirst() {
    if (points.length == 1) {
      return;
    }

    // removes all elements except the first one
    points.splice(1, points.length);
  }

  function removeMarkers() {
    for (var i = 0; i < markers.length; i++) {
      map.removeLayer(markers[i]);
    }
    markers = [];
  }

  function removeMarkersExceptFirst() {
    if (markers.length == 1) {
      return;
    }

    // removes all elements except the first one
    for (var i = 1; i < markers.length; i++) {
      map.removeLayer(markers[i]);
    }
    markers.splice(1, markers.length);
  }

  function removePolyline() {
    if (polyline != null) {
      map.removeLayer(polyline);
    }
  }

  function addPointToMap(point, options) {
    options = options || {};

    if (Array.isArray(point)) {
      points.push(point);
    } else {
      points.push([point.lat, point.lng || point.lon, point.id || -1]);
    }

    var marker = null;

    if (options.icon) {
      marker = L.marker(point, {icon: options.icon});
    } else {
      marker = L.marker(point);
    }

    if (options.clickHandler) {
      marker.on('click', options.clickHandler);
    }

    markers.push(marker.addTo(map));
  }

  function pointToString(point) {
    if (!!point && point.length >= 2) {
      return point[0] + ', ' + point[1];
    } else {
      return 'Missing point data.';
    }
  }

  function getPoiOptions() {
    $.ajax({
      url: '/api/pois',
      type: 'OPTIONS'
    }).done(function (result) {
      // add keys to select
      var keys = Object.keys(result);
      var typeKey = $('#typeKey');
      keys.sort().forEach(function (value) {
        typeKey.append('<option>' + value + '</option>')
      });

      // add click handler to change values appropriately
      typeKey.on('click', function (e) {
        var key = $(this).val();
        var values = result[key];
        var typeValue = $('#typeValue');
        typeValue.empty();
        values.sort().forEach(function (value) {
          typeValue.append('<option>' + value + '</option>');
        });
      });

      // trigger a click to add initial values
      typeKey.trigger('click');
    }).fail(function (error) {
      console.log('error occurred while retrieving POI types from server.', error);
    });
  }

  function getMetaData() {
    $.ajax({
      url: '/api/meta/system',
      type: 'GET'
    }).done(function (result) {
      var footerText = 'Server started with data from file <i>' + result.osmFile + '</i>.';
      footerText += ' Nodes: ' + result.nodes + ', Edges: ' + result.edges;
      $('#footer').html(footerText);
    }).fail(function (error) {
      console.log('error occurred while retrieving meta data from server.', error);
    });
  }

  getMetaData();
  getPoiOptions();
});