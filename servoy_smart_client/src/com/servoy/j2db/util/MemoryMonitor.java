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
package com.servoy.j2db.util;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * Tracks Memory allocated & used, displayed in graph form.
 */
public class MemoryMonitor extends JPanel
{

//    private static JCheckBox dateStampCB = new JCheckBox("Output Date Stamp");
	private Surface surf;
	private JPanel controls;
	private boolean doControls;
	private JTextField tf;

	public MemoryMonitor()
	{
		setLayout(new BorderLayout());
//        setBorder(new TitledBorder(new EtchedBorder(), "Memory Monitor"));
		add(surf = new Surface(), BorderLayout.CENTER);
//        controls = new JPanel();
//        controls.setPreferredSize(new Dimension(135,80));
//        Font font = new Font("serif", Font.PLAIN, 10);
//        JLabel label = new JLabel("Sample Rate");
//        label.setFont(font);
//        label.setForeground(Color.black);
//        controls.add(label);
//        tf = new JTextField("1000");
//        tf.setPreferredSize(new Dimension(45,20));
//        controls.add(tf);
//        controls.add(label = new JLabel("ms"));
//        label.setFont(font);
//        label.setForeground(Color.black);
//        controls.add(dateStampCB);
//        dateStampCB.setFont(font);
//        addMouseListener(new MouseAdapter() {
//            public void mouseClicked(MouseEvent e) {
//               removeAll();
//               if ((doControls = !doControls)) {
//                   surf.stop();
//                   add(controls);
//               } else {
//                   try { 
//                       surf.sleepAmount = Long.parseLong(tf.getText().trim());
//                   } catch (Exception ex) {}
//                   surf.start();
//                   add(surf);
//               }
//               validate();
//               repaint();
//            }
//        });
	}

	public void start()
	{
		surf.start();
	}

	public void stop()
	{
		surf.stop();
	}

	public class Surface extends JPanel implements Runnable
	{

		public Thread thread;
		public long sleepAmount = 1000;
		private int w, h;
		private BufferedImage bimg;
		private Graphics2D big;
		private final Font font = new Font("Times New Roman", Font.PLAIN, 11); //$NON-NLS-1$
		private final Runtime r = Runtime.getRuntime();
		private int columnInc;
		private int pts[];
		private int ptNum;
		private int ascent, descent;
		private float freeMemory, totalMemory;
		private final Rectangle graphOutlineRect = new Rectangle();
		private final Rectangle2D mfRect = new Rectangle2D.Float();
		private final Rectangle2D muRect = new Rectangle2D.Float();
		private final Line2D graphLine = new Line2D.Float();
		private final Color graphColor = new Color(46, 139, 87);
		private final Color mfColor = new Color(0, 100, 0);
		private String usedStr;


