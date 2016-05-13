/**
 * 
 */
var plugins = new Object;
var currentPlugin;

$(document).ready(function() {
    $(document)
    .ajaxSend(function() {   
        $('#loadingDiv').show();
    })
    .ajaxStop(function() {
        $('#loadingDiv').hide();
    })
    .ajaxError(function() {
        $('#loadingDiv').hide();
    });
});

(function($){ // secure $ jQuery alias
	  
	function setScale() {
		scale = scale + (direction * halfStep);
		
		if (scale < 0.1 ) scale = 0.1;
		if (scale > 2) scale = 2;

		var newWidth = naturalWidth * scale;
		var newHeight = naturalHeight * scale;
		
		image.width(newWidth);
		image.height(newHeight);		
	}
		
	$.fn.clickthroughhandler = function( options ){

		$(this).bind("mousewheel", function() {
	        return false;
	    });

		var x = 0;
		var y = 0;
		
		var settings = $.extend(
			{   
				dragSelector:'>:first',
				acceptPropagatedEvent: true,
	            preventDefault: true
			},options || {});
		 
		
		var clickthrough = {
			mouseDownHandler : function(event) {
				// mousedown, left click, check propagation

				if (event.which!=1 ||
					(!event.data.acceptPropagatedEvent && event.target != this)){ 
					return false; 
				}
				
				$(".custom-menu").hide(100);
				
				// Initial coordinates will be the last when dragging
				event.data.lastCoord = {left: event.clientX, top: event.clientY}; 
				$('#scrollDiv').data('clickthrough:hasMoved', false);
				$('#scrollDiv').data('clickthrough:lastCoord', {left: event.clientX, top: event.clientY});

				$.event.add( document, "mouseup", 
							 clickthrough.mouseUpHandler, event.data );
				$.event.add( document, "mousemove", 
							 clickthrough.mouseMoveHandler, event.data );
				if (event.data.preventDefault) {
	                event.preventDefault();
	                return false;
	            }
			},
			mouseMoveHandler : function(event) { // User is dragging
				// How much did the mouse move?
				var delta = {left: (event.clientX - $('#scrollDiv').data('clickthrough:lastCoord').left),
							 top: (event.clientY - $('#scrollDiv').data('clickthrough:lastCoord').top)};
				
				if (delta.left != 0 || event.clientY - delta.top != 0) {
					$('#scrollDiv').data('clickthrough:hasMoved', true);
				}
			},
			mouseUpHandler : function(event) { // Stop scrolling
				$.event.remove( document, "mousemove", clickthrough.mouseMoveHandler);
				$.event.remove( document, "mouseup", clickthrough.mouseUpHandler);
				
				if (!$('#scrollDiv').data('clickthrough:hasMoved')) {
					x = event.clientX;
					y = event.clientY;

					event.stopPropagation();
			    	$('#scrollDiv').hide();

					$('svg').css('z-index', '100');
					
					var element = document.elementFromPoint(event.clientX, event.clientY);
					if ($(element).is('polygon') || $(element).is('text')) {
						var name = $(element).siblings('text').context.textContent;
						var title = $(element).siblings('title')[0].textContent.replace(/_/g, '.');
						
						var isPackage = false;
						var isClass = false;
		    			$('.packages').each(function(index) {
		    				isPackage = isPackage || $(this).attr('name') === name;
		    			});
						
		    			$('.classes').each(function() {
		    				isClass = isClass || $(this).attr('name').replace(/_/g, '.') === title;
		    				if ($(this).attr('name').replace(/_/g, '.') === title) {
		    					title = $(this).attr('name');
		    				}
		    			});
						
						if (isPackage) { // not adding a context to a package right now
						}
						
						if (isClass) {
							var menu = $(".custom-menu"); 
						    $(menu).finish().toggle(100).
							    css({
							        top: event.pageY + "px",
							        left: event.pageX + "px"
							    });
						    
						    $(".custom-menu li").click(function(){
								// This is the triggered action name
								switch($(this).attr("data-action")) {
									// A case for each action. Your actions here
									case "source": 
										var win = window.open('/api/code/javaScanner/file?class=' + encodeURIComponent(title), '_blank');
										if(win){
										    //Browser has allowed it to be opened
										    win.focus();
										}else{
										    //Broswer has blocked it
										    alert('Please allow popups for this site');
										}
										break;
							    }
							    // Hide it AFTER the action was triggered
								$(".custom-menu").hide(100);
								$(".custom-menu li").off('click');
							});
						}
					}
					$('svg').css('z-index', '-1');
			    	$('#scrollDiv').show();
	            }
				
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
			$(this).bind('mousedown', data, clickthrough.mouseDownHandler);

			$(this).on('mousewheel', function(event) {
			});

		});
	}; //end plugin clickthroughhandler

})( jQuery ); // confine scope

