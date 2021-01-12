package us.ascendtech.gwt.simplerest.client;

public interface MultipleCallback<T> extends SimpleRestCallback<T> {

	void onData(T[] data);

}
