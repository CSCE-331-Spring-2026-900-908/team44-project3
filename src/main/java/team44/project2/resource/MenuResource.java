package team44.project2.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.menu.MenuItemContent;
import team44.project2.service.MenuService;

import java.util.List;

@Blocking
@Path("/api/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MenuResource {

    @Inject
    MenuService menuService;

    @GET
    @Path("/categories")
    public List<String> getCategories() {
        return menuService.getCategories();
    }

    @GET
    @Path("/items")
    public List<MenuItem> getItems(@QueryParam("category") String category) {
        if (category != null && !category.isEmpty()) {
            return menuService.getItemsByCategory(category);
        }
        return menuService.getAllMenuItems();
    }

    @POST
    @Path("/items")
    public Response addItem(MenuItem item) {
        int id = menuService.addMenuItemAndGetId(item);
        return Response.status(Response.Status.CREATED).entity(id).build();
    }

    @PUT
    @Path("/items/{id}")
    public Response updateItem(@PathParam("id") int id, MenuItem item) {
        MenuItem updated = new MenuItem(
                id, item.name(), item.category(),
                item.size(), item.basePrice(), item.isAvailable(), item.isHot()
        );
        menuService.updateMenuItem(updated);
        return Response.ok().build();
    }

    @DELETE
    @Path("/items/{id}")
    public Response deleteItem(@PathParam("id") int id) {
        menuService.deleteMenuItem(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/items/{id}/sizes")
    public List<String> getSizes(@PathParam("id") int id) {
        return menuService.getSizesForItem(id);
    }

    @GET
    @Path("/addons")
    public List<MenuItem> getAddOns() {
        return menuService.getAddOns();
    }

    @POST
    @Path("/item-content")
    public Response addContent(MenuItemContent content) {
        menuService.addMenuItemContent(content);
        return Response.status(Response.Status.CREATED).build();
    }

}
