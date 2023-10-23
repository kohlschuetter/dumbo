(function(Dumbo,$) {
    Dumbo.whenLoaded(
        function() {
            $('.dropdown-menu').on('click', function(e) {
                if ($(this).hasClass('dropdown-menu-form')) {
                    e.stopPropagation();
                }
            });
        }
    );
})(Dumbo,jQuery);
