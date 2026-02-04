package net.ccbluex.liquidbounce.utils.firefly.general.distance

import kotlin.math.*
import net.minecraft.util.Vec3
import net.minecraft.entity.Entity
/**
 * 点类型，表示三维空间中的一个点
 * @param x X坐标
 * @param y Y坐标
 * @param z Z坐标
 */
data class Point(val x: Double, val y: Double, val z: Double) {

    // 零向量
    companion object {
        val ZERO = Point(0.0, 0.0, 0.0)
        val ONE = Point(1.0, 1.0, 1.0)

        /**
         * 从Minecraft的Vec3创建Point
         */
        fun fromVec3(vec3: Vec3): Point {
            return Point(vec3.xCoord, vec3.yCoord, vec3.zCoord)
        }

        /**
         * 从数组创建Point
         */
        fun fromArray(array: DoubleArray): Point {
            require(array.size >= 3) { "数组必须至少包含3个元素" }
            return Point(array[0], array[1], array[2])
        }

        /**
         * 从列表创建Point
         */
        fun fromList(list: List<Double>): Point {
            require(list.size >= 3) { "列表必须至少包含3个元素" }
            return Point(list[0], list[1], list[2])
        }

        /**
         * 计算多个点的中心点
         */
        fun centroid(vararg points: Point): Point {
            if (points.isEmpty()) return ZERO
            var sumX = 0.0
            var sumY = 0.0
            var sumZ = 0.0
            for (point in points) {
                sumX += point.x
                sumY += point.y
                sumZ += point.z
            }
            return Point(sumX / points.size, sumY / points.size, sumZ / points.size)
        }

        /**
         * 线性插值
         */
        fun lerp(start: Point, end: Point, t: Double): Point {
            val clampedT = t.coerceIn(0.0, 1.0)
            return Point(
                start.x + (end.x - start.x) * clampedT,
                start.y + (end.y - start.y) * clampedT,
                start.z + (end.z - start.z) * clampedT
            )
        }
    }

    /**
     * 转换为Vec3
     */
    fun toVec3(): Vec3 {
        return Vec3(x, y, z)
    }

    /**
     * 转换为数组
     */
    fun toArray(): DoubleArray {
        return doubleArrayOf(x, y, z)
    }

    /**
     * 转换为列表
     */
    fun toList(): List<Double> {
        return listOf(x, y, z)
    }

    /**
     * 转换为字符串，可选精度
     */
    fun toString(precision: Int = 2): String {
        val factor = 10.0.pow(precision)
        val formattedX = (x * factor).roundToInt() / factor
        val formattedY = (y * factor).roundToInt() / factor
        val formattedZ = (z * factor).roundToInt() / factor
        return "($formattedX, $formattedY, $formattedZ)"
    }

    /**
     * 点加向量
     */
    operator fun plus(other: Point): Point {
        return Point(x + other.x, y + other.y, z + other.z)
    }

    /**
     * 点减向量
     */
    operator fun minus(other: Point): Point {
        return Point(x - other.x, y - other.y, z - other.z)
    }

    /**
     * 点乘标量
     */
    operator fun times(scalar: Double): Point {
        return Point(x * scalar, y * scalar, z * scalar)
    }

    /**
     * 点除标量
     */
    operator fun div(scalar: Double): Point {
        require(scalar != 0.0) { "除数不能为零" }
        return Point(x / scalar, y / scalar, z / scalar)
    }

    /**
     * 点乘（内积）
     */
    infix fun dot(other: Point): Double {
        return x * other.x + y * other.y + z * other.z
    }

