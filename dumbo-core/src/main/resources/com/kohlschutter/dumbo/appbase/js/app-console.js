(function($) {
    $.app.console = new Object();

    if (location.search == "?static") {
        $.app.console.whenLoaded = function(f) { $.app.whenReady(f); };
        return;
    }

    $.app.console.templates = new Object();
    $.app.console.templates.exception = $($
        .parseHTML("<div class=\"app-console-exception app-default\"><div class=\"app-javaClass\"></div><div class=\"app-exceptionMessage\"></div></div>"));
    $.app.console.templates.plaintext = $($
        .parseHTML("<div class=\"app-console-plaintext app-default\"><div class=\"app-text\"></div></div>"));
    $.app.console.templates.unknown = $($
        .parseHTML("<div class=\"app-console-unknown app-default\"><div class=\"app-json\"></div></div>"));

    $.app.console.defaultObjConverter = function(chunk) {
        if (chunk == null) {
            return null;
        }

        var noTarget = $($.app.console.target).length == 0;

        if (typeof chunk == "string") {
            if (noTarget) {
                return chunk;
            } else {
                var elem = $.app.console.templates.plaintext.clone(true);
                elem.find(".app-text").text(chunk);

                return elem;
            }
        }

        if (typeof chunk.cause != "undefined"
            && typeof chunk.javaClass != "undefined"
            && typeof chunk.message != "undefined"
            && typeof chunk.stackTrace != "undefined") {
            // probably an exception
            var elem = $.app.console.templates.exception.clone(true);
            elem.find(".app-javaClass").text(chunk.javaClass);
            elem.find(".app-exceptionMessage").text(chunk.message);

            console.error(chunk);

            if (noTarget) {
                return null;
            } else {
                return elem;
            }
        }

        if (typeof o == "object") {
            if (o._ == "ClearConsole") {
                $($.app.console.target).empty();
                return null;
            }
        }

        if (noTarget) {
            return null;
        }

        var elem = $.app.console.templates.unknown.clone(true);
        elem.find(".app-json").append(JSON.stringify(chunk, null, 2));
        return elem;
    };

    $.app.console.onClose = function() {

    };
    $.app.console._onClose = function() {
        if ($.app.console.worker) {
            $.app.console.worker.terminate();
            $.app.console.worker = null;
        }
        $.app.console.onClose();
    };

    $.app.console.objConverter = $.app.console.defaultObjConverter;
    $.app.console.target = null;

    $.fn.appConsole = function(objConverter) {
        $.app.console.target = $(this);
        if ($(this).length == 0) {
            $.app.console.target = null;
        }

        if (objConverter != null) {
            $.app.console.objConverter = objConverter;
        }

        return this;
    };

    var connProblemsTimeoutId = -1;
    var connProblemsCheckEnabled = true;
    const connProblems = function() {
        console.log("connection problems");

        var callout = $('#noLongerCurrentTopCallout');
        if (callout.size() >= 0) {
            $('BODY').addClass("noLongerCurrent");
            callout.removeClass("bs-callout-danger");
            callout.addClass("bs-callout-info");
            callout.text("There are connection problems.");
            callout.removeClass("hidden");
        }
    };
    const connProblemsCheck = function() {
        if (connProblemsCheckEnabled) {
            if (connProblemsTimeoutId == -1) {
                connProblemsTimeoutId = setTimeout(connProblems, 1000);
            }
        } else {
            connProblemsTimeoutId = -1;
        }
    };
    const connProblemsGone = function() {
        if (connProblemsTimeoutId == -1) {
            return;
        }
        clearTimeout(connProblemsTimeoutId);
        connProblemsTimeoutId = -1;
        $('#noLongerCurrentTopCallout').addClass("hidden");
        $('BODY').removeClass("noLongerCurrent");
    };

    var processChunk = function(chunk) {
        if (chunk == null) {
            // console was closed
            $.app.console._onClose();
            return false;
        } else if (chunk == "") {
            // no-op, continue
            return true;
        } else if (chunk.javaClass == "com.kohlschutter.dumbo.ConsoleImpl$MultipleChunks") {
            $.each(chunk.chunks, function(_, value) {
                processChunk(value);
            });
            return true;
        } else if (chunk.javaClass == "com.kohlschutter.dumbo.ConsoleImpl$ShutdownNotice") {
            // close console and shutdown app as if the window was closed.
            connProblemsCheckEnabled = false;
            connProblemsGone();

            if (chunk.clean) {
                // don't show modal on clean shutdowns
                $("#noLongerCurrentModal").remove();
            }
            $.rpc.AppControlService.notifyAppUnload($.app.ASYNC_IGNORE_RESPONSE,
                $.app.id);
            $.app.console._onClose();
            return false;
        }

        chunk = $.app.console.objConverter(chunk);
        if (chunk != null) {
            var target = $.app.console.target;
            if (target == null) {
                console.log(chunk);
            } else {
                target.append(chunk);
            }
        }
        return true;
    }

    const useWebWorkers = window.Worker != null;

    const noWorkerLoop = useWebWorkers ? null : function() {
        $.rpc.ConsoleService.requestNextChunk(chunkJob);
    };
    const delayStepInitial = 8;
    var delayStep = delayStepInitial;

    const checkError = function(e) {
        connProblemsCheck();
        if (e) {
            console.error("ConsoleService.requestNextChunk error", e);
            if (e.code == 403) {
                // forbidden, e.g., session no longer valid
                processChunk(null);
                return true;
            }
        }
        return false;
    };

    var chunkJob = useWebWorkers ? null : function(chunk, e) {
        if (e != null) {
            if (checkError(e)) {
                return;
            }

            setTimeout(noWorkerLoop, 2 ** (delayStep));
            if (++delayStep >= 12) {
                delayStep = 12;
            }
            return;
        }
        connProblemsGone();

        if (processChunk(chunk)) {
            $.rpc.ConsoleService.requestNextChunk(chunkJob);
        } else {
            connProblemsCheck();
            console.log("Console service stopped");
        }
    };

    $.app.whenReady(function() {
        if ($.rpc.ConsoleService) {
            // $.rpc.dontQueueCalls = true;

            setTimeout(function() {
                if (noWorkerLoop) {
                    noWorkerLoop();
                    return;
                }

                var worker = $.app.console.worker = new Worker("/_app_base/js/app-console-webworker.js");
                worker.onmessage = function(e) {
                    if (e && e.data) {
                        if (e.data.command == "chunk") {
                            connProblemsGone();
                            if (processChunk(e.data.chunk)) {
                                worker.postMessage({ command: "next" });
                            } else {
                                // FIXME proper shutdown?
                                connProblemsCheck();
                                console.log("Console service stopped");
                            }
                        } else if (e.data.command == "error") {
                            checkError(e.data.error);
                        }
                    }
                };
                worker.postMessage({ command: "init", appId: $.app.id, pageId: $.app.pageId });
            }, 0);
        }
    });

}(jQuery));
