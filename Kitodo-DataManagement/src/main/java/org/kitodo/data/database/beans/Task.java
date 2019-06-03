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

package org.kitodo.data.database.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.kitodo.data.database.converter.TaskEditTypeConverter;
import org.kitodo.data.database.converter.TaskStatusConverter;
import org.kitodo.data.database.enums.TaskEditType;
import org.kitodo.data.database.enums.TaskStatus;
import org.kitodo.data.database.persistence.TaskDAO;

@Entity
@Table(name = "task")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Task extends BaseIndexedBean {

    @Column(name = "title")
    private String title;

    @Column(name = "ordering")
    private Integer ordering;

    @Column(name = "processingStatus")
    @Convert(converter = TaskStatusConverter.class)
    private TaskStatus processingStatus = TaskStatus.LOCKED;

    @Column(name = "processingTime")
    private Date processingTime;

    @Column(name = "processingBegin")
    private Date processingBegin;

    @Column(name = "processingEnd")
    private Date processingEnd;

    @Column(name = "editType")
    @Convert(converter = TaskEditTypeConverter.class)
    private TaskEditType editType = TaskEditType.UNNOWKN;

    @Column(name = "homeDirectory")
    private short homeDirectory;

    @Column(name = "concurrent")
    private boolean concurrent = false;

    @Column(name = "last")
    private boolean last = false;

    @Column(name = "correction")
    private boolean correction = false;

    @Column(name = "typeMetadata")
    private boolean typeMetadata = false;

    @Column(name = "typeAutomatic")
    private boolean typeAutomatic = false;

    @Column(name = "typeImagesRead")
    private boolean typeImagesRead = false;

    @Column(name = "typeImagesWrite")
    private boolean typeImagesWrite = false;

    @Column(name = "typeExportDms")
    private boolean typeExportDMS = false;

    @Column(name = "typeAcceptClose")
    private boolean typeAcceptClose = false;

    @Column(name = "scriptName")
    private String scriptName;

    @Column(name = "scriptPath")
    private String scriptPath;

    @Column(name = "typeCloseVerify")
    private boolean typeCloseVerify = false;

    @Column(name = "batchStep")
    private boolean batchStep = false;

    @Column(name = "repeatOnCorrection")
    private boolean repeatOnCorrection = false;

    @Column(name = "workflowId")
    private String workflowId;

    @ManyToOne
    @JoinColumn(name = "workflowCondition_id", foreignKey = @ForeignKey(name = "FK_task_workflowCondition_id"))
    private WorkflowCondition workflowCondition;

    /**
     * This field contains information about user, which works on this task.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_task_user_id"))
    private User processingUser;

    @ManyToOne
    @JoinColumn(name = "template_id", foreignKey = @ForeignKey(name = "FK_task_template_id"))
    private Template template;

    @ManyToOne
    @JoinColumn(name = "process_id", foreignKey = @ForeignKey(name = "FK_task_process_id"))
    private Process process;

    /**
     * This field contains information about user's roles, which are allowed to
     * work on this task.
     */
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "task_x_role", joinColumns = {
            @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "FK_task_x_role_task_id")) }, inverseJoinColumns = {
                    @JoinColumn(name = "role_id", foreignKey = @ForeignKey(name = "FK_task_x_user_role_id")) })
    private List<Role> roles;

    /**
     * This field contains information about folders whose contents are to be
     * generated in this task.
     */
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "contentFolders_task_x_folder",
        joinColumns = @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "FK_contentFolders_task_x_folder_task_id")),
        inverseJoinColumns = @JoinColumn(name = "folder_id", foreignKey = @ForeignKey(name = "FK_task_x_folder_folder_id"))
    )
    private List<Folder> contentFolders;

    /**
     * This field contains information about folders whose contents are to be
     * validated in this task.
     */
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "validationFolders_task_x_folder",
        joinColumns = @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "FK_validationFolders_task_x_folder_task_id")),
        inverseJoinColumns = @JoinColumn(name = "folder_id", foreignKey = @ForeignKey(name = "FK_task_x_folder_folder_id"))
    )
    private List<Folder> validationFolders;

    @Transient
    private String localizedTitle;

    /**
     * Constructor.
     */
    public Task() {
        this.title = "";
        this.roles = new ArrayList<>();
        this.ordering = 0;
    }

    /**
     * Copy constructor.
     *
     * @param templateTask
     *            task to copy
     */
    public Task(Task templateTask) {
        this.title = templateTask.getTitle();
        this.ordering = templateTask.getOrdering();
        this.typeAutomatic = templateTask.isTypeAutomatic();
        this.scriptName = templateTask.getScriptName();
        this.scriptPath = templateTask.getScriptPath();
        this.batchStep = templateTask.isBatchStep();
        this.concurrent = templateTask.isConcurrent();
        this.last = templateTask.isLast();
        this.typeAcceptClose = templateTask.isTypeAcceptClose();
        this.typeCloseVerify = templateTask.isTypeCloseVerify();
        this.typeExportDMS = templateTask.isTypeExportDMS();
        this.typeImagesRead = templateTask.isTypeImagesRead();
        this.typeImagesWrite = templateTask.isTypeImagesWrite();
        this.typeMetadata = templateTask.isTypeMetadata();
        this.repeatOnCorrection = templateTask.isRepeatOnCorrection();
        this.processingStatus = templateTask.getProcessingStatus();
        this.homeDirectory = templateTask.getHomeDirectory();
        this.workflowId = templateTask.getWorkflowId();
        this.workflowCondition = templateTask.getWorkflowCondition();

        // necessary to create new ArrayList in other case session problem!
        this.roles = new ArrayList<>(templateTask.getRoles());

        // necessary to create new ArrayList in other case session problem!
        this.contentFolders = new ArrayList<>(templateTask.getContentFolders());
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getOrdering() {
        return this.ordering;
    }

    public void setOrdering(Integer ordering) {
        this.ordering = ordering;
    }

    /**
     * Get editType as {@link TaskEditType}.
     *
     * @return current edit type
     */
    public TaskEditType getEditType() {
        return this.editType;
    }

    /**
     * Set editType to specific value from {@link TaskEditType}.
     *
     * @param inputType
     *            as {@link TaskEditType}
     */
    public void setEditType(TaskEditType inputType) {
        this.editType = inputType;
    }

    /**
     * Set processing status to specific value from {@link TaskStatus}.
     *
     * @param inputStatus
     *            as {@link TaskStatus}
     */
    public void setProcessingStatus(TaskStatus inputStatus) {
        this.processingStatus = inputStatus;
    }

    /**
     * Get processing status as {@link TaskStatus}.
     *
     * @return current processing status
     */
    public TaskStatus getProcessingStatus() {
        return this.processingStatus;
    }

    public Date getProcessingTime() {
        return this.processingTime;
    }

    public void setProcessingTime(Date processingTime) {
        this.processingTime = processingTime;
    }

    public Date getProcessingBegin() {
        return this.processingBegin;
    }

    public void setProcessingBegin(Date processingBegin) {
        this.processingBegin = processingBegin;
    }

    public Date getProcessingEnd() {
        return this.processingEnd;
    }

    public void setProcessingEnd(Date processingEnd) {
        this.processingEnd = processingEnd;
    }

    public short getHomeDirectory() {
        return this.homeDirectory;
    }

    public void setHomeDirectory(short homeDirectory) {
        this.homeDirectory = homeDirectory;
    }

    /**
     * Get concurrent.
     *
     * @return value of concurrent
     */
    public boolean isConcurrent() {
        return concurrent;
    }

    /**
     * Set concurrent.
     *
     * @param concurrent
     *            as boolean
     */
    public void setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
    }

    /**
     * Get information if task is the last task in the workflow.
     *
     * @return information if task is the last task in the workflow
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Set last information if task is the last task in the workflow.
     *
     * @param last
     *            as true or false
     */
    public void setLast(boolean last) {
        this.last = last;
    }

    /**
     * Get correction.
     *
     * @return value of correction
     */
    public boolean isCorrection() {
        return correction;
    }

    /**
     * Set correction.
     *
     * @param correction as boolean
     */
    public void setCorrection(boolean correction) {
        this.correction = correction;
    }

    public User getProcessingUser() {
        return this.processingUser;
    }

    public void setProcessingUser(User processingUser) {
        this.processingUser = processingUser;
    }

    public Process getProcess() {
        return this.process;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    /**
     * Get template.
     *
     * @return value of template
     */
    public Template getTemplate() {
        return this.template;
    }

    /**
     * Set template.
     *
     * @param template
     *            as Template
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    /**
     * Get list of roles.
     *
     * @return list of Role objects or empty list
     */
    public List<Role> getRoles() {
        initialize(new TaskDAO(), this.roles);
        if (Objects.isNull(this.roles)) {
            this.roles = new ArrayList<>();
        }
        return this.roles;
    }

    /**
     * Set list of roles.
     *
     * @param roles
     *            as list of Role objects
     */
    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    /**
     * Get list of folders whose contents are to be generated.
     *
     * @return list of Folder objects or empty list
     */
    public List<Folder> getContentFolders() {
        initialize(new TaskDAO(), this.contentFolders);
        if (Objects.isNull(this.contentFolders)) {
            this.contentFolders = new ArrayList<>();
        }
        return contentFolders;
    }

    /**
     * Set list of folders whose contents are to be generated.
     *
     * @param contentFolders
     *            as list
     */
    public void setContentFolders(List<Folder> contentFolders) {
        this.contentFolders = contentFolders;
    }

    /**
     * Get list of folders whose contents are to be validated.
     *
     * @return list of Folder objects or empty list
     */
    public List<Folder> getValidationFolders() {
        initialize(new TaskDAO(), this.validationFolders);
        if (Objects.isNull(this.validationFolders)) {
            this.validationFolders = new ArrayList<>();
        }
        return validationFolders;
    }

    /**
     * Set list of folders whose contents are to be validated.
     *
     * @param validationFolders
     *            as list
     */
    public void setValidationFolders(List<Folder> validationFolders) {
        this.validationFolders = validationFolders;
    }

    public boolean isTypeImagesRead() {
        return this.typeImagesRead;
    }

    public void setTypeImagesRead(boolean typeImagesRead) {
        this.typeImagesRead = typeImagesRead;
    }

    public boolean isTypeImagesWrite() {
        return this.typeImagesWrite;
    }

    /**
     * Set task type images. If types is true, it also sets type images read to
     * true.
     *
     * @param typeImagesWrite
     *            true or false
     */
    public void setTypeImagesWrite(boolean typeImagesWrite) {
        this.typeImagesWrite = typeImagesWrite;
        if (typeImagesWrite) {
            this.typeImagesRead = true;
        }
    }

    public boolean isTypeExportDMS() {
        return this.typeExportDMS;
    }

    public void setTypeExportDMS(boolean typeExportDMS) {
        this.typeExportDMS = typeExportDMS;
    }

    public boolean isTypeMetadata() {
        return this.typeMetadata;
    }

    public void setTypeMetadata(boolean typeMetadata) {
        this.typeMetadata = typeMetadata;
    }

    public boolean isTypeAcceptClose() {
        return this.typeAcceptClose;
    }

    public void setTypeAcceptClose(boolean typeAcceptClose) {
        this.typeAcceptClose = typeAcceptClose;
    }

    public boolean isTypeAutomatic() {
        return this.typeAutomatic;
    }

    public void setTypeAutomatic(boolean typeAutomatic) {
        this.typeAutomatic = typeAutomatic;
    }

    public boolean isTypeCloseVerify() {
        return this.typeCloseVerify;
    }

    public void setTypeCloseVerify(boolean typeCloseVerify) {
        this.typeCloseVerify = typeCloseVerify;
    }

    public String getScriptName() {
        return this.scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getScriptPath() {
        return this.scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    /**
     * Get workflow id - id of task object in diagram - by this id we can identify
     * change done to task.
     *
     * @return workflow id as String
     */
    public String getWorkflowId() {
        return workflowId;
    }

    /**
     * Set workflow id.
     *
     * @param workflowId
     *            id of task object in diagram - by this id we can identify change
     *            done to task
     */
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    /**
     * Get workflowCondition.
     *
     * @return value of workflowCondition
     */
    public WorkflowCondition getWorkflowCondition() {
        return workflowCondition;
    }

    /**
     * Set workflowCondition.
     *
     * @param workflowCondition
     *            as String
     */
    public void setWorkflowCondition(WorkflowCondition workflowCondition) {
        this.workflowCondition = workflowCondition;
    }

    public boolean isBatchStep() {
        return this.batchStep;
    }

    public void setBatchStep(boolean batchStep) {
        this.batchStep = batchStep;
    }

    /**
     * Get information if task should be repeated on correction.
     *
     * @return value of repeatOnCorrection
     */
    public boolean isRepeatOnCorrection() {
        return repeatOnCorrection;
    }

    /**
     * Set information if task should be repeated on correction.
     *
     * @param repeatOnCorrection as boolean
     */
    public void setRepeatOnCorrection(boolean repeatOnCorrection) {
        this.repeatOnCorrection = repeatOnCorrection;
    }

    /**
     * Get localized title.
     *
     * @return localized title as String
     */
    public String getLocalizedTitle() {
        return this.localizedTitle;
    }

    /**
     * Set localized titles as String.
     *
     * @param localizedTitle
     *            as String
     */
    public void setLocalizedTitle(String localizedTitle) {
        this.localizedTitle = localizedTitle;
    }

    // Here will be methods which should be in TaskService but are used by jsp
    // files
    /**
     * Get task title with user full name.
     *
     * @return task title with user full name as String
     */
    public String getTitleWithUserName() {
        String result = this.getTitle();
        if (this.getProcessingUser() != null && this.getProcessingUser().getId() != null
                && this.getProcessingUser().getId() != 0) {
            result += " (" + this.getProcessingUser().getFullName() + ")";
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof Task) {
            Task task = (Task) object;
            return Objects.equals(this.getId(), task.getId());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, processingTime, processingBegin, processingEnd, process, template);
    }
}
