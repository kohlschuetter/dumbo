var appId = null;
var rpc = null;

self.importScripts("/_app_base/js/jsonrpc.js?");

var commands = { };

var chunkJob = function(chunk, err) {
    if (err != null) {
        console.error("requestNextChunk error", err);
        chunk = null;
    } else if (chunk == null || chunk == "") {
        commands["next"]();
        return;
    }
    self.postMessage({command:"chunk", chunk: chunk});
};

commands["next"] = function(data) {
    rpc.ConsoleService.requestNextChunk(chunkJob, appId);
};

commands["init"] = function(data) {
    appId = data.appId;
    rpc = new JSONRpcClient(function(res, err) {
        if (!res || err) {
            console.error("JSONRpcClient error", res, err);
        }
        commands["next"]();
    }, "/json");
};

self.onmessage = function(ev) {
    if (!ev || !ev.data || !ev.data.command) {
        return;
    }

    var f = commands[ev.data.command];
    if (f) {
        f(ev.data);
    }
}
