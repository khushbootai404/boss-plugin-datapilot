package ai.rever.boss.plugin.dynamic.datapilot

import ai.rever.boss.plugin.api.DynamicPlugin
import ai.rever.boss.plugin.api.PluginContext

/**
 * DataPilot — dataset intelligence for BOSS.
 *
 * The host instantiates this class and calls [register] when the plugin loads.
 */
class DataPilotDynamicPlugin : DynamicPlugin {

    override val pluginId = "ai.rever.boss.plugin.dynamic.datapilot"
    override val displayName = "DataPilot"
    override val version = "0.1.0"
    override val description = "Dataset intelligence and data quality analysis for BOSS"
    override val author = "Khushboo"
override val url = "https://github.com/khushbootai404/boss-plugin-datapilot"

    override fun register(context: PluginContext) {
        context.panelRegistry.registerPanel(DataPilotInfo) { ctx, panelInfo ->
            DataPilotComponent(ctx, panelInfo)
        }

        context.registerMcpToolProvider(
            DataPilotMcpTools(pluginId)
        )
    }

    override fun dispose() {
        // Release any resources here when the plugin is unloaded.
    }
}