package com.rundeck.plugin

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopierException
import com.dtolabs.rundeck.core.execution.utils.ResolverUtil
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder


@Plugin(service= ServiceNameConstants.FileCopier,name=LocalFileCopier.SERVICE_PROVIDER_NAME)
@PluginDescription(title="LocalFileCopier", description="Local File Copier")
class LocalFileCopier extends BaseFileCopier implements FileCopier, Describable{
    static final String SERVICE_PROVIDER_NAME = "sudo-local-file-copier"
    public static final String PROJ_PROP_PREFIX = "project."
    public static final String FWK_PROP_PREFIX = "framework."
    public static final String SUDO_PASSWORD = "sudo-password-path"

    ExecutionLogger logger

    final static Map<String, Object> renderingOptionsAuthenticationPassword = LocalUtil.getRenderOpt(false, true)

    @Override
    Description getDescription() {
        DescriptionBuilder builder = DescriptionBuilder.builder()
                                                       .name(SERVICE_PROVIDER_NAME)
                                                       .title("Sudo / Local / File Copier")
                                                       .description("Local File Copier, it can run commands with different users")
                                                       .property(PropertyBuilder.builder()
                                                                               .string(SUDO_PASSWORD)
                                                                               .title("Sudo Password")
                                                                               .description("Sudo Password, it can be set on with the local node attribute called sudo-password-path ")
                                                                               .required(false)
                                                                               .renderingOptions(renderingOptionsAuthenticationPassword)
                                                                               .build())

        //mapping config input on project and framework level
        builder.mapping(SUDO_PASSWORD, PROJ_PROP_PREFIX + SUDO_PASSWORD)
        builder.frameworkMapping(SUDO_PASSWORD, FWK_PROP_PREFIX + SUDO_PASSWORD)


        return builder.build()
    }

    @Override
    String copyFileStream(
            final ExecutionContext context,
            final InputStream input,
            final INodeEntry node,
            final String destination
    ) throws FileCopierException {

        logger = context.getExecutionListener()

        File copiedFile = BaseFileCopier.writeLocalFile(
                null,
                input,
                null,
                null != destination ? new File(destination) : null
        )

        this.setUserPermission(node, context, copiedFile.absolutePath)

        return copiedFile.absolutePath
    }

    @Override
    String copyFile(final ExecutionContext context, final File file, final INodeEntry node, final String destination)
            throws FileCopierException {

        logger = context.getExecutionListener()

        File copiedFile = BaseFileCopier.writeLocalFile(
                file,
                null,
                null,
                null != destination ? new File(destination) : null
        )

        this.setUserPermission(node, context, copiedFile.absolutePath)

        return copiedFile.absolutePath
    }

    @Override
    String copyScriptContent(
            final ExecutionContext context,
            final String script,
            final INodeEntry node,
            final String destination
    ) throws FileCopierException {

        logger = context.getExecutionListener()

        File copiedFile = BaseFileCopier.writeLocalFile(
                null,
                null,
                script,
                null != destination ? new File(destination) : null
        )

        this.setUserPermission(node, context, copiedFile.absolutePath)

        return destination

    }


    def setUserPermission(INodeEntry node, ExecutionContext context, String destination){

        def localuser = System.getProperty("user.name")
        boolean sudoIsRequested = false
        def shell="/bin/bash"
        def username

        String sudoPasswordPath = ResolverUtil.resolveProperty(SUDO_PASSWORD,
                                                               null,
                                                               node,
                                                               context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                                                               context.getFramework())

        if(node.attributes?.get("username")){
            username = node.attributes.get("username")
        }
        if(node.attributes?.get("shell")){
            shell = node.attributes.get("shell")
        }
        def sudoPassword
        if(username && username!=localuser) {
            sudoIsRequested = true
            sudoPassword = LocalUtil.getFromKeyStorage(sudoPasswordPath, context)
        }

        //if username is defined on the local node, the command will run as another user
        if(sudoIsRequested){
            int exitValue = LocalUtil.setUserPermission(shell,
                                                        username,
                                                        new File(destination),
                                                        sudoPassword,
                                                        sudoIsRequested,
                                                        logger)
            logger.log(5,"[debug] Exit Code Permissions: "+ exitValue)

        }
    }


}
