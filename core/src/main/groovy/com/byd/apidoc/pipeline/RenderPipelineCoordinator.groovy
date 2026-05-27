package com.byd.apidoc.pipeline

import com.byd.apidoc.metadata.VisibilityPolicy
import com.byd.apidoc.comment.InheritDocResolver
import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.DocCorpus
import com.byd.apidoc.output.DocCorpusWriter
import com.byd.apidoc.output.OutputManifestWriter
import com.byd.apidoc.output.ProjectionWriter
import com.byd.apidoc.parser.javadoc.JavadocApiParser
import com.byd.apidoc.parser.javadoc.JavadocParseResult
import com.byd.apidoc.projection.DocProjection
import com.byd.apidoc.projection.ProjectionBuilder
import com.byd.apidoc.reference.ReferenceResolver
import com.byd.apidoc.render.BuiltinHtmlRenderer
import com.byd.apidoc.render.MarkdownRenderer
import com.byd.apidoc.render.RenderContext

class RenderPipelineCoordinator {
    private final JavadocApiParser parser = new JavadocApiParser()
    private final DocCorpusWriter docCorpusWriter = new DocCorpusWriter()
    private final ProjectionWriter projectionWriter = new ProjectionWriter()
    private final OutputManifestWriter outputManifestWriter = new OutputManifestWriter()
    private final ProjectionBuilder projectionBuilder = new ProjectionBuilder()
    private final ReferenceResolver referenceResolver = new ReferenceResolver()
    private final InheritDocResolver inheritDocResolver = new InheritDocResolver()
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer()
    private final BuiltinHtmlRenderer htmlRenderer = new BuiltinHtmlRenderer()

    void generate(List<String> sourcePaths, String projectName, File outputDir, ApiConfig config) {
        JavadocParseResult parseResult = parser.parseResult(sourcePaths, config)
        writeV1Outputs(parseResult.docCorpus, outputDir, config, projectName)
    }

    private void writeV1Outputs(DocCorpus docCorpus, File outputDir, ApiConfig config, String projectName) {
        referenceResolver.resolve(docCorpus)
        inheritDocResolver.resolve(docCorpus)
        docCorpusWriter.write(docCorpus, outputDir)

        DocProjection projection = projectionBuilder.build(docCorpus, visibilityPolicy(config))
        projectionWriter.write(projection, outputDir)

        RenderContext renderContext = new RenderContext(
                corpus: docCorpus,
                projection: projection,
                outputDir: outputDir,
                projectName: projectName ?: config?.projectName ?: "API Reference"
        )
        markdownRenderer.render(renderContext)
        htmlRenderer.render(renderContext)
        outputManifestWriter.write(outputDir)
    }

    private static VisibilityPolicy visibilityPolicy(ApiConfig config) {
        return new VisibilityPolicy(
                includeHidden: config?.includeHidden ?: false,
                includeRemoved: config?.includeRemoved ?: false
        )
    }
}
