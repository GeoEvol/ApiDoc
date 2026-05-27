package com.byd.apidoc.doclet

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.model.DocCorpus
import com.sun.source.util.DocTrees
import jdk.javadoc.doclet.DocletEnvironment

import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class BuildContext {
    String id
    ApiConfig config
    DocletEnvironment environment
    DocTrees docTrees
    Elements elements
    Types types
    List<ApiDoc> apiDocs = []
    DocCorpus docCorpus
    RuntimeException failure
}
