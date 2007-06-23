package org.lnicholls.galleon.widget;
/*
 * Copyright (C) 2007 John T. Kohl
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * See the file "COPYING" for more details.
 */

import com.almilli.tivo.hme.hd.Resolution;
import com.tivo.hme.interfaces.IContext;

public class DefaultSDOnlyApplication extends DefaultApplication {
	protected void initApp(IContext context) {
		// don't initService, we have already done it earlier while in SD.
		// initService();
	}
    public void setReceiverResolution(Resolution res) {
    	// nothing, just override so we never do it.
    }
    public void init(IContext context) throws Exception {
		super.init(context);
		// call initService() before any resolution processing, to keep it SD.
		// XXX does this always work?  Could we be HD already?
		initService();
	}
}
