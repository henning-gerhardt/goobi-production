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

package de.sub.goobi.export.download;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.goobi.io.SafeFile;

import org.kitodo.data.database.beans.Benutzer;
import org.kitodo.data.database.beans.ProjectFileGroup;
import org.kitodo.data.database.beans.Projekt;
import org.kitodo.data.database.beans.Prozess;
import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.config.ConfigProjects;
import de.sub.goobi.export.dms.ExportDms;
import de.sub.goobi.export.dms.ExportDms_CorrectRusdml;
import de.sub.goobi.forms.LoginForm;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.VariableReplacer;
import org.kitodo.data.database.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.InvalidImagesException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.helper.exceptions.UghHelperException;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import de.sub.goobi.metadaten.copier.CopierData;
import de.sub.goobi.metadaten.copier.DataCopier;
import ugh.dl.ContentFile;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.dl.VirtualFileGroup;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsModsImportExport;

public class ExportMets {
	protected Helper help = new Helper();
	protected Prefs myPrefs;

	protected static final Logger myLogger = Logger.getLogger(ExportMets.class);

	/**
	 * DMS-Export in das Benutzer-Homeverzeichnis
	 * 
	 * @param myProzess
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws DAOException
	 * @throws SwapException
	 * @throws ReadException
	 * @throws UghHelperException
	 * @throws ExportFileException
	 * @throws MetadataTypeNotAllowedException
	 * @throws WriteException
	 * @throws PreferencesException
	 * @throws DocStructHasNoTypeException
	 * @throws TypeNotAllowedForParentException
	 */
	public boolean startExport(Prozess myProzess) throws IOException, InterruptedException, DocStructHasNoTypeException, PreferencesException,
			WriteException, MetadataTypeNotAllowedException, ExportFileException, UghHelperException, ReadException, SwapException, DAOException,
			TypeNotAllowedForParentException {
		LoginForm login = (LoginForm) Helper.getManagedBeanValue("#{LoginForm}");
		String benutzerHome = "";
		if (login != null) {
			benutzerHome = login.getMyBenutzer().getHomeDir();
		}
		return startExport(myProzess, benutzerHome);
	}

	/**
	 * DMS-Export an eine gewünschte Stelle
	 * 
	 * @param myProzess
	 * @param inZielVerzeichnis
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws PreferencesException
	 * @throws WriteException
	 * @throws UghHelperException
	 * @throws ExportFileException
	 * @throws MetadataTypeNotAllowedException
	 * @throws DocStructHasNoTypeException
	 * @throws DAOException
	 * @throws SwapException
	 * @throws ReadException
	 * @throws TypeNotAllowedForParentException
	 */
	public boolean startExport(Prozess myProzess, String inZielVerzeichnis) throws IOException, InterruptedException, PreferencesException,
			WriteException, DocStructHasNoTypeException, MetadataTypeNotAllowedException, ExportFileException, UghHelperException, ReadException,
			SwapException, DAOException, TypeNotAllowedForParentException {

		/*
		 * -------------------------------- Read Document --------------------------------
		 */
		this.myPrefs = myProzess.getRegelsatz().getPreferences();
		String atsPpnBand = myProzess.getTitel();
		Fileformat gdzfile = myProzess.readMetadataFile();

		String rules = ConfigMain.getParameter("copyData.onExport");
		if (rules != null && !rules.equals("- keine Konfiguration gefunden -")) {
			try {
				new DataCopier(rules).process(new CopierData(gdzfile, myProzess));
			} catch (ConfigurationException e) {
				Helper.setFehlerMeldung("dataCopier.syntaxError", e.getMessage());
				return false;
			} catch (RuntimeException exception) {
				Helper.setFehlerMeldung("dataCopier.runtimeException", exception.getMessage());
				return false;
			}
		}

		/* nur beim Rusdml-Projekt die Metadaten aufbereiten */
		ConfigProjects cp = new ConfigProjects(myProzess.getProjekt().getTitel());
		if (cp.getParamList("dmsImport.check").contains("rusdml")) {
			ExportDms_CorrectRusdml expcorr = new ExportDms_CorrectRusdml(myProzess, this.myPrefs, gdzfile);
			atsPpnBand = expcorr.correctionStart();
		}

		String zielVerzeichnis = prepareUserDirectory(inZielVerzeichnis);

		String targetFileName = zielVerzeichnis + atsPpnBand + "_mets.xml";
		return writeMetsFile(myProzess, targetFileName, gdzfile, false);

	}

