(function($) {
    var pageId = $dumbo.pageId;

    $.rpc = null;
    $.app = {
        id: null,
        pageId: pageId,
        proto: {}
    };

    /**
     * A reusable dummy callback method that simply ignores the result.
     */
    $.app.ASYNC_IGNORE_RESPONSE = function(_, _) { };

    $.app._onLoadedCallbacks = [];
    $.app._onLiveModeCallbacks = [];
    $.app._onLiveModeCallbacks.condition = function() {
        return location.search != "?static";
    };
    $.app._onStaticModeCallbacks = [];
    $.app._onStaticModeCallbacks.condition = function() {
        return location.search == "?static";
    };
    $.app._onReadyCallbacks = [];

    var _addCallback = function(list, f) {
        if (list.done) {
            if (list.condition == null || list.condition()) {
                window.setTimeout(function() {
                    f();
                }, 0);
            }
        } else {
            list.push(f);
        }
    };

    var _runCallbacks = function(list) {
        if (list.done) {
            return;
        }
        if (list.condition == null || list.condition()) {
            $.each(list, function(key, value) {
                window.setTimeout(function() {
                    value();
                }, 0);
            });
        }
        if (list.afterwards) {
            window.setTimeout(function() {
                list.afterwards();
                list.afterwards = null;
            }, 0);
        }
        list.length = 0;
        list.done = true;
    }

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is loaded. If the app is already initialized, the function
     * is called immediately (via a timeout).
     */
    $.app.whenLoaded = function(f) {
        _addCallback($.app._onLoadedCallbacks, f);
    }

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is in live mode.
     */
    $.app.whenLive = function(f) {
        _addCallback($.app._onLiveModeCallbacks, f);
    }

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is in live mode.
     */
    $.app.whenStatic = function(f) {
        _addCallback($.app._onStaticModeCallbacks, f);
    }

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is ready. If the app is already initialized, the function
     * is called immediately (via a timeout).
     */
    $.app.whenReady = function(f) {
        _addCallback($.app._onReadyCallbacks, f);
    }

    $(document).ready(
        function() {
            $.rpc = new JSONRpcClient(function(ret, e) {
                $.rpc.AppControlService.notifyAppLoaded(function(result, e) {
                    $.app.id = result;
                    $(window).unload(
                        function() {
                            $.rpc.AppControlService.notifyAppUnload(
                                $.app.ASYNC_IGNORE_RESPONSE, $.app.id);
                        });

                    var cb;
                    if ($.app._onStaticModeCallbacks.condition()) {
                        cb = $.app._onStaticModeCallbacks;
                    } else {
                        cb = $.app._onLiveModeCallbacks;
                    }

                    cb.afterwards = function() {
                        _runCallbacks($.app._onReadyCallbacks);
                    };

                    $.app._onLoadedCallbacks.afterwards = function() {
                        _runCallbacks(cb);
                    };

                    _runCallbacks($.app._onLoadedCallbacks);
                });
            }, '/json?pageId=' + pageId);
        });
}(jQuery));
