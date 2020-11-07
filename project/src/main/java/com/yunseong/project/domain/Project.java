package com.yunseong.project.domain;

import com.yunseong.board.api.BoardDetail;
import com.yunseong.common.AlreadyExistedElementException;
import com.yunseong.common.CannotReviseBoardIfWriterNotWereException;
import com.yunseong.common.UnsupportedStateTransitionException;
import com.yunseong.project.api.event.*;
import io.eventuate.tram.events.aggregates.ResultWithDomainEvents;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.lang.reflect.Member;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Project {

    @Id
    @GeneratedValue
    @Column(name = "project_id")
    private Long id;

    private Long teamId;

    private Long boardId;

    private Long weClassId;

    @Enumerated(EnumType.STRING)
    private ProjectState projectState;

    @Embedded
    private Board board;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(joinColumns = @JoinColumn(name = "project_id"))
    private final Set<String> members = new HashSet<>();

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastDate;

    private boolean isPublic;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    public Project(boolean open, LocalDate lastDate) {
        this.projectState = ProjectState.POST_PENDING;
        this.isPublic = open;
        this.lastDate = lastDate;
    }

    public static ResultWithDomainEvents<Project, ProjectEvent> create(boolean isPublic, LocalDate lastDate) {
        return new ResultWithDomainEvents<>(new Project(isPublic, lastDate), new ProjectCreatedEvent());
    }

    public void addMember(String username) {
        if(!this.members.add(username)) throw new AlreadyExistedElementException("이미 당신은 팀에 속해있습니다.");
    }

    public void removeMember(String username) {
        this.members.remove(username);
    }

    public List<ProjectEvent> revise() {
        if (this.projectState == ProjectState.POSTED) {
            this.projectState = ProjectState.REVISION_PENDING;
            return Collections.emptyList();
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public List<ProjectEvent> revised(ProjectSimpleRevision projectSimpleRevision) {
        if (this.projectState == ProjectState.REVISION_PENDING) {
            this.isPublic = projectSimpleRevision.isPublic();
            return Collections.emptyList();
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public List<ProjectEvent> cancel() {
        if (this.projectState == ProjectState.POSTED) {
            this.projectState = ProjectState.CANCEL_PENDING;
            return Collections.emptyList();
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public List<ProjectEvent> undoCancelOrPostedOrRevision() {
        switch (this.projectState) {
            case CANCEL_PENDING: case POST_PENDING: case REVISION_PENDING:
                this.projectState = ProjectState.POSTED;
                return Collections.emptyList();
            default:
                throw new UnsupportedStateTransitionException(this.projectState);
        }
    }

    public List<ProjectEvent> cancelled() {
        switch (this.projectState) {
            case CANCEL_PENDING: case POST_PENDING:
                this.projectState = ProjectState.CANCELLED;
                return Collections.singletonList(new ProjectCancelledEvent(this.id, this.members));
            default:
                throw new UnsupportedStateTransitionException(this.projectState);
        }
    }

    public List<ProjectEvent> close() {
        if (this.projectState == ProjectState.POSTED) {
            this.projectState = ProjectState.CLOSED;
            return Collections.emptyList();
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public List<ProjectEvent> reject() {
        if (this.projectState == ProjectState.CLOSED) {
            this.projectState = ProjectState.REJECTED;
            return Collections.singletonList(new ProjectRejectedEvent(this.id, this.members));
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public List<ProjectEvent> start() {
        if (this.projectState == ProjectState.CLOSED) {
            this.projectState = ProjectState.STARTED;
            return Collections.singletonList(new ProjectStartedEvent(this.id, this.members));
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public void registerTeam(long teamId, String username) {
        if (this.projectState == ProjectState.POST_PENDING) {
            this.teamId = teamId;
            this.members.add(username);
            return;
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public void registerBoard(long boardId, BoardDetail boardDetail) {
        if (this.projectState == ProjectState.POST_PENDING) {
            this.boardId = boardId;
            this.board = new Board(boardDetail.getWriter(), boardDetail.getSubject(), boardDetail.getBoardCategory());
            return;
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public void registerWeClass(long weClassId) {
        if (this.projectState == ProjectState.CLOSED) {
            this.weClassId = weClassId;
            return;
        }
        throw new UnsupportedStateTransitionException(this.projectState);
    }

    public boolean isWriter(String username) {
        String name = this.members.stream().findFirst().orElseThrow(() -> new EmptyCollectionException("프로젝트의 멤버리스트가 비어있습니다."));
        return !username.equals(name);
    }
}
