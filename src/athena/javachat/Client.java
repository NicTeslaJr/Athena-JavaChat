package athena.javachat;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

class BackgroundThreadsHandler extends Thread {
	
	private Socket clientSocket;
	private BufferedReader inpStreamBR;
	private PrintStream outStreamPW;
	private static String serverIP;
	private static int serverPort;
	private static String nickName;
	
	BackgroundThreadsHandler(String sIP, int sP, String nickN) {
		serverIP = sIP;
		serverPort = sP;
		nickName = nickN;
	}
	
	public boolean backgroundHandlerInit() {
		
		try {
			Client.editParentFrameTitle("Connecting to Server...");
			clientSocket = new Socket(serverIP, serverPort);
			inpStreamBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			outStreamPW = new PrintStream(clientSocket.getOutputStream());
			sendMessage(nickName);
			Client.chatAreaAppendMessage(2, 10, "<Client>", "Connected to \"" + serverIP + ":" + serverPort + "\" as \"" + nickName + "\"");
			Client.editParentFrameTitle(nickName + "@" + serverIP + ":" + serverPort  + "  |  Athena - JavaChat");
			return true;
			// The Client is Now Connected to a Server.
		} catch(UnknownHostException uhe) {
			Client.editParentFrameTitle("ERROR  |  Athena - JavaChat");
			Client.chatAreaAppendMessage(3, 10, "<Client>", "ERROR Connecting to Server \"" + serverIP + ":" + serverPort + "\"\nReason: Host/IP Not Found (java.net.UnknownHostException)\nCheck Server Name/IP Address.");
			Client.connectServerDialog(103);
		} catch(ConnectException ce) {
			Client.editParentFrameTitle("ERROR  |  Athena - JavaChat");
			Client.chatAreaAppendMessage(3, 10, "<Client>", "ERROR Connecting to Server \"" + serverIP + ":" + serverPort + "\"\nReason: Connection Timed Out. (java.net.ConnectException)\nServer is Offline or Running at Different Port.");
			Client.connectServerDialog(103);
		} catch(IOException ioe) {
			// The below catch block might not be executed in general execution.
			ioe.printStackTrace();
			Client.editParentFrameTitle("Athena - JavaChat");
			Client.connectServerDialog(103);
		}
		return false;
		
	}
	
	// To send messages.
	public void sendMessage(String toSendMsg) {
		outStreamPW.println(toSendMsg);
	}
	
	// The below run() method will keep receiving messages from the Server.
	@Override
	public void run() {
		
		String receivedMsg = "";
		try {
			while((receivedMsg = inpStreamBR.readLine()) != null) {
				System.out.println(receivedMsg);
				String[] msg = receivedMsg.split(":", 4);
				for(int i = 0 ; i < msg.length ; i++) {
					System.out.println(msg[i]);
				}
				if(msg[0].charAt(0) == '1') {
					Client.chatAreaAppendMessage(1, Integer.parseInt(msg[1]), msg[2], msg[3]);
				} else if(msg[0].charAt(0) == '2') {
					Client.chatAreaAppendMessage(2, 10, msg[2], msg[3]);
				} else if(msg[0].charAt(0) == '3') {
					Client.chatAreaAppendMessage(3, 10, msg[2], msg[3]);
				} else if(msg[0].charAt(0) == '4') {
					outStreamPW.println("%ActivUsr");
					Client.activeUsersUpdate(inpStreamBR.readLine());
					Client.chatAreaAppendMessage(4, 10, msg[2], msg[3]);
				} else if(msg[0].charAt(0) == '5') {
					Client.chatAreaAppendMessage(5, 10, "<Server>", msg[2]);
				}
			}
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
		
	}
}

public class Client extends JFrame {

	private static final long serialVersionUID = -8904752173305641234L;
	private static String CLIENT_VERSION = "0.7B";
	// frame below holds the Parent Frame (JFame?) of the GUI.
	private static Client frame;
	private static JPanel contentPane;
	private static JTextPane chatAreaTextPane;
	private static JTextField messageTextField;
	private static JTextPane activeUsersTextPane;
	
