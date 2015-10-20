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
package com.servoy.j2db.util.gui;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.PageFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Vector;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.text.NumberFormatter;

import com.servoy.j2db.Messages;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * A class which implements a cross-platform page size dialog.
 */
public class PageSetupDialog extends JEscapeDialog implements ActionListener, ItemListener
{

	public final static int DEFAULT = 2;
	/**
	 * Approve print status (user activated "Print" or "OK").
	 */
	public final static int APPROVE = 1;

	/**
	 * Cancel print status (user activated "Cancel");
	 */
	public final static int CANCEL = 0;

	public static final String UNIT_MM = "mm"; //$NON-NLS-1$
	public static final String UNIT_INCH = "inch"; //$NON-NLS-1$
	public static final String UNIT_PIXELS = "pixels"; //$NON-NLS-1$


	private JButton btnApprove, btnDefault;
	private final JComboBox cbxUnits;
	private int status;

	private int currentUnits = Size2DSyntax.MM;

	//page settings
	private MediaSizeName mediaSizeName = MediaSizeName.ISO_A4; //if this is null it's a custom size paper
	private Size2DSyntax printingPageSize = null;
	private MediaMargins mediaMargins = null; //this defines the margin
	private int orientation;

	private final PageSetupPanel pnlPageSetup;

	/**
	 * Constructor for the "standard" print dialog (containing all relevant tabs)
	 */
	public PageSetupDialog(Frame gc, boolean showDefault)
	{
		this(gc, true, showDefault);
	}

	/**
	 * Constructor for the "standard" print dialog (containing all relevant tabs)
	 */
	public PageSetupDialog(Frame gc, boolean modal, boolean showDefault)
	{
		super(gc, Messages.getString("servoy.pagesetup.title"), modal); //$NON-NLS-1$

		Container c = getContentPane();
		c.setLayout(new BorderLayout());

		pnlPageSetup = new PageSetupPanel();
		pnlPageSetup.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		c.add(pnlPageSetup, BorderLayout.CENTER);

		String defaultCountry = Locale.getDefault().getCountry();
		if (defaultCountry != null &&
			(defaultCountry.equals("") //$NON-NLS-1$
				||
				defaultCountry.equals(Locale.US.getCountry()) || defaultCountry.equals(Locale.CANADA.getCountry()) || defaultCountry.equals(Locale.UK.getCountry())))
		{
			currentUnits = Size2DSyntax.INCH;
		}
		else
		{
			currentUnits = Size2DSyntax.MM;
		}

		String[] unitOptions = { UNIT_MM, UNIT_INCH, UNIT_PIXELS };
		cbxUnits = new JComboBox(unitOptions);
		cbxUnits.addItemListener(this);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(cbxUnits, BorderLayout.WEST);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(btnApprove = createButton(Messages.getString("servoy.button.ok"), this)); //$NON-NLS-1$
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(/* btnCancel = */createButton(Messages.getString("servoy.button.cancel"), this)); //$NON-NLS-1$
		if (showDefault)
		{
			buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
			buttonPane.add(btnDefault = createButton(Messages.getString("servoy.button.default"), this)); //$NON-NLS-1$
		}
		c.add(buttonPane, BorderLayout.SOUTH);

		setName("PageSetupDialog"); //$NON-NLS-1$
		pack();
		setLocationRelativeTo(gc);
		setResizable(false);
	}

	public void showDialog(PageFormat pageFormat)
	{
		PageFormat pf = (pageFormat == null) ? new PageFormat() : pageFormat;

		orientation = pf.getOrientation();

		float w = (float)pf.getPaper().getWidth() / Utils.PPI;
		float h = (float)pf.getPaper().getHeight() / Utils.PPI;

		mediaSizeName = null;
		if (w > 0 && h > 0) mediaSizeName = MediaSize.findMedia(w, h, Size2DSyntax.INCH);
		if (mediaSizeName != null && softEqual(MediaSize.getMediaSizeForName(mediaSizeName), w, h))
		{
			setPaperSize(MediaSize.getMediaSizeForName(mediaSizeName));
		}
		else
		{
			setPaperSize(w, h, Size2DSyntax.INCH);
			mediaSizeName = null; // custom size
		}

		// Note: PageFormat get-methods take into account the orientation.
		double x = pf.getImageableX();
		double y = pf.getImageableY();
		double iw = pf.getImageableWidth();
		double ih = pf.getImageableHeight();

		float lm = (float)x / Utils.PPI;
		float rm = (float)((pf.getWidth() - (x + iw)) / Utils.PPI);
		float tm = (float)y / Utils.PPI;
		float bm = (float)((pf.getHeight() - (y + ih)) / Utils.PPI);
		mediaMargins = new MediaMargins(lm, rm, tm, bm, Size2DSyntax.INCH).convertToUnit(currentUnits);

		if (currentUnits == Size2DSyntax.MM)
		{
			cbxUnits.setSelectedIndex(0);
		}
		else if (currentUnits == Size2DSyntax.INCH)
		{
			cbxUnits.setSelectedIndex(1);
		}
		else
		{
			cbxUnits.setSelectedIndex(2);
		}

		updatePanels();
		setVisible(true);
	}

