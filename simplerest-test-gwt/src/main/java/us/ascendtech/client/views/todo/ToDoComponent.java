package us.ascendtech.client.views.todo;

import com.axellience.vuegwt.core.annotations.component.Component;
import com.axellience.vuegwt.core.annotations.component.Data;
import com.axellience.vuegwt.core.client.component.IsVueComponent;
import com.axellience.vuegwt.core.client.component.hooks.HasBeforeMount;
import com.axellience.vuegwt.core.client.component.hooks.HasCreated;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import elemental2.core.JsArray;
import elemental2.dom.DomGlobal;
import jsinterop.annotations.JsMethod;
import org.jboss.elemento.Elements;
import us.ascendtech.client.aggrid.AgReadyEvent;
import us.ascendtech.client.aggrid.ColumnDefinition;
import us.ascendtech.client.aggrid.GridApi;
import us.ascendtech.client.dto.ToDoDTO;
import us.ascendtech.client.services.ServiceProvider;
import us.ascendtech.gwt.simplerest.client.CompletableCallback;
import us.ascendtech.gwt.simplerest.client.MultipleCallback;
import us.ascendtech.gwt.simplerest.client.SingleCallback;

@Component
public class ToDoComponent implements IsVueComponent, HasBeforeMount, HasCreated, ResizeHandler {

	@Data
	String error;

	@Data
	boolean showError;

	@Data
	String inputTodo;

	@Data
	GridApi<ToDoDTO> gridApi;

	@Data
	JsArray<ColumnDefinition> columnDefs = new JsArray<>();

	@Data
	JsArray<ToDoDTO> rowData = new JsArray<>();

	@Data
	String tableHeight;

	@JsMethod
	void addToTable() {
		if (inputTodo == null || inputTodo.isEmpty()) {
			error = "Enter some text to add as a to do.";
			showError = true;
		}
		else {
			ToDoDTO newToDoDTO = new ToDoDTO();
			newToDoDTO.setTodo(inputTodo);
			rowData.push(newToDoDTO);
			ServiceProvider.get().getTodoServiceClient().addToDo(newToDoDTO, new SingleCallback<ToDoDTO>() {
				@Override
				public void onData(ToDoDTO data) {
					newToDoDTO.setId(data.getId());
				}

				@Override
				public void onError(int statusCode, String status, String errorBody) {
					error = errorBody;
					showError = true;
				}
			});
			inputTodo = "";
		}
	}

	@JsMethod
	void removeFromTable() {
		if (gridApi.getSelectedRows().length > 0) {
			gridApi.getSelectedRows().forEach((currentValue, index, array) -> {
				rowData.splice(rowData.indexOf(currentValue), 1);
				ServiceProvider.get().getTodoServiceClient().deleteToDo(currentValue.getId(), new CompletableCallback() {
					@Override
					public void onDone() {

					}

					@Override
					public void onError(int statusCode, String status, String errorBody) {
						error = errorBody;
						showError = true;
					}

				});
				return null;
			});
		}
		else {
			error = "Select at least one item to remove.";
			showError = true;
		}

	}

	@JsMethod
	public void onGridReady(AgReadyEvent<ToDoDTO> event) {
		gridApi = event.getApi();
		gridApi.sizeColumnsToFit();
	}

	@Override
	public void beforeMount() {
		ColumnDefinition<ToDoDTO> todoColumn = new ColumnDefinition<>();
		todoColumn.setHeaderName("ToDo");
		todoColumn.setField("todo");
		todoColumn.setCheckboxSelection(true);

		columnDefs.push(todoColumn);

		rowData = new JsArray<>();

		ServiceProvider.get().getTodoServiceClient().getCurrentToDos(new MultipleCallback<ToDoDTO>() {
			@Override
			public void onData(ToDoDTO[] data) {
				rowData.push(data);
			}

			@Override
			public void onError(int statusCode, String status, String errorBody) {
				error = errorBody;
				showError = true;
			}
		});

		// this is not used for anything, just showing how to iterate through a JsArray
		for (ToDoDTO toDoDTO : Elements.elements(rowData)) {
			DomGlobal.console.log(toDoDTO);
		}
	}

	@Override
	public void onResize(ResizeEvent resizeEvent) {
		calculateTableHeight();
	}

	private void calculateTableHeight() {
		int height = Window.getClientHeight();
		tableHeight = "height: " + (height - 310) + "px;";
	}

	@Override
	public void created() {
		calculateTableHeight();
		Window.addResizeHandler(this);
	}
}