	private static boolean showNotifications;
	private static SystemTray notificationTrayBase;
	private static TrayIcon notificationTray;
	private static BackgroundThreadsHandler bgHandler;
	
	// static variables for ActiveUsers functionality and ColorCoding NickNames.
	// totalConcurrentUsers should be kept the same as totalConcurrentUsers in the Server Program.
	private static int totalConcurrentUsers = 25;
	private static StyledDocument chatAreaDoc;
	private static SimpleAttributeSet fontItalics = new SimpleAttributeSet();
	static {
		StyleConstants.setItalic(fontItalics, true);
	}
	private static Color[] colorArray = {new Color(242, 5, 5), new Color(242, 226, 5), new Color(7, 140, 3), new Color(5, 17, 242), new Color(135, 4, 191), new Color(240, 120, 50), new Color(60, 190, 180), new Color(255, 0, 210), new Color(190, 255, 0), new Color(255, 255, 255)};
	private static SimpleAttributeSet[] colorsAttributeArray = new SimpleAttributeSet[totalConcurrentUsers];
	static {
		for(int i = 0 ; i < totalConcurrentUsers ; i++) {
			colorsAttributeArray[i] = new SimpleAttributeSet();
			StyleConstants.setForeground(colorsAttributeArray[i], colorArray[i % 10]);
		}
	}
	
