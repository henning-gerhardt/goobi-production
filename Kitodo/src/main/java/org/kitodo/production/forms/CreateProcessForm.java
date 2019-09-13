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

package org.kitodo.production.forms;

import com.sun.jersey.api.NotFoundException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom.JDOMException;
import org.kitodo.api.Metadata;
import org.kitodo.api.dataeditor.rulesetmanagement.RulesetManagementInterface;
import org.kitodo.api.dataformat.Workpiece;
import org.kitodo.config.ConfigCore;
import org.kitodo.config.ConfigProject;
import org.kitodo.config.DigitalCollection;
import org.kitodo.config.enums.ParameterCore;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.Template;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.enums.TaskEditType;
import org.kitodo.data.database.enums.TaskStatus;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.exceptions.InvalidMetadataValueException;
import org.kitodo.exceptions.ProcessGenerationException;
import org.kitodo.production.enums.ObjectType;
import org.kitodo.production.forms.createprocess.AdditionalDetailsTab;
import org.kitodo.production.forms.createprocess.AdditionalDetailsTableRow;
import org.kitodo.production.forms.createprocess.ImportTab;
import org.kitodo.production.forms.createprocess.ProcessDataTab;
import org.kitodo.production.forms.createprocess.SearchTab;
import org.kitodo.production.forms.createprocess.TitleRecordLinkTab;
import org.kitodo.production.forms.dataeditor.DataEditorForm;
import org.kitodo.production.helper.Helper;
import org.kitodo.production.interfaces.RulesetSetupInterface;
import org.kitodo.production.metadata.MetadataEditor;
import org.kitodo.production.process.ProcessGenerator;
import org.kitodo.production.process.ProcessValidator;
import org.kitodo.production.services.ServiceManager;

@Named("CreateProcessForm")
@ViewScoped
public class CreateProcessForm extends BaseForm implements RulesetSetupInterface {

    private static Logger logger = LogManager.getLogger(CreateProcessForm.class);

    private final ImportTab importTab = new ImportTab(this);
    private final ProcessDataTab processDataTab = new ProcessDataTab(this);
    private AdditionalDetailsTab additionalDetailsTab = new AdditionalDetailsTab(this);
    private final SearchTab searchTab = new SearchTab(this);
    private final TitleRecordLinkTab titleRecordLinkTab = new TitleRecordLinkTab(this);

    private RulesetManagementInterface rulesetManagementInterface;
    private List<Locale.LanguageRange> priorityList;
    private String acquisitionStage = "create";
    private Project project;
    private Workpiece workpiece = new Workpiece();
    private Template template;
    private boolean useTemplates;
    private LinkedList<Process> processes = new LinkedList<>(Collections.singletonList(new Process()));
    private final String processListPath = MessageFormat.format(REDIRECT_PATH, "processes");

    /**
     * Returns the ruleset management to access the ruleset.
     *
     * @return the ruleset
     */
    @Override
    public RulesetManagementInterface getRuleset() {
        return rulesetManagementInterface;
    }

    /**
     * Returns the current acquisition stage to adapt the displaying of fields
     * accordingly.
     *
     * @return the current acquisition stage
     */
    @Override
    public String getAcquisitionStage() {
        return acquisitionStage;
    }

    /**
     * Returns the language preference list of the editing user to display
     * labels and options in the user-preferred language.
     *
     * @return the language preference list
     */
    @Override
    public List<Locale.LanguageRange> getPriorityList() {
        return priorityList;
    }

    /**
     * Get importTab.
     *
     * @return value of importTab
     */
    public ImportTab getImportTab() {
        return importTab;
    }

    /**
     * Get processDataTab.
     *
     * @return value of processDataTab
     */
    public ProcessDataTab getProcessDataTab() {
        return processDataTab;
    }

    /**
     * Get additionalDetailsTab.
     *
     * @return value of additionalDetailsTab
     */
    public AdditionalDetailsTab getAdditionalDetailsTab() {
        return additionalDetailsTab;
    }

    /**
     * Get searchTab.
     *
     * @return value of searchTab
     */
    public SearchTab getSearchTab() {
        return searchTab;
    }

