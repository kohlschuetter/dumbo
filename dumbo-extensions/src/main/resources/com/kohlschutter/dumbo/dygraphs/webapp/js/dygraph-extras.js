(function($) {
    $(document).ready(function() {

        if (typeof $.ext == "undefined") {
            $.ext = {};
        }

        $.ext.Dygraph = function(elem, options) {
            var data = [[new Date(), 0, 0]];
            var g = new Dygraph($(elem).get(0), data, options);

            $(elem).data("control", {
                addValues: function(values) {

                    if (data.length == 1) {
                        data = [];
                        data.push([new Date()].concat(values));
                    }

                    data.push([new Date()].concat(values));

                    if (this.doUpdateTimeout == null) {
                        var me = this;
                        this.doUpdateTimeout = window.setTimeout(function() {
                            me.doUpdate();
                        }, 1000);
                    }
                },
                graph: g,
                data: data,

                doUpdate: function() {
                    this.doUpdateTimeout = null;
                    g.updateOptions({
                        "file": data
                    });
                },
                doUpdateTimeout: null
            });

            // fix bootstrap issue
            $('a[data-toggle="tab"]').on('shown.bs.tab', function(e) {
                g.resize();
            })
        };

    });
})(jQuery);
