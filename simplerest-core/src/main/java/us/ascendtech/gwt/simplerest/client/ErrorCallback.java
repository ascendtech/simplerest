package us.ascendtech.gwt.simplerest.client;

public interface ErrorCallback {
	void onError(int statusCode, String status, String errorBody);
}
