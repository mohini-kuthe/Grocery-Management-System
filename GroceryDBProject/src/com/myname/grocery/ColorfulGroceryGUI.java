package com.myname.grocery;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.*;

class Product {
    int id;
    String name;
    double price;
    int stock;

    public Product(int id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public void setPrice(double price) {
        if (price >= 0) this.price = price;
    }

    public void increaseStock(int qty) {
        if (qty > 0) stock += qty;
    }

    public void reduceStock(int qty) {
        if (qty > 0 && qty <= stock) stock -= qty;
    }
}

class CartItem {
    Product product;
    int quantity;

    public CartItem(Product p, int q) {
        this.product = p;
        this.quantity = q;
    }

    public double getTotal() {
        return product.price * quantity;
    }
}

public class ColorfulGroceryGUI {

    static final String DB_URL = "jdbc:sqlite:grocery.db";
    static ArrayList<Product> inventory = new ArrayList<>();
    static ArrayList<CartItem> cart = new ArrayList<>();
    static int productCounter = 1;

    public static void main(String[] args) {
        createBillsFolder();
        initializeDatabase();
        loadInventoryFromDB();
        SwingUtilities.invokeLater(ColorfulGroceryGUI::showMainMenu);
    }

    // ================= DATABASE =================

    static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE TABLE IF NOT EXISTS products(" +
                    "id INTEGER PRIMARY KEY," +
                    "name TEXT," +
                    "price REAL," +
                    "stock INTEGER)");

            stmt.execute("CREATE TABLE IF NOT EXISTS purchases(" +
                    "bill_id INTEGER," +
                    "customer_name TEXT," +
                    "product_name TEXT," +
                    "quantity INTEGER," +
                    "total_price REAL," +
                    "date_time TEXT)");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void loadInventoryFromDB() {
        inventory.clear();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM products")) {

            while (rs.next()) {
                inventory.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock")
                ));
            }

