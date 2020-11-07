package com.yunseong.project.sagas.createproject;

import com.yunseong.board.api.BoardDetail;
import com.yunseong.board.api.command.CreateBoardCommand;
import com.yunseong.board.api.command.CreateBoardReply;
import com.yunseong.project.api.command.CreateTeamCommand;
import com.yunseong.project.api.command.CreateTeamReply;
import com.yunseong.project.sagaparticipants.CreateProjectCommand;
import com.yunseong.project.sagaparticipants.ConfirmCancelProjectCommand;
import com.yunseong.project.sagaparticipants.RegisterBoardCommand;
import com.yunseong.project.sagaparticipants.RegisterTeamCommand;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class CreateProjectSagaState {

    private long teamId;
    private long boardId;
    @NonNull
    private Long projectId;
    @NonNull
    private String username;
    @NonNull
    private Integer minSize;
    @NonNull
    private Integer maxSize;
    @NonNull
    private BoardDetail boardDetail;

    public ConfirmCancelProjectCommand makeProjectConfirmCancelCommand() {
        return new ConfirmCancelProjectCommand(this.projectId);
    }

    public CreateTeamCommand makeCreateTeamCommand() {
        return new CreateTeamCommand(this.projectId, this.username, this.minSize, this.maxSize);
    }

    public CreateBoardCommand makeCreateBoardCommand() {
        return new CreateBoardCommand(this.projectId, this.boardDetail.getWriter(), this.boardDetail.getSubject(), this.boardDetail.getContent(), this.boardDetail.getBoardCategory(), this.boardDetail.getImages());
    }

    public void handleCreateBoardReply(CreateBoardReply reply) {
        this.boardId = reply.getBoardId();
    }

    public RegisterBoardCommand makeRegisterBoardCommand() {
        return new RegisterBoardCommand(this.projectId, this.boardId, this.boardDetail);
    }

    public RegisterTeamCommand makeRegisterTeamCommand() {
        return new RegisterTeamCommand(this.projectId, this.username, this.teamId);
    }

    public CreateProjectCommand makeCreateProjectCommand() {
        return new CreateProjectCommand(this.projectId);
    }

    public void handleCreateTeamReply(CreateTeamReply createTeamReply) {
        this.teamId = createTeamReply.getTeamId();
    }
}
