$.app.whenLoaded(function() {
  $("#link-to-source").attr("href", "view-source:" + location.href);
});

$.app.whenStatic(function() {
});

$.app.whenLive(function() {
  // asynchronous call (good, safe)
  $.rpc.DemoService.hello(function(ret,err) {
    if (err) {
      console.log(err);
      return;
    }
    
    $("#rpcResponse").text(ret);
  }, false);
  
  // synchronous call (discouraged, easier to write)
  // $("#rpcResponse").text(r$.rpc.DemoService.hello(false));
});

$.app.whenReady(function() {
});
