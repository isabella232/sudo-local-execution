package com.rundeck.plugin

import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

@Plugin(service= ServiceNameConstants.WorkflowStep,name=LocalInlineScriptStep.SERVICE_PROVIDER_NAME)
@PluginDescription(title="LocalInlineScriptStep", description="Run script locally")
class LocalInlineScriptStep extends LocalExecutionBase implements StepPlugin, Describable{
    static final String SERVICE_PROVIDER_NAME = "sudo-local-script-step"

    final static Map<String, Object> renderingOptionsAuthenticationPassword = LocalUtil.getRenderOpt(false, true)

    @Override
    Description getDescription() {
        return DescriptionBuilder.builder()
                                 .name(SERVICE_PROVIDER_NAME)
                                 .title("Sudo / Local / Run Script")
                                 .description("Run local Script as user")
                                 .property(PropertyBuilder.builder()
                                                           .string("script")
                                                           .title("Inline Script")
                                                           .description("Script to run")
                                                           .required(true)
                                                          .renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, "CODE")
                                                           .renderingOption(StringRenderingConstants.CODE_SYNTAX_MODE,"bash")
                                                           .build())
                                 .property(PropertyBuilder.builder()
                                                          .string("sudoPassword")
                                                          .title("Sudo Password")
                                                          .description("Sudo Password, it can be set on with the local node attribute called sudo-password-path ")
                                                          .required(false)
                                                          .renderingOptions(renderingOptionsAuthenticationPassword)
                                                          .build())
                                 .property(PropertyBuilder.builder()
                                                          .string("login")
                                                          .title("Login")
                                                          .description("Use login shell?")
                                                          .required(false)
                                                          .build())
                                 .build()
    }

    @Override
    void executeStep(final PluginStepContext context, final Map<String, Object> configuration) throws StepException {
        this.logger=context.getExecutionContext().getExecutionListener()
        this.rdeckBase = context.framework.properties.get("baseDir")

        String script = configuration.get("script")
        login = Boolean.valueOf(configuration.get("login"))
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

        String sudoPasswordPath = !configuration.get("sudoPassword")?rundeckServer?.attributes.get("sudo-password-path"):configuration.get("sudoPassword")

        def localuser = System.getProperty("user.name")
        //if username is defined on the local node, the command will run as another user
        if(username && username!=localuser){
            sudoIsRequested = true
            if (!sudoPasswordPath) {
                throw new StepException("Sudo password not set", LocalStepReason.SudoPasswordError)
            } else {
                sudoPassword = LocalUtil.getFromKeyStorage(sudoPasswordPath, context)
            }
        }

        int exitValue = process(script)

        if (exitValue!=0) {
            throw new StepException("Error running the script", Reason.ExecutionError)
        }
    }

}
