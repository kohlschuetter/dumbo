const jsonUrl = '<%@page session="false" contentType="application/javascript" %><%= application.getAttribute("jsonPath") %>';
const contextPath = '<%= application.getContextPath() %>';

var rpc = null;
var commands = {};

const delayStepInitial = 8;
var delayStep = delayStepInitial;

const chunkJob = function(chunk, err) {
    if (err != null) {
        if (err.code != 0) {
            console.error("requestNextChunk error", err);
        }

        self.postMessage({ command: "error", error: err });
        return;
    } else {
        delayStep = delayStepInitial;
        if (chunk == null || chunk == "") {
            commands["next"]();
            return;
        }
    }
    self.postMessage({ command: "chunk", chunk: chunk });
};

const nextChunkCall = function() {
    rpc.ConsoleService.requestNextChunk(chunkJob);
};

commands["next"] = function(data) {
    if (data && data.delay) {
        setTimeout(nextChunkCall, data.delay);
    } else {
        nextChunkCall();
    }
};

commands["init"] = function(data) {
    var url = data && data.url ? data.url : jsonUrl;

    self.importScripts(contextPath + "/js/jsonrpc.js");

    rpc = new JSONRpcClient(function(res, err) {
        if (!res || err) {
            console.error("JSONRpcClient error", res, err);
            self.postMessage({ command: "error", error: err });
        }
        commands["next"]();
    }, url);

    var methods = ["ConsoleService.requestNextChunk"]; // optimized
    rpc._fetchMethods(methods);
};

self.onmessage = function(ev) {
    if (!ev || !ev.data || !ev.data.command) {
        return;
    }

    var f = commands[ev.data.command];
    if (f) {
        f(ev.data);
    }
};
