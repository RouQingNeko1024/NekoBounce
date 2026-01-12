package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.block.BlockSlab
import net.minecraft.block.BlockSlime
import net.minecraft.block.BlockStairs
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos

object NekoBufferSpeed : Module("NekoBufferSpeed", Category.NEKO) {
    private val speedLimit by boolean("SpeedLimit", true)
    private val maxSpeed by float("MaxSpeed", 2f, 1f..5f)

    private val buffer by boolean("Buffer", true)

    private val stairs by boolean("Stairs", true)
    private val stairsMode by choices("StairsMode", arrayOf("Old", "New", "Coordinate"), "New")
    private val stairsBoost by float("StairsBoost", 1.87f, 1f..2f)
    private val stairsCoordinateBoost by float("StairsCoordinateBoost", 1.5f, 1f..2f)

    private val slabs by boolean("Slabs", true)
    private val slabsMode by choices("SlabsMode", arrayOf("Old", "New", "Coordinate"), "New")
    private val slabsBoost by float("SlabsBoost", 1.87f, 1f..2f)
    private val slabsCoordinateBoost by float("SlabsCoordinateBoost", 1.5f, 1f..2f)

    private val ice by boolean("Ice", false)
    private val iceBoost by float("IceBoost", 1.342f, 1f..2f)

    private val snow by boolean("Snow", true)
    private val snowBoost by float("SnowBoost", 1.87f, 1f..2f)
    private val snowPort by boolean("SnowPort", true)

    private val wall by boolean("Wall", true)
    private val wallMode by choices("WallMode", arrayOf("Old", "New", "FootWall"), "New")
    private val wallBoost by float("WallBoost", 1.87f, 1f..2f)
    private val footWallBoost by float("FootWallBoost", 1.5f, 1f..2f)

    private val headBlock by boolean("HeadBlock", true)
    private val headBlockBoost by float("HeadBlockBoost", 1.87f, 1f..2f)

    private val nonFullBlock by boolean("NonFullBlock", true)
    private val nonFullBlockBoost by float("NonFullBlockBoost", 1.5f, 1f..2f)

    private val slime by boolean("Slime", true)
    private val airStrafe by boolean("AirStrafe", false)
    private val noHurt by boolean("NoHurt", true)

    private var speed = 0.0
    private var down = false
    private var forceDown = false
    private var fastHop = false
    private var hadFastHop = false
    private var legitHop = false

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (noHurt && thePlayer.hurtTime > 0) {
            reset()
            return@handler
        }

        val blockPos = BlockPos(thePlayer)

        if (forceDown || down && thePlayer.motionY == 0.0) {
            thePlayer.motionY = -1.0
            down = false
            forceDown = false
        }

        if (fastHop) {
            thePlayer.speedInAir = 0.0211f
            hadFastHop = true
        } else if (hadFastHop) {
            thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }

        if (!thePlayer.isMoving || thePlayer.isSneaking || thePlayer.isInWater || mc.gameSettings.keyBindJump.isKeyDown) {
            reset()
            return@handler
        }

