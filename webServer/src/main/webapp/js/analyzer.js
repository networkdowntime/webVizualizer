/**
 * 
 */
var fake = "";
//fake = "Fake";

var referenceChainDepths = [ 0, 1, 2, 3, 4, 5, 6 ];
var layouts = { 'dot' : 'Top Down', 'neato' : 'Natural' };
var pkFilter = { 'All' : 'All', 'NoPK' : 'No PK', 'HasPK' : 'Has PK' };
var fkFilter = { 'All' : 'All', 'NoFK' : 'No FK', 'HasFK' : 'Has FK' };
var javaDiagramType = { 'PACKAGE_DIAGRAM' : 'Package Diagram', 'CLASS_ASSOCIATION_DIAGRAM' : 'Class Diagram', 'UNREFERENCED_CLASSES' : 'Unreferenced Classes' };

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

function javaAnalyzerInit(menuItem) {

	if (!$("#referenceChains").prop('checked')) {
		$("#referenceChainDetails").hide();
	}
	
	$.each(referenceChainDepths, function(index, value) {
		$("#upstreamReferenceDepth").append("<option value='"+value+"'>"+value+"</option>");
		$("#downstreamDependencyDepth").append("<option value='"+value+"'>"+value+"</option>");
	});
	
	$("#referenceChains").change(function() {
		if ($("#referenceChains").prop('checked')) {
			$("#referenceChainDetails").show();
		} else {
			$("#referenceChainDetails").hide();
			
		}
    	fixSideBarMaxHeight(null, true);
	});
	
	$.each(layouts, function(value, label) {
		$("#javaLayout").append("<option value='"+value+"'>"+label+"</option>");
	});
	
	$.each(javaDiagramType, function(value, label) {
		$("#javaDiagramType").append("<option value='"+value+"'>"+label+"</option>");
	});
	
	sourceDir = $.cookie("sourceDir");
	if (sourceDir != null) $("#sourceDir").val(sourceDir);
	
	$("#saveToPng").click(function() {
		var svg = $("svg")[0].outerHTML;
		
		var xhr = new XMLHttpRequest();
		var url = "/api/code/javaScanner/toPng";
		var params = "svg=" + encodeURIComponent(svg);
		xhr.responseType = 'blob';
		xhr.open("POST", url, true);

		//Send the proper header information along with the request
		xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
		xhr.onload = function(e) {
		  if (this.status == 200) {
			var blob = new Blob([this.response], {type: 'image/png'});
			var downloadUrl = URL.createObjectURL(blob);
			var a = document.createElement("a");
			a.href = downloadUrl;
		    a.download = "diagram.png";
		    document.body.appendChild(a);
		    a.click();
		    $(a).remove();
		  }
		};

		xhr.send(params);
	})

	$("#sourceAdd").click(function() {
		sourceDir = $("#sourceDir").val();
		$.ajax({
		    url:'/api/code/javaScanner' + fake + '/file',
		    type:'POST',
		    data: { path: sourceDir },
		    dataType: 'json',
		    contentType: "application/json",
		    success:function(res){
		    	$.cookie("sourceDir", sourceDir);
		    	loadFiles();
		    	loadPackages();
		    	loadClasses();
//				drawGraph();
		    },
		    error:function(res){
		        alert("Bad things happend! " + res.statusText);
		        alert("Bad things happend! " + res);
		    }
		});
		
	});
	
	function loadFiles() {
		container = $("#sourceDiv").next();
		filesDiv = $("#inputFiles");
		$(filesDiv).empty();
		$.get('/api/code/javaScanner' + fake + '/files', function(data) {
    	    $(container).css('overflow-y', 'hidden');
    		$(data).each(function() {
        	    $(container).css('overflow-y', 'hidden');
    			var html = "<div class='inputFile'>";
    			html += "<span class='ui-icon ui-icon-circle-minus left add-remove-icon-align sourceDel'/>";
    			html += "<span class='fileText'><span class='fileLabel'>" + this + "</span></span>";
    			html += "<div>";
	    		$(html).appendTo(filesDiv);
	    	});
    	    $(container).css('overflow-y', '');
    		fixSideBarMaxHeight(null, true);
    		
    		$(".sourceDel").click(function() {
    			var sourceDir = $(this).siblings(".fileText, fileLabel").text();
    			var parent = $(this).parent();
    			$.ajax({
    			    url:'/api/code/javaScanner' + fake + '/file' + '?' + $.param({'path': sourceDir}),
    			    type:'DELETE',
    			    success:function(res){
    			    	$(parent).remove().slideUp(250);
    		    		fixSideBarMaxHeight(null, true);
    			    	loadPackages();
    			    	loadClasses();
//    					drawGraph();
    			    },
    			    error:function(res){
    			        alert("Bad things happend! " + res.statusText);
    			        alert("Bad things happend! " + res);
    			    }
    			});
    			
    		});
    		
		});
	}
	
	function loadPackages() {
		container = $("#packagesDiv").next();
		packagesDiv = $(container).children(".content");
		$(packagesDiv).empty();
		$.get('/api/code/javaScanner' + fake + '/packages', function(data) {
    	    $(container).css('overflow-y', 'hidden');

    		$("<div><input type='button' id='packageCheckAll' value='Uncheck All' /><span class='right'><input id='packageSearch' type='text' placeholder='Package search' value=''/></span></div>").appendTo(packagesDiv);

    		$('#packageCheckAll:button').click(function() {
//    			console.log(this);
    			if ($(this).attr("value") === 'Uncheck All') {
        	        $('.packages:not(:hidden)').prop('checked', false);;
        	        $(this).val('Check All');            				
        	    	loadClasses();
        			drawGraph();
    			} else {
        	        $('.packages:not(:hidden)').prop('checked', true);;
        	        $(this).val('Uncheck All');
        	    	loadClasses();
        			drawGraph();
    			}
    	    });
    	    
    		$('#packageSearch').on('change keyup paste', function() {
    			packageToSearchFor = $("#packageSearch").val();
    			$('.packages').parent().hide();
    			$('.packages').filter(function() {
    				return $(this).attr('name').toLowerCase().indexOf(packageToSearchFor.toLowerCase()) > -1;
    			}).parent().show();;
    		});
    		
    		$(data).each(function() {
	    		$("<div><input class='packages' type='checkbox' name="+this+" value="+this+" checked>" + this + "</div>").appendTo(packagesDiv);
	    	});
    	    $(container).css('overflow-y', '');
    	    
    		fixSideBarMaxHeight(null, true);
    	    $(".packages").change(function() {
    	    	loadClasses();
//    			drawGraph();
    	    });
	    });
	}

	function loadClasses(animate) {
		var uncheckedPackages = $.makeArray( $.map($(".packages"), function(i) { if (!$(i).prop('checked')) { return $(i).val(); } }) );

		container = $("#classesDiv").next();
		classesDiv = $(container).children(".content");
		$(classesDiv).empty();
		$.ajax({
		    url:'/api/code/javaScanner' + fake + '/classes',
		    type:'POST',
		    data: JSON.stringify(uncheckedPackages),
		    dataType: 'json',
		    contentType: "application/json",
		    success:function(data) {
//	    	    $(container).css('overflow-y', 'hidden');
	    	    
	    		$("<div><input type='button' id='classCheckAll' value='Uncheck All' /><span class='right'><input id='classSearch' type='text' placeholder='Class search' value=''/></span></div>").appendTo(classesDiv);

	    		$('#classCheckAll:button').click(function() {
//	    			console.log(this);
	    			if ($(this).attr("value") === 'Uncheck All') {
	        	        $('.classes:not(:hidden)').prop('checked', false);;
	        	        $(this).val('Check All');            				
	        			drawGraph();
	    			} else {
	        	        $('.classes:not(:hidden)').prop('checked', true);;
	        	        $(this).val('Uncheck All');
	        			drawGraph();
	    			}
	    	    });
	    	    
	    		$('#classSearch').on('change keyup paste', function() {
	    			classToSearchFor = $("#classSearch").val();
	    			$('.classes').parent().hide();
	    			$('.classes').filter(function() {
	    				return $(this).attr('name').toLowerCase().indexOf(classToSearchFor.toLowerCase()) > -1;
	    			}).parent().show();
	    		});
	    		
		    	$(data).each(function() {
		    		$("<div><input class='classes' type='checkbox' name="+this+" value="+this+" checked>" + this + "</div>").appendTo(classesDiv);
		    	});

	    		fixSideBarMaxHeight(null, animate);

//	    		$(container).css('overflow-y', '');
//	    	    $(".classes").change(function() {
//	    			drawGraph();
//	    	    });
		    },
		    error:function(res){
		        alert("Bad thing happend! " + res.statusText);
		        alert("Bad thing happend! " + res);
		    }
		});
	}

	$("#javaRender").click(function() {
		drawGraph();
	});
		
	var graphVizFile = null;	
		
	function drawGraph() {
		var uncheckedPackages = $.makeArray( $.map($(".packages"), function(i) { if (!$(i).prop('checked')) { return $(i).val(); } }) );
		var uncheckedClasses = $.makeArray( $.map($(".classes"), function(i) { if (!$(i).prop('checked')) { return $(i).val(); } }) );
		
		var upstreamReferenceDepth = null;
		var downstreamDependencyDepth = null;
		
		if ($("#referenceChains").prop('checked')) {
			upstreamReferenceDepth = $("#upstreamReferenceDepth option:selected").val();
			downstreamDependencyDepth = $("#downstreamDependencyDepth option:selected").val();
		}
		
		filter = {
			advancedSearchQuery : $('#advancedSearchQuery').val(),
			diagramType : $("#javaDiagramType option:selected").val(), // PACKAGE_DIAGRAM, CLASS_ASSOCIATION_DIAGRAM, METHOD_CALL_DIAGRAM
			showFields : $("#showFields").prop('checked'), // boolean
			showMethods : $("#showMethods").prop('checked'), //boolean
			fromFile : $("#fromFile").prop('checked'), //boolean
			packagesToExclude : uncheckedPackages, //["CREATED_BY", "UPDATED_BY"], // Set<String>
			classesToExclude : uncheckedClasses, // NoFK, HasFK, All
			upstreamReferenceDepth : upstreamReferenceDepth,
			downstreamDependencyDepth : downstreamDependencyDepth
		};
		
		$.ajax({
		    url:'/api/code/javaScanner' + fake + '/dot',
		    type:'POST',
		    data: JSON.stringify(filter),
		    dataType: 'text',
		    contentType: "application/json",
		    success:function(res){
			     var dotFile = res;
			     
			     var format = "svg"; // dot, plain, svg, xdot
			     var engine = $("#javaLayout option:selected").val(); // dot, neato
			     
			     var result = Viz(dotFile, format, engine);
			     
			     //  Make file that can be downloaded later.
			     var data = new Blob([dotFile], {type: 'text/plain'});
	    		 if (graphVizFile !== null) {
      				window.URL.revokeObjectURL(graphVizFile);
    			 }
    			 graphVizFile = window.URL.createObjectURL(data);
			     var link = document.getElementById('saveToGv');
			     link.href = graphVizFile;
			     
			     $('svg').remove();
			     $("#scrollDiv").after(result);
			     initializeDragScrollZoom();
		    },
		    error:function(res){
		        alert("Bad thing happend! " + res.statusText);
		        alert("Bad thing happend! " + res);
		    }
		});
	
	}

	loadFiles();
	loadPackages();
	loadClasses(false);

};

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
					console.log('element', element);
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
						
						console.log('isPackage', isPackage);
						console.log('isClass', isClass);
	
						if (isPackage) { // not adding a context to a package right now
//						    $(".custom-menu").finish().toggle(100).
//						    
//						    // In the right position (the mouse)
//						    css({
//						        top: event.pageY + "px",
//						        left: event.pageX + "px"
//						    });
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
										var win = window.open('/api/code/javaScanner' + fake + '/file?class=' + encodeURIComponent(title), '_blank');
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
	this.bar = "bar";
	
	// Private Variables
	var mainMenu = [
	                { label: "Java Analyzer", id: "javaAnalyzer", initializer: "javaAnalyzerInit" }
	               ];
	
	// Public Functions
	this.init = function() {
		initializeSideBars();
		initializeMainMenu();
	};
	
	// Private Functions
	function initializeSideBars() {
		$(window).resize(function() {
	    	var overlay = $('#scrollDiv');
	    	var totalHeight = $(window).height();
	    	var totalWidth = $(window).width();
	    	var top = $('.header').outerHeight();
	    	var left = $('.sidebar').outerWidth();
	    	overlay.css('top', top);
	    	overlay.css('left', left);
	    	overlay.css('width', totalWidth - left);
	    	overlay.css('height', totalHeight - top);

			fixSideBarMaxHeight(null, false);
		});
		
		$(mainMenu).each(function() {
			// hide the main divs
//			$("#"+this.id).hide();
//			$("#"+this.id).zIndex(-1);
			$("#"+this.id).resizable({
			    handles: 'e', minWidth: 5,
			    resize: function( event, ui ) {
			    	var overlay = $('#scrollDiv');
			    	var totalHeight = $(window).height();
			    	var totalWidth = $(window).width();
			    	var top = $('.header').outerHeight();
			    	var left = $('.sidebar').outerWidth();
			    	overlay.css('top', top);
			    	overlay.css('left', left);
			    	overlay.css('width', totalWidth - left);
			    	overlay.css('height', totalHeight - top);
			    },
			    stop: function( event, ui ) {
			    	var overlay = $('#scrollDiv');
			    	var totalHeight = $(window).height();
			    	var totalWidth = $(window).width();
			    	var top = $('.header').outerHeight();
			    	var left = $('.sidebar').outerWidth();
			    	overlay.css('top', top);
			    	overlay.css('left', left);
			    	overlay.css('width', totalWidth - left);
			    	overlay.css('height', totalHeight - top);
			    }
			});
//			$("#"+this.id).css({ position: "absolute" });
			
			window[this.initializer](this); // calls the initializer function specified in the mainMenu data structure
//			fixSideBarMaxHeight(null, true);
		});
		
		
        $.fn.slideFadeToggle = function(speed, easing, callback) {
        	fixSideBarMaxHeight(this, true);
            return this.animate({opacity: 'toggle', height: 'toggle'}, speed, easing, callback);
        };

        //collapsible management
        $('.collapsible').collapsible({
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

	}
	
	function initializeMainMenu() {
		
		var maxWidth = 0;
		
		// Create the menuItems
		$(mainMenu).each(function() {
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

				$(mainMenu).each(function() {
					if (label == this.label) {
						$("#"+this.id).show();
						$("#"+this.id).zIndex(1);
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
			$(mainMenu).each(function() {
				$("#"+this.id).zIndex(-1);
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
			$(mainMenu).each(function() {
				$("#"+this.id).zIndex(1);
			});
		});
		
	}
}