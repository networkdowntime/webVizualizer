/**
 * 
 */
var fake = "";
//fake = "Fake";

var layouts = { 'dot' : 'Top Down', 'neato' : 'Natural' };
var pkFilter = { 'All' : 'All', 'NoPK' : 'No PK', 'HasPK' : 'Has PK' };
var fkFilter = { 'All' : 'All', 'NoFK' : 'No FK', 'HasFK' : 'Has FK' };
var javaDiagramType = { 'PACKAGE_DIAGRAM' : 'Package Diagram', 'CLASS_ASSOCIATION_DIAGRAM' : 'Class Diagram', 'UNREFERENCED_CLASSES' : 'Unreferenced Classes' };

function javaAnalyzerInit(menuItem) {

	$.each(layouts, function(value, label) {
		$("#javaLayout").append("<option value='"+value+"'>"+label+"</option>");
	});
	
	$.each(javaDiagramType, function(value, label) {
		$("#javaDiagramType").append("<option value='"+value+"'>"+label+"</option>");
	});
	
	sourceDir = $.cookie("sourceDir");
	if (sourceDir != null) $("#sourceDir").val(sourceDir);
	
	$("#sourceScan").click(function() {
		sourceDir = $("#sourceDir").val();
		console.log(sourceDir);
		$.ajax({
		    url:'/api/code/javaScanner' + fake + '/file',
		    type:'POST',
		    data: { path: sourceDir },
		    dataType: 'json',
		    contentType: "application/json",
		    success:function(res){
		    	$.cookie("sourceDir", sourceDir);
		    	loadPackages();
		    	loadClasses();
				drawGraph();
		    },
		    error:function(res){
		        alert("Bad thing happend! " + res.statusText);
		        alert("Bad thing happend! " + res);
		    }
		});
		
	});
	
	function loadPackages() {
		container = $("#packagesDiv").next();
		packagesDiv = $(container).children(".content");
		$(packagesDiv).empty();
		$.get('/api/code/javaScanner' + fake + '/packages', function(data) {
    	    $(container).css('overflow-y', 'hidden');
	    	$(data).each(function() {
	    		$("<div><input class='packages' type='checkbox' name="+this+" value="+this+" checked>" + this + "</div>").hide().appendTo(packagesDiv).slideDown(250);
	    	});
    	    $(container).css('overflow-y', '');
    		fixSideBarMaxHeight(null, true)
    	    $(".packages").change(function() {
    	    	loadClasses();
    			drawGraph();
    	    });
	    });
	}

	function loadClasses() {
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
		    success:function(data){
	    	    $(container).css('overflow-y', 'hidden');
		    	$(data).each(function() {
		    		$("<div><input class='classes' type='checkbox' name="+this+" value="+this+" checked>" + this + "</div>").hide().appendTo(classesDiv).slideDown(250);
		    	});
	    	    $(container).css('overflow-y', '');
	    	    $(".classes").change(function() {
	    			drawGraph();
	    	    });
	    		fixSideBarMaxHeight(null, true);
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
		
	function drawGraph() {
		var uncheckedPackages = $.makeArray( $.map($(".packages"), function(i) { if (!$(i).prop('checked')) { return $(i).val(); } }) );
		var uncheckedClasses = $.makeArray( $.map($(".classes"), function(i) { if (!$(i).prop('checked')) { return $(i).val(); } }) );

		filter = {
			diagramType : $("#javaDiagramType option:selected").val(), // PACKAGE_DIAGRAM, CLASS_ASSOCIATION_DIAGRAM, METHOD_CALL_DIAGRAM
			showFields : $("#showFields").prop('checked'), // boolean
			showMethods : $("#showMethods").prop('checked'), //boolean
			fromFile : $("#fromFile").prop('checked'), //boolean
			packagesToExclude : uncheckedPackages, //["CREATED_BY", "UPDATED_BY"], // Set<String>
			classesToExclude : uncheckedClasses // NoFK, HasFK, All
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
			     
			     //console.log(result);
			     $("#imgDiv").html(result);
			     initializeDragScrollZoom();
		    },
		    error:function(res){
		        alert("Bad thing happend! " + res.statusText);
		        alert("Bad thing happend! " + res);
		    }
		});
	
	}

}