	/**
	 * prepare user directory
	 * 
	 * @param inTargetFolder the folder to prove and maybe create it
	 */
	protected String prepareUserDirectory(String inTargetFolder) {
		String target = inTargetFolder;
		Benutzer myBenutzer = (Benutzer) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
		if (myBenutzer != null) {
			try {
				FilesystemHelper.createDirectoryForUser(target, myBenutzer.getLogin());
			} catch (Exception e) {
				Helper.setFehlerMeldung("Export canceled, could not create destination directory: " + inTargetFolder, e);
			}
		}
		return target;
	}

	/**
	 * write MetsFile to given Path
	 * 
	 * @param myProzess the Process to use
	 * @param targetFileName the filename where the metsfile should be written
	 * @param gdzfile the FileFormat-Object to use for Mets-Writing
	 * @throws DAOException
	 * @throws SwapException
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws TypeNotAllowedForParentException
	 */

	protected boolean writeMetsFile(Prozess myProzess, String targetFileName, Fileformat gdzfile, boolean writeLocalFilegroup)
			throws PreferencesException, WriteException, IOException, InterruptedException, SwapException, DAOException,
			TypeNotAllowedForParentException {

		MetsModsImportExport mm = new MetsModsImportExport(this.myPrefs);
		mm.setWriteLocal(writeLocalFilegroup);
		String imageFolderPath = myProzess.getImagesDirectory();
		SafeFile imageFolder = new SafeFile(imageFolderPath);
		/*
		 * before creating mets file, change relative path to absolute -
		 */
		DigitalDocument dd = gdzfile.getDigitalDocument();
		if (dd.getFileSet() == null) {
			Helper.setMeldung(myProzess.getTitel() + ": digital document does not contain images; temporarily adding them for mets file creation");

			MetadatenImagesHelper mih = new MetadatenImagesHelper(this.myPrefs, dd);
			mih.createPagination(myProzess, null);
		}

		/*
		 * get the topstruct element of the digital document depending on anchor property
		 */
		DocStruct topElement = dd.getLogicalDocStruct();
		if (this.myPrefs.getDocStrctTypeByName(topElement.getType().getName()).getAnchorClass() != null) {
			if (topElement.getAllChildren() == null || topElement.getAllChildren().size() == 0) {
				throw new PreferencesException(myProzess.getTitel()
						+ ": the topstruct element is marked as anchor, but does not have any children for physical docstrucs");
			} else {
				topElement = topElement.getAllChildren().get(0);
			}
		}

		/*
		 * -------------------------------- if the top element does not have any image related, set them all --------------------------------
		 */
		if (topElement.getAllToReferences("logical_physical") == null || topElement.getAllToReferences("logical_physical").size() == 0) {
			if (dd.getPhysicalDocStruct() != null && dd.getPhysicalDocStruct().getAllChildren() != null) {
				Helper.setMeldung(myProzess.getTitel()
						+ ": topstruct element does not have any referenced images yet; temporarily adding them for mets file creation");
				for (DocStruct mySeitenDocStruct : dd.getPhysicalDocStruct().getAllChildren()) {
					topElement.addReferenceTo(mySeitenDocStruct, "logical_physical");
				}
			} else {
				if (this instanceof ExportDms && ((ExportDms) this).exportDmsTask != null) {
					((ExportDms) this).exportDmsTask.setException(new RuntimeException(myProzess.getTitel()
							+ ": could not find any referenced images, export aborted"));
				} else {
					Helper.setFehlerMeldung(myProzess.getTitel()
							+ ": could not find any referenced images, export aborted");
				}
				return false;
			}
		}

		for (ContentFile cf : dd.getFileSet().getAllFiles()) {
			String location = cf.getLocation();
			// If the file's location string shoes no sign of any protocol,
			// use the file protocol.
			if (!location.contains("://")) {
				location = "file://" + location;
			}
			String url = new URL(location).getFile();
			SafeFile f = new SafeFile(!url.startsWith(imageFolder.toURL().getPath()) ? imageFolder : null, url);
			cf.setLocation(f.toURI().toString());
		}

		mm.setDigitalDocument(dd);

		/*
		 * -------------------------------- wenn Filegroups definiert wurden, werden diese jetzt in die Metsstruktur übernommen
		 * --------------------------------
		 */
		// Replace all paths with the given VariableReplacer, also the file
		// group paths!
		VariableReplacer vp = new VariableReplacer(mm.getDigitalDocument(), this.myPrefs, myProzess, null);
		Set<ProjectFileGroup> myFilegroups = myProzess.getProjekt().getFilegroups();

		if (myFilegroups != null && myFilegroups.size() > 0) {
			for (ProjectFileGroup pfg : myFilegroups) {
				// check if source files exists
				if (pfg.getFolder() != null && pfg.getFolder().length() > 0) {
					SafeFile folder = new SafeFile(myProzess.getMethodFromName(pfg.getFolder()));
					if (folder.exists() && folder.list().length > 0) {
						VirtualFileGroup v = new VirtualFileGroup();
						v.setName(pfg.getName());
						v.setPathToFiles(vp.replace(pfg.getPath()));
						v.setMimetype(pfg.getMimetype());
						v.setFileSuffix(pfg.getSuffix());
						v.setOrdinary(!pfg.isPreviewImage());
						mm.getDigitalDocument().getFileSet().addVirtualFileGroup(v);
					}
				} else {

					VirtualFileGroup v = new VirtualFileGroup();
					v.setName(pfg.getName());
					v.setPathToFiles(vp.replace(pfg.getPath()));
					v.setMimetype(pfg.getMimetype());
					v.setFileSuffix(pfg.getSuffix());
					v.setOrdinary(!pfg.isPreviewImage());
					mm.getDigitalDocument().getFileSet().addVirtualFileGroup(v);
				}
			}
		}

		// Replace rights and digiprov entries.
		mm.setRightsOwner(vp.replace(myProzess.getProjekt().getMetsRightsOwner()));
		mm.setRightsOwnerLogo(vp.replace(myProzess.getProjekt().getMetsRightsOwnerLogo()));
		mm.setRightsOwnerSiteURL(vp.replace(myProzess.getProjekt().getMetsRightsOwnerSite()));
		mm.setRightsOwnerContact(vp.replace(myProzess.getProjekt().getMetsRightsOwnerMail()));
		mm.setDigiprovPresentation(vp.replace(myProzess.getProjekt().getMetsDigiprovPresentation()));
		mm.setDigiprovReference(vp.replace(myProzess.getProjekt().getMetsDigiprovReference()));
		mm.setDigiprovPresentationAnchor(vp.replace(myProzess.getProjekt().getMetsDigiprovPresentationAnchor()));
		mm.setDigiprovReferenceAnchor(vp.replace(myProzess.getProjekt().getMetsDigiprovReferenceAnchor()));

		mm.setPurlUrl(vp.replace(myProzess.getProjekt().getMetsPurl()));
		mm.setContentIDs(vp.replace(myProzess.getProjekt().getMetsContentIDs()));

		// Set mets pointers. MetsPointerPathAnchor or mptrAnchorUrl  is the
		// pointer used to point to the superordinate (anchor) file, that is
		// representing a “virtual” group such as a series. Several anchors
		// pointer paths can be defined/ since it is possible to define several
		// levels of superordinate structures (such as the complete edition of
		// a daily newspaper, one year ouf of that edition, …)
		String anchorPointersToReplace = myProzess.getProjekt().getMetsPointerPath();
		mm.setMptrUrl(null);
		for (String anchorPointerToReplace : anchorPointersToReplace.split(Projekt.ANCHOR_SEPARATOR)) {
			String anchorPointer = vp.replace(anchorPointerToReplace);
			mm.setMptrUrl(anchorPointer);
		}

		// metsPointerPathAnchor or mptrAnchorUrl is the pointer used to point
		// from the (lowest) superordinate (anchor) file to the lowest level
		// file (the non-anchor file). 
		String metsPointerToReplace = myProzess.getProjekt().getMetsPointerPathAnchor();
		String metsPointer = vp.replace(metsPointerToReplace);
		mm.setMptrAnchorUrl(metsPointer);

		if (ConfigMain.getBooleanParameter("ExportValidateImages", true)) {
			try {
				// TODO andere Dateigruppen nicht mit image Namen ersetzen
				List<String> images = new MetadatenImagesHelper(this.myPrefs, dd).getDataFiles(myProzess);
				int sizeOfPagination = dd.getPhysicalDocStruct().getAllChildren().size();
				if (images != null) {
					int sizeOfImages = images.size();
					if (sizeOfPagination == sizeOfImages) {
						dd.overrideContentFiles(images);
					} else {
						List<String> param = new ArrayList<String>();
						param.add(String.valueOf(sizeOfPagination));
						param.add(String.valueOf(sizeOfImages));
						Helper.setFehlerMeldung(Helper.getTranslation("imagePaginationError", param));
						return false;
					}
				}
			} catch (IndexOutOfBoundsException e) {
				myLogger.error(e);
				return false;
			} catch (InvalidImagesException e) {
				myLogger.error(e);
				return false;
			}
		} else {
			// create pagination out of virtual file names
			dd.addAllContentFiles();

		}
		mm.write(targetFileName);
		Helper.setMeldung(null, myProzess.getTitel() + ": ", "ExportFinished");
		return true;
	}
}