        if (thePlayer.onGround) {
            fastHop = false

            // 非完整方块坐标检测
            if (nonFullBlock && isOnNonFullBlock && !isIntegerCoordinate) {
                boost(nonFullBlockBoost)
                return@handler
            }

            if (slime && (blockPos.down().block is BlockSlime || blockPos.block is BlockSlime)) {
                thePlayer.tryJump()

                thePlayer.motionX = thePlayer.motionY * 1.132
                thePlayer.motionY = 0.08
                thePlayer.motionZ = thePlayer.motionY * 1.132

                down = true
                return@handler
            }
            if (slabs && blockPos.block is BlockSlab) {
                when (slabsMode.lowercase()) {
                    "old" -> {
                        boost(slabsBoost)
                        return@handler
                    }
                    "new" -> {
                        fastHop = true
                        if (legitHop) {
                            thePlayer.tryJump()
                            thePlayer.onGround = false
                            legitHop = false
                            return@handler
                        }
                        thePlayer.onGround = false

                        strafe(0.375f)

                        thePlayer.tryJump()
                        thePlayer.motionY = 0.41
                        return@handler
                    }
                    "coordinate" -> {
                        if (!isIntegerCoordinate) {
                            boost(slabsCoordinateBoost)
                            return@handler
                        }
                    }
                }
            }
            if (stairs && (blockPos.down().block is BlockStairs || blockPos.block is BlockStairs)) {
                when (stairsMode.lowercase()) {
                    "old" -> {
                        boost(stairsBoost)
                        return@handler
                    }
                    "new" -> {
                        fastHop = true

                        if (legitHop) {
                            thePlayer.tryJump()
                            thePlayer.onGround = false
                            legitHop = false
                            return@handler
                        }

                        thePlayer.onGround = false
                        strafe(0.375f)
                        thePlayer.tryJump()
                        thePlayer.motionY = 0.41
                        return@handler
                    }
                    "coordinate" -> {
                        if (!isIntegerCoordinate) {
                            boost(stairsCoordinateBoost)
                            return@handler
                        }
                    }
                }
            }
            legitHop = true

            if (headBlock && blockPos.up(2).block != Blocks.air) {
                boost(headBlockBoost)
                return@handler
            }

            if (ice && blockPos.down().block.let { it == Blocks.ice || it == Blocks.packed_ice }) {
                boost(iceBoost)
                return@handler
            }

            if (snow && blockPos.block == Blocks.snow_layer && (snowPort || thePlayer.posY - thePlayer.posY.toInt() >= 0.12500)) {
                if (thePlayer.posY - thePlayer.posY.toInt() >= 0.12500) {
                    boost(snowBoost)
                } else {
                    thePlayer.tryJump()
                    forceDown = true
                }
                return@handler
            }

            if (wall) {
                when (wallMode.lowercase()) {
                    "old" -> if (thePlayer.isCollidedVertically && isNearBlock || BlockPos(thePlayer).up(2).block != Blocks.air) {
                        boost(wallBoost)
                        return@handler
                    }
                    "new" -> if (isNearBlock && !thePlayer.movementInput.jump) {
                        thePlayer.tryJump()
                        thePlayer.motionY = 0.08
                        thePlayer.motionX *= 0.99
                        thePlayer.motionZ *= 0.99
                        down = true
                        return@handler
                    }
                    "footwall" -> if (isFootWall) {
                        boost(footWallBoost)
                        return@handler
                    }
                }
            }
            val currentSpeed = speed

            if (speed < currentSpeed)
                speed = currentSpeed

            if (buffer && speed > 0.2) {
                speed /= 1.0199999809265137
                strafe()
            }
        } else {
            speed = 0.0

            if (airStrafe)
                strafe()
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is S08PacketPlayerPosLook)
            speed = 0.0
    }

    override fun onEnable() = reset()

    override fun onDisable() = reset()

    private fun reset() {
        val thePlayer = mc.thePlayer ?: return
        legitHop = true
        speed = 0.0

        if (hadFastHop) {
            thePlayer.speedInAir = 0.02f
            hadFastHop = false
        }
    }

    private fun boost(boost: Float) {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.motionX *= boost
        thePlayer.motionZ *= boost

        speed = MovementUtils.speed.toDouble()

        if (speedLimit && speed > maxSpeed)
            speed = maxSpeed.toDouble()
    }

    private val isNearBlock: Boolean
        get() {
            val thePlayer = mc.thePlayer ?: return false
            val theWorld = mc.theWorld ?: return false
            val blocks = arrayOf(
                BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ - 0.7),
                BlockPos(thePlayer.posX + 0.7, thePlayer.posY + 1, thePlayer.posZ),
                BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ + 0.7),
                BlockPos(thePlayer.posX - 0.7, thePlayer.posY + 1, thePlayer.posZ)
            )

            for (blockPos in blocks) {
                val blockState = theWorld.getBlockState(blockPos)

                val collisionBoundingBox = blockState.block.getCollisionBoundingBox(theWorld, blockPos, blockState)

                if ((collisionBoundingBox == null || collisionBoundingBox.maxX ==
                            collisionBoundingBox.minY + 1) &&
                    !blockState.block.isTranslucent && blockState.block == Blocks.water &&
                    blockState.block !is BlockSlab || blockState.block == Blocks.barrier
                ) return true
            }
            return false
        }

    private val isFootWall: Boolean
        get() {
            val thePlayer = mc.thePlayer ?: return false
            val theWorld = mc.theWorld ?: return false
            val footPos = BlockPos(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ)
            val blockState = theWorld.getBlockState(footPos)
            
            return !blockState.block.isTranslucent && 
                   blockState.block != Blocks.air && 
                   blockState.block != Blocks.water && 
                   blockState.block != Blocks.flowing_water
        }

    private val isOnNonFullBlock: Boolean
        get() {
            val thePlayer = mc.thePlayer ?: return false
            val theWorld = mc.theWorld ?: return false
            val blockPos = BlockPos(thePlayer)
            val blockState = theWorld.getBlockState(blockPos)
            
            val collisionBox = blockState.block.getCollisionBoundingBox(theWorld, blockPos, blockState)
            return collisionBox == null || collisionBox.maxY - collisionBox.minY < 1.0
        }

    private val isIntegerCoordinate: Boolean
        get() {
            val thePlayer = mc.thePlayer ?: return true
            return thePlayer.posY - thePlayer.posY.toInt() == 0.0
        }
}