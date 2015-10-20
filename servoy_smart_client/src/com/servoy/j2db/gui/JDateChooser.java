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
package com.servoy.j2db.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;

import com.servoy.j2db.Messages;
import com.servoy.j2db.smart.cmd.MnemonicCheckAction;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.gui.JEscapeDialog;


/**
 * JDateChooser is a simple Date choosing component with similar functionality to JFileChooser and JColorChooser. It can be used as a component, to be inserted
 * into a client layout, or can display it's own Dialog through use of the {@link #showDialog(Component, String) showDialog} method.
 * <p>
 * JDateChooser can be initialized to the current date using the no argument constructor, or initialized to a predefined date by passing an instance of Calendar
 * to the constructor.
 * <p>
 * Using the JDateChooser dialog works in a similar manner to JFileChooser or JColorChooser. The {@link #showDialog(Component, String) showDialog} method
 * returns an int that equates to the public variables ACCEPT_OPTION, CANCEL_OPTION or ERROR_OPTION.
 * <p>
 * <tt>
 * JDateChooser chooser = new JDateChooser();<br>
 * if (chooser.showDialog(this, "Select a date...") == JDateChooser.ACCEPT_OPTION) {<br>
 * &nbsp;&nbsp;Calendar selectedDate = chooser.getSelectedDate();<br>
 * &nbsp;&nbsp;// process date here...<br>
 * }<p>
 * To use JDateChooser as a component within a GUI, users should subclass
 * JDateChooser and override the {@link #acceptSelection() acceptSelection} and
 * {@link #cancelSelection() cancelSelection} methods to process the 
 * corresponding user selection.<p>
 * The current date can be retrieved by calling {@link #getSelectedDate() getSelectedDate}
 * method.
 */

public class JDateChooser extends JEscapeDialog implements ActionListener, DaySelectionListener
{

	/**
	 * Value returned by {@link #showDialog(Component, String) showDialog} upon an error.
	 */
	public static final int ERROR_OPTION = 0;
	/**
	 * Value returned by {@link #showDialog(Component, String) showDialog} upon pressing the "okay" button.
	 */
	public static final int ACCEPT_OPTION = 2;
	/**
	 * Value returned by {@link #showDialog(Component, String) showDialog} upon pressing the "cancel" button.
	 */
	public static final int CANCEL_OPTION = 4;
	private int currentDay;
	private int currentMonth;
	private JLabel dateText;
	private Calendar calendar;
	private Calendar todayCalender;
	private JButton previousYear;
	private JButton previousMonth;
	private JButton nextMonth;
	private JButton nextYear;
	private JButton okay;
	private JButton cancel;
	private int returnValue;
	private JPanel days;
	private DayButton[] array;
	private Color background;

	private SimpleDateFormat sdf;

	private String defaultPattern;
	private JFormattedTextField timeField;

	/**
	 * This constructor creates a new instance of JDateChooser initialized to the current date.
	 */
	public JDateChooser(JFrame parent, String title, String defaultPattern)
	{
		this(parent, title, defaultPattern, Calendar.getInstance());
	}

	/**
	 * Creates a new instance of JDateChooser initialized to the given Calendar.
	 */
	public JDateChooser(JFrame parent, String title, String defaultPattern, Calendar c)
	{
		super(parent, title, true);
		init(defaultPattern, c);
	}

	/**
	 * This constructor creates a new instance of JDateChooser initialized to the current date.
	 */
	public JDateChooser(JDialog parent, String title, String defaultPattern)
	{
		this(parent, title, defaultPattern, Calendar.getInstance());
	}

	/**
	 * Creates a new instance of JDateChooser initialized to the given Calendar.
	 */
	public JDateChooser(JDialog parent, String title, String defaultPattern, Calendar c)
	{
		super(parent, title, true);
		init(defaultPattern, c);
	}

	private void init(String defaultPattern, Calendar c)
	{
		if (defaultPattern == null) throw new NullPointerException("DefaultPattern can't be null"); //$NON-NLS-1$

		this.calendar = c;
		this.defaultPattern = defaultPattern;
		this.todayCalender = (Calendar)c.clone();
		this.calendar.setLenient(true);
		sdf = new SimpleDateFormat();
		array = new DayButton[31];

		setup();
		loadBounds("JDateChooser"); //$NON-NLS-1$
	}

