
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * Sample program to reproduce an issue where the Eclipse IDE content assist
 * stops working.
 * 
 * @author Adam Martinu
 */
@SuppressWarnings({ "serial", "javadoc" })
public class ContentAssist extends JFrame {

	// action keys
	@Key(KeyType.INTERNAL)
	public static final String AK_CLEAR_PROPERTY = "clearProperty";
	@Key(KeyType.INTERNAL)
	public static final String AK_NAV_GO_TO = "navGoTo";
	@Key(KeyType.INTERNAL)
	public static final String AK_NAV_NEXT = "navNext";
	@Key(KeyType.INTERNAL)
	public static final String AK_NAV_PREV = "navPrev";
	@Key(KeyType.INTERNAL)
	public static final String AK_NEW_PROPERTY = "newProperty";

	// component keys
	@Key(KeyType.INTERNAL)
	public static final String CK_COLUMN_HEADER = "columnHeader";
	@Key(KeyType.INTERNAL)
	public static final String CK_CONTENT_PANE = "contentPane";
	@Key(KeyType.INTERNAL)
	public static final String CK_CORNER_BUTTON = "cornerButton";
	@Key(KeyType.INTERNAL)
	public static final String CK_LIST = "list";
	@Key(KeyType.INTERNAL)
	public static final String CK_MENU_BAR = "menuBar";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_ABOUT = "mAbout";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_CLEAR_PROPERTY = "mClearProperty";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_EXIT = "mExit";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_FILE = "mFile";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_HELP = "mHelp";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_NAVIGATE = "mNavigate";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_NAV_NEXT = "mNavNext";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_NAV_GO_TO = "mNavGoTo";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_NAV_PREV = "mNavPrev";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_NEW_PROPERTY = "mNewProperty";
	@Key(KeyType.INTERNAL)
	public static final String CK_M_UPDATE = "mUpdate";
	@Key(KeyType.INTERNAL)
	public static final String CK_SCROLL_PANE = "scrollPane";
	// application strings
	private static final String STRING_TITLE = "Java System Properties";

	public static void main(final String[] args) {
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}
				catch (final Exception e) {
					System.out.println("could not set system look and feel: " + e.getMessage());
				}

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

