package ru.mail.polis.service.art241111.utils;

import org.jetbrains.annotations.NotNull;

public class ExtractId {
    private static final String PREFIX = "id=";

    @NotNull
    public static String extractId(@NotNull final String query){
        if(!query.startsWith(PREFIX)){
            throw new IllegalArgumentException("Id not set");
        }

        final String id = query.substring(PREFIX.length());

        if(id.isEmpty()){
            throw new IllegalArgumentException("Id is empty");
        }

        return id;
    }
}