function fixSideBarMaxHeight(divChanging, animateChange) {
	
	totalLabelHeight = 0;
	$('.collapsible').each(function() { totalLabelHeight += $(this).outerHeight(); });
	
	availableHeight = ($(window).height() - totalLabelHeight - $(".sidebar").offset().top);
	
	oldHeights = new Object();
	
	openContainers = 0;
	$('.container').each(function() { // count all open or opening containers
		oldHeights[$(this).attr('id')] = $(this).outerHeight();
		sameDiv = divChanging != null && $(this).attr('id') == divChanging.attr('id');

		if ((!sameDiv && $(this).css('display') != 'none') || (sameDiv && $(this).css('display') == 'none')) { 
			openContainers++;
		} 
	});

	// this assumes that all divs would be at least max height tall 
	maxHeight1 = availableHeight / openContainers;
	console.log("availableHeight:", availableHeight, "openContainers:", openContainers, "maxHeight1:", maxHeight1);

	$('.container').css('height', ''); // clear any set heights

	$('.container').each(function() { // if any divs are less than the max height remove them from the calculations and subtract their height
		sameDiv = divChanging != null && $(this).attr('id') == divChanging.attr('id');
		if ((!sameDiv && $(this).css('display') != 'none') || (sameDiv && $(this).css('display') == 'none')) {
			console.log("outerHeight:", $(this).outerHeight());
			if ($(this).outerHeight() < maxHeight1) {
				console.log("inside2");
				openContainers--;
				availableHeight -= $(this).outerHeight();
			} 
		}
	});
	
	// asserting that maxHeight2 > maxHeight1
	maxHeight2 = availableHeight / openContainers; // calculate the new max height

	console.log("availableHeight:", availableHeight, "openContainers:", openContainers, "maxHeight2:", maxHeight2);
	console.log("window width:", $(window).width(), "window height:", $(window).height());

	$('.container').each(function() { // if the container is open or opening and it's natural height exceeds the new max height set the height 
		sameDiv = divChanging != null && $(this).attr('id') == divChanging.attr('id');
		
		console.log(sameDiv, ($(this).css('display') == 'none'), $(this), divChanging);
		if (sameDiv && $(this).css('display') != 'none') {
			$(this).css('height', oldHeights[$(this).attr('id')]);
		}
	
		if ((!sameDiv && $(this).css('display') != 'none') || (sameDiv && $(this).css('display') == 'none')) {
			console.log("outerHeight:", $(this).outerHeight());
			if ($(this).outerHeight() > maxHeight2) {
				if (sameDiv) {
					$(this).css('height', maxHeight2+'px');
				} else {
					if (animateChange) {
						$(this).css('height', oldHeights[$(this).attr('id')]).animate({ height: maxHeight2 }, 'slow');
					} else {
						$(this).css('height', maxHeight2);
					}
				}
			} 
		}
	});

}

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
			fixSideBarMaxHeight(null, false);
		});
		
		$(mainMenu).each(function() {
			// hide the main divs
//			$("#"+this.id).hide();
//			$("#"+this.id).zIndex(-1);
			$("#"+this.id).resizable({
			    handles: 'e', minWidth: 5
			});
//			$("#"+this.id).css({ position: "absolute" });
			
			window[this.initializer](this); // calls the initializer function specified in the mainMenu data structure
			fixSideBarMaxHeight(null, true);
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
            	console.log("animateOpen");
                elem.next().slideFadeToggle(opts.speed);
            },
            animateClose: function (elem, opts) { //replace the standard slideDown with custom function
            	console.log("animateClose");
                elem.next().slideFadeToggle(opts.speed);
            },
            loadOpen: function (elem) { //replace the standard open state with custom function
            	console.log("loadOpen");
                elem.next().show();
            },
            loadClose: function (elem, opts) { //replace the close state with custom function
            	console.log("loadClose");
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