	private void setOrientation(int newOrientation)
	{
		if (orientation != newOrientation)
		{
			if ((orientation == PageFormat.PORTRAIT) || (newOrientation == PageFormat.PORTRAIT)) printingPageSize = new MySize2DSyntax(printingPageSize, true); // needs flip
			orientation = newOrientation;
		}
	}

	/**
	 * @param w the paper width. (ignoring orientation)
	 * @param h the pager height. (ignoring orientation)
	 */
	private void setPaperSize(float w, float h, int currentUnits)
	{
		if (orientation == PageFormat.PORTRAIT)
		{
			printingPageSize = new MySize2DSyntax(w, h, currentUnits);
		}
		else
		{
			printingPageSize = new MySize2DSyntax(h, w, currentUnits);
		}
	}

	/**
	 * @param size the paper size. (ignoring orientation)
	 */
	private void setPaperSize(Size2DSyntax size)
	{
		printingPageSize = new MySize2DSyntax(size, orientation != PageFormat.PORTRAIT);
	}

	private Size2DSyntax getPaperSize()
	{
		Size2DSyntax d;
		if (orientation == PageFormat.PORTRAIT)
		{
			d = printingPageSize;
		}
		else
		{
			d = new MySize2DSyntax(printingPageSize, true); // flip
		}
		return d;
	}

	public static final float precision = 1e-4f;//check for up to 4 digits after the comma

	private boolean softEqual(Size2DSyntax size, float w, float h)
	{
		if (size == null) return false;

		float a = size.getX(Size2DSyntax.INCH);
		float b = w;
		float c = size.getY(Size2DSyntax.INCH);
		float d = h;
		return (a == b || (Math.abs(a - b) < precision && ((1 - precision) < a / b && a / b < (1 + precision)))) &&
			(c == d || (Math.abs(c - d) < precision && ((1 - precision) < c / d && c / d < (1 + precision))));
	}

	private static NumberFormatter getNumberFormatter(int unit)
	{
		DecimalFormat format = new DecimalFormat();
		if (unit == Size2DSyntax.INCH)
		{ //units in inches
			format = new DecimalFormat("####.##"); //$NON-NLS-1$
			format.setMaximumIntegerDigits(4);
			format.setMinimumFractionDigits(1);
			format.setMaximumFractionDigits(2);
			format.setDecimalSeparatorAlwaysShown(true);
		}
		else if (unit == Size2DSyntax.MM)
		{ //units in mm
			format = new DecimalFormat("####.##"); //$NON-NLS-1$
			format.setMaximumIntegerDigits(4);
			format.setMinimumFractionDigits(1);
			format.setMaximumFractionDigits(2);
			format.setDecimalSeparatorAlwaysShown(true);
		}
		else if (unit == Size2DSyntax.INCH / Utils.PPI)
		{ //units in pixels
			format = new DecimalFormat("#"); //$NON-NLS-1$
			format.setMaximumIntegerDigits(4);
			format.setMaximumFractionDigits(0);
		}

		format.setMinimumIntegerDigits(1);
		format.setParseIntegerOnly(false);

		NumberFormatter nf = new NumberFormatter(format);
		nf.setMinimum(new Float(0.0f));
		nf.setMaximum(new Float(9999.0f));
		nf.setAllowsInvalid(true);
		nf.setCommitsOnValidEdit(true);

		return nf;
	}


