/**
 * 
 */
package com.soluvas.shell.xmpp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.fusesource.jansi.AnsiOutputStream;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.PacketExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ceefour
 * Connects to an XMPP Server and exposes the GoGo Shell
 * via XMPP chat messages.
 */
public class XmppShell {

	private transient Logger log = LoggerFactory.getLogger(XmppShell.class);
	
	private boolean autoConnect = false;
	private String serverHost = "localhost";
	private String username = "karaf";
	private String password = "";
	private CommandProcessor commandProcessor;
	
	private XMPPConnection conn;

	@PostConstruct public void init() throws Exception {
		log.info("Soluvas XMPP Shell initialized");
		if (autoConnect) {
			new Thread("Soluvas XMPP Shell - Connect") {
				@Override
				public void run() {
					connect();
				}
			}.start();
		}
	}
	
	@PreDestroy public void destroy() throws Exception {
		disconnect();
	}
	
	public class HtmlMessage extends Message {

		String html;
		
		public HtmlMessage(String plain, String html) {
			super();
			setBody(plain, html);
			addExtension(new PacketExtension() {
				
				@Override
				public String toXML() {
					return "<html xmlns=\"http://jabber.org/protocol/xhtml-im\"><body>" + HtmlMessage.this.html + "</body></html>";
				}
				
				@Override
				public String getNamespace() {
					return "http://jabber.org/protocol/xhtml-im";
				}
				
				@Override
				public String getElementName() {
					return "html";
				}
			});
		}
		
		public void setBody(String plain, String html) {
			super.setBody(plain);
			this.html = html;
		}
	}
	
	/**
	 * Reference: http://xmpp.org/extensions/xep-0071.html
	 * @author ceefour
	 *
	 */
	public class HtmlAnsiOutputStream extends AnsiOutputStream {

		OutputStreamWriter writer;
		/**
		 * Colors from Solarized: http://ethanschoonover.com/solarized
		 */
		String[] colors = new String[] {"#002b36",
			"#073642", "#586e75", "#657b83", "#839496",
			"#93a1a1", "#eee8d5", "#fdf6e3", "#b58900", "#cb4b16", "#dc322f", "#d33682", "#6c71c4", "#268bd2", "#2aa198",
			"#859900"};
		int[] colorMappings = new int[] { 1, 10, 15, 8, 13, 11, 14, 6, 0, 9, 2, 3, 4, 12, 5, 7 };
		String mode = "";
		String foreColor = "";
		String backColor = "";
		String fontFamily;
		
		public HtmlAnsiOutputStream(OutputStream os, String fontFamily) {
			super(os);
			writer = new OutputStreamWriter(os);
			this.fontFamily = fontFamily;
			enhance(getSpan(), "span");
		}
		
