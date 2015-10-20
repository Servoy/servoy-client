/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.j2db.printing;


import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.PrinterJob;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.SimpleDoc;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.RepaintManager;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.ISmartClientApplication;
import com.servoy.j2db.cmd.ICmd;
import com.servoy.j2db.cmd.ICmdManager;
import com.servoy.j2db.dataprocessing.IFoundSetInternal;
import com.servoy.j2db.smart.J2DBClient;
import com.servoy.j2db.smart.cmd.CmdPageSetup;
import com.servoy.j2db.ui.IComponent;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ObjectWrapper;
import com.servoy.j2db.util.SwingHelper;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.toolbar.Toolbar;
import com.servoy.j2db.util.toolbar.ToolbarButton;

/**
 * This is the preview shower class with all the controls arround it
 * 
 * @author jblok
 */
public class PrintPreview extends JPanel implements ActionListener, ItemListener, IComponent
{
	private final JPanel desk;
	private final FormPreviewPanel fpp;
	private final String preferredPrinterName;
	private PrinterJob printerJob;
	private final int currentShowingPage;
	private final ISmartClientApplication application;

	private final JComboBox zoom;
	private final JComboBox pages;
	private final JButton previous_page;
	private final JButton next_page;

	private static final float MIN_ZOOM_VALUE = 0.1f;
	private static final float MAX_ZOOM_VALUE = 4.0f;

	public PrintPreview(ISmartClientApplication app, FormController formPanel, IFoundSetInternal fs, int zoomFactor, PrinterJob printerJob) throws Exception
	{
		this(app, formPanel, fs, printerJob);
		float z = getValidZoom(zoomFactor + "");
		fpp.zoom(z);
		updateEditor(z);
	}


