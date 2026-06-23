package com.byd.apidoc.render

import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.projection.DocProjection

class RenderContext {
    DocCorpus corpus
    DocProjection projection
    File outputDir
    String projectName = "API Reference"
    String siteBasePath = ""
    String assetVersion = ""
    boolean stableAssetLinks = false
}
