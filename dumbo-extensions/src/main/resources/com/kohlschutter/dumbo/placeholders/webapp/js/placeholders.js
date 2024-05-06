Dumbo.whenReady(function() {
    setTimeout(() => document.body.classList.add("dumbo-ready"), 0);
});
Dumbo.whenStatic(function() {
    document.body.classList.add("dumbo-ready");
});
