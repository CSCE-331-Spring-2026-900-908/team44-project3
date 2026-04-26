package team44.project2.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.menu.MenuItem;
import team44.project2.model.menu.MenuItemContent;
import team44.project2.service.MenuService;

import java.io.InputStream;
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
                item.size(), item.basePrice(), item.isAvailable(), item.isHot(), item.hasImage()
        );
        menuService.updateMenuItem(updated);
        return Response.ok().build();
    }

    @GET
    @Path("/items/{id}/image")
    public Response getImage(@PathParam("id") int id) {
        byte[] data = menuService.getMenuItemImage(id);
        if (data == null) return Response.status(Response.Status.NOT_FOUND).build();
        String mime = "application/octet-stream";
        if (data.length >= 3 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8)
            mime = "image/jpeg";
        else if (data.length >= 4 && data[0] == (byte) 0x89 && data[1] == 0x50)
            mime = "image/png";
        else if (data.length >= 4 && data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46)
            mime = "image/gif";
        else if (data.length >= 12 && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50)
            mime = "image/webp";
        return Response.ok(data, mime)
                .header("Cache-Control", "public, max-age=3600")
                .build();
    }

    @POST
    @Path("/items/{id}/image")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadImage(@PathParam("id") int id, InputStream body) {
        try {
            byte[] data = body.readAllBytes();
            if (data.length == 0) return Response.status(Response.Status.BAD_REQUEST).build();
            menuService.saveMenuItemImage(id, data);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/items/{id}/image")
    public Response deleteImage(@PathParam("id") int id) {
        menuService.deleteMenuItemImage(id);
        return Response.noContent().build();
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
