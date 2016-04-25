var naturalWidth;
var naturalHeight;
var direction = 0;
var halfStep = 0.1;
var scale = 1;
var image;

function initializeDragScrollZoom() {
	var overlay = $('#scrollDiv');
	var totalHeight = $(window).height();
	var totalWidth = $(window).width();
	var top = $('.header').outerHeight();
	var left = $('.sidebar').outerWidth();
	
	overlay.css('position', 'absolute');
	overlay.css('opacity', '0');
	overlay.css('filter', 'alpha(opacity = 0)');
	overlay.css('background', 'transparent');
	
	overlay.css('top', top);
	overlay.css('left', left);
	overlay.css('width', totalWidth - left);
	overlay.css('height', totalHeight - top);
	overlay.show();
	overlay.css('z-index', '1');
		
	var image = $($("#imgDiv").children("svg:first")[0]);

	naturalWidth = image.width();
	naturalHeight = image.height();

	direction = 0;
	halfStep = 0.1;
	scale = 1;

	var viewWidth = $("#scrollDiv").width();
	var viewHeight = $("#scrollDiv").height();
	
	var divToMove = $("#imgDiv");
	
	left = -1 * (naturalWidth - viewWidth) / 2;
	tp = -1 * (naturalHeight - viewHeight) / 2;

	divToMove.css("top", tp+"px");
	divToMove.css("left", left+"px");
}

;(function($){ // secure $ jQuery alias


function setScale() {
	scale = scale + (direction * halfStep);
	
	if (scale < 0.1 ) scale = 0.1;
	if (scale > 2) scale = 2;

	var newWidth = naturalWidth * scale;
	var newHeight = naturalHeight * scale;
	
	image.width(newWidth);
	image.height(newHeight);		
}
	
$.fn.dragscrollzoom = function( options ){

	$(this).css('overflow', 'hidden');
	$(this).css('z-index', '1');
	$(this).css('height', '100%');
	$(this).css('cursor', 'pointer');
	$(this).bind("mousewheel", function() {
        return false;
    });
	
	var divToMove = $("#imgDiv");

	divToMove.css("z-index", "-1");
	divToMove.css("position", "absolute");
	divToMove.css("top", "0px");
	divToMove.css("left", "0px");
	
	var settings = $.extend(
		{   
			dragSelector:'>:first',
			acceptPropagatedEvent: true,
            preventDefault: true
		},options || {});
	 
	
	var dragscroll = {
		mouseDownHandler : function(event) {
			// mousedown, left click, check propagation

			if (event.which!=1 ||
				(!event.data.acceptPropagatedEvent && event.target != this)){ 
				return false; 
			}
			
			// Initial coordinates will be the last when dragging
			event.data.lastCoord = {left: event.clientX, top: event.clientY}; 
		
			$.event.add( document, "mouseup", 
						 dragscroll.mouseUpHandler, event.data );
			$.event.add( document, "mousemove", 
						 dragscroll.mouseMoveHandler, event.data );
			if (event.data.preventDefault) {
                event.preventDefault();
                return false;
            }
		},
		mouseMoveHandler : function(event) { // User is dragging
			// How much did the mouse move?
			var delta = {left: (event.clientX - event.data.lastCoord.left),
						 top: (event.clientY - event.data.lastCoord.top)};
			
			var left = divToMove.css('left').substring(0, divToMove.css('left').length - 2);
			var top = divToMove.css('top').substring(0, divToMove.css('top').length - 2);
			
			var newLeft = parseInt(left) + delta.left;
			var newTop = parseInt(top) + delta.top;
			
			// Set the scroll position relative to what ever the scroll is now
			divToMove.css('left', newLeft + "px");

			divToMove.css('top', newTop + "px");
			
			
			// Save where the cursor is
			event.data.lastCoord={left: event.clientX, top: event.clientY};
			if (event.data.preventDefault) {
                event.preventDefault();
                return false;
            };

		},
		mouseUpHandler : function(event) { // Stop scrolling
			$.event.remove( document, "mousemove", dragscroll.mouseMoveHandler);
			$.event.remove( document, "mouseup", dragscroll.mouseUpHandler);
			if (event.data.preventDefault) {
                event.preventDefault();
                return false;
            }
		}
	};
	
	// set up the initial events
	this.each(function() {
		// closure object data for each scrollable element
		var data = {scrollable : $(this),
					acceptPropagatedEvent : settings.acceptPropagatedEvent,
                    preventDefault : settings.preventDefault };
		// Set mouse initiating event on the desired descendant
		$(this).bind('mousedown', data, dragscroll.mouseDownHandler);

		$(this).on('mousewheel', function(event) {
			var divToMove = $(this).children("div:first");
			
			var svg = $("#imgDiv").children("svg:first");
			
			var width = svg.width(); // current image width
			var height = svg.height(); // current image height

			var divWidth = svg.width(); // current image width
			var divHeight = svg.height(); // current image height

			var x = event.pageX - divToMove.position().left; // mouse x position in div
	        var y = event.pageY - divToMove.position().top; // mouse y position in div

			var left = parseInt(divToMove.css('left').substring(0, divToMove.css('left').length - 2));
			var top = parseInt(divToMove.css('top').substring(0, divToMove.css('top').length - 2));

			var natX = x / scale;
			var natY = y / scale;
			
		    if (event.deltaY >= 0) { // zoom in
		    	direction = 1;
				setScale();

		    } else if (event.deltaY < 0) { // zoom out
		    	direction = -1;
				setScale();
		    }

			var newY = natY * scale;
			var deltaY = y - newY;
			var newTop = (top + deltaY );
			$("#imgDiv").css('top', newTop + "px");

			var newX = natX * scale;
			var deltaX = x - newX;
			var newLeft = (left + deltaX );
			$("#imgDiv").css('left', newLeft + "px");
		});
	});
}; //end plugin dragscrollzoom

})( jQuery ); // confine scope
