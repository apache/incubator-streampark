(function ($) {
    'use strict';

    var aplandWindow = $(window);

    // ------------------
    // :: Nav Active Code
    // ------------------
    if ($.fn.classyNav) {
        $('#aplandNav').classyNav();
    }

    // -----------------------------
    // :: Welcome Slider Active Code
    // -----------------------------
    if ($.fn.owlCarousel) {
        var wel_slides = $('.hero-slides');
        wel_slides.owlCarousel({
            items: 1,
            loop: true,
            nav: true,
            navText: ['<i class="lni-chevron-left"></i>', '<i class="lni-chevron-right"></i>'],
            dots: false,
            dotsSpeed: 1000,
            autoplay: true,
            smartSpeed: 1000,
            autoplayHoverPause: false
        });

        wel_slides.on('translate.owl.carousel', function () {
            var layer = $("[data-animation]");
            layer.each(function () {
                var anim_name = $(this).data('animation');
                $(this).removeClass('animated ' + anim_name).css('opacity', '0');
            });
        });

        $("[data-delay]").each(function () {
            var anim_del = $(this).data('delay');
            $(this).css('animation-delay', anim_del);
        });

        $("[data-duration]").each(function () {
            var anim_dur = $(this).data('duration');
            $(this).css('animation-duration', anim_dur);
        });

        wel_slides.on('translated.owl.carousel', function () {
            var layer = wel_slides.find('.owl-item.active').find("[data-animation]");
            layer.each(function () {
                var anim_name = $(this).data('animation');
                $(this).addClass('animated ' + anim_name).css('opacity', '1');
            });
        });
    }

    // ---------------------------
    // :: Testimonials Active Code
    // ---------------------------
    if ($.fn.owlCarousel) {
        $(".testimonials").owlCarousel({
            items: 1,
            margin: 0,
            loop: true,
            nav: true,
            dots: true,
            autoplay: true,
            navText: ['<i class="lni-arrow-left"></i>', '<i class="lni-arrow-right"></i>'],
            smartSpeed: '1000'
        });
    }

    // ------------------------------
    // :: App Screenshots Active Code
    // ------------------------------
    if ($.fn.owlCarousel) {
        $(".app_screenshots").owlCarousel({
            items: 4,
            margin: 30,
            loop: true,
            nav: false,
            dots: true,
            autoplay: true,
            autoplayTimeout: 3000,
            smartSpeed: 1000,
            responsive: {
                0: {
                    items: 2
                },
                576: {
                    items: 2
                },
                768: {
                    items: 3
                },
                992: {
                    items: 4
                }
            }
        });
    }

    // -------------------------------
    // :: Partner Carousel Active Code
    // -------------------------------
    if ($.fn.owlCarousel) {
        $(".our-partner-slides").owlCarousel({
            items: 6,
            margin: 50,
            loop: true,
            nav: false,
            dots: false,
            autoplay: true,
            autoplayTimeout: 3000,
            smartSpeed: 1000,
            responsive: {
                0: {
                    items: 3
                },
                576: {
                    items: 4
                },
                768: {
                    items: 5
                },
                992: {
                    items: 6
                }
            }
        });
    }

    // --------------------------
    // :: Onepage Nav Active Code
    // --------------------------
    if ($.fn.onePageNav) {
        $('#corenav').onePageNav({
            easing: 'easeInOutQuart',
            scrollSpeed: 1440
        });
    }

    // --------------------
    // :: Video Active Code
    // --------------------
    if ($.fn.magnificPopup) {
        $('.video_btn').magnificPopup({
            type: 'iframe'
        });
    }

    // -----------------------
    // :: ScrollUp Active Code
    // -----------------------
    if ($.fn.scrollUp) {
        $.scrollUp({
            scrollSpeed: 1500,
            easingType: 'easeInOutQuart',
            scrollText: ['<i class="lni-chevron-up"></i>'],
            scrollImg: false
        });
    }

    // ----------------------
    // :: Tooltip Active Code
    // ----------------------
    if ($.fn.tooltip) {
        $('[data-toggle="tooltip"]').tooltip();
    }

    // ------------------------
    // :: Counterup Active Code
    // ------------------------
    if ($.fn.counterUp) {
        $('.counter').counterUp({
            delay: 10,
            time: 1500
        });
    }

    // ---------------------
    // :: Sticky Active Code
    // ---------------------
    if ($.fn.sticky) {
        $(".main_header_area").sticky({
            topSpacing: 0
        });
    }

    // ------------------
    // :: wow Active Code
    // ------------------
    if (aplandWindow.width() > 767) {
        new WOW().init();
    }

    // -----------------------
    // :: Jarallax Active Code
    // -----------------------
    if ($.fn.jarallax) {
        $('.jarallax').jarallax({
            speed: 0.2
        });
    }

    // -------------------------
    // :: PreventDefault a Click
    // -------------------------
    $("a[href='#']").on('click', function ($) {
        $.preventDefault();
    });

})(jQuery);
