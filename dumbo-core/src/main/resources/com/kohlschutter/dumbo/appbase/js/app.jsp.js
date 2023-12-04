var Dumbo;
(function (Dumbo) {
    Dumbo.getService = function(id) {
        var unaliased = Dumbo.serviceAliases[id];
        if (unaliased) {
            id = unaliased; // for jacline/j2cl
        }
        if (id && id.__class) {
            id = id.__class; // for jsweet
        }
        var service = Dumbo.rpc[id];
        if (!service && id && id.endsWith && id.endsWith("Async")) {
            id = id.substring(0, id.length - 5);
            service = Dumbo.rpc[id];
        }

        if (!service) {
            console.error("Cannot resolve Dumbo service: " + id);
            throw new Error("Cannot resolve Dumbo service: " + id);
        }

        return service;
    };

    Dumbo.setServiceAlias = function(al,service) {
        Dumbo.serviceAliases[al] = service;
    };

    const jsonUrl = '<%@page session="false" contentType="application/javascript; charset=UTF-8" %><%= application.getAttribute("jsonPath") %>';

    Dumbo.rpc = null;
    Dumbo.serviceAliases = {};
    Dumbo.app = {
        proto: {}
    };

    /**
     * A reusable dummy callback method that simply ignores the result.
     */
    Dumbo.app.ASYNC_IGNORE_RESPONSE = function(_, _) { };

    Dumbo.app._onLoadedCallbacks = [];
    Dumbo.app._onLiveModeCallbacks = [];
    Dumbo.app._onLiveModeCallbacks.condition = function() {
        return location.search != "?static";
    };
    Dumbo.app._onStaticModeCallbacks = [];
    Dumbo.app._onStaticModeCallbacks.condition = function() {
        return location.search == "?static";
    };
    Dumbo.app._onReadyCallbacks = [];

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
            for (var i = 0, n = list.length; i < n; i++) {
                const el = list[i];
                if (el) {
                    window.setTimeout(function() {
                        el();
                    }, 0);
                }
            }
        }
        if (list.afterwards) {
            window.setTimeout(function() {
                list.afterwards();
                list.afterwards = null;
            }, 0);
        }
        list.length = 0;
        list.done = true;
    };

    /**
     * Dummy method to prevent dead code removal by Closure etc.
     */
    Dumbo.keep = function(obj) { };

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is loaded. If the app is already initialized, the function
     * is called immediately (via a timeout).
     */
    Dumbo.whenLoaded = function(f) {
        _addCallback(Dumbo.app._onLoadedCallbacks, f);
    };

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is in live mode.
     */
    Dumbo.whenLive = function(f) {
        _addCallback(Dumbo.app._onLiveModeCallbacks, f);
    };

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is in live mode.
     */
    Dumbo.whenStatic = function(f) {
        _addCallback(Dumbo.app._onStaticModeCallbacks, f);
    };

    /**
     * Adds a function to the list of callbacks that should be called as soon as
     * the application is ready. If the app is already initialized, the function
     * is called immediately (via a timeout).
     */
    Dumbo.whenReady = function(f) {
        _addCallback(Dumbo.app._onReadyCallbacks, f);
    };

    Dumbo.parseHTML = function(t) {
        var doc = document.implementation.createHTMLDocument("");
        var base = doc.createElement("base");
        base.href = document.location.href;

        var node = doc.createElement("div");
        doc.body.appendChild(node);

        doc.head.appendChild(base);

        node.innerHTML = t;

        return node;
    };

    Dumbo.clone = function(n) {
        if (typeof n == "string") {
            n = document.querySelector(n);
        }

        var cloned = n.cloneNode(true);

        // FIXME re-attach event listeners

        return cloned;
    };

    Dumbo.cloneTemplate = function(template, n) {
        if (typeof template == "string") {
            template = document.querySelector(template);
        }

        var base = document;
        if (template) {
            base = template;
            if (base.content) {
                base = base.content;
            }
        }

        if (typeof n == "string") {
            n = base.querySelector(n);
        }

        var cloned = n.cloneNode(true);

        // FIXME re-attach event listeners

        return cloned;
    };

    Dumbo.forEach = function(node, selector, op) {
        if (typeof node == "string" && typeof op == "undefined") {
            op = selector;
            selector = node;
            node = document.body;
        }

        node.querySelectorAll(selector).forEach(op);
    };

    Dumbo.setText = function(node, selector, text) {
        if (typeof node == "string" && typeof text == "undefined") {
            text = selector;
            selector = node;
            node = document.body;
        }

        if (text && text.textContent) {
            text = text.textContent;
        }
        if (text == null) {
            text = "";
        } else {
            text = "" + text;
        }
        Dumbo.forEach(node, selector, n => { n.textContent = text; });
    };

    Dumbo.empty = function(n) {
        if (typeof n == "string") {
            n = document.querySelector(n);
        }
        if (n) {
            n.innerHTML = "";
        }
    };

    window.addEventListener('load', 
        function() {
            Dumbo.rpc = new JSONRpcClient(function(_, _) {
                var cb;
                if (Dumbo.app._onStaticModeCallbacks.condition()) {
                    cb = Dumbo.app._onStaticModeCallbacks;
                } else {
                    cb = Dumbo.app._onLiveModeCallbacks;
                }

                cb.afterwards = function() {
                    _runCallbacks(Dumbo.app._onReadyCallbacks);
                };

                Dumbo.app._onLoadedCallbacks.afterwards = function() {
                    _runCallbacks(cb);
                };

                _runCallbacks(Dumbo.app._onLoadedCallbacks);
            }, jsonUrl);
        });

    if (location.search == "?static") {
        let showOutline = (location.hash == "#outline");

        console.log("Static design mode enabled; outline " + (showOutline ? "enabled" : "disabled — enable by adding #outline to URL"));

        Dumbo.whenLoaded(function() {
            var templates = document.getElementsByTagName("template");
            for (var i = 0, n = templates.length; i < n; i++) {
                var template = templates[i];
                var div = document.createElement("DIV");
                div.classList = template.classList;
                div.classList.add("templates");
                div.append(template.content);
                template.parentNode.insertBefore(div, template);
                template.parentNode.removeChild(template);
                div.id = template.id;
            }
            document.body.classList.add("static-design-mode");
            if (showOutline) {
                document.body.classList.add("static-design-mode-outline");
            }
        });
    }
})(Dumbo || (window.Dumbo = Dumbo = {}));
