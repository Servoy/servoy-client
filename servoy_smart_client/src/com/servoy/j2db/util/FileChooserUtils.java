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


import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.servoy.j2db.Messages;
import com.servoy.j2db.util.gui.FileNameSuggestionFileChooser;

//This class is a copy of the nl.profdata.syne.util.Utils when more classes are used bundle lib instead of copy
/**
 * <br>
 * <br>
 * Normal Use: static methods <br>
 * <br>
 * 
 * @author jblok
 */
public class FileChooserUtils
{
	private static File lastDir = null;

	/**
	 * Change the passed class name to its corresponding file name. E.G. change &quot;Utilities&quot; to &quot;Utilities.class&quot;.
	 * 
	 * @param name Class name to be changed.
	 * 
	 * @throws IllegalArgumentException If a null <TT>name</TT> passed.
	 */
	public static String changeClassNameToFileName(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("Class Name == null"); //$NON-NLS-1$
		}
		return name.replace('.', '/').concat(".class"); //$NON-NLS-1$
	}

	/**
	 * Change the passed file name to its corresponding class name. E.G. change &quot;Utilities.class&quot; to &quot;Utilities&quot;.
	 * 
	 * @param name Class name to be changed. If this does not represent a Java class then <TT>null</TT> is returned.
	 * 
	 * @throws IllegalArgumentException If a null <TT>name</TT> passed.
	 */
	public static String changeFileNameToClassName(String name)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("File Name == null"); //$NON-NLS-1$
		}
		String className = null;
		if (name.toLowerCase().endsWith(".class")) { //$NON-NLS-1$
			className = name.replace('/', '.');
			className = className.replace('\\', '.');
			className = className.substring(0, className.length() - 6);
		}
		return className;
	}

/*
 * _____________________________________________________________ Declaration of attributes
 */


