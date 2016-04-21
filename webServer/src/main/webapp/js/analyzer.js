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

function dbAnalyzerInit(menuItem) {
	
	$.each(layouts, function(value, label) {
		$("#dbLayout").append("<option value='"+value+"'>"+label+"</option>");
	});
	
	$.each(pkFilter, function(value, label) {
		all = (value == 'All') ? " selected='selected'" : "";
		$("#pkFilter").append("<option value='"+value+"'"+all+">"+label+"</option>");
	});
	
	$.each(fkFilter, function(value, label) {
		$("#fkFilter").append("<option value='"+value+"'"+all+">"+label+"</option>");
	});
	
	$.get('/api/db/connection' + fake + '/supportedDatabases', function(data) {
		var select = $("#dbType");
    	$(data).each(function() {
    		$(select).append("<option value="+this+">"+this+"</option>");

    		dbType = $.cookie("dbType");
    		if (dbType != null) $("#dbType").val(dbType);
    	});
    });

	dbUser = $.cookie("dbUser");
	dbPasswd = $.cookie("dbPasswd");
	dbUrl = $.cookie("dbUrl");

	if (dbUser != null) $("#username").val(dbUser);
	if (dbPasswd != null) $("#password").val(dbPasswd);
	if (dbUrl != null) $("#jdbcUrl").val(dbUrl);
	
	$("#connect").click(function() {
		var dbType = $("#dbType option:selected").val();
		var user = $.trim($("#username").val());
		var passwd = $.trim($("#password").val());
		var url = $.trim($("#jdbcUrl").val());
		
		$.ajax({
		    url:'/api/db/connection' + fake + '/connect',
		    type:'POST',
		    data: { dbType: dbType, username: user, password: passwd, jdbcUrl: url},
		    dataType: 'json',
		    contentType: "application/json",
		    success:function(res){
				$.cookie("dbType", dbType);
				$.cookie("dbUser", user);
				$.cookie("dbPasswd", passwd);
				$.cookie("dbUrl", url);

				loadSchemas();
		    },
		    error:function(res){
		        alert("Bad thing happend! " + res.statusText);
		    }
		});
	});
	
	function loadSchemas() {
		container = $("#schemasDiv").next();
		schemasDiv = $(container).children(".content");

		$.get('/api/db/connection' + fake + '/schemasWithTables', function(data) {
    	    $(container).css('overflow-y', 'hidden');
	    	
    	    $(data).each(function() {
	    		$("<div><input class='schemas' type='checkbox' name="+this+" value="+this+">" + this + "</div>").hide().appendTo(schemasDiv).slideDown(250);
	    	});

	    	$(container).css('overflow-y', '');
    		fixSideBarMaxHeight(null, true)
	    	
	    	$('.schemas').change(function() {
	    		scanSchemas();
	    	});
	    });
	}
	
	function scanSchemas() {
		var checkedSchemas = $.makeArray( $.map($(".schemas"), function(i) { if ($(i).prop('checked')) { return $(i).val(); } }) );
		$.get('/api/db/connection' + fake + '/scanSchemas', { schemas : checkedSchemas }, function(data) {
			loadTables();
	    });
	}
	
	function loadTables() {
		container = $("#tablesDiv").next();
		tablesDiv = $(container).children(".content");
		$(tablesDiv).empty();
		$.get('/api/db/connection' + fake + '/scannedTables', function(data) {
    	    $(container).css('overflow-y', 'hidden');
	    	$(data).each(function() {
	    		$("<div><input class='tables' type='checkbox' name="+this+" value="+this+" checked>" + this + "</div>").hide().appendTo(tablesDiv).slideDown(250);
	    	});
    	    $(container).css('overflow-y', '');
    		fixSideBarMaxHeight(null, true)
	    });
	}

	function getExcludedTables() {
		return $.makeArray( $.map($(".tables"), function(i) { if (!$(i).prop('checked')) { return $(i).val(); } }) );
	}
	
	$("#connectionRender").click(function() {
		filter = {
			showAllColumnsOnTables : $("#showAllColumnsOnTables").prop('checked'), // boolean
			includeTablesWithMoreXRows : $("#includeTablesWithMoreXRows").val(), // long
			tablesToExclude : getExcludedTables(), // Set<String>
			pkFilter : $("#pkFilter option:selected").val(), // NoPK, HasPK, All
			connectWithFKs : $("#connectWithFKs").prop('checked'), // boolean
			showLabelsOnFKs : $("#showLabelsOnFKs").prop('checked'), //boolean
			excludeFKForColumnsNamed : [], //["CREATED_BY", "UPDATED_BY"], // Set<String>
			fkFilter : $("#fkFilter option:selected").val() // NoFK, HasFK, All
		};
		
		$.ajax({
		    url:'/api/db/connection' + fake + '/dot',
		    type:'POST',
		    data: JSON.stringify(filter),
		    dataType: 'text',
		    contentType: "application/json",
		    success:function(res){
			     var dotFile = res;
			     
			     var format = "svg"; // dot, plain, svg, xdot
			     var engine = $("#dbLayout option:selected").val(); // dot, neato
			     
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

	});
	

}

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
		var svg = $("#imgDiv").html();
		
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
				drawGraph();
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
    			var html = "<div class='inputFile'>";
    			html += "<span class='ui-icon ui-icon-circle-minus left sourceDel'/>";
    			html += "<span class='fileText'><span class='fileLabel'>" + this + "</span></span>";
    			html += "<div>";
	    		$(html).hide().appendTo(filesDiv).slideDown(250);
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
    			    	console.log(parent);
    			    	$(parent).remove().slideUp(250);
    			    	loadPackages();
    			    	loadClasses();
    					drawGraph();
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

    		$("<div><input type='button' id='packageCheckAll' value='Uncheck All' /><span class='right'><input id='packageSearch' type='text' placeholder='Package search' value=''/></span></div>").appendTo(packagesDiv).slideDown(250);

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
	    	    
	    		$("<div><input type='button' id='classCheckAll' value='Uncheck All' /><span class='right'><input id='classSearch' type='text' placeholder='Class search' value=''/></span></div>").hide().appendTo(classesDiv).slideDown(250);

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
	    			packageToSearchFor = $("#classSearch").val();
	    			$('.classes').parent().hide();
	    			$('.classes').filter(function() {
	    				return $(this).attr('name').toLowerCase().indexOf(packageToSearchFor.toLowerCase()) > -1;
	    			}).parent().show();;
	    		});
	    		
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

	loadFiles();
	loadPackages();
	loadClasses();

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
//	console.log("availableHeight:", availableHeight, "openContainers:", openContainers, "maxHeight1:", maxHeight1);

	$('.container').css('height', ''); // clear any set heights

	$('.container').each(function() { // if any divs are less than the max height remove them from the calculations and subtract their height
		sameDiv = divChanging != null && $(this).attr('id') == divChanging.attr('id');
		if ((!sameDiv && $(this).css('display') != 'none') || (sameDiv && $(this).css('display') == 'none')) {
//			console.log("outerHeight:", $(this).outerHeight());
			if ($(this).outerHeight() < maxHeight1) {
//				console.log("inside2");
				openContainers--;
				availableHeight -= $(this).outerHeight();
			} 
		}
	});
	
	// asserting that maxHeight2 > maxHeight1
	maxHeight2 = availableHeight / openContainers; // calculate the new max height

//	console.log("availableHeight:", availableHeight, "openContainers:", openContainers, "maxHeight2:", maxHeight2);
//	console.log("window width:", $(window).width(), "window height:", $(window).height());

	$('.container').each(function() { // if the container is open or opening and it's natural height exceeds the new max height set the height 
		sameDiv = divChanging != null && $(this).attr('id') == divChanging.attr('id');
		
//		console.log(sameDiv, ($(this).css('display') == 'none'), $(this), divChanging);
		if (sameDiv && $(this).css('display') != 'none') {
			$(this).css('height', oldHeights[$(this).attr('id')]);
		}
	
		if ((!sameDiv && $(this).css('display') != 'none') || (sameDiv && $(this).css('display') == 'none')) {
//			console.log("outerHeight:", $(this).outerHeight());
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
	                { label: "DB Analyzer", id: "dbAnalyzer", initializer: "dbAnalyzerInit" }, 
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
			$("#"+this.id).hide();
			$("#"+this.id).zIndex(-1);
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