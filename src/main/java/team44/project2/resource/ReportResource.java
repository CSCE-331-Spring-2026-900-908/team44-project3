package team44.project2.resource;

import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import team44.project2.model.report.*;
import team44.project2.service.ReportService;

import java.time.LocalDate;
import java.util.List;

@Blocking
@Path("/api/reports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReportResource {

    @Inject
    ReportService reportService;

    @GET
    @Path("/product-sales")
    public List<ProductSalesData> getProductSales(
            @QueryParam("start") String start,
            @QueryParam("end") String end
    ) {
        return reportService.getProductSales(
                LocalDate.parse(start), LocalDate.parse(end)
        );
    }

    @GET
    @Path("/daily-summary")
    public List<DailySalesSummary> getDailySummary(
            @QueryParam("start") String start,
            @QueryParam("end") String end
    ) {
        return reportService.getDailySalesSummary(
                LocalDate.parse(start), LocalDate.parse(end)
        );
    }

    @GET
    @Path("/x-report")
    public List<PaymentMethodSummary> getXReport(@QueryParam("hour") int hour) {
        return reportService.getHourlyPaymentSummary(hour);
    }

    @GET
    @Path("/z-report/status")
    public boolean zReportStatus() {
        return reportService.hasZRunToday();
    }

    @POST
    @Path("/z-report")
    public Response runZReport(ZReportRequest req) {
        try {
            ZReportResult result = reportService.runZReport(req.signedBy());
            return Response.ok(result).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/inventory-usage")
    public List<InventoryUsageData> getInventoryUsage(
            @QueryParam("start") String start,
            @QueryParam("end") String end
    ) {
        return reportService.getInventoryUsage(
                LocalDate.parse(start), LocalDate.parse(end)
        );
    }

    public record ZReportRequest(String signedBy) {}
}
