<g:javascript library="wadi/CountChart" />

<script type="text/javascript">
var makeCharts = function() {
    var peerNames = [<%=  countingInfos.inject("") { value, countingInfo ->
        if (value == "") {
            return "\"${countingInfo.hostingPeer.name}\"";
        } else {
            return "${value}, \"${countingInfo.hostingPeer.name}\"";
        }
    }
    %>];
    var multiCharts = new MultiCharts(peerNames, ${getCountingInfosAsJSONURL});
    var multiChartsUpdate = multiCharts.update.bind(multiCharts);
    new AutoRefreshManager('handle', 'track', multiChartsUpdate).observeSlider();
};
dojo.addOnLoad(makeCharts);
</script>
