package com.hbrb.spider.tool.view;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import org.dom4j.DocumentException;

import com.hbrb.spider.ConstantsHome;
import com.hbrb.spider.exception.TemplateParseException;
import com.hbrb.spider.model.article.SourceType;
import com.hbrb.spider.tool.controller.MainController;
import com.hbrb.util.ModelUtils;

public class MainFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private MainController mc;

	private JScrollPane articleItemTableScrollPane;
	private JTextArea templateTextArea;
	private JPanel templatesPanel;

	private JPanel siteSettingPanel;
	private JLabel sleepTimeLabel;
	private JTextField sleepTimeTextfield;
	private JLabel retryTimesLabel;
	private JTextField retryTimesTextfield;
	private JLabel charsetLabel;
	private JComboBox<String> charsetComboBox;
	private JLabel sourceTypeLabel;
	private JComboBox<String> sourceTypeComboBox;
	private JButton siteSettingButton;

	private Box mainNorthBox;
	private JPanel startUrlPanel;
	private JPanel templatesPathPanel;
	private JTextField startUrlTextField;
	private JTextField templatesPathTextField;
	private JLabel startUrlLabel;
	private JLabel templatesPathLabel;
	private JPanel templatesOperationPanel;
	private JButton templatesReloadButton;
	private JPanel spiderOperationPanel;
	private JButton spiderOperationButton;

	private JFileChooser fc;

	public MainFrame(final MainController mc) throws HeadlessException {
		super();
		this.mc = mc;
		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				mc.closeAction();
			}
		});
		setSize(800, 600);
		initComponents();
		loadTemplate();
	}

	private void initComponents() {
		initTemplatesPanel();
		getContentPane().add(templatesPanel, BorderLayout.CENTER);

		mainNorthBox = new Box(BoxLayout.Y_AXIS);
		siteSettingPanel = new JPanel();
		retryTimesLabel = new JLabel("重试次数:");
		siteSettingPanel.add(retryTimesLabel);
		int retryTimes = 0;
		retryTimesTextfield = new JTextField(String.valueOf(retryTimes), 2);
		DocumentListener siteSettingDocumentListener = new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				siteSettingDocumentChangedAction();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				siteSettingDocumentChangedAction();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				siteSettingDocumentChangedAction();
			}
		};
		retryTimesTextfield.getDocument().addDocumentListener(
				siteSettingDocumentListener);
		siteSettingPanel.add(retryTimesTextfield);
		sleepTimeLabel = new JLabel("请求间隔:");
		siteSettingPanel.add(sleepTimeLabel);
		int sleepTime = 0;
		sleepTimeTextfield = new JTextField(String.valueOf(sleepTime), 4);
		sleepTimeTextfield.getDocument().addDocumentListener(
				siteSettingDocumentListener);
		siteSettingPanel.add(sleepTimeTextfield);
		
		charsetLabel = new JLabel("编码:");
		siteSettingPanel.add(charsetLabel);
		charsetComboBox = new JComboBox<String>(new String[] { "GBK", "UTF-8",
				"null" });
		String defaultCharset = null;
		charsetComboBox.setSelectedItem(null == defaultCharset ? "null"
				: defaultCharset);
		charsetComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					siteSettingButton.setEnabled(true);
				}
			}
		});
		siteSettingPanel.add(charsetComboBox);
		
		sourceTypeLabel = new JLabel("信源类型:");
		siteSettingPanel.add(sourceTypeLabel);
		String[] items = new String[] { SourceType.Name.WEB, SourceType.Name.PAPER,
				SourceType.Name.BBS, SourceType.Name.BLOG };
		sourceTypeComboBox = new JComboBox<String>(items);
		sourceTypeComboBox.setSelectedItem(items[0]);
		sourceTypeComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					siteSettingButton.setEnabled(true);
				}
			}
		});
		siteSettingPanel.add(sourceTypeComboBox);
		
		siteSettingButton = new JButton("应用");
		siteSettingButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int retryTimes = -1;
				try {
					retryTimes = Integer.parseInt(retryTimesTextfield.getText());
				} catch (NumberFormatException exp) {
				}
				if (retryTimes < 0) {
					JOptionPane.showMessageDialog(MainFrame.this,
							"重试次数只能为大于等于0的整数", "提示",
							JOptionPane.WARNING_MESSAGE);
				}
				int sleepTime = 0;
				try {
					sleepTime = Integer.parseInt(sleepTimeTextfield.getText());
				} catch (NumberFormatException exp) {
				}
				if (sleepTime < 0) {
					JOptionPane.showMessageDialog(MainFrame.this,
							"请求间隔只能为大于等于0的整数", "提示", JOptionPane.WARNING_MESSAGE);
				}
				String charset = charsetComboBox.getSelectedItem().toString();
				String sourceTypeName = sourceTypeComboBox.getSelectedItem().toString();
				int sourceType = ModelUtils.sourceName2Type(sourceTypeName);
				mc.updateSite(retryTimes, sleepTime,
						charset.equals("null") ? null : charset, sourceType);
				siteSettingButton.setEnabled(false);
			}
		});
		siteSettingButton.setEnabled(false);
		siteSettingPanel.add(siteSettingButton);
		mainNorthBox.add(siteSettingPanel);

		startUrlPanel = new JPanel(new BorderLayout());
		startUrlTextField = new JTextField(null);
		startUrlTextField.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {}
			
			@Override
			public void mousePressed(MouseEvent e) {}
			
			@Override
			public void mouseExited(MouseEvent e) {}
			
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				((JTextField)e.getSource()).selectAll();
			}
		});
		startUrlPanel.add(startUrlTextField, BorderLayout.CENTER);
		startUrlLabel = new JLabel("URL：");
		startUrlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		startUrlPanel.add(startUrlLabel, BorderLayout.WEST);
		spiderOperationPanel = new JPanel();
		spiderOperationButton = new JButton("执行");
		spiderOperationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String url = startUrlTextField
						.getText().trim();
				if (!url.startsWith("http")) {
					JOptionPane.showMessageDialog(spiderOperationButton, "URL：" + url, "URL异常", JOptionPane.WARNING_MESSAGE);
					return;
				}
				mc.executeTaskAction(url);
			}
		});
		spiderOperationPanel.add(spiderOperationButton);
		startUrlPanel.add(spiderOperationPanel, BorderLayout.EAST);
		mainNorthBox.add(startUrlPanel);

		templatesPathPanel = new JPanel(new BorderLayout());
		templatesPathTextField = new JTextField(ConstantsHome.USER_DIR + File.separatorChar + "template_page.xml");
		templatesPathTextField.setEditable(false);
		templatesPathTextField.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				fc.setCurrentDirectory(new File(templatesPathTextField
						.getText()).getParentFile());
				int returnVal = fc.showOpenDialog(MainFrame.this);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					if (file.exists()) {
						templatesPathTextField.setText(file.getAbsolutePath());
						loadTemplate();
					}
				}
			}
		});
		templatesPathPanel.add(templatesPathTextField, BorderLayout.CENTER);
		templatesPathLabel = new JLabel("模板：");
		templatesPathLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		templatesPathPanel.add(templatesPathLabel, BorderLayout.WEST);
		templatesOperationPanel = new JPanel();
		templatesReloadButton = new JButton("重载");
		templatesReloadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadTemplate();
			}
		});
		templatesOperationPanel.add(templatesReloadButton);
		templatesPathPanel.add(templatesOperationPanel, BorderLayout.EAST);
		mainNorthBox.add(templatesPathPanel);

		getContentPane().add(mainNorthBox, BorderLayout.NORTH);

		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.addChoosableFileFilter(new FileFilter() {
			
			@Override
			public String getDescription() {
				return "XML";
			}
			
			@Override
			public boolean accept(File file) {
				if (file.isDirectory()) {
					return true;
				}
				if (file.getName().toLowerCase().endsWith(".xml")) {
					return true;
				}
				return false;
			}
		});
	}

	private void initTemplatesPanel() {
		templatesPanel = new JPanel(new BorderLayout());
		templateTextArea = new JTextArea();
		templateTextArea.setEditable(false);
		Font font = new Font("SYSTEM", Font.PLAIN, 16);
		templateTextArea.setFont(font);
		articleItemTableScrollPane = new JScrollPane(templateTextArea);
		templatesPanel.add(articleItemTableScrollPane, BorderLayout.CENTER);
	}

	private void siteSettingDocumentChangedAction() {
		if (!siteSettingButton.isEnabled()) {
			siteSettingButton.setEnabled(true);
		}
	}

	private void loadTemplate() {
		String content = "";
		try {
			content = mc.loadTemplate(templatesPathTextField.getText());
		} catch (TemplateParseException | DocumentException | IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "模板载入失败", JOptionPane.WARNING_MESSAGE);
		}
		refreshTemplate(content);
	}
	
	public void refreshTemplate(String content){
		templateTextArea.setText(content.replace("\t", "    "));
	}

	public String getTemplatesPath() {
		return templatesPathTextField.getText();
	}
}
