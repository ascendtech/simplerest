package us.ascendtech.gwt.simplerest.client;

public class SimpleRestClient {
	protected final String baseUrl;
	private final String servicePath;

	public SimpleRestClient(String baseUrl, String servicePath) {
		this.baseUrl = baseUrl;
		this.servicePath = servicePath;
	}

	protected SimpleRequestBuilder method(String method) {
		SimpleRequestBuilder simpleRequestBuilder = new SimpleRequestBuilder(baseUrl);
		return simpleRequestBuilder.method(method).path(servicePath);
	}
}
