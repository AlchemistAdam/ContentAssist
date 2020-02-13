

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/**
 * Sample program to reproduce an issue where the Eclipse IDE content assist
 * stops working.
 * 
 * @author Adam Martinu
 */
public class ContentAssist extends JFrame {

	// component keys
	public static final String CK_COLUMN_HEADER = "columnHeader";
	public static final String CK_CONTENT_PANE = "contentPane";
	public static final String CK_CORNER_BUTTON = "cornerButton";
	public static final String CK_LIST = "list";
	public static final String CK_MENU_BAR = "menuBar";
	public static final String CK_M_FILE = "mFile";
	public static final String CK_M_HELP = "mHelp";
	public static final String CK_M_NAVIGATE = "mNavigate";
	public static final String CK_SCROLL_PANE = "scrollPane";

	private static final String STRING_TITLE = "Java System Properties";

	private static final long serialVersionUID = 1L;

	public static void main(final String[] args) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				final ContentAssist ca = new ContentAssist();
				ca.setVisible(true);
			});
		}
		catch (final InvocationTargetException e) {
			e.printStackTrace();
		}
		catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static GraphicsConfiguration defaultGC() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
	}

	protected final DefaultListModel<String> listModel = new DefaultListModel<>();
	protected final HashMap<String, JComponent> componentMap = new HashMap<>();

	public ContentAssist() {
		super(STRING_TITLE, defaultGC());
		initListModel();
		createGUI();
	}

	public JComponent getComponent(final String componentKey) {
		return componentMap.get(componentKey);
	}

	protected void createGUI() {

		/* content */

		final JPanel contentPane = new JPanel(new BorderLayout(0, 0), true);
		final JScrollPane scrollPane =
				new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JLabel columnHeader = new JLabel("A list of properties in System.getProperties()");
		final JButton cornerButton = new JButton("");
		final JList<String> list = new JList<>(listModel);

		list.setCellRenderer(new PropertyCellRenderer());
		list.setName(CK_LIST);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		componentMap.put(CK_LIST, list);

		columnHeader.setHorizontalAlignment(JLabel.CENTER);
		columnHeader.setName(CK_COLUMN_HEADER);
		componentMap.put(CK_COLUMN_HEADER, columnHeader);

		cornerButton.setName(CK_CORNER_BUTTON);
		cornerButton.setToolTipText("Resets the position of all scroll bars");
		cornerButton.addActionListener(event -> {
			final JScrollBar vsb = scrollPane.getVerticalScrollBar();
			vsb.setValue(Math.max(0, vsb.getMinimum()));

			final JScrollBar hsb = scrollPane.getHorizontalScrollBar();
			hsb.setValue(Math.max(0, hsb.getMinimum()));
		});
		componentMap.put(CK_CORNER_BUTTON, cornerButton);

		scrollPane.setColumnHeaderView(columnHeader);
		scrollPane.setCorner(ScrollPaneConstants.LOWER_TRAILING_CORNER, cornerButton);
		scrollPane.setName(CK_SCROLL_PANE);
		scrollPane.setViewportView(list);
		componentMap.put(CK_SCROLL_PANE, scrollPane);

		contentPane.setPreferredSize(new Dimension(400, 500));
		contentPane.setName(CK_CONTENT_PANE);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		componentMap.put(CK_CONTENT_PANE, contentPane);

		/* menus */

		final JMenuBar menuBar = new JMenuBar();
		final JMenu mFile = new JMenu("File");
		final JMenu mNavigate = new JMenu("Navigate");
		final JMenu mHelp = new JMenu("Help");

		mFile.setName(CK_M_FILE);
		mFile.setMnemonic(KeyEvent.VK_F);
		componentMap.put(CK_M_FILE, mFile);

		mNavigate.setName(CK_M_NAVIGATE);
		mNavigate.setMnemonic(KeyEvent.VK_N);
		componentMap.put(CK_M_NAVIGATE, mNavigate);

		mHelp.setName(CK_M_HELP);
		mHelp.setMnemonic(KeyEvent.VK_H);
		componentMap.put(CK_M_HELP, mHelp);

		menuBar.setName(CK_MENU_BAR);
		menuBar.add(mFile);
		menuBar.add(mNavigate);
		menuBar.add(mHelp);
		componentMap.put(CK_MENU_BAR, menuBar);

		/* frame */

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent event) {
				event.getWindow().dispose();
			}
		});
		setContentPane(contentPane);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setJMenuBar(menuBar);

		pack();
		setLocationRelativeTo(null);
	}

	protected void initListModel() {
		listModel.clear();
		final Properties p = System.getProperties();
		for (final Entry<Object, Object> entry : p.entrySet())
			listModel.addElement(entry.getKey() + " = " + entry.getValue());
	}

	public static class PropertyCellRenderer extends JPanel implements ListCellRenderer<String> {

		public static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255);
		public static final Color DEFAULT_FOREGROUND = new Color(0, 0, 0);
		public static final Color SELECTED_BACKGROUND = new Color(50, 50, 255);
		public static final Color SELECTED_FOREGROUND = new Color(255, 255, 255);

		private static final long serialVersionUID = 1L;

		protected final JLabel label = new JLabel();

		public PropertyCellRenderer() {
			setLayout(new BorderLayout(0, 0));
			add(Box.createHorizontalStrut(5), BorderLayout.WEST);
			add(label, BorderLayout.CENTER);
		}

		@Override
		public Component getListCellRendererComponent(final JList<? extends String> list, final String value,
				final int index, final boolean isSelected, final boolean cellHasFocus) {

			if (isSelected) {
				setBackground(SELECTED_BACKGROUND);
				label.setForeground(SELECTED_FOREGROUND);
			}
			else {
				setBackground(DEFAULT_BACKGROUND);
				label.setForeground(DEFAULT_FOREGROUND);
			}

			final FontMetrics fm = getFontMetrics(getFont());
			label.setText(value);
			label.setPreferredSize(new Dimension(fm.stringWidth(value), fm.getHeight() + 6));

			return this;
		}

	}
}
