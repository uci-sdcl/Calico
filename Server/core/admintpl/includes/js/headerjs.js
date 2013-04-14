
var jump_page = 'Enter the page number you wish to go to:';
var on_page = '';
var per_page = '';
var base_url = '';

var menu_state = 'shown';


/** Jump to page */
function jumpto()
{
	var page = prompt(jump_page, on_page);

	if(page !== null && !isNaN(page) && page == Math.floor(page) && page > 0)
	{
		if(base_url.indexOf('?') == -1)
		{
			document.location.href = base_url + '?start=' + ((page - 1) * per_page);
		}
		else
		{
			document.location.href = base_url.replace(/&amp;/g, '&') + '&start=' + ((page - 1) * per_page);
		}
	}
}

/* Set display of page element s[-1,0,1] = hide,toggle display,show */
function dE(n, s, type)
{
	if(!type)
	{
		type = 'block';
	}

	var e = document.getElementById(n);
	if(!s)
	{
		s = (e.style.display == '') ? -1 : 1;
	}
	e.style.display = (s == 1) ? type : 'none';
}

/* Mark/unmark checkboxes * id = ID of parent container, name = name prefix, state = state [true/false] */
function marklist(id, name, state)
{
	var parent = document.getElementById(id);
	if(!parent)
	{
		eval('parent = document.' + id);
	}

	if(!parent)
	{
		return;
	}

	var rb = parent.getElementsByTagName('input');
	
	for(var r=0; r < rb.length; r++)
	{
		if(rb[r].name.substr(0, name.length) == name)
		{
			rb[r].checked = state;
		}
	}
}

/* Find a member */
function find_username(url)
{
	popup(url, 760, 570, '_usersearch');
	return false;
}

/* Window popup */
function popup(url, width, height, name)
{
	if(!name)
	{
		name = '_popup';
	}

	window.open(url.replace(/&amp;/g, '&'), name, 'height=' + height + ',resizable=yes,scrollbars=yes, width=' + width);
	return false;
}

/* Hiding/Showing the side menu */
function switch_menu()
{
	var menu = $('menu');
	var main = $('main');
	var toggle = $('toggle');
	var handle = $('toggle-handle');

	switch (menu_state)
	{
		case 'shown':
			main.style.width = '93%';
			menu_state = 'hidden';
			menu.style.display = 'none';
			toggle.style.width = '20px';
			handle.style.backgroundImage = 'url(/gui/images/toggle.gif)';
			handle.style.backgroundRepeat = 'no-repeat';
			handle.style.backgroundPosition = '100% 50%';
			toggle.style.left = '0';
			break;
		case 'hidden':
			main.style.width = '76%';
			menu_state = 'shown';
			menu.style.display = 'block';
			toggle.style.width = '5%';
			handle.style.backgroundImage = 'url(/gui/images/toggle.gif)';
			handle.style.backgroundRepeat = 'no-repeat';
			handle.style.backgroundPosition = '0% 50%';
			toggle.style.left = '15%';
			break;
	}
}
