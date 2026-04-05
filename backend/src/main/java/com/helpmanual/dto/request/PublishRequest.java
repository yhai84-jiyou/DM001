package com.helpmanual.dto.request;

public record PublishRequest(
        String versionLabel,
        String changeNotes
) {}