	public PrintPreview(ISmartClientApplication app, FormController formPanel, IFoundSetInternal fs, PrinterJob printerJob) throws Exception
	{
		application = app;
		application.getScriptEngine().getJSApplication().setDidLastPrintPreviewPrint(false);

		preferredPrinterName = formPanel.getPreferredPrinterName();
		this.printerJob = printerJob;
		setLayout(new BorderLayout());
		Toolbar buttonPane = new Toolbar("print", application.getI18NMessage("servoy.toolbar.print.title"), false); //$NON-NLS-1$ //$NON-NLS-2$
		buttonPane.setFloatable(false);
		//        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		//        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		//        
		//        buttonPane.add(Box.createRigidArea(new
		// Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.addSeparator(new Dimension(J2DBClient.BUTTON_SPACING, 0));

		JButton print = new ToolbarButton(application.getI18NMessage("servoy.toolbar.print.button.print"), application.loadImage("print.gif")); //$NON-NLS-1$//$NON-NLS-2$
		print.setActionCommand("print"); //$NON-NLS-1$
		print.addActionListener(this);
		buttonPane.add(print);

		buttonPane.addSeparator(new Dimension(J2DBClient.BUTTON_SPACING, 0));
		//        buttonPane.add(Box.createRigidArea(new
		// Dimension(J2DBClient.BUTTON_SPACING, 0)));

		JButton pagesetup = new ToolbarButton(application.getI18NMessage("servoy.toolbar.print.button.pageSetup"), application.loadImage("page_setup.gif")); //$NON-NLS-1$//$NON-NLS-2$
		pagesetup.setActionCommand("pagesetup"); //$NON-NLS-1$
		pagesetup.addActionListener(this);
		buttonPane.add(pagesetup);
		//        buttonPane.add(Box.createRigidArea(new
		// Dimension(J2DBClient.BUTTON_SPACING, 0)));
		buttonPane.addSeparator(new Dimension(30, 0));

		//        buttonPane.add(new JLabel("Page: "));
		previous_page = new ToolbarButton(application.loadImage("page_previous.gif")); //$NON-NLS-1$
		//        previous_page.setPreferredSize(new Dimension(20,print.getHeight()));
		previous_page.setToolTipText(application.getI18NMessage("servoy.toolbar.print.button.prevPage.tooltip")); //$NON-NLS-1$
		previous_page.setActionCommand("pageprevious"); //$NON-NLS-1$
		previous_page.addActionListener(this);
		previous_page.setFocusPainted(false);
		buttonPane.add(previous_page);

		pages = new JComboBox();
		pages.setPreferredSize(new Dimension(70, ToolbarButton.PREF_HEIGHT));
		pages.setMaximumSize(new Dimension(70, ToolbarButton.PREF_HEIGHT));
		buttonPane.add(pages);

		next_page = new ToolbarButton(application.loadImage("page_next.gif")); //$NON-NLS-1$
		//        next_page.setPreferredSize(new Dimension(20,print.getHeight()));
		next_page.setToolTipText(application.getI18NMessage("servoy.toolbar.print.button.nextPage.tooltip")); //$NON-NLS-1$
		next_page.setActionCommand("pagenext"); //$NON-NLS-1$
		next_page.addActionListener(this);
		next_page.setFocusPainted(false);
		buttonPane.add(next_page);

		buttonPane.addSeparator(new Dimension(30, 0));
		//      buttonPane.add(Box.createRigidArea(new
		// Dimension(J2DBClient.BUTTON_SPACING, 0)));

		//        buttonPane.add(new JLabel("Zoom: "));

		zoom = new JComboBox();
		zoom.setEditable(true);
		zoom.setEditor(new BasicComboBoxEditor());
		zoom.setPreferredSize(new Dimension(100, ToolbarButton.PREF_HEIGHT));
		zoom.setMaximumSize(new Dimension(100, ToolbarButton.PREF_HEIGHT));
		zoom.addItem(new ObjectWrapper(application.getI18NMessage("servoy.print.zoom"), new Float(0.99f)));//to be enable //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper(new Float(MIN_ZOOM_VALUE * 100.0f).intValue() + "%", new Float(MIN_ZOOM_VALUE))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper("25%", new Float(0.25f))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper("50%", new Float(0.5f))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper("80%", new Float(0.8f))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper("100%", new Float(1f))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper("125%", new Float(1.25f))); //$NON-NLS-1$		
		zoom.addItem(new ObjectWrapper("150%", new Float(1.5f))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper("200%", new Float(2f))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper("300%", new Float(3f))); //$NON-NLS-1$
		zoom.addItem(new ObjectWrapper(new Float(MAX_ZOOM_VALUE * 100.0f).intValue() + "%", new Float(MAX_ZOOM_VALUE))); //$NON-NLS-1$
		//        zoom.setSelectedIndex(3);
		zoom.addItemListener(this);
		buttonPane.add(zoom);

		buttonPane.addSeparator(new Dimension(J2DBClient.BUTTON_SPACING, 0));
		//      buttonPane.add(Box.createRigidArea(new
		// Dimension(J2DBClient.BUTTON_SPACING, 0)));

		JButton close = new ToolbarButton(application.getI18NMessage("servoy.button.close")); //$NON-NLS-1$
		close.setActionCommand("close"); //$NON-NLS-1$
		close.addActionListener(this);
		//        close.setPreferredSize(print.getPreferredSize());
		buttonPane.add(close);
		//        buttonPane.add(Box.createHorizontalGlue());
		//        buttonPane.setBorder(BorderFactory.createEtchedBorder());//
		// mptyBorder(10, 10, 10, 10));
		add(buttonPane, BorderLayout.NORTH);

		desk = new JPanel();
		desk.setBackground(new Color(141, 141, 141));
		desk.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

		fpp = new FormPreviewPanel(application, formPanel, fs);
		desk.add(fpp, BorderLayout.CENTER);

		JScrollPane deskScroller = new ReleaseScrollPane(desk);
		add(deskScroller);

		currentShowingPage = 0;
	}

	private static class ReleaseScrollPane extends JScrollPane
	{
		public ReleaseScrollPane(Component view)
		{
			super(view);
		}

		/**
		 * @see JScrollPane#createHorizontalScrollBar()
		 */
		@Override
		public JScrollBar createHorizontalScrollBar()
		{
			return new EditListScrollBar(Adjustable.HORIZONTAL);
		}

		/**
		 * @see JScrollPane#createVerticalScrollBar()
		 */
		@Override
		public JScrollBar createVerticalScrollBar()
		{
			return new EditListScrollBar(Adjustable.VERTICAL);
		}

		protected class EditListScrollBar extends ScrollBar
		{
			protected int iValue = -1;

