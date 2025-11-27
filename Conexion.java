import java.sql.*;
import javax.swing.*;
import java.util.*;

public class Conexion {
    private Connection con = null;

    // --------------------- CONECTAR ---------------------
    public void conectar() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/sistemaventas",
                "root",
                ""
            );
            System.out.println("Conectado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error al conectar: " + ex.getMessage());
        }
    }

    public void desconectar() {
        try { if (con != null) con.close(); }
        catch (SQLException e) {}
    }

    // --------------------- EJECUTAR ---------------------
    private boolean ejecutar(String sql, Object... params) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                Object p = params[i];
                if (p instanceof Integer) ps.setInt(i + 1, (Integer)p);
                else if (p instanceof Double) ps.setDouble(i + 1, (Double)p);
                else ps.setString(i + 1, p.toString());
            }

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error SQL: " + e.getMessage());
            return false;
        }
    }

    // --------------------- SELECCIONAR ID ---------------------
    private int seleccionarID(String tabla, String pk, String campo) {
        try {
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT " + pk + "," + campo + " FROM " + tabla
            );

            List<String> lista = new ArrayList<>();
            while (rs.next()) lista.add(rs.getInt(1) + " - " + rs.getString(2));

            if (lista.isEmpty()) return -1;

            Object sel = JOptionPane.showInputDialog(
                null, "Seleccione:", tabla,
                JOptionPane.QUESTION_MESSAGE, null,
                lista.toArray(), lista.get(0)
            );

            if (sel == null) return -1;

            return Integer.parseInt(sel.toString().split(" - ")[0]);

        } catch (SQLException e) {
            return -1;
        }
    }

    // ======================================================
    // MODULO INSERTAR (ahora con mensajes de éxito)
    // ======================================================
    public void insertarCliente() {
        String n = JOptionPane.showInputDialog("Nombre:");
        String t = JOptionPane.showInputDialog("Teléfono:");

        if (ejecutar("INSERT INTO Cliente (Nombre,Telefono) VALUES (?,?)", n, t))
            JOptionPane.showMessageDialog(null, "✔ Cliente insertado correctamente");
    }

    public void insertarProducto() {
        String n = JOptionPane.showInputDialog("Producto:");
        double p = Double.parseDouble(JOptionPane.showInputDialog("Precio:"));
        String d = JOptionPane.showInputDialog("Descripción:");

        if (ejecutar("INSERT INTO Producto (Nombre,Precio,Descripcion) VALUES (?,?,?)", n, p, d))
            JOptionPane.showMessageDialog(null, "✔ Producto insertado correctamente");
    }

    public void insertarInventario() {
        int idp = seleccionarID("Producto","ID_Producto","Nombre");
        int stock = Integer.parseInt(JOptionPane.showInputDialog("Stock:"));
        String fecha = JOptionPane.showInputDialog("Fecha:");

        if (ejecutar("INSERT INTO Inventario (ID_Producto,Stock,Fecha_Registro) VALUES (?,?,?)",
                 idp, stock, fecha))
            JOptionPane.showMessageDialog(null, "✔ Inventario insertado correctamente");
    }

    public void insertarVenta() {
        int idc = seleccionarID("Cliente","ID_Cliente","Nombre");
        int idt = seleccionarID("Terminal","ID_Terminal","Nombre_Usuario");
        String fecha = JOptionPane.showInputDialog("Fecha:");

        if (ejecutar("INSERT INTO Venta (Fecha,ID_Cliente,ID_Terminal) VALUES (?,?,?)",
                 fecha, idc, idt))
            JOptionPane.showMessageDialog(null, "✔ Venta insertada correctamente");
    }

    public void insertarDetalleVenta() {
        int idv = seleccionarID("Venta","ID_Venta","Fecha");
        int idp = seleccionarID("Producto","ID_Producto","Nombre");
        int c = Integer.parseInt(JOptionPane.showInputDialog("Cantidad:"));

        if (ejecutar("INSERT INTO DetalleVenta (ID_Venta,ID_Producto,Cantidad) VALUES (?,?,?)",
                 idv, idp, c))
            JOptionPane.showMessageDialog(null, "✔ Detalle insertado correctamente");
    }


    // ======================================================
    // MODULO ACTUALIZACION (con mensaje al finalizar)
    // ======================================================
    private void actualizarRegistro(String tabla, String pk, String campoMostrar,
                                   String[] campos, String[] tipos) {

        int id = seleccionarID(tabla, pk, campoMostrar);
        if (id == -1) return;

        Map<String,JCheckBox> map = new LinkedHashMap<>();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        for (String c : campos) {
            JCheckBox chk = new JCheckBox(c);
            panel.add(chk);
            map.put(c, chk);
        }

        int op = JOptionPane.showConfirmDialog(null, panel, "Seleccione campos",
                JOptionPane.OK_CANCEL_OPTION);
        if (op != JOptionPane.OK_OPTION) return;

        List<String> seleccionados = new ArrayList<>();
        for (var e : map.entrySet())
            if (e.getValue().isSelected()) seleccionados.add(e.getKey());

        if (seleccionados.isEmpty()) return;

        List<Object> valores = new ArrayList<>();
        for (String campo : seleccionados) {
            valores.add(JOptionPane.showInputDialog("Nuevo valor: " + campo));
        }

        StringBuilder sql = new StringBuilder("UPDATE " + tabla + " SET ");
        for (int i = 0; i < seleccionados.size(); i++) {
            sql.append(seleccionados.get(i)).append("=?");
            if (i < seleccionados.size() - 1) sql.append(",");
        }

        sql.append(" WHERE ").append(pk).append("=?");
        valores.add(id);

        // Ejecutar actualización
        if (ejecutar(sql.toString(), valores.toArray()))
            JOptionPane.showMessageDialog(null, "✔ Registro actualizado correctamente");
    }

    public void actualizarCliente() {
        actualizarRegistro("Cliente","ID_Cliente","Nombre",
                new String[]{"Nombre","Telefono"}, new String[]{"s","s"});
    }

    public void actualizarProducto() {
        actualizarRegistro("Producto","ID_Producto","Nombre",
                new String[]{"Nombre","Precio","Descripcion"},
                new String[]{"s","d","s"});
    }

    public void actualizarInventario() {
        actualizarRegistro("Inventario","ID_Inventario","Stock",
                new String[]{"Stock","Fecha_Registro"},
                new String[]{"i","s"});
    }

    public void actualizarVenta() {
        actualizarRegistro("Venta","ID_Venta","Fecha",
                new String[]{"Fecha"}, new String[]{"s"});
    }

    public void actualizarDetalleVenta() {
        actualizarRegistro("DetalleVenta","ID_DetalleVenta","Cantidad",
                new String[]{"Cantidad"}, new String[]{"i"});
    }


    // ======================================================
    // MODULO ELIMINAR
    // ======================================================
    private void eliminarRegistro(String tabla, String pk, String campoMostrar) {

        int id = seleccionarID(tabla, pk, campoMostrar);
        if (id == -1) return;

        int c = JOptionPane.showConfirmDialog(null,
                "¿ELIMINAR PERMANENTEMENTE?\nID: " + id,
                "Eliminar", JOptionPane.YES_NO_OPTION);

        if (c != JOptionPane.YES_OPTION) return;

        try {
            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM " + tabla + " WHERE " + pk + "=?");

            ps.setInt(1, id);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(null, "✔ Eliminado correctamente");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "No se puede eliminar.");
        }
    }

    // Métodos públicos
    public void eliminarCliente()      { eliminarRegistro("Cliente","ID_Cliente","Nombre"); }
    public void eliminarProducto()     { eliminarRegistro("Producto","ID_Producto","Nombre"); }
    public void eliminarInventario()   { eliminarRegistro("Inventario","ID_Inventario","Stock"); }
    public void eliminarVenta()        { eliminarRegistro("Venta","ID_Venta","Fecha"); }
    public void eliminarDetalleVenta() { eliminarRegistro("DetalleVenta","ID_DetalleVenta","Cantidad"); }
}
