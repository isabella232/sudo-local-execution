package com.rundeck.plugin

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl
import com.dtolabs.rundeck.core.execution.utils.ResolverUtil
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

@Plugin(service= ServiceNameConstants.NodeExecutor,name=LocalNodeExecutor.SERVICE_PROVIDER_NAME)
@PluginDescription(title="LocalNodeExecutor", description="Local Node Executor")
class LocalNodeExecutor extends LocalExecutionBase  implements NodeExecutor, Describable{
    static final String SERVICE_PROVIDER_NAME = "sudo-local-node-executor"
    public static final String PROJ_PROP_PREFIX = "project."
    public static final String FWK_PROP_PREFIX = "framework."
    public static final String SUDO_PASSWORD = "sudo-password-path"
    public static final String DRY_RUN = "dryRun"

    final static Map<String, Object> renderingOptionsAuthenticationPassword = LocalUtil.getRenderOpt(false, true)

    @Override
    Description getDescription() {
        DescriptionBuilder builder = DescriptionBuilder.builder()
                                                       .name(SERVICE_PROVIDER_NAME)
                                                       .title("Sudo / Local / Node Executor")
                                                       .description("Local Node Executor, it can run commands with different users")
                                                       .property(PropertyBuilder.builder()
                                                                                .string(SUDO_PASSWORD)
                                                                                .title("Sudo Password")
                                                                                .description("Sudo Password, it can be set on with the local node attribute called sudo-password-path ")
                                                                                .required(false)
                                                                                .renderingOptions(renderingOptionsAuthenticationPassword)
                                                                                .build())
                                                       .property(PropertyBuilder.builder()
                                                                                .booleanType(DRY_RUN)
                                                                                .title("Dry Run")
                                                                                .description("Dry Run?")
                                                                                .required(false)
                                                                                .defaultValue("false")
                                                                                .build())

        //mapping config input on project and framework level
        builder.mapping(SUDO_PASSWORD, PROJ_PROP_PREFIX + SUDO_PASSWORD)
        builder.frameworkMapping(SUDO_PASSWORD, FWK_PROP_PREFIX + SUDO_PASSWORD)

        builder.mapping(DRY_RUN, PROJ_PROP_PREFIX + DRY_RUN)
        builder.frameworkMapping(DRY_RUN, FWK_PROP_PREFIX + DRY_RUN)

        return builder.build()
    }

    @Override
    NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command, final INodeEntry node) {
        String rundeckHostName=context.framework.properties.get("frameworkNodeName")
        this.logger= context.getExecutionLogger()
        this.rdeckBase = context.framework.properties.get("baseDir")

        shell="/bin/bash"

        if(node.nodename != rundeckHostName){
            return NodeExecutorResultImpl.createFailure(
                    StepFailureReason.ConfigurationFailure,
                    "[sudo-local-node-executor] Cannot run on node '" + node.getNodename() + ", it is not the local server'",
                    node
            )
        }

        if(node.attributes?.get("username")){
            username = node.attributes.get("username")
        }

        if(node.attributes?.get("shell")){
            shell = node.attributes.get("shell")
        }

        String sudoPasswordPath = ResolverUtil.resolveProperty(SUDO_PASSWORD,
                                                                null,
                                                                node,
                                                                context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                                                                context.getFramework())

        Boolean dryRun = ResolverUtil.resolveBooleanProperty(DRY_RUN,
                                                                false,
                                                                node,
                                                                context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                                                                context.getFramework())


        logger.log(dryRun?2:5, "[debug] Running Command: " + command + " as user " + username)

        def localuser = System.getProperty("user.name")
        if(username && username!=localuser) {
            sudoIsRequested = true
            if (!sudoPasswordPath) {
                return NodeExecutorResultImpl.createFailure(
                        StepFailureReason.ConfigurationFailure,
                        "[sudo-local-node-executor] Sudo password not set",
                        node
                )
            }

            sudoPassword = LocalUtil.getFromKeyStorage(sudoPasswordPath, context)

        }

        if(!dryRun){
            int exitValue = process(command)

            if(exitValue!=0){
                return NodeExecutorResultImpl.createFailure(
                        StepFailureReason.ConfigurationFailure,
                        "[sudo-local-node-executor] Error running command",
                        node
                )
            }else{
                return NodeExecutorResultImpl.createSuccess(node);
            }
        }

        return null
    }
}
