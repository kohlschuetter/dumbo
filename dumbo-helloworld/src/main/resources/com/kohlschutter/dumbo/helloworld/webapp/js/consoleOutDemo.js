(function(Dumbo) {
    if (location.search == "?static") {
        return;
    }

    let consoleElement = document.getElementById("console");
    consoleElement.innerHTML = "";

    Dumbo.whenLoaded(() =>
        Dumbo.setConsole(consoleElement)
    );
})(Dumbo);