            if (inventory.isEmpty()) {
                inventory.add(new Product(productCounter++, "Milk", 50, 100));
                inventory.add(new Product(productCounter++, "Bread", 30, 80));
                saveInventoryToDB();
            } else {
                productCounter = inventory.stream().mapToInt(p -> p.id).max().orElse(0) + 1;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveInventoryToDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            Statement del = conn.createStatement();
            del.execute("DELETE FROM products");

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products(id,name,price,stock) VALUES(?,?,?,?)");

            for (Product p : inventory) {
                ps.setInt(1, p.id);
                ps.setString(2, p.name);
                ps.setDouble(3, p.price);
                ps.setInt(4, p.stock);
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MAIN MENU =================

    static void showMainMenu() {
        JFrame frame = new JFrame("Grocery Store");
        frame.setSize(400, 250);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JLabel label = new JLabel("Welcome to Grocery Store", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel buttonPanel = new JPanel();
        JButton adminBtn = new JButton("Admin");
        JButton customerBtn = new JButton("Customer");

        buttonPanel.add(adminBtn);
        buttonPanel.add(customerBtn);

        frame.add(label, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);

        adminBtn.addActionListener(e -> {
            String password = JOptionPane.showInputDialog(frame, "Enter Admin Password:");
            if (password != null && password.equals("admin123")) {
                showAdminGUI();
                frame.dispose();
            } else {
                JOptionPane.showMessageDialog(frame, "Wrong Password!");
            }
        });

        customerBtn.addActionListener(e -> {
            showCustomerGUI();
            frame.dispose();
        });

        frame.setVisible(true);
    }

    // ================= ADMIN PANEL =================

    static void showAdminGUI() {
        JFrame frame = new JFrame("Admin Panel");
        frame.setSize(700, 400);
        frame.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Name", "Price", "Stock"}, 0);
        JTable table = new JTable(model);
        loadProductsToTable(model);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton backBtn = new JButton("Back");

        panel.add(addBtn);
        panel.add(updateBtn);
        panel.add(deleteBtn);
        panel.add(backBtn);

        addBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Product Name:");
            double price = Double.parseDouble(JOptionPane.showInputDialog("Price:"));
            int stock = Integer.parseInt(JOptionPane.showInputDialog("Stock:"));

            inventory.add(new Product(productCounter++, name, price, stock));
            saveInventoryToDB();
            loadProductsToTable(model);
        });

        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                Product p = findProduct((int) model.getValueAt(row, 0));

                double price = Double.parseDouble(JOptionPane.showInputDialog("New Price:", p.price));
                int stock = Integer.parseInt(JOptionPane.showInputDialog("New Stock:", p.stock));

                p.setPrice(price);
                p.stock = stock;

                saveInventoryToDB();
                loadProductsToTable(model);
            }
        });

        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                Product p = findProduct((int) model.getValueAt(row, 0));
                inventory.remove(p);
                saveInventoryToDB();
                loadProductsToTable(model);
            }
        });

        backBtn.addActionListener(e -> {
            showMainMenu();
            frame.dispose();
        });

        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    // ================= CUSTOMER PANEL =================

    static void showCustomerGUI() {
        JFrame frame = new JFrame("Customer Panel");
        frame.setSize(750, 400);
        frame.setLayout(new BorderLayout());

        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Name", "Price", "Stock"}, 0);
        JTable table = new JTable(model);
        loadProductsToTable(model);

        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel();

        JButton addCartBtn = new JButton("Add to Cart");
        JButton viewCartBtn = new JButton("View Cart");
        JButton updateCartBtn = new JButton("Update Cart");
        JButton removeCartBtn = new JButton("Remove Item");
        JButton billBtn = new JButton("Generate Bill");
        JButton backBtn = new JButton("Back");

        panel.add(addCartBtn);
        panel.add(viewCartBtn);
        panel.add(updateCartBtn);
        panel.add(removeCartBtn);
        panel.add(billBtn);
        panel.add(backBtn);

        addCartBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                Product p = findProduct((int) model.getValueAt(row, 0));
                int qty = Integer.parseInt(JOptionPane.showInputDialog("Quantity:", 1));

                if (qty > 0 && qty <= p.stock) {
                    cart.add(new CartItem(p, qty));
                    p.reduceStock(qty);
                    saveInventoryToDB();
                    loadProductsToTable(model);
                }
            }
        });

        viewCartBtn.addActionListener(e -> {
            if (cart.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Cart Empty!");
                return;
            }

            StringBuilder sb = new StringBuilder("Items in Cart:\n\n");

            for (CartItem i : cart) {
                sb.append(i.product.name)
                        .append(" | Qty: ").append(i.quantity)
                        .append(" | Total: ").append(i.getTotal())
                        .append("\n");
            }

            JOptionPane.showMessageDialog(frame, sb.toString());
        });

        removeCartBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter product name:");

            Iterator<CartItem> it = cart.iterator();

            while (it.hasNext()) {
                CartItem i = it.next();
                if (i.product.name.equalsIgnoreCase(name)) {
                    i.product.increaseStock(i.quantity);
                    it.remove();
                    saveInventoryToDB();
                    loadProductsToTable(model);
                    JOptionPane.showMessageDialog(frame, "Item Removed!");
                    return;
                }
            }

            JOptionPane.showMessageDialog(frame, "Item Not Found!");
        });

        updateCartBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog("Enter product name:");

            for (CartItem i : cart) {
                if (i.product.name.equalsIgnoreCase(name)) {
                    int newQty = Integer.parseInt(
                            JOptionPane.showInputDialog("New Quantity:", i.quantity));

                    int diff = newQty - i.quantity;

                    if (diff > 0) {
                        if (diff <= i.product.stock) {
                            i.product.reduceStock(diff);
                            i.quantity = newQty;
                        } else {
                            JOptionPane.showMessageDialog(frame, "Not enough stock!");
                            return;
                        }
                    } else if (diff < 0) {
                        i.product.increaseStock(-diff);
                        i.quantity = newQty;
                    }

                    saveInventoryToDB();
                    loadProductsToTable(model);
                    JOptionPane.showMessageDialog(frame, "Cart Updated!");
                    return;
                }
            }

            JOptionPane.showMessageDialog(frame, "Item Not Found!");
        });

        billBtn.addActionListener(e -> generateBill());

        backBtn.addActionListener(e -> {
            showMainMenu();
            frame.dispose();
        });

        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    // ================= BILL =================

    static void generateBill() {

        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Cart Empty!");
            return;
        }

        String customer = JOptionPane.showInputDialog("Customer Name:");
        if (customer == null || customer.trim().isEmpty())
            customer = "Guest";

        int billId = (int) (System.currentTimeMillis() / 1000);
        String dateTime = java.time.LocalDateTime.now().toString();

        double total = 0;
        StringBuilder bill = new StringBuilder();

        bill.append("Bill ID: ").append(billId).append("\n");
        bill.append("Customer: ").append(customer).append("\n");
        bill.append("Date: ").append(dateTime).append("\n\n");

        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO purchases(bill_id,customer_name,product_name,quantity,total_price,date_time) VALUES(?,?,?,?,?,?)");

            for (CartItem i : cart) {

                double itemTotal = i.getTotal();
                total += itemTotal;

                bill.append(i.product.name)
                        .append(" x ").append(i.quantity)
                        .append(" = ").append(itemTotal).append("\n");

                ps.setInt(1, billId);
                ps.setString(2, customer);
                ps.setString(3, i.product.name);
                ps.setInt(4, i.quantity);
                ps.setDouble(5, itemTotal);
                ps.setString(6, dateTime);
                ps.addBatch();
            }

            ps.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }

        double discount = (total > 1000) ? total * 0.10 : 0;
        double finalAmount = total - discount;

        bill.append("\nSubtotal: ").append(total);
        bill.append("\nDiscount: ").append(discount);
        bill.append("\nFinal Amount: ").append(finalAmount);

        try {
            String fileName = "Bills/Bill_" + billId + ".txt";
            PrintWriter pw = new PrintWriter(new File(fileName));
            pw.print(bill.toString());
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JOptionPane.showMessageDialog(null, bill.toString());

        cart.clear();
        saveInventoryToDB();
    }

    static void loadProductsToTable(DefaultTableModel model) {
        model.setRowCount(0);
        for (Product p : inventory) {
            model.addRow(new Object[]{p.id, p.name, p.price, p.stock});
        }
    }

    static Product findProduct(int id) {
        for (Product p : inventory)
            if (p.id == id) return p;
        return null;
    }

    static void createBillsFolder() {
        new File("Bills").mkdirs();
    }
}