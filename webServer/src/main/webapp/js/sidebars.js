function isSameDiv(divChanging, otherDiv) {
	return divChanging != null && $(otherDiv).attr('id') == divChanging.attr('id');
};

function openContainerCount(parentDiv, divChanging) {
	var openContainers = 0;
	$(parentDiv).find('.container').each(function() { // count all open or opening containers
		var sameDiv = isSameDiv(divChanging, $(this));
		if ((!sameDiv && $(this).css('display') != 'none') || (sameDiv && $(this).css('display') == 'none')) { 
			openContainers++;
		} 
	});	
	return openContainers;
};

function getCurrentHeights(parentDiv) {
	var heights = new Object();
	$(parentDiv).find('.container').each(function() { // count all open or opening containers
		heights[$(this).attr('id')] = $(this).outerHeight();
	});
	return heights;
};

function getTotalCollapsibleLabelBarHeight(parentDiv) {
	var height = 0;
	$(parentDiv).find('.collapsible').each(function() { height += $(this).outerHeight(); });
	return height;
};

function isVisible(element) {
	return $(element)
};


function fixSideBarMaxHeight(parentDiv, divChanging, animateChange) {
	var totalWindowHeight = $(window).height();
	var	totalLabelBarHeight = getTotalCollapsibleLabelBarHeight(parentDiv);
	var availableHeight = (totalWindowHeight - totalLabelBarHeight - $(parentDiv).offset().top);
	var oldNaturalHeights = getCurrentHeights(parentDiv);
	var openContainers = openContainerCount(parentDiv, divChanging);

	var evenHeight = availableHeight / openContainers; // if an even height split
	
//	console.log("totalWindowHeight:", totalWindowHeight, "total window height:", $(window).height() , "totalLabelBarHeight:", totalLabelBarHeight);
//	console.log("availableHeight:", availableHeight, "openContainers:", openContainers, "evenHeight:", evenHeight);
//	console.log(oldNaturalHeights);
	
	$(parentDiv).find('.container').css('height', ''); // clear any set heights

	var changing = false;
	var prevEvenHeight = 0;
	do {
		changing = false;
		$(parentDiv).find('.container').each(function() { // if any divs are less than the even height remove them from the calculations and subtract their height
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


	// if the container is open or opening and it's natural height exceeds the new max height set the height
	$(parentDiv).find('.container').each(function() {  
		var sameDiv = isSameDiv(divChanging, $(this));
		
//		console.log('fixing heights', 'div', $(this).attr('id'), 'sameDiv', sameDiv);
		
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
	
//	console.log('final heights', getCurrentHeights(parentDiv));
}