function analyzer() {
	
	// Public Variables
	
	// Public Functions
	this.init = function() {
		loadPlugins();
	};
	
	// Private Functions
	function loadPlugins() {
		var pluginsUrl = '/api/plugin/plugins';
		$.get(pluginsUrl, function(data) {
			plugins = $(data);
			$(data).each(function() {
				console.log(this);
			});
			initializeSideBars();
			initializeMainMenu();
		});

	}
	
	function fixScrollDiv() {
    	var overlay = $('#scrollDiv');
    	var totalHeight = $(window).height();
    	var totalWidth = $(window).width();
    	var top = $('.header').outerHeight();
    	var left = $('.sidebar').outerWidth();
    	overlay.css('top', top);
    	overlay.css('left', left);
    	overlay.css('width', totalWidth - left);
    	overlay.css('height', totalHeight - top);
	};

    $.fn.slideFadeToggle = function(speed, easing, callback) {
    	fixSideBarMaxHeight($('#' + currentPlugin.id), this, true);
        return this.animate({opacity: 'toggle', height: 'toggle'}, speed, easing, callback);
    };

	function initializeSideBars() {
		$(window).resize(function() {
			fixScrollDiv();
			
	    	if (currentPlugin) {
	        	fixSideBarMaxHeight($('#' + currentPlugin.id), this, true);
	    	}
		});
		
		$(plugins).each(function() {
			// load the sidebar for the plugin
			var id = this.id;
			var sideBarUrl = '/api/plugin/sideBar' + '?' + $.param({'pluginClass': this.pluginClass})
			var jsUrl = '/api/plugin/javaScript' + '?' + $.param({'pluginClass': this.pluginClass})

			$.get(sideBarUrl, function(data) {
				element = $(data);
				$('.mainBody').prepend(element);
				element.attr('id', id);
				element.hide();
				element.zIndex(-1);
				element.resizable({
					handles: 'e', minWidth: 5,
				    resize: function( event, ui ) {
						fixScrollDiv();
				    },
				    stop: function( event, ui ) {
						fixScrollDiv();
				    }
				});

		        //collapsible management
				element.find('.collapsible').collapsible({
		            defaultOpen: 'connection',
		            cookieName: 'connection',
		            speed: 'slow',
		            animateOpen: function (elem, opts) { //replace the standard slideUp with custom function
		                elem.next().slideFadeToggle(opts.speed);
		            },
		            animateClose: function (elem, opts) { //replace the standard slideDown with custom function
		                elem.next().slideFadeToggle(opts.speed);
		            },
		            loadOpen: function (elem) { //replace the standard open state with custom function
		                elem.next().show();
		            },
		            loadClose: function (elem, opts) { //replace the close state with custom function
		                elem.next().hide();
		            }
		        });
		        
				$.getScript(jsUrl, function() {
				});

			});
		});
	}
	
	function initializeMainMenu() {
		
		var maxWidth = 0;
		
		// Create the menuItems
		$(plugins).each(function() {
			menuItem = $('<div class="mainMenuItem">' + this.label + '</div>').appendTo("#mainMenu");
			if ($(menuItem).width() > maxWidth)
				maxWidth = $(menuItem).width(); 

			// default hidden and in front
			$(menuItem).zIndex(100);
			$(menuItem).hide();
			
			// highlight on mouseover
			$(menuItem).mouseover(function() {
				$(this).addClass("mainMenuItemHighlight");
			});

			// de-highlight on mouseout
			$(menuItem).mouseout(function() {
				$(this).removeClass("mainMenuItemHighlight");
			});

			// swap out main divs on click
			$(menuItem).click(function() {
				var label = $(this).text();

				$(plugins).each(function() {
					if (label == this.label) {
						$("#"+this.id).show();
						$("#"+this.id).zIndex(1);
			        	fixSideBarMaxHeight($('.mainBody').find('#' + this.id)[0], null, true);
						currentPlugin = this;
					} else {
						$("#"+this.id).hide();
						$("#"+this.id).zIndex(1);
					}
				});
				$(this).removeClass("mainMenuItemHighlight");
				
				$("#mainMenu").children(".mainMenuItem").each(function( index ) {
					$(this).hide();
				});

			});

		});
		
		// Set consistent menu widths
		$("#mainMenu").children(".mainMenuItem").each(function( index ) {
			$(this).width(maxWidth + 5);
		});
		
		// Show menu items on mouseover
		$("#mainMenu").mouseover(function() {
			$(plugins).each(function() {
//				$("#"+this.id).zIndex(-1);
			});

			$("#mainMenu").children(".mainMenuItem").each(function( index ) {
				$(this).show();
			});
		});
		
		// Hide menu items on mouseout
		$("#mainMenu").mouseout(function() {
			$("#mainMenu").children(".mainMenuItem").each(function( index ) {
				$(this).hide();
			});
			$(plugins).each(function() {
				$("#"+this.id).zIndex(1);
			});
		});
		
	}
}