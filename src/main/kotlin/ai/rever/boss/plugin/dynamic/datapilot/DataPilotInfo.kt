package ai.rever.boss.plugin.dynamic.datapilot

import ai.rever.boss.plugin.api.Panel.Companion.bottom
import ai.rever.boss.plugin.api.Panel.Companion.left
import ai.rever.boss.plugin.api.PanelId
import ai.rever.boss.plugin.api.PanelInfo
import compose.icons.FeatherIcons
import compose.icons.feathericons.BarChart2

/** Describes the DataPilot panel: its id, sidebar icon, and default slot. */
object DataPilotInfo : PanelInfo {

    override val id = PanelId("datapilot", 50)

    override val displayName = "DataPilot"

    override val icon = FeatherIcons.BarChart2

    override val defaultSlotPosition = left.bottom
}