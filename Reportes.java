import java.sql.*;
import java.io.*;
import java.awt.Desktop;
import java.text.DecimalFormat;
import java.util.*;

import javax.swing.JOptionPane;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Reportes.java
 * Reporte profesional azul: Ventas por Fecha (gráfica + tabla) para tu esquema actual.
 * Requiere: iText 5.x y JFreeChart en el classpath.
 */
public class Reportes {

    // Evento para encabezado y pie (iText 5)
    class HeaderFooter extends PdfPageEventHelper {
        Font fontHeader = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(0, 51, 102));
        Font fontFooter = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY);

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfPTable header = new PdfPTable(1);
                header.setTotalWidth(document.getPageSize().getWidth() - 72);
                header.setLockedWidth(true);

                PdfPCell cell = new PdfPCell(new Phrase("PAPELERÍA — REPORTE DE VENTAS", fontHeader));
                cell.setBorder(Rectangle.BOTTOM);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPaddingBottom(6);
                header.addCell(cell);
                header.writeSelectedRows(0, -1, 36, document.getPageSize().getHeight() - 20, writer.getDirectContent());

                PdfPTable footer = new PdfPTable(1);
                footer.setTotalWidth(document.getPageSize().getWidth() - 72);
                footer.setLockedWidth(true);

                PdfPCell f = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), fontFooter));
                f.setBorder(Rectangle.TOP);
                f.setHorizontalAlignment(Element.ALIGN_CENTER);
                f.setPaddingTop(6);
                footer.addCell(f);
                footer.writeSelectedRows(0, -1, 36, 30, writer.getDirectContent());
            } catch (Exception e) {
                // no hacemos nada, el reporte puede continuar sin header/footer
            }
        }
    }

    /**
     * Genera reporte de ventas por fecha (gráfica + tabla) y abre el PDF automáticamente.
     * Utiliza tus tablas: venta (Fecha), detalleventa (ID_Producto, Cantidad), producto (Precio).
     */
    public void reporteVentas(Connection con) {
        if (con == null) {
            JOptionPane.showMessageDialog(null, "Conexión nula. No se puede generar el reporte.");
            return;
        }

        // Consulta: suma ingresos por fecha usando precio de la tabla producto
        String sql = "SELECT v.Fecha, SUM(d.Cantidad * p.Precio) AS Total " +
                     "FROM venta v " +
                     "JOIN detalleventa d ON v.ID_Venta = d.ID_Venta " +
                     "JOIN producto p ON d.ID_Producto = p.ID_Producto " +
                     "WHERE v.Estado IS NULL OR v.Estado = 'ACTIVA' " + // incluye solo ventas activas (ajusta según tu uso)
                     "GROUP BY v.Fecha " +
                     "ORDER BY v.Fecha";

        java.util.List<Map.Entry<String, Double>> filas = new ArrayList<Map.Entry<String, Double>>();

        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String fecha = rs.getString("Fecha");
                double total = rs.getDouble("Total");
                filas.add(new AbstractMap.SimpleEntry<String, Double>(fecha, total));
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al consultar ventas: " + ex.getMessage());
            return;
        }

        if (filas.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No hay datos de ventas para generar el reporte.");
            return;
        }

        // Dataset para la gráfica
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> e : filas) {
            dataset.addValue(e.getValue(), "Ventas", e.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Ventas por Fecha",
                "Fecha",
                "Total (MXN)",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        // Formateador para montos
        DecimalFormat df = new DecimalFormat("#,##0.00");

        try {
            // Convertir gráfica a PNG en memoria
            ByteArrayOutputStream chartBaos = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(chartBaos, chart, 900, 420);
            byte[] chartBytes = chartBaos.toByteArray();

            // Nombre del archivo con timestamp
            String fileName = "Reporte_Ventas_" + System.currentTimeMillis() + ".pdf";

            Document document = new Document(PageSize.A4, 40, 40, 70, 50);
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
            writer.setPageEvent(new HeaderFooter());

            document.open();

            // Título
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, new BaseColor(0, 51, 102));
            Paragraph title = new Paragraph("REPORTE DE VENTAS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(8);
            document.add(title);

            Paragraph subtitle = new Paragraph("Resumen de ingresos por fecha",
                    new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, BaseColor.GRAY));
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(12);
            document.add(subtitle);

            Paragraph fechaGen = new Paragraph("Generado: " + new java.util.Date(),
                    new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, BaseColor.GRAY));
            fechaGen.setAlignment(Element.ALIGN_CENTER);
            fechaGen.setSpacingAfter(14);
            document.add(fechaGen);

            // Insertar logo opcional (si existe logo.png en el classpath o carpeta del proyecto)
            try {
                // Intenta cargar /logo.png desde resources; si no existe, se ignora
                Image logo = Image.getInstance(getClass().getResource("/logo.png"));
                logo.scaleToFit(80, 80);
                logo.setAlignment(Image.ALIGN_CENTER);
                document.add(logo);
                document.add(Chunk.NEWLINE);
            } catch (Exception ignored) {
            }

            // Agregar gráfica
            Image chartImage = Image.getInstance(chartBytes);
            chartImage.scaleToFit(PageSize.A4.getWidth() - 80, 350);
            chartImage.setAlignment(Image.ALIGN_CENTER);
            document.add(chartImage);

            document.add(Chunk.NEWLINE);

            // Tabla de detalle (Fecha | Total)
            PdfPTable table = new PdfPTable(new float[] { 3f, 2f });
            table.setWidthPercentage(100f);
            table.setSpacingBefore(6f);
            table.setSpacingAfter(6f);

            Font headFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);

            PdfPCell th1 = new PdfPCell(new Phrase("Fecha", headFont));
            th1.setHorizontalAlignment(Element.ALIGN_CENTER);
            th1.setBackgroundColor(new BaseColor(0, 51, 102));
            th1.setPadding(6f);
            table.addCell(th1);

            PdfPCell th2 = new PdfPCell(new Phrase("Total (MXN)", headFont));
            th2.setHorizontalAlignment(Element.ALIGN_CENTER);
            th2.setBackgroundColor(new BaseColor(0, 51, 102));
            th2.setPadding(6f);
            table.addCell(th2);

            Font cellFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
            boolean alterna = false;
            double suma = 0.0;

            for (Map.Entry<String, Double> e : filas) {
                BaseColor bg = alterna ? new BaseColor(245, 245, 245) : BaseColor.WHITE;
                alterna = !alterna;

                PdfPCell c1 = new PdfPCell(new Phrase(e.getKey(), cellFont));
                c1.setBackgroundColor(bg);
                c1.setPadding(6f);
                table.addCell(c1);

                PdfPCell c2 = new PdfPCell(new Phrase(df.format(e.getValue()), cellFont));
                c2.setBackgroundColor(bg);
                c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                c2.setPadding(6f);
                table.addCell(c2);

                suma += e.getValue();
            }

            document.add(table);

            // Total general
            Paragraph total = new Paragraph("Total general: " + df.format(suma),
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.BLACK));
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(8f);
            document.add(total);

            // Pie informativo
            Paragraph nota = new Paragraph(
                    "\nNota: Valores calculados usando precio actual de tabla 'producto' y cantidades en 'detalleventa'.",
                    new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
            nota.setAlignment(Element.ALIGN_LEFT);
            nota.setSpacingBefore(10f);
            document.add(nota);

            document.close();
            writer.close();

            // Abrir automáticamente el PDF (si el sistema lo soporta)
            try {
                File pdfFile = new File(fileName);
                if (pdfFile.exists() && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                }
            } catch (Exception ex) {
                // si falla abrir, no interrumpimos; usuario verá mensaje final
            }

            JOptionPane.showMessageDialog(null, "Reporte generado correctamente: " + fileName);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error al generar el reporte: " + ex.getMessage());
        }
    }
}