	public PageFormat getPageFormat()
	{
		if (status != APPROVE)
		{
			return null;
		}
		Size2DSyntax paperSize = getPaperSize();
		MediaMargins inchPageMargins = mediaMargins.convertToUnit(Size2DSyntax.INCH);
		return Utils.createPageFormat(paperSize.getX(Size2DSyntax.INCH), paperSize.getY(Size2DSyntax.INCH), inchPageMargins.getLeftMargin(),
			inchPageMargins.getRightMargin(), inchPageMargins.getTopMargin(), inchPageMargins.getBottomMargin(), orientation, Size2DSyntax.INCH);
	}

	public static class MySize2DSyntax extends Size2DSyntax
	{

		public MySize2DSyntax(float x, float y, int units)
		{
			super(x, y, units);
		}

		public MySize2DSyntax(Size2DSyntax size, boolean flip)
		{
			super(flip ? size.getY(Size2DSyntax.INCH) : size.getX(Size2DSyntax.INCH), flip ? size.getX(Size2DSyntax.INCH) : size.getY(Size2DSyntax.INCH),
				Size2DSyntax.INCH);
		}

	}

	private static class MediaMargins
	{

		private final float leftMargin;
		private final float rightMargin;
		private final float topMargin;
		private final float bottomMargin;
		private final int unit;

		MediaMargins(float leftMargin, float rightMargin, float topMargin, float bottomMargin, int unit)
		{
			this.leftMargin = leftMargin;
			this.rightMargin = rightMargin;
			this.topMargin = topMargin;
			this.bottomMargin = bottomMargin;
			this.unit = unit;
		}

		float getBottomMargin()
		{
			return bottomMargin;
		}

		float getLeftMargin()
		{
			return leftMargin;
		}

		float getRightMargin()
		{
			return rightMargin;
		}

		float getTopMargin()
		{
			return topMargin;
		}

		int getUnit()
		{
			return unit;
		}

		static MediaMargins getMarginsMinimal(int unit)
		{
			return new MediaMargins(0f, 0f, 0f, 0f, unit);
		}

		MediaMargins convertToUnit(int newUnit)
		{
			if (newUnit == unit)
			{
				return this;
			}
			return new MediaMargins((float)Utils.convertPageFormatUnit(unit, newUnit, leftMargin), (float)Utils.convertPageFormatUnit(unit, newUnit,
				rightMargin), (float)Utils.convertPageFormatUnit(unit, newUnit, topMargin), (float)Utils.convertPageFormatUnit(unit, newUnit, bottomMargin),
				newUnit);
		}

	}


	private class MediaPanel extends JPanel implements ItemListener
	{

		private final String strTitle = Messages.getString("servoy.pagesetup.paper.title"); //$NON-NLS-1$
		private final JLabel lblSize;
		private final JComboBox cbSize;
		private final Vector sizes = new Vector();
		private MarginsPanel pnlMargins = null;
		private DimensionsPanel pnlDimensions = null;

		public MediaPanel()
		{
			super(new BorderLayout());
			setBorder(BorderFactory.createTitledBorder(strTitle));

			cbSize = new JComboBox();

			lblSize = new JLabel(Messages.getString("servoy.pagesetup.label.size"), SwingConstants.TRAILING); //$NON-NLS-1$
			lblSize.setLabelFor(cbSize);
			add(lblSize, BorderLayout.WEST);
			add(cbSize, BorderLayout.CENTER);

			pnlDimensions = new DimensionsPanel();
			add(pnlDimensions, BorderLayout.SOUTH);
		}

		public void itemStateChanged(ItemEvent e)
		{
			Object source = e.getSource();
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				if (source == cbSize)
				{
					int index = cbSize.getSelectedIndex();
					if ((index >= 0) && (index < sizes.size()))
					{
						mediaSizeName = (MediaSizeName)sizes.get(index);
						setPaperSize(MediaSize.getMediaSizeForName(mediaSizeName));
						mediaMargins = MediaMargins.getMarginsMinimal(currentUnits);
					}
					else if (index >= sizes.size())
					{
						mediaSizeName = null;
					}
				}
				if (pnlMargins != null)
				{
					pnlMargins.updateInfo();//check if margins ok
				}
				if (pnlDimensions != null)
				{
					pnlDimensions.updateInfo();//display correct size for selected paper
				}
			}
		}

		/* this is ad hoc to keep things simple */
		public void addMediaListener(MarginsPanel pnl)
		{
			pnlMargins = pnl;
		}