			public EditListScrollBar(int orientation)
			{
				super(orientation);
				setUnitIncrement(25);
			}

			//			public void setUI(ComponentUI ui)
			//			{
			//				super.setUI(ui);
			//			}
			//		
			/**
			 * @see Adjustable#setValue(int)
			 */
			@Override
			public void setValue(int v)
			{
				if (iValue == -1)
				{
					super.setValue(v);
				}
				else
				{
					iValue = v;
				}
			}

			/**
			 * @see Component#processMouseMotionEvent(MouseEvent)
			 */
			@Override
			protected void processMouseMotionEvent(MouseEvent e)
			{
				if (iValue == -1 && e.getID() == MouseEvent.MOUSE_DRAGGED)
				{
					iValue = getValue();
				}
				super.processMouseMotionEvent(e);
			}

			/**
			 * @see Component#processMouseEvent(MouseEvent)
			 */
			@Override
			protected void processMouseEvent(MouseEvent e)
			{
				//				if(e.getID() == MouseEvent.MOUSE_PRESSED)
				//				{
				//					if(getViewport().getView() instanceof CellEditorListener)
				//					{
				//						((CellEditorListener)getViewport().getView()).editingStopped(null);
				//						requestFocus();
				//					}
				//				}
				if (iValue != -1 && e.getID() == MouseEvent.MOUSE_RELEASED)
				{
					super.setValue(iValue);
					iValue = -1;
				}
				super.processMouseEvent(e);
			}
		}
	}

	public void showPages()
	{
		application.getScheduledExecutor().execute(new Runnable()
		{
			public void run()
			{
				application.blockGUI(application.getI18NMessage("servoy.print.status.generatePages")); //$NON-NLS-1$
				try
				{
					int pageCount = fpp.process();
					fillPageComboModel(pageCount);

					application.invokeLater(new Runnable()
					{
						public void run()
						{
							showPage(currentShowingPage);//show again in new Format
						}
					});
				}
				catch (Exception ex)
				{
					application.reportError(application.getI18NMessage("servoy.print.error.retrievingAllData"), ex); //$NON-NLS-1$
				}
				finally
				{
					application.releaseGUI();
				}
			}
		});
	}

	private void fillPageComboModel(int pageCount)
	{
		DefaultComboBoxModel dml = new DefaultComboBoxModel();
		for (int i = 0; i < pageCount; i++)
		{
			dml.addElement(new Integer(i + 1));
		}
		if (dml.getSize() != 0)
		{
			pages.setModel(dml);
			pages.setSelectedIndex(0);
			pages.addItemListener(this);
		}
		previous_page.setEnabled(false);
		next_page.setEnabled(dml.getSize() > 1);
	}

	//shows dialog if argument is null
	private void setPageFormat()//boolean showDialog)
	{
		try
		{
			PageFormat pf = CmdPageSetup.getPageFormat(fpp.getPageFormat(), application.getSettings(), application.getMainApplicationFrame());
			if (pf != null)
			{
				fpp.setPageFormat(pf);
			}
			showPages();
		}
		catch (Exception ex)
		{
			application.reportError(application.getI18NMessage("servoy.print.error.cannotCreatePreview"), ex); //$NON-NLS-1$
		}
		showPage(currentShowingPage);//show again in new Format
	}

	public static PrintService[] capablePrintServices;