    /**
     * 叉乘（外积）
     */
    infix fun cross(other: Point): Point {
        return Point(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }

    /**
     * 计算到另一个点的距离
     */
    fun distanceTo(other: Point): Double {
        return sqrt(distanceSquaredTo(other))
    }

    /**
     * 计算到另一个点的平方距离（性能更好）
     */
    fun distanceSquaredTo(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return dx * dx + dy * dy + dz * dz
    }

    /**
     * 计算水平距离（忽略Y轴）
     */
    fun horizontalDistanceTo(other: Point): Double {
        val dx = x - other.x
        val dz = z - other.z
        return sqrt(dx * dx + dz * dz)
    }

    /**
     * 计算垂直距离（只考虑Y轴）
     */
    fun verticalDistanceTo(other: Point): Double {
        return abs(y - other.y)
    }

    /**
     * 计算向量的长度（模）
     */
    fun length(): Double {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * 计算向量的平方长度（性能更好）
     */
    fun lengthSquared(): Double {
        return x * x + y * y + z * z
    }

    /**
     * 归一化（单位向量）
     */
    fun normalized(): Point {
        val len = length()
        return if (len > 0) this / len else ZERO
    }

    /**
     * 限制向量的最大长度
     */
    fun clampLength(maxLength: Double): Point {
        val len = length()
        return if (len > maxLength) this * (maxLength / len) else this
    }

    /**
     * 绕Y轴旋转（水平旋转）
     */
    fun rotateYaw(yaw: Double): Point {
        val radians = Math.toRadians(yaw)
        val cos = cos(radians)
        val sin = sin(radians)
        return Point(
            x * cos - z * sin,
            y,
            x * sin + z * cos
        )
    }

    /**
     * 绕X轴旋转（俯仰旋转）
     */
    fun rotatePitch(pitch: Double): Point {
        val radians = Math.toRadians(pitch)
        val cos = cos(radians)
        val sin = sin(radians)
        return Point(
            x,
            y * cos - z * sin,
            y * sin + z * cos
        )
    }

    /**
     * 绕Z轴旋转（滚转旋转）
     */
    fun rotateRoll(roll: Double): Point {
        val radians = Math.toRadians(roll)
        val cos = cos(radians)
        val sin = sin(radians)
        return Point(
            x * cos - y * sin,
            x * sin + y * cos,
            z
        )
    }

    /**
     * 线性插值到另一个点
     */
    fun lerp(other: Point, t: Double): Point {
        return Point.lerp(this, other, t)
    }

    /**
     * 计算两点之间的中点
     */
    fun midpoint(other: Point): Point {
        return Point(
            (x + other.x) / 2,
            (y + other.y) / 2,
            (z + other.z) / 2
        )
    }

    /**
     * 判断是否在给定的矩形区域内（忽略Y轴）
     */
    fun inHorizontalRectangle(minX: Double, minZ: Double, maxX: Double, maxZ: Double): Boolean {
        return x in minX..maxX && z in minZ..maxZ
    }

    /**
     * 判断是否在给定的立方体区域内
     */
    fun inCuboid(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return x in minX..maxX && y in minY..maxY && z in minZ..maxZ
    }

    /**
     * 判断是否在球体内
     */
    fun inSphere(center: Point, radius: Double): Boolean {
        return distanceSquaredTo(center) <= radius * radius
    }

    /**
     * 判断是否与另一点相等（考虑误差）
     */
    fun equals(other: Point, epsilon: Double = 1e-6): Boolean {
        return abs(x - other.x) < epsilon &&
                abs(y - other.y) < epsilon &&
                abs(z - other.z) < epsilon
    }

    override fun toString(): String {
        return toString(2)
    }
}

/**
 * 向量类型别名（与Point相同，但语义上表示方向）
 */
typealias Vector = Point

/**
 * 扩展函数：从实体位置创建Point
 */
fun Entity.toPoint(): Point {
    return Point(this.posX, this.posY, this.posZ)
}

/**
 * 扩展函数：从实体眼睛位置创建Point
 */
fun Entity.toEyePoint(): Point {
    return Point(this.posX, this.posY + this.getEyeHeight(), this.posZ)
}

/**
 * 扩展函数：计算实体到Point的距离
 */
fun Entity.distanceTo(point: Point): Double {
    val dx = this.posX - point.x
    val dy = this.posY - point.y
    val dz = this.posZ - point.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}

/**
 * 扩展函数：计算实体到Point的水平距离
 */
fun Entity.horizontalDistanceTo(point: Point): Double {
    val dx = this.posX - point.x
    val dz = this.posZ - point.z
    return sqrt(dx * dx + dz * dz)
}