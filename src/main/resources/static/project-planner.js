(() => {
	const application = Stimulus.Application.start();

	application.register('form', class extends Stimulus.Controller {
		submitForm(event) {
			event.preventDefault();

			const request = new XMLHttpRequest();
			request.open(event.target.getAttribute('method'), event.target.getAttribute('action'));
			request.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
			request.onreadystatechange = function () {
				function decodeHtml(html) {
					var txt = document.createElement("textarea");
					txt.innerHTML = html;
					return txt.value;
				}

				if (request.readyState === 4) {
					if(request.status >= 200 && request.status <= 299) {
						Turbolinks.visit(window.location.href);
					} else {
						document.getElementById('css-loader').classList.remove('is-active');
						alert(decodeHtml(request.responseText.substring(request.responseText.indexOf('<div>') + 5, 
								request.responseText.indexOf('</div>'))));
					}
				}
			};
			
			// NOTE Loader will be removed automatically by Turoblinks with the new HTML that contains the disabled 
			//      loader (only in case of an error it has to be removed manually).
			document.getElementById('css-loader').classList.add('is-active');
			
			request.send(new FormData(event.target));
		}
	});

	Turbolinks.start();

	//
	// Source of scroll position preservation: https://spenser.xyz/notes/turbolinks-scroll-position
	//

	Turbolinks.scrollPosition = {x: 0, y: 0};
	Turbolinks.lastHref = undefined;
	
	window.actionDetailStatus = {};
	
	document.addEventListener('turbolinks:before-visit', function () {
		// Before visit, simply store scroll position & url/href.
		Turbolinks.scrollPosition = {x: window.scrollX, y: window.scrollY};
		Turbolinks.lastHref = window.location.href;
		
		// Store the open status of all details tags.
		const detailEls = document.querySelectorAll('details');
		for(var i = 0; i < detailEls.length; i++) {
			window.actionDetailStatus[detailEls[i].id] = detailEls[i].open;
		}
	});

	document.addEventListener('turbolinks:load', function () {
		// Restore the open status of all details tags before restoring the scroll position.
		const detailEls = document.querySelectorAll('details');
		for(var i = 0; i < detailEls.length; i++) {
			if(window.actionDetailStatus[detailEls[i].id]) {
				detailEls[i].open = window.actionDetailStatus[detailEls[i].id];
			}
		}
		
		// If we have a scroll position AND we're on the same page.
		if (Turbolinks.scrollPosition && (this.location.href === Turbolinks.lastHref)) {
			// Scroll to our previous position
			window.scrollTo(Turbolinks.scrollPosition.x, Turbolinks.scrollPosition.y);
		}
	});
})();