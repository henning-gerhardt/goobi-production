/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package de.sub.goobi.forms;

import org.goobi.io.SafeFile;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.goobi.production.GoobiVersion;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.PluginLoader;

import org.kitodo.data.database.beans.Docket;
import org.kitodo.data.database.beans.Regelsatz;
import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.MetadataFormat;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.persistence.DocketDAO;
import org.kitodo.data.database.persistence.RegelsatzDAO;

/**
 * @author Wulf Riebensahm
 */
public class HelperForm {

	public static final String MAIN_JSF_PATH = "/newpages";
	public static final String IMAGE_PATH = "/newpages/images";
	public static final String CSS_PATH = "/css";

	public String getBuildVersion() {
		return GoobiVersion.getBuildversion();
	}

	public String getVersion() {
		return GoobiVersion.getBuildversion();
	}

	public String getApplicationLogo() {
		String logo = getServletPathWithHostAsUrl() + IMAGE_PATH + "/template/";
		logo += ConfigMain.getParameter("ApplicationLogo", "kitodo-header-logo.svg");

		return logo;
	}

	public String getApplicationHeaderBackground() {
		String logo = getServletPathWithHostAsUrl() + IMAGE_PATH + "/template/";
		logo += ConfigMain.getParameter("ApplicationHeaderBackground", "goobi_meta_verlauf.jpg");
		/* wenn ein Background angegeben wurde, dann diesen jetzt strecken */
		if (logo.length() > 0) {
			logo = "background: url(" + logo + ") repeat-x;";
		}
		return logo;
	}

	// TODO: Change the defaults
	public String getApplicationHeaderTitle() {
		String rueck = ConfigMain.getParameter("ApplicationHeaderTitle", "Goobi - Universitätsbibliothek Göttingen");
		return rueck;
	}

	public String getApplicationTitle() {
		String rueck = ConfigMain.getParameter("ApplicationTitle", "http://goobi.gdz.uni-goettingen.de");
		return rueck;
	}

	public String getApplicationTitleStyle() {
		String rueck = ConfigMain.getParameter("ApplicationTitleStyle", "font-size:17; font-family:verdana; color: black;");
		return rueck;
	}

	public String getApplicationWebsiteUrl() {
		return getServletPathAsUrl();
	}

	public String getApplicationWebsiteMsg() {
		String rueck = ConfigMain.getParameter("ApplicationWebsiteMsg", getApplicationWebsiteUrl());
		return Helper.getTranslation(rueck);
	}

	public String getApplicationHomepageMsg() {
		String rueck = ConfigMain.getParameter("ApplicationHomepageMsg", getApplicationWebsiteUrl());
		return Helper.getTranslation(rueck);
	}

	public String getApplicationTechnicalBackgroundMsg() {
		String rueck = ConfigMain.getParameter("ApplicationTechnicalBackgroundMsg", getApplicationWebsiteUrl());
		return Helper.getTranslation(rueck);
	}

	public String getApplicationImpressumMsg() {
		String rueck = ConfigMain.getParameter("ApplicationImpressumMsg", getApplicationWebsiteUrl());
		return Helper.getTranslation(rueck);
	}

	public String getApplicationIndividualHeader() {
		String rueck = ConfigMain.getParameter("ApplicationIndividualHeader", "");
		return rueck;
	}

	public boolean getAnonymized() {
		return ConfigMain.getBooleanParameter("anonymize");
	}

	public List<SelectItem> getRegelsaetze() throws DAOException {
		List<SelectItem> myPrefs = new ArrayList<SelectItem>();
		List<Regelsatz> temp = new RegelsatzDAO().search("from Regelsatz ORDER BY titel");
		for (Iterator<Regelsatz> iter = temp.iterator(); iter.hasNext();) {
			Regelsatz an = iter.next();
			myPrefs.add(new SelectItem(an, an.getTitel(), null));
		}
		return myPrefs;
	}
	
	public List<SelectItem> getDockets() {
		List<SelectItem> answer = new ArrayList<SelectItem>();
		try {
			List<Docket> temp = new DocketDAO().search("from Docket ORDER BY name");
			for (Docket d : temp) {
				answer.add(new SelectItem(d, d.getName(), null));
			}
		} catch (DAOException e) {
			
		}
		
		return answer;
	}
	

	public List<String> getFileFormats() {
		ArrayList<String> ffs = new ArrayList<String>();
		for (MetadataFormat ffh : MetadataFormat.values()) {
			if (!ffh.equals(MetadataFormat.RDF)) {
				ffs.add(ffh.getName());
			}
		}
		return ffs;
	}

	public List<String> getFileFormatsInternalOnly() {
		ArrayList<String> ffs = new ArrayList<String>();
		for (MetadataFormat ffh : MetadataFormat.values()) {
			if (ffh.isUsableForInternal()) {
				if (!ffh.equals(MetadataFormat.RDF)) {
					ffs.add(ffh.getName());
				}
			}
		}
		return ffs;
	}

	public String getServletPathAsUrl() {
		FacesContext context = FacesContext.getCurrentInstance();
		return context.getExternalContext().getRequestContextPath() + "/";
	}

	public String getServletPathWithHostAsUrl() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		String scheme = request.getScheme(); // http
		String serverName = request.getServerName(); // hostname.com
		int serverPort = request.getServerPort(); // 80
		String contextPath = request.getContextPath(); // /mywebapp
		String reqUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;
		return reqUrl;
	}

	public boolean getMessagesExist() {
		return FacesContext.getCurrentInstance().getMessages().hasNext();
	}

	public List<SelectItem> getCssFiles() {
		List<SelectItem> myList = new ArrayList<SelectItem>();

		FacesContext context = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
		String filename = session.getServletContext().getRealPath("/css") + File.separator;
		SafeFile cssDir = new SafeFile(filename);
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(".css"));
			}
		};

		String[] dateien = cssDir.list(filter);
		for (String string : dateien) {
			myList.add(new SelectItem("/css/" + string, string));
		}
		return myList;
	}

	/**
	 * method returns a valid css file, which is the suggestion unless
	 * suggestion is not available if not available default.css is returned
	 * 
	 * @param cssFileName suggested css file
	 * 
	 * @return valid css file
	 */
	public String getCssLinkIfExists(String cssFileName) {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
		String filename = session.getServletContext().getRealPath(CSS_PATH) + File.separator;
		SafeFile cssDir = new SafeFile(filename);
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.endsWith(".css"));
			}
		};

		String[] dateien = cssDir.list(filter);
		for (String string : dateien) {
			if ((CSS_PATH + "/" + string).equals(cssFileName)) {
				return cssFileName;
			}
		}
		return CSS_PATH + "/default.css";
	}

	public TimeZone getTimeZone() {
		return TimeZone.getDefault();
	}

	public String getLogoUrl() {
		return getServletPathWithHostAsUrl() + "/newpages/images/template/kitodo-homepage-logo.svg";
	}

	public boolean getMassImportAllowed() {
		boolean value = false;
		if (ConfigMain.getBooleanParameter("massImportAllowed", false)) {
			return !PluginLoader.getPluginList(PluginType.Import).isEmpty();
		}
		return value;
	}

	public boolean getIsIE() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		if (request.getHeader("User-Agent").contains("MSIE")) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getUserAgent() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		
		return request.getHeader("User-Agent"); 
	}

	/**
	 * Returning value of configuration parameter withUserStepDoneSearch.
	 * Used for enabling/disabling search for done steps by user.
	 *
	 * @return boolean
	 */
	public boolean getUserStepDoneSearchEnabled() {
		return ConfigMain.getBooleanParameter("withUserStepDoneSearch");
	}
}