/*
 * _____________________________________________________________ Declaration and definition of constructors
 */
	private FileChooserUtils()
	{
	}

	public static File getAWriteFile(Component parent, String fileName, boolean doFileSuggestion)
	{
		return getAWriteFile(parent, new File(fileName), doFileSuggestion);
	}

	public static File getAWriteFile(Component parent, File file, boolean doFileSuggestion)
	{
		return getAWriteFile(parent, file, doFileSuggestion, null);
	}

	public static File getAWriteFile(Component parent, File file, boolean doFileSuggestion, String title)
	{
		JFileChooser fc = null;
		if (file != null)
		{
			if (file.isDirectory())
			{
				fc = new JFileChooser(file);
				lastDir = file;
			}
			else if (doFileSuggestion)
			{
				fc = new FileNameSuggestionFileChooser(file.getParentFile());
				lastDir = file.getParentFile();
				((FileNameSuggestionFileChooser)fc).suggestFileName(file.getName());
			}
		}
		else
		{
			fc = new JFileChooser(lastDir);
		}

		if (fc != null)
		{
			if (title == null)
			{
				fc.setDialogTitle(Messages.getString("servoy.filechooser.title")); //$NON-NLS-1$
			}
			else
			{
				fc.setDialogTitle(title);
			}
			fc.setApproveButtonText(Messages.getString("servoy.filechooser.button.title")); //$NON-NLS-1$);
			int returnVal = fc.showSaveDialog(parent);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				file = fc.getSelectedFile();
				lastDir = fc.getCurrentDirectory();
			}
			else
			{
				file = null;
			}

		}

		return file;
	}

	public static File getAReadFile(Component parent, String fileName, int selectionMode, String[] filter)
	{
		return getAReadFile(parent, new File(fileName), selectionMode, filter);
	}

	public static File getAReadFile(Component parent, File file, int selectionMode, String[] filter)
	{
		return getAReadFile(parent, file, selectionMode, filter, null);
	}

	public static File getAReadFile(Component parent, File file, int selectionMode, String[] filter, String title)
	{
		JFileChooser fc = getFileChooser(file, selectionMode, filter, title);

		if (fc != null)
		{
			fc.setMultiSelectionEnabled(false);
			int returnVal = fc.showOpenDialog(parent);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				file = fc.getSelectedFile();
				lastDir = fc.getCurrentDirectory();
			}
			else
			{
				file = null;
			}
		}
		return file;
	}

	public static File[] getFiles(Component parent, File file, int selectionMode, String[] filter)
	{
		return getFiles(parent, file, selectionMode, filter, null);
	}

	public static File[] getFiles(Component parent, File file, int selectionMode, String[] filter, String title)
	{
		JFileChooser fc = getFileChooser(file, selectionMode, filter, title);

		File[] files = null;
		if (fc != null)
		{
			fc.setMultiSelectionEnabled(true);
			int returnVal = fc.showOpenDialog(parent);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				files = fc.getSelectedFiles();
				lastDir = fc.getCurrentDirectory();
			}
		}
		return files;
	}

	/**
	 * @param file
	 * @return
	 */
	private static JFileChooser getFileChooser(File file, int selectionMode, final String[] filter)
	{
		return getFileChooser(file, selectionMode, filter, null);
	}

	private static JFileChooser getFileChooser(File file, int selectionMode, String[] filter, String title)
	{
		JFileChooser fc = null;
		if (file != null)
		{
			if (!file.exists())
			{
				fc = new JFileChooser(lastDir);
			}
			else if (file.isDirectory())
			{
				fc = new JFileChooser(file);
			}
		}
		else
		{
			fc = new JFileChooser(lastDir);
		}
		if (fc != null)
		{
			if (title == null)
			{
				fc.setDialogTitle(Messages.getString("servoy.filechooser.title")); //$NON-NLS-1$
			}
			else
			{
				fc.setDialogTitle(title);
			}
			fc.setApproveButtonText(Messages.getString("servoy.filechooser.button.title")); //$NON-NLS-1$);
			fc.setFileSelectionMode(selectionMode);

			if (filter != null && filter.length > 0)
			{
				List<String> filterList = new ArrayList<String>();
				filterList.addAll(Arrays.asList(filter));
				if (!filterList.contains("*"))//$NON-NLS-1$
				{
					fc.setAcceptAllFileFilterUsed(false);
				}
				else
				{
					fc.setAcceptAllFileFilterUsed(true);
					filterList.remove("*"); //$NON-NLS-1$
					filter = filterList.toArray(new String[] { });
				}
				if (filter.length > 0)
				{
					final String[] finalFilter = filter;
					fc.setFileFilter(new FileFilter()
					{

						@Override
						public String getDescription()
						{
							return finalFilter[0];
						}

						@Override
						public boolean accept(File f)
						{
							for (String element : finalFilter)
							{
								if (f.isDirectory() || f.getName().toLowerCase().endsWith(element))
								{
									return true;
								}
							}
							return false;
						}
					});
				}
			}
		}
		return fc;
	}

	public static byte[] paintingReadFile(Executor threadPool, IUIBlocker application, final File file, final long size) throws Exception
	{
		if (file != null)
		{
			SwingHelper.BackgroundRunner runner = new SwingHelper.BackgroundRunner()
			{
				@Override
				public Object doWork() throws Exception
				{
					return readFile(file, size);
				}
			};
			if (file.length() < 1000000)
			{
				return (byte[])runner.doWork();
			}
			else
			{
				return (byte[])SwingHelper.getSwingHelper(threadPool).paintingWait(application,
					Messages.getString("servoy.servoy.readingfile.msg"), 2000, runner); //$NON-NLS-1$
			}
		}
		return null;
	}

	public static byte[] paintingReadFile(Executor threadPool, IUIBlocker application, final File file) throws Exception
	{
		return paintingReadFile(threadPool, application, file, -1);
	}

	/**
	 * @param file
	 * @return the file contents
	 * @throws Exception
	 * @throws IOException
	 */
	public static byte[] readFile(File file) throws Exception, IOException
	{
		return Utils.getFileContent(file);
//		FileInputStream fis = null;
//		try
//		{
//			int length = (int)file.length();
//			fis = new FileInputStream(file);
//			FileChannel fc = fis.getChannel();
//			ByteBuffer bb =  ByteBuffer.allocate(length);
//			fc.read(bb);
//			bb.rewind();
//			byte[] bytes = null;
//			if(bb.hasArray())
//			{
//				bytes = bb.array();
//			}
//			else
//			{
//				bytes = new byte[length];
//				bb.get(bytes, 0, length);
//			}
//			return bytes;
//		}
//		catch (Exception e)
//		{
//			throw e;
//		}
//		finally
//		{
//			if (fis != null) fis.close();
//		}
	}

	public static byte[] readFile(File file, long size) throws Exception, IOException
	{
		return Utils.readFile(file, size);
	}

	/**
	 * @param file
	 * @return the file contents
	 * @throws Exception
	 * @throws IOException
	 */
	public static String readTxtFile(File file) throws Exception, IOException
	{
		return Utils.getTXTFileContent(file);
//		
//		FileInputStream fis = null;
//		try
//		{
//			int length = (int)file.length();
//			fis = new FileInputStream(file);
//			FileChannel fc = fis.getChannel();
//			ByteBuffer bb =  ByteBuffer.allocate(length);
//			fc.read(bb);
//			bb.rewind();
//			CharBuffer cb = Charset.defaultCharset().decode(bb);
//			return cb.toString();
//		}
//		catch (Exception e)
//		{
//			throw e;
//		}
//		finally
//		{
//			if (fis != null) fis.close();
//		}
	}

}