    /**
     * Get titleRecordLinkTab.
     *
     * @return value of titleRecordLinkTab
     */
    public TitleRecordLinkTab getTitleRecordLinkTab() {
        return titleRecordLinkTab;
    }

    /**
     * Get useTemplate.
     *
     * @return value of useTemplate
     */
    public boolean isUseTemplates() {
        return useTemplates;
    }

    /**
     * Set useTemplate.
     *
     * @param useTemplates as boolean
     */
    public void setUseTemplates(boolean useTemplates) {
        this.useTemplates = useTemplates;
    }

    /**
     * Get newProcesses.
     *
     * @return value of newProcesses
     */
    public List<Process> getProcesses() {
        return processes;
    }

    /**
     * Set newProcesses.
     *
     * @param processes as java.util.List<org.kitodo.data.database.beans.Process>
     */
    public void setProcesses(LinkedList<Process> processes) {
        this.processes = processes;
    }

    /**
     * Get the main Process that want to be created.
     *
     * @return value of first element in newProcesses
     */
    public Process getMainProcess() {
        if (this.processes.isEmpty()) {
            throw new NotFoundException("Process list is empty!");
        }
        return this.processes.get(0);
    }

    /**
     * Get template.
     *
     * @return value of template
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * Set template.
     *
     * @param template as org.kitodo.data.database.beans.Template
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    /**
     * Get project.
     *
     * @return value of project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Get workpiece.
     *
     * @return value of workpiece
     */
    public Workpiece getWorkpiece() {
        return workpiece;
    }

    /**
     * Set project.
     *
     * @param project as org.kitodo.data.database.beans.Project
     */
    public void setProject(Project project) {
        this.project = project;
    }

    public void resetForm() {
        this.getProcessDataTab().resetProcessData();
        this.getAdditionalDetailsTab().resetAddtionalDetailsTable();
    }

    /**
     * Create the process and save the metadata.
     */
    public String createNewProcess() {
        if (Objects.nonNull(titleRecordLinkTab.getTitleRecordProcess())) {
            if (Objects.isNull(titleRecordLinkTab.getSelectedInsertionPosition())) {
                Helper.setErrorMessage("prozesskopieForm.createNewProcess.noInsertionPositionSelected");
                return stayOnCurrentPage;
            } else {
                User titleRecordOpenUser = DataEditorForm
                        .getUserOpened(titleRecordLinkTab.getTitleRecordProcess().getId());
                if (Objects.nonNull(titleRecordOpenUser)) {
                    Helper.setErrorMessage("prozesskopieForm.createNewProcess.titleRecordOpen",
                            titleRecordOpenUser.getFullName());
                    return stayOnCurrentPage;
                }
            }
        }
        if (createProcess()) {
            Process mainProcess = getMainProcess();
            if (Objects.nonNull(titleRecordLinkTab.getTitleRecordProcess())) {
                ServiceManager.getProcessService().refresh(mainProcess);
                try {
                    MetadataEditor.addLink(titleRecordLinkTab.getTitleRecordProcess(),
                            titleRecordLinkTab.getSelectedInsertionPosition(), mainProcess.getId());

                } catch (IOException exception) {
                    Helper.setErrorMessage("errorSaving", titleRecordLinkTab.getTitleRecordProcess().getTitle(), logger,
                            exception);
                }
            }
            return processListPath;
        }

        return this.stayOnCurrentPage;
    }

    /**
     * Prepare template and project for which new process will be created.
     *
     * @param templateId
     *            id of template to query from database
     * @param projectId
     *            id of project to query from database
     * @param referringView
     *            JSF page the user came from
     *
     * @return path to page with form
     */
    public String prepare(int templateId, int projectId, String referringView) {
        if (prepareProcess(templateId, projectId)) {
            return stayOnCurrentPage;
        }
        return MessageFormat.format(REDIRECT_PATH, referringView);
    }

