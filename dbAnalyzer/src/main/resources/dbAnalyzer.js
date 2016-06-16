// begin variable declarations
var layouts = { 'dot' : 'Top Down', 'neato' : 'Natural' };
var pkFilter = { 'All' : 'All', 'NoPK' : 'No PK', 'HasPK' : 'Has PK' };
var fkFilter = { 'All' : 'All', 'NoFK' : 'No FK', 'HasFK' : 'Has FK' };
var model = new Object();
// end variable declarations


// begin attaching actions to the UI elements
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

$.get('/api/db/dbScanner/supportedDatabases', function(data) {
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

$("#connectionSaveToPng").click(function() {
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

$("#addJdbcUrl").click(function() {
	var dbType = $("#dbType option:selected").val();
	var user = $.trim($("#username").val());
	var passwd = $.trim($("#password").val());
	var url = $.trim($("#jdbcUrl").val());
	
	$.ajax({
	    url:'/api/db/dbScanner/connection',
	    type:'POST',
	    data: JSON.stringify({ dbType: dbType, username: user, password: passwd, jdbcUrl: url}),
	    dataType: 'json',
	    contentType: "application/json",
	    success:function(res){
			$.cookie("dbType", dbType);
			$.cookie("dbUser", user);
			$.cookie("dbPasswd", passwd);
			$.cookie("dbUrl", url);

			loadUrls();
			loadSchemas();
	    },
	    error:function(res){
	        alert("Bad things happend! " + res.statusText);
	        alert("Bad things happend! " + res);
	    }
	});
});

$("#connectionRender").click(function() {
	drawDbGraph();
});
// end attaching actions to the UI elements

loadUrls();

// begin function declarations
function loadUrls() {
	container = $("#connectionDiv").next();
	urlsDiv = $("#jdbcUrls");

	$(urlsDiv).empty();
	$.get('/api/db/dbScanner/connections', function(data) {
		model = data;
		
	    $(container).css('overflow-y', 'hidden');
		$(model).each(function() {
    	    $(container).css('overflow-y', 'hidden');
			var html = "<div class='inputJdbcUrl'>";
			html += "<span class='ui-icon ui-icon-circle-minus left add-remove-icon-align jdbcUrlDel'/>";
			html += "<span class='jdbcText'><span class='jdbcLabel'>" + this.url + "</span></span>";
			html += "<div>";
    		$(html).appendTo(urlsDiv);
    	});
	    $(container).css('overflow-y', '');
		fixSideBarMaxHeight($('#dbAnalyzer'), null, true);
		
		$(".jdbcUrlDel").click(function() {
			var jdbcUrl = $(this).siblings(".jdbcText, jdbcLabel").text();
			var parent = $(this).parent();
			$.ajax({
			    url:'/api/db/dbScanner/connection' + '?' + $.param({'url': jdbcUrl}),
			    type:'DELETE',
			    success:function(res){
			    	$(parent).remove().slideUp(250);
			    	loadSchemas();
//					$('#' + jdbcUrl.replace(/[:\/]/g, '_')).remove();
		    		fixSideBarMaxHeight($('#dbAnalyzer'), null, true);
			    },
			    error:function(res){
			        alert("Bad things happend! " + res.statusText);
			        alert("Bad things happend! " + res);
			    }
			});
			
		});
		
		loadSchemas();
	});
}

function loadSchemas() {
	container = $("#schemasDiv").next();
	schemasDiv = $(container).children(".content");
	$(schemasDiv).empty();

	$("<div><input type='button' id='schemaCheckAll' value='Check Shown' /><span class='right'><input id='schemaSearch' type='text' placeholder='Schema search' value=''/></span></div>").appendTo(schemasDiv);

	$('#schemaCheckAll:button').click(function() {
		if ($(this).attr("value") === 'Uncheck Shown') {
	        $('.schemas:not(:hidden)').prop('checked', false);;
	        $(this).val('Check Shown');            				
    		loadTables();
		} else {
	        $('.schemas:not(:hidden)').prop('checked', true);;
	        $(this).val('Uncheck Shown');
	        loadTables();
		}
    });
    
	$('#schemaSearch').on('change keyup paste', function() {
		schemaToSearchFor = $("#schemaSearch").val();
		$('.schemas').parent().hide();
		$('.schemas').filter(function() {
			return $(this).attr('name').toLowerCase().indexOf(schemaToSearchFor.toLowerCase()) > -1;
		}).parent().show();
	});
	
	$(model).each(function() {
    	var schemas = "<div id='schema_" + this.url.replace(/[:\/]/g, '_') + "'>\r<div>" + this.url + "</div>\r";
	    $(this.schemas).each(function() {
	    	var schemaId = (this.url + '.' + this.schemaName).replace(/[:\/]/g, '_');
		    schemas += "<div><input class='schemas' type='checkbox' id=" + schemaId + " name=" + schemaId + " value=" + schemaId + ">" + this.schemaName + "</div>\r";
    	});
	    schemas += "</div>";
	    $(schemas).appendTo(schemasDiv);
	});

	fixSideBarMaxHeight($('#dbAnalyzer'), null, true)
	
	$('.schemas').change(function() {
		loadTables();
	});

	loadTables();

}

function jqEscape(id) {
	return "#" + id.replace( /(:|\.|\[|\]|,)/g, "\\$1" ); //.replace( /(\/)/g, "\\\\$1");
}

function loadTables() {
	var checkedSchemas = $.makeArray( $.map($(".schemas"), function(i) { if ($(i).prop('checked')) { return $(i).val(); } }) );
//	console.log(checkedSchemas);
	
	container = $("#tablesDiv").next();
	tablesDiv = $(container).children(".content");
	$(tablesDiv).empty();

	$("<div><input type='button' id='tableCheckAll' value='Uncheck Shown' /><span class='right'><input id='tableSearch' type='text' placeholder='Table search' value=''/></span></div>").appendTo(tablesDiv);

	$('#tableCheckAll:button').click(function() {
		if ($(this).attr("value") === 'Uncheck Shown') {
	        $('.tables:not(:hidden)').prop('checked', false);;
	        $(this).val('Check Shown');            				
		} else {
	        $('.tables:not(:hidden)').prop('checked', true);;
	        $(this).val('Uncheck Shown');
		}
    });
    
	$('#tableSearch').on('change keyup paste', function() {
		tableToSearchFor = $("#tableSearch").val();
		$('.tables').parent().hide();
		$('.tables').filter(function() {
			return $(this).attr('name').toLowerCase().indexOf(tableToSearchFor.toLowerCase()) > -1;
		}).parent().show();
	});
	
	$(model).each(function() {
    	var tables = "<div>\r<div>" + this.url + "</div>\r";
    	var hasVisibleTable = false;
    	
    	$(this.schemas).each(function() {
	    	var schemaId = (this.url + '.' + this.schemaName).replace(/[:\/]/g, '_');

	    	if ($( jqEscape(schemaId) ).prop('checked')) {
			    $(this.tables).each(function() {
			    	hasVisibleTable = true;
			    	var tableId = (schemaId + '.' + this.tableName).replace(/[:\/]/g, '_');
			    	tables += "<div><input class='tables' type='checkbox' name=" + tableId + " value=" + this.schemaName + "." + this.tableName + " checked>" + this.schemaName + "." + this.tableName + "</div>\r";
			    });
	    	}
	    });
    	
    	if (hasVisibleTable) {
		    tables += "</div>";
		    $(tables).appendTo(tablesDiv);
		}
	});

	fixSideBarMaxHeight($('#dbAnalyzer'), null, true)
}

function getIncludedTables() {
	 return $.makeArray( $.map($(".tables"), function(i) { if ($(i).prop('checked')) { return ($(i).parent().parent().first().first().children().first().html() + "." + $(i).val()); } }) );
}

function drawDbGraph() {
	var excludeFKForColumnsNamed = $('#excludeFKForColumnsNamed').val().split(',');
	$(excludeFKForColumnsNamed).each(function(i, val) {
		excludeFKForColumnsNamed[i] = val.trim();
	});
	
	var excludeTablesContaining = $('#excludeTablesContaining').val().split(',');
	$(excludeTablesContaining).each(function(i, val) {
		excludeTablesContaining[i] = val.trim();
	});
	
	filter = {
		showAllColumnsOnTables : $("#showAllColumnsOnTables").prop('checked'), // boolean
		includeTablesWithMoreXRows : $("#includeTablesWithMoreXRows").val(), // long
		pkFilter : $("#pkFilter option:selected").val(), // NoPK, HasPK, All
		fkFilter : $("#fkFilter option:selected").val(), // NoFK, HasFK, All
		connectWithFKs : $("#connectWithFKs").prop('checked'), // boolean
		showLabelsOnFKs : $("#showLabelsOnFKs").prop('checked'), //boolean
		excludeFKForColumnsNamed : excludeFKForColumnsNamed, //["CREATED_BY", "UPDATED_BY"], // Set<String>
		excludeTablesContaining : excludeTablesContaining,
		tablesToInclude : getIncludedTables() // Set<String>
	};
	
	$.ajax({
	    url:'/api/db/dbScanner/dot',
	    type:'POST',
	    data: JSON.stringify(filter),
	    dataType: 'text',
	    contentType: "application/json",
	    success:function(res){
		     var dotFile = res;
		     
		     var format = "svg"; // dot, plain, svg, xdot
		     var engine = $("#dbLayout option:selected").val(); // dot, neato
		     var result = Viz(dotFile, format, engine);
		     
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