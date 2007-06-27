/*
 * Copyright (C) 2007, John T Kohl
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
package org.lnicholls.galleon.server;


import com.apple.eawt.*;

public class OSXServer extends ApplicationAdapter {

	private static Server mServer;
	private static com.apple.eawt.Application theApplication;

	public void handleQuit(ApplicationEvent ae) {
		
		if (mServer != null) {
			ae.setHandled(false);
			try {
				mServer.stop();
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			throw new IllegalStateException("Unexpected quit before server initialized?");
		}
	}
		

	public static void main(String args[]) {
		theApplication = new com.apple.eawt.Application();
		theApplication.addApplicationListener(new OSXServer());
		mServer = Server.getServer();
		// Set up action event handler
	}
}
