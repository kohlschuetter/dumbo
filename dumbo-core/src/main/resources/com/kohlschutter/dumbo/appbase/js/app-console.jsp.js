(function (Dumbo) {
    const contextPath = '<%@page session="false" contentType="application/javascript" %><%= application.getContextPath() %>';
    Dumbo.app.console = new Object();

    if (location.search == "?static") {
        Dumbo.app.console.whenLoaded = function(f) { Dumbo.whenReady(f); };
        return;
    }

    Dumbo.app.console.templates = new Object();
    Dumbo.app.console.templates.exception = Dumbo.parseHTML("<div class=\"app-console-exception app-default\"><div class=\"app-javaClass\"></div><div class=\"app-exceptionMessage\"></div></div>");
    Dumbo.app.console.templates.plaintext = Dumbo.parseHTML("<div class=\"app-console-plaintext app-default\"><div class=\"app-text\"></div></div>");
    Dumbo.app.console.templates.unknown = Dumbo.parseHTML("<div class=\"app-console-unknown app-default\"><div class=\"app-json\"></div></div>");

    Dumbo.app.console.defaultObjConverter = function(chunk) {
        if (chunk == null) {
            return null;
        }

        var noTarget = Dumbo.app.console.target == null;

        if (typeof chunk == "string") {
            if (noTarget) {
                return chunk;
            } else {
                var elem = Dumbo.clone(Dumbo.app.console.templates.plaintext);
                Dumbo.setText(elem, ".app-text", chunk);

                return elem;
            }
        }

        if (typeof chunk.cause != "undefined"
            && typeof chunk.javaClass != "undefined"
            && typeof chunk.message != "undefined"
            && typeof chunk.stackTrace != "undefined") {
            // probably an exception
            var elem = Dumbo.clone(Dumbo.app.console.templates.exception);
            Dumbo.setText(elem, ".app-javaClass", chunk.javaClass);
            Dumbo.setText(elem, ".app-exceptionMessage", chunk.message);

            console.error(chunk);

            if (noTarget) {
                return null;
            } else {
                return elem;
            }
        }

        if (typeof o == "object") {
            if (o._ == "ClearConsole") {
                Dumbo.empty(Dumbo.app.console.target);
                return null;
            }
        }

        if (noTarget) {
            return null;
        }

        var elem = Dumbo.clone(Dumbo.app.console.templates.unknown);

        const jsonText = document.createTextNode(JSON.stringify(chunk, null, 2));
        Dumbo.forEach(elem, ".app-json", (e) => {
            e.appendChild(jsonText);
            });
        return elem;
    };

    Dumbo.app.console.onClose = function() {

    };
    Dumbo.app.console._onClose = function() {
        const worker = Dumbo.app.console.worker;
        if (worker) {
            worker.terminate();
            Dumbo.app.console.worker = null;
        }
        Dumbo.app.console.onClose();
    };

    Dumbo.app.console.objConverter = Dumbo.app.console.defaultObjConverter;
    Dumbo.app.console.target = null;

    Dumbo.consoleDefaultObjConverter = Dumbo.app.console.defaultObjConverter;

    Dumbo.setConsole = function(targetElement, objConverter) {
        if (typeof targetElement == "string") {
            targetElement = document.body.querySelector(targetElement);
        }
        Dumbo.app.console.target = targetElement;
        if (objConverter != null) {
            Dumbo.app.console.objConverter = objConverter;
        }
    };

    var connProblemsTimeoutId = -1;
    var connProblemsCheckEnabled = true;
    const connProblems = function() {
        var callout = document.getElementById("noLongerCurrentTopCallout");
        if (callout) {
            document.body.classList.add("noLongerCurrent");
            var cl = callout.classList;
            cl.remove("bs-callout-danger");
            cl.add("bs-callout-info");
            callout.textContent = "There are connection problems.";
            cl.remove("hidden");
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
        if (connProblemsTimeoutId != -1) {
            clearTimeout(connProblemsTimeoutId);
            connProblemsTimeoutId = -1;
        }
        var callout = document.getElementById("noLongerCurrentTopCallout");
        if (callout) {
            callout.classList.add("hidden");
        }
        document.body.classList.remove("noLongerCurrent");
    };

    if (addEventListener) {
        addEventListener('beforeunload', connProblemsGone);
    }

    var processChunk = function(chunk) {
        connProblemsGone();
        if (chunk == null) {
            // console was closed
            Dumbo.app.console._onClose();
            return false;
        } else if (chunk == "") {
            // no-op, continue
            return true;
        } else if (chunk.javaClass == "com.kohlschutter.dumbo.MultipleChunks") {
            chunk.chunks.forEach(v => processChunk(v));
            return true;
        } else if (chunk.javaClass == "com.kohlschutter.dumbo.ShutdownNotice") {
            // close console and shutdown app as if the window was closed.
            connProblemsCheckEnabled = false;

            if (chunk.clean) {
                // don't show modal on clean shutdowns
                var modal = document.getElementById("noLongerCurrentModal");
                if (modal) {
                    modal.remove();
                }
            }
            Dumbo.app.console._onClose();
            return false;
        }

        chunk = Dumbo.app.console.objConverter(chunk);
        if (chunk != null) {
            var target = Dumbo.app.console.target;
            if (target == null) {
                console.log("Dumbo chunk", chunk);
            } else {
                if (chunk && chunk.get) {
                    chunk = chunk.get();
                }
                target.append(chunk);
            }
        }
        return true;
    }

    const useWebWorkers = window.Worker != null;

    const noWorkerLoop = useWebWorkers ? null : function() {
        Dumbo.getService("ConsoleService").requestNextChunk(chunkJob);
    };
    const delayStepInitial = 8;
    const delayStepMax = 12;
    var delayStep = delayStepInitial;

    const checkError = function(e, step) {
        if (e) {
            // console.error("ConsoleService.requestNextChunk error", e);
            switch (e.code) {
                case 401: // unauthorized
                case 403: // forbidden
                case 410: // gone
                case 501: // not implemented
                    // session no longer valid
                    processChunk(null);
                    return true;
                default:
                    if (step < delayStepMax) {
                        // we're optimistic...
                        break;
                    } else {
                        // ... up to a point.
                        processChunk(null);
                        return true;
                    }
            }
        }
        connProblemsCheck();

        return false;
    };

    var chunkJob = useWebWorkers ? null : function(chunk, e) {
        if (e != null) {
            if (checkError(e, delayStep)) {
                return;
            }

            setTimeout(noWorkerLoop, 2 ** (delayStep));
            if (++delayStep > delayStepMax) {
                delayStep = delayStepMax;
            }
            return;
        }
        connProblemsGone();

        if (processChunk(chunk)) {
            Dumbo.getService("ConsoleService").requestNextChunk(chunkJob);
        } else {
            connProblemsCheck();
            console.log("Console service stopped");
        }
    };

    Dumbo.whenReady(function() {
        const consoleService = Dumbo.getService("ConsoleService");
        if (consoleService) {
            // Dumbo.rpc.dontQueueCalls = true;

            setTimeout(function() {
                if (noWorkerLoop) {
                    noWorkerLoop();
                    return;
                }

                var worker = Dumbo.app.console.worker = new Worker(contextPath + "/js/app-console-webworker.js");

                var workerNextMessage = function(delay) {
                    worker.postMessage({ command: "next", delay: delay });
                }

                worker.onmessage = function(e) {
                    if (e && e.data) {
                        if (e.data.command == "chunk") {
                            if (processChunk(e.data.chunk)) {
                                workerNextMessage(0);
                            } else {
                                // FIXME proper shutdown?
                                connProblemsCheck();
                                console.log("Console service stopped");
                            }
                        } else if (e.data.command == "error") {
                            if (!checkError(e.data.error, delayStep)) {
                                workerNextMessage(2 ** (delayStep));
                                if (++delayStep >= 12) {
                                    delayStep = 12;
                                }
                            }
                        }
                    }
                };
                worker.postMessage({ command: "init", url: Dumbo.rpc.serverURL });
            }, 0);
        }
    });
}(Dumbo));
