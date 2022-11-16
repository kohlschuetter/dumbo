(function($) {
    if (location.search == "?static") {
        return;
    }

    $("#console").empty();
    $(document).ready(function() {
        $("#console").appConsole();
    });
})(jQuery);