	/* This Constructor creates the Main JFrame (GUI). */
	public Client() {
		
		// Colors
		Color almostBlack = new Color(31, 31, 31);
		Color darkGray = new Color(64, 64, 64);
		
		// Fonts
		Font monospaced13plain = new Font("Monospaced", Font.PLAIN, 13);
		
		// JFrame
		setIconImage(Toolkit.getDefaultToolkit().getImage(Client.class.getResource("/athena/javachat/images/clientLogo.png")));
		setTitle("Athena - JavaChat");
		setBounds(100, 100, 669, 420);
		// Use the setBounds below if using AdoptJDK JRE. GUI changes in that JRE.
//		setBounds(100, 100, 680, 430);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// contentPane
		contentPane = new JPanel();
		contentPane.setForeground(Color.WHITE);
		contentPane.setBackground(almostBlack);
		contentPane.setBorder(null);
		contentPane.setLayout(null);
		// Add contentPane to JFrame
		setContentPane(contentPane);
		
		// messageTextField
		messageTextField = new JTextField();
		messageTextField.setText("Enter Message Here...");
		messageTextField.setFont(monospaced13plain);
		messageTextField.setForeground(Color.WHITE);
		messageTextField.setBackground(darkGray);
		messageTextField.setBorder(UIManager.getBorder("Button.border"));
		messageTextField.setColumns(10);
		messageTextField.setCaretColor(Color.WHITE);
		messageTextField.setBounds(10, 345, 400, 35);
		messageTextField.addFocusListener(new FocusListener() {
			@Override
	        public void focusGained(FocusEvent e){
	        	messageTextField.setText("");
	        }
			@Override
	        public void focusLost(FocusEvent e) {
	        	messageTextField.setText("Enter Message Here...");
	        }
	    });
		messageTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				bgHandler.sendMessage(messageTextField.getText().trim());
				messageTextField.setText("");
			}
		});
		contentPane.add(messageTextField);
		
		// messageSendButton
		JButton messageSendButton = new JButton("Send");
		messageSendButton.setForeground(Color.WHITE);
		messageSendButton.setBackground(darkGray);
		messageSendButton.setFocusPainted(false);
		messageSendButton.setBorder(null);
		messageSendButton.setFont(monospaced13plain);
		messageSendButton.setBounds(420, 345, 90, 35);
		messageSendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				bgHandler.sendMessage(messageTextField.getText().trim());
				messageTextField.setText("");
			}
		});
		contentPane.add(messageSendButton);
		
		JSeparator chatAreaFillerTopSeparator = new JSeparator();
		chatAreaFillerTopSeparator.setOpaque(true);
		chatAreaFillerTopSeparator.setForeground(Color.DARK_GRAY);
		chatAreaFillerTopSeparator.setBackground(Color.DARK_GRAY);
		chatAreaFillerTopSeparator.setBounds(10, 11, 483, 3);
		contentPane.add(chatAreaFillerTopSeparator);
		
		JSeparator scrollBarFillerTopSeparator = new JSeparator();
		scrollBarFillerTopSeparator.setOpaque(true);
		scrollBarFillerTopSeparator.setForeground(Color.WHITE);
		scrollBarFillerTopSeparator.setBackground(Color.WHITE);
		scrollBarFillerTopSeparator.setBounds(493, 11, 17, 3);
		contentPane.add(scrollBarFillerTopSeparator);
		
		// scrollPane for chatAreaTextPane
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setBorder(null);
		scrollPane.setAutoscrolls(true);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(10, 14, 500, 317);
		contentPane.add(scrollPane);
		
		// chatAreaTextPane - can hold 61 Characters in Monospaced 13 Plain Font.
		chatAreaTextPane = new JTextPane();
		chatAreaTextPane.setMargin(new Insets(0, 3, 3, 3));
		chatAreaTextPane.setText(" ____________________Athena - JavaChat____________________\n");
		chatAreaTextPane.setEditable(false);
		chatAreaTextPane.setForeground(Color.WHITE);
		chatAreaTextPane.setBorder(UIManager.getBorder("Button.border"));
		chatAreaTextPane.setFont(monospaced13plain);
		chatAreaTextPane.setBackground(darkGray);
		scrollPane.setViewportView(chatAreaTextPane);
		
		// Initialize StyledDocument for chatAreaTextPane
		chatAreaDoc = chatAreaTextPane.getStyledDocument();
		
		JSeparator chatAreaFillerBottomSeparator = new JSeparator();
		chatAreaFillerBottomSeparator.setOpaque(true);
		chatAreaFillerBottomSeparator.setForeground(Color.DARK_GRAY);
		chatAreaFillerBottomSeparator.setBackground(Color.DARK_GRAY);
		chatAreaFillerBottomSeparator.setBounds(10, 331, 483, 3);
		contentPane.add(chatAreaFillerBottomSeparator);
		
		JSeparator scrollBarFillerBottomSeparator = new JSeparator();
		scrollBarFillerBottomSeparator.setForeground(Color.WHITE);
		scrollBarFillerBottomSeparator.setBackground(Color.WHITE);
		scrollBarFillerBottomSeparator.setOpaque(true);
		scrollBarFillerBottomSeparator.setBounds(493, 331, 17, 3);
		contentPane.add(scrollBarFillerBottomSeparator);
		
		// Separator Vertical
		JSeparator separatorVertical = new JSeparator();
		separatorVertical.setBorder(null);
		separatorVertical.setForeground(darkGray);
		separatorVertical.setBackground(almostBlack);
		separatorVertical.setOrientation(SwingConstants.VERTICAL);
		separatorVertical.setBounds(520, 11, 1, 369);
		contentPane.add(separatorVertical);
		
		// activeUsersLabel
		JLabel activeUsersLabel = new JLabel("Active Users:");
		activeUsersLabel.setForeground(Color.WHITE);
		activeUsersLabel.setBackground(darkGray);
		activeUsersLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		activeUsersLabel.setHorizontalAlignment(SwingConstants.CENTER);
		activeUsersLabel.setFont(monospaced13plain);
		activeUsersLabel.setBounds(531, 11, 122, 14);
		contentPane.add(activeUsersLabel);
		
		// activeUsersTextArea
		activeUsersTextPane = new JTextPane();
		activeUsersLabel.setLabelFor(activeUsersTextPane);
		activeUsersTextPane.setEditable(false);
		activeUsersTextPane.setBorder(UIManager.getBorder("Button.border"));
		activeUsersTextPane.setForeground(Color.WHITE);
		activeUsersTextPane.setBackground(darkGray);
		activeUsersTextPane.setFont(monospaced13plain);
		activeUsersTextPane.setBounds(531, 35, 122, 190);
		contentPane.add(activeUsersTextPane);
		
		// Separator Horizontal
		JSeparator separatorHorizontal = new JSeparator();
		separatorHorizontal.setForeground(darkGray);
		separatorHorizontal.setBackground(almostBlack);
		separatorHorizontal.setBounds(531, 237, 122, 1);
		contentPane.add(separatorHorizontal);
		
		// aboutTextPane - Has Italic font type and Light Gray font color.
		JTextPane aboutTextPane = new JTextPane();
		aboutTextPane.setMargin(new Insets(0, 3, 3, 3));
		aboutTextPane.setContentType("text/plain");
		aboutTextPane.setDisabledTextColor(Color.GRAY);
		aboutTextPane.setSelectionColor(Color.LIGHT_GRAY);
		aboutTextPane.setEnabled(false);
		aboutTextPane.setEditable(false);
		aboutTextPane.setForeground(Color.LIGHT_GRAY);
		aboutTextPane.setBackground(almostBlack);
		aboutTextPane.setFont(new Font("Monospaced", Font.ITALIC, 11));
		aboutTextPane.setBorder(UIManager.getBorder("Button.border"));
		aboutTextPane.setBounds(531, 249, 122, 132);
		aboutTextPane.setText("Made by Tesla\u26A1\nDriven by CRaZY\n(2019)\n\n\n\nClient Version:\n" + CLIENT_VERSION);
		// To make the above text center aligned.
		StyledDocument aboutStyleDoc = aboutTextPane.getStyledDocument();
		SimpleAttributeSet center = new SimpleAttributeSet();
		StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
		aboutStyleDoc.setParagraphAttributes(0, aboutStyleDoc.getLength(), center, false);
		contentPane.add(aboutTextPane);
		
		// For System Notifications using SystemTray.
		try {
			notificationTrayBase = SystemTray.getSystemTray();
			Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("/athena/javachat/images/notificationLogo.png"));
			notificationTray = new TrayIcon(image, "New Message");
			notificationTray.setImageAutoSize(true);
			notificationTray.setToolTip("New Message Icon Tip");
			notificationTrayBase.add(notificationTray);
		} catch(AWTException awte) {
			awte.printStackTrace();
		}
		
	}
	
	public static void sendNotification(String notificationTitle, String notificationMessage) {
		
		try {
			notificationTray.displayMessage(notificationTitle, notificationMessage, MessageType.NONE);
		} catch(Exception e) {
			e.printStackTrace();
		}	
		
	}
	
	public static void connectServerDialog(int eCode) {
		
		// connectServerDialogJPanel
		JPanel connectServerDialogJPanel = new JPanel();
		connectServerDialogJPanel.setFont(new Font("Monospaced", Font.PLAIN, 13));
		connectServerDialogJPanel.setBorder(null);
		connectServerDialogJPanel.setLayout(new GridLayout(4, 2, 2, 7));
		
		JLabel serverIPLabel = new JLabel("Server Name / IP Address: ", JLabel.RIGHT);
		JTextField serverIPTextField = new JTextField();
		serverIPLabel.setLabelFor(serverIPTextField);
		JLabel serverPortLabel = new JLabel("Server Port: ", JLabel.RIGHT);
		JTextField serverPortTextField = new JTextField();
		serverPortLabel.setLabelFor(serverPortTextField);
		JLabel nickNameLabel = new JLabel("Your Nickname: ", JLabel.RIGHT);
		JTextField nickNameTextField = new JTextField();
		nickNameLabel.setLabelFor(nickNameTextField);
		
		if(eCode == 100) {
			// Normal Message Display.
			connectServerDialogJPanel.add(new JLabel("Connect to JavaChat Relay"));
			connectServerDialogJPanel.add(new JLabel("Server:"));
		} else if(eCode == 101) {
			// NumberFormatException message Display due to non-numeric char in Port Field.
			connectServerDialogJPanel.add(new JLabel("ERROR: Server Port must be"));
			connectServerDialogJPanel.add(new JLabel("an Integer."));
		} else if(eCode == 102) {
			// All Fields Required message.
			connectServerDialogJPanel.add(new JLabel("All Fields are Required."));
			connectServerDialogJPanel.add(new JLabel());
		} else if(eCode == 103) {
			// Cannot Connect to Server message.
			connectServerDialogJPanel.add(new JLabel("ERROR: Cannot Connect to"));
			connectServerDialogJPanel.add(new JLabel("the specified Server."));
		}
		
		connectServerDialogJPanel.add(serverIPLabel);
		connectServerDialogJPanel.add(serverIPTextField);
		connectServerDialogJPanel.add(serverPortLabel);
		connectServerDialogJPanel.add(serverPortTextField);
		connectServerDialogJPanel.add(nickNameLabel);
		connectServerDialogJPanel.add(nickNameTextField);
		
		int okCheck = JOptionPane.showConfirmDialog(frame, connectServerDialogJPanel, "Connect to Server", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
		if(okCheck == JOptionPane.OK_OPTION) {
			String sIP = serverIPTextField.getText().trim();
			String sPort = serverPortTextField.getText().trim();
			String nickName = nickNameTextField.getText().trim();
			if(!sIP.equals("") && !sPort.equals("") && !nickName.equals("")) {
				try {
					bgHandler = new BackgroundThreadsHandler(sIP, Integer.parseInt(sPort), nickName);
					// The below return kills the recursion.
					return;
				} catch(NumberFormatException nfe) {
					connectServerDialog(101);
				}
			}
			else {
				// If Any Field is Empty.
				connectServerDialog(102);
			}
		}
		else {
			// If Dialog Window's Close button (X) is pressed, full program will be terminated.
			frame.dispose();
			System.exit(0);
		}
		
	}
	
	public static void editParentFrameTitle(String newTitle) {
		frame.setTitle(newTitle);
	}
	
	public static void chatAreaAppendMessage(int msgType, int colorCode, String userName, String newMsg) {
		
		// if possible make the StyledDocument static;
		try {
			if(msgType == 1) {
				chatAreaDoc.insertString(chatAreaDoc.getLength(), "\n" + userName, colorsAttributeArray[colorCode]);
				chatAreaDoc.insertString(chatAreaDoc.getLength(), ": " + newMsg, null);
			} else if(msgType == 2) {
				chatAreaDoc.insertString(chatAreaDoc.getLength(), "\n" + userName + ": " + newMsg, fontItalics);
			} else if(msgType == 3) {
				SimpleAttributeSet errMsgAttribute = new SimpleAttributeSet();
				StyleConstants.setForeground(errMsgAttribute, Color.RED);
				StyleConstants.setItalic(errMsgAttribute, true);
				chatAreaDoc.insertString(chatAreaDoc.getLength(), "\n" + newMsg, errMsgAttribute);
			} else if(msgType == 4) {
				chatAreaDoc.insertString(chatAreaDoc.getLength(), "\n" + userName + ": " + newMsg, null);
			} else if(msgType == 5) {
				chatAreaDoc.insertString(chatAreaDoc.getLength(), "\n" + newMsg, fontItalics);
			}
			chatAreaTextPane.setCaretPosition(chatAreaDoc.getLength());
			if(showNotifications) {
				sendNotification("New Message from: " + userName, (newMsg.length() < 25) ? newMsg : newMsg.substring(0, 20) + "...");
			}
		} catch(BadLocationException ble) {
			ble.printStackTrace();
		}
		
	}
	
	public static void activeUsersUpdate(String activeUsrMsg) {
		
		activeUsersTextPane.setText("");
		StyledDocument activeUsersDoc = activeUsersTextPane.getStyledDocument();
		String[] usrColorPair = activeUsrMsg.split(";");
		for(int i = 0 ; i < usrColorPair.length ; i++) {
			System.out.println(usrColorPair[i]);
			try {
				String[] usrAndColor = usrColorPair[i].split(":");
				activeUsersDoc.insertString(activeUsersDoc.getLength(), usrAndColor[1] + "\n", colorsAttributeArray[Integer.parseInt(usrAndColor[0])]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			}
		}
		
	}
	
	/* Launch the application. */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					frame = new Client();
					frame.setVisible(true);
					frame.addWindowFocusListener(new WindowAdapter() {
						@Override
					    public void windowGainedFocus(WindowEvent e) {
					        showNotifications = false;
					    }
						@Override
						public void windowLostFocus(WindowEvent e) {
							showNotifications = true;
						}
					});
					connectServerDialog(100);
					while(!bgHandler.backgroundHandlerInit()) {
						System.out.println("init() method.");
						continue;
					}
					bgHandler.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}