	private void setup()
	{
		MouseListener ml = new ButtonMouseListener();
		for (int i = 0; i < 31; i++)
		{
			array[i] = new DayButton(i + 1);
			array[i].addDaySelectionListener(this);
			array[i].addMouseListener(ml);
		}
		background = array[0].getBackground();
		GridBagLayout g = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel header = new JPanel(g);
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(2, 0, 2, 0);
		previousYear = (JButton)header.add(new JButton("<<")); //$NON-NLS-1$
		previousYear.addActionListener(this);
		previousYear.setToolTipText(Messages.getString("servoy.datechooser.previousyear")); //$NON-NLS-1$
		g.setConstraints(previousYear, c);
		previousMonth = (JButton)header.add(new JButton("<")); //$NON-NLS-1$
		previousMonth.addActionListener(this);
		previousMonth.setToolTipText(Messages.getString("servoy.datechooser.previousmonth")); //$NON-NLS-1$
		c.gridx++;
		g.setConstraints(previousMonth, c);

		JPanel labelTime = new JPanel(new BorderLayout(5, 5));
		labelTime.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		dateText = new JLabel("", SwingConstants.CENTER); //$NON-NLS-1$
//		dateText.setBorder(BorderFactory.createEmptyBorder(0, 12,0, 12));
		timeField = new JFormattedTextField();
		labelTime.add(dateText, BorderLayout.CENTER);
		labelTime.add(timeField, BorderLayout.EAST);

		header.add(labelTime);
//		dateText.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		c.gridx++;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		g.setConstraints(dateText, c);
		nextMonth = (JButton)header.add(new JButton(">")); //$NON-NLS-1$
		nextMonth.addActionListener(this);
		nextMonth.setToolTipText(Messages.getString("servoy.datechooser.nextmonth")); //$NON-NLS-1$
		c.gridx++;
		c.weightx = 0.0;
		c.fill = GridBagConstraints.NONE;
		g.setConstraints(nextMonth, c);
		nextYear = (JButton)header.add(new JButton(">>")); //$NON-NLS-1$
		nextYear.addActionListener(this);
		nextYear.setToolTipText(Messages.getString("servoy.datechooser.nextyear")); //$NON-NLS-1$
		c.gridx++;
		g.setConstraints(nextYear, c);

		JButton today = new JButton(Messages.getString("servoy.datechooser.today")); //$NON-NLS-1$
		today.setActionCommand("today"); //$NON-NLS-1$
		today.addActionListener(this);


		okay = new JButton(Messages.getString("servoy.button.ok")); //$NON-NLS-1$
		okay.addActionListener(this);
		cancel = new JButton(Messages.getString("servoy.button.cancel")); //$NON-NLS-1$
		cancel.addActionListener(this);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		buttonPane.add(today);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okay);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(cancel);

		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		updateCalendar(calendar);

		panel.add("North", header); //$NON-NLS-1$
		panel.add("Center", days); //$NON-NLS-1$
		panel.add("South", buttonPane); //$NON-NLS-1$
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(panel, BorderLayout.CENTER);

		getRootPane().setDefaultButton(okay);

		InputMap im = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = getRootPane().getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down"); //$NON-NLS-1$
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up"); //$NON-NLS-1$
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left"); //$NON-NLS-1$
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right"); //$NON-NLS-1$

