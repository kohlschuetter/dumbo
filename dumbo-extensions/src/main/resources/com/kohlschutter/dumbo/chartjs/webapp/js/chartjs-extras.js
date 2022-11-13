(function($) {
    $(document).ready(
        function() {

            if (typeof $.ext == "undefined") {
                $.ext = {};
            }
            if (typeof $.ext.Chart == "undefined") {
                $.ext.Chart = {};
            }

            $.ext.Chart.Bar = function(canvasElem, data, options) {
                var chart = new Chart($(canvasElem).get(0).getContext("2d")).Bar(
                    data, options);

                var control = {
                    update: function(label, absVals) {
                        if (chart.datasets[0].bars.length == 0) {
                            // uninitialized
                            chart.addData(absVals, label);
                            chart.addData(absVals, label);
                            chart.removeData();
                            return;
                        }

                        var barIndex = -1;
                        var ds = chart.datasets[0];
                        for (var i in ds.bars) {
                            if (ds.bars[i].label == label) {
                                barIndex = i;
                                break;
                            }
                        }

                        if (barIndex == -1) {
                            // new label
                            chart.addData(absVals, label);
                            return;
                        }

                        for (var i in absVals) {
                            chart.datasets[i].bars[barIndex].value = absVals[i];
                        }
                        if (this.doUpdateTimeout == null) {
                            var me = this;
                            this.doUpdateTimeout = window.setTimeout(function() {
                                me.doUpdate();
                            }, this.updateInterval);
                        }
                    },
                    chart: chart,
                    doUpdate: function() {
                        this.doUpdateTimeout = null;
                        chart.update();
                    },
                    doUpdateTimeout: null,
                    updateInterval: 2000
                };

                $(canvasElem).data("control", control);

                return control;
            };
        });
})(jQuery);
