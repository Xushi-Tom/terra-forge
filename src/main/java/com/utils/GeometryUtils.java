package com.utils;

/**
 * 几何计算工具类
 * 
 * <p>提供三维几何计算的基础方法，主要用于地形网格处理和三维地理计算。
 * 在量化网格算法和半边数据结构处理中，需要进行大量的向量运算和几何计算，
 * 此工具类提供了高性能的几何计算方法。</p>
 * 
 * <h3>主要应用场景：</h3>
 * <ul>
 *   <li>地形网格法向量计算</li>
 *   <li>三角形面积和角度计算</li>
 *   <li>顶点光照计算</li>
 *   <li>地形坡度和坡向分析</li>
 * </ul>
 * 
 * <p><b>性能优化</b>：所有方法都针对大批量数据处理进行了优化，
 * 避免了不必要的对象创建和复杂的数学函数调用。</p>
 * 
 * @author TerraForge开发团队
 * @version 2.8.0
 * @since 2025-01-01
 * @see com.terrain.common.HalfEdgeFace 半边面几何处理
 * @see com.terrain.geometry.TileMatrix 瓦片矩阵变换
 */
public class GeometryUtils {

    /**
     * 计算两个单位向量之间的余弦值
     * 
     * <p>计算两个三维单位向量的点积，结果就是它们夹角的余弦值。
     * 在地形处理中，此方法用于：</p>
     * <ul>
     *   <li>计算三角形法向量与光照方向的夹角</li>
     *   <li>判断地形面的朝向和倾斜度</li>
     *   <li>进行地形光照和阴影计算</li>
     *   <li>优化地形网格的视觉效果</li>
     * </ul>
     * 
     * <h4>数学原理：</h4>
     * <p>对于两个单位向量 <b>a</b> = (ax, ay, az) 和 <b>b</b> = (bx, by, bz)，
     * 它们的点积等于夹角θ的余弦值：</p>
     * <pre>
     * cos(θ) = <b>a</b> · <b>b</b> = ax*bx + ay*by + az*bz
     * </pre>
     * 
     * <p><b>前提条件</b>：输入的两个向量必须都是单位向量（长度为1），
     * 否则结果将不是准确的夹角余弦值。</p>
     * 
     * @param ax 第一个向量的X分量
     * @param ay 第一个向量的Y分量  
     * @param az 第一个向量的Z分量
     * @param bx 第二个向量的X分量
     * @param by 第二个向量的Y分量
     * @param bz 第二个向量的Z分量
     * @return 两个向量夹角的余弦值，范围为[-1, 1]
     *         <ul>
     *           <li>1：向量完全同向（夹角0°）</li>
     *           <li>0：向量垂直（夹角90°）</li>
     *           <li>-1：向量完全反向（夹角180°）</li>
     *         </ul>
     * 
     * @example
     * <pre>
     * // 计算地形法向量与垂直向上方向的夹角余弦值
     * double cosAngle = GeometryUtils.cosineBetweenUnitaryVectors(
     *     normalX, normalY, normalZ,  // 地形法向量
     *     0.0, 0.0, 1.0              // 垂直向上单位向量
     * );
     * 
     * // 根据余弦值判断地形倾斜程度
     * if (cosAngle > 0.866) {        // cos(30°) ≈ 0.866
     *     // 平缓地形（倾斜角度小于30°）
     * } else if (cosAngle > 0.5) {   // cos(60°) = 0.5
     *     // 中等倾斜（30°-60°）
     * } else {
     *     // 陡峭地形（倾斜角度大于60°）
     * }
     * </pre>
     */
    public static double cosineBetweenUnitaryVectors(double ax, double ay, double az, double bx, double by, double bz) {
        return ax * bx + ay * by + az * bz;
    }
}
