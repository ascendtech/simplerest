package us.ascendtech.gwt.simplerest.client;

import elemental2.core.Global;
import elemental2.dom.DomGlobal;
import elemental2.dom.FormData;
import elemental2.dom.Headers;
import elemental2.dom.RequestInit;
import elemental2.dom.Response;
import elemental2.promise.Promise;
import jsinterop.base.Js;

import javax.ws.rs.HttpMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

public class SimpleRequestBuilder {

	public static class Param {
		public final String k;
		public final Object v;

		public Param(String k, Object v) {
			this.k = k;
			this.v = v;
		}

		@Override
		public String toString() {
			return "Param{k='" + k + "', v=" + v + '}';
		}
	}

	protected final String base;
	protected List<String> paths = new ArrayList<>();
	protected List<Param> queryParams = new ArrayList<>();
	protected List<Param> headerParams = new ArrayList<>();
	protected List<Param> formParams = new ArrayList<>();
	protected String method = HttpMethod.GET;
	protected Object data = null;
	private String[] produces = {};
	private String[] consumes = {};

	public SimpleRequestBuilder(String base) {
		this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}

	protected String encodeComponent(String str) {
		return Global.encodeURIComponent(str).replaceAll("%20", "+");
	}

	@SuppressWarnings("unchecked")
	public <S, T> void execute(SimpleRestCallback<T> callback, ErrorCallback errorCallback) {

		boolean textResponse = (callback instanceof SingleStringCallback);

		request(textResponse).then(response -> {
			if (response.ok) {
				if (callback instanceof SingleStringCallback) {
					response.text().then(text -> {
						((SingleStringCallback) callback).onData(text);
						return null;
					});
				}
				else if (callback instanceof SingleCallback) {

					response.json().then(json -> {
						((SingleCallback<T>) callback).onData(Js.cast(json));
						return null;
					});
				}
				else if (callback instanceof MultipleCallback) {
					response.json().then(json -> {
						((MultipleCallback<T>) callback).onData(Js.cast(json));
						return null;
					});
				}
				else if (callback instanceof CompletableCallback) {
					response.text().then(v -> {
						((CompletableCallback) callback).onDone();
						return null;
					});
				}
				else {
					throw new UnsupportedOperationException(
							"Second to last parameter must be a callback of type SingleStringCallback, SingleCallback, MultipleCallback, or CompletableCallback");
				}
			}
			else {
				response.text().then(text -> {
					errorCallback.onError(response.status, response.statusText, Js.cast(text));
					return null;
				});
			}
			return null;
		}).catch_(error -> {
			errorCallback.onError(-1, "TypeError", error.toString());
			return null;
		});

	}

	public Promise<Response> request(boolean textResponse) {
		RequestInit requestInit = RequestInit.create();
		requestInit.setCredentials("same-origin");
		requestInit.setMethod(method);

		if (data != null) {
			requestInit.setBody(Global.JSON.stringify(data));
		}

		Headers headers = new Headers();
		headerParams.forEach(h -> headers.append(h.k, Objects.toString(h.v)));

		if (!formParams.isEmpty()) {
			FormData form = new FormData();
			formParams.forEach(p -> form.append(p.k, Objects.toString(p.v)));
			requestInit.setBody(form);
		}
		else if (data != null) {
			if (headers.get(CONTENT_TYPE) == null) {
				if (consumes.length > 0) {
					for (String consume : consumes) {
						headers.append(CONTENT_TYPE, consume);
					}
				}
				else {
					headers.append(CONTENT_TYPE, APPLICATION_JSON);
				}
			}
			if (headers.get(ACCEPT) == null) {
				if (produces.length > 0) {
					for (String produce : produces) {
						headers.append(ACCEPT, produce);
					}
				}
				else {
					if (!textResponse) {
						headers.append(ACCEPT, APPLICATION_JSON);
					}
					else {
						headers.append(ACCEPT, TEXT_HTML);
					}
				}
			}
			requestInit.setBody(Global.JSON.stringify(data));
		}

		requestInit.setHeaders(headers);

		return DomGlobal.window.fetch(uri(), requestInit);

	}

	public SimpleRequestBuilder method(String method) {
		Objects.requireNonNull(method, "path required");
		this.method = method;
		return this;
	}

	public SimpleRequestBuilder path(Object... paths) {
		for (Object path : paths) {
			path(Objects.toString(Objects.requireNonNull(path, "path required")));
		}
		return this;
	}

	public SimpleRequestBuilder path(String path) {
		if (path.isEmpty()) {
			throw new IllegalArgumentException("non-empty path required");
		}
		this.paths.add(path);
		return this;
	}

	public SimpleRequestBuilder produces(String... produces) {
		this.produces = produces;
		return this;
	}

	public SimpleRequestBuilder consumes(String... consumes) {
		if (consumes.length > 0 /*0 means undefined, so do not override default*/)
			this.consumes = consumes;
		return this;
	}

	public SimpleRequestBuilder produces(String key, Object value) {
		Objects.requireNonNull(key, "header param key required");
		handleLists(key, value, headerParams);
		return this;
	}

	public SimpleRequestBuilder form(String key, Object value) {
		Objects.requireNonNull(key, "form param key required");
		handleLists(key, value, formParams);
		return this;
	}

	private void handleLists(String key, Object value, List<Param> params) {
		if (value != null) {
			if ((value instanceof Iterable<?>)) {
				for (Object v : ((Iterable<?>) value)) {
					params.add(new Param(key, v));
				}
			}
			else {
				params.add(new Param(key, value));
			}
		}
	}

	public SimpleRequestBuilder data(Object data) {
		this.data = data;
		return this;
	}

	public String uri() {
		String out = base;
		for (String pathComponent : paths) {
			out += "/" + pathComponent;
		}
		return out + query();
	}

	public String query() {
		String q = encodeParams(queryParams);
		return q.isEmpty() ? "" : "?" + q;
	}

	protected String encodeParams(List<Param> params) {
		String out = "";
		for (Param p : params) {
			out += (out.isEmpty() ? "" : "&") + encodeComponent(p.k) + "=" + encodeComponent(Objects.toString(p.v));
		}
		return out;
	}

	@Override
	public String toString() {
		return method + " " + uri();
	}

}
