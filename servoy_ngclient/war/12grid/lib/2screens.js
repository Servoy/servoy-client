function screenToggle(element) {
		$(element.parentElement.parentElement.firstElementChild).toggleClass('in');
		$(element.parentElement.parentElement.lastElementChild).toggleClass('in');
}
