
# Usage

### Define a Client Service Definition
```java

@SimpleRestGwt
@Path("/service/todo")
public interface ToDoServiceClient {

	@GET
	@Path("/list")
	void getCurrentToDos(MultipleCallback<ToDoDTO> callback);

	@PUT
	@Path("/add")
	void addToDo(ToDoDTO toDo, SingleCallback<ToDoDTO> callback);

	@DELETE
	@Path("/delete/{id}")
	void deleteToDo(@PathParam("id") Integer id, CompletableCallback callback);

	@POST
	@Path("/search/{query}")
	void searchToDos(@PathParam("query") String query, MultipleCallback<ToDoDTO> callback);

}
```

### Define DTO objects
DTOs should be defined as JS native objects on the client and POJO on the backend
```java
@JsType(namespace = GLOBAL, name = "Object", isNative = true)
public class ToDoDTO {
	
}
```


### Sample backend definition in micronaut
```java

@Controller("/service/todo")
public class ToDoController {

	private ToDoService todoService;

	public ToDoController(ToDoService todoService) {
		this.todoService = todoService;
	}

	@Get("/list")
	public HttpResponse<Collection<ToDo>> list() {
		return HttpResponse.created(todoService.getCurrentTODOs());
	}

	@Put("/add")
	public HttpResponse<ToDo> add(@Body ToDo todo) {
		todoService.addTodo(todo);
		return HttpResponse.created(todo);
	}

	@Delete("/delete/{id}")
	public HttpResponse delete(@Parameter Integer id) {
		todoService.removeTodo(id);
		return HttpResponse.ok();
	}

	@Post("/search/{query}")
	public HttpResponse<Collection<ToDo>> searchToDos(@QueryValue String query) {

		Collection<ToDo> todos = new ArrayList<>();
		for (ToDo todo : todoService.getCurrentTODOs()) {
			if (todo.getTodo().toLowerCase().contains(query.toLowerCase())) {
				todos.add(todo);
			}
		}

		return HttpResponse.created(todos);

	}

}
```

### Build code to generate client implementions using annotation processing
From ToDoServiceClient a class ToDoServiceClientSimpleRest will be generated.

### Create service
Service can be made static in a singleton class and shared
```java
String baseUrl = DomGlobal.window.location.protocol + "//" + DomGlobal.window.location.host;
ToDoServiceClient todoServiceClient = new ToDoServiceClientSimpleRest(baseUrl);
```

### Use service
```java
todoServiceClient.addToDo(newToDoDTO, new SingleCallback<ToDoDTO>() {
	@Override
	public void onData(ToDoDTO data) {

	}

	@Override
	public void onError(int statusCode, String status, String errorBody) {
	
	}
});


todoServiceClient.deleteToDo(currentValue.getId(), new CompletableCallback() {
	@Override
	public void onDone() {
	
	}

	@Override
	public void onError(int statusCode, String status, String errorBody) {
					
	}

});

todoServiceClient.getCurrentToDos(new MultipleCallback<ToDoDTO>() {
	@Override
	public void onData(ToDoDTO[] data) {

	}

	@Override 
	public void onError(int statusCode, String status, String errorBody) {

	}
});

```






