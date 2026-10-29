package com.markethub.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.markethub.exception.ValidationException;

import java.io.IOException;
import java.io.Reader;

/** Single shared {@link Gson} instance plus request-body parsing that reports errors as 400s. */
public final class Json {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private Json() { }

    public static String write(Object value) {
        return GSON.toJson(value);
    }

    /** Parses a JSON request body; empty or malformed bodies become a {@link ValidationException}. */
    public static <T> T read(Reader body, Class<T> type) throws ValidationException, IOException {
        try {
            T value = GSON.fromJson(body, type);
            if (value == null) {
                throw new ValidationException("Request body is required");
            }
            return value;
        } catch (JsonParseException e) {
            throw new ValidationException("Request body is not valid JSON");
        }
    }
}
