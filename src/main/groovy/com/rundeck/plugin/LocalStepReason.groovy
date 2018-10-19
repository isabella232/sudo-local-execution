package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason

/**
 * This enum lists the known reasons this plugin might fail
 */
enum LocalStepReason implements FailureReason{
    ExecutionError,
    SudoPasswordError
}