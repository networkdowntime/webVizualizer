/**
 * 
 */
var fake = "";
//fake = "Fake";

var layouts = { 'dot' : 'Top Down', 'neato' : 'Natural' };
var pkFilter = { 'All' : 'All', 'NoPK' : 'No PK', 'HasPK' : 'Has PK' };
var fkFilter = { 'All' : 'All', 'NoFK' : 'No FK', 'HasFK' : 'Has FK' };

	
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
		$(schemasDiv).empty();

		$.get('/api/db/connection' + fake + '/schemasWithTables', function(data) {
    	    
    		$("<div><input type='button' id='schemaCheckAll' value='Check Shown' /><span class='right'><input id='schemaSearch' type='text' placeholder='Schema search' value=''/></span></div>").appendTo(schemasDiv);

    		$('#schemaCheckAll:button').click(function() {
    			if ($(this).attr("value") === 'Uncheck Shown') {
        	        $('.schemas:not(:hidden)').prop('checked', false);;
        	        $(this).val('Check Shown');            				
    	    		scanSchemas();
    			} else {
        	        $('.schemas:not(:hidden)').prop('checked', true);;
        	        $(this).val('Uncheck Shown');
    	    		scanSchemas();
    			}
    	    });
    	    
    		$('#schemaSearch').on('change keyup paste', function() {
    			schemaToSearchFor = $("#schemaSearch").val();
    			$('.schemas').parent().hide();
    			$('.schemas').filter(function() {
    				return $(this).attr('name').toLowerCase().indexOf(schemaToSearchFor.toLowerCase()) > -1;
    			}).parent().show();
    		});
    		
	    	
    	    $(data).each(function() {
	    		$("<div><input class='schemas' type='checkbox' name="+this+" value="+this+">" + this + "</div>").appendTo(schemasDiv);
	    	});

    		fixSideBarMaxHeight($('#dbAnalyzer'), null, true)
	    	
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
    	    
    		$("<div><input type='button' id='tableCheckAll' value='Uncheck Shown' /><span class='right'><input id='tableSearch' type='text' placeholder='Table search' value=''/></span></div>").appendTo(tablesDiv);

    		$('#tableCheckAll:button').click(function() {
    			if ($(this).attr("value") === 'Uncheck Shown') {
        	        $('.tables:not(:hidden)').prop('checked', false);;
        	        $(this).val('Check Shown');            				
        			drawGraph();
    			} else {
        	        $('.tables:not(:hidden)').prop('checked', true);;
        	        $(this).val('Uncheck Shown');
        			drawGraph();
    			}
    	    });
    	    
    		$('#tableSearch').on('change keyup paste', function() {
    			tableToSearchFor = $("#tableSearch").val();
    			$('.tables').parent().hide();
    			$('.tables').filter(function() {
    				return $(this).attr('name').toLowerCase().indexOf(tableToSearchFor.toLowerCase()) > -1;
    			}).parent().show();
    		});
    		
			$(data).each(function() {
	    		$("<div><input class='tables' type='checkbox' name="+this+" value="+this+" checked>" + this + "</div>").appendTo(tablesDiv);
	    	});

	    	fixSideBarMaxHeight($('#dbAnalyzer'), null, true)
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
			     
			     $('svg').remove();
			     $("#scrollDiv").after(result);
			     initializeDragScrollZoom();
		    },
		    error:function(res){
		        alert("Bad thing happend! " + res.statusText);
		        alert("Bad thing happend! " + res);
		    }
		});

	});
