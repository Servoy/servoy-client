package com.servoy.j2db.server.ngclient;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.ngclient.component.WebComponentSpecProvider;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.template.IndexTemplateGenerator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;

@WebFilter(urlPatterns = { "/solutions/*" })
@SuppressWarnings("nls")
public class TemplateGeneratorFilter implements Filter
{
	public static final String SOLUTIONS_PATH = "solutions/";
	public static final String FORMS_PATH = "forms/";

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		try
		{
			if (Boolean.valueOf(Settings.getInstance().getProperty("servoy.internal.reloadSpecsAllTheTime", "false")).booleanValue()) WebComponentSpecProvider.reload();

			HttpServletRequest request = (HttpServletRequest)servletRequest;
			String uri = request.getRequestURI();
			if (uri != null && (uri.endsWith(".html") || uri.endsWith(".js")))
			{
				int solutionIndex = uri.indexOf(SOLUTIONS_PATH);
				int formIndex = uri.indexOf(FORMS_PATH);
				if (solutionIndex > 0)
				{
					String solutionName = uri.substring(solutionIndex + SOLUTIONS_PATH.length(), uri.indexOf("/", solutionIndex + SOLUTIONS_PATH.length() + 1));
					FlattenedSolution fs = null;
					try
					{
						IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
						fs = new FlattenedSolution((SolutionMetaData)ApplicationServerRegistry.get().getLocalRepository().getRootObjectMetaData(solutionName,
							IRepository.SOLUTIONS), new AbstractActiveSolutionHandler(as)
						{

							@Override
							public IRepository getRepository()
							{
								return ApplicationServerRegistry.get().getLocalRepository();
							}

						});
					}
					catch (RepositoryException e)
					{
						Debug.error(e);
					}
					if (fs != null && formIndex > 0)
					{
						String formName = uri.substring(formIndex + FORMS_PATH.length());
						formName = formName.replace(".html", "");
						formName = formName.replace(".js", "");
						Form f = fs.getForm(formName);
						Form form = (f != null) ? fs.getFlattenedForm(f) : null;
						if (form != null)
						{
							String view = form.getView() == IFormConstants.VIEW_TYPE_TABLE || form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED
								? "tableview" : "recordview";
							String output = uri.endsWith(".html") ? "html" : "js";
							((HttpServletResponse)servletResponse).setContentType("text/" + (output == "html" ? "html" : "javascript"));
							new FormTemplateGenerator(fs).generate(form, "form_" + view + "_" + output + ".ftl", servletResponse.getWriter());
							return;
						}
					}
					else if (uri.endsWith("index.html"))
					{
						((HttpServletResponse)servletResponse).setContentType("text/html");
						new IndexTemplateGenerator(fs, request.getContextPath()).generate(fs, "index.ftl", servletResponse.getWriter());
						return;
					}
				}
			}

			filterChain.doFilter(servletRequest, servletResponse);
		}
		catch (RuntimeException | Error e)
		{
			Debug.error(e);
			throw e;
		}
	}

	@Override
	public void init(final FilterConfig fc) throws ServletException
	{
		WebComponentSpecProvider.init(fc.getServletContext());
	}

	@Override
	public void destroy()
	{
	}
}