		public Surface()
		{
			setBackground(Color.black);
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 1)
					{
						System.gc();
					}
					else if (e.getClickCount() == 2)
					{
						if (thread == null) start();
						else stop();
					}
				}
			});
			setToolTipText("click will hint for garbage collect, double click will stop the updates");
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(135, 80);
		}


		@Override
		public void paint(Graphics g)
		{

			if (big == null)
			{
				return;
			}

			big.setBackground(getBackground());
			big.clearRect(0, 0, w, h);

			freeMemory = r.freeMemory();
			int between = (int)(r.totalMemory() - this.totalMemory);
			totalMemory = r.totalMemory();

			// .. Draw allocated and used strings ..
			big.setColor(Color.green);
			big.drawString(String.valueOf((int)totalMemory / 1024) + "K allocated", 4.0f, ascent + 0.5f); //$NON-NLS-1$
			usedStr = String.valueOf(((int)(totalMemory - freeMemory)) / 1024) + "K used"; //$NON-NLS-1$
			big.drawString(usedStr, 4, h - descent);

			// Calculate remaining size
			float ssH = ascent + descent;
			float remainingHeight = (h - (ssH * 2) - 0.5f);
			float blockHeight = remainingHeight / 10;
			float blockWidth = 20.0f;
			float remainingWidth = (w - blockWidth - 10);

			// .. Memory Free ..
			big.setColor(mfColor);
			int MemUsage = (int)((freeMemory / totalMemory) * 10);
			int i = 0;
			for (; i < MemUsage; i++)
			{
				mfRect.setRect(5, ssH + i * blockHeight, blockWidth, blockHeight - 1);
				big.fill(mfRect);
			}

			// .. Memory Used ..
			big.setColor(Color.green);
			for (; i < 10; i++)
			{
				muRect.setRect(5, ssH + i * blockHeight, blockWidth, blockHeight - 1);
				big.fill(muRect);
			}

			// .. Draw History Graph ..
			big.setColor(graphColor);
			int graphX = 30;
			int graphY = (int)ssH;
			int graphW = w - graphX - 5;
			int graphH = (int)remainingHeight;
			graphOutlineRect.setRect(graphX, graphY, graphW, graphH);
			big.draw(graphOutlineRect);

			int graphRow = graphH / 10;

			// .. Draw row ..
			for (int j = graphY; j <= graphH + graphY; j += graphRow)
			{
				graphLine.setLine(graphX, j, graphX + graphW, j);
				big.draw(graphLine);
			}

			// .. Draw animated column movement ..
			int graphColumn = graphW / 15;

			if (columnInc == 0)
			{
				columnInc = graphColumn;
			}

			for (int j = graphX + columnInc; j < graphW + graphX; j += graphColumn)
			{
				graphLine.setLine(j, graphY, j, graphY + graphH);
				big.draw(graphLine);
			}

			--columnInc;

			if (pts == null)
			{
				pts = new int[graphW];
				ptNum = 0;
			}
			else if (pts.length != graphW)
			{
				int tmp[] = null;
				if (ptNum < graphW)
				{
					tmp = new int[ptNum];
					System.arraycopy(pts, 0, tmp, 0, tmp.length);
				}
				else
				{
					tmp = new int[graphW];
					System.arraycopy(pts, pts.length - tmp.length, tmp, 0, tmp.length);
					ptNum = tmp.length - 2;
				}
				pts = new int[graphW];
				System.arraycopy(tmp, 0, pts, 0, tmp.length);
			}
			else
			{

				if (between != 0)
				{
					float f = between / (totalMemory - between);
					for (int j = 0; j < ptNum; j++)
					{
						pts[j] = pts[j] + (int)(pts[j] * f);
					}
				}
				big.setColor(Color.yellow);
				pts[ptNum] = (int)(graphY + graphH * (freeMemory / totalMemory));
				for (int j = graphX + graphW - ptNum, k = 0; k < ptNum; k++, j++)
				{
					if (k != 0)
					{
						if (pts[k] != pts[k - 1])
						{
							big.drawLine(j - 1, pts[k - 1], j, pts[k]);
						}
						else
						{
							big.fillRect(j, pts[k], 1, 1);
						}
					}
				}
				if (ptNum + 2 == pts.length)
				{
					// throw out oldest point
					for (int j = 1; j < ptNum; j++)
					{
						pts[j - 1] = pts[j];
					}
					--ptNum;
				}
				else
				{
					ptNum++;
				}
			}
			g.drawImage(bimg, 0, 0, this);
		}


		public void start()
		{
			thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setName("MemoryMonitor"); //$NON-NLS-1$
			thread.start();
		}


		public synchronized void stop()
		{
			thread = null;
			notify();
		}


		public void run()
		{

			Thread me = Thread.currentThread();

			while (thread == me && !isShowing() || getSize().width == 0)
			{
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
					return;
				}
			}

			while (thread == me && isShowing())
			{
				Dimension d = getSize();
				if (d.width != w || d.height != h)
				{
					w = d.width;
					h = d.height;
					bimg = (BufferedImage)createImage(w, h);
					big = bimg.createGraphics();
					big.setFont(font);
					FontMetrics fm = big.getFontMetrics(font);
					ascent = fm.getAscent();
					descent = fm.getDescent();
				}
				repaint();
				try
				{
					Thread.sleep(sleepAmount);
				}
				catch (InterruptedException e)
				{
					break;
				}
//                if (MemoryMonitor.dateStampCB.isSelected()) {
//                     //System.out.println(new Date().toString() + " " + usedStr);
//                }
			}
			thread = null;
		}
	}


//    public static void main(String s[]) {
//        final MemoryMonitor demo = new MemoryMonitor();
//        WindowListener l = new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {System.exit(0);}
//            public void windowDeiconified(WindowEvent e) { demo.surf.start(); }
//            public void windowIconified(WindowEvent e) { demo.surf.stop(); }
//        };
//        JFrame f = new JFrame("Java2D Demo - MemoryMonitor");
//        f.addWindowListener(l);
//        f.getContentPane().add("Center", demo); //$NON-NLS-1$
//        f.pack();
//        f.setSize(new Dimension(200,200));
//        f.setVisible(true);
//        demo.surf.start();
//    }
}
