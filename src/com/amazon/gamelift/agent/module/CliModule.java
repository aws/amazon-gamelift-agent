/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 */
package com.amazon.gamelift.agent.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import dagger.Module;
import dagger.Provides;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

/**
 * Module to provide the dependencies for the cli parser.
 */
@Module
public class CliModule {
    /**
     * Provides CommandLineParser
     * @return
     */
    @Provides
    public CommandLineParser provideCommandLineParser() {
        return new DefaultParser();
    }

    /**
     * Provides HelpFormatter
     * @return
     */
    @Provides
    public HelpFormatter provideHelpFormatter() {
        return new HelpFormatter();
    }

    /**
     * Provides ObjectMapper
     * @return
     */
    @Provides
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }
}
