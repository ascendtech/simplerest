package us.ascendtech.gwt.simplerest.processor;

import com.google.common.base.Throwables;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import us.ascendtech.gwt.simplerest.client.CompletableCallback;
import us.ascendtech.gwt.simplerest.client.ErrorCallback;
import us.ascendtech.gwt.simplerest.client.MultipleCallback;
import us.ascendtech.gwt.simplerest.client.SimpleRestClient;
import us.ascendtech.gwt.simplerest.client.SimpleRestGwtSync;
import us.ascendtech.gwt.simplerest.client.SingleCallback;
import us.ascendtech.gwt.simplerest.client.SingleStringCallback;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.tools.Diagnostic.Kind;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.auto.common.MoreTypes.asElement;
import static java.util.Collections.singleton;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;

public class SimpleRestGwtSyncProcessor extends AbstractProcessor {
	private static final Set<String> HTTP_METHODS = Stream.of(GET, POST, PUT, DELETE, HEAD, OPTIONS).collect(toSet());

	private static final String autoRestGwt = SimpleRestGwtSync.class.getCanonicalName();

	@Override
	public Set<String> getSupportedOptions() {
		return singleton("debug");
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return singleton(autoRestGwt);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver())
			return false;
		roundEnv.getElementsAnnotatedWith(SimpleRestGwtSync.class).stream().filter(e -> e.getKind().isInterface() && e instanceof TypeElement)
				.map(e -> (TypeElement) e).forEach(restService -> {
					try {
						processRestService(restService);
					}
					catch (Exception e) {
						// We don't allow exceptions of any kind to propagate to the compiler
						error("uncaught exception processing rest service " + restService + ": " + e + "\n" + Throwables.getStackTraceAsString(e));
					}
				});
		return true;
	}

	private void processRestService(TypeElement restService) throws Exception {
		String rsPath = restService.getAnnotation(Path.class).value();
		String[] empty = {};
		String[] produces = ofNullable(restService.getAnnotation(Produces.class)).map(Produces::value).orElse(empty);
		String[] consumes = ofNullable(restService.getAnnotation(Consumes.class)).map(Consumes::value).orElse(empty);

		ClassName rsName = ClassName.get(restService);
		log("rest service interface: " + rsName);

		ClassName modelName = ClassName.get(rsName.packageName(), rsName.simpleName() + "SimpleRest");
		log("rest service model: " + modelName);

		TypeSpec.Builder modelTypeBuilder = TypeSpec.classBuilder(modelName.simpleName()).addOriginatingElement(restService).addModifiers(Modifier.PUBLIC)
				.superclass(SimpleRestClient.class);

		modelTypeBuilder.addMethod(
				MethodSpec.constructorBuilder().addAnnotation(Inject.class).addModifiers(PUBLIC).addParameter(TypeName.get(String.class), "baseUrl", FINAL)
						.addStatement("super($L, $S);", "baseUrl", rsPath).build());

		List<ExecutableElement> methods = restService.getEnclosedElements().stream()
				.filter(e -> e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement).map(e -> (ExecutableElement) e)
				.filter(method -> !(method.getModifiers().contains(STATIC) || method.isDefault())).collect(Collectors.toList());

		Set<String> methodImports = new HashSet<>();
		for (ExecutableElement method : methods) {
			String methodName = method.getSimpleName().toString();

			Optional<? extends AnnotationMirror> incompatible = isIncompatible(method);
			if (incompatible.isPresent()) {
				modelTypeBuilder.addMethod(MethodSpec.overriding(method).addAnnotation(AnnotationSpec.get(incompatible.get()))
						.addStatement("throw new $T(\"$L\")", UnsupportedOperationException.class, methodName).build());
				continue;
			}

			CodeBlock.Builder builder = CodeBlock.builder().add("$[");

			// method type
			builder.add("method($L)", methodImport(methodImports,
					method.getAnnotationMirrors().stream().map(a -> asElement(a.getAnnotationType()).getAnnotation(HttpMethod.class)).filter(Objects::nonNull)
							.map(HttpMethod::value).findFirst().orElse(GET)));
			// resolve paths
			builder.add(".path($L)",
					Arrays.stream(ofNullable(method.getAnnotation(Path.class)).map(Path::value).orElse("").split("/")).filter(s -> !s.isEmpty())
							.map(path -> !path.startsWith("{") ?
									"\"" + path + "\"" :
									method.getParameters().stream()
											.filter(a -> ofNullable(a.getAnnotation(PathParam.class)).map(PathParam::value).map(v -> path.equals("{" + v + "}"))
													.orElse(false)).findFirst().map(VariableElement::getSimpleName).map(Object::toString)
											// next comment will produce a compilation error so the user get notified
											.orElse("/* path param " + path + " does not match any argument! */")).collect(Collectors.joining(", ")));

			// produces
			builder.add(".produces($L)",
					Arrays.stream(ofNullable(method.getAnnotation(Produces.class)).map(Produces::value).orElse(produces)).map(str -> "\"" + str + "\"")
							.collect(Collectors.joining(", ")));
			// consumes
			builder.add(".consumes($L)",
					Arrays.stream(ofNullable(method.getAnnotation(Consumes.class)).map(Consumes::value).orElse(consumes)).map(str -> "\"" + str + "\"")
							.collect(Collectors.joining(", ")));

			// query params
			method.getParameters().stream().filter(p -> p.getAnnotation(QueryParam.class) != null)
					.forEach(p -> builder.add(".param($S, $L)", p.getAnnotation(QueryParam.class).value(), p.getSimpleName()));
			// header params
			method.getParameters().stream().filter(p -> p.getAnnotation(HeaderParam.class) != null)
					.forEach(p -> builder.add(".header($S, $L)", p.getAnnotation(HeaderParam.class).value(), p.getSimpleName()));
			// form params
			method.getParameters().stream().filter(p -> p.getAnnotation(FormParam.class) != null)
					.forEach(p -> builder.add(".form($S, $L)", p.getAnnotation(FormParam.class).value(), p.getSimpleName()));

			method.getParameters().stream().filter(p -> !isParam(p)).findFirst().ifPresent(data -> builder.add(".data($L)", data.getSimpleName()));

			builder.add(".execute($L,$L);\n$]", "onDone", "onError");

			TypeMirror returnType = method.getReturnType();
			if (returnType.getKind().equals(TypeKind.VOID)) {

				MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

				LinkedHashSet<Modifier> modifiers = new LinkedHashSet<>(method.getModifiers());
				modifiers.remove(Modifier.ABSTRACT);
				modifiers.remove(Modifier.DEFAULT);
				methodBuilder.addModifiers(modifiers);

				for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
					TypeVariable var = (TypeVariable) typeParameterElement.asType();
					methodBuilder.addTypeVariable(TypeVariableName.get(var));
				}

				methodBuilder.returns(TypeName.VOID);
				List<ParameterSpec> parameterSpecs = new ArrayList<>();
				for (VariableElement parameter : method.getParameters()) {
					parameterSpecs.add(ParameterSpec.get(parameter));
				}

				parameterSpecs.add(ParameterSpec.builder(CompletableCallback.class, "onDone").build());
				parameterSpecs.add(ParameterSpec.builder(ErrorCallback.class, "onError").build());
				methodBuilder.addParameters(parameterSpecs);
				methodBuilder.varargs(method.isVarArgs());
				modelTypeBuilder.addMethod(methodBuilder.addCode(builder.build()).build());
			}
			else {

				TypeElement typeElement = (TypeElement) ((DeclaredType) returnType).asElement();

				MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName);

				LinkedHashSet<Modifier> modifiers = new LinkedHashSet<>(method.getModifiers());
				modifiers.remove(Modifier.ABSTRACT);
				modifiers.remove(Modifier.DEFAULT);
				methodBuilder.addModifiers(modifiers);

				for (TypeParameterElement typeParameterElement : method.getTypeParameters()) {
					TypeVariable var = (TypeVariable) typeParameterElement.asType();
					methodBuilder.addTypeVariable(TypeVariableName.get(var));
				}

				methodBuilder.returns(TypeName.VOID);
				List<ParameterSpec> parameterSpecs = new ArrayList<>();
				for (VariableElement parameter : method.getParameters()) {
					parameterSpecs.add(ParameterSpec.get(parameter));
				}

				TypeName returnTypeName = TypeName.get(returnType);
				TypeName rawTypeName = TypeName.get(returnType);
				if (returnTypeName instanceof ParameterizedTypeName) {
					rawTypeName = ((ParameterizedTypeName) returnTypeName).rawType;
				}

				if (TypeName.get(String.class).equals(returnTypeName)) {
					parameterSpecs.add(ParameterSpec.builder(SingleStringCallback.class, "onDone").build());
				}
				else if (TypeName.get(Collection.class).equals(rawTypeName) || TypeName.get(List.class).equals(rawTypeName)) {
					ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(ClassName.get(MultipleCallback.class),
							TypeName.get(((DeclaredType) returnType).getTypeArguments().get(0)));
					parameterSpecs.add(ParameterSpec.builder(parameterizedTypeName, "onDone").build());
				}
				else {
					ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(ClassName.get(SingleCallback.class), returnTypeName);
					parameterSpecs.add(ParameterSpec.builder(parameterizedTypeName, "onDone").build());
				}

				parameterSpecs.add(ParameterSpec.builder(ErrorCallback.class, "onError").build());
				methodBuilder.addParameters(parameterSpecs);
				methodBuilder.varargs(method.isVarArgs());
				modelTypeBuilder.addMethod(methodBuilder.addCode(builder.build()).build());
			}
		}

		Filer filer = processingEnv.getFiler();
		JavaFile.Builder file = JavaFile.builder(rsName.packageName(), modelTypeBuilder.build());
		for (String methodImport : methodImports)
			file.addStaticImport(HttpMethod.class, methodImport);
		boolean skipJavaLangImports = processingEnv.getOptions().containsKey("skipJavaLangImports");
		file.skipJavaLangImports(skipJavaLangImports).build().writeTo(filer);
	}

	private String methodImport(Set<String> methodImports, String method) {
		if (HTTP_METHODS.contains(method)) {
			methodImports.add(method);
			return method;
		}
		else {
			return "\"" + method + "\"";
		}
	}

	public boolean isParam(VariableElement a) {
		return a.getAnnotation(CookieParam.class) != null || a.getAnnotation(FormParam.class) != null || a.getAnnotation(HeaderParam.class) != null
				|| a.getAnnotation(MatrixParam.class) != null || a.getAnnotation(PathParam.class) != null || a.getAnnotation(QueryParam.class) != null;
	}

	private Optional<? extends AnnotationMirror> isIncompatible(ExecutableElement method) {
		return method.getAnnotationMirrors().stream().filter(this::isIncompatible).findAny();
	}

	private boolean isIncompatible(AnnotationMirror a) {
		return a.getAnnotationType().toString().endsWith("GwtIncompatible");
	}

	private void log(String msg) {
		if (processingEnv.getOptions().containsKey("debug")) {
			processingEnv.getMessager().printMessage(Kind.NOTE, msg);
		}
	}

	private void error(String msg) {
		processingEnv.getMessager().printMessage(Kind.ERROR, msg);
	}
}
