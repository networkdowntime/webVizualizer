// fake is used for redirecting to a fake API
var fake = "";
//fake = "Fake";


// begin variable declarations
var referenceChainDepths = [ 0, 1, 2, 3, 4, 5, 6 ];
var layouts = { 'dot' : 'Top Down', 'neato' : 'Natural' };
var javaDiagramType = { 'PACKAGE_DIAGRAM' : 'Package Diagram', 'CLASS_ASSOCIATION_DIAGRAM' : 'Class Diagram', 'UNREFERENCED_CLASSES' : 'Unreferenced Classes' };
var graphVizFile = null;	
// end variable declarations


// begin attaching actions to the UI elements
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
	fixSideBarMaxHeight($('#javaAnalyzer'), null, true);
});

$.each(layouts, function(value, label) {
	$("#javaLayout").append("<option value='"+value+"'>"+label+"</option>");
});

$.each(javaDiagramType, function(value, label) {
	$("#javaDiagramType").append("<option value='"+value+"'>"+label+"</option>");
});

sourceDir = $.cookie("sourceDir");
if (sourceDir != null) $("#sourceDir").val(sourceDir);

$("#javaSaveToPng").click(function() {
	var svg = $("svg")[0].outerHTML;
	
	var xhr = new XMLHttpRequest();
	var url = "/api/converter/svgToPng";
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
	    data: JSON.stringify({ path: sourceDir }),
	    dataType: 'json',
	    contentType: "application/json",
	    success:function(res){
	    	$.cookie("sourceDir", sourceDir);
	    	loadFiles();
	    	loadPackages();
	    	loadClasses();
	    },
	    error:function(res){
	        alert("Bad things happend! " + res.statusText);
	        alert("Bad things happend! " + res);
	    }
	});
	
});

$("#javaRender").click(function() {
	drawGraph();
});
// end attaching actions to the UI elements


// begin function declarations
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
		fixSideBarMaxHeight($('#javaAnalyzer'), null, true);
		
		$(".sourceDel").click(function() {
			var sourceDir = $(this).siblings(".fileText, fileLabel").text();
			var parent = $(this).parent();
			$.ajax({
			    url:'/api/code/javaScanner' + fake + '/file' + '?' + $.param({'path': sourceDir}),
			    type:'DELETE',
			    success:function(res){
			    	$(parent).remove().slideUp(250);
		    		fixSideBarMaxHeight($('#javaAnalyzer'), null, true);
			    	loadPackages();
			    	loadClasses();
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

		$("<div><input type='button' id='packageCheckAll' value='Uncheck Shown' /><span class='right'><input id='packageSearch' type='text' placeholder='Package search' value=''/></span></div>").appendTo(packagesDiv);

		$('#packageCheckAll:button').click(function() {
			if ($(this).attr("value") === 'Uncheck Shown') {
    	        $('.packages:not(:hidden)').prop('checked', false);;
    	        $(this).val('Check Shown');            				
    	    	loadClasses();
			} else {
    	        $('.packages:not(:hidden)').prop('checked', true);;
    	        $(this).val('Uncheck Shown');
    	    	loadClasses();
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
	    
		fixSideBarMaxHeight($('#javaAnalyzer'), null, true);
	    $(".packages").change(function() {
	    	loadClasses();
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
	    data: JSON.stringify({ excludePackages: uncheckedPackages }),
	    dataType: 'json',
	    contentType: "application/json",
	    success:function(data) {
    	    
    		$("<div><input type='button' id='classCheckAll' value='Uncheck Shown' /><span class='right'><input id='classSearch' type='text' placeholder='Class search' value=''/></span></div>").appendTo(classesDiv);

    		$('#classCheckAll:button').click(function() {
    			if ($(this).attr("value") === 'Uncheck Shown') {
        	        $('.classes:not(:hidden)').prop('checked', false);;
        	        $(this).val('Check Shown');            				
    			} else {
        	        $('.classes:not(:hidden)').prop('checked', true);;
        	        $(this).val('Uncheck Shown');
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

    		fixSideBarMaxHeight($('#javaAnalyzer'), null, false);
	    },
	    error:function(res){
	        alert("Bad thing happend! " + res.statusText);
	        alert("Bad thing happend! " + res);
	    }
	});
}

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
		advancedSearchQuery : $('#advancedSearchQuery').val(), // String
		diagramType : $("#javaDiagramType option:selected").val(), // PACKAGE_DIAGRAM, CLASS_ASSOCIATION_DIAGRAM, METHOD_CALL_DIAGRAM
		showFields : $("#showFields").prop('checked'), // boolean
		showMethods : $("#showMethods").prop('checked'), //boolean
		fromFile : $("#fromFile").prop('checked'), //boolean
		packagesToExclude : uncheckedPackages, // Set<String> of package names
		classesToExclude : uncheckedClasses, // Set<String> of class names
		upstreamReferenceDepth : upstreamReferenceDepth, // integer
		downstreamDependencyDepth : downstreamDependencyDepth // integer
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
// end function declarations
