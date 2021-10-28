package us.ascendtech.gwt.simplerest.processor;

import us.ascendtech.gwt.simplerest.client.ErrorCallback;
import us.ascendtech.gwt.simplerest.client.SimpleRestGwt;
import us.ascendtech.gwt.simplerest.client.SingleStringCallback;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@SimpleRestGwt
@Path("/service/todo")
public interface ToDoServiceClient {

	@POST
	@Path("todo/store")
	void storeTodo(ToDoDTO todo, SingleStringCallback callback, ErrorCallback errorCallback);

}
