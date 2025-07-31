package com.terrain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * PriorityType 枚举类用于定义瓦片优先级的类型。
 * 不同的优先级类型可用于在处理瓦片时决定处理顺序。
 */
@Getter
@RequiredArgsConstructor
public enum PriorityType {
    /**
     * 分辨率优先级。
     * 表示在处理瓦片时，将优先处理高分辨率的瓦片。
     */
    RESOLUTION("resolution"),
    /**
     * 更高值优先级。
     * 表示在处理瓦片时，将优先处理具有更高特定值（具体取决于业务逻辑）的瓦片。
     */
    HIGHER_VALUE("higher");

    /**
     * 与优先级类型对应的参数名称，用于通过字符串参数指定优先级类型。
     */
    private final String argumentName;

    /**
     * 根据传入的字符串查找对应的 PriorityType 枚举实例。
     * 该方法会忽略字符串的大小写进行匹配。
     *
     * @param text 用于匹配优先级类型的字符串
     * @return 匹配到的 PriorityType 枚举实例，如果未匹配到则返回默认的 RESOLUTION 类型
     */
    public static PriorityType fromString(String text) {
        // 遍历所有的 PriorityType 枚举值
        for (PriorityType type : PriorityType.values()) {
            // 忽略大小写比较传入的字符串和枚举值的 argumentName
            if (type.argumentName.equalsIgnoreCase(text)) {
                return type;
            }
        }
        // 未匹配到则返回默认的 RESOLUTION 类型
        return RESOLUTION;
    }
}