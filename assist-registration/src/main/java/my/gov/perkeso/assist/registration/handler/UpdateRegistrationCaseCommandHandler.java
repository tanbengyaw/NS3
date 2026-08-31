package my.gov.perkeso.assist.registration.handler;

import lombok.RequiredArgsConstructor;
import my.gov.perkeso.assist.core.commands.annotation.CommandType;
import my.gov.perkeso.assist.core.commands.api.JsonCommand;
import my.gov.perkeso.assist.core.commands.handler.CommandHandler;
import my.gov.perkeso.assist.core.infrastructure.data.CommandProcessingResult;
import my.gov.perkeso.assist.registration.service.RegistrationCaseWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = "REGISTRATION_CASE", action = "UPDATE")
@RequiredArgsConstructor
public class UpdateRegistrationCaseCommandHandler implements CommandHandler {

    private final RegistrationCaseWritePlatformService writeService;

    @Override
    @Transactional
    public CommandProcessingResult processCommand(final JsonCommand command) {
        return writeService.updateCase(command);
    }
}
