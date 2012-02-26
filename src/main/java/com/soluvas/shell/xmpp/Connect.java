/**
 * 
 */
package com.soluvas.shell.xmpp;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * @author ceefour
 * Connect to XMPP Server.
 */
@Command(name="connect", scope="xmpp", description="Connect to XMPP Server.",
	detailedDescription="Connect and log in to XMPP Server specified by" +
			" 'serverHost', 'username', and 'password' configuration in 'com.soluvas.shell.xmpp' service PID." +
			" It will then start accepting shell commands over XMPP chat messages.")
public class Connect extends OsgiCommandSupport {

	private XmppShell shell;
	
	/* (non-Javadoc)
	 * @see org.apache.karaf.shell.console.AbstractAction#doExecute()
	 */
	@Override
	protected Object doExecute() throws Exception {
		getShell().connect();
		return null;
	}

	public XmppShell getShell() {
		return shell;
	}

	public void setShell(XmppShell shell) {
		this.shell = shell;
	}

}
