package com.banking_application.dto;

import java.time.Instant;

public record RevisionInfoDto(Number revisionNumber, Instant revisionTimestamp) {}
