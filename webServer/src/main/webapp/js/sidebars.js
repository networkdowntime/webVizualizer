function isSameDiv(divChanging, otherDiv) {
	return divChanging != null && $(otherDiv).attr('id') == divChanging.attr('id');
};

function openContainerCount(divChanging) {
	var openContainers = 0;
	$('.container').each(function() { // count all open or opening containers
		var sameDiv = isSameDiv(divChanging, $(this));
		if ((!sameDiv && $(this).css('display') != 'none') || (sameDiv && $(this).css('display') == 'none')) { 
			openContainers++;
		} 
	});	
	return openContainers;
};

function getCurrentHeights() {
	var heights = new Object();
	$('.container').each(function() { // count all open or opening containers
		heights[$(this).attr('id')] = $(this).outerHeight();
	});
	return heights;
};

function getTotalCollapsibleLabelBarHeight() {
	var height = 0;
	$('.collapsible').each(function() { height += $(this).outerHeight(); });
	return height;
};

function isVisible(element) {
	return $(element)
};


function fixSideBarMaxHeight(divChanging, animateChange) {
	
	var totalWindowHeight = $(window).height();
	var	totalLabelBarHeight = getTotalCollapsibleLabelBarHeight();
	var availableHeight = (totalWindowHeight - totalLabelBarHeight - $(".sidebar").offset().top);
	var oldNaturalHeights = getCurrentHeights();
	var openContainers = openContainerCount(divChanging);

	var evenHeight = availableHeight / openContainers; // if an even height split
	
//	console.log("totalWindowHeight:", totalWindowHeight, "total window height:", $(window).height() , "totalLabelBarHeight:", totalLabelBarHeight);
//	console.log("availableHeight:", availableHeight, "openContainers:", openContainers, "evenHeight:", evenHeight);
//	console.log(oldNaturalHeights);
	
	$('.container').css('height', ''); // clear any set heights

	var changing = false;
	var prevEvenHeight = 0;
	do {
		changing = false;
		$('.container').each(function() { // if any divs are less than the even height remove them from the calculations and subtract their height
			var sameDiv = isSameDiv(divChanging, $(this));
			if ((!sameDiv && $(this).is(':visible')) || (sameDiv && $(this).is(':hidden'))) {
				if ($(this).outerHeight() < evenHeight && $(this).outerHeight() >= prevEvenHeight) {
					openContainers--;
					availableHeight -= $(this).outerHeight();
					changing = true;
				} 
			}
//			console.log($(this).attr('id'), $(this).outerHeight());
		});
		prevEvenHeight = evenHeight;
		evenHeight = availableHeight / openContainers; // calculate the new max height
//		console.log("adjusted availableHeight:", availableHeight, "adjusted openContainers:", openContainers, "evenHeight: ", evenHeight);
	} while (changing && openContainers > 0);
	
	var maxHeight2 = availableHeight / openContainers; // calculate the new max height
//	console.log("adjusted availableHeight:", availableHeight, "adjusted openContainers:", openContainers, "maxHeight2: ", maxHeight2);


	$('.container').each(function() { // if the container is open or opening and it's natural height exceeds the new max height set the height 
		var sameDiv = isSameDiv(divChanging, $(this));
		
		// start by resetting to the old height
		if (sameDiv && $(this).is(':visible')) {
			$(this).css('height', oldNaturalHeights[$(this).attr('id')]);
		}
	
		if ((!sameDiv && $(this).is(':visible')) || (sameDiv && $(this).is(':hidden'))) {
//			console.log("outerHeight:", $(this).outerHeight());
			var outerHeight = $(this).outerHeight();
			if (outerHeight > maxHeight2) {
				if (sameDiv) {
					$(this).css('height', maxHeight2+'px');
				} else {
					if (animateChange) {
//						console.log('animate change1', $(this).attr('id'), oldNaturalHeights[$(this).attr('id')]);
						$(this).css('height', oldNaturalHeights[$(this).attr('id')]+'px').animate({ height: maxHeight2 }, 'fast');
					} else {
//						console.log('change1', $(this).attr('id'), maxHeight2);
						$(this).height(maxHeight2).animate({ height: maxHeight2 }, 'fast');;
					}
				}
			} else {
				if (!sameDiv) {
					if (animateChange) {
//						console.log('animate change2', $(this).attr('id'), oldNaturalHeights[$(this).attr('id')]);
						$(this).height(outerHeight).animate({ height: outerHeight }, 'fast');
					} else {				
//						console.log('change2', $(this).attr('id'), outerHeight);
						$(this).height(outerHeight).animate({ height: outerHeight }, 'fast');
					}
				}
			}
		}
	});
	
//	console.log('final heights', getCurrentHeights());
}
