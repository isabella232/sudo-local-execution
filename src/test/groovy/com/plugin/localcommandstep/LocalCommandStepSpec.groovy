package com.plugin.localcommandstep

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.plugins.PluginLogger
import com.rundeck.plugin.LocalCommandStep
import spock.lang.Specification

class LocalCommandStepSpec extends Specification {

    def getContext(ExecutionListener logger){

        INodeSet nodeSet = new NodeSetImpl()

        def node = Mock(INodeEntry){
            getHostname()>>"localhost"
            getNodename()>>"localhost"
            getAttributes()>>["hostname":"localhost","osFamily":"linux","forceFail":"true","username":"test"]
        }

        nodeSet.putNode(node)

        Mock(PluginStepContext){
            getExecutionContext()>> Mock(ExecutionContext){
                getExecutionListener()>>logger
                getNodes()>>nodeSet
            }

            getFramework() >> Mock(Framework) {
                getProperty(_) >> "localhost"
            }
        }
    }


}