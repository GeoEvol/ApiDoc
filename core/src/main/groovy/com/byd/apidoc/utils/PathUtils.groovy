package com.byd.apidoc.utils

import java.nio.file.InvalidPathException

/**
 * 路径处理工具类
 * 提供路径规范化、解析等通用方法
 * @author qiao.zhi2
 */
class PathUtils {

    private PathUtils() {
        // 工具类不允许实例化
    }

    /**
     * 规范化路径，优先返回 canonicalPath，失败时返回 absolutePath
     *
     * @param dir 文件对象
     * @return 规范化后的路径字符串，如果 dir 为 null 则返回 null
     */
    static String normalizePath(File dir) {
        if (dir == null) {
            return null
        }
        try {
            String canonical = dir.canonicalPath
            return canonical != null ? canonical : dir.absolutePath
        } catch (IOException | InvalidPathException e) {
            return dir.absolutePath
        }
    }

    /**
     * 解析相对路径为绝对路径
     *
     * @param path 相对或绝对路径字符串
     * @param rootDir 根目录，用于解析相对路径
     * @return 解析后的绝对路径，如果路径无效或目录不存在则返回 null
     */
    static String resolvePath(String path, File rootDir) {
        if (path == null || path.trim().isEmpty()) {
            return null
        }

        File dir = new File(path).isAbsolute()
                ? new File(path)
                : new File(rootDir, path)

        if (dir.exists() && dir.isDirectory()) {
            return normalizePath(dir)
        }
        return null
    }

    /**
     * 解析路径列表，去重并返回有效路径
     *
     * @param paths 路径字符串列表
     * @param rootDir 根目录
     * @return 去重后的有效路径列表
     */
    static List<String> resolvePaths(List<String> paths, File rootDir) {
        List<String> validPaths = new ArrayList<>()
        Set<String> seenPaths = new HashSet<>()

        for (String path : paths) {
            String resolved = resolvePath(path, rootDir)
            if (resolved != null && !seenPaths.contains(resolved)) {
                seenPaths.add(resolved)
                validPaths.add(resolved)
            }
        }

        return validPaths
    }
}