	// helper method to get the default graphics configuration
	private static GraphicsConfiguration defaultGC() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
	}

	protected final DefaultListModel<String> listModel = new DefaultListModel<>();
	protected final HashMap<String, JComponent> componentMap = new HashMap<>();
	protected final HashMap<String, ActionListener> actionMap = new HashMap<>();

	public ContentAssist() {
		super(STRING_TITLE, defaultGC());
		setLocale(Locale.ROOT);
		// init model with system properties
		initListModel();
		// create user interface
		createGUI();
	}

	public void addAction(final String actionKey, final ActionListener action) throws NullPointerException {
		if (action == null)
			throw new NullPointerException("action must not be null");
		if (actionMap.containsKey(actionKey))
			throw new IllegalArgumentException("key already added to map");
		actionMap.put(actionKey, action);
	}

	public void addComponent(final String componentKey, final JComponent component) throws NullPointerException {
		if (component == null)
			throw new NullPointerException("component must not be null");
		if (componentMap.containsKey(componentKey))
			throw new IllegalArgumentException("key already added to map");
		componentMap.put(componentKey, component);
	}

	public String clearProperty() {
		// cannot modify an empty list
		if (listModel.isEmpty())
			return null;

		final String[] rv = new String[1];
		rv[0] = null;

		// combo box foreground colors
		final Color defaultColor;
		final Color errorColor = Color.RED;

		final JDialog dialog =
				new JDialog(this, "Clear Property", ModalityType.APPLICATION_MODAL, getGraphicsConfiguration());

		final JPanel dialogContentPane = new JPanel(null, true);
		final JLabel label = new JLabel("Property:");
		final JComboBox<Object> comboBox = new JComboBox<>(getPropertyKeys());
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0), true);
		final JButton buttonFinish = new JButton("Finish");
		final JButton buttonCancel = new JButton("Cancel");

		label.setAlignmentX(0.0F);
		label.setHorizontalAlignment(JLabel.HORIZONTAL);
		label.setLabelFor(comboBox);
		label.setName("label");

		comboBox.setAlignmentX(0.0F);
		comboBox.setEditable(false);
		comboBox.setName("comboBox");
		defaultColor = comboBox.getForeground();

		buttonFinish.setName("buttonFinish");
		buttonFinish.addActionListener(dialogEvent -> {
			final Object key = comboBox.getSelectedItem();
			assert key != null : "key is null";

			try {
				rv[0] = System.clearProperty(key.toString());
				comboBox.setForeground(defaultColor);
				for (int index = 0; index < listModel.size(); index++)
					if (listModel.get(index).startsWith(key.toString()))
						listModel.remove(index);
				dialog.dispose();
			}
			catch (final SecurityException e) {
				e.printStackTrace();
				comboBox.setForeground(errorColor);
				getToolkit().beep();
				JOptionPane.showMessageDialog(dialog,
						"The installed security manager does not allow access to the specified property.", "Error!",
						JOptionPane.ERROR_MESSAGE);
				comboBox.requestFocusInWindow();
			}
		});

		buttonCancel.setName("buttonCancel");
		buttonCancel.addActionListener(dialogEvent -> { dialog.dispose(); });

		buttonPanel.setAlignmentX(0.0F);
		buttonPanel.setName("buttonPanel");
		buttonPanel.add(buttonFinish);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(buttonCancel);

		dialogContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		dialogContentPane.setLayout(new BoxLayout(dialogContentPane, BoxLayout.Y_AXIS));
		dialogContentPane.setName("dialogContentPane");
		dialogContentPane.add(label);
		dialogContentPane.add(Box.createVerticalStrut(2));
		dialogContentPane.add(comboBox);
		dialogContentPane.add(Box.createVerticalStrut(5));
		dialogContentPane.add(new JSeparator(JSeparator.HORIZONTAL));
		dialogContentPane.add(Box.createVerticalStrut(5));
		dialogContentPane.add(buttonPanel);

		dialog.setContentPane(dialogContentPane);
		dialog.setName("ClearPropertyDialog");
		dialog.setResizable(false);
		dialog.addWindowFocusListener(new WindowAdapter() {

			@Override
			public void windowGainedFocus(final WindowEvent dialogEvent) {
				comboBox.requestFocusInWindow();
			}
		});

		dialog.pack();
		dialog.setLocationRelativeTo(this);

		dialog.setVisible(true);
		return rv[0];
	}

	public String clearProperty(final int index) throws IndexOutOfBoundsException {
		// cannot modify an empty list
		if (listModel.isEmpty())
			return null;

		if (index < 0 || index >= listModel.size())
			throw new IndexOutOfBoundsException();

		final String property = listModel.get(index);
		final String key = property.substring(0, property.indexOf(" = "));

		try {
			final String rv = System.clearProperty(key);
			listModel.remove(index);
			return rv;
		}
		catch (final SecurityException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"The installed security manager does not allow access to the specified property.", "Error!",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	public String clearProperty(final Object key) {
		// cannot modify an empty list
		if (listModel.isEmpty())
			return null;

		try {
			final String rv = System.clearProperty(key.toString());
			for (int index = 0; index < listModel.size(); index++)
				if (listModel.get(index).startsWith(key.toString()))
					listModel.remove(index);
			return rv;
		}
		catch (final SecurityException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"The installed security manager does not allow access to the specified property.", "Error!",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}

	public ActionListener getAction(final String actionKey) {
		return actionMap.get(actionKey);
	}

	public JComponent getComponent(final String componentKey) {
		return componentMap.get(componentKey);
	}

	public int getPropertyCount() {
		return listModel.size();
	}

	public void goToProperty(final int index) throws IndexOutOfBoundsException {
		// cannot navigate an empty list
		if (listModel.isEmpty())
			return;

		if (index < 0 || index >= listModel.size())
			throw new IndexOutOfBoundsException();

		@SuppressWarnings("unchecked")
		final JList<String> list = (JList<String>) getComponent(CK_LIST);
		list.setSelectedIndex(index);
	}

	public void navigateNext() {
		// cannot navigate an empty list
		if (listModel.isEmpty())
			return;

		@SuppressWarnings("unchecked")
		final JList<String> list = (JList<String>) getComponent(CK_LIST);
		final ListSelectionModel model = list.getSelectionModel();
		int index = model.getMinSelectionIndex();
		if (index == -1)
			index = 0;
		else {
			index++;
			if (index >= listModel.size())
				index = 0;
		}

		model.setSelectionInterval(index, index);
	}

	public void navigatePrevious() {
		// cannot navigate an empty list
		if (listModel.isEmpty())
			return;

		@SuppressWarnings("unchecked")
		final JList<String> list = (JList<String>) getComponent(CK_LIST);
		final ListSelectionModel model = list.getSelectionModel();
		int index = model.getMinSelectionIndex();
		if (index == -1)
			index = listModel.size() - 1;
		else {
			index--;
			if (index < 0)
				index = listModel.size() - 1;
		}

		model.setSelectionInterval(index, index);
	}

	public void newProperty() {
		// text field background colors
		final Color defaultColor;
		final Color errorColor = new Color(255, 100, 100);

		final JDialog dialog =
				new JDialog(this, "New Property", ModalityType.APPLICATION_MODAL, getGraphicsConfiguration());

		final JPanel dialogContentPane = new JPanel(null, true);
		final JLabel labelKey = new JLabel("Key:");
		final JTextField textFieldKey = new JTextField(new PlainDocument(), "", 25);
		final JLabel labelValue = new JLabel("Value:");
		final JTextField textFieldValue = new JTextField(new PlainDocument(), "", 25);
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0), true);
		final JButton buttonFinish = new JButton("Finish");
		final JButton buttonCancel = new JButton("Cancel");

		labelKey.setAlignmentX(0.0F);
		labelKey.setHorizontalAlignment(JLabel.HORIZONTAL);
		labelKey.setLabelFor(textFieldKey);
		labelKey.setName("labelKey");

		textFieldKey.setAlignmentX(0.0F);
		textFieldKey.setHorizontalAlignment(JTextField.LEFT);
		textFieldKey.setName("textFieldKey");
		defaultColor = textFieldKey.getBackground();

		labelValue.setAlignmentX(0.0F);
		labelValue.setHorizontalAlignment(JLabel.HORIZONTAL);
		labelValue.setLabelFor(textFieldValue);
		labelValue.setName("labelValue");

		textFieldValue.setAlignmentX(0.0F);
		textFieldValue.setHorizontalAlignment(JTextField.LEFT);
		textFieldValue.setName("textFieldValue");

		buttonFinish.setName("buttonFinish");
		buttonFinish.addActionListener(dialogEvent -> {
			final String key = textFieldKey.getText();
			final String value = textFieldValue.getText();

			try {
				System.setProperty(key, value);
				textFieldKey.setBackground(defaultColor);
				listModel.addElement(key + " = " + value);
				dialog.dispose();
				@SuppressWarnings("unchecked")
				final JList<String> list = (JList<String>) getComponent(CK_LIST);
				list.setSelectedIndex(listModel.size() - 1);
			}
			catch (final IllegalArgumentException e) {
				e.printStackTrace();
				textFieldKey.setBackground(errorColor);
				getToolkit().beep();
				JOptionPane.showMessageDialog(dialog, "Property key is empty", "Error!", JOptionPane.ERROR_MESSAGE);
				textFieldKey.select(0, textFieldKey.getText().length());
				textFieldKey.requestFocusInWindow();
			}
			catch (final SecurityException e) {
				e.printStackTrace();
				getToolkit().beep();
				JOptionPane.showMessageDialog(dialog,
						"The installed security manager does not allow setting of the specified property.", "Error!",
						JOptionPane.ERROR_MESSAGE);
				textFieldKey.select(0, textFieldKey.getText().length());
				textFieldKey.requestFocusInWindow();
			}

		});

		buttonCancel.setName("buttonCancel");
		buttonCancel.addActionListener(dialogEvent -> { dialog.dispose(); });

		buttonPanel.setAlignmentX(0.0F);
		buttonPanel.setName("buttonPanel");
		buttonPanel.add(buttonFinish);
		buttonPanel.add(Box.createHorizontalStrut(5));
		buttonPanel.add(buttonCancel);

		dialogContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		dialogContentPane.setLayout(new BoxLayout(dialogContentPane, BoxLayout.Y_AXIS));
		dialogContentPane.setName("dialogContentPane");
		dialogContentPane.add(labelKey);
		dialogContentPane.add(Box.createVerticalStrut(2));
		dialogContentPane.add(textFieldKey);
		dialogContentPane.add(Box.createVerticalStrut(5));
		dialogContentPane.add(labelValue);
		dialogContentPane.add(Box.createVerticalStrut(2));
		dialogContentPane.add(textFieldValue);
		dialogContentPane.add(Box.createVerticalStrut(5));
		dialogContentPane.add(new JSeparator(JSeparator.HORIZONTAL));
		dialogContentPane.add(Box.createVerticalStrut(5));
		dialogContentPane.add(buttonPanel);

		dialog.setContentPane(dialogContentPane);
		dialog.setName("NewPropertyDialog");
		dialog.setResizable(false);
		dialog.addWindowFocusListener(new WindowAdapter() {

			@Override
			public void windowGainedFocus(final WindowEvent dialogEvent) {
				textFieldKey.requestFocusInWindow();
			}
		});

		dialog.pack();
		dialog.setLocationRelativeTo(this);

		dialog.setVisible(true);
	}

	public void newProperty(final String key, final String value)
			throws IllegalArgumentException, NullPointerException {
		if (key == null)
			throw new NullPointerException("key must not be null");
		if (value == null)
			throw new NullPointerException("value must not be null");
		if (key.isEmpty())
			throw new IllegalArgumentException("key is empty");

		try {
			System.setProperty(key, value);
			listModel.addElement(key + " = " + value);
			@SuppressWarnings("unchecked")
			final JList<String> list = (JList<String>) getComponent(CK_LIST);
			list.setSelectedIndex(listModel.size() - 1);
		}
		catch (final SecurityException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					"The installed security manager does not allow setting of the specified property.", "Error!",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public ActionListener removeAction(final String actionKey) throws IllegalArgumentException {
		if (!actionMap.containsKey(actionKey))
			return null;

		for (final Field field : this.getClass().getDeclaredFields()) {
			final Key ck = field.getAnnotation(Key.class);
			if (ck != null && ck.value() != null && ck.value().equals(KeyType.INTERNAL))
				throw new IllegalArgumentException("internal component key");
		}

		return actionMap.remove(actionKey);
	}

	public JComponent removeComponent(final String componentKey) throws IllegalArgumentException {
		if (!componentMap.containsKey(componentKey))
			return null;

		for (final Field field : this.getClass().getDeclaredFields()) {
			final Key ck = field.getAnnotation(Key.class);
			if (ck != null && ck.value() != null && ck.value().equals(KeyType.INTERNAL))
				throw new IllegalArgumentException("internal component key");
		}

		return componentMap.remove(componentKey);
	}

	protected void createGUI() {

		/* ********** */
		/* COMPONENTS */
		/* ********** */

		final JPanel contentPane = new JPanel(new BorderLayout(0, 0), true);
		final JScrollPane scrollPane =
				new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JLabel columnHeader = new JLabel("A list of properties in System.getProperties()");
		final JButton cornerButton = new JButton("");
		final JList<String> list = new JList<>(listModel);

		final JMenuBar menuBar = new JMenuBar();

		final JMenu mFile = new JMenu("File");
		final JMenuItem mNewProperty = new JMenuItem("New Property...", KeyEvent.VK_N);
		final JMenuItem mClearProperty = new JMenuItem("Clear Property...", KeyEvent.VK_C);
		final JMenuItem mUpdate = new JMenuItem("Update List", KeyEvent.VK_U);
		final JMenuItem mExit = new JMenuItem("Exit", KeyEvent.VK_E);

		final JMenu mNavigate = new JMenu("Navigate");
		final JMenuItem mNavNext = new JMenuItem("Next Property", KeyEvent.VK_N);
		final JMenuItem mNavPrev = new JMenuItem("Previous Property", KeyEvent.VK_P);
		final JMenuItem mNavGoTo = new JMenuItem("Go to Property...", KeyEvent.VK_G);

		final JMenu mHelp = new JMenu("Help");
		final JMenuItem mAbout = new JMenuItem("About", KeyEvent.VK_A);

		/* ******* */
		/* ACTIONS */
		/* ******* */

		final ActionListener actionClearProperty = event -> {
			// cannot modify an empty list
			if (listModel.isEmpty())
				return;

			// combo box foreground colors
			final Color defaultColor;
			final Color errorColor = Color.RED;

			final JDialog dialog =
					new JDialog(this, "Clear Property", ModalityType.APPLICATION_MODAL, getGraphicsConfiguration());

			final JPanel dialogContentPane = new JPanel(null, true);
			final JLabel label = new JLabel("Property:");
			final JComboBox<Object> comboBox = new JComboBox<>(getPropertyKeys());
			final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0), true);
			final JButton buttonFinish = new JButton("Finish");
			final JButton buttonCancel = new JButton("Cancel");

			label.setAlignmentX(0.0F);
			label.setHorizontalAlignment(JLabel.HORIZONTAL);
			label.setLabelFor(comboBox);
			label.setName("label");

			comboBox.setAlignmentX(0.0F);
			comboBox.setEditable(false);
			comboBox.setName("comboBox");
			defaultColor = comboBox.getForeground();

			buttonFinish.setName("buttonFinish");
			buttonFinish.addActionListener(dialogEvent -> {
				final Object key = comboBox.getSelectedItem();
				assert key != null : "key is null";

				try {
					System.clearProperty(key.toString());
					comboBox.setForeground(defaultColor);
					for (int index = 0; index < listModel.size(); index++)
						if (listModel.get(index).startsWith(key.toString()))
							listModel.remove(index);
					dialog.dispose();
				}
				catch (final SecurityException e) {
					e.printStackTrace();
					comboBox.setForeground(errorColor);
					getToolkit().beep();
					JOptionPane.showMessageDialog(dialog,
							"The installed security manager does not allow access to the specified property.", "Error!",
							JOptionPane.ERROR_MESSAGE);
					comboBox.requestFocusInWindow();
				}
			});

			buttonCancel.setName("buttonCancel");
			buttonCancel.addActionListener(dialogEvent -> { dialog.dispose(); });

			buttonPanel.setAlignmentX(0.0F);
			buttonPanel.setName("buttonPanel");
			buttonPanel.add(buttonFinish);
			buttonPanel.add(Box.createHorizontalStrut(5));
			buttonPanel.add(buttonCancel);

			dialogContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			dialogContentPane.setLayout(new BoxLayout(dialogContentPane, BoxLayout.Y_AXIS));
			dialogContentPane.setName("dialogContentPane");
			dialogContentPane.add(label);
			dialogContentPane.add(Box.createVerticalStrut(2));
			dialogContentPane.add(comboBox);
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(new JSeparator(JSeparator.HORIZONTAL));
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(buttonPanel);

			dialog.setContentPane(dialogContentPane);
			dialog.setName("ClearPropertyDialog");
			dialog.setResizable(false);
			dialog.addWindowFocusListener(new WindowAdapter() {

				@Override
				public void windowGainedFocus(final WindowEvent dialogEvent) {
					comboBox.requestFocusInWindow();
				}
			});

			dialog.pack();
			dialog.setLocationRelativeTo(this);

			dialog.setVisible(true);
		};

		final ActionListener actionNavNext = event -> {
			// cannot navigate an empty list
			if (listModel.isEmpty())
				return;

			final ListSelectionModel model = list.getSelectionModel();
			int index = model.getMinSelectionIndex();
			if (index == -1)
				index = 0;
			else {
				index++;
				if (index >= listModel.size())
					index = 0;
			}

			model.setSelectionInterval(index, index);
		};

		final ActionListener actionNavPrev = event -> {
			// cannot navigate an empty list
			if (listModel.isEmpty())
				return;

			final ListSelectionModel model = list.getSelectionModel();
			int index = model.getMinSelectionIndex();
			if (index == -1)
				index = listModel.size() - 1;
			else {
				index--;
				if (index < 0)
					index = listModel.size() - 1;
			}

			model.setSelectionInterval(index, index);
		};

		final ActionListener actionNavGoTo = event -> {
			// cannot navigate an empty list
			if (listModel.isEmpty())
				return;

			// text field background colors
			final Color defaultColor;
			final Color errorColor = new Color(255, 100, 100);

			final JDialog dialog =
					new JDialog(this, "Go to Property", ModalityType.APPLICATION_MODAL, getGraphicsConfiguration());

			final JPanel dialogContentPane = new JPanel(null, true);
			final JLabel label = new JLabel("Enter property number (0.." + (listModel.size() - 1) + "):");
			final JTextField textField = new JTextField(new PlainDocument(), "", 25);
			final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0), true);
			final JButton buttonGo = new JButton("Go to property");
			final JButton buttonCancel = new JButton("Cancel");

			label.setAlignmentX(0.0F);
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setLabelFor(textField);
			label.setName("label");

			textField.setAlignmentX(0.0F);
			textField.setHorizontalAlignment(JTextField.LEFT);
			textField.setName("textField");
			((PlainDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {

				@Override
				public void insertString(final FilterBypass fb, final int offset, final String string,
						final AttributeSet attr) throws BadLocationException {
					// only allow insertion of strings with digits
					for (final char c : string.toCharArray())
						if (!Character.isDigit(c))
							return;

					// insert string
					super.insertString(fb, offset, string, attr);
				}

				@Override
				public void replace(final FilterBypass fb, final int offset, final int length, final String text,
						final AttributeSet attrs) throws BadLocationException {
					// only allow replacing with strings with digits
					for (final char c : text.toCharArray())
						if (!Character.isDigit(c))
							return;

					// replace string
					super.replace(fb, offset, length, text, attrs);
				}
			});
			if (list.getSelectedIndex() != -1)
				textField.setText(String.valueOf(list.getSelectedIndex()));
			defaultColor = textField.getBackground();

			buttonGo.setName("buttonGo");
			buttonGo.addActionListener(dialogEvent -> {
				// unparsed text
				final String text = textField.getText();

				// parsed value
				int value;
				try {
					value = Integer.parseInt(text);
				}
				catch (final NumberFormatException e) {
					value = -1;
					e.printStackTrace();
				}

				// value is legal - dispose dialog and go to property
				if (value != -1 && value < listModel.size()) {
					textField.setBackground(defaultColor);
					dialog.dispose();
					list.setSelectedIndex(value);
				}
				// error - notify user
				else {
					textField.setBackground(errorColor);
					getToolkit().beep();
				}
			});

			buttonCancel.setName("buttonCancel");
			buttonCancel.addActionListener(dialogEvent -> { dialog.dispose(); });

			buttonPanel.setAlignmentX(0.0F);
			buttonPanel.setName("buttonPanel");
			buttonPanel.add(buttonGo);
			buttonPanel.add(Box.createHorizontalStrut(5));
			buttonPanel.add(buttonCancel);

			dialogContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			dialogContentPane.setLayout(new BoxLayout(dialogContentPane, BoxLayout.Y_AXIS));
			dialogContentPane.setName("dialogContentPane");
			dialogContentPane.add(label);
			dialogContentPane.add(Box.createVerticalStrut(2));
			dialogContentPane.add(textField);
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(new JSeparator(JSeparator.HORIZONTAL));
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(buttonPanel);

			dialog.setContentPane(dialogContentPane);
			dialog.setName("GoToPropertyDialog");
			dialog.setResizable(false);
			dialog.addWindowFocusListener(new WindowAdapter() {

				@Override
				public void windowGainedFocus(final WindowEvent dialogEvent) {
					textField.requestFocusInWindow();
				}
			});

			dialog.pack();
			dialog.setLocationRelativeTo(this);

			dialog.setVisible(true);
		};

		final ActionListener actionNewProperty = event -> {
			// text field background colors
			final Color defaultColor;
			final Color errorColor = new Color(255, 100, 100);

			final JDialog dialog =
					new JDialog(this, "New Property", ModalityType.APPLICATION_MODAL, getGraphicsConfiguration());

			final JPanel dialogContentPane = new JPanel(null, true);
			final JLabel labelKey = new JLabel("Key:");
			final JTextField textFieldKey = new JTextField(new PlainDocument(), "", 25);
			final JLabel labelValue = new JLabel("Value:");
			final JTextField textFieldValue = new JTextField(new PlainDocument(), "", 25);
			final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0), true);
			final JButton buttonFinish = new JButton("Finish");
			final JButton buttonCancel = new JButton("Cancel");

			labelKey.setAlignmentX(0.0F);
			labelKey.setHorizontalAlignment(JLabel.HORIZONTAL);
			labelKey.setLabelFor(textFieldKey);
			labelKey.setName("labelKey");

			textFieldKey.setAlignmentX(0.0F);
			textFieldKey.setHorizontalAlignment(JTextField.LEFT);
			textFieldKey.setName("textFieldKey");
			defaultColor = textFieldKey.getBackground();

			labelValue.setAlignmentX(0.0F);
			labelValue.setHorizontalAlignment(JLabel.HORIZONTAL);
			labelValue.setLabelFor(textFieldValue);
			labelValue.setName("labelValue");

			textFieldValue.setAlignmentX(0.0F);
			textFieldValue.setHorizontalAlignment(JTextField.LEFT);
			textFieldValue.setName("textFieldValue");

			buttonFinish.setName("buttonFinish");
			buttonFinish.addActionListener(dialogEvent -> {
				final String key = textFieldKey.getText();
				final String value = textFieldValue.getText();

				try {
					System.setProperty(key, value);
					textFieldKey.setBackground(defaultColor);
					listModel.addElement(key + " = " + value);
					dialog.dispose();
					list.setSelectedIndex(listModel.size() - 1);
				}
				catch (final IllegalArgumentException e) {
					e.printStackTrace();
					textFieldKey.setBackground(errorColor);
					getToolkit().beep();
					JOptionPane.showMessageDialog(dialog, "Property key is empty", "Error!", JOptionPane.ERROR_MESSAGE);
					textFieldKey.select(0, textFieldKey.getText().length());
					textFieldKey.requestFocusInWindow();
				}
				catch (final SecurityException e) {
					e.printStackTrace();
					getToolkit().beep();
					JOptionPane.showMessageDialog(dialog,
							"The installed security manager does not allow setting of the specified property.",
							"Error!", JOptionPane.ERROR_MESSAGE);
					textFieldKey.select(0, textFieldKey.getText().length());
					textFieldKey.requestFocusInWindow();
				}

			});

			buttonCancel.setName("buttonCancel");
			buttonCancel.addActionListener(dialogEvent -> { dialog.dispose(); });

			buttonPanel.setAlignmentX(0.0F);
			buttonPanel.setName("buttonPanel");
			buttonPanel.add(buttonFinish);
			buttonPanel.add(Box.createHorizontalStrut(5));
			buttonPanel.add(buttonCancel);

			dialogContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			dialogContentPane.setLayout(new BoxLayout(dialogContentPane, BoxLayout.Y_AXIS));
			dialogContentPane.setName("dialogContentPane");
			dialogContentPane.add(labelKey);
			dialogContentPane.add(Box.createVerticalStrut(2));
			dialogContentPane.add(textFieldKey);
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(labelValue);
			dialogContentPane.add(Box.createVerticalStrut(2));
			dialogContentPane.add(textFieldValue);
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(new JSeparator(JSeparator.HORIZONTAL));
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(buttonPanel);

			dialog.setContentPane(dialogContentPane);
			dialog.setName("NewPropertyDialog");
			dialog.setResizable(false);
			dialog.addWindowFocusListener(new WindowAdapter() {

				@Override
				public void windowGainedFocus(final WindowEvent dialogEvent) {
					textFieldKey.requestFocusInWindow();
				}
			});

			dialog.pack();
			dialog.setLocationRelativeTo(this);

			dialog.setVisible(true);
		};

		actionMap.put(AK_CLEAR_PROPERTY, actionClearProperty);
		actionMap.put(AK_NAV_GO_TO, actionNavGoTo);
		actionMap.put(AK_NAV_NEXT, actionNavNext);
		actionMap.put(AK_NAV_PREV, actionNavPrev);
		actionMap.put(AK_NEW_PROPERTY, actionNewProperty);

		/* ******* */
		/* CONTENT */
		/* ******* */

		list.setCellRenderer(new PropertyCellRenderer());
		list.setName(CK_LIST);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(event -> {
			// changes are still being made - ignore event
			if (event.getValueIsAdjusting())
				return;

			if (!listModel.isEmpty())
				list.scrollRectToVisible(list.getCellBounds(list.getSelectedIndex(), list.getSelectedIndex()));
		});
		componentMap.put(CK_LIST, list);

		columnHeader.setHorizontalAlignment(JLabel.CENTER);
		columnHeader.setName(CK_COLUMN_HEADER);
		componentMap.put(CK_COLUMN_HEADER, columnHeader);

		cornerButton.setName(CK_CORNER_BUTTON);
		cornerButton.setToolTipText("Reset the position of all scroll bars");
		cornerButton.addActionListener(event -> {
			final JScrollBar vsb = scrollPane.getVerticalScrollBar();
			if (vsb != null)
				vsb.setValue(Math.max(0, vsb.getMinimum()));

			final JScrollBar hsb = scrollPane.getHorizontalScrollBar();
			if (hsb != null)
				hsb.setValue(Math.max(0, hsb.getMinimum()));
		});
		componentMap.put(CK_CORNER_BUTTON, cornerButton);

		scrollPane.setColumnHeaderView(columnHeader);
		scrollPane.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		scrollPane.setCorner(ScrollPaneConstants.LOWER_TRAILING_CORNER, cornerButton);
		scrollPane.setName(CK_SCROLL_PANE);
		scrollPane.setViewportView(list);
		componentMap.put(CK_SCROLL_PANE, scrollPane);

		contentPane.setPreferredSize(new Dimension(400, 500));
		contentPane.setName(CK_CONTENT_PANE);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		componentMap.put(CK_CONTENT_PANE, contentPane);

		/* ***** */
		/* MENUS */
		/* ***** */

		mNewProperty.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
		mNewProperty.setName(CK_M_NEW_PROPERTY);
		mNewProperty.addActionListener(actionNewProperty);
		componentMap.put(CK_M_NEW_PROPERTY, mNewProperty);

		mClearProperty.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		mClearProperty.setName(CK_M_CLEAR_PROPERTY);
		mClearProperty.addActionListener(actionClearProperty);
		componentMap.put(CK_M_CLEAR_PROPERTY, mClearProperty);

		mUpdate.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));
		mUpdate.setName(CK_M_UPDATE);
		mUpdate.addActionListener(event -> {
			initListModel();
			if (!listModel.isEmpty())
				list.scrollRectToVisible(list.getCellBounds(0, 0));
		});
		componentMap.put(CK_M_UPDATE, mUpdate);

		mExit.setName(CK_M_EXIT);
		mExit.addActionListener(event -> { dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)); });
		componentMap.put(CK_M_EXIT, mExit);

		mFile.setMnemonic(KeyEvent.VK_F);
		mFile.setName(CK_M_FILE);
		mFile.add(mNewProperty);
		mFile.add(mClearProperty);
		mFile.add(mUpdate);
		mFile.addSeparator();
		mFile.add(mExit);
		componentMap.put(CK_M_FILE, mFile);

		mNavNext.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, InputEvent.CTRL_DOWN_MASK));
		mNavNext.setName(CK_M_NAV_NEXT);
		mNavNext.setToolTipText("Select the next property in the list");
		mNavNext.addActionListener(actionNavNext);
		componentMap.put(CK_M_NAV_NEXT, mNavNext);

		mNavPrev.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.CTRL_DOWN_MASK));
		mNavPrev.setName(CK_M_NAV_PREV);
		mNavPrev.setToolTipText("Select the previous property in the list");
		mNavPrev.addActionListener(actionNavPrev);
		componentMap.put(CK_M_NAV_PREV, mNavPrev);

		mNavGoTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
		mNavGoTo.setName(CK_M_NAV_GO_TO);
		mNavGoTo.setToolTipText("Go to the property at the specified index");
		mNavGoTo.addActionListener(actionNavGoTo);
		componentMap.put(CK_M_NAV_GO_TO, mNavGoTo);

		mNavigate.setMnemonic(KeyEvent.VK_N);
		mNavigate.setName(CK_M_NAVIGATE);
		mNavigate.add(mNavNext);
		mNavigate.add(mNavPrev);
		mNavigate.addSeparator();
		mNavigate.add(mNavGoTo);
		componentMap.put(CK_M_NAVIGATE, mNavigate);

		mAbout.setName(CK_M_ABOUT);
		mAbout.addActionListener(event -> {
			final JDialog dialog =
					new JDialog(this, "About", ModalityType.APPLICATION_MODAL, getGraphicsConfiguration());

			final JPanel dialogContentPane = new JPanel(null, true);
			final JTextArea textArea = new JTextArea(new PlainDocument(),
					"Sample application to reproduce an issue with content assist in the Eclipse IDE. "
							+ "When the source file for this application is viewed in the Eclipse IDE, "
							+ "content assist is not working in certain parts of the file.\n\n"
							+ "Author: Adam Martinu",
					9, 30);
			final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0), true);
			final JButton buttonClose = new JButton("Close");

			textArea.setAlignmentX(0.0F);
			textArea.setEditable(false);
			textArea.setFont(Font.decode("Arial PLAIN 11"));
			textArea.setLineWrap(true);
			textArea.setOpaque(false);
			textArea.setWrapStyleWord(true);

			buttonClose.setName("buttonClose");
			buttonClose.addActionListener(dialogEvent -> { dialog.dispose(); });

			buttonPanel.setAlignmentX(0.0F);
			buttonPanel.setName("buttonPanel");
			buttonPanel.add(buttonClose);

			dialogContentPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			dialogContentPane.setLayout(new BoxLayout(dialogContentPane, BoxLayout.Y_AXIS));
			dialogContentPane.setName("dialogContentPane");
			dialogContentPane.add(textArea);
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(new JSeparator(JSeparator.HORIZONTAL));
			dialogContentPane.add(Box.createVerticalStrut(5));
			dialogContentPane.add(buttonPanel);

			dialog.setContentPane(dialogContentPane);
			dialog.setName("AboutDialog");
			dialog.setResizable(false);
			dialog.addWindowFocusListener(new WindowAdapter() {

				@Override
				public void windowGainedFocus(final WindowEvent dialogEvent) {
					buttonClose.requestFocusInWindow();
				}
			});

			dialog.pack();
			dialog.setLocationRelativeTo(this);

			dialog.setVisible(true);
		});

		mHelp.setMnemonic(KeyEvent.VK_H);
		mHelp.setName(CK_M_HELP);
		mHelp.add(mAbout);
		componentMap.put(CK_M_HELP, mHelp);

		menuBar.setName(CK_MENU_BAR);
		menuBar.add(mFile);
		menuBar.add(mNavigate);
		menuBar.add(mHelp);
		componentMap.put(CK_MENU_BAR, menuBar);

		/* ***** */
		/* FRAME */
		/* ***** */

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

	protected Object[] getPropertyKeys() {
		final Set<Object> keySet = System.getProperties().keySet();
		return keySet.toArray(new Object[keySet.size()]);
	}

	protected void initListModel() {
		listModel.clear();
		final Properties p = System.getProperties();
		for (final Entry<Object, Object> entry : p.entrySet())
			listModel.addElement(entry.getKey() + " = " + entry.getValue());
	}

	public static class PropertyCellRenderer extends JLabel implements ListCellRenderer<String> {

		public static final Color DEFAULT_BACKGROUND = new Color(255, 255, 255);
		public static final Color DEFAULT_FOREGROUND = new Color(0, 0, 0);
		public static final Color SELECTED_BACKGROUND = new Color(50, 50, 255);
		public static final Color SELECTED_FOREGROUND = new Color(255, 255, 255);

		public PropertyCellRenderer() {
			setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(final JList<? extends String> list, final String value,
				final int index, final boolean isSelected, final boolean cellHasFocus) {
			if (isSelected) {
				setBackground(SELECTED_BACKGROUND);
				setForeground(SELECTED_FOREGROUND);
			}
			else {
				setBackground(DEFAULT_BACKGROUND);
				setForeground(DEFAULT_FOREGROUND);
			}

			final FontMetrics fm = getFontMetrics(getFont());
			setText(value);
			setPreferredSize(new Dimension(fm.stringWidth(value), fm.getHeight() + 6));

			return this;
		}

	}

	@Retention(RetentionPolicy.RUNTIME)
	protected static @interface Key {

		KeyType value();
	}

	protected enum KeyType {
		DEFAULT, INTERNAL;
	}
}
