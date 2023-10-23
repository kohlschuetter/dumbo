(function(Dumbo) {
  if (location.search == "?static") {
    return;
  }

  var commandLineIn = function(target) {
    var service;

    target.addEventListener('change', function(_ev) {
      var line = target.value;

      service = service || Dumbo.getService("com.kohlschutter.dumbo.helloworld.console.CommandLineService");

      try {
        service.sendLine(line);
        target.value = "";
      } catch (e) {
        console.error(e);
        alert(e);
      }

      return false;
    });

    let enclosingForm = target.closest("FORM");
    if (enclosingForm) {
        enclosingForm.submit((ev) => ev.preventDefault());
    };
  };

  Dumbo.app.console.templates.exception = Dumbo.clone("#console .app-console-exception");

  var colorMessageProto = Dumbo.clone("#console .color-message");
  Dumbo.empty("#console");

  Dumbo.whenLoaded(
      function() {
        Dumbo.setConsole("#console",
            // Here we define our custom object renderer for "ColorMessage"
            // Try to take this code out (uncomment the null&& line),
            // and see what happens!

            function(chunk) {
              if (typeof chunk == "object") {
                if (typeof chunk.color != "undefined"
                    && typeof chunk.message != "undefined") {
                  var elem = Dumbo.clone(colorMessageProto);
                  var text = elem.querySelector(".message-text");
                  text.style.color = chunk.color;
                  text.textContent = chunk.message;
                  return elem;
                }
              }

              return Dumbo.app.console.defaultObjConverter(chunk);
            });

        // Setup our console input
        commandLineIn(document.getElementById("commandLine"));
      });
})(Dumbo);
