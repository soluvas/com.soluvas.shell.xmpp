
package com.soluvas.shell.xmpp;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;


/**
 * Displays the last log entries
 */
public class XmppCommands {

	private XmppShell shell;

	public XmppCommands() {
		System.out.println("I am constructed!");
	}
	
	@PostConstruct public void init() {
		System.out.println("I am @PostConstruct!");
	}
	
	@PreDestroy public void destroy() throws Exception {
	}

//	@Command(scope = "xmpp", name = "hello", description = "Say Hello")
	@Descriptor("Say hello")
    public String hello() throws Exception {
         System.out.println("Executing command hello");
         return "Hello World!";
    }
    
	@Descriptor("Connect to XMPP Server")
    public void connect(@Descriptor("XMPP Server host") String host) throws Exception {
		shell.setServerHost(host);
		shell.connect();
   }

	@Descriptor("Connect to XMPP Server")
    public void connect() throws Exception {
		getShell().connect();
   }

	public void disconnect() throws Exception {
		getShell().disconnect();
	}

	public XmppShell getShell() {
		return shell;
	}

	public void setShell(XmppShell shell) {
		this.shell = shell;
	}
    
}
