package team44.project2.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.Employee;
import team44.project2.service.employee.EmployeeService;

import java.util.List;

@Blocking
@Path("/api/employees")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmployeeResource {

    @Inject
    EmployeeService employeeService;

    @GET
    public List<Employee> getAll() {
        return employeeService.getAllEmployees();
    }

    @POST
    public Response add(Employee employee) {
        employeeService.addEmployee(employee);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") int id, Employee employee) {
        Employee updated = new Employee(
                id, employee.firstName(), employee.lastName(),
                employee.role(), employee.startDate(), employee.email(),
                employee.passwordHash(), employee.isActive()
        );
        employeeService.updateEmployee(updated);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") int id) {
        employeeService.deleteEmployee(id);
        return Response.noContent().build();
    }
}
