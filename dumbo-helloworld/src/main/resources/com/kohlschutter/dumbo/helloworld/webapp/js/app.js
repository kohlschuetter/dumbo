(function(Dumbo) {
    Dumbo.whenLoaded(function() {
    });

    Dumbo.whenStatic(function() {
    });

    Dumbo.whenLive(function() {
        // asynchronous call (good, safe)
        const demoService = Dumbo.getService("com.kohlschutter.dumbo.helloworld.DemoService");

        demoService.hello(function(ret, err) {
            if (err) {
                console.log(err);
                return;
            }

            document.getElementById("rpcResponse").textContent = ret;
        }, false);

        // synchronous call (discouraged, easier to write)
        // document.getElementById("rpcResponse").textContent = demoService.hello(false);
    });

    Dumbo.whenReady(function() {
    });
})(Dumbo);