		am.put("left", new ArrowKeyAction(-1)); //$NON-NLS-1$
		am.put("right", new ArrowKeyAction(1)); //$NON-NLS-1$
		am.put("down", new ArrowKeyAction(7)); //$NON-NLS-1$
		am.put("up", new ArrowKeyAction(-7)); //$NON-NLS-1$

	}

	private JPanel panel;
	private String timePattern;

	public void updateCalendar(Calendar c)
	{
		if (days != null)
		{
			days.removeAll();
		}
		else
		{
			days = new JPanel(new GridLayout(7, 8));
			panel.add(days, BorderLayout.CENTER);
			days.setBorder(BorderFactory.createTitledBorder(Messages.getString("servoy.datechooser.label.monthoverview"))); //$NON-NLS-1$
		}
		calendar = c;
		currentDay = calendar.get(Calendar.DAY_OF_MONTH);
		currentMonth = calendar.get(Calendar.MONTH);
		Calendar setup = (Calendar)calendar.clone();
		setup.set(Calendar.DAY_OF_WEEK, setup.getFirstDayOfWeek());
		days.add(new JLabel(Messages.getString("servoy.datechooser.label.week"), SwingConstants.CENTER));
		int lastLayoutPosition = 0;
		for (int i = 0; i < 7; i++)
		{
			int dayInt = setup.get(Calendar.DAY_OF_WEEK);
			if (dayInt == Calendar.MONDAY) days.add(new JLabel(Messages.getString("servoy.datechooser.label.monday"), SwingConstants.CENTER)); //$NON-NLS-1$
			if (dayInt == Calendar.TUESDAY) days.add(new JLabel(Messages.getString("servoy.datechooser.label.tuesday"), SwingConstants.CENTER)); //$NON-NLS-1$
			if (dayInt == Calendar.WEDNESDAY) days.add(new JLabel(Messages.getString("servoy.datechooser.label.wednesday"), SwingConstants.CENTER)); //$NON-NLS-1$
			if (dayInt == Calendar.THURSDAY) days.add(new JLabel(Messages.getString("servoy.datechooser.label.thursday"), SwingConstants.CENTER)); //$NON-NLS-1$
			if (dayInt == Calendar.FRIDAY) days.add(new JLabel(Messages.getString("servoy.datechooser.label.friday"), SwingConstants.CENTER)); //$NON-NLS-1$
			if (dayInt == Calendar.SATURDAY) days.add(new JLabel(Messages.getString("servoy.datechooser.label.saturday"), SwingConstants.CENTER)); //$NON-NLS-1$
			if (dayInt == Calendar.SUNDAY) days.add(new JLabel(Messages.getString("servoy.datechooser.label.sunday"), SwingConstants.CENTER)); //$NON-NLS-1$
			setup.roll(Calendar.DAY_OF_WEEK, true);
			lastLayoutPosition++;
		}
		setup = (Calendar)calendar.clone();
		setup.set(Calendar.DAY_OF_MONTH, 1);
		days.add(new JLabel(String.valueOf(setup.get(Calendar.WEEK_OF_YEAR)), SwingConstants.CENTER));
		int first = setup.get(Calendar.DAY_OF_WEEK) + Calendar.SUNDAY - setup.getFirstDayOfWeek();
		if (first <= 0) first += 7;
		for (int i = 0; i < (first - 1); i++)
		{
			days.add(new JLabel("")); //$NON-NLS-1$
			lastLayoutPosition++;
		}

		setup.set(Calendar.DAY_OF_MONTH, 1);
		for (int i = 0; i < setup.getMaximum(Calendar.DAY_OF_MONTH); i++) /* Actual */
		{
			if (todayCalender.get(Calendar.MONTH) == setup.get(Calendar.MONTH) && todayCalender.get(Calendar.YEAR) == setup.get(Calendar.YEAR) &&
				todayCalender.get(Calendar.DATE) == setup.get(Calendar.DAY_OF_MONTH))
			{
				array[i].setBackground(UIManager.getColor("Table.selectionBackground")); //$NON-NLS-1$
				array[i].setForeground(UIManager.getColor("Table.selectionForeground")); //$NON-NLS-1$
				array[i].setToolTipText(Messages.getString("servoy.datechooser.today")); //$NON-NLS-1$
			}
			else
			{
				array[i].setBackground(background);
				array[i].setToolTipText(null);
			}

			days.add(array[i]);
			setup.roll(Calendar.DAY_OF_MONTH, true);
			lastLayoutPosition++;
			if (setup.get(Calendar.DAY_OF_MONTH) == 1)
			{
				break;
			}
			if ((first + i) % 7 == 0) days.add(new JLabel(String.valueOf(setup.get(Calendar.WEEK_OF_YEAR)), SwingConstants.CENTER));
		}
		for (int i = lastLayoutPosition; i < 49; i++)
			days.add(new JLabel("")); //$NON-NLS-1$

		days.invalidate();
		validate();
		days.repaint();

		setup = null;
		updateLabel();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				array[currentDay - 1].requestFocus();
			}
		});
	}

	private void updateLabel()
	{
		String text = sdf.format(calendar.getTime());

		dateText.setText(text);
		setTitle(Messages.getString("servoy.datechooser.title", new Object[] { text })); //$NON-NLS-1$

	}

	/**
	 * Returns the currently selected Date in the form of a java.util.Calendar object. Typically called adter receipt of an {@link #ACCEPT_OPTION ACCEPT_OPTION}
	 * (using the {@link #showDialog(Component, String) showDialog} method) or within the {@link #acceptSelection() acceptSelection} method (using the
	 * JDateChooser as a component.)
	 * <p>
	 * 
	 * @return java.util.Calendar The selected date in the form of a Calendar object.
	 */
	public Calendar getSelectedDate()
	{
		return calendar;
	}

	/**
	 * Pops up a Date chooser dialog with the supplied <i>title</i>, centered about the component <i>parent</i>.
	 * 
	 * @return int An integer that equates to the static variables <i>ERROR_OPTION</i>, <i>ACCEPT_OPTION</i> or <i>CANCEL_OPTION</i>.
	 */
