package dev.lost.engine.commands;

import dev.lost.engine.LostEngine;
import dev.lost.engine.WebServer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class EditorCommand implements BasicCommand {
    @Override
    public void execute(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] strings) {
        Player player = commandSourceStack.getExecutor() instanceof Player player1 ? player1 : null;
        if (player != null) {
            player.showDialog(
                    Dialog.create(dialogRegistryBuilderFactory -> dialogRegistryBuilderFactory.empty()
                            .base(DialogBase.builder(Component.text("LostEngine Web Editor"))
                                    .body(
                                            List.of(
                                                    DialogBody.plainMessage(
                                                            Component.text("Open link\uD83E\uDC55")
                                                                    .clickEvent(
                                                                            ClickEvent.openUrl(LostEngine.getResourcePackUrl() +
                                                                                    "?token=" +
                                                                                    WebServer.getToken()
                                                                            )
                                                                    )
                                                    ),
                                                    DialogBody.plainMessage(
                                                            Component.text("Read-only link\uD83E\uDC55")
                                                                    .clickEvent(
                                                                            ClickEvent.openUrl(LostEngine.getResourcePackUrl() +
                                                                                    "?token=" +
                                                                                    WebServer.getReadOnlyToken()
                                                                            )
                                                                    )
                                                    )
                                            )
                                    )
                                    .build()
                            )
                            .type(DialogType.notice())
                    )
            );
        } else {
            commandSourceStack.getSender().sendPlainMessage(
                    "Web editor link: " +
                            LostEngine.getResourcePackUrl() +
                            "?token=" +
                            WebServer.getToken() +
                            "\n" +
                            "Read-only link: " +
                            LostEngine.getResourcePackUrl() +
                            "?token=" +
                            WebServer.getReadOnlyToken()
            );
        }
    }

    @Override
    public @Nullable String permission() {
        return "op";
    }
}
