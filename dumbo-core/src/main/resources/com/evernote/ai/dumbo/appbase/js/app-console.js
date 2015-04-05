(function($) {
  if (location.search == "?static") {
    return;
  }

  $.app.console = new Object();

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
  
  $.app.console.objConverter = $.app.console.defaultObjConverter;
  $.app.console.target = null;

  $.fn.appConsole = function(objConverter) {
    $.app.console.target = $(this);
    if($(this).length == 0) {
      $.app.console.target = null;
    }

    if (objConverter != null) {
      $.app.console.objConverter = objConverter; 
    }

    return this;
  };

  var processChunk = function(chunk) {
    if (chunk == null) {
      // console was closed
      $.app.console.onClose();
      return false;
    } else if (chunk == "") {
      // no-op, continue
      return true;
    } else if (chunk.javaClass == "com.evernote.ai.dumbo.console.Console$ShutdownNotice") {
      // close console and shutdown app as if the window was closed.

      if (chunk.clean) {
        // don't show modal on clean shutdowns
        $("#noLongerCurrentModal").remove();
      }
      $.rpc.AppControlService.notifyAppUnload($.app.ASYNC_IGNORE_RESPONSE,
          $.app.id);
      $.app.console.onClose();
      return false;
    } else if (chunk.javaClass == "com.evernote.ai.dumbo.console.Console$MultipleChunks") {
      $.each(chunk.chunks, function(key, value) {
        processChunk(value);
      });
      return true;
    }

    chunk = $.app.console.objConverter(chunk);
    if (chunk != null) {
      var target = $.app.console.target;
      if(target == null) {
        console.log(chunk);
      } else {
        target.append(chunk);
      }
    }
    return true;
  }

  var chunkJob = function(chunk, e) {
    if (e != null) {
      console.error("ConsoleService.requestNextChunk threw exception", e);
      chunk = null;
    }

    if (processChunk(chunk)) {
      $.rpc.ConsoleService.requestNextChunk(chunkJob, $.app.id);
    } else {
      console.log("Console service stopped");
    }
  };

  $.app.whenReady(function() {
    if ($.rpc.ConsoleService) {
      $.rpc.dontQueueCalls = true;

      setTimeout(function(){
        $.rpc.ConsoleService.requestNextChunk(chunkJob, $.app.id);
      },0);
    }
  });

}(jQuery));
