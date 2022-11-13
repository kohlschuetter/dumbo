(function($) {
  if (location.search == "?static") {
    return;
  }

  $.fn.commandLineIn = function() {
    var target = this;
    target.change(function(_ev) {
      var line = target.val();

      try {
        $.rpc.CommandLineService.sendLine(line);
        target.val("");
      } catch (e) {
        console.log(e);
        alert(e);
      }

      return false;
    });

    target.closest("FORM").submit(function(ev) {
      ev.preventDefault();
    });
  };

  $.app.console.templates.exception = $("#console .app-console-exception:first")
      .clone(true);

  var colorMessageProto = $("#console .color-message:first").clone(true);
  $("#console").empty();

  $(document).ready(
      function() {
        $("#link-to-source").attr("href", "view-source:" + location.href);

        $("#console").appConsole(
            // Here we define our custom object renderer for "ColorMessage"
            // Try to take this code out (uncomment the null&& line),
            // and see what happens!

            // null&&
            function(chunk) {
              if (typeof chunk == "object") {
                if (typeof chunk.color != "undefined"
                    && typeof chunk.message != "undefined") {
                  var elem = colorMessageProto.clone(true);
                  var text = elem.find(".message-text");
                  text.css("color", chunk.color);
                  text.text(chunk.message);
                  return elem;
                }
              }

              return $.app.console.defaultObjConverter(chunk);
            });

        // Setup our console input
        $("#commandLine").commandLineIn();
      });
})(jQuery);
