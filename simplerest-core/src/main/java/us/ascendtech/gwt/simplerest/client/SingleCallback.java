package us.ascendtech.gwt.simplerest.client;

public interface SingleCallback<T> extends SimpleRestCallback<T> {

	void onData(T data);

}