//	public int showDialog(JFrame parent, String title) {
//		returnValue = ERROR_OPTION;
////		Frame frame = parent instanceof Frame ? (Frame) parent : (Frame)SwingUtilities.getAncestorOfClass(Frame.class, parent);
//
//		dialog = new JEscapeDialog(parent, title, true);
//		dialog.getContentPane().add("Center", this);
//		dialog.pack();
//		dialog.setLocationRelativeTo(parent);
//		dialog.getRootPane().setDefaultButton(okay);
//		dialog.show();
//		return returnValue;
//	}
	public Date format(Date date, String pattern)
	{
		sdf.applyPattern(getDisplayPattern(pattern));
		try
		{
			return sdf.parse(sdf.format(date));
		}
		catch (ParseException e)
		{
		}
		return date;
	}

	public int showDialog(String pattern)
	{
		String pat = getDisplayPattern(pattern);
		int tmp = pat.toLowerCase().indexOf("hh:mm"); //$NON-NLS-1$
		if (tmp == -1)
		{
			tmp = pat.toLowerCase().indexOf("hhmm"); //$NON-NLS-1$
		}
		if (tmp != -1)
		{
			int space = pat.indexOf(" ", tmp); //$NON-NLS-1$
			if (space == -1) space = pat.length();

			timePattern = pat.substring(tmp, space);
			pat = pat.substring(0, tmp) + pat.substring(space);

			sdf.applyPattern(timePattern);

			StringBuilder sb = new StringBuilder(timePattern.length());
			for (int i = 0; i < timePattern.length(); i++)
			{
				char ch = timePattern.charAt(i);
				if (Character.isLetter(ch))
				{
					sb.append('#');
				}
				else
				{
					sb.append(ch);
				}

			}
			DefaultFormatter defaultFormatter = new DefaultFormatter();
			DefaultFormatter maskFormatter;
			try
			{
				maskFormatter = new MaskFormatter(sb.toString());
				maskFormatter.setOverwriteMode(true);
			}
			catch (ParseException e)
			{
				Debug.error(e);
				maskFormatter = defaultFormatter;
			}
			timeField.setFormatterFactory(new DefaultFormatterFactory(defaultFormatter, defaultFormatter, maskFormatter));
			timeField.setVisible(true);
			timeField.setValue(sdf.format(calendar.getTime()));
		}
		else
		{
			timeField.setVisible(false);
			timePattern = null;
		}
		sdf.applyPattern(pat.trim());
		updateLabel();
		returnValue = ERROR_OPTION;
		setVisible(true);
		return returnValue;
	}

	private String getDisplayPattern(String pattern)
	{
		int index;
		if (pattern == null)
		{
			return defaultPattern;
		}
		else if ((index = pattern.indexOf("|")) != -1) //$NON-NLS-1$
		{
			return pattern.substring(0, index);
		}
		return pattern;
	}

	/**
	 * This method is called when the user presses the "okay" button. Users must subclass JDateChooser and override this method to use JDateChooser as a
	 * Component and receive accept selections by the user.
	 */
	public void acceptSelection()
	{
		if (timePattern != null)
		{
			String pattern = sdf.toPattern();
			sdf.applyPattern(timePattern);
			try
			{
				Date d = sdf.parse(timeField.getText());
				Calendar cal = Calendar.getInstance();
				cal.setTime(d);
				calendar.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
				calendar.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
				calendar.set(Calendar.SECOND, cal.get(Calendar.SECOND));
				calendar.set(Calendar.MILLISECOND, cal.get(Calendar.MILLISECOND));
			}
			catch (ParseException e)
			{
				JOptionPane.showMessageDialog(this, Messages.getString("servoy.datechooser.message.timeinvalid")); //$NON-NLS-1$
				return;
			}
			finally
			{
				sdf.applyPattern(pattern);
			}
		}
		setVisible(false);
	}

	/**
	 * This method is called when the user presses the "cancel" button. Users must subclass JDateChooser and override this method to use JDateChooser as a
	 * Component and receive cancel selections by the user.
	 */
	@Override
	public void cancel()
	{
		setVisible(false);
	}

	/**
	 * Used to process events from the previous month, previous year, next month, next year, okay and cancel buttons. Users should call
	 * super.actionPerformed(ActionEvent) if overriding this method.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if ("today".equals(e.getActionCommand())) //$NON-NLS-1$
		{
			Calendar today = Calendar.getInstance();
			today.setTime(new Date());
			calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
			calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
			calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
			updateCalendar(calendar);
		}
		else if (e.getSource() == okay)
		{
			returnValue = ACCEPT_OPTION;
			acceptSelection();
		}
		else if (e.getSource() == cancel)
		{
			returnValue = CANCEL_OPTION;
			cancel();
		}
		else if (e.getSource() == previousYear)
		{
			calendar.roll(Calendar.YEAR, false);//-1
			updateCalendar(calendar);
		}
		else if (e.getSource() == previousMonth)
		{
			int month = calendar.get(Calendar.MONTH);
			if (month == 0)
			{
				int year = calendar.get(Calendar.YEAR);
				calendar.roll(Calendar.MONTH, false);//-1
				calendar.set(Calendar.YEAR, year - 1);
			}
			else
			{
				calendar.roll(Calendar.MONTH, false);//-1
			}
			updateCalendar(calendar);
		}
		else if (e.getSource() == nextMonth)
		{
			int month = calendar.get(Calendar.MONTH);
			if (month == 11)
			{
				int year = calendar.get(Calendar.YEAR);
				calendar.roll(Calendar.MONTH, true);//1
				calendar.set(Calendar.YEAR, year + 1);
			}
			else
			{
				calendar.roll(Calendar.MONTH, true);//1
			}
			updateCalendar(calendar);
		}
		else if (e.getSource() == nextYear)
		{
			calendar.roll(Calendar.YEAR, true);//1
			updateCalendar(calendar);
		}
	}

	/**
	 * Used to process day selection events from the user. This method resets resets the Calendar object to the selected day. Subclasses should make a call to
	 * super.daySelected() if overriding this method.
	 */
	public void daySelected(int d)
	{
		calendar.set(Calendar.DAY_OF_MONTH, d);
		updateLabel();
		currentDay = d;
		returnValue = ACCEPT_OPTION;
//		acceptSelection();
	}

	class ArrowKeyAction extends MnemonicCheckAction
	{
		int ndays;

		ArrowKeyAction(int ndays)
		{
			this.ndays = ndays;
		}

		public void actionPerformed(ActionEvent e)
		{
			final int oldDay = currentDay;
			calendar.roll(Calendar.DAY_OF_YEAR, ndays);//-1
			currentDay = calendar.get(Calendar.DAY_OF_MONTH);
			int month = calendar.get(Calendar.MONTH);
			if (month != currentMonth)
			{
				updateCalendar(calendar);
				SwingUtilities.invokeLater(new Runnable()
				{
					/**
					 * @see java.lang.Runnable#run()
					 */
					public void run()
					{
						JDateChooser.this.days.invalidate();
						JDateChooser.this.days.getParent().validate();
						array[oldDay - 1].repaint();
					}
				});
			}
			else
			{
				array[currentDay - 1].requestFocus();
				updateLabel();
			}
		}
	}

	public class ButtonMouseListener extends MouseAdapter
	{
		/**
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseReleased(MouseEvent e)
		{
			if (e.getClickCount() == 2)
			{
				acceptSelection();
			}
		}
	}

}

interface DaySelectionListener
{
	public void daySelected(int day);
}

class DayButton extends JButton implements ActionListener
{

	private final int day;
	private Vector listeners;

	public DayButton(int d)
	{
		super((new Integer(d)).toString());
		this.day = d;
		setMargin(new Insets(0, 0, 0, 0));
		setPreferredSize(new Dimension(40, 22));
		addActionListener(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (listeners != null)
		{
			for (int i = 0; i < listeners.size(); i++)
			{
				((DaySelectionListener)listeners.elementAt(i)).daySelected(day);
			}
		}
	}

	public void addDaySelectionListener(DaySelectionListener l)
	{
		if (listeners == null) listeners = new Vector(1, 1);
		listeners.addElement(l);
	}

	public void removeDaySelectionListener(DaySelectionListener l)
	{
		if (listeners != null) listeners.removeElement(l);
	}

	public void removeAllListeners()
	{
		listeners = new Vector(1, 1);
	}


}