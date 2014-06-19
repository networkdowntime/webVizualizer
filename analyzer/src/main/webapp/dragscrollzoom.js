;(function($){ // secure $ jQuery alias

$.fn.dragscrollzoom = function( options ){

	var divToMove = $(this).children("div:first");

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
	 
	
	var dragscroll= {
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
			
			// console.log(left, delta.left, divToMove.css('left'), (left - delta.left));

			var newLeft = parseInt(left) + delta.left;
			var newTop = parseInt(top) + delta.top;
			
			// Set the scroll position relative to what ever the scroll is now
			divToMove.css('left', newLeft + "px");

			divToMove.css('top', newTop + "px");
			
			
			// Save where the cursor is
			event.data.lastCoord={left: event.clientX, top: event.clientY}
			if (event.data.preventDefault) {
                event.preventDefault();
                return false;
            }

		},
		mouseUpHandler : function(event) { // Stop scrolling
			$.event.remove( document, "mousemove", dragscroll.mouseMoveHandler);
			$.event.remove( document, "mouseup", dragscroll.mouseUpHandler);
			if (event.data.preventDefault) {
                event.preventDefault();
                return false;
            }
		}
	}
	
	// set up the initial events
	this.each(function() {
		// closure object data for each scrollable element
		var data = {scrollable : $(this),
					acceptPropagatedEvent : settings.acceptPropagatedEvent,
                    preventDefault : settings.preventDefault }
		// Set mouse initiating event on the desired descendant
		$(this).bind('mousedown', data, dragscroll.mouseDownHandler);
	});
}; //end plugin dragscrollzoom

})( jQuery ); // confine scope
