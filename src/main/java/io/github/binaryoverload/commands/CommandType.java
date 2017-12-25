package io.github.binaryoverload.commands;

import io.github.binaryoverload.CookieBot;

import java.util.Set;

public enum CommandType {

    GENERAL,
    MODERATION,
    MUSIC,
    INTERNAL,
    USEFUL,
    CURRENCY,
    RANDOM,
    SECRET, INFORMATIONAL;

    public String toString() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    public static CommandType[] getTypes() {
        return new CommandType[]{GENERAL, MODERATION, MUSIC};
    }

    public Set<Command> getCommands() {
        return CookieBot.getInstance().getCommandsByType(this);
    }
}
