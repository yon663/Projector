package com.yunseong.project.sagaparticipants;

import com.yunseong.project.api.ProjectServiceChannels;
import io.eventuate.tram.commands.common.Success;
import io.eventuate.tram.sagas.simpledsl.CommandEndpoint;
import io.eventuate.tram.sagas.simpledsl.CommandEndpointBuilder;
import org.springframework.stereotype.Component;

@Component
public class ProjectProxyService {

    public final CommandEndpoint<CreateProjectCommand> create = CommandEndpointBuilder
            .forCommand(CreateProjectCommand.class)
            .withChannel(ProjectServiceChannels.projectServiceChannel)
            .withReply(Success.class)
            .build();

    public final CommandEndpoint<RegisterTeamCommand> registerTeam = CommandEndpointBuilder
            .forCommand(RegisterTeamCommand.class)
            .withChannel(ProjectServiceChannels.projectServiceChannel)
            .withReply(Success.class)
            .build();

    public final CommandEndpoint<RegisterBoardCommand> registerBoard = CommandEndpointBuilder
            .forCommand(RegisterBoardCommand.class)
            .withChannel(ProjectServiceChannels.projectServiceChannel)
            .withReply(Success.class)
            .build();

    public final CommandEndpoint<ConfirmCancelProjectCommand> cancel = CommandEndpointBuilder
            .forCommand(ConfirmCancelProjectCommand.class)
            .withChannel(ProjectServiceChannels.projectServiceChannel)
            .withReply(Success.class)
            .build();
}
