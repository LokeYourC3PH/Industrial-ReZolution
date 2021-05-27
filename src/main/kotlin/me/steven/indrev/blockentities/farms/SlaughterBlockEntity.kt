package me.steven.indrev.blockentities.farms

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import me.steven.indrev.api.machines.Tier
import me.steven.indrev.blockentities.crafters.UpgradeProvider
import me.steven.indrev.config.BasicMachineConfig
import me.steven.indrev.inventories.inventory
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.FakePlayerEntity
import me.steven.indrev.utils.redirectDrops
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.SwordItem
import net.minecraft.server.world.ServerWorld

class SlaughterBlockEntity(tier: Tier) : AOEMachineBlockEntity<BasicMachineConfig>(tier, MachineRegistry.SLAUGHTER_REGISTRY), UpgradeProvider {

    override val backingMap: Object2IntMap<Upgrade> = Object2IntArrayMap()
    override val upgradeSlots: IntArray = intArrayOf(11, 12, 13, 14)
    override val availableUpgrades: Array<Upgrade> = arrayOf(Upgrade.SPEED, Upgrade.ENERGY, Upgrade.BUFFER, Upgrade.DAMAGE)

    init {
        this.inventoryComponent = inventory(this) {
            input { slot = 1 }
            output { slots = intArrayOf(2, 3, 4, 5, 6, 7, 8, 9, 10) }
        }
    }

    override val maxInput: Double = config.maxInput
    override val maxOutput: Double = 0.0

    var cooldown = 0.0
    override var range = 5
    private val fakePlayer by lazy { FakePlayerEntity(world as ServerWorld, pos) }

    override fun machineTick() {
        if (world?.isClient == true) return
        val inventory = inventoryComponent?.inventory ?: return
        val upgrades = getUpgrades(inventory)
        cooldown += Upgrade.getSpeed(upgrades, this)
        if (cooldown < config.processSpeed) return
        val mobs = world?.getEntitiesByClass(LivingEntity::class.java, getWorkingArea()) { e -> (e !is PlayerEntity && e !is ArmorStandEntity && !e.isDead) }
            ?: emptyList()
        val energyCost = Upgrade.getEnergyCost(upgrades, this)
        if (mobs.isEmpty() || !canUse(energyCost)) {
            workingState = false
            return
        } else workingState = true
        val swordStack = inventory.inputSlots.map { inventory.getStack(it) }.firstOrNull { it.item is SwordItem }
        fakePlayer.inventory.selectedSlot = 0
        if (swordStack != null && !swordStack.isEmpty && swordStack.damage < swordStack.maxDamage) {
            val swordItem = swordStack.item as SwordItem
            use(energyCost)
            mobs.forEach { mob ->
                swordStack.damage(1, world?.random, null)
                if (swordStack.damage >= swordStack.maxDamage) swordStack.decrement(1)

                mob.redirectDrops(inventory) {
                    mob.damage(DamageSource.player(fakePlayer), (swordItem.attackDamage * Upgrade.getDamageMultiplier(upgrades, this)).toFloat())
                }
            }
        }
        fakePlayer.inventory.clear()
        cooldown = 0.0
    }

    override fun getBaseValue(upgrade: Upgrade): Double =
        when (upgrade) {
            Upgrade.ENERGY -> config.energyCost
            Upgrade.SPEED -> 1.0
            Upgrade.BUFFER -> config.maxEnergyStored
            else -> 0.0
        }

    override fun getMaxUpgrade(upgrade: Upgrade): Int {
        return if (upgrade == Upgrade.SPEED || upgrade == Upgrade.DAMAGE) return 1 else super.getMaxUpgrade(upgrade)
    }

    override fun getEnergyCapacity(): Double = Upgrade.getBuffer(this)
}