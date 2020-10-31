package com.yunseong.project.service;

import com.yunseong.common.UnsupportedStateTransitionException;
import com.yunseong.project.api.controller.CreateProjectRequest;
import com.yunseong.project.api.event.ProjectEvent;
import com.yunseong.project.controller.ProjectSearchCondition;
import com.yunseong.project.domain.Project;
import com.yunseong.project.domain.ProjectDomainEventPublisher;
import com.yunseong.project.domain.ProjectRepository;
import com.yunseong.project.domain.ProjectRevision;
import com.yunseong.project.sagas.cancelproject.CancelProjectSagaData;
import com.yunseong.project.sagas.createproject.CreateProjectSagaState;
import com.yunseong.project.sagas.reviseproject.ReviseProjectSagaData;
import com.yunseong.project.sagas.startproject.StartProjectSagaState;
import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import io.eventuate.tram.sagas.orchestration.SagaManager;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.function.Function;

@Service
@Transactional
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;

    private final ProjectDomainEventPublisher projectDomainEventPublisher;

    private final SagaManager<CreateProjectSagaState> createProjectSagaSagaManager;

    private final SagaManager<StartProjectSagaState> createWeClassSagaSagaManager;

    private final SagaManager<CancelProjectSagaData> cancelProjectSagaDataSagaManager;

    private final SagaManager<ReviseProjectSagaData> reviseProjectSagaDataSagaManager;

    public ResultWithDomainEvents<Project, ProjectEvent> createProject(CreateProjectRequest request) {
        ResultWithDomainEvents<Project, ProjectEvent> rwe = Project.create(request.getSubject(), request.getContent(), request.getProjectTheme());
        Project project = this.projectRepository.save(rwe.result);
        this.projectDomainEventPublisher.publish(rwe.result, rwe.events);

        this.createProjectSagaSagaManager.create(new CreateProjectSagaState(project.getId(), request.getUsername(), request.getMinSize(), request.getMaxSize()), Project.class, project.getId());

        return rwe;
    }

    public Project cancel(long projectId) throws EntityNotFoundException {
        Project project = this.findProject(projectId);
        CancelProjectSagaData data = new CancelProjectSagaData(projectId);
        this.cancelProjectSagaDataSagaManager.create(data);
        return project;
    }

    public Project revise(long projectId, ProjectRevision projectRevision) throws EntityNotFoundException {
        Project project = this.findProject(projectId);
        project.revised(projectRevision);
//        ReviseProjectSagaData data = new ReviseProjectSagaData(projectId, projectRevision);
//        this.reviseProjectSagaDataSagaManager.create(data);
        return project;
    }

    @Transactional(readOnly = true)
    public Page<Project> findBySearch(ProjectSearchCondition projectSearchCondition, Pageable pageable) {
        return this.projectRepository.findBySearch(projectSearchCondition, pageable);
    }

    public void startProject(long projectId, long teamId) {
        StartProjectSagaState data = new StartProjectSagaState(projectId, teamId);
        this.createWeClassSagaSagaManager.create(data, Project.class, projectId);
    }


    private Project updateProject(long projectId, Function<Project, List<ProjectEvent>> func) {
        Project project = findProject(projectId);
        this.projectDomainEventPublisher.publish(project, func.apply(project));
        return project;
    }

    public boolean closeProject(long projectId) {
        try {
            updateProject(projectId, Project::close);
            return true;
        } catch(UnsupportedStateTransitionException e) {
            return false;
        }
    }

    public void approveProject(long projectId) {
        updateProject(projectId, Project::start);
    }

    public void rejectProject(long projectId) {
        updateProject(projectId, Project::reject);
    }

    public boolean cancelProject(long projectId) {
        try {
            updateProject(projectId, Project::cancel);
            return true;
        }catch (UnsupportedStateTransitionException e) {
            return false;
        }
    }

    public boolean reviseProject(long projectId) {
        try {
            updateProject(projectId, Project::revise);
            return true;
        }catch (UnsupportedStateTransitionException e) {
            return false;
        }
    }

    public void undoCancelOrUndoReviseOrPostedProject(long projectId) {
        updateProject(projectId, Project::undoCancelOrPostedOrRevision);
    }

    public boolean revisedProject(long projectId, ProjectRevision projectRevision) {
        try {
            Project project = findProject(projectId);
            this.projectDomainEventPublisher.publish(project, project.revised(projectRevision));
            return true;
        }catch (UnsupportedStateTransitionException e) {
            return false;
        }
    }

    public boolean cancelledProject(long projectId) {
        try {
            updateProject(projectId, Project::cancelled);
            return true;
        }catch (UnsupportedStateTransitionException e) {
            return false;
        }
    }

    public void registerTeam(long projectId, long teamId) {
        Project project = findProject(projectId);
        project.registerTeam(teamId);
    }

    public void registerWeClass(long projectId, long weClassId) {
        Project project = findProject(projectId);
        project.registerWeClass(weClassId);
    }

    public Project findProject(long projectId) {
        Project project = this.projectRepository.findById(projectId).orElseThrow(() -> new EntityNotFoundException("해당 프로젝트는 존재하지않습니다"));
        return project;
    }
}
