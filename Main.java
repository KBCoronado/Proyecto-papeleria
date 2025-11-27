// =============== MAIN.JAVA ===============
import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        Conexion con = new Conexion();
        con.conectar();

        while (true) {
            int accion = JOptionPane.showOptionDialog(
                null,
                "Seleccione una acción:",
                "SISTEMA DE PAPELERÍA",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"INSERTAR", "ACTUALIZAR", "ELIMINAR", "SALIR"},
                "INSERTAR"
            );

            if (accion == 3 || accion == JOptionPane.CLOSED_OPTION) {
                con.desconectar();
                System.exit(0);
            }

            // INSERTAR
            if (accion == 0) {
                String opcion = (String) JOptionPane.showInputDialog(
                    null, "¿Qué desea insertar?", "INSERTAR",
                    JOptionPane.QUESTION_MESSAGE, null,
                    new String[]{"Cliente", "Producto", "Inventario", "Venta", "Detalle Venta", "Cancelar"},
                    "Cliente"
                );

                if (opcion == null || opcion.equals("Cancelar")) continue;

                switch (opcion) {
                    case "Cliente" -> con.insertarCliente();
                    case "Producto" -> con.insertarProducto();
                    case "Inventario" -> con.insertarInventario();
                    case "Venta" -> con.insertarVenta();
                    case "Detalle Venta" -> con.insertarDetalleVenta();
                }
            }

            // ACTUALIZAR
            if (accion == 1) {
                String opcion = (String) JOptionPane.showInputDialog(
                    null, "¿Qué desea actualizar?", "ACTUALIZAR",
                    JOptionPane.QUESTION_MESSAGE, null,
                    new String[]{"Cliente", "Producto", "Inventario", "Venta", "Detalle Venta", "Cancelar"},
                    "Cliente"
                );

                if (opcion == null || opcion.equals("Cancelar")) continue;

                switch (opcion) {
                    case "Cliente" -> con.actualizarCliente();
                    case "Producto" -> con.actualizarProducto();
                    case "Inventario" -> con.actualizarInventario();
                    case "Venta" -> con.actualizarVenta();
                    case "Detalle Venta" -> con.actualizarDetalleVenta();
                }
            }

            // ELIMINAR (REAL – NO LÓGICO)
            if (accion == 2) {

                String opcion = (String) JOptionPane.showInputDialog(
                    null, "¿Qué desea eliminar?", "ELIMINAR",
                    JOptionPane.QUESTION_MESSAGE, null,
                    new String[]{"Cliente", "Producto", "Inventario", "Venta", "Detalle Venta", "Cancelar"},
                    "Cliente"
                );

                if (opcion == null || opcion.equals("Cancelar")) continue;

                switch (opcion) {
                    case "Cliente" -> con.eliminarCliente();
                    case "Producto" -> con.eliminarProducto();
                    case "Inventario" -> con.eliminarInventario();
                    case "Venta" -> con.eliminarVenta();
                    case "Detalle Venta" -> con.eliminarDetalleVenta();
                }
            }
        }
    }
}
