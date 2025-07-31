package com.terrain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.media.jai.Interpolation;

@Getter
@RequiredArgsConstructor
public enum InterpolationType {

    /**
     * 最近邻插值，使用离目标点最近的像素值作为插值结果。
     * 该方法计算速度快，但可能会导致图像边缘出现锯齿状。
     */
    NEAREST(Interpolation.INTERP_NEAREST, "nearest"),
    /**
     * 双线性插值，通过对目标点周围四个像素进行线性插值来计算结果。
     * 该方法计算量适中，图像质量比最近邻插值好，能有效减少锯齿现象。
     */
    BILINEAR(Interpolation.INTERP_BILINEAR, "bilinear");

    /**
     * 插值类型对应的 JAI (Java Advanced Imaging) 库中的插值常量。
     */
    private final int interpolation;
    /**
     * 插值类型对应的字符串参数，用于通过字符串查找对应的插值类型。
     */
    private final String interpolationArgument;

    /**
     * 根据传入的插值类型字符串参数，返回对应的 InterpolationType 枚举实例。
     *
     * @param interpolationArgument 插值类型的字符串参数
     * @return 对应的 InterpolationType 枚举实例
     * @throws IllegalArgumentException 如果传入的参数没有匹配的插值类型，则抛出此异常
     */
    public static InterpolationType fromString(String interpolationArgument) {
        for (InterpolationType interpolationType : values()) {
            if (interpolationType.interpolationArgument.equals(interpolationArgument)) {
                return interpolationType;
            }
        }
        throw new IllegalArgumentException("无效的插值类型: " + interpolationArgument);
    }
}
