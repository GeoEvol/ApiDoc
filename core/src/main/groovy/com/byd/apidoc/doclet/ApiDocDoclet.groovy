package com.byd.apidoc.doclet

import com.byd.apidoc.model.ApiConfig
import com.byd.apidoc.model.ApiDoc
import com.byd.apidoc.parser.javadoc.DocCorpusBuilder
import com.byd.apidoc.parser.javadoc.converter.ElementClassConverter
import jdk.javadoc.doclet.Doclet
import jdk.javadoc.doclet.DocletEnvironment
import jdk.javadoc.doclet.Reporter

import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

class ApiDocDoclet implements Doclet {
    private Reporter reporter
    private String contextId

    @Override
    void init(Locale locale, Reporter reporter) {
        this.reporter = reporter
    }

    @Override
    String getName() {
        return "ApiDocDoclet"
    }

    @Override
    Set<? extends Doclet.Option> getSupportedOptions() {
        return [contextIdOption()] as Set
    }

    @Override
    SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest()
    }

    @Override
    boolean run(DocletEnvironment environment) {
        BuildContext context = BuildContextRegistry.get(contextId)
        if (context == null) {
            throw new IllegalStateException("ApiDocDoclet context is missing. Use ${DocletConfig.CONTEXT_ID_OPTION}.")
        }

        try {
            context.environment = environment
            context.docTrees = environment.docTrees
            context.elements = environment.elementUtils
            context.types = environment.typeUtils

            ApiConfig config = context.config ?: new ApiConfig()
            context.docCorpus = new DocCorpusBuilder(context).build()

            ElementClassConverter converter = new ElementClassConverter(context, config)
            List<ApiDoc> docs = []
            collectTypeElements(environment).each { TypeElement typeElement ->
                ApiDoc doc = converter.convert(typeElement)
                if (doc != null) {
                    docs.add(doc)
                }
            }
            context.apiDocs = docs
            return true
        } catch (RuntimeException e) {
            context.failure = e
            return false
        } catch (Exception e) {
            context.failure = new RuntimeException("Javadoc doclet parsing failed", e)
            return false
        }
    }

    private Doclet.Option contextIdOption() {
        return new Doclet.Option() {
            @Override
            int getArgumentCount() {
                return 1
            }

            @Override
            String getDescription() {
                return "ApiDoc build context id"
            }

            @Override
            Doclet.Option.Kind getKind() {
                return Doclet.Option.Kind.STANDARD
            }

            @Override
            List<String> getNames() {
                return [DocletConfig.CONTEXT_ID_OPTION]
            }

            @Override
            String getParameters() {
                return "<id>"
            }

            @Override
            boolean process(String option, List<String> arguments) {
                contextId = arguments ? arguments[0] : null
                return contextId != null && !contextId.trim().isEmpty()
            }
        }
    }

    private static List<TypeElement> collectTypeElements(DocletEnvironment environment) {
        LinkedHashSet<TypeElement> result = new LinkedHashSet<>()
        environment.includedElements.each { Element element ->
            collectTypeElements(element, result)
        }
        return new ArrayList<>(result)
    }

    private static void collectTypeElements(Element element, Set<TypeElement> result) {
        if (element instanceof TypeElement) {
            result.add((TypeElement) element)
        }
        element.enclosedElements.each { Element child ->
            if (child instanceof TypeElement) {
                collectTypeElements(child, result)
            }
        }
    }
}
