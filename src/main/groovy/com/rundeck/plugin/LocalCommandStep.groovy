package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

@Plugin(service= ServiceNameConstants.WorkflowStep,name=LocalCommandStep.SERVICE_PROVIDER_NAME)
@PluginDescription(title="LocalCommandStep", description="Run command locally")
class LocalCommandStep extends LocalExecutionBase implements StepPlugin, Describable{
    static final String SERVICE_PROVIDER_NAME = "sudo-local-command-step"

    final static Map<String, Object> renderingOptionsAuthenticationPassword = LocalUtil.getRenderOpt(false, true)

    @Override
    Description getDescription() {
        return DescriptionBuilder.builder()
                                 .name(SERVICE_PROVIDER_NAME)
                                 .title("Sudo / Local / Run Command")
                                 .description("Run local commands as user")
                                 .property(PropertyBuilder.builder()
                                                           .string("command")
                                                           .title("Command")
                                                           .description("Command to run")
                                                           .required(true)
                                                           .build())
                                 .property(PropertyBuilder.builder()
                                                          .string("sudoPassword")
                                                          .title("Sudo Password")
                                                          .description("Sudo Password, it can be set on with the local node attribute called sudo-password-path ")
                                                          .required(false)
                                                          .renderingOptions(renderingOptionsAuthenticationPassword)
                                                          .build())
                                 .property(PropertyBuilder.builder()
                                                          .booleanType("dryRun")
                                                          .title("Dry Run")
                                                          .description("Dry Run?")
                                                          .required(false)
                                                          .defaultValue("false")
                                                          .build())
                                 .build()
    }


    @Override
    void executeStep(final PluginStepContext context, final Map<String, Object> configuration) throws StepException {
        this.logger=context.getExecutionContext().getExecutionListener()
        this.rdeckBase = context.framework.properties.get("baseDir")

        String command = configuration.get("command")
        Boolean dryRun = Boolean.valueOf(configuration.get("dryRun"))
        shell="/bin/bash"

        String rundeckHostName=context.framework.properties.get("frameworkNodeName")
        def rundeckServer = context.executionContext.nodes.find{it.nodename==rundeckHostName}

        if(rundeckServer){
            if(rundeckServer.attributes?.get("username")){
                username = rundeckServer.attributes.get("username")
            }

            if(rundeckServer.attributes?.get("shell")){
                shell = rundeckServer.attributes.get("shell")
            }
        }

        def localuser = System.getProperty("user.name")
        String sudoPasswordPath = !configuration.get("sudoPassword")?rundeckServer?.attributes.get("sudo-password-path"):configuration.get("sudoPassword")

        //if username is defined on the local node, the command will run as another user
        if(username && username!=localuser){
            sudoIsRequested = true
            if (!sudoPasswordPath) {
                throw new StepException("Sudo password not set", LocalStepReason.SudoPasswordError)
            } else {
                sudoPassword = LocalUtil.getFromKeyStorage(sudoPasswordPath, context)
            }
        }

        logger.log(dryRun?2:5, "[debug] Running Command: " + command + " as user " + username)

        if(!dryRun){
            def exitValue = process(command)

            if (exitValue!=0) {
                throw new StepException("Error running the command", LocalStepReason.ExecutionError)
            }
        }
    }

}
