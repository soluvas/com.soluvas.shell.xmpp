/**
 * 
 */
package com.soluvas.shell.xmpp;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

/**
 * @author ceefour
 * Disconnect from XMPP Server.
 */
@Command(name="disconnect", scope="xmpp", description="Disconnect to XMPP Server.",
detailedDescription="Disconnect from XMPP Server and stop accepting shell commands via XMPP chat messages.")
public class Disconnect extends OsgiCommandSupport {

	private XmppShell shell;
	
	/* (non-Javadoc)
	 * @see org.apache.karaf.shell.console.AbstractAction#doExecute()
	 */
	@Override
	protected Object doExecute() throws Exception {
		getShell().disconnect();
		return null;
	}

	public XmppShell getShell() {
		return shell;
	}

	public void setShell(XmppShell shell) {
		this.shell = shell;
	}
	
}
