<!--
var AutoRefreshManager = Class.create();
AutoRefreshManager.prototype = {
  initialize: function(handle, track) {
    this.handle = handle;
    this.track = track;
    this.periodicalExecuter = null;
  },

  observeSlider: function() {
    var startExecutor = this.startExecutor.bind(this);
    new Control.Slider(this.handle, this.track,
      {
        range:$R(0,10),
        values:[0,1,2,3,4,5,6,7,8,9,10],
        onChange:function(pauseTime) {
          startExecutor(pauseTime);
        }
      }
    );
  },

  startExecutor: function(pauseTime) {
    if (this.periodicalExecuter) {
      this.periodicalExecuter.stop();
    }
    if (pauseTime > 0) {
      $(seconds).innerHTML = "Auto-refresh every " + pauseTime + " secs.";
      var refresh = this.refresh.bind(this);
      this.periodicalExecuter = new PeriodicalExecuter(refresh, pauseTime);
    } else {
      $(seconds).innerHTML = "Use this slider to auto-refresh";
    }
  },
  
  refresh: function() {
    document.refreshForm.refresh.click();
  }
}
-->
