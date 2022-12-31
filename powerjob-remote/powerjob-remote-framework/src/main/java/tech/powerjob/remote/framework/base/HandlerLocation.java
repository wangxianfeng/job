package tech.powerjob.remote.framework.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * handler location
 *
 * @author tjq
 * @since 2022/12/31
 */
@Getter
@Setter
@Accessors(chain = true)
public class HandlerLocation implements Serializable {
    /**
     * 根路径
     */
    private String rootPath;
    /**
     * 方法路径
     */
    private String methodPath;
}