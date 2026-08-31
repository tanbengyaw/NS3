package my.gov.perkeso.assist.core.commands.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import my.gov.perkeso.assist.core.commands.annotation.CommandType;
import my.gov.perkeso.assist.core.commands.handler.CommandHandler;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

@Service
public class CommandHandlerProvider {

    private final Map<String, CommandHandler> handlers = new HashMap<>();

    public CommandHandlerProvider(final List<CommandHandler> handlerList) {
        for (final CommandHandler handler : handlerList) {
            final CommandType type = AnnotationUtils.findAnnotation(AopUtils.getTargetClass(handler), CommandType.class);
            if (type != null) {
                handlers.put(key(type.entity(), type.action()), handler);
            }
        }
    }

    public CommandHandler getHandler(final String entity, final String action) {
        final CommandHandler handler = handlers.get(key(entity, action));
        if (handler == null) {
            throw new IllegalStateException("No handler for entity=" + entity + " action=" + action);
        }
        return handler;
    }

    private static String key(final String entity, final String action) {
        return entity + "|" + action;
    }
}