		public void addMediaListener(DimensionsPanel pnl)
		{
			pnlDimensions = pnl;
		}

		//copy object values to display
		public void updateInfo()
		{
			boolean mediaSupported = false;

			cbSize.removeItemListener(this);
			cbSize.removeAllItems();

			sizes.clear();
			{
				mediaSupported = true;

				class MyMedia extends MediaSizeName
				{
					MyMedia()
					{
						super(1);
					}

					@Override
					public EnumSyntax[] getEnumValueTable()
					{
						return super.getEnumValueTable();
					}

					@Override
					public String[] getStringTable()
					{
						return super.getStringTable();
					}
				}
				MyMedia mym = new MyMedia();
				Media[] media = (Media[])mym.getEnumValueTable();
				String[] names = mym.getStringTable();
				for (int i = 0; i < media.length; i++)
				{
					Media medium = media[i];
					if (medium instanceof MediaSizeName)
					{
						sizes.add(medium);
						cbSize.addItem(names[i]);
					}
				}
				cbSize.addItem(Messages.getString("servoy.pagesetup.list.size.custom")); //$NON-NLS-1$
			}

			boolean msSupported = (mediaSupported && (sizes.size() > 0));
			lblSize.setEnabled(msSupported);
			cbSize.setEnabled(msSupported);
			if (mediaSupported)
			{
				if (mediaSizeName != null)
				{
					int index = sizes.indexOf(mediaSizeName);
					cbSize.setSelectedIndex(index);
				}
				else
				{
					//custom page dimensions
					cbSize.setSelectedIndex(sizes.size());
				}
			}
			cbSize.addItemListener(this);

			pnlDimensions.updateInfo();
		}
	}

	public void actionPerformed(ActionEvent e)
	{
		status = CANCEL;

		Object source = e.getSource();
		if (source == btnApprove)
		{
			status = APPROVE;
		}
		if (source == btnDefault)
		{
			status = DEFAULT;
		}
		setVisible(false);
	}

	public JButton getOKButton()
	{
		return btnApprove;
	}

	public void itemStateChanged(ItemEvent e)
	{
		Object source = e.getSource();
		if (source == cbxUnits)
		{
			String item = cbxUnits.getSelectedItem().toString();
			int unit = Size2DSyntax.INCH;
			if (item.equals(UNIT_INCH))
			{
				unit = Size2DSyntax.INCH;
			}
			if (item.equals(UNIT_MM))
			{
				unit = Size2DSyntax.MM;
			}
			if (item.equals(UNIT_PIXELS))
			{
				unit = (int)(Size2DSyntax.INCH / Utils.PPI);
			}
			pnlPageSetup.pnlMedia.pnlDimensions.updateUnits(unit);
			pnlPageSetup.pnlMargins.updateUnits(unit);
			currentUnits = unit;
		}
	}

	/**
	 * Updates each of the top level panels
	 */
	private void updatePanels()
	{
		pnlPageSetup.updateInfo();
	}

	/**
	 * Creates a new JButton and sets its text, mnemonic, and ActionListener
	 */
	private static JButton createButton(String key, ActionListener al)
	{
		JButton btn = new JButton(key);
		btn.addActionListener(al);
		return btn;
	}

	/**
	 * The "Page Setup" tab. Includes the controls for MediaSource/MediaTray, OrientationRequested, and Sides.
	 */
	private class PageSetupPanel extends JPanel
	{
		private final MediaPanel pnlMedia;
		private final OrientationPanel pnlOrientation;
		private final MarginsPanel pnlMargins;

		public PageSetupPanel()
		{
			super();
			setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			setLayout(new BorderLayout());

			pnlMedia = new MediaPanel();
			add(pnlMedia, BorderLayout.NORTH);

			pnlOrientation = new OrientationPanel();
			add(pnlOrientation, BorderLayout.WEST);

			pnlMargins = new MarginsPanel();
			pnlOrientation.addOrientationListener(pnlMedia.pnlDimensions);
			pnlMedia.addMediaListener(pnlMargins);
			add(pnlMargins, BorderLayout.CENTER);
		}

		public void updateInfo()
		{
			pnlMedia.updateInfo();
			pnlOrientation.updateInfo();
			pnlMargins.updateInfo();
		}
	}

	private class DimensionsPanel extends JPanel implements ActionListener, FocusListener
	{
		private final JFormattedTextField height, width;
		private final JLabel lblHeight, lblWidth;

		public DimensionsPanel()
		{
			super(new BorderLayout());

			setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
			JPanel dimensions = new JPanel();
			dimensions.setLayout(new GridLayout(2, 2, 5, 5));

			NumberFormatter nf = getNumberFormatter(currentUnits);

			height = new JFormattedTextField(nf);
			height.addFocusListener(this);
			height.addActionListener(this);

			width = new JFormattedTextField(nf);
			width.addFocusListener(this);
			width.addActionListener(this);

			lblHeight = new JLabel(Messages.getString("servoy.pagesetup.label.pageheight"), SwingConstants.LEADING); //$NON-NLS-1$
			lblHeight.setLabelFor(height);

			lblWidth = new JLabel(Messages.getString("servoy.pagesetup.label.pagewidth"), SwingConstants.LEADING); //$NON-NLS-1$
			lblWidth.setLabelFor(width);

			dimensions.add(lblWidth);
			dimensions.add(width);
			dimensions.add(lblHeight);
			dimensions.add(height);

			add(dimensions, BorderLayout.NORTH);
			add(new JLabel(), BorderLayout.CENTER);
		}

		public void setNumberFormat()
		{
			NumberFormatter nf = getNumberFormatter(currentUnits);

			nf.install(height);
			nf.install(width);
		}

		public void actionPerformed(ActionEvent e)
		{
			updateDimensions(e.getSource());
		}

		public void focusLost(FocusEvent e)
		{
			updateDimensions(e.getSource());
		}

		public void focusGained(FocusEvent e)
		{
		}

		public void updateUnits(int unit)
		{
			currentUnits = unit;
			setNumberFormat();
			updateInfo();
		}

		//copy the value from the object into the field
		public void updateInfo()
		{
			boolean isCustomFormat = (mediaSizeName == null);
			height.setEnabled(isCustomFormat);
			width.setEnabled(isCustomFormat);
			lblHeight.setEnabled(isCustomFormat);
			lblWidth.setEnabled(isCustomFormat);

			height.setValue(new Float(printingPageSize.getY(currentUnits)));
			width.setValue(new Float(printingPageSize.getX(currentUnits)));
		}

		//copy the values from the fields into the objects
		public void updateDimensions(Object source)
		{
			if (!(source instanceof JFormattedTextField))
			{
				updateInfo();
				return;
			}
			else
			{
				JFormattedTextField tf = (JFormattedTextField)source;
				try
				{
					tf.commitEdit();
				}
				catch (ParseException e)
				{
					Debug.error(e);
				}
				Float val = (Float)tf.getValue();
				if (val == null)
				{
					updateInfo();
					return;
				}

				printingPageSize = new MySize2DSyntax(Utils.getAsFloat(width.getValue()), Utils.getAsFloat(height.getValue()), currentUnits);
				updateInfo();
			}
		}
	}

	private class MarginsPanel extends JPanel implements ActionListener, FocusListener
	{
		private final String strTitle = Messages.getString("servoy.pagesetup.margins.title"); //$NON-NLS-1$
		private final JFormattedTextField leftMargin, rightMargin, topMargin, bottomMargin;
		private final JLabel lblLeft, lblRight, lblTop, lblBottom;

		public MarginsPanel()
		{
			super(new BorderLayout());

			NumberFormatter nf = getNumberFormatter(currentUnits);

			leftMargin = new JFormattedTextField(nf);
			leftMargin.addFocusListener(this);
			leftMargin.addActionListener(this);
			rightMargin = new JFormattedTextField(nf);
			rightMargin.addFocusListener(this);
			rightMargin.addActionListener(this);
			topMargin = new JFormattedTextField(nf);
			topMargin.addFocusListener(this);
			topMargin.addActionListener(this);
			bottomMargin = new JFormattedTextField(nf);
			bottomMargin.addFocusListener(this);
			bottomMargin.addActionListener(this);

			setBorder(BorderFactory.createTitledBorder(strTitle));
			JPanel margins = new JPanel();
			margins.setLayout(new GridLayout(4, 2, 5, 5));

			lblLeft = new JLabel(Messages.getString("servoy.pagesetup.label.leftmargin"), SwingConstants.LEADING); //$NON-NLS-1$
			lblLeft.setLabelFor(leftMargin);

			lblRight = new JLabel(Messages.getString("servoy.pagesetup.label.rigthmargin"), SwingConstants.LEADING); //$NON-NLS-1$
			lblRight.setLabelFor(rightMargin);

			lblTop = new JLabel(Messages.getString("servoy.pagesetup.label.topmargin"), SwingConstants.LEADING); //$NON-NLS-1$
			lblTop.setLabelFor(topMargin);

			lblBottom = new JLabel(Messages.getString("servoy.pagesetup.label.bottommargin"), SwingConstants.LEADING); //$NON-NLS-1$
			lblBottom.setLabelFor(bottomMargin);

			margins.add(lblLeft);
			margins.add(leftMargin);
			margins.add(lblRight);
			margins.add(rightMargin);

			margins.add(lblTop);
			margins.add(topMargin);
			margins.add(lblBottom);
			margins.add(bottomMargin);

			add(margins, BorderLayout.NORTH);
			add(new JLabel(), BorderLayout.CENTER);
		}

		public void setNumberFormat()
		{
			NumberFormatter nf = getNumberFormatter(currentUnits);

			nf.install(leftMargin);
			nf.install(rightMargin);
			nf.install(topMargin);
			nf.install(bottomMargin);
		}


		public void actionPerformed(ActionEvent e)
		{
			updateMargins(e.getSource());
		}

		public void focusLost(FocusEvent e)
		{
			updateMargins(e.getSource());
		}

		public void focusGained(FocusEvent e)
		{
		}

		/*
		 * Get the numbers, use to create a MPA. (field values -> mpa) If its valid, accept it and update the attribute set. If its not valid, then reject it
		 * and call updateInfo() to re-establish the previous entries.
		 */
		public void updateMargins(Object source)
		{
			if (!(source instanceof JFormattedTextField))
			{
				updateInfo();
				return;
			}
			else
			{
				JFormattedTextField tf = (JFormattedTextField)source;
				try
				{
					tf.commitEdit();
				}
				catch (ParseException e)
				{
					Debug.error(e);
				}
				Float val = (Float)tf.getValue();
				if (val == null)
				{
					updateInfo();
					return;
				}
			}

			float lm = Utils.getAsFloat(leftMargin.getValue());
			float rm = Utils.getAsFloat(rightMargin.getValue());
			float tm = Utils.getAsFloat(topMargin.getValue());
			float bm = Utils.getAsFloat(bottomMargin.getValue());

			mediaMargins = validateMargins(lm, rm, tm, bm, currentUnits);
			updateInfo();
		}

		/*
		 * This method either accepts the values and creates a new MediaPrintableArea, or does nothing. It should not attempt to create a printable area from
		 * anything other than the exact values passed in. But REMIND/TBD: it would be user friendly to replace margins the user entered but are out of bounds
		 * with the minimum. At that point this method will need to take responsibility for updating the "stored" values and the UI.
		 */
		private MediaMargins validateMargins(float lm, float rm, float tm, float bm, int unit)
		{
			if (lm < 0f || rm < 0f || tm < 0f || bm < 0f || (lm + rm) >= printingPageSize.getX(unit) || (tm + bm) >= printingPageSize.getY(unit))
			{
				// no more area left to print...
				return MediaMargins.getMarginsMinimal(unit);
			}

			return new MediaMargins(lm, rm, tm, bm, unit);
		}

		public void updateUnits(int unit)
		{
			setNumberFormat();
			mediaMargins = mediaMargins.convertToUnit(unit);
			updateInfo();
		}

		/*
		 * Copy mpa object values to the fields. This is complex as a MediaPrintableArea is valid only within a particular context of media size. So we need a
		 * MediaSize as well as a MediaPrintableArea. MediaSize can be obtained from MediaSizeName. If the application specifies a MediaPrintableArea, we accept
		 * it to the extent its valid for the Media they specify. If they don't specify a Media, then the default is assumed.
		 * 
		 * If an application doesn't define a MediaPrintableArea, we need to create a suitable one, this is created using the specified (or default) Media and
		 * default 1 inch margins. This is validated against the paper in case this is too large for tiny media.
		 */
		public void updateInfo()
		{
			leftMargin.setValue(new Float(mediaMargins.getLeftMargin()));
			rightMargin.setValue(new Float(mediaMargins.getRightMargin()));
			topMargin.setValue(new Float(mediaMargins.getTopMargin()));
			bottomMargin.setValue(new Float(mediaMargins.getBottomMargin()));

			setNumberFormat();
		}
	}

	private class OrientationPanel extends JPanel implements ActionListener
	{
		private final String strTitle = Messages.getString("servoy.pagesetup.orientation.title"); //$NON-NLS-1$
		private final IconRadioButton rbPortrait, rbLandscape, /* rbRevPortrait, */rbRevLandscape;
		private DimensionsPanel pnlDimensions;

		public OrientationPanel()
		{
			super(new GridLayout(4, 1));
			setBorder(BorderFactory.createTitledBorder(strTitle));

			ButtonGroup bg = new ButtonGroup();
			rbPortrait = new IconRadioButton(Messages.getString("servoy.pagesetup.button.portrait"), //$NON-NLS-1$
				true, bg, this);
			rbPortrait.addActionListener(this);
			add(rbPortrait);
			rbLandscape = new IconRadioButton(Messages.getString("servoy.pagesetup.button.landscape"), //$NON-NLS-1$
				false, bg, this);
			rbLandscape.addActionListener(this);
			add(rbLandscape);
//			rbRevPortrait = new IconRadioButton(Messages.getString("servoy.pagesetup.button.reversedportrait"), //$NON-NLS-1$
//				false, bg, this);
//			rbRevPortrait.addActionListener(this);
//			add(rbRevPortrait);
			rbRevLandscape = new IconRadioButton(Messages.getString("servoy.pagesetup.button.reversedlandscape"), //$NON-NLS-1$
				false, bg, this);
			rbRevLandscape.addActionListener(this);
			add(rbRevLandscape);
		}

		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			if (rbPortrait == (source))
			{
				setOrientation(PageFormat.PORTRAIT);
			}
			else if (rbLandscape == (source))
			{
				setOrientation(PageFormat.LANDSCAPE);
			}
//			else if (rbRevPortrait == (source))
//			{
//				orientation = PageFormat.REVERSE_PORTRAIT;
//			}
			else if (rbRevLandscape == (source))
			{
				setOrientation(PageFormat.REVERSE_LANDSCAPE);
			}

			// orientation affects display of margins.
			pnlDimensions.updateInfo();
		}

		/* This is ad hoc to keep things simple */
		void addOrientationListener(DimensionsPanel pnlDimensions)
		{
			this.pnlDimensions = pnlDimensions;
		}

		public void updateInfo()
		{
			rbPortrait.setEnabled(true);
			rbLandscape.setEnabled(true);
			rbRevLandscape.setEnabled(true);

			if (orientation == PageFormat.PORTRAIT)
			{
				rbPortrait.setSelected(true);
			}
			else if (orientation == PageFormat.LANDSCAPE)
			{
				rbLandscape.setSelected(true);
			}
			else if (orientation == PageFormat.REVERSE_LANDSCAPE)
			{
				rbRevLandscape.setSelected(true);
			}
//			else
//			{ // if (or == OrientationRequested.REVERSE_LANDSCAPE)
//				rbRevPortrait.setSelected(true);
//			}
		}
	}

	private class IconRadioButton extends JRadioButton
	{
		public IconRadioButton(String key, boolean selected, ButtonGroup bg, ActionListener al)
		{
			super(key);

			setSelected(selected);
			bg.add(this);
			addActionListener(al);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.j2db.util.JEscapeDialog#cancel()
	 */
	@Override
	protected void cancel()
	{
		setVisible(false);
	}

	/**
	 * @return
	 */
	public int getStatus()
	{
		return status;
	}


	/**
	 * mainly for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		Frame f = new Frame();
		int status = APPROVE;
		String formatString = "0;7200.0;14400.0;216.0;144.0;6696.0;14184.0;"; //$NON-NLS-1$
		while (status == APPROVE)
		{
			System.out.println(formatString);
			PageSetupDialog psd = new PageSetupDialog(f, true);
			psd.showDialog(PersistHelper.createPageFormat(formatString));
			status = psd.getStatus();
			if (status == APPROVE)
			{
				formatString = PersistHelper.createPageFormatString(psd.getPageFormat());
			}
			psd.dispose();
		}
		System.exit(0);
	}

}
