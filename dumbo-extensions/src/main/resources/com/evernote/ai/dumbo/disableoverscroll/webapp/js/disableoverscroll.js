$.app.whenLoaded(function() {
  $(document.body).on("mousewheel", function(e) {
    if (e.originalEvent.wheelDeltaY > 0) {
      window.scrollTo(0, 1);
    }
  });
});