    /**
     * Prepare new process which will be created.
     *
     * @param templateId
     *            id of template to query from database
     * @param projectId
     *            id of project to query from database
     *
     * @return true if process was prepared, otherwise false
     */
    public boolean prepareProcess(int templateId, int projectId) {
        //atstsl = "";
        ProcessGenerator processGenerator = new ProcessGenerator();
        try {
            boolean generated = processGenerator.generateProcess(templateId, projectId);
            if (generated) {
                this.processes = new LinkedList<>(Collections.singletonList(processGenerator.getGeneratedProcess()));
                this.project = processGenerator.getProject();
                this.template = processGenerator.getTemplate();
                resetForm();
                this.rulesetManagementInterface = openRulesetFile(this.getMainProcess().getRuleset().getFile());
                readProjectConfigs();
                this.processDataTab.setDigitalCollections(new ArrayList<>());
                initializePossibleDigitalCollections();
                // TODO: do we really still need this?
                //this.rdf = null;
                return true;
            }
        } catch (ProcessGenerationException | IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
        return false;
    }

    /**
     * Create process.
     *
     * @return true if process was created, otherwise false
     */
    public boolean createProcess() {
        Process mainProcess = this.getMainProcess();
        if (!ProcessValidator.isContentValid(mainProcess.getTitle(),
                this.additionalDetailsTab.getAdditionalDetailsTableRows(),
                this.processDataTab.getDigitalCollections(),
                this.processDataTab.getStandardFields(),
                true)) {
            return false;
        }
        addProperties();
        updateTasks(mainProcess);
        try {
            mainProcess.setSortHelperImages(this.processDataTab.getGuessedImages());
            ServiceManager.getProcessService().save(mainProcess);
        } catch (DataException e) {
            Helper.setErrorMessage("errorCreating", new Object[] {ObjectType.PROCESS.getTranslationSingular() }, logger,
                    e);
            return false;
        }
        if (!createProcessLocation()) {
            return false;
        }

        // TODO: check if this is still required!
        //processRdfConfiguration();
        if (Objects.nonNull(workpiece)) {
            workpiece.getRootElement().setType(processDataTab.getDocType());
            additionalDetailsTab.preserve();
            try (OutputStream out = ServiceManager.getFileService()
                    .write(ServiceManager.getProcessService().getMetadataFileUri(getMainProcess()))) {
                ServiceManager.getMetsService().save(workpiece, out);
            } catch (IOException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
        }

        if (Objects.nonNull(titleRecordLinkTab.getTitleRecordProcess())) {
            getMainProcess().setParent(titleRecordLinkTab.getTitleRecordProcess());
            titleRecordLinkTab.getTitleRecordProcess().getChildren().add(getMainProcess());
        }

        try {
            ServiceManager.getProcessService().save(mainProcess);
        } catch (DataException e) {
            Helper.setErrorMessage("errorCreating", new Object[] {ObjectType.PROCESS.getTranslationSingular() }, logger,
                    e);
            return false;
        }
        return true;
    }

    private void addProperties() {
        Process mainProcess = getMainProcess();
        addMetadataProperties(this.getAdditionalDetailsTab().getAdditionalDetailsTableRows(), mainProcess);

        for (String col : this.getProcessDataTab().getDigitalCollections()) {
            ProcessGenerator.addPropertyForProcess(mainProcess, "digitalCollection", col);
        }

        ProcessGenerator.addPropertyForWorkpiece(mainProcess, "DocType", this.getProcessDataTab().getDocType());
        ProcessGenerator.addPropertyForWorkpiece(mainProcess, "TifHeaderImagedescription",
                this.getProcessDataTab().getTifHeaderImageDescription());
        ProcessGenerator.addPropertyForWorkpiece(mainProcess, "TifHeaderDocumentname",
                this.getProcessDataTab().getTifHeaderDocumentName());
        ProcessGenerator.addPropertyForProcess(mainProcess, "Template", this.template.getTitle());
        ProcessGenerator.addPropertyForProcess(mainProcess, "TemplateID", String.valueOf(this.template.getId()));
    }

    private void addMetadataProperties(List<AdditionalDetailsTableRow> additionalRows, Process process) {
        try {
            for (AdditionalDetailsTableRow row : additionalRows) {
                if (!row.getMetadata().isEmpty() && row.getMetadata().toArray()[0] instanceof Metadata) {
                    String metadataValue = AdditionalDetailsTab.getMetadataValue(row);
                    Metadata metadata = (Metadata) row.getMetadata().toArray()[0];
                    switch (metadata.getDomain()) {
                        case DMD_SEC:
                            ProcessGenerator.addPropertyForWorkpiece(process, row.getLabel(), metadataValue);
                            break;
                        case SOURCE_MD:
                            ProcessGenerator.addPropertyForTemplate(process, row.getLabel(), metadataValue);
                            break;
                        case TECH_MD:
                            ProcessGenerator.addPropertyForProcess(process, row.getLabel(), metadataValue);
                            break;
                        default:
                            logger.info("Don't save metadata '" + row.getMetadataID() + "' with domain '"
                                    + metadata.getDomain() + "' to property.");
                            break;
                    }
                }
            }
        } catch (InvalidMetadataValueException e) {
            e.printStackTrace();
        }
    }

    private void updateTasks(Process process) {
        for (Task task : process.getTasks()) {
            // always save date and user for each step
            task.setProcessingTime(process.getCreationDate());
            task.setEditType(TaskEditType.AUTOMATIC);
            // only if its done, set edit start and end date
            if (task.getProcessingStatus() == TaskStatus.DONE) {
                task.setProcessingBegin(process.getCreationDate());
                // this concerns steps, which are set as done right on creation
                // bearbeitungsbeginn is set to creation timestamp of process
                // because the creation of it is basically begin of work
                Date date = new Date();
                task.setProcessingTime(date);
                task.setProcessingEnd(date);
            }
        }
    }

    private boolean createProcessLocation() {
        Process mainProcess = getMainProcess();
        try {
            URI processBaseUri = ServiceManager.getFileService().createProcessLocation(mainProcess);
            mainProcess.setProcessBaseUri(processBaseUri);
            return true;
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            try {
                ServiceManager.getProcessService().remove(mainProcess);
            } catch (DataException ex) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
            return false;
        }
    }

    /**
     * Read project configs for display in GUI.
     */
    protected void readProjectConfigs() {
        ConfigProject cp;
        try {
            cp = new ConfigProject(this.project.getTitle());
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            return;
        }
        this.processDataTab.setDocType(cp.getDocType());
        //this.useOpac = cp.isUseOpac();
        this.useTemplates = cp.isUseTemplates();

        //this.tifDefinition = cp.getTifDefinition();
        //this.titleDefinition = cp.getTitleDefinition();

        this.processDataTab.getStandardFields().putAll(cp.getHiddenFields());
    }

    private RulesetManagementInterface openRulesetFile(String fileName) throws IOException {
        final long begin = System.nanoTime();
        String metadataLanguage = ServiceManager.getUserService().getCurrentUser().getMetadataLanguage();
        priorityList = Locale.LanguageRange.parse(metadataLanguage.isEmpty() ? "en" : metadataLanguage);
        RulesetManagementInterface ruleset = ServiceManager.getRulesetManagementService().getRulesetManagement();
        ruleset.load(new File(Paths.get(ConfigCore.getParameter(ParameterCore.DIR_RULESETS), fileName).toString()));
        if (logger.isTraceEnabled()) {
            logger.trace("Reading ruleset took {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
        }
        return ruleset;
    }

    private void initializePossibleDigitalCollections() {
        try {
            DigitalCollection.possibleDigitalCollectionsForProcess(getMainProcess().getProject());
        } catch (JDOMException | IOException e) {
            Helper.setErrorMessage("Error while parsing digital collections", logger, e);
        }

        this.processDataTab.setAvailableDigitalCollections(DigitalCollection.getPossibleDigitalCollection());
        this.processDataTab.setDigitalCollections(DigitalCollection.getDigitalCollections());

        // if only one collection is possible take it directly
        if (this.processDataTab.getAvailableDigitalCollections().size() == 1) {
            this.processDataTab.getDigitalCollections().add(this.processDataTab.getAvailableDigitalCollections().get(0));
        }
    }
}