		public void enhance(String tag, String mode) {
			this.mode = mode;
			try {
				writer.write(tag);
				writer.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public void restore() {
			if (mode.equals("strong")) {
				enhance("</strong>", "");
			}
			if (mode.equals("em")) {
				enhance("</em>", "");
			}
			if (mode.equals("span")) {
				enhance("</span>", "");
			}
			if (mode.equals("u")) {
				enhance("</u>", "");
			}
		}
		
		@Override
		protected void processAttributeRest() throws IOException {
			foreColor = "";
			backColor = "";
			restore();
		}
		
		@Override
		protected void processSetAttribute(int attribute) throws IOException {
			restore();
			
			switch (attribute) {
			case ATTRIBUTE_INTENSITY_BOLD:
				enhance("<strong>", "strong");
				break;
			case ATTRIBUTE_ITALIC:
				enhance("<em>", "em");
				break;
			case ATTRIBUTE_UNDERLINE:
				enhance("<u>", "u");
				break;
			case ATTRIBUTE_NEGATIVE_ON:
				backColor = colors[colorMappings[0]];
				foreColor = colors[colorMappings[7]];
				break;
			default:
				foreColor = "";
				backColor = "";
			}
		}
		
		@Override
		protected void processSetForegroundColor(int color) throws IOException {
			foreColor = colors[colorMappings[color]];
			restore();
			enhance(getSpan(), "span");
		}
		
		@Override
		protected void processSetBackgroundColor(int color) throws IOException {
			backColor = colors[colorMappings[color]];
			restore();
			enhance(getSpan(), "span");
		}
		
		protected String getSpan() {
			String s = "<span style=\"";
			// not working fully
//			if (!fontFamily.isEmpty())
//				s += "font-family:" + fontFamily +";";
			if (!foreColor.isEmpty())
				s += "color:" + foreColor + ";";
			if (!backColor.isEmpty())
				s += "background:" + backColor + ";";
			s += "\">";
			return s;
		}
	}
	
	public class HtmlChatOutputStream extends OutputStream {
		HtmlAnsiOutputStream html;
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		Chat chat;
		
		public HtmlChatOutputStream(Chat chat) {
			this.chat = chat;
			html = new HtmlAnsiOutputStream(buf, "monospace");
		}
		
		@Override
		public void write(int b) throws IOException {
			if (b == 0x0a) {
				try {
					html.restore();
					final String msg = buf.toString();
					String htmlBody = msg;
					htmlBody = htmlBody.replaceAll("<[^>]*><\\/[^>]*>", ""); // cleanup empty spans
					log.info("Send: {}", htmlBody);
					Message message = new HtmlMessage(msg, htmlBody);
					chat.sendMessage(message);
					buf.reset();
				} catch (Exception e) {
					throw new IOException("Cannot send chat", e);
				}
			} else {
				html.write(b);
			}
		}
	}
	
	public class DumbChatOutputStream extends OutputStream {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		Chat chat;
		
		public DumbChatOutputStream(Chat chat) {
			this.chat = chat;
		}
		
		@Override
		public void write(int b) throws IOException {
			if (b == 0x0a) {
				try {
					final String msg = buf.toString();
					log.info("Send: {}", msg);
					String html = "<strong>" + msg + "</strong>";
					chat.sendMessage(msg);
					buf.reset();
				} catch (Exception e) {
					throw new IOException("Cannot send chat", e);
				}
			} else if (!Character.isISOControl(b)) {
				buf.write(b);
			}
		}
		
	}

    public void connect() {
    	disconnect();
    	
    	conn = new XMPPConnection(getServerHost());
    	log.info("Connecting to {}...", serverHost);
//    	System.out.println(String.format("Connecting to %s...", getServerHost()));
    	try {
			conn.connect();
		} catch (XMPPException e) {
			log.error("Cannot connect to " + serverHost, e);
			throw new RuntimeException(e);
		}
    	log.info("Connected to {}. Logging in as {}...", serverHost, username);
//    	System.out.println("Connected.");
    	try {
			conn.login(getUsername(), getPassword());
		} catch (XMPPException e) {
			log.error("Cannot login to " + serverHost + " as " + username, e);
			conn.disconnect();
			conn = null;
			throw new RuntimeException(e);
		}
    	log.info("Logged in to {} as {}", serverHost, username);
//    	System.out.println("Logged in.");
    	ChatManager chatManager = conn.getChatManager();
    	chatManager.addChatListener(new ChatManagerListener() {
			
			@Override
			public void chatCreated(Chat chat, boolean arg1) {
				log.info("Got chat {}", chat.getParticipant());
//				System.out.println("Got chat " + chat.getParticipant());
				chat.addMessageListener(new MessageListener() {
					
					@Override
					public void processMessage(final Chat chat, Message msg) {
						log.info("Received: {}", msg.getBody());
//						System.out.println("Got message "+ msg.getBody());
						final OutputStream chatBuf = new OutputStream() {
							
							ByteArrayOutputStream byteBuf = new ByteArrayOutputStream();
							AnsiOutputStream htmlOut = new AnsiOutputStream(byteBuf) {
								OutputStreamWriter writer;
								
								public String state = "";
								
								@Override
								public void close() throws IOException {
//									resetState();
									super.close();
								}
								
								protected OutputStreamWriter getWriter() {
									if (writer == null)
										writer = new OutputStreamWriter(out);
									return writer;
								}

//								@Override
//								protected void processSetAttribute(int attribute)
//										throws IOException {
//									resetState();
//									getWriter().write("[strong]B");
//									getWriter().flush();
//									state = "strong";
//								}
//								
//								public void resetState() throws IOException {
//									if (state.equals("strong")) {
//										getWriter().write("[/strong]");
//										getWriter().flush();
//										state = "";
//									}
//								}
								
							};
							
							@Override
							public void write(int chr) throws IOException {
//								System.out.print(chr);
								if (chr == 0x0a) {
									try {
//										htmlOut.close();
										String msg = byteBuf.toString();
										LoggerFactory.getLogger(XmppShell.class).error("Send: {}", msg);
										chat.sendMessage(msg);
										byteBuf.reset();
									} catch (Exception e) {
										throw new IOException("Cannot send chat", e);
									}
								} else //if (!Character.isISOControl(chr))
								{
									htmlOut.write(chr);
									//sb.append((char)chr);
								}
							}
							
							@Override
							public void write(byte[] b) throws IOException {
								super.write(b);
							}
							
						};
						
//						OutputStream out = new DumbChatOutputStream(chat);
						OutputStream out = new HtmlChatOutputStream(chat);
						PrintStream chatOut = new PrintStream(out, true);
						CommandSession session = commandProcessor.createSession(System.in, chatOut, System.err);
						Object result;
						try {
							result = session.execute(msg.getBody());
							chatOut.flush(); // final flush
							if (result != null)
								chat.sendMessage(result.toString());
						} catch (Exception e) {
							try {
								chat.sendMessage("ERROR: " + e.getMessage());
							} catch (XMPPException e1) {
								e1.printStackTrace();
							}
						}
					}
				});
			}
		});
   }
	
	public void disconnect() {
		log.info("Disconnecting...");
//		System.out.println("Disconnecting...");
		if (conn != null && conn.isConnected()) {
			conn.disconnect();
			conn = null;
		}
		log.info("Disconnected.");
//		System.out.println("Disconnected.");
	}
	
	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public CommandProcessor getCommandProcessor() {
		return commandProcessor;
	}

	public void setCommandProcessor(CommandProcessor commandProcessor) {
		this.commandProcessor = commandProcessor;
	}

	public boolean isAutoConnect() {
		return autoConnect;
	}

	public void setAutoConnect(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}

}
