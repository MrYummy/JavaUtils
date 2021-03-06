command busy {
	perm utils.imbusy;
	
	on {
		type player;
		perm utils.imbusy.use;
		run busy_on;
		help Toggles your busy status on;
	}
	
	off {
		type player;
		perm utils.imbusy.use;
		run busy_off;
		help Toggles your busy status off;
	}
	
	toggle {
		type player;
		perm utils.imbusy.use;
		run busy_toggle;
		help Toggles your busy status;
	}
	
	status [string:player] {
		help Checks whether a player is busy;
		run busy_status player;
	}
}