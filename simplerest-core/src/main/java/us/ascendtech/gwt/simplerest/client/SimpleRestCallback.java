package us.ascendtech.gwt.simplerest.client;

public interface SimpleRestCallback<T> {

	void onError(int statusCode, String status, String errorBody);
}
