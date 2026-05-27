package com.byd.apidoc.reference

class LinkPathResolver {

    String htmlUrl(String fromPage, String targetPage, String anchor = null) {
        return relativeUrl(fromPage, targetPage, anchor)
    }

    String markdownUrl(String fromPage, String targetPage, String anchor = null) {
        return relativeUrl(fromPage, targetPage, anchor)
    }

    String anchorUrl(String pageUrl, String anchor = null) {
        return anchor ? "${pageUrl}#${anchor}" : pageUrl
    }

    String relativeUrl(String fromPage, String targetPage, String anchor = null) {
        if (!targetPage) {
            return anchor ? "#${anchor}" : ""
        }
        String cleanTarget = normalize(targetPage)
        String cleanFrom = normalize(fromPage)
        String relative = relativize(cleanFrom, cleanTarget)
        return anchorUrl(relative, anchor)
    }

    private static String relativize(String fromPage, String targetPage) {
        if (!fromPage || fromPage == targetPage) {
            return targetPage.tokenize('/').last()
        }
        List<String> fromParts = fromPage.tokenize('/')
        List<String> targetParts = targetPage.tokenize('/')
        if (fromParts.size() > 1) {
            fromParts = fromParts[0..-2]
        } else {
            fromParts = []
        }
        int common = 0
        while (common < fromParts.size()
                && common < targetParts.size()
                && fromParts[common] == targetParts[common]) {
            common++
        }
        List<String> relative = []
        (common..<fromParts.size()).each { relative.add("..") }
        relative.addAll(targetParts.subList(common, targetParts.size()))
        return relative.join('/') ?: targetPage.tokenize('/').last()
    }

    private static String normalize(String path) {
        return (path ?: "").replace('\\', '/').replaceAll(/^\/+/, '')
    }
}
