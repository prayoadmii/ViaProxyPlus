/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package git.prayoadmii.viaproxyplus.cli.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import git.prayoadmii.viaproxyplus.cli.command.Command;
import git.prayoadmii.viaproxyplus.cli.command.CommandManager;
import git.prayoadmii.viaproxyplus.cli.command.executor.CommandExecutor;

public class HelpCommand extends Command {

    private final CommandManager commandManager;

    public HelpCommand(final CommandManager commandManager) {
        super("help", "Print a list of available commands and their arguments", "help");
        this.commandManager = commandManager;
    }

    @Override
    public void register(final LiteralArgumentBuilder<CommandExecutor> builder) {
        builder
                .executes(context -> {
                    final CommandExecutor executor = context.getSource();
                    for (Command command : this.commandManager.getCommands()) {
                        executor.sendMessage(command.getName() + " (" + command.getDescription() + "): " + command.getHelp());
                    }
                    return 1;
                });
    }

}
