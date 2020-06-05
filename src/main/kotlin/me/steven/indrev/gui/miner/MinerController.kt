package me.steven.indrev.gui.miner

import io.github.cottonmc.cotton.gui.CottonCraftingController
import io.github.cottonmc.cotton.gui.widget.WGridPanel
import io.github.cottonmc.cotton.gui.widget.WItemSlot
import me.steven.indrev.blockentities.HeatMachineBlockEntity
import me.steven.indrev.blockentities.crafters.UpgradeProvider
import me.steven.indrev.gui.widgets.EnergyWidget
import me.steven.indrev.gui.widgets.StringWidget
import me.steven.indrev.gui.widgets.TemperatureInfoWidget
import me.steven.indrev.gui.widgets.TemperatureWidget
import me.steven.indrev.inventories.DefaultSidedInventory
import me.steven.indrev.utils.add
import me.steven.indrev.world.chunkveins.ChunkVeinType
import net.minecraft.client.resource.language.I18n
import net.minecraft.container.BlockContext
import net.minecraft.entity.player.PlayerInventory

class MinerController(syncId: Int, playerInventory: PlayerInventory, blockContext: BlockContext) :
    CottonCraftingController(null, syncId, playerInventory, getBlockInventory(blockContext), getBlockPropertyDelegate(blockContext)) {
    init {
        val root = WGridPanel()
        setRootPanel(root)
        root.setSize(150, 120)

        root.add(StringWidget(I18n.translate("block.indrev.miner"), titleColor), 4, 0)
        root.add(createPlayerInventoryPanel(), 0, 5)

        root.add(EnergyWidget(propertyDelegate), 0, 0, 16, 64)
        val batterySlot = WItemSlot.of(blockInventory, 0)
        root.add(batterySlot, 0.0, 3.7)

        var x = 2
        var y = 1
        (blockInventory as DefaultSidedInventory).outputSlots.forEach { slot ->
            if (x == 6) {
                x = 2
                y++
            }
            x++
            val itemSlot = WItemSlot.of(blockInventory, slot)
            root.add(itemSlot, x, y)
        }

        root.add(StringWidget({
            val typeId = propertyDelegate[3]
            val type = if (typeId >= 0) ChunkVeinType.values()[typeId] else null
            I18n.translate("block.indrev.miner.gui1", type)
        }, titleColor), 4.0, 3.5)
        root.add(StringWidget({
            I18n.translate("block.indrev.miner.gui2","${propertyDelegate[4]}%")
        }, titleColor), 4, 4)

        blockContext.run { world, pos ->
            val blockEntity = world.getBlockEntity(pos)
            if (blockEntity is UpgradeProvider) {
                for ((i, slot) in blockEntity.getUpgradeSlots().withIndex()) {
                    val s = WItemSlot.of(blockInventory, slot)
                    root.add(s, 8, i)
                }
            }
            if (blockEntity is HeatMachineBlockEntity) {
                root.add(TemperatureWidget(propertyDelegate, blockEntity), 1, 0, 16, 64)
                root.add(TemperatureInfoWidget(propertyDelegate, blockEntity), 2.0, 0.5, 8.0, 8.0)
                val coolerSlot = WItemSlot.of(blockInventory, 1)
                root.add(coolerSlot, 1.0, 3.7)
            }
        }

        root.validate(this)
    }
}