	public static void startPrinting(IApplication application, Pageable pageable, PrinterJob a_printerJob, String a_preferredPrinterName,
		boolean showPrinterSelectDialog, boolean avoidDialogs)
	{
		RepaintManager currentManager = RepaintManager.currentManager(application.getPrintingRendererParent().getParent());
		boolean isDoubleBufferingEnabled = false;
		try
		{
			if (currentManager != null)
			{
				isDoubleBufferingEnabled = currentManager.isDoubleBufferingEnabled();
			}

			if (a_printerJob != null)
			{
				//for plugin printing
				a_printerJob.setPageable(pageable);
				a_printerJob.setJobName("Servoy Print");//$NON-NLS-1$
				if (showPrinterSelectDialog)
				{
					if (!a_printerJob.printDialog())
					{
						return;
					}
					SwingHelper.dispatchEvents(100);//hide dialog
				}
				if (currentManager != null)
				{
					currentManager.setDoubleBufferingEnabled(false);
				}
				a_printerJob.print();
			}
			else
			{
				//by default we use old system for mac, new is not always working
				boolean useSystemPrintDialog = Utils.getAsBoolean(application.getSettings().getProperty("useSystemPrintDialog", "" + Utils.isAppleMacOS())); //$NON-NLS-1$//$NON-NLS-2$
				DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
				PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
				if (capablePrintServices == null)
				{
					capablePrintServices = PrintServiceLookup.lookupPrintServices(flavor, pras);
				}

				PrintService service = null;
				if (capablePrintServices == null || capablePrintServices.length == 0)
				{
					if (avoidDialogs)
					{
						Debug.warn("Cannot find capable print services. Print aborted.");
						return;
					}
					else
					{
						JOptionPane.showConfirmDialog(((ISmartClientApplication)application).getMainApplicationFrame(),
							application.getI18NMessage("servoy.print.msg.noPrintersFound"), application.getI18NMessage("servoy.print.printing.title"), //$NON-NLS-1$ //$NON-NLS-2$
							JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
						capablePrintServices = new PrintService[0];
						showPrinterSelectDialog = true;//must show select printer and if none found show this
						useSystemPrintDialog = true;//we leave to system to show no printers are found, important for apple mac
					}
				}
				else
				{
					service = capablePrintServices[0];//default select
				}

				PrintService systemDefaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
				if (systemDefaultPrinter != null)
				{
					//check if system default printer is in capable list
					for (PrintService ps : capablePrintServices)
					{
						if (ps.getName().equalsIgnoreCase(systemDefaultPrinter.getName()))
						{
							service = systemDefaultPrinter;
							break;
						}
					}
				}

				boolean didFindPrinter = true; //want custom preferred printer
				if (a_preferredPrinterName != null)
				{
					didFindPrinter = false;
					for (PrintService ps : capablePrintServices)
					{
						if (ps.getName().equalsIgnoreCase(a_preferredPrinterName))
						{
							didFindPrinter = true;
							service = ps;
							break;
						}
					}
				}

				if (!didFindPrinter)
				{
					if (avoidDialogs)
					{
						Debug.warn("Cannot find capable printer for preferred form printer name '" + a_preferredPrinterName +
							"'. Trying to use default/any capable printer.");
					}
					else
					{
						showPrinterSelectDialog = true;//did not found the prefered , do show
					}
				}

				if (!useSystemPrintDialog)
				{
					if (showPrinterSelectDialog)
					{
						JFrame frame = ((ISmartClientApplication)application).getMainApplicationFrame();
						GraphicsConfiguration gc = frame.getGraphicsConfiguration();
						Point loc = frame.getLocation();
						service = ServiceUI.printDialog(gc, loc.x + 50, loc.y + 50, capablePrintServices, service, flavor, pras);
					}
					if (service != null)
					{
						if (currentManager != null)
						{
							currentManager.setDoubleBufferingEnabled(false);
						}
						DocPrintJob job = service.createPrintJob();
						DocAttributeSet das = new HashDocAttributeSet();
						Doc doc = new SimpleDoc(pageable, flavor, das);
						if (job != null)
						{
							job.print(doc, pras);
						}
						else
						{
							// for example if the print service cancels (e.g. print to pdf and then user cancel when choosing save location)
							application.reportWarning(application.getI18NMessage("servoy.print.error.cannotPrintDocument")); //$NON-NLS-1$
						}
					}
				}
				else
				{
					a_printerJob = PrinterJob.getPrinterJob();
					a_printerJob.setPageable(pageable);
					a_printerJob.setJobName("Servoy Print");//$NON-NLS-1$
					if (service != null)
					{
						a_printerJob.setPrintService(service);
					}
					if (showPrinterSelectDialog)
					{
						if (!a_printerJob.printDialog())
						{
							return;
						}
						SwingHelper.dispatchEvents(100);//hide dialog
					}
					if (currentManager != null)
					{
						currentManager.setDoubleBufferingEnabled(false);
					}
					a_printerJob.print();
				}
			}
		}
		catch (Exception ex)
		{
			application.reportError(application.getI18NMessage("servoy.print.error.cannotPrintDocument"), ex); //$NON-NLS-1$
		}
		finally
		{
			if (currentManager != null)
			{
				currentManager.setDoubleBufferingEnabled(isDoubleBufferingEnabled);
			}
		}
	}

	public void destroy()
	{
		if (fpp != null)
		{
			fpp.destroy();
			fpp.setVisible(false);//no more paints
		}
		printerJob = null;
	}

	public void showPage(int pageNumber)
	{
		try
		{
			fpp.showPage(pageNumber);
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}
	}

	private void show(int diff)
	{
		int idx = pages.getSelectedIndex();
		if (idx != -1)
		{
			int newIndex = idx + diff;
			if (newIndex < pages.getModel().getSize())
			{
				pages.setSelectedIndex(newIndex);
			}
		}
	}

	public void actionPerformed(ActionEvent ae)
	{
		String command = ae.getActionCommand();
		if (command.equals("print")) //$NON-NLS-1$
		{
			startPrinting(application, fpp.getPageable(), printerJob, preferredPrinterName, true, false);
		}
		else if (command.equals("pagesetup")) setPageFormat();//true); //$NON-NLS-1$
		else if (command.equals("pageprevious")) show(-1); //$NON-NLS-1$
		else if (command.equals("pagenext")) show(+1); //$NON-NLS-1$
		else if (command.equals("close")) //$NON-NLS-1$
		{
			ICmdManager cm = application.getCmdManager();
			Action a = cm.getRegisteredAction("cmdbrowsemode"); //$NON-NLS-1$
			ICmd cmd = (ICmd)a;
			cmd.doIt(ae);
		}
	}

	public void itemStateChanged(ItemEvent ie)
	{
		if (ie.getStateChange() == ItemEvent.SELECTED)
		{
			int idx = pages.getSelectedIndex();
			previous_page.setEnabled(idx != 0);
			next_page.setEnabled(idx < pages.getModel().getSize() - 1);

			if (ie.getSource() == pages)
			{
				showPage(idx);
			}
			else
			{
				//the comboBox editor also triggers an unnecessary ItemEvent(.SELECTED)
				if (zoom.getSelectedItem() instanceof String) return;
				ObjectWrapper tw = (ObjectWrapper)zoom.getSelectedItem();
				Float z = (Float)tw.getType();
				fpp.zoom(z.floatValue());

				if (getParent() != null)
				{
					invalidate();
					getParent().validate();
				}
			}
		}
		if (ie.getStateChange() == ItemEvent.DESELECTED)
		{
			int idx = pages.getSelectedIndex();
			previous_page.setEnabled(idx != 0);
			next_page.setEnabled(idx < pages.getModel().getSize() - 1);

			String zoomString = "";
			try
			{
				zoomString = (String)zoom.getEditor().getItem();
			}
			catch (ClassCastException e)
			{
				return;
			}
			if (zoomString.indexOf("%") == zoomString.length() - 1) zoomString = zoomString.substring(0, zoomString.length() - 1);
			float z = getValidZoom(zoomString);
			fpp.zoom(z);
			updateEditor(z);

			if (getParent() != null)
			{
				invalidate();
				getParent().validate();
			}
		}

	}

	/**
	 * sets the zoom factor to a value between MIN_ZOOM_VALUE and MAX_ZOOM_VALUE, validating the input
	 * 
	 * @param zoomFactor
	 * @return the zoom factor for the print preview
	 */
	private float getValidZoom(Object zoomFactor)
	{
		float z = Utils.getAsFloat(zoomFactor);
		if (z == 0) return 1.0f;
		z /= 100.0f;
		if (z < MIN_ZOOM_VALUE) z = MIN_ZOOM_VALUE;
		if (z > MAX_ZOOM_VALUE) z = MAX_ZOOM_VALUE;
		return z;
	}

	/**
	 * always display the zoom factor in the comboBox (when setting it manually or programatically)
	 * 
	 * @param zoomFactor
	 */
	private void updateEditor(float zoomFactor)
	{
		zoom.getEditor().setItem((new Float(zoomFactor * 100.0f)).intValue() + "%");
	}

	public void setComponentVisible(boolean visible)
	{
		setVisible(visible);
	}

	public void setComponentEnabled(boolean enabled)
	{
		setEnabled(enabled);
	}

	public String getId()
	{
		return (String)getClientProperty("Id"); //$NON-NLS-1$
	}
}
