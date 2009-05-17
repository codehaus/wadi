<!--
dojo.require("dojox.charting.Chart2D");
dojo.require("dojox.charting.themes.PlotKit.orange");

var CountChart = Class.create();
CountChart.prototype = {
  initialize: function(peerName, limit, magnitude) {
    this.peerName = peerName;
    this.inCptSerieOffset;
    this.inCptSerie = [];
    this.outCptSerieOffset;
    this.outCptSerie = [];
    for (var i = 0; i < limit; i++) {
      this.inCptSerie.push(0);
      this.outCptSerie.push(0);
    }

    this.chart = new dojox.charting.Chart2D(peerName + "_GRAPH");
    this.chart.setTheme(dojox.charting.themes.PlotKit.orange);
    this.chart.addAxis("x", {fixLower: "minor", natural: true, min: 1, max: limit});
    this.chart.addAxis("y", {vertical: true, min: 0, max: magnitude, majorTickStep: 5, minorTickStep: 5});
    this.chart.addPlot("default", {type: "StackedLines", markers: true, shadows: {dx: 2, dy: 2, dw: 2}});
    this.chart.addPlot("grid", {type: "Grid", hMinorLines: true});
    this.chart.addSeries("In Cpt", this.inCptSerie, {stroke: {color: "blue", width: 2}, fill: "lightblue", marker: "m-3,0 c0,-4 6,-4 6,0 m-6,0 c0,4 6,4 6,0"});
    this.chart.addSeries("Out Cpt", this.outCptSerie, {stroke: {color: "green", width: 2}, fill: "lightgreen", marker: "m0,-3 l3,3 -3,3 -3,-3 z"});
    this.chart.render();
  },

  addCountInfo: function(pauseTime, countInfo) {
    if (this.inCptSerieOffset) {
      this.inCptSerie.shift();
      this.inCptSerie.push((countInfo.inboundEnvelopeCpt - this.inCptSerieOffset) / pauseTime);
      this.inCptSerieOffset = countInfo.inboundEnvelopeCpt;
      this.chart.updateSeries("In Cpt", this.inCptSerie);

      this.outCptSerie.shift();
      this.outCptSerie.push((countInfo.outboundEnvelopeCpt - this.outCptSerieOffset) / pauseTime);
      this.outCptSerieOffset = countInfo.outboundEnvelopeCpt;
      this.chart.updateSeries("Out Cpt", this.outCptSerie);
      this.chart.render();
    } else {
      this.inCptSerieOffset = countInfo.inboundEnvelopeCpt;
      this.outCptSerieOffset = countInfo.outboundEnvelopeCpt;
    }
  }
};

var MultiCharts = Class.create();
MultiCharts.prototype = {
  initialize: function(peerNames, getCountingInfosAsJSONURL) {
    this.getCountingInfosAsJSONURL = getCountingInfosAsJSONURL;
    this.limit = 10;
    this.magnitude = 50;

    this.charts = [];
    var addChartToThis = this.addChart.bind(this);
    peerNames.each(function(peerName) {
        addChartToThis(peerName);
    });
  },
  
  addChart: function(peerName) {
    this.charts.push(new CountChart(peerName, this.limit, this.magnitude));
  },

  update: function(pauseTime) {
    var addCountInfos = this.addCountInfos.bind(this, pauseTime);
    new Ajax.Request(this.getCountingInfosAsJSONURL, {
      method: 'get',
      onSuccess: function(transport) {
       var countInfos = transport.responseText.evalJSON();
        addCountInfos(countInfos);
      }
    });
  },
  
  addCountInfos: function(pauseTime, countInfos) {
    this.charts.each(function(chart) {
      for (var i = 0; i < countInfos.countingInfos.length; i++) {
        var countingInfo = countInfos.countingInfos[i];
        if (countingInfo.hostingPeerName == chart.peerName) {
          chart.addCountInfo(pauseTime, countingInfo);
        }
      }
    });
  }
};
-->