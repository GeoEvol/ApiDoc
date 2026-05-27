package com.byd.apidoc.generator

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.pipeline.RenderPipelineCoordinator

import java.util.logging.Logger

class DocGenerator {
    private static final Logger logger = Logger.getLogger(DocGenerator.class.name)
    private final RenderPipelineCoordinator pipeline = new RenderPipelineCoordinator()

    void generateApiDocs(List<String> sourcePaths, String projectName, File outputDir, ApiConfig config) {
        logger.info("Generating API docs from ${sourcePaths} into ${outputDir.absolutePath}")
        try {
            pipeline.generate(sourcePaths, projectName, outputDir, config)
            logger.info("API docs generated successfully")
        } catch (Exception e) {
            logger.severe("API doc generation failed: ${e.message}")
            throw new RuntimeException("API doc generation failed", e)
        }
    }

    void generateApiDocs(File sourceDir, String projectName, File outputDir, ApiConfig config) {
        generateApiDocs([sourceDir.absolutePath], projectName, outputDir, config)
    }
}
